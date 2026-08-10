package com.auralis.app.visualization

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lightweight OpenGL ES 2.0 renderer: a slowly rotating wireframe sphere at the
 * center (scales with bass), a flat audio ring, and a field of drifting points
 * that rise with high-frequency energy. Deliberately low-poly and cheap.
 */
class Audio3DRenderer(
    private val quality: Quality = Quality.BALANCED,
) : GLSurfaceView.Renderer {

    enum class Quality(val particleCount: Int) { LOW(60), BALANCED(140), HIGH(260) }

    // Audio-reactive inputs, written from the UI thread, read on the GL thread.
    @Volatile var bass = 0f
    @Volatile var rms = 0f
    @Volatile var highEnergy = 0f
    @Volatile var playing = false
    @Volatile var reducedMotion = false

    private var program = 0
    private var startTime = System.nanoTime()

    private val view = FloatArray(16)
    private val proj = FloatArray(16)
    private val vp = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    private lateinit var sphere: Mesh
    private lateinit var ring: Mesh
    private lateinit var particles: Mesh

    private val particleCount = quality.particleCount
    private val particleSeeds = FloatArray(particleCount * 4) // x,y,z,speed

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.031f, 0.035f, 0.043f, 1f) // #08090B
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)

        sphere = Mesh(buildSphere(latSegments = 12, lonSegments = 18, radius = 0.55f), GLES20.GL_LINES)
        ring = Mesh(buildRing(segments = 64, radius = 1.0f), GLES20.GL_LINE_LOOP)

        // Seed particles in a disc around the center
        val rnd = java.util.Random(42)
        for (i in 0 until particleCount) {
            val angle = rnd.nextFloat() * Math.PI * 2
            val r = 0.8f + rnd.nextFloat() * 1.4f
            particleSeeds[i * 4 + 0] = (cos(angle) * r).toFloat()
            particleSeeds[i * 4 + 1] = (rnd.nextFloat() - 0.5f) * 1.6f
            particleSeeds[i * 4 + 2] = (sin(angle) * r).toFloat()
            particleSeeds[i * 4 + 3] = 0.2f + rnd.nextFloat() * 0.6f
        }
        particles = Mesh(buildParticles(), GLES20.GL_POINTS, dynamic = true)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height
        Matrix.perspectiveM(proj, 0, 45f, aspect, 0.1f, 100f)
        // Slow, distant camera
        Matrix.setLookAtM(view, 0, 0f, 0.6f, 4.2f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)
    }

    override fun onDrawFrame(unused: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        val t = (System.nanoTime() - startTime) / 1_000_000_000f
        val motionScale = if (reducedMotion) 0.05f else 1f
        val rotation = if (reducedMotion) 0f else t * 8f // slow degrees/sec
        val brightness = 0.35f + rms * 0.65f
        val sphereScale = 1f + bass * 0.35f

        val uMvp = GLES20.glGetUniformLocation(program, "uMvp")
        val uColor = GLES20.glGetUniformLocation(program, "uColor")

        // Central sphere — accent lavender, scales with bass
        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, rotation, 0f, 1f, 0f)
        Matrix.rotateM(model, 0, 20f, 1f, 0f, 0f)
        Matrix.scaleM(model, 0, sphereScale, sphereScale, sphereScale)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glUniform4f(uColor, 0.56f, 0.65f, 1.0f, 0.55f * brightness)
        sphere.draw(program)

        // Audio ring — green activity tint, expands slightly with RMS
        Matrix.setIdentityM(model, 0)
        val ringScale = 1f + rms * 0.2f
        Matrix.scaleM(model, 0, ringScale, 1f, ringScale)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glUniform4f(uColor, 0.45f, 0.9f, 0.76f, 0.35f * brightness)
        ring.draw(program)

        // Particles — rise with high-frequency energy
        updateParticles(t, motionScale)
        Matrix.setIdentityM(model, 0)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glUniform4f(uColor, 0.72f, 0.77f, 1.0f, 0.5f * brightness)
        particles.draw(program)
    }

    private fun updateParticles(t: Float, motionScale: Float) {
        val verts = particles.vertices
        val activity = if (playing) 0.4f + highEnergy * 1.2f else 0.15f
        for (i in 0 until particleCount) {
            val sx = particleSeeds[i * 4 + 0]
            val sy = particleSeeds[i * 4 + 1]
            val sz = particleSeeds[i * 4 + 2]
            val speed = particleSeeds[i * 4 + 3]
            // gentle vertical drift + slight radial sway
            val y = sy + sin(t * speed * motionScale + i) * 0.15f * activity + highEnergy * 0.4f * speed
            val sway = 1f + sin(t * 0.5f * motionScale + i * 0.7f) * 0.05f
            verts[i * 3 + 0] = sx * sway
            verts[i * 3 + 1] = y
            verts[i * 3 + 2] = sz * sway
        }
        particles.update()
    }

    // ---- geometry builders ----

    private fun buildSphere(latSegments: Int, lonSegments: Int, radius: Float): FloatArray {
        val verts = ArrayList<Float>()
        fun point(lat: Int, lon: Int): FloatArray {
            val theta = Math.PI * lat / latSegments
            val phi = 2 * Math.PI * lon / lonSegments
            return floatArrayOf(
                (radius * sin(theta) * cos(phi)).toFloat(),
                (radius * cos(theta)).toFloat(),
                (radius * sin(theta) * sin(phi)).toFloat(),
            )
        }
        for (lat in 0..latSegments) {
            for (lon in 0 until lonSegments) {
                val a = point(lat, lon)
                val b = point(lat, lon + 1)
                val c = point(lat + 1, lon)
                verts.addAll(a.toList()); verts.addAll(b.toList())
                verts.addAll(a.toList()); verts.addAll(c.toList())
            }
        }
        return verts.toFloatArray()
    }

    private fun buildRing(segments: Int, radius: Float): FloatArray {
        val verts = FloatArray(segments * 3)
        for (i in 0 until segments) {
            val a = 2 * Math.PI * i / segments
            verts[i * 3 + 0] = (cos(a) * radius).toFloat()
            verts[i * 3 + 1] = 0f
            verts[i * 3 + 2] = (sin(a) * radius).toFloat()
        }
        return verts
    }

    private fun buildParticles(): FloatArray = FloatArray(particleCount * 3)

    // ---- GL plumbing ----

    private class Mesh(verts: FloatArray, val mode: Int, val dynamic: Boolean = false) {
        val vertices: FloatArray = verts
        private var buffer: FloatBuffer = ByteBuffer.allocateDirect(verts.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(verts); position(0)
            }
        val count = verts.size / 3

        fun update() {
            buffer.clear()
            buffer.put(vertices)
            buffer.position(0)
        }

        fun draw(program: Int) {
            val aPos = GLES20.glGetAttribLocation(program, "aPos")
            GLES20.glEnableVertexAttribArray(aPos)
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 12, buffer)
            if (mode == GLES20.GL_POINTS) GLES20.glLineWidth(1f)
            GLES20.glDrawArrays(mode, 0, count)
            GLES20.glDisableVertexAttribArray(aPos)
        }
    }

    private fun buildProgram(vs: String, fs: String): Int {
        fun compile(type: Int, src: String): Int {
            val id = GLES20.glCreateShader(type)
            GLES20.glShaderSource(id, src)
            GLES20.glCompileShader(id)
            return id
        }
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, compile(GLES20.GL_VERTEX_SHADER, vs))
        GLES20.glAttachShader(p, compile(GLES20.GL_FRAGMENT_SHADER, fs))
        GLES20.glLinkProgram(p)
        return p
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMvp;
            attribute vec3 aPos;
            void main() {
                gl_Position = uMvp * vec4(aPos, 1.0);
                gl_PointSize = 3.0;
            }
        """
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 uColor;
            void main() { gl_FragColor = uColor; }
        """
    }
}

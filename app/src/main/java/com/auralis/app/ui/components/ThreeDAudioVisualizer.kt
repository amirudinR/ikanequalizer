package com.auralis.app.ui.components

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.auralis.app.visualization.Audio3DRenderer

/**
 * Hosts the OpenGL 3D audio field inside Compose. Pauses rendering when the
 * lifecycle stops to save battery; pushes audio-reactive values into the renderer.
 */
@Composable
fun ThreeDAudioVisualizer(
    bass: Float,
    rms: Float,
    highEnergy: Float,
    playing: Boolean,
    reducedMotion: Boolean,
    quality: Audio3DRenderer.Quality,
    modifier: Modifier = Modifier,
) {
    val renderer = remember(quality) { Audio3DRenderer(quality) }
    val glView = remember { mutableStateOf<GLSurfaceView?>(null) }

    // Push latest audio values into the renderer each recomposition
    renderer.bass = bass
    renderer.rms = rms
    renderer.highEnergy = highEnergy
    renderer.playing = playing
    renderer.reducedMotion = reducedMotion

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.value?.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.value?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(2)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                glView.value = this
            }
        },
    )
}

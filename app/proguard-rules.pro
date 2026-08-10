# Keep Kotlin serialization
-keepclassmembers class **$serializer { *; }
-keepclasseswithmembernames class * { kotlinx.serialization.KSerializer serializer(); }
# Keep data models used by serialization
-keep class com.auralis.app.data.model.** { *; }

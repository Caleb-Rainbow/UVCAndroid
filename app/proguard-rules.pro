# Preserve line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep CameraHelper and related interfaces (used via reflection by JNI layer)
-keep class com.herohan.uvcapp.CameraHelper { *; }
-keep class com.herohan.uvcapp.ICameraHelper { *; }
-keep class com.herohan.uvcapp.ICameraHelper$* { *; }
-keep class com.herohan.uvcapp.IImageCapture { *; }
-keep class com.herohan.uvcapp.IImageCapture$* { *; }
-keep class com.herohan.uvcapp.VideoCapture { *; }
-keep class com.herohan.uvcapp.VideoCapture$* { *; }
-keep class com.herohan.uvcapp.CameraException { *; }

# Keep UVC library classes (JNI bridge)
-keep class com.serenegiant.** { *; }

# Compose default rules are provided by the Compose compiler plugin

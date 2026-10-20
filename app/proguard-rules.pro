# R8 rules for samar's release build.
# AndroidX and Compose ship their own rules inside their libraries.

# ML Kit face detection. Without these, R8 breaks how ML Kit finds its face
# detector at runtime: FaceDetection.getClient() crashes with a
# NullPointerException in release builds only, and the editor shows
# "Couldn't open this photo." Found on Android 8.0 (API 26), October 2026.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_face.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_face_bundled.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_common.** { *; }
-keep class com.google.firebase.components.** { *; }

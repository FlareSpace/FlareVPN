# FlareVPN ProGuard Rules

# Attributes
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext {
    volatile <fields>;
}

# Sing-box (libbox) - Critical for VPN functionality
-keep class io.nekohasekai.libbox.** { *; }
-keep class io.nekohasekai.libbox.internal.** { *; }

# Gson (if used via dependencies)
-keep class com.google.gson.** { *; }
-keep @com.google.gson.annotations.SerializedName class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Bouncy Castle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# SSHJ
-keep class com.hierynomus.** { *; }
-keep class net.schmizz.** { *; }
-dontwarn com.hierynomus.**
-dontwarn net.schmizz.**
-dontwarn org.slf4j.**

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# App Specific Data layer: Models, Entities, DAOs, Database, and Parsers
-keep class flare.client.app.data.** { *; }

# Sing-box Integration (critical to prevent JNI callback name mismatches)
-keep class flare.client.app.singbox.** { *; }

# Background Services
-keep class flare.client.app.service.** { *; }

# ViewModels and UI Components
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class flare.client.app.ui.widget.** { *; }
-keep class flare.client.app.ui.components.** { *; }

# BlurView & Haze
-keep class com.eightbitlab.blurview.** { *; }
-keep class dev.chrisbanes.haze.** { *; }

# ZXing QR Generator
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# Go Mobile Runtime (critical for libbox JNI communication)
-keep class go.** { *; }

# CameraX
-keep class androidx.camera.** { *; }
-keep interface androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Google ML Kit Barcode Scanning
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep Google Play Services components used by ML Kit
-keep class com.google.android.gms.** { *; }
-keep interface com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep Google Datatransport (telemetry used by ML Kit)
-keep class com.google.android.datatransport.** { *; }
-keep interface com.google.android.datatransport.** { *; }
-dontwarn com.google.android.datatransport.**


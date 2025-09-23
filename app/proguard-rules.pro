# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 카카오 SDK ProGuard 규칙
-keep class com.kakao.sdk.** { *; }
-keep class com.kakao.maps.open.** { *; }
-keep class com.kakao.vectormap.** { *; }
-dontwarn com.kakao.sdk.**
-dontwarn com.kakao.maps.open.**
-dontwarn com.kakao.vectormap.**

# OpenGL ES 및 GPU 렌더링 관련 최적화
-keep class javax.microedition.khronos.** { *; }
-keep class android.opengl.** { *; }
-keep class com.google.android.gles.** { *; }
-dontwarn javax.microedition.khronos.**
-dontwarn android.opengl.**

# 네이티브 라이브러리 관련
-keepclasseswithmembernames class * {
    native <methods>;
}

# GPU 드라이버 관련 클래스 보존
-keep class * extends android.view.Surface { *; }
-keep class * extends android.view.SurfaceView { *; }

# 하드웨어 가속 관련
-keep class android.view.** { *; }
-keep class android.graphics.** { *; }

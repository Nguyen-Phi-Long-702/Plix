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

# ==========================================================================
# Day 11 - Chuan bi som ProGuard cho Sprint 5/6 (chua bat, isMinifyEnabled=false)
# Dagger/Hilt KHONG can keep rule rieng vi dung annotation processing
# (sinh code o compile-time, khong dung reflection runtime).
# ==========================================================================

# ---- Retrofit (retrofit2:3.0.0) ----
# Cac rule nay thuc ra da duoc dong goi san trong .aar cua Retrofit
# (consumer proguard rules, tu ap dung). Liet ke tuong minh o day de
# lam tai lieu tham khao khi bat minify that.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ---- OkHttp (dung boi Retrofit + logging-interceptor) ----
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# ---- Gson (converter-gson) - QUAN TRONG, day la rule THAT SU CAN THIET ----
# Gson dung reflection de doc/ghi field cua DTO khi parse JSON. Khac voi
# Retrofit/OkHttp o tren (da co consumer rule dong goi san), Gson KHONG
# BIET cac lop DTO cua rieng project nay de tu giu - neu thieu rule nay,
# khi bat minify cac field trong DTO co the bi doi ten/xoa, lam sai JSON.
-keep class com.longvuong.plix.data.remote.dto.** { *; }

# ---- Room (room-runtime:2.8.4) ----
# Rule "-keep class * extends androidx.room.RoomDatabase" da duoc dong
# goi san trong .aar cua room-runtime (tu ap dung). Entity/Dao duoc Room
# compiler sinh code truy cap truc tiep o compile-time, khong qua
# reflection runtime nen ve nguyen tac khong bat buoc giu rieng. Van liet
# ke them dong duoi day lam lop phong thu bo sung theo dung yeu cau ke
# hoach (Retrofit/Room):
-keep class com.longvuong.plix.data.local.entity.** { *; }
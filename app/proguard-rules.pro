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

# Giữ lại lớp StaticWelcomeHelper và các thành viên của nó, bao gồm cả các lớp nội bộ ẩn danh.
-keep class com.keenon.peanut.common.welcome.StaticWelcomeHelper { *; }
-keep class com.keenon.peanut.common.welcome.StaticWelcomeHelper$* { *; }

# Bảo vệ tất cả các class trong package welcome
-keep class com.keenon.peanut.common.welcome.** { *; }

# Bảo vệ tất cả các inner class và anonymous class
-keepclassmembers class com.keenon.peanut.common.welcome.** {
    *;
}

# Giữ lại các lớp liên quan đến TheRouter, được sử dụng trong StaticWelcomeHelper.
-keep class com.therouter.** { *; }

# Giữ lại tất cả các inner class và anonymous class
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Giữ lại tất cả các class trong package peanut
-keep class com.keenon.peanut.** { *; }
-keep class com.keenon.peanut.core.utils.third.** { *; }

# Giữ lại tất cả các method và field
-keepclassmembers class com.keenon.peanut.** {
    *;
}
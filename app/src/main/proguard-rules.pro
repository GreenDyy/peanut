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

# Giữ lại các lớp liên quan đến TheRouter, được sử dụng trong StaticWelcomeHelper.
-keep class com.therouter.** { *; }
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

# AGP 9 的 proguard-android-optimize.txt 默认丢弃 SourceFile 与 LineNumberTable，
# 混淆后的用户堆栈将失去文件名与行号；sideload 分发没有 Play Console 自动还原，必须保留。
-keepattributes SourceFile,LineNumberTable

-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-dontwarn org.slf4j.**
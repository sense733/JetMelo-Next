# Consumer ProGuard rules for ncmapi
# Preserve model classes and kotlinx.serialization generated serializers to prevent release minification crashes

-keep class com.rcmiku.ncmapi.model.** { *; }
-keep interface com.rcmiku.ncmapi.model.** { *; }
-keepclassmembers class com.rcmiku.ncmapi.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.rcmiku.ncmapi.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class com.rcmiku.ncmapi.model.**$$serializer { *; }
-keepclassmembers class com.rcmiku.ncmapi.model.** {
    @kotlinx.serialization.SerialName <fields>;
}

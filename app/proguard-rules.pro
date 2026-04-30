# Keep JNI bridge names stable in release builds.
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class com.aegisinput.engine.RimeBridge {
    <methods>;
}

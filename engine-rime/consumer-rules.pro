# Preserve native entry points for apps that shrink the engine module.
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class com.aegisinput.engine.RimeBridge {
    <methods>;
}

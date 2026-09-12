# Add project specific ProGuard rules here.
# Gson models are serialized/deserialized reflectively; keep them if minification is enabled.
-keep class com.notishuttle.model.** { *; }
-keep class com.google.gson.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

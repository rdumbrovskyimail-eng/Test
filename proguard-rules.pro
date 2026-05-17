# Default proguard rules
-keep class com.test.app.** { *; }
-dontwarn okio.**
-dontwarn javax.annotation.**

-keepattributes Signature
-keep class retrofit2.** { *; }
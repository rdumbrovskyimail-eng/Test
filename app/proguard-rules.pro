-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod
-dontobfuscate

-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }

-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keepclassmembers class androidx.compose.** {
    <init>(...);
}
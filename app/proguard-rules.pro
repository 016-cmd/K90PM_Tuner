# K90PM Tuner ProGuard Rules

# Xposed
-keep class de.robv.android.xposed.** { *; }
-keep class com.k90pm.tuner.hook.** { *; }

# libsu
-dontwarn com.topjohnwu.superuser.**

# Kotlin
-keepattributes *Annotation*
-keepattributes InnerClasses
-keep class kotlin.Metadata { *; }

# Compose
-dontwarn androidx.compose.**

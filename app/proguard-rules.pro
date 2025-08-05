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

# Keep Google API classes
-keep class com.google.api.** { *; }
-keep class com.google.auth.** { *; }
-keep class com.google.cloud.** { *; }
-keep class com.google.gson.** { *; }

# Keep service account credential files and related classes
-keep class * extends java.io.Serializable { *; }

# Keep our data models for Gson serialization
-keep class com.sj9.chavara.data.model.** { *; }

# Prevent obfuscation of Google credential fields
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Google HTTP client classes
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**

# Keep Google Auth library classes
-keep class com.google.auth.** { *; }
-dontwarn com.google.auth.**

# Keep Google Cloud Storage classes
-keep class com.google.cloud.storage.** { *; }
-dontwarn com.google.cloud.storage.**

# Keep classes that use reflection
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

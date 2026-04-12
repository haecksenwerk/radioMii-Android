# Add project specific ProGuard rules here.
-keep class com.radiomii.data.remote.dto.** { *; }
-keep class com.radiomii.domain.model.** { *; }

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepattributes Signature
-keepattributes *Annotation*

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class * { @kotlinx.serialization.Serializable <fields>; }

# Media3
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.extractor.** { *; }

# Hilt
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel
-keepclasseswithmembers class * { @javax.inject.Inject <init>(...); }
-keep class javax.inject.** { *; }

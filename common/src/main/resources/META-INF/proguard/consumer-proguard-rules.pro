-dontobfuscate
-keep,allowoptimization class dev.brahmkshatriya.echo.common.** { public protected *; }
-keep,allowoptimization class kotlin.** { public protected *; }
-keep,allowoptimization class kotlinx.coroutines.** { public protected *; }
-keep,allowoptimization class kotlinx.serialization.** { public protected *; }
-keep,allowoptimization class okhttp3.** { public protected *; }
-keep,allowoptimization class com.google.protobuf.** { public protected *; }

-keep class okhttp3.WebSocketListener { *; }
-keep class kotlinx.serialization.json.JsonTransformingSerializer { *; }
-keep class kotlin.jvm.internal.MutablePropertyReference1Impl { *; }
-keep class com.google.protobuf.GeneratedMessageV3 { *; }

-dontwarn com.oracle.svm.core.annotate.Delete
-dontwarn com.oracle.svm.core.annotate.Substitute
-dontwarn com.oracle.svm.core.annotate.TargetClass
-dontwarn java.lang.Module
-dontwarn org.graalvm.nativeimage.hosted.Feature$BeforeAnalysisAccess
-dontwarn org.graalvm.nativeimage.hosted.Feature
-dontwarn org.graalvm.nativeimage.hosted.RuntimeResourceAccess

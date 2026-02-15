 # Landscapist + Ktor rules for Desktop

-keep class io.ktor.** { *; }
-keep class io.ktor.client.engine.cio.CIOEngineContainer { *; }
-keep class * implements io.ktor.client.HttpClientEngineContainer { *; }
-keep class okio.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep enum com.skydoves.landscapist.** { *; }
-keep class com.skydoves.landscapist.core.model.** { *; }
-dontoptimize
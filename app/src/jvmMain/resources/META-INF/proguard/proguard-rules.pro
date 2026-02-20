#noinspection ShrinkerUnresolvedReference
-dontoptimize
-keep class io.ktor.client.engine.cio.CIOEngineContainer { *; }
-keep enum com.skydoves.landscapist.** { *; }
-keep class com.skydoves.landscapist.core.model.** { *; }

-keep class com.sun.jna.** { *; }
-keep class com.kdroid.composetray.** { *; }

-keep class org.freedesktop.dbus.** { *; }
-keep class io.github.selemba1000.** { *; }
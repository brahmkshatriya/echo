
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJVM)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}
dependencies {
    implementation(project(":app"))
}

apply(from = "proguards.gradle.kts")

compose.desktop {
    application {
        mainClass = "dev.brahmkshatriya.echo.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.AppImage)
            packageName = property("GROUP").toString()
            packageVersion = "${property("VERSION")}"
            buildTypes.release.proguard {
                configurationFiles.from(
                    tasks.named("proguards").map {
                        it.outputs.files.asFileTree.matching { include("**/*.pro") }
                    }
                )
            }
        }
    }
}
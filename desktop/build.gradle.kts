import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJVM)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

group = property("GROUP").toString()
version = property("VERSION").toString()

dependencies {
    implementation(projects.app)
}

apply(from = "proguards.gradle.kts")

compose.resources {
    publicResClass = true
    packageOfResClass = "echo.app.generated.resources"
    generateResClass = always
}

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
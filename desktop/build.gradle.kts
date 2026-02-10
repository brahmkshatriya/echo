import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.kotlinJVM)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}
dependencies {
    implementation(project(":app"))
}

val collectDependencyProguard = tasks.register("collectDependencyProguard") {
    val outDir = layout.buildDirectory.dir("proguard")
    outputs.dir(outDir)
    val runtimeArtifactFilesProvider = providers.provider {
        val config = configurations.findByName("jvmRuntimeClasspath") ?: configurations.findByName("runtimeClasspath")
        if (config == null) {
            logger.warn("No runtime classpath configuration found; collectDependencyProguard will be empty.")
            emptyList()
        } else {
            config.resolvedConfiguration.resolvedArtifacts.map { it.file }
        }
    }
    inputs.files(runtimeArtifactFilesProvider.map { it.toTypedArray() })

    doLast {
        val out = outDir.get().asFile
        out.mkdirs()
        val artifactFiles = runtimeArtifactFilesProvider.get()
        artifactFiles.forEach { file ->
            if (!file.exists()) return@forEach
            ZipFile(file).use { zip ->
                val entries = zip.entries().asSequence().toList().filterIsInstance<ZipEntry>()
                entries.filter { entry ->
                    val name = entry.name
                    (name.startsWith("META-INF/") || name.startsWith("META-INF\\")) && name.endsWith(".pro")
                }.forEach label@{ entry ->
                    if (entry.name.endsWith("r8.pro")) return@label
                    val dest = File(out, File(entry.name).name)
                    zip.getInputStream(entry).use { input ->
                        dest.outputStream().use { outStream ->
                            input.copyTo(outStream)
                        }
                    }
                }
            }
        }
    }
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
                    collectDependencyProguard.map {
                        it.outputs.files.asFileTree.matching { include("**/*.pro") }
                    }
                )
            }
        }
    }
}
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

tasks.register("proguards") {
    val outDir = layout.buildDirectory.dir("proguard")
    outputs.dir(outDir)
    val runtimeArtifactFilesProvider = providers.provider {
        val config = configurations.findByName("jvmRuntimeClasspath")
            ?: configurations.findByName("runtimeClasspath")
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
                    if (entry.name.contains("landscapist")) return@label
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

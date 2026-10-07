plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeNative)
    alias(libs.plugins.composeCompiler)
}

group = property("GROUP").toString()
version = property("VERSION").toString()

kotlin {
    desktopNative {
        binaries.executable {
            entryPoint = "dev.brahmkshatriya.echo.main"
            when {
                System.getProperty("os.name").startsWith("Mac", ignoreCase = true) -> {
                    val sdlLibraryDir = providers.environmentVariable("SDL3_LIBRARY_DIR").orNull
                    if (sdlLibraryDir != null) linkerOpts("-L$sdlLibraryDir")
                    linkerOpts("-lSDL3")
                }

                System.getProperty("os.name").startsWith("Linux", ignoreCase = true) -> {
                    val libraryDirs =
                        providers.environmentVariable("LINUX_NATIVE_LIBRARY_DIRS").orNull
                            ?.split(':')
                            ?.filter(String::isNotBlank)
                            .orEmpty()
                            .ifEmpty { listOf("/usr/lib") }
                    libraryDirs.forEach { linkerOpts("-L$it") }
                    linkerOpts("-lEGL")
                }
            }
        }
    }

    sourceSets {
        desktopNativeMain.dependencies {
            implementation(projects.client)
            implementation(libs.compose.native.desktop)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "echo.desktop.generated.resources"
    generateResClass = always
}

composeNativeApplication {
    applicationName.set("Echo")
    packageName.set("dev.brahmkshatriya.echo")
    executableName.set("echo")
    description.set("Music, but better")
    categories.set(listOf("AudioVideo", "Audio", "Player"))
    startupWmClass.set("Echo")
    iconFile.set(rootProject.layout.projectDirectory.file("apps/android/src/main/ic_launcher-playstore.png"))

    providers.environmentVariable("LINUX_ARM64_RUNTIME_DIR").orNull?.let {
        linuxArm64RuntimeFiles.from(file(it))
    }
}

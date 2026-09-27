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
                    linkerOpts("-L/usr/lib")
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
}

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

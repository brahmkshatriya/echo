plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

group = property("GROUP").toString()
version = property("VERSION").toString()

val gitHash = execute("git", "rev-parse", "HEAD").take(7)
val gitCount = execute("git", "rev-list", "--count", "HEAD").toInt()

android {
    namespace = "dev.brahmkshatriya.echo"
    compileSdk {
        val (major, minor) = libs.versions.compileSdk.get().split('.').map(String::toInt)
        version = release(major) { minorApiLevel = minor }
    }

    defaultConfig {
        applicationId = group.toString()
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = compileSdk
        versionCode = gitCount
        versionName = "${version}-$gitHash"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlin.get()
    }
}

dependencies {
    implementation(projects.client)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)
}

fun execute(vararg command: String): String = providers.exec {
    commandLine(*command)
}.standardOutput.asText.get().trim()

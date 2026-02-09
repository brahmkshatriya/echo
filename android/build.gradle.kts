plugins {
    alias(libs.plugins.androidApplication)
}
val gitHash = execute("git", "rev-parse", "HEAD").take(7)
val gitCount = execute("git", "rev-list", "--count", "HEAD").toInt()

android {
    namespace = "dev.brahmkshatriya.echo.android"
    compileSdk = 36

    defaultConfig {
        applicationId = property("GROUP").toString()
        minSdk = 24
        targetSdk = 36
        versionCode = gitCount
        versionName = "${property("VERSION")}-$gitHash"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            packaging.resources.excludes.add("META-INF/*")
        }
    }
}

dependencies {
    implementation(project(":app"))
}

fun execute(vararg command: String): String = providers.exec {
    commandLine(*command)
}.standardOutput.asText.get().trim()
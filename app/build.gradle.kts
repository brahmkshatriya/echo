plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)

    alias(libs.plugins.gms) apply false
    alias(libs.plugins.crashlytics) apply false
}

val hasGoogleServices = file("google-services.json").exists()
val gitHash = execute("git", "rev-parse", "HEAD").take(7)
val gitCount = execute("git", "rev-list", "--count", "HEAD").toInt()
val version = "3.0.$gitCount"

android {
    namespace = "dev.brahmkshatriya.echo"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.brahmkshatriya.echo"
        minSdk = 24
        targetSdk = 36
        versionCode = gitCount
        versionName = "v${version}_$gitHash($gitCount)"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
            )
        }
        create("nightly") {
            initWith(getByName("release"))
            applicationIdSuffix = ".nightly"
            resValue("string", "app_name", "Echo Nightly")
        }
        create("stable") {
            initWith(getByName("release"))
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":common"))
    implementation(libs.kotlin.reflect)
    implementation(libs.bundles.androidx)
    implementation(libs.material)
    implementation(libs.bundles.paging)
    implementation(libs.filekache)
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.media3)
    implementation(libs.bundles.coil)

    implementation(libs.pikolo)
    implementation(libs.fadingedgelayout)
    implementation(libs.fastscroll)
    implementation(libs.kenburnsview)
    implementation(libs.nestedscrollwebview)
    implementation(libs.acsbendi.webview)

    if (!hasGoogleServices) return@dependencies
    implementation(libs.bundles.firebase)
}

if (hasGoogleServices) {
    apply(plugin = libs.plugins.gms.get().pluginId)
    apply(plugin = libs.plugins.crashlytics.get().pluginId)
}

fun execute(vararg command: String): String = providers.exec {
    commandLine(*command)
}.standardOutput.asText.get().trim()
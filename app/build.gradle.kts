import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.gms)
    alias(libs.plugins.crashlytics)
}

val gitHash = execute("git", "rev-parse", "HEAD").take(7)
val gitCount = execute("git", "rev-list", "--count", "HEAD").toInt()

android {
    namespace = "dev.brahmkshatriya.echo"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.brahmkshatriya.echo"
        minSdk = 24
        targetSdk = 35
        versionCode = gitCount
        versionName = "1.0.0-$gitHash"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    viewBinding {
        enable = true
    }
    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
}

dependencies {
    implementation(project(":common"))
    implementation(libs.plugger)
    implementation("org.smali:dexlib2:2.5.2")

    implementation(libs.appcompat)

    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.swiperefresh)

    implementation(libs.fragment)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.bundles.paging)
    implementation(libs.preference)

    implementation(libs.work.runtime.ktx)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.bundles.media3)

    implementation(libs.splashscreen)
    implementation(libs.palette)

    implementation(libs.material)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    implementation(libs.bundles.coil)

    implementation(libs.pikolo)
    implementation(libs.fadingedgelayout)
    implementation(libs.fastscroll)
    implementation(libs.kenburnsview)
    implementation(libs.nestedscrollwebview)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.junit)
}

fun execute(vararg command: String): String {
    val outputStream = ByteArrayOutputStream()
    project.exec {
        commandLine(*command)
        standardOutput = outputStream
    }
    return outputStream.toString().trim()
}
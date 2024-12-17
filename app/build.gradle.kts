import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    kotlin("plugin.serialization") version "2.0.10"
    id("com.google.gms.google-services") version "4.4.2"
    id("com.google.firebase.crashlytics") version "3.0.2"
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
    implementation("com.github.jeelpatel231:plugger:82e00ea5a4")

    implementation("androidx.appcompat:appcompat:1.7.0")

    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.paging:paging-common-ktx:3.3.5")
    implementation("androidx.paging:paging-runtime-ktx:3.3.5")
    implementation("androidx.preference:preference-ktx:1.2.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.media3:media3-exoplayer:1.5.0")
    implementation("androidx.media3:media3-session:1.5.0")
    implementation("androidx.media3:media3-ui:1.5.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.5.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.0")

    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.palette:palette-ktx:1.0.0")

    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.google.android.material:material:1.12.0")

    implementation("com.google.dagger:hilt-android:2.48.1")
    ksp("com.google.dagger:hilt-android-compiler:2.48.1")

    implementation("io.coil-kt.coil3:coil:3.0.0-rc01")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.0-rc01")

    implementation("com.github.madrapps:pikolo:2.0.2")
    implementation("com.github.bosphere.android-fadingedgelayout:fadingedgelayout:1.0.0")
    implementation("me.zhanghai.android.fastscroll:library:1.3.0")
    implementation("com.flaviofaria:kenburnsview:1.0.7")
    implementation("com.telefonica:nestedscrollwebview:0.1.6")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")

    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
    
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.9.24")
    testImplementation("junit:junit:4.13.2")
}

fun execute(vararg command: String): String {
    val outputStream = ByteArrayOutputStream()
    project.exec {
        commandLine(*command)
        standardOutput = outputStream
    }
    return outputStream.toString().trim()
}
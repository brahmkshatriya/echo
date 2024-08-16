plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    kotlin("plugin.serialization") version "2.0.10"
}

android {
    namespace = "dev.brahmkshatriya.echo"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.brahmkshatriya.echo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
    implementation("com.github.brahmkshatriya:plugger:structure-overhaul-SNAPSHOT")

    implementation("androidx.appcompat:appcompat:1.6.1")

    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.paging:paging-common-ktx:3.2.1")
    implementation("androidx.paging:paging-runtime-ktx:3.2.1")
    implementation("androidx.preference:preference-ktx:1.2.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")

    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.palette:palette-ktx:1.0.0")

    implementation("com.google.android.material:material:1.12.0")

    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-android-compiler:2.48")

    ksp("com.github.bumptech.glide:ksp:4.14.2")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("jp.wasabeef:glide-transformations:4.3.0")

    implementation("com.github.madrapps:pikolo:2.0.2")
    implementation("com.github.bosphere.android-fadingedgelayout:fadingedgelayout:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.3")
    implementation("me.zhanghai.android.fastscroll:library:1.3.0")
    implementation("com.flaviofaria:kenburnsview:1.0.7")

    implementation("com.github.Kyant0:taglib:1.0.0-alpha17")

    //TODO : use fetch instead of download manager
//    implementation("com.github.tonyofrancis.Fetch:xfetch2:3.1.6")

    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    testImplementation("junit:junit:4.13.2")
}
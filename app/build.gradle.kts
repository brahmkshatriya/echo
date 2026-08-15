plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)

    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeNative)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)

    alias(libs.plugins.androidKMPLibrary)
}

group = property("GROUP").toString() + ".app"
version = property("VERSION").toString()

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.addAll(
            listOf(
                "ExperimentalMaterial3Api",
                "ExperimentalMaterial3ExpressiveApi",
            ).map { "-opt-in=androidx.compose.material3.$it" }
        )
    }

    jvmToolchain(21)
    linuxX64()
    mingwX64()
    android {
        namespace = group.toString()
        compileSdk = 37
        androidResources.enable = true
    }
    sourceSets {
        commonMain.dependencies {
            api(libs.bundles.parsing)
            api(libs.ktor.client.core)

            api(libs.bundles.compose)
            api(libs.androidx.navigation)
            api(libs.bundles.lifecycle)

            api(libs.materialKolor)
            api(libs.bundles.landscapist)
            api(libs.hypnoticcanvas)
        }
        desktopNativeMain.dependencies {
            implementation(libs.skiko.native)
        }
        androidMain.dependencies {
            api(libs.ktor.client.okhttp)
            api(libs.androidx.activity.compose)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "echo.app.generated.resources"
    generateResClass = always
}
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

group = property("GROUP").toString()
version = property("VERSION").toString()

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(projects.client)
            implementation("org.jetbrains.skiko:skiko-wasm-js:0.152.0")
        }
    }
}

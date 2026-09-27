plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

group = property("GROUP").toString()
version = property("VERSION").toString()

kotlin {
    iosArm64().binaries.framework {
        baseName = "EchoApp"
        isStatic = true
    }

    sourceSets {
        iosArm64Main.dependencies {
            implementation(projects.client)
            implementation(libs.ui)
        }
    }
}

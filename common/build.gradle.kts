plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
    id("com.vanniktech.maven.publish") version "0.34.0"
    id("org.jetbrains.dokka") version "2.0.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(libs.bundles.kotlinx)
    api(libs.okhttp)
    api(libs.protobuf.java)
}

// build.gradle.kts

mavenPublishing {
    publishToMavenCentral(true)
    signAllPublications()

    coordinates("dev.brahmkshatriya.echo", "common", "1.0")

    pom {
        name = "Echo common library"
        description = "A common library for echo extensions."
        inceptionYear = "2025"
        url = "https://github.com/brahmkshatriya/echo"
        licenses {
            license {
                name = "Unabandon Public License"
                url = "https://github.com/brahmkshatriya/echo/blob/main/LICENSE.md"
                distribution = "https://github.com/brahmkshatriya/echo/blob/main/LICENSE.md"
            }
        }
        developers {
            developer {
                id = "brahmkshatriya"
                name = "Shivam"
                url = "https://github.com/brahmkshatriya/"
            }
        }
        scm {
            url = "https://github.com/brahmkshatriya/echo/"
            connection = "scm:git:git://github.com/brahmkshatriya/echo.git"
            developerConnection = "scm:git:ssh://git@github.com/brahmkshatriya/echo.git"
        }
    }
}

dokka {
    moduleName.set("common")
    moduleVersion.set("1.0")
    dokkaSourceSets.main {
        includes.from("README.md")
        sourceLink {
            localDirectory.set(file("src/main/java"))
            remoteUrl("https://github.com/brahmkshatriya/echo/tree/main/common/src/main/java")
            remoteLineSuffix.set("#L")
        }
    }
    pluginsConfiguration.html {
        customStyleSheets.from("styles.css")
        footerMessage.set("made by <a style=\"color: inherit; text-decoration: underline;\" href=\"https://github.com/brahmkshatriya\">@brahmkshatriya</a>")
    }
}
plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
    id("maven-publish")
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "dev.brahmkshatriya.echo"
            artifactId = "common"
            version = "1.0"

            from(components["java"])
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
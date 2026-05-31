plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false

    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false

    alias(libs.plugins.stabilityAnalyzer) apply false

    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKMPLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false

    alias(libs.plugins.kotlinJVM) apply false
}

val androidComposeSubstitutions = mapOf(
    "androidx.compose.material3:material3-android" to libs.compose.material3,
    "androidx.compose.foundation:foundation-android" to libs.compose.foundation,
)

subprojects {
    configurations.all {
        resolutionStrategy.dependencySubstitution {
            androidComposeSubstitutions.forEach { (requestedModule, dependency) ->
                val catalogDependency = dependency.get()
                val androidCoordinate = "${requireNotNull(catalogDependency.group)}:${catalogDependency.name}-android:${catalogDependency.versionConstraint.requiredVersion}"
                substitute(module(requestedModule)).using(module(androidCoordinate))
            }
        }
    }
}
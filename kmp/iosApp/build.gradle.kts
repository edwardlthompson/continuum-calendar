plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

val enableIos = (findProperty("continuum.enableIos") as String?) == "true"

kotlin {
    if (enableIos) {
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64(),
        ).forEach { target ->
            target.binaries.framework {
                baseName = "ContinuumIos"
                isStatic = true
            }
        }
    } else {
        // Placeholder JVM target so the module configures on Windows/Linux CI.
        jvm()
    }
    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":continuum-ui"))
            implementation(project(":continuum-data"))
            implementation(project(":continuum-platform"))
            implementation(project(":continuum-domain"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
        }
    }
}

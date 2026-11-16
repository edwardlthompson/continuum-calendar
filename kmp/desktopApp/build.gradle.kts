plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvm("desktop")
    applyDefaultHierarchyTemplate()

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":continuum-ui"))
                implementation(project(":continuum-data"))
                implementation(project(":continuum-platform"))
                implementation(project(":continuum-domain"))
                implementation(compose.desktop.currentOs)
                implementation(libs.sqldelight.sqlite)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.continuumcalendar.desktop.MainKt"
        nativeDistributions {
            packageName = "Continuum Calendar CMP"
            packageVersion = "0.1.0"
        }
    }
}

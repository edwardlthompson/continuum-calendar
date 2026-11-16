pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "continuum-kmp"

include(
    ":continuum-domain",
    ":continuum-platform",
    ":continuum-data",
    ":continuum-ui",
    ":androidApp",
    ":desktopApp",
    ":iosApp",
)

// Path contains spaces on some Windows checkouts — remap build/ like examples/android.
gradle.beforeProject {
    val rootPath = settings.rootDir.absolutePath
    if (' ' !in rootPath) return@beforeProject
    val base =
        System.getenv("CONTINUUM_KMP_BUILD_DIR")
            ?: "${System.getProperty("user.home")}/.cache/continuum-kmp-build"
    val leaf = path.replace(':', '_').ifEmpty { "root" }
    layout.buildDirectory.set(file("$base/$leaf"))
}

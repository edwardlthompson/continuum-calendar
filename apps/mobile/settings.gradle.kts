pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Continuum-patched Fossify commons (FakeVersionCheck accepts org.continuumcalendar.*)
        maven { setUrl(uri("${rootDir}/libs/m2")) }
        google()
        mavenCentral()
        // Fossify commons 6.1.6 is not on Maven Central; patch task rewrites it to 6.1.6-continuum in libs/m2.
        maven { setUrl("https://jitpack.io") }
        mavenLocal()
    }
}
include(":app")

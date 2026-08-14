import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

fun hasSigningVars(): Boolean {
    return providers.environmentVariable("SIGNING_KEY_ALIAS").orNull != null
            && providers.environmentVariable("SIGNING_KEY_PASSWORD").orNull != null
            && providers.environmentVariable("SIGNING_STORE_FILE").orNull != null
            && providers.environmentVariable("SIGNING_STORE_PASSWORD").orNull != null
}

/** Same GCP project Client ID as desktop so Drive App Data is a shared peer remote. */
fun Project.resolveContinuumGoogleClientId(): String {
    (findProperty("CONTINUUM_GOOGLE_CLIENT_ID") as String?)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    System.getenv("CONTINUUM_GOOGLE_CLIENT_ID")?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

    val localProps = rootProject.file("local.properties")
    if (localProps.exists()) {
        val props = Properties()
        FileInputStream(localProps).use { props.load(it) }
        props.getProperty("continuum.google.client.id")?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    }

    return resolveDesktopEnvValue("VITE_GOOGLE_CLIENT_ID")
}

fun Project.isContinuumPublicRelease(): Boolean {
    val include = (findProperty("CONTINUUM_INCLUDE_CLIENT_SECRET") as String?)?.trim()
        ?: System.getenv("CONTINUUM_INCLUDE_CLIENT_SECRET")?.trim()
    if (include.equals("true", ignoreCase = true)) return false
    val tasks = gradle.startParameter.taskNames.joinToString(" ").lowercase()
    if (tasks.contains("release")) return true
    val prop = (findProperty("CONTINUUM_PUBLIC_RELEASE") as String?)?.trim()
    val env = System.getenv("CONTINUUM_PUBLIC_RELEASE")?.trim()
    return prop.equals("true", ignoreCase = true) || env.equals("true", ignoreCase = true)
}

/**
 * Desktop OAuth clients require client_secret on the token endpoint.
 * Keep this in gitignored local.properties / desktop .env only (debug builds).
 * Release tasks omit the secret unless `-PCONTINUUM_INCLUDE_CLIENT_SECRET=true`.
 */
fun Project.resolveContinuumGoogleClientSecret(): String {
    if (isContinuumPublicRelease()) return ""
    (findProperty("CONTINUUM_GOOGLE_CLIENT_SECRET") as String?)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    System.getenv("CONTINUUM_GOOGLE_CLIENT_SECRET")?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

    val localProps = rootProject.file("local.properties")
    if (localProps.exists()) {
        val props = Properties()
        FileInputStream(localProps).use { props.load(it) }
        props.getProperty("continuum.google.client.secret")?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    }

    return resolveDesktopEnvValue("VITE_GOOGLE_CLIENT_SECRET")
}

/** Android-type OAuth client (same GCP project) for Custom Tabs peer sync. */
fun Project.resolveContinuumGoogleAndroidClientId(): String {
    (findProperty("CONTINUUM_GOOGLE_ANDROID_CLIENT_ID") as String?)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    System.getenv("CONTINUUM_GOOGLE_ANDROID_CLIENT_ID")?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    val localProps = rootProject.file("local.properties")
    if (localProps.exists()) {
        val props = Properties()
        FileInputStream(localProps).use { props.load(it) }
        props.getProperty("continuum.google.android.client.id")?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    }
    return ""
}

fun Project.resolveDesktopEnvValue(key: String): String {
    val desktopEnv = rootProject.rootDir.resolve("../desktop/.env")
    val desktopEnvAlt = rootProject.rootDir.parentFile?.resolve("desktop/.env")
    val envFile = listOf(desktopEnv, desktopEnvAlt).firstOrNull { it != null && it.exists() } ?: return ""
    envFile.readLines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("$key=")) {
            val value = trimmed.substringAfter("=").trim()
            if (value.isNotEmpty()) return value
        }
    }
    return ""
}

base {
    val versionCode = project.property("VERSION_CODE").toString().toInt()
    archivesName = "calendar-$versionCode"
}

android {
    compileSdk = project.libs.versions.app.build.compileSDKVersion.get().toInt()

    defaultConfig {
        applicationId = project.property("APP_ID").toString()
        minSdk = project.libs.versions.app.build.minimumSDK.get().toInt()
        targetSdk = project.libs.versions.app.build.targetSDK.get().toInt()
        versionCode = project.property("VERSION_CODE").toString().toInt()
        versionName = project.property("VERSION_NAME").toString()
        vectorDrawables.useSupportLibrary = true
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
        // Peer settings sync with desktop requires the same Google Cloud project Client ID.
        // Resolution order: -P / env → local.properties → apps/desktop/.env (local dev).
        val continuumClientId = project.resolveContinuumGoogleClientId()
        val continuumAndroidClientId = project.resolveContinuumGoogleAndroidClientId()
        val continuumClientSecret = project.resolveContinuumGoogleClientSecret()
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "CONTINUUM_GOOGLE_CLIENT_ID", "\"$continuumClientId\"")
        buildConfigField("String", "CONTINUUM_GOOGLE_ANDROID_CLIENT_ID", "\"$continuumAndroidClientId\"")
        // Needed when using a Desktop OAuth client for Android browser/PKCE token exchange.
        buildConfigField("String", "CONTINUUM_GOOGLE_CLIENT_SECRET", "\"$continuumClientSecret\"")
        // Custom Tabs redirect scheme: com.googleusercontent.apps.<android-client-prefix>
        val authScheme = if (continuumAndroidClientId.contains(".apps.googleusercontent.com")) {
            "com.googleusercontent.apps." + continuumAndroidClientId.substringBefore(".apps.googleusercontent.com")
        } else {
            "${project.property("APP_ID")}.debug"
        }
        manifestPlaceholders["appAuthRedirectScheme"] = authScheme
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            register("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        } else if (hasSigningVars()) {
            register("release") {
                keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").get()
                storeFile = file(providers.environmentVariable("SIGNING_STORE_FILE").get())
                storePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").get()
            }
        } else {
            logger.warn("Warning: No signing config found. Build will be unsigned.")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists() || hasSigningVars()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    flavorDimensions.add("variants")
    productFlavors {
        register("core")
        register("foss")
        register("gplay")
    }

    sourceSets {
        getByName("main").java.directories.add("src/main/kotlin")
    }

    compileOptions {
        val currentJavaVersionFromLibs = JavaVersion.valueOf(libs.versions.app.build.javaVersion.get().toString())
        sourceCompatibility = currentJavaVersionFromLibs
        targetCompatibility = currentJavaVersionFromLibs
    }

    dependenciesInfo {
        includeInApk = false
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }

    tasks.withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(
            JvmTarget.fromTarget(project.libs.versions.app.build.kotlinJVMTarget.get())
        )
    }

    // applicationId = Continuum; namespace stays on fork package so R/imports keep working
    namespace = project.property("SOURCE_NAMESPACE").toString()

    lint {
        checkReleaseBuilds = false
        abortOnError = true
        warningsAsErrors = false
        baseline = file("lint-baseline.xml")
        lintConfig = rootProject.file("lint.xml")
    }

    bundle {
        language {
            enableSplit = false
        }
    }
}

detekt {
    baseline = file("detekt-baseline.xml")
    config.setFrom("$rootDir/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false
}

// Resolve stock commons into Gradle cache, then patch FakeVersionCheck for Continuum package id.
val commonsUpstream: Configuration by configurations.creating
dependencies {
    commonsUpstream("org.fossify:commons:6.1.6")
}

val continuumRepoRoot: File =
    rootProject.projectDir.parentFile?.parentFile
        ?: error("Expected apps/mobile nested under Continuum Calendar repo root")

val patchFossifyCommons by tasks.registering(Exec::class) {
    group = "continuum"
    description = "Patch Fossify commons FakeVersionCheck for org.continuumcalendar.*"
    workingDir = continuumRepoRoot
    commandLine("python", "scripts/patch-fossify-commons-fake-version.py")
    dependsOn(tasks.named("resolveCommonsUpstream"))
}

tasks.register("resolveCommonsUpstream") {
    doLast {
        commonsUpstream.resolve()
    }
}

tasks.matching { it.name.startsWith("preBuild") || it.name == "preBuild" }.configureEach {
    dependsOn(patchFossifyCommons)
}

dependencies {
    implementation(libs.fossify.commons)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.print)
    implementation(libs.bundles.room)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.browser)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.ext.truth)
    ksp(libs.androidx.room.compiler)
    detektPlugins(libs.compose.detekt)
}

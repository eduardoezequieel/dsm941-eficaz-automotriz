import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Applied below only when app/google-services.json exists (see the comment).
    alias(libs.plugins.google.services) apply false
}

// google-services.json is never committed (Firebase project credentials). Without the file
// the plugin would fail the build, so it is applied only when present; when missing, the APK
// still builds but Firebase is not initialized. Template: google-services.json.example
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
} else {
    logger.warn(
        "WARNING: app/google-services.json is missing; building without the Google Services plugin " +
            "and Firebase will not work at runtime. Download it from the Firebase console " +
            "(see app/google-services.json.example)."
    )
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties"); if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.eficazautomotriz.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.eficazautomotriz.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "3.0.0"
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${localProps["cloudinary.cloudName"] ?: ""}\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${localProps["cloudinary.uploadPreset"] ?: ""}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProps["google.webClientId"] ?: ""}\"")
    }
    buildTypes {
        // Offline demo with the mockup data (no Firebase). Opt-in per machine through
        // local.properties (`demo.inMemory=true`) and only for debug: release is always false.
        debug {
            val demo = localProps["demo.inMemory"]?.toString()?.trim()?.toBoolean() ?: false
            buildConfigField("boolean", "DEMO_IN_MEMORY", demo.toString())
        }
        release {
            buildConfigField("boolean", "DEMO_IN_MEMORY", "false")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui); implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview); debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.material.icons)
    implementation(libs.lifecycle.viewmodel.compose); implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.coroutines.android); implementation(libs.coroutines.play.services)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth); implementation(libs.firebase.firestore)
    implementation(libs.play.services.auth)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    implementation(libs.androidx.exifinterface)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)

    // JVM screenshot harness (ui/shots, opt-in with -Pshots): Robolectric + Compose UI test.
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage)
    debugImplementation(libs.compose.ui.test.manifest)
}

android.testOptions.unitTests.isIncludeAndroidResources = true

// The screenshot harness renders whole screens with Robolectric (slow), so it only runs with
// `./gradlew :app:testDebugUnitTest -Pshots --tests '*shots*'`; PNGs land in app/build/shots.
val runShots = project.hasProperty("shots")
tasks.withType<Test> {
    useJUnitPlatform()
    if (runShots) {
        systemProperty("shots.dir", layout.buildDirectory.dir("shots").get().asFile.absolutePath)
        maxHeapSize = "2g"
    } else {
        exclude("**/shots/**")
    }
}

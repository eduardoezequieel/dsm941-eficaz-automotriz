plugins { alias(libs.plugins.kotlin.jvm) }
group = "com.eficazautomotriz"; version = "3.0.0"
kotlin { jvmToolchain(17) }
dependencies {
    testImplementation(kotlin("test"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}
tasks.test { useJUnitPlatform(); testLogging { events("passed", "skipped", "failed") } }

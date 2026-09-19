// El plugin de Foojay resuelve y descarga el JDK que pide `jvmToolchain(17)` cuando la
// maquina no lo tiene instalado: `./gradlew run` funciona sin preparar nada antes.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "eficaz-automotriz"

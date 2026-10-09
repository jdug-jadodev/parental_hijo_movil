plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-test-fixtures`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Los ejemplos congelados solo se empaquetan como recursos de pruebas.
sourceSets.named("testFixtures") {
    resources.srcDir("../../compartido/ejemplos")
    resources.srcDir("../../pruebas")
    resources.include("*.json")
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.serialization.json)
}

tasks.named<Test>("test") {
    outputs.dir(layout.buildDirectory.dir("interoperabilidad"))
}

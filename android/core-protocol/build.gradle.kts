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
    resources.include("*.json")
}

dependencies {
    testImplementation(libs.junit)
}

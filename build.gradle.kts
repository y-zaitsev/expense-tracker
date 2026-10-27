import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    runtimeOnly("org.slf4j:slf4j-nop:2.0.16") // silences SLF4J startup warnings

    testImplementation(kotlin("test"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

application {
    mainClass.set("com.example.expensetracker.MainKt")
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

tasks.test {
    useJUnitPlatform()
}

// Lets `./gradlew run` read from the keyboard.
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
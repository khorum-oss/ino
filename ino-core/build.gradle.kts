import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// ino-core — the Spring Boot engine. Hosts persistence, SPI interfaces,
// registries (ProviderRegistry, ToolRegistry), the AgentExecutor loop, and
// the REST + SSE API. Provider/tool implementations ship as separate JARs
// loaded at runtime via ServiceLoader (see architecture.md).
//
// Pinned to Kotlin 2.3.0 + Spring Boot 4.1.0-M1 to match spektr's baseline.
// ino-dsl is consumed as a project dependency; its Kotlin 2.1.20 generated
// code is binary-compatible with this module's Kotlin 2.3.

plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    id("org.springframework.boot") version "4.1.0-M1"
    id("io.spring.dependency-management") version "1.1.7"
}

version = file("VERSION").readText().trim()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://open-reliquary.nyc3.digitaloceanspaces.com") }
}

dependencies {
    implementation(project(":ino-dsl"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")

    // Database — SQLite + Liquibase
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    implementation("org.liquibase:liquibase-core")

    // Jackson (newer 3.x coords, matching spektr)
    implementation("tools.jackson.module:jackson-module-kotlin")

    // Kotlin / coroutines
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Logging
    implementation("io.github.microutils:kotlin-logging:4.0.0-beta-2")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("io.mockk:mockk:1.13.13")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.compilerOptions {
    // Kotlin 2.2+ flag — places Spring annotations on both the JVM param and
    // the property, which Spring's reflection-based introspection needs.
    freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
}

tasks.test {
    useJUnitPlatform()
}

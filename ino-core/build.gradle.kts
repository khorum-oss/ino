import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// ino-core — the Spring Boot engine. Hosts persistence, REST/SSE API, and
// the Koog-driven agent execution. Konstellation DSL declarations translate
// to Koog AIAgent.builder() at construction time; Koog handles the LLM loop,
// tool dispatch, and streaming.
//
// Spring Boot 4.1.0-M1 + Kotlin 2.3.10 (Koog requires 2.3.10+; SB starter is
// Spring Boot 3-only, so we wire koog-agents-jvm as plain beans ourselves
// — same pattern we used for Liquibase autoconfig).

plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.spring") version "2.3.10"
    id("org.springframework.boot") version "4.1.0-M1"
    id("io.spring.dependency-management") version "1.1.7"
    // Kover for test coverage. Detekt is intentionally NOT applied here —
    // Detekt 1.23.x is incompatible with Kotlin 2.3.x at the Gradle-extension
    // level, and Detekt 2.x isn't released yet. Re-add once Detekt 2.0 ships.
    id("org.jetbrains.kotlinx.kover") version "0.9.4"
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

    // Koog — agent engine. Single meta artifact at 1.0.0 (matches sandbox).
    // The 1.x line consolidates the 0.x prompt-executor-* clients into one.
    // Note: SB starter requires Spring Boot 3, so we wire Koog as plain beans.
    implementation("ai.koog:koog-agents:1.0.0")

    // Jackson (newer 3.x coords, matching spektr)
    implementation("tools.jackson.module:jackson-module-kotlin")

    // Kotlin / coroutines
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Logging
    implementation("io.github.microutils:kotlin-logging:4.0.0-beta-2")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation(project(":ino-test"))
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

// ---------------------------------------------------------------------------
// Dashboard integration
// ---------------------------------------------------------------------------
// `npm run build` in ino-dashboard produces a static SvelteKit app at
// ino-dashboard/build/. We copy it into Spring Boot's static resources at
// build/resources/main/static/dashboard/, so Spring serves it at /dashboard/**
// automatically once you run `./gradlew :ino-core:bootRun`.
//
// The dashboard build is skipped if `npm install` hasn't been run — that way
// `./gradlew :ino-core:bootRun` still works without a Node setup; it just
// serves no static assets. Run `npm install` in ino-dashboard once to enable.

val dashboardDir = rootProject.layout.projectDirectory.dir("ino-dashboard")
val dashboardBuildOutput = dashboardDir.dir("build")
val dashboardStaticTarget = layout.buildDirectory.dir("resources/main/static/dashboard")

val buildDashboard by tasks.registering(Exec::class) {
    description = "Builds the SvelteKit dashboard via npm (run npm install first)."
    group = "build"
    workingDir = dashboardDir.asFile
    val npmCmd = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) "npm.cmd" else "npm"
    commandLine(npmCmd, "run", "build")
    environment("DASHBOARD_BASE", "/dashboard")
    // Incremental: rebuild only when sources change.
    inputs.dir(dashboardDir.dir("src"))
    inputs.file(dashboardDir.file("package.json"))
    inputs.file(dashboardDir.file("svelte.config.js"))
    inputs.file(dashboardDir.file("vite.config.ts"))
    outputs.dir(dashboardBuildOutput)
    onlyIf {
        val nodeModules = dashboardDir.dir("node_modules").asFile
        val present = nodeModules.exists()
        if (!present) {
            logger.lifecycle(
                "Skipping :ino-core:buildDashboard — run `npm install` in ino-dashboard to enable embedded UI.",
            )
        }
        present
    }
}

val copyDashboard by tasks.registering(Copy::class) {
    description = "Copies the built dashboard into ino-core static resources."
    group = "build"
    dependsOn(buildDashboard)
    from(dashboardBuildOutput)
    into(dashboardStaticTarget)
}

tasks.processResources {
    dependsOn(copyDashboard)
}

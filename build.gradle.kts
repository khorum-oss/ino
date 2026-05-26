// Root project for the `ino` agent framework.
//
// Each submodule declares its own `kotlin("jvm")` plugin version so we can mix
// Kotlin versions across modules during the konstellation 2.1.20 → 2.3.0 transition
// (ino-dsl is pinned to konstellation's Kotlin; ino-core uses 2.3 for Spring Boot 4.1).
//
// This file applies cross-cutting concerns to every submodule: Detekt for static
// analysis, Kover for coverage, and SonarQube for quality gates. These mirror
// the spektr root build so the same `violabs/public-cicd` workflows work here.

plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jetbrains.kotlinx.kover") version "0.9.4"
    id("org.sonarqube") version "7.0.0.6105"
}

group = "org.khorum.oss.ino"
version = file("VERSION").readText().trim()

// Root project needs its own repositories so Kover's `koverExternalArtifacts`
// configuration can resolve the Kotlin stdlib it tracks coverage against.
// (The `subprojects { repositories { ... } }` block below only configures
// the children's classpath.)
repositories {
    mavenCentral()
    // Kotlin 2.3.x is still in Spring's milestone repo at the time of writing.
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://open-reliquary.nyc3.digitaloceanspaces.com") }
}

subprojects {
    group = rootProject.group
    version = file("VERSION").readText().trim()

    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://open-reliquary.nyc3.digitaloceanspaces.com") }
    }
}

// Detekt + Kover are applied at the root, NOT via `subprojects { apply(...) }`,
// because those plugins require the Kotlin Gradle plugin to be present in the
// same classpath. ino-dsl is on Kotlin 2.1.20 while ino-core is on 2.3.10, so
// applying Kotlin uniformly at the root isn't safe. The root tasks still walk
// every submodule's source sets via Detekt's source-set discovery, and Kover
// aggregates submodule coverage when `kover { aggregate { ... } }` is set.

dependencies {
    // Detekt source-set discovery follows project dependencies.
    detekt(project(":ino-dsl"))
    detekt(project(":ino-core"))
    detekt(project(":ino-test"))

    // Kover aggregates coverage from each test-producing module.
    kover(project(":ino-core"))
    kover(project(":ino-test"))
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    // Default Detekt ruleset until we commit a project-specific config.
    // Point at `$rootDir/detekt.yml` once a tuned config exists.
    baseline = file("$rootDir/detekt-baseline.xml").takeIf { it.exists() }
    parallel = true
}

sonar {
    properties {
        property("sonar.projectKey", "khorum-oss_ino")
        property("sonar.organization", "khorum-oss")
        property("sonar.host.url", "https://sonarcloud.io")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${layout.buildDirectory.get()}/reports/kover/report.xml",
        )
    }
}

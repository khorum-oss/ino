// ino-test — shared test fixtures consumed by ino-core (and later by every
// extension JAR's integration tests). Houses InoHomeExtension (per-test
// tempdir + ino.home isolation), FixedClockConfig (deterministic timestamps),
// and a credential scrubber so provider tests can never accidentally leak to
// a live API.
//
// Kotlin 2.3.0 matches ino-core; types declared here are imported as
// testImplementation by every downstream module.
//
// InMemoryLlmProvider lives here once the LlmProvider SPI lands in
// ino-core (Step 3 in docs/roadmap.md).

plugins {
    kotlin("jvm") version "2.3.0"
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
    maven { url = uri("https://open-reliquary.nyc3.digitaloceanspaces.com") }
}

dependencies {
    implementation(kotlin("stdlib"))

    // JUnit 5 surface — InoHomeExtension implements BeforeEachCallback / AfterEachCallback.
    api("org.junit.jupiter:junit-jupiter-api:5.11.3")

    // Spring test surface — FixedClockConfig is a @TestConfiguration consumed by slice tests.
    api("org.springframework:spring-context:7.0.0-M5")
    api("org.springframework.boot:spring-boot-test:4.1.0-M1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

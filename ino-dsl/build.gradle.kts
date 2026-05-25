// ino-dsl — declarative types for agents, tools, and providers.
//
// Pinned to Kotlin 2.1.20 + KSP 2.1.20-1.0.32 to match konstellation-dsl, which is
// the KSP processor that generates our DSL builders.
//
// When konstellation publishes a Kotlin 2.3.x build (or we bump it ourselves),
// flip this whole module to 2.3.0 + matching KSP. Generated code is binary-compatible
// either way, so downstream modules on Kotlin 2.3 can still consume what we produce here.

plugins {
    `java-library`                       // enables `api(...)` configuration
    kotlin("jvm") version "2.1.20"
    id("com.google.devtools.ksp") version "2.1.20-1.0.32"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Konstellation: annotations at compile time, processor for KSP.
    // Exposed as `api` because konstellation-generated builders extend
    // CoreDslBuilder from meta-dsl — consumers (ino-core, etc.) need it.
    api("org.khorum.oss.konstellation:konstellation-meta-dsl:1.0.15")
    ksp("org.khorum.oss.konstellation:konstellation-dsl:2.0.14")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

ksp {
    // Where konstellation looks for @GeneratedDsl-annotated source classes.
    arg("projectRootClasspath", "org.khorum.oss.ino.dsl")
    // Package containing the @DslMarker class and the DslBuilder<T> interface.
    arg("dslBuilderClasspath", "org.khorum.oss.ino.dsl")
    // The @DslMarker konstellation will stamp on every generated builder.
    arg("dslMarkerClass", "org.khorum.oss.ino.dsl.InoDsl")
}

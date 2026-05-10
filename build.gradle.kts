// Root project for the `ino` agent framework.
//
// Each submodule declares its own `kotlin("jvm")` plugin version so we can mix
// Kotlin versions across modules during the konstellation 2.1.20 → 2.3.0 transition
// (ino-dsl is pinned to konstellation's Kotlin; ino-core will be on 2.3 for Spring Boot 4.1).
//
// Shared concerns (group, repositories, java toolchain default) live here.

group = "org.khorum.oss.ino"
version = file("VERSION").readText().trim()

subprojects {
    group = rootProject.group
    version = file("VERSION").readText().trim()

    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://open-reliquary.nyc3.digitaloceanspaces.com") }
    }
}

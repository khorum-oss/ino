pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven {
            url = uri("https://open-reliquary.nyc3.digitaloceanspaces.com")
        }
    }
}

rootProject.name = "ino"

include(":ino-dsl", ":ino-core", ":ino-test")
// Phase-1 modules added as they come online:
// include(":ino-providers-anthropic", ":ino-providers-openai", ":ino-providers-ollama")
// include(":ino-tools-builtin")

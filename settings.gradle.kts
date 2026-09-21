pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.8"
}

stonecutter {
    create(rootProject) {
        // Each node is "<mcVersion>-<loader>"; the buildscript is picked per loader.
        fun match(mc: String, vararg loaders: String) = loaders.forEach {
            version("$mc-$it", mc).buildscript = "build.$it.gradle.kts"
        }

        // Start narrow and verified; further versions are added once this one is green.
        // Loader axis is NeoForge only: ForgeGradle 6.0.54 rejects Gradle 9+, which
        // Stonecutter 0.9.8 requires, so MinecraftForge cannot share this build.
        match("1.21.1", "neoforge")

        vcsVersion = "1.21.1-neoforge"
    }
}

rootProject.name = "Mantle"

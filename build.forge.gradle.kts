import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("eclipse")
    id("idea")
    id("maven-publish")
    id("net.minecraftforge.gradle")
}

val mcVersion: String = stonecutter.current.version
val loader: String = stonecutter.current.project.substringAfterLast('-')

repositories {
    mavenCentral()
    maven("https://dvs1.progwml6.com/files/maven") { name = "Prog's Maven" }
    maven("https://maven.blamejared.com/") { name = "Jared's maven" }
    maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
}

group = "slimeknights.mantle"
base.archivesName = "Mantle"

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

minecraft {
    mappings("official", mcVersion)
    val at = rootProject.file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) accessTransformer(at)
}

dependencies {
    "minecraft"("net.minecraftforge:forge:$mcVersion-${property("deps.forge")}")
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.isFork = true
    options.forkOptions.jvmArgs = (options.forkOptions.jvmArgs ?: mutableListOf()) + listOf(
        "jdk.compiler/com.sun.tools.javac.code",
        "jdk.compiler/com.sun.tools.javac.comp",
        "jdk.compiler/com.sun.tools.javac.file",
        "jdk.compiler/com.sun.tools.javac.main",
        "jdk.compiler/com.sun.tools.javac.model",
        "jdk.compiler/com.sun.tools.javac.parser",
        "jdk.compiler/com.sun.tools.javac.processing",
        "jdk.compiler/com.sun.tools.javac.tree",
        "jdk.compiler/com.sun.tools.javac.util",
        "jdk.compiler/com.sun.tools.javac.jvm"
    ).map { "--add-opens=$it=ALL-UNNAMED" }
}

fun gitRevision(): String = try {
    providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        workingDir = rootDir
    }.standardOutput.asText.get().trim().ifEmpty { "GITBORK" }
} catch (_: Exception) { "gitgud" }

val buildNum: String = System.getenv("BUILD_NUMBER") ?: "DEV.${gitRevision()}"
val artifactVersion: String = System.getenv("ARTIFACT_VERSION") ?: "${property("mod_version")}.$buildNum"
version = "$mcVersion-$artifactVersion"

println("Mantle | MC $mcVersion | loader $loader | version $version")

tasks.named("compileJava") {
    dependsOn(tasks.named("stonecutterGenerate"))
}

tasks.named<ProcessResources>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val replaceProperties = mapOf(
        "version" to artifactVersion,
        "minecraft_range" to property("minecraft_range"),
        "forge_range" to property("forge_range"),
        "loader_range" to property("loader_range")
    )
    inputs.properties(replaceProperties)
    filesMatching(listOf("META-INF/mods.toml")) { expand(replaceProperties) }
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude(".cache/**")
    exclude("**/*.md")
    manifest {
        attributes(
            "Specification-Title" to "Mantle",
            "Specification-Vendor" to "Slime Knights",
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to version,
            "Implementation-Vendor" to "Slime Knights",
            "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date())
        )
    }
}

sourceSets.named("main") {
    resources.srcDir(rootProject.file("src/generated/resources"))
    resources.exclude(".cache")
}

java { withSourcesJar() }

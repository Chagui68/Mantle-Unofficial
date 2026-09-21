import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("eclipse")
    id("idea")
    id("maven-publish")
    id("net.neoforged.moddev")
}

val mcVersion: String = stonecutter.current.version
val loader: String = stonecutter.current.project.substringAfterLast('-')

repositories {
    mavenCentral()
    maven("https://dvs1.progwml6.com/files/maven") { name = "Prog's Maven" }
    maven("https://maven.blamejared.com/") { name = "Jared's maven" }
    maven("https://maven.neoforged.net/releases") { name = "NeoForged" }
}

group = "slimeknights.mantle"
base.archivesName = "Mantle"

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    testCompileOnly("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.34")
    findProperty("deps.jei")?.let { compileOnly("mezz.jei:jei-$mcVersion-neoforge-api:$it") }
}

// Lombok needs these opens on JDK 21.
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

// ---- version metadata ----
fun gitRevision(): String = try {
    providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        workingDir = rootDir
    }.standardOutput.asText.get().trim().ifEmpty { "GITBORK" }
} catch (_: Exception) { "gitgud" }

val buildNum: String = System.getenv("BUILD_NUMBER") ?: "DEV.${gitRevision()}"
val artifactVersion: String = System.getenv("ARTIFACT_VERSION") ?: "${property("mod_version")}.$buildNum"
version = "$mcVersion-$artifactVersion"

println("Mantle | MC $mcVersion | loader $loader | version $version | JVM ${System.getProperty("java.version")}")

neoForge {
    version = property("deps.neoforge") as String

    // Mantle's access transformer. Required for the GUI/model internals it touches.
    val at = rootProject.file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at)
        validateAccessTransformers = false
    }

    runs {
        register("client") { client() }
        register("server") { server() }
        register("data") {
            data()
            programArguments.addAll(
                "--mod", "mantle", "--all",
                "--output", rootProject.file("src/generated/resources/").absolutePath,
                "--existing", rootProject.file("src/main/resources/").absolutePath
            )
        }
    }

    mods {
        register("mantle") { sourceSet(sourceSets["main"]) }
    }
}

tasks.named("createMinecraftArtifacts") {
    dependsOn(tasks.named("stonecutterGenerate"))
}

tasks.named<ProcessResources>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val replaceProperties = mapOf(
        "version" to artifactVersion,
        "minecraft_range" to property("minecraft_range"),
        "neo_version_range" to property("neo_version_range")
    )
    inputs.properties(replaceProperties)
    filesMatching(listOf("META-INF/neoforge.mods.toml")) { expand(replaceProperties) }
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

java { withSourcesJar() }

publishing {
    publications {
        register<MavenPublication>("mavenJava") { from(components["java"]) }
    }
}

// Datagen output lives at the repository root, shared across version nodes.
sourceSets.named("main") {
    resources.srcDir(rootProject.file("src/generated/resources"))
    resources.exclude(".cache")
}

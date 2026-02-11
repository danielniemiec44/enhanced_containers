import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import java.security.MessageDigest

plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Dev bundle zawiera Paper API + CraftBukkit + NMS (Mojang mappings)
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Trzymamy kod NMS w osobnym katalogu, żeby Maven nadal mógł budować projekt bez dev bundle.
sourceSets {
    val main by getting {
        java.srcDirs("src/main/java", "src/nms/java")
    }

    create("integrationTest") {
        java.srcDir("integration-tests/src/test/java")
        resources.srcDir("integration-tests/src/test/resources")

        // Make integration tests see the plugin code (including compiled NMS classes).
        // Gradle kompiluje NMS z paperweight dev-bundle, więc klasy są dostępne w main.output.
        compileClasspath += main.output
        runtimeClasspath += main.output
    }
}

// Integration tests (MockBukkit) live under integration-tests/src/test/java.
// We run them as a separate Gradle test task to keep unit tests fast.
dependencies {
    add("integrationTestImplementation", platform("org.junit:junit-bom:5.10.2"))
    add("integrationTestImplementation", "org.junit.jupiter:junit-jupiter")
    add("integrationTestRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    add("integrationTestImplementation", "org.mockito:mockito-core:5.12.0")
    add("integrationTestImplementation", "org.mockito:mockito-junit-jupiter:5.12.0")
    add("integrationTestImplementation", "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    add("integrationTestImplementation", "org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.101.0") {
        exclude(group = "io.papermc.paper", module = "paper-api")
        exclude(group = "org.junit.jupiter")
        exclude(group = "org.junit.platform")
    }
    // Plugin używa Netty do obsługi NMS packet handlers (RecipeBookPacketHandler).
    // MockBukkit nie dostarcza Netty, więc dodajemy explicite dla testów integracyjnych.
    add("integrationTestImplementation", "io.netty:netty-all:4.1.97.Final")
}

// Artefakt produkcyjny: Spigot mappings (kompatybilność z serwerami produkcyjnymi).
// Na Paper 1.20.5+ taki plugin zostanie automatycznie zdeobfuskowany przy pierwszym starcie.
paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.REOBF_PRODUCTION

tasks.test {
    // Unit tests: lekkie testy logiki (bez pełnego środowiska Bukkit).
    enabled = true

    useJUnitPlatform {
        // Tak samo jak w Maven (Surefire): integracyjne są w osobnym kroku.
        excludeTags("integration")
    }
    
    // TYLKO test FurnaceTypeRecipeRestrictionsTest działa bez środowiska Bukkit
    // Wszystkie inne testy wymagają MockBukkit (integracyjne) lub Paper NMS
    filter {
        includeTestsMatching("*FurnaceTypeRecipeRestrictionsTest*")
    }
    
    // Mockito/ByteBuddy na nowszych JDK potrafi wymagać tej flagi.
    jvmArgs("-Dnet.bytebuddy.experimental=true")
}

tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Runs integration tests (MockBukkit) from integration-tests/src/test/java."

    val integrationTestSourceSet = sourceSets["integrationTest"]
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath

    shouldRunAfter(tasks.test)
    useJUnitPlatform()
    jvmArgs("-Dnet.bytebuddy.experimental=true")
}

tasks.register("allTests") {
    group = "verification"
    description = "Runs Gradle unit tests + integrationTest in one command."
    dependsOn(tasks.test)
    dependsOn(tasks.named("integrationTest"))
}

// Domyślnie buduj też artefakt kompatybilny z serwerem (nie -dev).
tasks.assemble {
    dependsOn(tasks.named("reobfJar"))
}

// Wygodny skrót: odpal ciężkie testy NMS na prawdziwym Paper serwerze.
// Uruchamianie jest celowo ręczne (opty-in), bo pobiera jar Paper i odpala proces serwera.
tasks.register<Exec>("realPaperIT") {
    group = "verification"
    description = "Uruchamia opt-in testy integracyjne z prawdziwym Paper serwerem (NMS assemble())."

    workingDir = file("integration-tests")
    commandLine(
        "mvn",
        "-q",
        "-DrealPaperIT=true",
        "clean",
        "verify"
    )
}

// --- RESOURCE PACK GENERATION ---
tasks.register<Zip>("zipResourcePack") {
    group = "build"
    description = "Packs the resource pack assets into a zip file."
    
    archiveFileName.set("PlethorasExtendedTools.zip")
    destinationDirectory.set(file("resourcepack_out"))

    from("src/main/resources") {
        include("assets/**")
        include("pack.mcmeta")
    }

    // Always recreate the ZIP (no Gradle up-to-date skipping).
    outputs.upToDateWhen { false }
    
    doLast {
        val zipFile = archiveFile.get().asFile

        // If the pack is served via server.properties (resource-pack + resource-pack-sha1),
        // a mismatched SHA-1 will make clients refuse to apply the pack (all custom fonts disappear).
        val sha1 = MessageDigest.getInstance("SHA-1")
            .digest(zipFile.readBytes())
            .joinToString("") { "%02x".format(it) }

        val sha1File = destinationDirectory.get().file("${archiveFileName.get()}.sha1").asFile
        sha1File.writeText(sha1)

        println("Resource Pack created: ${zipFile.absolutePath}")
        println("Resource Pack SHA-1: $sha1")
        println("Wrote SHA-1 file: ${sha1File.absolutePath}")
    }
}

tasks.build {
    dependsOn("zipResourcePack")
}
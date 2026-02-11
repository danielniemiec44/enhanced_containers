import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import java.security.MessageDigest

plugins {
    java
    id("io.papermc.paperweight.userdev")
}

dependencies {
    // Dev bundle: Paper API + CraftBukkit + NMS (Mojang mappings)
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")

    // Wspólna biblioteka (WorldEdit integration, itp.)
    implementation(project(":common"))
}

// Kod NMS w osobnym katalogu
sourceSets {
    main {
        java.srcDirs("src/main/java", "src/nms/java")
    }
}

// Testy jednostkowe: Mockito only. Testy wymagające Bukkit runtime wykluczamy.
// Te testy uruchamiamy w integration-tests (MockBukkit).
tasks.test {
    filter {
        excludeTestsMatching("*EnhancedFurnaceLogicTest*")
        excludeTestsMatching("*FurnaceFuelProportionalityTest*")
        excludeTestsMatching("*FurnaceCommandTest*")
        excludeTestsMatching("*ShiftClickPriorityUnitTest*")
    }
}

// Dołącz klasy z common do JAR-a pluginu
tasks.jar {
    from(project(":common").sourceSets.main.get().output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Produkcyjny artefakt (Spigot mappings)
paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.REOBF_PRODUCTION

tasks.assemble {
    dependsOn(tasks.named("reobfJar"))
}

// Testy integracyjne (MockBukkit) - osobny source set
sourceSets {
    create("integrationTest") {
        java.srcDir("src/integrationTest/java")
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

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
    add("integrationTestImplementation", "io.netty:netty-all:4.1.97.Final")
}

tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Testy integracyjne EnhancedFurnaces (MockBukkit)."

    val integrationTestSourceSet = sourceSets["integrationTest"]
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath

    shouldRunAfter(tasks.test)
    useJUnitPlatform()
    jvmArgs("-Dnet.bytebuddy.experimental=true")
}

// --- RESOURCE PACK: EnhancedFurnaces ---
tasks.register<Zip>("zipResourcePack") {
    group = "build"
    description = "Buduje resource pack dla EnhancedFurnaces."

    archiveFileName.set("EnhancedFurnaces-ResourcePack.zip")
    destinationDirectory.set(file("${rootProject.projectDir}/resourcepack_out"))

    from("${rootProject.projectDir}/resourcepack") {
        include("pack.mcmeta")
        include("assets/enhanced_containers/models/item/fire_*.json")
        include("assets/enhanced_containers/textures/fire_*.png")
        include("assets/minecraft/textures/font/fire_*.png")
        include("assets/minecraft/font/**")
        include("assets/minecraft/textures/gui/plethora/furnace_*.png")
    }

    outputs.upToDateWhen { false }

    doLast {
        val zipFile = archiveFile.get().asFile
        val sha1 = MessageDigest.getInstance("SHA-1")
            .digest(zipFile.readBytes())
            .joinToString("") { "%02x".format(it) }
        val sha1File = destinationDirectory.get().file("${archiveFileName.get()}.sha1").asFile
        sha1File.writeText(sha1)
        println("EnhancedFurnaces Resource Pack: ${zipFile.absolutePath}")
        println("SHA-1: $sha1")
    }
}

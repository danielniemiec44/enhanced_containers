import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import java.security.MessageDigest

plugins {
    java
    id("io.papermc.paperweight.userdev")
}

dependencies {
    // Dev bundle: Paper API + CraftBukkit + NMS (Mojang mappings)
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")

    // Wspólna biblioteka (vanilla crafting, WorldEdit integration, itp.)
    implementation(project(":common"))

    // Netty - potrzebne do obsługi NMS packet handlers (RecipeBookPacketHandler)
    compileOnly("io.netty:netty-all:4.1.97.Final")
}

// Kod NMS w osobnym katalogu
sourceSets {
    main {
        java.srcDirs("src/main/java", "src/nms/java")
    }
}

// Dołącz klasy z common do JAR-a pluginu
tasks.jar {
    from(project(":common").sourceSets.main.get().output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Testy jednostkowe: większość testów SharedCrafting wymaga Bukkit runtime (ItemStack, Inventory).
// Te testy uruchamiamy w integration-tests (MockBukkit).
tasks.test {
    filter {
        // Na razie brak testów czysto logicznych - wszystkie wymagają Bukkit
        excludeTestsMatching("*SharedCraftingRecipeBookTest*")
        excludeTestsMatching("*SharedCraftingListenerTest*")
        excludeTestsMatching("*SharedCraftingManagerTest*")
        excludeTestsMatching("*SharedCraftingCommandTest*")
        excludeTestsMatching("*RecipeBookVanillaBehaviorTest*")
        isFailOnNoMatchingTests = false
    }
}

// Produkcyjny artefakt (Spigot mappings)
paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.REOBF_PRODUCTION

tasks.assemble {
    dependsOn(tasks.named("reobfJar"))
}

// --- RESOURCE PACK: SharedCrafting ---
tasks.register<Zip>("zipResourcePack") {
    group = "build"
    description = "Buduje resource pack dla SharedCrafting."

    archiveFileName.set("SharedCrafting-ResourcePack.zip")
    destinationDirectory.set(file("${rootProject.projectDir}/resourcepack_out"))

    from("${rootProject.projectDir}/resourcepack") {
        include("pack.mcmeta")
        // SharedCrafting nie ma własnych assetów GUI - używa vanilla crafting table
    }

    outputs.upToDateWhen { false }

    doLast {
        val zipFile = archiveFile.get().asFile
        val sha1 = MessageDigest.getInstance("SHA-1")
            .digest(zipFile.readBytes())
            .joinToString("") { "%02x".format(it) }
        val sha1File = destinationDirectory.get().file("${archiveFileName.get()}.sha1").asFile
        sha1File.writeText(sha1)
        println("SharedCrafting Resource Pack: ${zipFile.absolutePath}")
        println("SHA-1: $sha1")
    }
}

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

// Testy jednostkowe: Mockito only. Testy wymagające Bukkit runtime (ItemStack) wykluczamy.
// Te testy uruchamiamy w integration-tests (MockBukkit).
tasks.test {
    filter {
        excludeTestsMatching("*CraftingGUICursorStackingUnitTest*")
        excludeTestsMatching("*CraftingGUIListenerEventTest*")
        excludeTestsMatching("*NmsCraftingEngineTest*")
        excludeTestsMatching("*InputItemStackNormalizerUnitTest*")
        excludeTestsMatching("*RecipeMatcherTest*")
        excludeTestsMatching("*RecipeMatcherUnitTest*")
        excludeTestsMatching("*CraftingAutofillPlannerUnitTest*")
        excludeTestsMatching("*RecipeSuggestionEngineUnitTest*")
    }
}

// Produkcyjny artefakt (Spigot mappings)
paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.REOBF_PRODUCTION

tasks.assemble {
    dependsOn(tasks.named("reobfJar"))
}

// --- RESOURCE PACK: EnhancedCrafting ---
tasks.register<Zip>("zipResourcePack") {
    group = "build"
    description = "Buduje resource pack dla EnhancedCrafting (4x4 crafting GUI)."

    archiveFileName.set("EnhancedCrafting-ResourcePack.zip")
    destinationDirectory.set(file("${rootProject.projectDir}/resourcepack_out"))

    from("${rootProject.projectDir}/resourcepack") {
        include("pack.mcmeta")
        include("assets/enhanced_containers/models/item/arrow_*.json")
        include("assets/enhanced_containers/textures/arrow_*.png")
        include("assets/minecraft/textures/font/arrow_*.png")
        include("assets/minecraft/textures/gui/plethora/crafting_*.png")
        include("assets/minecraft/items/**")
        include("assets/minecraft/models/**")
    }

    outputs.upToDateWhen { false }

    doLast {
        val zipFile = archiveFile.get().asFile
        val sha1 = MessageDigest.getInstance("SHA-1")
            .digest(zipFile.readBytes())
            .joinToString("") { "%02x".format(it) }
        val sha1File = destinationDirectory.get().file("${archiveFileName.get()}.sha1").asFile
        sha1File.writeText(sha1)
        println("EnhancedCrafting Resource Pack: ${zipFile.absolutePath}")
        println("SHA-1: $sha1")
    }
}

plugins {
    `java-library`
    id("io.papermc.paperweight.userdev")
}

dependencies {
    // Dev bundle: Paper API + CraftBukkit + NMS (Mojang mappings)
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
}

// Kod NMS trzymamy w osobnym katalogu - tak jak było w monolicie
sourceSets {
    main {
        java.srcDirs("src/main/java", "src/nms/java")
    }
}

// Common to biblioteka - nie produkuje pluginowego JAR-a.
// Klasy z common są dołączane do JAR-ów poszczególnych pluginów.

// Testy jednostkowe: TYLKO testy czysto logiczne (bez Bukkit API).
// Testy wymagające ItemStack/Material uruchamiamy w integration-tests (MockBukkit).
tasks.test {
    filter {
        includeTestsMatching("*GridShrinkWrapperTest*")
    }
}

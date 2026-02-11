pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "enhanced_containers"

// Moduły projektu: wspólna biblioteka + 3 pluginy
include("common")
include("shared-crafting")
include("enhanced-crafting")
include("enhanced-furnaces")

// Testy integracyjne (MockBukkit) - osobny moduł
include("integration-tests")

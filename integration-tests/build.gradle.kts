// Moduł testów integracyjnych (MockBukkit).
// Testy integracyjne mają dostęp do wszystkich 3 pluginów.
// Uruchamianie: ./gradlew :integration-tests:test

plugins {
    java
}

// Nie uruchamiaj testów integracyjnych automatycznie w ./gradlew build.
// Uruchamiaj ręcznie: ./gradlew :integration-tests:test
tasks.test {
    onlyIf { project.hasProperty("runIntegrationTests") }
}

dependencies {
    // 3 pluginy - kompilujemy testy pod ich klasy
    testImplementation(project(":shared-crafting"))
    testImplementation(project(":enhanced-crafting"))
    testImplementation(project(":enhanced-furnaces"))
    testImplementation(project(":common"))

    // Paper API
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // MockBukkit - fake serwer do testów integracyjnych
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.101.0") {
        exclude(group = "io.papermc.paper", module = "paper-api")
        exclude(group = "org.junit.jupiter")
        exclude(group = "org.junit.platform")
    }

    // Netty - potrzebne dla NMS packet handlers w testach
    testImplementation("io.netty:netty-all:4.1.97.Final")
}

// Uruchamiaj jako testy integracyjne
tasks.test {
    useJUnitPlatform()
    jvmArgs("-Dnet.bytebuddy.experimental=true")

    // Wszystkie testy w tym module to testy integracyjne z MockBukkit
    shouldRunAfter(
        project(":common").tasks.named("test"),
        project(":shared-crafting").tasks.named("test"),
        project(":enhanced-crafting").tasks.named("test"),
        project(":enhanced-furnaces").tasks.named("test")
    )
}

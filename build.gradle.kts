import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

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

    // Ten projekt ma część testów, które używają MockBukkit.
    // Docelowo można je przenieść do osobnego modułu (integration-tests), ale na razie
    // dodajemy dependency, żeby build Gradle działał tak jak Maven.
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.101.0") {
        exclude(group = "io.papermc.paper", module = "paper-api")
        exclude(group = "net.kyori", module = "adventure-api")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Trzymamy kod NMS w osobnym katalogu, żeby Maven nadal mógł budować projekt bez dev bundle.
sourceSets {
    named("main") {
        java.srcDirs("src/main/java", "src/nms/java")
    }
}

// Artefakt produkcyjny: Spigot mappings (kompatybilność z serwerami produkcyjnymi).
// Na Paper 1.20.5+ taki plugin zostanie automatycznie zdeobfuskowany przy pierwszym starcie.
paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.REOBF_PRODUCTION

tasks.test {
    // Uwaga: w tym repo testy są "ustawione" pod Maven (są też osobne integration-tests/).
    // Gradle dokładamy głównie po to, żeby mieć paperweight-userdev i kompilować NMS.
    // Dlatego domyślnie nie odpalamy testów Gradle (żeby build JAR działał przewidywalnie).
    enabled = false

    useJUnitPlatform {
        // Tak samo jak w Maven (Surefire): integracyjne są w osobnym kroku.
        excludeTags("integration")
    }
    // Mockito/ByteBuddy na nowszych JDK potrafi wymagać tej flagi.
    jvmArgs("-Dnet.bytebuddy.experimental=true")
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

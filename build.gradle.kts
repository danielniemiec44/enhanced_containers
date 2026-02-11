import java.security.MessageDigest

plugins {
    // Paperweight tylko deklarujemy - poszczególne moduły go aplikują
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19" apply false
}

// Wspólna konfiguracja dla wszystkich podprojektów
subprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    // Konfiguracja Java 21 - musi być po apply(plugin = "java")
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    // Wszystkie testy jednostkowe używają JUnit 5 + Mockito (BEZ MockBukkit)
    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.2")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        "testImplementation"("org.mockito:mockito-core:5.12.0")
        "testImplementation"("org.mockito:mockito-junit-jupiter:5.12.0")
    }

    tasks.withType<Test> {
        useJUnitPlatform {
            // Wykluczamy testy integracyjne z normalnego 'test' taska
            excludeTags("integration")
        }
        jvmArgs("-Dnet.bytebuddy.experimental=true")
    }
}

// --- RESOURCE PACK: połączony z wszystkich modułów ---
tasks.register<Zip>("zipCombinedResourcePack") {
    group = "build"
    description = "Buduje połączony resource pack ze wszystkich modułów."

    archiveFileName.set("CombinedResourcePack.zip")
    destinationDirectory.set(file("resourcepack_out"))

    // Zbieraj assety z katalogu głównego resourcepack/
    from("resourcepack")

    outputs.upToDateWhen { false }

    doLast {
        val zipFile = archiveFile.get().asFile
        val sha1 = MessageDigest.getInstance("SHA-1")
            .digest(zipFile.readBytes())
            .joinToString("") { "%02x".format(it) }
        val sha1File = destinationDirectory.get().file("${archiveFileName.get()}.sha1").asFile
        sha1File.writeText(sha1)
        println("Combined Resource Pack: ${zipFile.absolutePath}")
        println("SHA-1: $sha1")
    }
}

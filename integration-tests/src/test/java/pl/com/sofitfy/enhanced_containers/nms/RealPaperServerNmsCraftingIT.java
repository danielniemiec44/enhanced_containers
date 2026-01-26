package pl.com.sofitfy.enhanced_containers.nms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Prawdziwy test integracyjny z Paper serwerem (bez gry).
 *
 * Co to sprawdza:
 * - uruchamiamy Paper 1.21.11 jako proces,
 * - ładujemy nasz plugin,
 * - plugin uruchamia self-test i wywołuje NMS recipe.assemble(...),
 * - wynik (repair/dye) zapisuje do pliku, a my go tu asercjami sprawdzamy.
 *
 * Jak uruchomić:
 * - mvn -f integration-tests/pom.xml -DrealPaperIT=true clean verify
 */
class RealPaperServerNmsCraftingIT {

    @Test
    @Timeout(180)
    void shouldComputeDynamicCraftingResultsUsingNmsAssemble() throws Exception {
        assumeTrue(Boolean.getBoolean("realPaperIT"), "Real Paper IT disabled. Use -DrealPaperIT=true");

        Path itWorkDir = Path.of("target", "real-paper-it").toAbsolutePath();
        deleteDir(itWorkDir);
        Files.createDirectories(itWorkDir);

        // 1) Zbuduj plugin jar z NMS (Gradle/paperweight)
        Path repoRoot = Path.of("..").toAbsolutePath().normalize();
        run(repoRoot, new String[]{"./gradlew", "--no-daemon", "-q", "assemble"});

        Path pluginJar = findPluginJar(repoRoot.resolve("build/libs"));
        assertTrue(Files.exists(pluginJar), "Plugin jar not found: " + pluginJar);

        // 2) Pobierz Paper serwer (latest build dla 1.21.11)
        Path paperJar = itWorkDir.resolve("paper-1.21.11.jar");
        downloadLatestPaperJar("1.21.11", paperJar);

        // 3) Przygotuj folder serwera
        Path serverDir = itWorkDir.resolve("server");
        Path pluginsDir = serverDir.resolve("plugins");
        Files.createDirectories(pluginsDir);

        // EULA
        Files.writeString(serverDir.resolve("eula.txt"), "eula=true\n", StandardCharsets.UTF_8);

        // Minimalne ustawienia, żeby serwer wstał szybciej
        Files.writeString(serverDir.resolve("server.properties"),
            "online-mode=false\n" +
                "spawn-protection=0\n" +
                "view-distance=2\n" +
                "simulation-distance=2\n" +
                "max-players=1\n" +
                "level-type=minecraft:flat\n",
            StandardCharsets.UTF_8
        );

        // Wrzucamy plugin
        Path targetPluginJar = pluginsDir.resolve("PlethorasExtendedTools.jar");
        Files.copy(pluginJar, targetPluginJar);

        // 4) Start Paper
        Process process = new ProcessBuilder(
            "java",
            "-Xms256m",
            "-Xmx1024m",
            "-Dplethoras.nmsSelfTest=true",
            "-jar",
            paperJar.toAbsolutePath().toString(),
            "--nogui"
        )
            .directory(serverDir.toFile())
            .redirectErrorStream(true)
            .start();

        // 5) Czekamy na wynik self-testu
        Path selfTestFile = serverDir.resolve("plugins").resolve("PlethorasExtendedTools").resolve("nms-selftest.properties");
        waitForFile(selfTestFile, Duration.ofSeconds(120));

        // 6) Czytamy wynik
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(selfTestFile.toFile())) {
            props.load(fis);
        }

        boolean ok = Boolean.parseBoolean(props.getProperty("ok", "false"));
        assertTrue(ok, "Self-test reported ok=false, props=" + props);

        assertEquals("true", props.getProperty("repair.ok"), "Repair should pass");
        assertEquals("true", props.getProperty("dye.ok"), "Dye should pass");

        assertTrue(Boolean.parseBoolean(props.getProperty("suggestions.ok")), "suggestions.ok should be true");
        assertTrue(Boolean.parseBoolean(props.getProperty("suggestions.planks.ok")), "suggestions.planks.ok should be true");

            assertTrue(Boolean.parseBoolean(props.getProperty("repairClamp.ok")), "repairClamp.ok should be true");
            assertTrue(Boolean.parseBoolean(props.getProperty("dyeMulti.ok")), "dyeMulti.ok should be true");
            assertTrue(Boolean.parseBoolean(props.getProperty("dyeMultiDyed.ok")), "dyeMultiDyed.ok should be true");
            assertTrue(Boolean.parseBoolean(props.getProperty("firework.ok")), "firework.ok should be true");

            assertTrue(Boolean.parseBoolean(props.getProperty("mapClone.ok")), "mapClone.ok should be true");
            assertTrue(Boolean.parseBoolean(props.getProperty("shieldBanner.ok")), "shieldBanner.ok should be true");
            assertTrue(Boolean.parseBoolean(props.getProperty("stew.ok")), "stew.ok should be true");

            assertTrue(Boolean.parseBoolean(props.getProperty("bannerDup.ok")), "bannerDup.ok should be true");
            assertTrue(Boolean.parseBoolean(props.getProperty("bookClone.ok")), "bookClone.ok should be true");
            assertTrue(Boolean.parseBoolean(props.getProperty("shulker.ok")), "shulker.ok should be true");
            assertTrue(Boolean.parseBoolean(props.getProperty("fireworkStar.ok")), "fireworkStar.ok should be true");

        // Dodatkowo: jakby proces jeszcze żył, poproś o stop.
        if (process.isAlive()) {
            process.getOutputStream().write("stop\n".getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().flush();
        }

        // i czekamy aż się zamknie (plugin i tak woła Bukkit.shutdown())
        process.waitFor();
    }

    private static void waitForFile(Path file, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (Files.exists(file) && Files.size(file) > 0) {
                return;
            }
            Thread.sleep(250);
        }
        fail("Timeout waiting for file: " + file);
    }

    private static void run(Path cwd, String[] command) throws Exception {
        Process p = new ProcessBuilder(command)
            .directory(cwd.toFile())
            .redirectErrorStream(true)
            .start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line).append('\n');
            }
        }

        int code = p.waitFor();
        if (code != 0) {
            throw new IllegalStateException("Command failed (" + code + "): " + String.join(" ", command) + "\n" + out);
        }
    }

    private static void downloadLatestPaperJar(String mcVersion, Path outJar) throws Exception {
        if (Files.exists(outJar) && Files.size(outJar) > 0) {
            return;
        }

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

        // 1) pobierz listę buildów
        URI buildsUri = URI.create("https://api.papermc.io/v2/projects/paper/versions/" + mcVersion + "/builds");
        HttpRequest buildsReq = HttpRequest.newBuilder(buildsUri)
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();

        String buildsJson = client.send(buildsReq, HttpResponse.BodyHandlers.ofString()).body();

        // Najprostsze parsowanie: wyciągamy ostatni numer builda.
        // Format JSON jest stabilny, a my chcemy tylko 1 liczbę.
        int lastBuild = extractLastBuildNumber(buildsJson);
        if (lastBuild <= 0) {
            throw new IllegalStateException("Cannot parse Paper build from JSON");
        }

        String fileName = "paper-" + mcVersion + "-" + lastBuild + ".jar";
        URI downloadUri = URI.create(
            "https://api.papermc.io/v2/projects/paper/versions/" + mcVersion + "/builds/" + lastBuild + "/downloads/" + fileName
        );

        HttpRequest jarReq = HttpRequest.newBuilder(downloadUri)
            .timeout(Duration.ofMinutes(2))
            .GET()
            .build();

        HttpResponse<byte[]> jarResp = client.send(jarReq, HttpResponse.BodyHandlers.ofByteArray());
        if (jarResp.statusCode() != 200) {
            throw new IllegalStateException("Cannot download Paper jar: HTTP " + jarResp.statusCode());
        }

        Files.write(outJar, jarResp.body());
    }

    private static int extractLastBuildNumber(String json) {
        // Szukamy "build": <number> i bierzemy ostatnie dopasowanie.
        // To jest celowo proste, bo nie chcemy tu dodatkowych zależności JSON.
        int idx = -1;
        int last = -1;
        while (true) {
            idx = json.indexOf("\"build\":", idx + 1);
            if (idx < 0) break;
            int start = idx + "\"build\":".length();
            while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
            int end = start;
            while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
            if (end > start) {
                try {
                    last = Integer.parseInt(json.substring(start, end));
                } catch (NumberFormatException ignored) {
                    // ignore
                }
            }
        }
        return last;
    }

    private static Path findPluginJar(Path libsDir) throws Exception {
        // Szukamy "najbardziej sensownego" jar-a: bez -dev, bez sources/javadoc.
        // paperweight zwykle generuje jar reobf, a my chcemy właśnie ten.
        return Files.list(libsDir)
            .filter(p -> p.getFileName().toString().endsWith(".jar"))
            .filter(p -> !p.getFileName().toString().contains("-dev"))
            .filter(p -> !p.getFileName().toString().contains("sources"))
            .filter(p -> !p.getFileName().toString().contains("javadoc"))
            .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
            .orElseThrow(() -> new IllegalStateException("No plugin jar found in " + libsDir));
    }

    private static void deleteDir(Path dir) throws Exception {
        if (!Files.exists(dir)) {
            return;
        }
        Files.walk(dir)
            .sorted(Comparator.reverseOrder())
            .forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
            });
    }
}

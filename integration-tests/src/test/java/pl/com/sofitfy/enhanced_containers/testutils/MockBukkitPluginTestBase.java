package pl.com.sofitfy.enhanced_containers.testutils;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import pl.com.sofitfy.enhanced_containers.Main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Baza dla testów integracyjnych, gdzie potrzebujemy prawdziwie załadowanego pluginu.
 *
 * Uwaga praktyczna:
 * - MockBukkit (Paper) wymaga pluginu z pliku .jar.
 * - W Mavenie jar zwykle już istnieje w target/, ale w runnerze z IDE może go nie być.
 * - Jeśli jar nie istnieje, tworzymy minimalny jar z target/classes.
 */
public abstract class MockBukkitPluginTestBase extends MockBukkitTestBase {

    protected Plugin plugin;

    @BeforeEach
    void loadPlugin() {
        // Preferowana ścieżka w MockBukkit: ładujemy plugin po klasie.
        // To daje poprawny classloader (w przeciwnym razie JavaPlugin rzuca wyjątek).
        try {
            plugin = MockBukkit.load(Main.class);
            return;
        } catch (Exception ignored) {
            // Fallback: jeśli coś pójdzie nie tak, spróbujmy przez jar.
        }

        File pluginJar = ensurePluginJarExists();
        plugin = MockBukkit.loadJar(pluginJar);
    }

    private static File ensurePluginJarExists() {
        File existing = findBuiltPluginJarOrNull();
        if (existing != null) {
            return existing;
        }

        // W testach integracyjnych uruchamiamy Maven-a z modułu integration-tests,
        // ale klasy pluginu są w głównym module (".."), więc szukamy też w ../target.
        File targetDir = new File("target");
        File classesDir = new File(targetDir, "classes");
        if (!classesDir.isDirectory()) {
            File parentTarget = new File("../target");
            File parentClasses = new File(parentTarget, "classes");
            if (parentClasses.isDirectory()) {
                classesDir = parentClasses;
            } else {
                throw new IllegalStateException(
                    "Nie znaleziono target/classes ani ../target/classes. Uruchom najpierw kompilację (np. mvn -DskipTests package) albo odpal testy z Maven-a."
                );
            }
        }

        File jarFile = new File(targetDir, "enhanced_containers-under-test.jar");
        try {
            createJarFromClassesDir(classesDir.toPath(), jarFile.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się utworzyć jar-a testowego dla MockBukkit.", e);
        }

        return jarFile;
    }

    private static File findBuiltPluginJarOrNull() {
        File[] targetDirs = new File[] {
            new File("target"),
            new File("../target")
        };

        return Arrays.stream(targetDirs)
            .filter(File::isDirectory)
            .map(dir -> dir.listFiles((d, name) ->
                name.startsWith("enhanced_containers-")
                    && name.endsWith(".jar")
                    && !name.endsWith("-tests.jar")
                    && !name.endsWith("-sources.jar")
                    && !name.endsWith("-javadoc.jar")
            ))
            .filter(files -> files != null && files.length > 0)
            .flatMap(Arrays::stream)
            .max(Comparator.comparingLong(File::lastModified))
            .orElse(null);
    }

    private static void createJarFromClassesDir(Path classesDir, Path jarFile) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        Files.createDirectories(jarFile.getParent());

        try (JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(jarFile.toFile()), manifest)) {
            Files.walk(classesDir)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String entryName = classesDir.relativize(path)
                        .toString()
                        .replace(File.separatorChar, '/');

                    JarEntry entry = new JarEntry(entryName);
                    entry.setTime(path.toFile().lastModified());
                    try {
                        jarOut.putNextEntry(entry);
                        Files.copy(path, jarOut);
                        jarOut.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException io) {
                throw io;
            }
            throw e;
        }
    }
    
    /**
     * Załaduj vanilla receptury do MockBukkita.
     * MockBukkit jest pusty domyślnie - ta metoda dodaje podstawowe vanilla receptury.
     */
    protected void loadVanillaRecipes() {
        // OAK_LOG -> 4x OAK_PLANKS (szapeless)
        ShapelessRecipe logToPlanks = new ShapelessRecipe(
            new NamespacedKey("minecraft", "oak_planks"),
            new ItemStack(Material.OAK_PLANKS, 4)
        );
        logToPlanks.addIngredient(Material.OAK_LOG);
        Bukkit.getServer().addRecipe(logToPlanks);
        
        // PAPER + GUNPOWDER -> FIREWORK_ROCKET (shapeless)
        ShapelessRecipe firework = new ShapelessRecipe(
            new NamespacedKey("minecraft", "firework_rocket"),
            new ItemStack(Material.FIREWORK_ROCKET, 1)
        );
        firework.addIngredient(Material.PAPER);
        firework.addIngredient(Material.GUNPOWDER);
        Bukkit.getServer().addRecipe(firework);
    }
}

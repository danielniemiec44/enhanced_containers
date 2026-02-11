package pl.com.sofitfy.enhanced_containers.testutils;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;

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
 * Baza dla testów integracyjnych - ładuje konkretny plugin przez MockBukkit.
 *
 * Każdy test powinien nadpisać getPluginClass() wskazując na właściwą klasę Main:
 * - SharedCraftingMain.class dla testów SharedCrafting
 * - EnhancedCraftingMain.class dla testów EnhancedCrafting
 * - EnhancedFurnacesMain.class dla testów EnhancedFurnaces
 */
public abstract class MockBukkitPluginTestBase extends MockBukkitTestBase {

    protected Plugin plugin;

    /**
     * Zwraca klasę Main pluginu do załadowania.
     * Domyślnie ładuje EnhancedCraftingMain (większość testów go używa).
     * Testy furnace/shared-crafting powinny to nadpisać.
     */
    protected Class<? extends JavaPlugin> getPluginClass() {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends JavaPlugin> clazz = (Class<? extends JavaPlugin>)
                Class.forName("pl.com.sofitfy.enhanced_containers.EnhancedCraftingMain");
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Nie znaleziono klasy EnhancedCraftingMain", e);
        }
    }

    @BeforeEach
    void loadPlugin() {
        // Ładujemy plugin po klasie - daje poprawny classloader
        try {
            plugin = MockBukkit.load(getPluginClass());
            return;
        } catch (Exception ignored) {
            // Fallback: próbujemy przez jar
        }

        File pluginJar = ensurePluginJarExists();
        plugin = MockBukkit.loadJar(pluginJar);
    }

    private File ensurePluginJarExists() {
        File existing = findBuiltPluginJarOrNull();
        if (existing != null) {
            return existing;
        }

        File targetDir = new File("target");
        File classesDir = new File(targetDir, "classes");
        if (!classesDir.isDirectory()) {
            File parentTarget = new File("../target");
            File parentClasses = new File(parentTarget, "classes");
            if (parentClasses.isDirectory()) {
                classesDir = parentClasses;
            } else {
                throw new IllegalStateException(
                    "Nie znaleziono target/classes. Uruchom najpierw kompilację."
                );
            }
        }

        File jarFile = new File(targetDir, "plugin-under-test.jar");
        try {
            createJarFromClassesDir(classesDir.toPath(), jarFile.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się utworzyć jar-a testowego.", e);
        }

        return jarFile;
    }

    private File findBuiltPluginJarOrNull() {
        File[] targetDirs = new File[] {
            new File("target"),
            new File("../target")
        };

        return Arrays.stream(targetDirs)
            .filter(File::isDirectory)
            .map(dir -> dir.listFiles((d, name) ->
                name.endsWith(".jar")
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
     * Załaduj podstawowe vanilla receptury do MockBukkita.
     */
    protected void loadVanillaRecipes() {
        ShapelessRecipe logToPlanks = new ShapelessRecipe(
            new NamespacedKey("minecraft", "oak_planks"),
            new ItemStack(Material.OAK_PLANKS, 4)
        );
        logToPlanks.addIngredient(Material.OAK_LOG);
        Bukkit.getServer().addRecipe(logToPlanks);

        ShapelessRecipe firework = new ShapelessRecipe(
            new NamespacedKey("minecraft", "firework_rocket"),
            new ItemStack(Material.FIREWORK_ROCKET, 1)
        );
        firework.addIngredient(Material.PAPER);
        firework.addIngredient(Material.GUNPOWDER);
        Bukkit.getServer().addRecipe(firework);
    }
}

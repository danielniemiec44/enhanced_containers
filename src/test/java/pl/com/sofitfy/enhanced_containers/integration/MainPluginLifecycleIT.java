package pl.com.sofitfy.enhanced_containers.integration;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import pl.com.sofitfy.enhanced_containers.Main;
import pl.com.sofitfy.enhanced_containers.commands.CraftCommand;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne dla lifecycle pluginu.
 *
 * Co tu sprawdzamy:
 * - plugin ładuje się jak na serwerze (z jar-a)
 * - config.yml jest zapisany do data folder
 * - komenda /4x4 ma executor, a /ecraft jest aliasem
 * - scheduler działa po tickach
 *
 * Komentarze są po polsku i prostym językiem.
 */
@Tag("integration")
@DisplayName("Main - integracja (start pluginu)")
@Disabled("MockBukkit (Paper) nie potrafi tu załadować pluginu z jar-a, bo klasy pluginu są na classpath tego samego modułu. Testy integracyjne pluginu są w osobnym projekcie integration-tests/.")
class MainPluginLifecycleIT extends MockBukkitPluginTestBase {

    @Test
    @DisplayName("Plugin jest włączony i ma zarejestrowaną komendę /4x4 (alias: /ecraft)")
    void pluginIsEnabledAndHasCommand() {
        // Dado: plugin jest ładowany w bazie testowej
        assertNotNull(plugin, "Plugin powinien być załadowany");
        assertTrue(plugin.isEnabled(), "Plugin powinien być enabled");
        assertEquals("PlethorasExtendedTools", plugin.getName(), "Nazwa pluginu powinna brać się z plugin.yml");

        // To: komenda istnieje i ma executor
        assertTrue(plugin instanceof Main, "W tym projekcie main class to pl.com.sofitfy.enhanced_containers.Main");

        PluginCommand mainCmd = ((Main) plugin).getCommand("4x4");
        assertNotNull(mainCmd, "Komenda 4x4 powinna istnieć (plugin.yml)");
        assertNotNull(mainCmd.getExecutor(), "Komenda 4x4 powinna mieć ustawiony executor");
        assertTrue(mainCmd.getExecutor() instanceof CraftCommand, "Executor powinien być CraftCommand");

        PluginCommand aliasCmd = ((Main) plugin).getCommand("ecraft");
        assertNotNull(aliasCmd, "Alias ecraft powinien istnieć (plugin.yml)");
        assertNotNull(aliasCmd.getExecutor(), "Alias ecraft powinien mieć ustawiony executor");
        assertSame(mainCmd.getExecutor(), aliasCmd.getExecutor(), "Alias ecraft powinien wskazywać na ten sam executor co 4x4");
    }

    @Test
    @DisplayName("Config jest zapisany do data folder i można go wczytać")
    void configIsSavedToDataFolder() {
        // Dado: plugin zapisuje config.yml przy starcie (saveResource)
        File dataFolder = plugin.getDataFolder();
        assertNotNull(dataFolder);
        assertTrue(dataFolder.exists(), "Data folder powinien istnieć po starcie pluginu");

        File configFile = new File(dataFolder, "config.yml");
        assertTrue(configFile.isFile(), "config.yml powinien być zapisany do data folder");

        // To: da się wczytać YAML i ma klucz tytułu GUI
        var yaml = YamlConfiguration.loadConfiguration(configFile);
        assertNotNull(yaml.getString("crafting.gui.title"), "W config.yml powinien być crafting.gui.title");
    }

    @Test
    @DisplayName("Scheduler wykonuje task po tickach")
    void schedulerRunsTaskAfterTicks() {
        // Dado: task który ma się wykonać za 2 ticki
        AtomicBoolean executed = new AtomicBoolean(false);
        server.getScheduler().runTaskLater(plugin, () -> executed.set(true), 2L);

        // Kiedy: lecą ticki
        tick(1);
        assertFalse(executed.get(), "Po 1 ticku task nie powinien się jeszcze wykonać");

        tick(1);
        assertTrue(executed.get(), "Po 2 tickach task powinien się wykonać");
    }

    @Test
    @DisplayName("Plugin ma zarejestrowane jakieś listenery")
    void pluginRegistersListeners() {
        // To: plugin powinien mieć chociaż jednego listenera (w onEnable rejestrujemy kilka)
        var listeners = HandlerList.getRegisteredListeners(plugin);
        assertNotNull(listeners);
        assertTrue(listeners.size() > 0, "Plugin powinien mieć zarejestrowane listenery");
    }
}

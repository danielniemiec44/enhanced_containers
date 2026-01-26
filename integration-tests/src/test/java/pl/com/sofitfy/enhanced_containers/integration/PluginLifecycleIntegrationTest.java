package pl.com.sofitfy.enhanced_containers.integration;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import pl.com.sofitfy.enhanced_containers.Main;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne cyklu życia pluginu.
 *
 * Używamy MockBukkit.load(Main.class) do ładowania pluginu bezpośrednio z classpath.
 * Dzięki temu mamy dostęp do klas pluginu w testach.
 *
 * Komentarze są po polsku i prostym językiem.
 */
@DisplayName("PlethorasExtendedTools - testy integracyjne cyklu życia")
class PluginLifecycleIntegrationTest {

    private ServerMock server;
    private Main plugin;

    @BeforeEach
    void setUp() {
        // Inicjalizujemy mock serwera i ładujemy plugin
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Main.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Tag("integration")
    @Test
    @DisplayName("Plugin startuje, zapisuje config i rejestruje /4x4")
    void pluginStartsAndRegistersCommand() {
        // Plugin powinien być włączony
        assertTrue(plugin.isEnabled(), "Plugin powinien być enabled");
        assertEquals("PlethorasExtendedTools", plugin.getName(), "Nazwa pluginu powinna brać się z plugin.yml");

        // config.yml jest zapisany do data folder
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        assertTrue(configFile.isFile(), "config.yml powinien istnieć po starcie pluginu");

        // Da się wczytać YAML i ma klucz tytułu GUI
        var yaml = YamlConfiguration.loadConfiguration(configFile);
        assertNotNull(yaml.getString("crafting.gui.title"), "W config.yml powinien być crafting.gui.title");

        // /4x4 istnieje i ma executor
        PluginCommand cmd = plugin.getCommand("4x4");
        assertNotNull(cmd, "Komenda 4x4 powinna być zarejestrowana");

        CommandExecutor executor = cmd.getExecutor();
        assertNotNull(executor, "Komenda 4x4 powinna mieć ustawiony executor");
        assertEquals(
            "pl.com.sofitfy.enhanced_containers.commands.CraftCommand",
            executor.getClass().getName(),
            "Executor powinien być CraftCommand"
        );

        // Plugin ma jakieś listenery
        var listeners = HandlerList.getRegisteredListeners(plugin);
        assertNotNull(listeners);
        assertTrue(listeners.size() > 0, "Plugin powinien mieć zarejestrowane listenery");
    }

    @Tag("integration")
    @Test
    @DisplayName("Scheduler wykonuje task po tickach")
    void schedulerRunsTaskAfterTicks() {
        assertTrue(plugin.isEnabled(), "Plugin powinien być enabled");

        AtomicBoolean executed = new AtomicBoolean(false);
        server.getScheduler().runTaskLater(plugin, () -> executed.set(true), 2L);

        // Kiedy: lecą ticki
        server.getScheduler().performOneTick();
        assertFalse(executed.get(), "Po 1 ticku task nie powinien się wykonać");

        server.getScheduler().performOneTick();
        assertTrue(executed.get(), "Po 2 tickach task powinien się wykonać");
    }
}

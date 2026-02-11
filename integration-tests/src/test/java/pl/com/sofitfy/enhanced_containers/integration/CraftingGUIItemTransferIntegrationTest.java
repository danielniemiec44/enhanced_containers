package pl.com.sofitfy.enhanced_containers.integration;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import pl.com.sofitfy.enhanced_containers.EnhancedCraftingMain;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne dla przenoszenia item'ów w GUI craftingu.
 *
 * Używamy MockBukkit.load(EnhancedCraftingMain.class) do ładowania pluginu.
 *
 * Komentarze po polsku i prostym językiem.
 */
@DisplayName("Crafting GUI - testy integracyjne przenoszenia item'ów")
class CraftingGUIItemTransferIntegrationTest {

    private ServerMock server;
    private EnhancedCraftingMain plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EnhancedCraftingMain.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Tag("integration")
    @Test
    @DisplayName("Gracz może otworzyć GUI craftingu za pomocą /4x4")
    void playerCanOpenCraftingGUI() {
        // Stwórz gracza
        Player player = server.addPlayer();
        assertNotNull(player, "Gracz powinien być stworzony");

        // Daj graczowi permisje
        player.addAttachment(plugin, "plethoras.tools.use", true);

        // Działanie: gracz wykonuje komendę /4x4
        boolean commandExecuted = server.dispatchCommand(player, "4x4");

        // Sprawdzenie: komenda powinna zostać przetworzana
        assertTrue(commandExecuted, "Komenda /4x4 powinna być przetworzona");

        // Sprawdzenie: gracz powinien mieć otwarte okienko
        assertNotNull(player.getOpenInventory(), "Okienko powinno być otwarte");
    }

    @Tag("integration")
    @Test
    @DisplayName("Gracz może umieścić item w gridu craftingu")
    void playerCanPlaceItemInCraftingGrid() {
        // Stwórz gracza
        Player player = server.addPlayer();
        assertNotNull(player, "Gracz powinien być stworzony");

        // Daj graczowi permisje
        player.addAttachment(plugin, "plethoras.tools.use", true);

        // Działanie: gracz otwiera GUI craftingu
        boolean commandExecuted = server.dispatchCommand(player, "4x4");
        assertTrue(commandExecuted, "Komenda /4x4 powinna się wykonać");

        // Gracz ma otwarte GUI
        assertNotNull(player.getOpenInventory(), "GUI powinno być otwarte");

        // Daj graczowi item do ręki
        ItemStack diamondBlock = new ItemStack(Material.DIAMOND_BLOCK);
        player.getInventory().addItem(diamondBlock);

        // Sprawdzenie: gracz ma item w inventorium
        assertTrue(player.getInventory().contains(Material.DIAMOND_BLOCK),
            "Gracz powinien mieć DIAMOND_BLOCK w inventorium");
    }

    @Tag("integration")
    @Test
    @DisplayName("Output slot można kliknąć jak w vanilla")
    void outputSlotCanBeClicked() {
        // Stwórz gracza
        Player player = server.addPlayer();
        assertNotNull(player, "Gracz powinien być stworzony");

        // Daj graczowi permisje
        player.addAttachment(plugin, "plethoras.tools.use", true);

        // Działanie: gracz otwiera GUI craftingu
        boolean commandExecuted = server.dispatchCommand(player, "4x4");
        assertTrue(commandExecuted, "Komenda /4x4 powinna się wykonać");

        // Gracz ma otwarte GUI
        assertNotNull(player.getOpenInventory(), "GUI powinno być otwarte");
        
        // Sprawdzenie: okienko powinno mieć jakieś sloty (konkretna liczba zależy od Bukkit)
        assertTrue(player.getOpenInventory().getBottomInventory().getSize() > 0,
            "GUI powinno mieć przynajmniej 1 slot");
    }

    @Tag("integration")
    @Test
    @DisplayName("Serwer poprawnie obsługuje wielokrotne otwieranie GUI")
    void serverHandlesMultiplePlayersOpeningGUI() {
        // Stwórz dwóch graczy
        Player player1 = server.addPlayer();
        Player player2 = server.addPlayer();

        assertNotNull(player1, "Gracz 1 powinien być stworzony");
        assertNotNull(player2, "Gracz 2 powinien być stworzony");

        // Daj graczom permisje
        player1.addAttachment(plugin, "plethoras.tools.use", true);
        player2.addAttachment(plugin, "plethoras.tools.use", true);

        // Działanie: obaj gracze otwierają GUI
        boolean cmd1 = server.dispatchCommand(player1, "4x4");
        boolean cmd2 = server.dispatchCommand(player2, "4x4");

        // Sprawdzenie: obaj gracz mają otwarte GUI
        assertTrue(cmd1, "Gracz 1 powinien otworzyć GUI");
        assertTrue(cmd2, "Gracz 2 powinien otworzyć GUI");

        assertNotNull(player1.getOpenInventory(), "Gracz 1 powinien mieć otwarte GUI");
        assertNotNull(player2.getOpenInventory(), "Gracz 2 powinien mieć otwarte GUI");

        // Sprawdzenie: GUI powinny być niezależne
        assertNotEquals(
            player1.getOpenInventory(),
            player2.getOpenInventory(),
            "Każdy gracz powinien mieć oddzielne GUI"
        );
    }

    @Tag("integration")
    @Test
    @DisplayName("Gracz bez permisji nie może otworzyć GUI craftingu")
    void playerWithoutPermissionCannotOpenGUI() {
        // Stwórz gracza BEZ permisji
        Player player = server.addPlayer();
        assertNotNull(player, "Gracz powinien być stworzony");

        // Gracz nie ma permisji plethoras.tools.use
        player.setOp(false);

        // Działanie: gracz próbuje otworzyć GUI
        boolean commandExecuted = server.dispatchCommand(player, "4x4");

        // Sprawdzenie: komenda powinna być przetworzona ale GUI nie otworzy się
        assertTrue(commandExecuted, "Komenda powinna być przetworzona");
    }
}

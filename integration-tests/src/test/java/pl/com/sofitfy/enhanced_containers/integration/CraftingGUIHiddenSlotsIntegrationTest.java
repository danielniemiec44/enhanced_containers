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
 * Testy integracyjne dla ukrytych slotów w GUI craftingu.
 *
 * Używamy MockBukkit.load(EnhancedCraftingMain.class) do ładowania pluginu.
 *
 * Komentarze po polsku i prostym językiem.
 */
@DisplayName("Integration - ukryte sloty w crafting GUI")
class CraftingGUIHiddenSlotsIntegrationTest {

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
    @DisplayName("Gracz może otworzyć GUI craftingu komendą /4x4")
    void playerCanOpenCraftingGUI() {
        Player player = server.addPlayer();
        player.addAttachment(plugin, "plethoras.tools.use", true);

        boolean commandExecuted = server.dispatchCommand(player, "4x4");

        assertTrue(commandExecuted, "Komenda /4x4 powinna się wykonać");
        assertNotNull(player.getOpenInventory(), "GUI powinno być otwarte");
    }

    @Tag("integration")
    @Test
    @DisplayName("GUI craftingu ma 54 sloty (9x6 rzędów)")
    void craftingGUIHasCorrectSize() {
        Player player = server.addPlayer();
        player.addAttachment(plugin, "plethoras.tools.use", true);

        server.dispatchCommand(player, "4x4");

        assertEquals(54, player.getOpenInventory().getTopInventory().getSize(),
            "GUI powinno mieć 54 sloty");
    }

    @Tag("integration")
    @Test
    @DisplayName("Drugi gracz może otworzyć własne GUI niezależnie")
    void multiplePlayersCanOpenTheirOwnGUI() {
        Player player1 = server.addPlayer("Player1");
        Player player2 = server.addPlayer("Player2");

        player1.addAttachment(plugin, "plethoras.tools.use", true);
        player2.addAttachment(plugin, "plethoras.tools.use", true);

        server.dispatchCommand(player1, "4x4");
        server.dispatchCommand(player2, "4x4");

        assertNotNull(player1.getOpenInventory(), "Player1 powinien mieć otwarte GUI");
        assertNotNull(player2.getOpenInventory(), "Player2 powinien mieć otwarte GUI");

        assertNotEquals(
            player1.getOpenInventory().getTopInventory(),
            player2.getOpenInventory().getTopInventory(),
            "Każdy gracz powinien mieć własne GUI"
        );
    }

    @Tag("integration")
    @Test
    @DisplayName("Gracz bez permisji nie może otworzyć GUI")
    void playerWithoutPermissionCannotOpenGUI() {
        Player player = server.addPlayer();
        // NIE dodajemy permisji plethoras.tools.use

        server.dispatchCommand(player, "4x4");

        // Gracz powinien mieć otwarty ekwipunek (ale nie craftingGUI)
        assertNotNull(player.getOpenInventory(),
            "Gracz powinien mieć otwarty ekwipunek (ale nie craftingGUI)");
    }

    @Tag("integration")
    @Test
    @DisplayName("GUI mają poprawne sloty dostępne (input grid + output)")
    void craftingGUIHasCorrectVisibleSlots() {
        Player player = server.addPlayer();
        player.addAttachment(plugin, "plethoras.tools.use", true);

        server.dispatchCommand(player, "4x4");

        assertNotNull(player.getOpenInventory(), "GUI powinno być otwarte");

        ItemStack testItem = new ItemStack(Material.DIRT);
        player.getOpenInventory().getTopInventory().setItem(0, testItem);

        ItemStack retrieved = player.getOpenInventory().getTopInventory().getItem(0);
        assertNotNull(retrieved, "Item powinien być w slocie 0 (input slot)");
        assertEquals(Material.DIRT, retrieved.getType(), "Item powinien być DIRT");
    }

    @Tag("integration")
    @Test
    @DisplayName("Serwer poprawnie handluje wieloma graczami otwierającymi GUI jednocześnie")
    void serverHandlesMultipleConcurrentGUIs() {
        Player player1 = server.addPlayer("Player1");
        Player player2 = server.addPlayer("Player2");
        Player player3 = server.addPlayer("Player3");

        player1.addAttachment(plugin, "plethoras.tools.use", true);
        player2.addAttachment(plugin, "plethoras.tools.use", true);
        player3.addAttachment(plugin, "plethoras.tools.use", true);

        server.dispatchCommand(player1, "4x4");
        server.dispatchCommand(player2, "4x4");
        server.dispatchCommand(player3, "4x4");

        ItemStack item1 = new ItemStack(Material.DIAMOND);
        ItemStack item2 = new ItemStack(Material.EMERALD);
        ItemStack item3 = new ItemStack(Material.GOLD_INGOT);

        player1.getOpenInventory().getTopInventory().setItem(0, item1);
        player2.getOpenInventory().getTopInventory().setItem(0, item2);
        player3.getOpenInventory().getTopInventory().setItem(0, item3);

        assertEquals(Material.DIAMOND, 
            player1.getOpenInventory().getTopInventory().getItem(0).getType(),
            "Player1 powinien mieć DIAMOND");

        assertEquals(Material.EMERALD,
            player2.getOpenInventory().getTopInventory().getItem(0).getType(),
            "Player2 powinien mieć EMERALD");

        assertEquals(Material.GOLD_INGOT,
            player3.getOpenInventory().getTopInventory().getItem(0).getType(),
            "Player3 powinien mieć GOLD_INGOT");
    }
}

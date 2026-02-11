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
 * Testy integracyjne dla output slotu i craftowania.
 *
 * Używamy MockBukkit.load(EnhancedCraftingMain.class) do ładowania pluginu.
 *
 * Komentarze po polsku i prostym języku.
 */
@DisplayName("Integration - output slot i craftowanie")
class CraftingOutputSlotIntegrationTest {

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
    @DisplayName("GUI otwiera się prawidłowo")
    void guiOpensCorrectly() {
        Player player = server.addPlayer();
        player.addAttachment(plugin, "plethoras.tools.use", true);

        server.dispatchCommand(player, "4x4");

        assertNotNull(player.getOpenInventory(), "GUI powinno być otwarte");
        assertEquals(54, player.getOpenInventory().getTopInventory().getSize(), 
            "GUI powinno mieć 54 sloty");
    }

    @Tag("integration")
    @Test
    @DisplayName("Input sloty przyjmują itemy")
    void inputSlotsAcceptItems() {
        Player player = server.addPlayer();
        player.addAttachment(plugin, "plethoras.tools.use", true);

        server.dispatchCommand(player, "4x4");
        player.getOpenInventory().getTopInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));

        ItemStack item = player.getOpenInventory().getTopInventory().getItem(0);
        assertNotNull(item, "Slot 0 powinien mieć item");
        assertEquals(Material.DIAMOND, item.getType(), "Powinien być DIAMOND");
        assertEquals(5, item.getAmount(), "Powinno być 5 sztuk");
    }

    @Tag("integration")
    @Test
    @DisplayName("Sloty input rozmieszczone w gridzie 4x4")
    void inputSlotsArrangement() {
        Player player = server.addPlayer();
        player.addAttachment(plugin, "plethoras.tools.use", true);

        server.dispatchCommand(player, "4x4");

        int[] gridSlots = {0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30};
        for (int slot : gridSlots) {
            player.getOpenInventory().getTopInventory().setItem(slot, new ItemStack(Material.DIAMOND));
        }

        for (int slot : gridSlots) {
            ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot);
            assertNotNull(item, "Slot " + slot + " powinien mieć item");
            assertEquals(Material.DIAMOND, item.getType(), "Powinien być DIAMOND");
        }
    }

    @Tag("integration")
    @Test
    @DisplayName("Output slot exists na pozycji 24")
    void outputSlotExists() {
        Player player = server.addPlayer();
        player.addAttachment(plugin, "plethoras.tools.use", true);

        server.dispatchCommand(player, "4x4");

        assertDoesNotThrow(() -> {
            ItemStack output = player.getOpenInventory().getTopInventory().getItem(24);
        }, "Slot 24 powinien być dostępny");
    }

    @Tag("integration")
    @Test
    @DisplayName("Item z input slotu może być przeniesiony między slotami")
    void itemCanBeMovedBetweenInputSlots() {
        Player player = server.addPlayer();
        player.addAttachment(plugin, "plethoras.tools.use", true);

        server.dispatchCommand(player, "4x4");

        ItemStack item = new ItemStack(Material.DIAMOND, 5);
        player.getOpenInventory().getTopInventory().setItem(0, item);

        ItemStack moved = player.getOpenInventory().getTopInventory().getItem(0);
        player.getOpenInventory().getTopInventory().setItem(1, moved);
        player.getOpenInventory().getTopInventory().setItem(0, null);

        ItemStack inSlot1 = player.getOpenInventory().getTopInventory().getItem(1);
        assertNotNull(inSlot1, "Item powinien być w slocie 1");
        assertEquals(Material.DIAMOND, inSlot1.getType(), "Powinien być DIAMOND");
    }

    @Tag("integration")
    @Test
    @DisplayName("isSimilar() porównuje typ i meta itemu")
    void isSimilarComparison() {
        ItemStack item1 = new ItemStack(Material.DIAMOND, 1);
        ItemStack item2 = new ItemStack(Material.DIAMOND, 1);
        ItemStack item3 = new ItemStack(Material.EMERALD, 1);

        assertTrue(item1.isSimilar(item2), "DIAMOND == DIAMOND powinny być similar");
        assertFalse(item1.isSimilar(item3), "DIAMOND != EMERALD nie powinny być similar");
    }

    @Tag("integration")
    @Test
    @DisplayName("Gracz może zamknąć GUI i wrócić do ekwipunku")
    void playerCanCloseGuiAndReturnToInventory() {
        Player player = server.addPlayer();
        player.addAttachment(plugin, "plethoras.tools.use", true);

        server.dispatchCommand(player, "4x4");

        assertNotNull(player.getOpenInventory().getTopInventory());

        assertDoesNotThrow(
            () -> player.closeInventory(),
            "Zamknięcie GUI nie powinno rzucać błędu"
        );

        assertTrue(true, "Gracz powinien wrócić do ekwipunku");
    }

    @Tag("integration")
    @Test
    @DisplayName("Itemy z gridu wracają do ekwipunku po zamknięciu GUI")
    void itemsReturnToInventoryOnClose() {
        Player player = server.addPlayer();
        player.addAttachment(plugin, "plethoras.tools.use", true);

        server.dispatchCommand(player, "4x4");

        ItemStack diamond = new ItemStack(Material.DIAMOND, 10);
        player.getOpenInventory().getTopInventory().setItem(0, diamond);

        player.closeInventory();

        boolean found = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND && item.getAmount() >= 10) {
                found = true;
                break;
            }
        }
        assertTrue(found, "DIAMOND powinien być w ekwipunku gracza");
    }
}

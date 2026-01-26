package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne (MockBukkit) dla CraftingGUI.
 *
 * Uwaga: to wymaga prawdziwego Bukkit API, więc ten test jest w module integration-tests.
 */
@DisplayName("CraftingGUI - testy inventory (MockBukkit)")
@Tag("integration")
class CraftingGUIBehaviorTest extends MockBukkitTestBase {

    @Test
    @DisplayName("GUI ma 54 sloty")
    void guiHas54Slots() {
        var player = createPlayer();
        CraftingGUI gui = new CraftingGUI(player, "Test");
        assertEquals(54, gui.getInventory().getSize());
    }

    @Test
    @DisplayName("Output slot jest w slocie 24")
    void outputSlotIs24() {
        assertEquals(24, CraftingGUI.OUTPUT_SLOT, "Slot output musi być stały");
    }

    @Test
    @DisplayName("Output slot startuje pusty (jak crafting table)")
    void outputStartsEmpty() {
        var player = createPlayer();
        CraftingGUI gui = new CraftingGUI(player, "Test");

        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);

        // Wymaganie: jak w vanilli - brak placeholdera.
        assertNull(output, "Output powinien być pusty dopóki nie ma matcha receptury");
    }

    @Test
    @DisplayName("setInputItem ustawia item tylko w slotach gridu")
    void setInputItemOnlyForGridSlots() {
        var player = createPlayer();
        CraftingGUI gui = new CraftingGUI(player, "Test");

        gui.setInputItem(5, new ItemStack(Material.DIAMOND));
        assertNull(gui.getInventory().getItem(5));

        gui.setInputItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.DIAMOND));

        assertNotNull(gui.getInventory().getItem(CraftingGUI.GRID_SLOTS[0]));
        assertEquals(Material.DIAMOND, gui.getInventory().getItem(CraftingGUI.GRID_SLOTS[0]).getType());
    }

    @Test
    @DisplayName("clearInput czyści cały grid")
    void clearInputClearsGrid() {
        var player = createPlayer();
        CraftingGUI gui = new CraftingGUI(player, "Test");
        gui.setInputItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.DIAMOND));
        gui.setInputItem(CraftingGUI.GRID_SLOTS[3], new ItemStack(Material.IRON_INGOT));

        gui.clearInput();

        for (int slot : CraftingGUI.GRID_SLOTS) {
            assertNull(gui.getInventory().getItem(slot));
        }
    }
}

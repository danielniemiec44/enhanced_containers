package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.crafting.gui.CraftingGUI;
import pl.com.sofitfy.enhanced_containers.crafting.gui.CraftingGUIListener;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test integracyjny: sprawdzamy zachowanie "jak crafting table" dla vanilla shapeless.
 *
 * Przykład, który zgłaszał gracz:
 * - 3 deski w różnych slotach nie mogą dawać przycisku.
 */
@DisplayName("Integracja - vanilla shapeless bez dodatkowych slotów")
class VanillaShapelessExactnessIntegrationTest extends MockBukkitPluginTestBase {

    @Test
    @DisplayName("Różne liczby itemów mogą pasować do różnych vanilla receptur (deski)")
    void differentAmountsMatchDifferentRecipes() {
        // Zabezpieczenie: w tym teście nie interesują nas custom receptury
        RecipeManager.getInstance().clearRecipes();

        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Case 1: 1 deska -> przycisk
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.OAK_PLANKS, 1));
        listener.updateOutput(player, gui);

        ItemStack output1 = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(output1, "1 deska powinna dać jakiś output");
        assertEquals(Material.OAK_BUTTON, output1.getType(), "1 deska powinna dać dębowy przycisk");

        // Case 2: 2 deski -> może dać pressure plate (to też jest vanilla receptura!)
        gui.getInventory().clear();
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.OAK_PLANKS, 1));
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[1], new ItemStack(Material.OAK_PLANKS, 1));
        listener.updateOutput(player, gui);

        ItemStack output2 = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(output2, "2 deski powinny dać jakiś output");
        // Może to być pressure plate - to jest vanilla receptura!
        assertTrue(
            output2.getType() == Material.OAK_PRESSURE_PLATE,
            "2 deski mogą dać pressure plate (vanilla receptura)"
        );

        // Case 3: Dodatkowy item (STONE) w gridzie blokuje match
        gui.getInventory().clear();
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.OAK_PLANKS, 1));
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[1], new ItemStack(Material.OAK_PLANKS, 1));
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[2], new ItemStack(Material.STONE, 1)); // Dodatkowy!
        listener.updateOutput(player, gui);

        ItemStack output3 = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNull(output3, "Dodatkowy item (stone) powinien zablokować vanilla recepturę desek");
    }
}

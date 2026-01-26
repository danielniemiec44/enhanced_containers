package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.crafting.gui.CraftingGUI;
import pl.com.sofitfy.enhanced_containers.crafting.gui.CraftingGUIListener;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test integracyjny: shapeless powinien działać także, gdy w slocie jest większy stack
 * (np. 5x DANDELION nadal powinno dać YELLOW_DYE).
 */
@DisplayName("Integracja - vanilla shapeless działa ze stackiem")
class VanillaShapelessStackInOneSlotIntegrationTest extends MockBukkitPluginTestBase {

    @Test
    @DisplayName("Shapeless: większy stack w jednym slocie nadal matchuje i zużywa 1 sztukę")
    void shapelessAllowsBiggerStackInSingleSlotAndConsumesOne() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Vanilla: DANDELION -> YELLOW_DYE (shapeless, 1 składnik)
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.DANDELION, 3));
        listener.updateOutput(player, gui);

        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(output, "Output powinien się pojawić dla 3x DANDELION");
        assertEquals(Material.YELLOW_DYE, output.getType(), "Wynik powinien być YELLOW_DYE");
        assertTrue(output.getAmount() >= 1);

        player.openInventory(gui.getInventory());
        player.setItemOnCursor(null); // Pusty kursor - normalny pickup

        InventoryClickEvent clickOutput = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            CraftingGUI.OUTPUT_SLOT,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        listener.onInventoryClick(clickOutput);
        
        // Event jest ZAWSZE anulowany dla output slotu (zapobiegamy exploitom)
        // ALE gracz powinien dostać item na kursor (obsługujemy ręcznie!)
        assertTrue(clickOutput.isCancelled(), 
            "Event MUSI być anulowany (wszystkie kliki na output obsługujemy ręcznie)");
        
        // Najważniejsze: gracz powinien otrzymać output na kursor!
        ItemStack cursor = player.getItemOnCursor();
        assertNotNull(cursor, "Gracz powinien dostać output na kursor!");
        assertEquals(Material.YELLOW_DYE, cursor.getType(), "Na kursorze powinien być YELLOW_DYE!");

        // Składniki zostały pobrane synchronicznie przez handleNormalClickOutput()
        ItemStack remaining = gui.getInventory().getItem(CraftingGUI.GRID_SLOTS[0]);
        assertNotNull(remaining);
        assertEquals(Material.DANDELION, remaining.getType());
        assertEquals(2, remaining.getAmount(), "Po craft powinny zostać 2x DANDELION");

        // Output powinien być przeliczony (zostały 2x DANDELION)
        ItemStack outputAfter = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(outputAfter, "Output powinien się pojawić ponownie (zostały 2x DANDELION)");
        assertEquals(Material.YELLOW_DYE, outputAfter.getType());
    }
}

package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Integracja - drag eventy w CraftingGUI")
class CraftingGUIDragEventIntegrationTest extends MockBukkitPluginTestBase {

    @Test
    @DisplayName("Drag dotykający niewidocznego slotu jest blokowany")
    void dragTouchingInvisibleSlotIsCancelled() {
        PlayerMock player = createPlayer();
        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        int invisibleSlot = 4; // slot poza gridem i poza output
        assertFalse(gui.isVisibleSlot(invisibleSlot));

        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(gui.getInventory());
        when(event.getRawSlots()).thenReturn(Set.of(invisibleSlot));

        listener.onInventoryDrag(event);

        verify(event).setCancelled(true);
    }

    @Test
    @DisplayName("Drag dotykający output slotu jest blokowany")
    void dragTouchingOutputSlotIsCancelled() {
        PlayerMock player = createPlayer();
        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(gui.getInventory());
        when(event.getRawSlots()).thenReturn(Set.of(CraftingGUI.OUTPUT_SLOT));

        listener.onInventoryDrag(event);

        verify(event).setCancelled(true);
    }

    @Test
    @DisplayName("BUG FIX: po drag na input slot output jest przeliczany na następnym ticku")
    void dragOnInputSchedulesOutputRecalculation() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Setup: 1 deska w gridzie powinna dać wynik (np. przycisk).
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.OAK_PLANKS, 1));
        listener.updateOutput(player, gui);

        ItemStack initialOutput = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(initialOutput, "Dla 1 deski output powinien się pojawić");
        assertFalse(initialOutput.getType().isAir());

        // Symulujemy drag: po zakończeniu dragowania w gridzie pojawia się dodatkowy item.
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[1], new ItemStack(Material.DIRT, 1));

        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(gui.getInventory());
        when(event.getRawSlots()).thenReturn(Set.of(CraftingGUI.GRID_SLOTS[1]));

        // Act: event drag + tick
        listener.onInventoryDrag(event);
        verify(event, never()).setCancelled(true);

        tick(1);

        // Assert: output powinien zniknąć, bo mamy dodatkowy zajęty slot (vanilla-like strict).
        ItemStack outputAfter = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNull(outputAfter, "Po dodaniu dodatkowego itemu output powinien zostać wyczyszczony");
    }
}

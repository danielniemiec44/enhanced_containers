package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.Mockito;
import pl.com.sofitfy.enhanced_containers.config.ConfigManager;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeDraft;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RecipeEditor - blokada slotów tła w górnym inventory")
class RecipeEditorSlotLockIT extends MockBukkitPluginTestBase {

    @Test
    @DisplayName("Kliknięcie w slot tła w top inventory jest blokowane")
    void clickOnBackgroundSlotIsCancelled() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        RecipeDraft draft = new RecipeDraft("slotlock1");
        RecipeEditorGUI editor = new RecipeEditorGUI(player, draft);
        RecipeEditorListener listener = new RecipeEditorListener(plugin, new ConfigManager((org.bukkit.plugin.java.JavaPlugin) plugin));
        listener.registerEditor(editor);
        editor.open();

        int backgroundSlot = 4; // top inventory, ale nie jest w GRID_SLOTS i nie jest OUTPUT_SLOT
        assertFalse(RecipeEditorGUI.isGridSlot(backgroundSlot));
        assertFalse(RecipeEditorGUI.isOutputSlot(backgroundSlot));

        InventoryClickEvent click = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            backgroundSlot,
            ClickType.LEFT,
            InventoryAction.PLACE_ALL
        );

        listener.onEditorInventoryClick(click);
        assertTrue(click.isCancelled(), "Sloty tła w górnym inventory powinny być zablokowane");
    }

    @Test
    @DisplayName("Shift-click z ekwipunku gracza trafia tylko do gridu (po 1), a nie do slotów tła")
    void shiftClickFromPlayerInventoryOnlyFillsGrid() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        // Gracz ma 5 diamentów w slocie 0
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));

        RecipeDraft draft = new RecipeDraft("slotlock2");
        RecipeEditorGUI editor = new RecipeEditorGUI(player, draft);
        RecipeEditorListener listener = new RecipeEditorListener(plugin, new ConfigManager((org.bukkit.plugin.java.JavaPlugin) plugin));
        listener.registerEditor(editor);
        editor.open();

        // MockBukkit nie zawsze poprawnie mapuje clickedInventory/slot przy ręcznie tworzonym InventoryClickEvent,
        // więc w tym teście mockujemy event i ustawiamy dane, które handler faktycznie wykorzystuje.
        InventoryClickEvent shiftClick = Mockito.mock(InventoryClickEvent.class);
        Mockito.when(shiftClick.getWhoClicked()).thenReturn(player);
        Mockito.when(shiftClick.getView()).thenReturn(player.getOpenInventory());
        Mockito.when(shiftClick.getAction()).thenReturn(InventoryAction.MOVE_TO_OTHER_INVENTORY);
        Mockito.when(shiftClick.getClickedInventory()).thenReturn(player.getOpenInventory().getBottomInventory());
        Mockito.when(shiftClick.getSlot()).thenReturn(0);
        Mockito.when(shiftClick.getCurrentItem()).thenReturn(player.getInventory().getItem(0));

        listener.onEditorInventoryClick(shiftClick);

        Mockito.verify(shiftClick).setCancelled(true);

        // Cały stack powinien zostać rozłożony do gridu po 1 sztuce na slot.
        int diamondsInGrid = 0;
        for (int slot : RecipeEditorGUI.GRID_SLOTS) {
            ItemStack s = editor.getInventory().getItem(slot);
            if (s != null && s.getType() == Material.DIAMOND) {
                assertEquals(1, s.getAmount(), "W każdym slocie input ma być max 1 sztuka");
                diamondsInGrid += s.getAmount();
            }
        }
        assertEquals(5, diamondsInGrid, "Cały stack ma zostać przeniesiony do gridu");

        // Slot tła (np. 4) ma pozostać pusty
        assertNull(editor.getInventory().getItem(4), "Slot tła nie powinien dostać itemu");

        // Z ekwipunku gracza stack powinien zniknąć (został w całości przeniesiony)
        assertNull(player.getInventory().getItem(0));
    }

    @Test
    @DisplayName("Drag na slot tła w top inventory jest blokowany")
    void dragOverBackgroundSlotIsCancelled() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        RecipeDraft draft = new RecipeDraft("slotlock-drag");
        RecipeEditorGUI editor = new RecipeEditorGUI(player, draft);
        RecipeEditorListener listener = new RecipeEditorListener(plugin, new ConfigManager((org.bukkit.plugin.java.JavaPlugin) plugin));
        listener.registerEditor(editor);
        editor.open();

        int backgroundSlot = 4; // top inventory, ale nie jest w GRID_SLOTS i nie jest OUTPUT_SLOT
        assertFalse(RecipeEditorGUI.isGridSlot(backgroundSlot));
        assertFalse(RecipeEditorGUI.isOutputSlot(backgroundSlot));

        InventoryDragEvent drag = Mockito.mock(InventoryDragEvent.class);
        Mockito.when(drag.getWhoClicked()).thenReturn(player);
        Mockito.when(drag.getView()).thenReturn(player.getOpenInventory());
        Mockito.when(drag.getRawSlots()).thenReturn(Set.of(backgroundSlot));

        listener.onEditorInventoryDrag(drag);

        Mockito.verify(drag).setCancelled(true);
    }
}

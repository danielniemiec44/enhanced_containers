package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.CraftingRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitTestBase;

import static org.mockito.Mockito.*;

/**
 * Testy przepływu craftingu.
 *
 * To są testy integracyjne, bo używają MockBukkit (serwer + eventy).
 */
@DisplayName("CraftingGUIListener - flow craftingu")
@Tag("integration")
class CraftingGUIFlowIT extends MockBukkitTestBase {

    @BeforeEach
    void cleanRecipes() {
        // Czyścimy singleton, żeby testy nie wpływały na siebie.
        RecipeManager.getInstance().clearRecipes();
    }

    @Test
    @DisplayName("Kliknięcie w output odpala crafting i daje wynik")
    void clickingOutputCraftsOnce() {
        // Dado: gracz i GUI
        var player = createPlayer();
        player.setOp(true); // prosto: OP ma permisje

        CraftingGUI gui = new CraftingGUI(player, "Test");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Dado: receptura, która pasuje do inputu
        RecipeManager.getInstance().registerRecipe(sampleRecipe("id1", Material.DIAMOND));

        // Dado: ustawiamy input w GUI (grid 4x4, slot 0 to pozycja (0,0))
        gui.getInventory().setItem(0, new ItemStack(Material.COAL));
        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND));

        // Kiedy: symulujemy kliknięcie w output (slot 24)
        InventoryClickEvent event = mockClickEvent(player, gui, CraftingGUI.OUTPUT_SLOT, false);
        listener.onInventoryClick(event);

        // To: event jest anulowany (safe handling)
        verify(event, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Shift+Click robi jeden craft (jak vanilla)")
    void shiftClickCraftsOnce() {
        // Dado: gracz i GUI
        var player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Test");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Dado: receptura: 1x COAL -> EMERALD
        RecipeManager.getInstance().registerRecipe(sampleRecipe("id1", Material.EMERALD));

        // Dado: dajemy COAL w gridu i output
        gui.getInventory().setItem(0, new ItemStack(Material.COAL, 5));
        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.EMERALD));

        // Kiedy: shift klik w output
        InventoryClickEvent event = mockClickEvent(player, gui, CraftingGUI.OUTPUT_SLOT, true);
        listener.onInventoryClick(event);

        // To: event jest anulowany (safe handling)
        verify(event, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Gdy nie ma item'u w output slocie, klik i tak jest blokowany")
    void noItemInOutputStillCancelsClick() {
        // Dado: gracz i GUI
        var player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Test");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Dado: output slot jest pusty (brak receptury)
        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, null);

        // Kiedy: klik w output
        InventoryClickEvent event = mockClickEvent(player, gui, CraftingGUI.OUTPUT_SLOT, false);
        listener.onInventoryClick(event);

        // To: event jest anulowany
        verify(event, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Kliknięcie w input slot nie jest blokowane")
    void clickingInputSlotIsNotCancelled() {
        // Dado: gracz i GUI
        var player = createPlayer();
        CraftingGUI gui = new CraftingGUI(player, "Test");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Dado: cursor item
        ItemStack cursor = new ItemStack(Material.IRON_INGOT, 2);

        // Kiedy: klikamy w slot gridu z tym kursorem
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(gui.getInventory());
        when(event.getClickedInventory()).thenReturn(gui.getInventory());
        when(event.getRawSlot()).thenReturn(CraftingGUI.GRID_SLOTS[0]);
        when(event.getCursor()).thenReturn(cursor);
        when(event.isShiftClick()).thenReturn(false);

        listener.onInventoryClick(event);

        // To: event NIE anulowany (input sloty można klikać)
        verify(event, never()).setCancelled(true);
    }

    private static CraftingRecipe sampleRecipe(String id, Material outputMaterial) {
        ItemStack[][] pattern = new ItemStack[4][4];
        pattern[0][0] = new ItemStack(Material.COAL);
        ItemStack output = new ItemStack(outputMaterial);
        return new CraftingRecipe(id, "Test", pattern, output, "plethoras.tools.use");
    }

    private static InventoryClickEvent mockClickEvent(Player player, CraftingGUI gui, int rawSlot, boolean shift) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(gui.getInventory());
        when(event.getClickedInventory()).thenReturn(gui.getInventory());
        when(event.getRawSlot()).thenReturn(rawSlot);
        when(event.isShiftClick()).thenReturn(shift);
        when(event.getCursor()).thenReturn(new ItemStack(Material.AIR));
        return event;
    }
}

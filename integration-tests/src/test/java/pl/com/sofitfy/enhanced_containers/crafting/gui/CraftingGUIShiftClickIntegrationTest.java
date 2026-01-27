package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.CraftingRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test integracyjny dla kluczowego buga:
 * shift-click z ekwipunku gracza potrafił wrzucić itemy w ukryte sloty GUI.
 *
 * Po poprawce: shift-click jest przechwytywany i itemy trafiają tylko do slotów gridu 4x4.
 */
@DisplayName("Integracja - shift-click do gridu 4x4")
class CraftingGUIShiftClickIntegrationTest extends MockBukkitPluginTestBase {

    @Test
    @DisplayName("Shift-click przerzuca itemy tylko do gridu i planuje updateOutput")
    void shiftClickMovesOnlyIntoGridAndUpdatesOutput() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Gracz ma item w slocie 9 ekwipunku (pierwszy slot main inventory, nie hotbar)
        // Slot 9 w PlayerInventory to pierwszy slot "main inventory" (nad hotbarem)
        ItemStack planks = new ItemStack(Material.OAK_PLANKS, 20);
        player.getInventory().setItem(9, planks);

        // Otwieramy GUI, żeby mieć InventoryView
        player.openInventory(gui.getInventory());

        // Rozmiar top inventory (GUI)
        int topSize = gui.getInventory().getSize();
        
        // W Minecraft inventory view dla GUI + player inventory:
        // - Raw slots 0 do topSize-1: top inventory (GUI)
        // - Raw slots topSize do topSize+26: player main inventory (sloty 9-35 w PlayerInventory)
        // 
        // Gracz ma item w player.getInventory() slot 9 (pierwszy slot main inventory)
        // więc raw slot = topSize + 0 = 54
        int rawSlotForPlayerSlot9 = topSize; // = 54
        
        // Tworzymy InventoryClickEvent ręcznie i wywołujemy listener bezpośrednio
        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            rawSlotForPlayerSlot9, // raw slot dla player inventory slot 9
            ClickType.SHIFT_LEFT,
            InventoryAction.MOVE_TO_OTHER_INVENTORY
        );
        
        // Bezpośrednie wywołanie listenera (jak w testach SharedCrafting)
        listener.onInventoryClick(event);

        // Assert: W pierwszym slocie gridu powinno się pojawić 20 desek
        ItemStack inGrid = gui.getInventory().getItem(CraftingGUI.GRID_SLOTS[0]);
        assertNotNull(inGrid, "Item powinien być przeniesiony do gridu");
        assertEquals(Material.OAK_PLANKS, inGrid.getType());
        assertEquals(20, inGrid.getAmount());

        // Slot w ekwipunku gracza powinien zostać wyczyszczony
        ItemStack remaining = player.getInventory().getItem(9);
        assertTrue(remaining == null || remaining.getType().isAir(), 
            "Slot gracza powinien być pusty po przeniesieniu");

        // UpdateOutput jest planowany na następny tick
        tick(1);
        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(output, "Po wrzuceniu desek wynik powinien się pojawić");
    }

    @Test
    @DisplayName("Shift-click na output - craftuje wielokrotnie aż receptura się zmieni")
    void shiftClickOutputCraftsMultipleTimes() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Przygotowujemy recepturę - prosty 1x1: jeden DIAMOND -> jeden DIAMOND_BLOCK
        ItemStack[][] pattern = new ItemStack[1][1];
        pattern[0][0] = new ItemStack(Material.DIAMOND);

        CraftingRecipe recipe = new CraftingRecipe(
            "diamond_to_block",
            "Diamond to Block",
            pattern,
            new ItemStack(Material.DIAMOND_BLOCK, 1),
            "*"
        );

        RecipeManager.getInstance().registerRecipe(recipe);

        // Dodajemy 5 diamenty do gridu (aby craftować 5 razy)
        gui.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5)); // slot [0,0]

        // Otwieramy GUI
        player.openInventory(gui.getInventory());

        // Ręcznie ustawiamy output (jak by to było po updateOutput)
        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND_BLOCK, 1));

        // Tworzymy event shift-click na output slot
        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            CraftingGUI.OUTPUT_SLOT,
            ClickType.SHIFT_LEFT,
            InventoryAction.PICKUP_ALL
        );

        // Act - shift-click na output
        listener.onInventoryClick(event);

        // Assert: event został anulowany (bo się craftuje)
        assertTrue(event.isCancelled());

        // Czekamy na wykonanie async craftingu
        // Async task wykonuje się w tle i planuje callback na main thread
        // Musimy dać czas na zakończenie async i potem wykonać scheduled task
        try {
            Thread.sleep(100); // Daj czas na async task
        } catch (InterruptedException e) {
            // ignore
        }
        tick(1); // Wykonaj scheduled callback z async task;

        // Po craftingu: gracz powinien mieć 5 bloków diamantu w inventorzu
        // (bo było 5 diamenty = można craftować 5 razy)
        int totalBlocks = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND_BLOCK) {
                totalBlocks += item.getAmount();
            }
        }

        assertEquals(5, totalBlocks, "Gracz powinien mieć 5 bloków diamantu (5 diamenty = 5 crafty)");

        // Grid powinien być pusty (wszystkie diamenty zużyte)
        ItemStack gridItem = gui.getInventory().getItem(0);
        assertTrue(gridItem == null || gridItem.getType().isAir(), "Grid powinien być pusty po craftingu");

        // Output slot powinien być pusty (bo nie ma więcej składników)
        ItemStack outputAfter = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertTrue(outputAfter == null || outputAfter.getType().isAir(), "Output slot powinien być pusty");
    }

    @Test
    @DisplayName("Shift-click na output - nie craftuje gdy inventory pełne (vanilla behavior)")
    void shiftClickOutputDoesNotCraftWhenInventoryFull() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Przygotowujemy recepturę
        ItemStack[][] pattern = new ItemStack[1][1];
        pattern[0][0] = new ItemStack(Material.DIAMOND);

        CraftingRecipe recipe = new CraftingRecipe(
            "diamond_to_block",
            "Diamond to Block",
            pattern,
            new ItemStack(Material.DIAMOND_BLOCK, 1),
            "*"
        );

        RecipeManager.getInstance().registerRecipe(recipe);

        // Wrzucamy diamenty do gridu
        gui.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 2));

        // Otwieramy GUI
        player.openInventory(gui.getInventory());

        // Zapełniamy inventarz gracza (pełny ekwipunek)
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            player.getInventory().setItem(i, new ItemStack(Material.STONE, 64));
        }

        // Ręcznie ustawiamy output
        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND_BLOCK, 1));

        // Tworzymy event shift-click na output
        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            CraftingGUI.OUTPUT_SLOT,
            ClickType.SHIFT_LEFT,
            InventoryAction.PICKUP_ALL
        );

        // Act
        listener.onInventoryClick(event);

        // Assert: event został anulowany (obsługiwany przez nasz listener)
        assertTrue(event.isCancelled(), "Event powinien być anulowany");
        
        // Czekamy na wykonanie async craftingu i scheduled tasków
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // ignore
        }
        
        // Wykonaj ticki aby async task mógł się zakończyć
        for (int i = 0; i < 10; i++) {
            tick(1);
        }

        // Vanilla behavior: gdy inventory jest pełne, shift-click NIE konsumuje składników
        // Diamenty powinny pozostać w gridzie (2 sztuki)
        ItemStack gridItem = gui.getInventory().getItem(0);
        assertNotNull(gridItem, "Diamenty powinny pozostać w gridzie (inventory pełne)");
        assertEquals(Material.DIAMOND, gridItem.getType(), "Typ materiału nie powinien się zmienić");
        assertEquals(2, gridItem.getAmount(), 
            "Ilość diamentów powinna pozostać niezmieniona (2) gdy inventory jest pełne");
    }
}

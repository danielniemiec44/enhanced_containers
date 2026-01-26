package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integracyjne testy "nietypowych" akcji w inventory.
 *
 * Cel:
 * - Sprawdzamy zachowania, które na normalnym crafting table są zablokowane
 *   albo działają inaczej i często są źródłem duplikacji.
 * - Interesuje nas bezpieczeństwo: output slot ma być read-only.
 * - Przy okazji sprawdzamy też bezpieczeństwo niewidocznych slotów.
 */
@DisplayName("Nietypowe akcje inventory - integracja")
class CraftingGUIAbnormalInventoryActionsIntegrationTest extends MockBukkitPluginTestBase {

    @Test
    @DisplayName("SWAP_OFFHAND na output jest zablokowany i nic nie zmienia")
    void swapOffhandOnOutput_IsBlockedAndDoesNotChangeState() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND_BLOCK, 1));
        player.getInventory().setItemInOffHand(new ItemStack(Material.COBBLESTONE, 32));

        player.openInventory(gui.getInventory());

        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            CraftingGUI.OUTPUT_SLOT,
            ClickType.SWAP_OFFHAND,
            InventoryAction.SWAP_WITH_CURSOR
        );

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled(), "SWAP_OFFHAND na output musi być zablokowany");

        // Output nie może się zmienić
        ItemStack outputAfter = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(outputAfter);
        assertEquals(Material.DIAMOND_BLOCK, outputAfter.getType());

        // Offhand nie może się zmienić
        ItemStack offhandAfter = player.getInventory().getItemInOffHand();
        assertNotNull(offhandAfter);
        assertEquals(Material.COBBLESTONE, offhandAfter.getType());
        assertEquals(32, offhandAfter.getAmount());
    }

    @Test
    @DisplayName("HOTBAR_MOVE_AND_READD na output jest zablokowany")
    void hotbarMoveAndReaddOnOutput_IsBlocked() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND_BLOCK, 1));
        player.getInventory().setItem(0, new ItemStack(Material.COBBLESTONE, 64));

        player.openInventory(gui.getInventory());

        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            CraftingGUI.OUTPUT_SLOT,
            ClickType.NUMBER_KEY,
            InventoryAction.HOTBAR_MOVE_AND_READD
        );

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled(), "HOTBAR_MOVE_AND_READD na output musi być zablokowany");

        ItemStack outputAfter = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(outputAfter);
        assertEquals(Material.DIAMOND_BLOCK, outputAfter.getType());
    }

    @Test
    @DisplayName("COLLECT_TO_CURSOR na output z pustym kursorem jest zablokowany")
    void collectToCursorOnOutputWithEmptyCursor_IsBlocked() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND_BLOCK, 1));

        player.openInventory(gui.getInventory());
        player.setItemOnCursor(null);

        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            CraftingGUI.OUTPUT_SLOT,
            ClickType.DOUBLE_CLICK,
            InventoryAction.COLLECT_TO_CURSOR
        );

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled(), "COLLECT_TO_CURSOR na output musi być zablokowany");
        // MockBukkit czasem reprezentuje pusty kursor jako AIR x 0 zamiast null
        ItemStack cursorAfter = player.getItemOnCursor();
        assertTrue(cursorAfter == null || cursorAfter.getType().isAir(), "Kursor powinien pozostać pusty");

        ItemStack outputAfter = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(outputAfter);
        assertEquals(Material.DIAMOND_BLOCK, outputAfter.getType());
    }

    @Test
    @DisplayName("Drag zawierający niewidoczny slot jest blokowany")
    void dragThatTouchesInvisibleSlot_IsBlocked() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        player.openInventory(gui.getInventory());

        // Slot 4 jest niewidoczny (nie grid i nie output)
        Map<Integer, ItemStack> newItems = new HashMap<>();
        newItems.put(4, new ItemStack(Material.STONE, 1));

        InventoryDragEvent event = new InventoryDragEvent(
            player.getOpenInventory(),
            new ItemStack(Material.STONE, 1),
            new ItemStack(Material.STONE, 1),
            false,
            newItems
        );

        listener.onInventoryDrag(event);

        assertTrue(event.isCancelled(), "Drag dotykający niewidocznego slotu musi być zablokowany");
    }

    @Test
    @DisplayName("Drag zawierający output slot jest blokowany nawet gdy są też input sloty")
    void dragThatTouchesOutputAlongsideInput_IsBlocked() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        player.openInventory(gui.getInventory());

        Map<Integer, ItemStack> newItems = new HashMap<>();
        newItems.put(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.DIAMOND, 1));
        newItems.put(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.COBBLESTONE, 1));

        InventoryDragEvent event = new InventoryDragEvent(
            player.getOpenInventory(),
            new ItemStack(Material.DIAMOND, 1),
            new ItemStack(Material.DIAMOND, 1),
            false,
            newItems
        );

        listener.onInventoryDrag(event);

        assertTrue(event.isCancelled(), "Jeśli drag dotknie outputu, to musi być zablokowany");
    }

    @Test
    @DisplayName("Zamknięcie GUI zwraca też itemy z niewidocznych slotów (bez duplikacji)")
    void closingGui_ReturnsItemsFromInvisibleSlotsToo() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Symulujemy zły stan: item wylądował w niewidocznym slocie.
        // To nie powinno się zdarzyć w normalnej grze, ale musimy być odporni.
        gui.getInventory().setItem(4, new ItemStack(Material.DIAMOND, 2));

        // I do tego normalny item w gridzie
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.DIAMOND, 3));

        player.openInventory(gui.getInventory());
        InventoryCloseEvent closeEvent = new InventoryCloseEvent(player.getOpenInventory());

        listener.onInventoryClose(closeEvent);

        // Gracz powinien dostać łącznie 5 diamentów
        int totalDiamonds = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                totalDiamonds += item.getAmount();
            }
        }

        assertEquals(5, totalDiamonds, "Gracz powinien odzyskać wszystkie diamenty z GUI");

        // GUI powinno być wyczyszczone, żeby nie było duplikacji
        assertNull(gui.getInventory().getItem(4), "Niewidoczny slot powinien być wyczyszczony");
        ItemStack gridAfter = gui.getInventory().getItem(CraftingGUI.GRID_SLOTS[0]);
        assertTrue(gridAfter == null || gridAfter.getType().isAir(), "Grid powinien być wyczyszczony");
    }
}

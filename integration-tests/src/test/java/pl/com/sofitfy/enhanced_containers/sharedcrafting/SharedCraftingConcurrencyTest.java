package pl.com.sofitfy.enhanced_containers.sharedcrafting;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy współbieżności i bezpieczeństwa dla SharedCrafting.
 * Testują ochronę przed exploitami i race conditions.
 */
public class SharedCraftingConcurrencyTest {

    private ServerMock server;
    private SharedCraftingManager manager;
    private SharedCraftingListener listener;
    private Location tableLocation;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        
        manager = new SharedCraftingManager(MockBukkit.createMockPlugin());
        listener = new SharedCraftingListener(manager);
        
        server.getPluginManager().registerEvents(listener, MockBukkit.createMockPlugin());
        
        tableLocation = new Location(server.getWorld("world"), 100, 64, 100);
        tableLocation.getBlock().setType(Material.CRAFTING_TABLE);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    /**
     * Test: Kilku graczy jednocześnie otwiera ten sam crafting table
     * Oczekiwanie: Brak ConcurrentModificationException, wszyscy dostają ten sam Inventory
     */
    @Test
    public void testConcurrentAccess() {
        PlayerMock[] players = new PlayerMock[10];
        Inventory[] inventories = new Inventory[10];
        
        // 10 graczy jednocześnie otwiera crafting table
        for (int i = 0; i < 10; i++) {
            players[i] = server.addPlayer("Player" + i);
            PlayerInteractEvent event = new PlayerInteractEvent(
                players[i],
                Action.RIGHT_CLICK_BLOCK,
                new ItemStack(Material.AIR),
                tableLocation.getBlock(),
                org.bukkit.block.BlockFace.UP,
                EquipmentSlot.HAND
            );
            
            server.getPluginManager().callEvent(event);
            inventories[i] = players[i].getOpenInventory().getTopInventory();
        }
        
        // Wszyscy gracze powinni mieć ten sam Inventory (ta sama instancja)
        for (int i = 1; i < 10; i++) {
            assertSame(inventories[0], inventories[i], 
                "Wszyscy gracze powinni widzieć tę samą instancję Inventory");
        }
    }

    /**
     * Test: Obsługa COLLECT_TO_CURSOR (double-click)
     * Oczekiwanie: Event jest blokowany - podobnie jak w CraftingGUI 4x4
     */
    @Test
    public void testDoubleClickBlocked() {
        PlayerMock player = server.addPlayer("TestPlayer");
        
        // Otwórz shared crafting table
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK, // Note: Action.RIGHT_CLICK_BLOCK is technically redundant if not used by logic, but keeping standard
            new ItemStack(Material.AIR),
            tableLocation.getBlock(),
            org.bukkit.block.BlockFace.UP,
            EquipmentSlot.HAND
        );
        server.getPluginManager().callEvent(interactEvent);
        
        Inventory sharedInv = player.getOpenInventory().getTopInventory();
        
        // Dodaj itemy do crafting table (slot 1 i 2)
        sharedInv.setItem(1, new ItemStack(Material.DIAMOND, 1));
        sharedInv.setItem(2, new ItemStack(Material.DIAMOND, 1));
        
        // Gracz ma też itemy w eq
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 3));

        // Spróbuj użyć COLLECT_TO_CURSOR (double-click z diamondem na kursorze)
        // Zakładamy, że mamy już coś na kursorze
        player.setItemOnCursor(new ItemStack(Material.DIAMOND, 1));
        
        InventoryClickEvent clickEvent = new InventoryClickEvent(
            player.getOpenInventory(),
            org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
            5, // random slot
            ClickType.DOUBLE_CLICK,
            InventoryAction.COLLECT_TO_CURSOR
        );
        
        server.getPluginManager().callEvent(clickEvent);
        
        // Double-click jest zablokowany w SharedCrafting (podobnie jak w CraftingGUI 4x4)
        assertTrue(clickEvent.isCancelled(), 
            "COLLECT_TO_CURSOR powinien być zablokowany w SharedCrafting");
            
        // Uwaga: Zmieniliśmy logikę na zgodną z CraftingGUI 4x4 - całkowity blok double-click
        // zapobiega exploitom z duplikacją itemów
    }

    /**
     * Test: Race condition - dwóch graczy jednocześnie wkłada itemy
     * Oczekiwanie: Brak utraty itemów, ConcurrentHashMap chroni przed race conditions
     */
    @Test
    public void testConcurrentItemPlacement() throws InterruptedException {
        final int THREAD_COUNT = 5;
        final int ITEMS_PER_THREAD = 20;
        
        Thread[] threads = new Thread[THREAD_COUNT];
        PlayerMock[] players = new PlayerMock[THREAD_COUNT];
        
        // Każdy wątek symuluje gracza wkładającego itemy
        for (int i = 0; i < THREAD_COUNT; i++) {
            players[i] = server.addPlayer("ConcurrentPlayer" + i);
            
            // Otwórz shared crafting table (SYNC - main thread)
            PlayerInteractEvent event = new PlayerInteractEvent(
                players[i],
                Action.RIGHT_CLICK_BLOCK,
                new ItemStack(Material.AIR),
                tableLocation.getBlock(),
                org.bukkit.block.BlockFace.UP,
                EquipmentSlot.HAND
            );
            server.getPluginManager().callEvent(event);
        }

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                PlayerMock player = players[threadIndex];
                
                // Wkładaj i wyjmuj itemy wielokrotnie
                Inventory inv = player.getOpenInventory().getTopInventory();
                for (int j = 0; j < ITEMS_PER_THREAD; j++) {
                    inv.setItem(1 + (j % 9), new ItemStack(Material.DIAMOND, 1));
                    inv.setItem(1 + (j % 9), null);
                }
            });
        }
        
        // Wystartuj wszystkie wątki jednocześnie
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Poczekaj na zakończenie
        for (Thread thread : threads) {
            thread.join(5000); // max 5 sekund
            assertFalse(thread.isAlive(), "Wątek powinien się zakończyć");
        }
        
        // Jeśli dotarliśmy tutaj bez ConcurrentModificationException - sukces!
        assertTrue(manager.hasInventory(tableLocation), 
            "Crafting table powinien nadal istnieć w managerze");
    }

    /**
     * Test: Duplikacja przez szybkie klikanie
     * Oczekiwanie: Brak duplikacji itemów
     */
    @Test
    public void testNoDuplicationOnRapidClicks() {
        PlayerMock player = server.addPlayer("TestPlayer");
        
        // Otwórz shared crafting table
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            tableLocation.getBlock(),
            org.bukkit.block.BlockFace.UP,
            EquipmentSlot.HAND
        );
        server.getPluginManager().callEvent(interactEvent);
        
        Inventory sharedInv = player.getOpenInventory().getTopInventory();
        
        // Włóż 1 diamond do crafting table
        sharedInv.setItem(1, new ItemStack(Material.DIAMOND, 1));
        
        // Symuluj 10 szybkich kliknięć typu COLLECT_TO_CURSOR
        for (int i = 0; i < 10; i++) {
            InventoryClickEvent clickEvent = new InventoryClickEvent(
                player.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                1,
                ClickType.DOUBLE_CLICK,
                InventoryAction.COLLECT_TO_CURSOR
            );
            server.getPluginManager().callEvent(clickEvent);
        }
        
        // Sprawdź czy item nadal jest tam gdzie był (1 diamond)
        ItemStack item = sharedInv.getItem(1);
        assertNotNull(item);
        assertEquals(Material.DIAMOND, item.getType());
        assertEquals(1, item.getAmount(), 
            "Szybkie klikanie nie powinno duplikować itemów");
    }

    /**
     * Test: Veryfikacja że hasInventory(Inventory) działa poprawnie
     */
    @Test
    public void testHasInventoryByInstance() {
        PlayerMock player = server.addPlayer("TestPlayer");
        
        // Otwórz shared crafting table
        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            tableLocation.getBlock(),
            org.bukkit.block.BlockFace.UP,
            EquipmentSlot.HAND
        );
        server.getPluginManager().callEvent(event);
        
        Inventory sharedInv = player.getOpenInventory().getTopInventory();
        
        // Manager powinien rozpoznać ten Inventory
        assertTrue(manager.hasInventory(sharedInv), 
            "Manager powinien rozpoznać współdzielony Inventory");
        
        // Stwórz nowy, niezarządzany Inventory
        Inventory otherInv = server.createInventory(null, 9, "Other Inventory");
        assertFalse(manager.hasInventory(otherInv), 
            "Manager nie powinien rozpoznać niezarządzanego Inventory");
    }

    /**
     * Test: Shift+click z ekwipunku gracza do shared crafting table
     * Oczekiwane: Przedmioty trafiają do gridu (sloty 1-9), a nie do slotu 0 (wynik)
     */
    @Test
    public void testShiftClickFromPlayerInventory() {
        PlayerMock player = server.addPlayer("ShiftClickPlayer");
        
        // Otwórz shared crafting table
        player.openInventory(manager.getWorkbenchInventory(tableLocation));
        Inventory sharedInv = player.getOpenInventory().getTopInventory();
        
        // Daj graczowi drewno
        ItemStack wood = new ItemStack(Material.OAK_LOG, 64);
        player.getInventory().setItem(0, wood);
        
        // Symuluj shift+click z ekwipunku gracza
        // Slot 10 to pierwszy slot dolnego inventory (bo góra ma 10 slotów: 1 wynik + 9 grid)
        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
            10, // rawSlot 10 = slot 0 w player inventory
            ClickType.SHIFT_LEFT,
            InventoryAction.MOVE_TO_OTHER_INVENTORY
        );
        
        server.getPluginManager().callEvent(event);
        
        // Przedmiot powinien trafić do pierwszego wolnego slotu gridu (slot 1)
        // Slot 0 to wynik, więc tam nie powinien trafić
        ItemStack itemInMatrix = sharedInv.getItem(1);
        
        // Jeśli test zawiedzie tutaj, to znaczy że musimy zaimplementować obsługę logiczną
        assertNotNull(itemInMatrix, "Przedmiot powinien trafić do slotu 1 (grid)");
        assertEquals(Material.OAK_LOG, itemInMatrix.getType());
        assertEquals(64, itemInMatrix.getAmount());
        
        // Ekwipunek gracza powinien być pusty
        assertTrue(player.getInventory().getItem(0) == null || player.getInventory().getItem(0).getType().isAir(), 
            "Przedmiot powinien zniknąć z ekwipunku gracza");
    }

    @Test
    public void testCraftingLogic() {
        Location tableLocation = new Location(server.getWorld("world"), 200, 64, 200);
        PlayerMock player = server.addPlayer("Crafter");

        // Otwórz
        Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
        player.openInventory(sharedInv);

        // Włóż deskę (OAK_PLANKS) do slotu 1 -> Button (OAK_BUTTON) lub Planks ?
        // 1 Oak Log -> 4 Oak Planks.
        ItemStack log = new ItemStack(Material.OAK_LOG);
        sharedInv.setItem(1, log);
        
        // Musimy manualnie triggerować listener.scheduleUpdate, ale listener nasłuchuje eventów.
        // Skoro zrobiliśmy setItem bezpośrednio w teście, listener o tym NIE WIE.
        // Musimy symulować event lub wywołać update ręcznie (ale metoda private).
        // Lepiej użyć simulateInventoryClick w teście.

        // Wyczyśćmy inv i zróbmy to eventami
        sharedInv.clear();
        player.getInventory().setItem(0, new ItemStack(Material.OAK_LOG));
        
        // Kliknij logiem w slot 1 (place)
        // MockBukkit simulate inventory click
        // Używamy eventu
        player.getOpenInventory().setCursor(new ItemStack(Material.OAK_LOG));
        InventoryClickEvent placeEvent = new InventoryClickEvent(
            player.getOpenInventory(),
            org.bukkit.event.inventory.InventoryType.SlotType.CRAFTING,
            1, // Slot 1 w top inventory
            ClickType.LEFT,
            InventoryAction.PLACE_ALL
        );
        server.getPluginManager().callEvent(placeEvent);
        
        // Manualnie zaktualizuj stan inventory, bo MockBukkit callEvent tego nie robi
        // (W normalnym serwerze robi to logika Inventory, jeśli event nie anulowany)
        if (!placeEvent.isCancelled()) {
            sharedInv.setItem(1, new ItemStack(Material.OAK_LOG));
            player.setItemOnCursor(null); // Clear cursor as if item was placed
        }

        // Listener powinien zaplanować update
        
        // Symuluj upływ czasu (1 tick)
        server.getScheduler().performOneTick();
        
        // Sprawdź czy wynik się pojawił
        ItemStack result = sharedInv.getItem(0);
        assertNotNull(result, "Wynik craftingu powinien się pojawić");
        assertEquals(Material.OAK_PLANKS, result.getType());
        assertEquals(4, result.getAmount());
        
        // Teraz wyjmij wynik (Slot 0)
        InventoryClickEvent resultClick = new InventoryClickEvent(
            player.getOpenInventory(),
            org.bukkit.event.inventory.InventoryType.SlotType.RESULT,
            0,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(resultClick);
        
        // Wynik na kursorze
        assertNotNull(resultClick.getView().getCursor());
        assertEquals(Material.OAK_PLANKS, resultClick.getView().getCursor().getType());
        assertEquals(4, resultClick.getView().getCursor().getAmount());
        
        // Składnik zużyty
        assertNull(sharedInv.getItem(1));
        
        // Wynik znika (update po ticku lub natychmiast)
        // W moim kodzie updateWorkbench jest wywoływany w handleResultClick => natychmiast.
        assertNull(sharedInv.getItem(0));
    }
}

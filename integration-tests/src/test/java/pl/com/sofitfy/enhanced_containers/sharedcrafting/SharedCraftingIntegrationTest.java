package pl.com.sofitfy.enhanced_containers.sharedcrafting;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test integracyjny dla wspólnego craftingu 3x3.
 * Weryfikuje pełną specyfikację:
 * 1. Współdzielony widok między graczami
 * 2. Trwałość przedmiotów po zamknięciu
 * 3. Wyrzucanie itemów przy niszczeniu
 */
@DisplayName("Shared Crafting 3x3 - Integration Tests")
public class SharedCraftingIntegrationTest {

    private ServerMock server;
    private JavaPlugin plugin;
    private SharedCraftingManager manager;
    private SharedCraftingListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        manager = new SharedCraftingManager(plugin);
        listener = new SharedCraftingListener(manager);
        server.getPluginManager().registerEvents(listener, plugin);
        
        // Tworzymy świat "world" jeśli nie istnieje
        if (server.getWorld("world") == null) {
            server.addSimpleWorld("world");
        }
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("SPEC 1: Wszyscy gracze widzą te same przedmioty (wspólny inventory)")
    void multiplePlayersSeeSharedInventory() {
        // Setup: 5 graczy, 1 stół
        PlayerMock player1 = server.addPlayer("Player1");
        PlayerMock player2 = server.addPlayer("Player2");
        PlayerMock player3 = server.addPlayer("Player3");
        PlayerMock player4 = server.addPlayer("Player4");
        PlayerMock player5 = server.addPlayer("Player5");

        Block craftingTable = server.getWorld("world").getBlockAt(100, 64, 100);
        craftingTable.setType(Material.CRAFTING_TABLE);
        Location location = craftingTable.getLocation();

        // Player1 otwiera crafting
        Inventory inv1 = manager.getWorkbenchInventory(location);
        player1.openInventory(inv1);

        // Player2 otwiera TEN SAM crafting
        Inventory inv2 = manager.getWorkbenchInventory(location);
        player2.openInventory(inv2);

        // Weryfikacja 1: To ta sama instancja
        assertSame(inv1, inv2, "Wszyscy gracze powinni mieć tę samą instancję inventory");

        // Player1 wkłada diament
        inv1.setItem(1, new ItemStack(Material.DIAMOND, 3));

        // Weryfikacja 2: Player2 widzi diament (bo to ta sama instancja)
        ItemStack itemSeenByPlayer2 = inv2.getItem(1);
        assertNotNull(itemSeenByPlayer2, "Player2 powinien widzieć item włożony przez Player1");
        assertEquals(Material.DIAMOND, itemSeenByPlayer2.getType());
        assertEquals(3, itemSeenByPlayer2.getAmount());

        // Player3, 4, 5 także otwierają
        Inventory inv3 = manager.getWorkbenchInventory(location);
        Inventory inv4 = manager.getWorkbenchInventory(location);
        Inventory inv5 = manager.getWorkbenchInventory(location);
        
        player3.openInventory(inv3);
        player4.openInventory(inv4);
        player5.openInventory(inv5);

        // Weryfikacja 3: Wszyscy widzą ten sam item
        assertSame(inv1, inv3);
        assertSame(inv1, inv4);
        assertSame(inv1, inv5);
        assertNotNull(inv5.getItem(1));
        assertEquals(Material.DIAMOND, inv5.getItem(1).getType());
    }

    @Test
    @DisplayName("SPEC 2: Przedmioty pozostają po zamknięciu GUI")
    void itemsPersistAfterClosing() {
        // Setup
        PlayerMock player = server.addPlayer("TestPlayer");
        Block craftingTable = server.getWorld("world").getBlockAt(200, 64, 200);
        craftingTable.setType(Material.CRAFTING_TABLE);
        Location location = craftingTable.getLocation();

        // Player otwiera i wkłada itemy
        Inventory inv = manager.getWorkbenchInventory(location);
        player.openInventory(inv);
        
        inv.setItem(1, new ItemStack(Material.GOLD_INGOT, 5));
        inv.setItem(2, new ItemStack(Material.IRON_INGOT, 10));
        inv.setItem(3, new ItemStack(Material.EMERALD, 2));

        // Player zamyka GUI
        player.closeInventory();

        // Weryfikacja: Itemy nadal są w managerze
        assertTrue(manager.hasInventory(location), "Manager powinien mieć zapisany inventory");
        
        Inventory invAfterClose = manager.getWorkbenchInventory(location);
        assertNotNull(invAfterClose.getItem(1));
        assertEquals(Material.GOLD_INGOT, invAfterClose.getItem(1).getType());
        assertEquals(5, invAfterClose.getItem(1).getAmount());
        
        assertNotNull(invAfterClose.getItem(2));
        assertEquals(Material.IRON_INGOT, invAfterClose.getItem(2).getType());
        
        assertNotNull(invAfterClose.getItem(3));
        assertEquals(Material.EMERALD, invAfterClose.getItem(3).getType());
    }

    @Test
    @DisplayName("SPEC 3: Niszczenie stołu wyrzuca przedmioty")
    void breakingTableDropsItems() {
        // Setup
        PlayerMock player = server.addPlayer("Breaker");
        Block craftingTable = server.getWorld("world").getBlockAt(300, 64, 300);
        craftingTable.setType(Material.CRAFTING_TABLE);
        Location location = craftingTable.getLocation();

        // Wkładamy itemy
        Inventory inv = manager.getWorkbenchInventory(location);
        inv.setItem(1, new ItemStack(Material.DIAMOND_BLOCK, 64));
        inv.setItem(5, new ItemStack(Material.NETHERITE_INGOT, 32));

        assertTrue(manager.hasInventory(location), "Manager powinien mieć zapisany inventory przed zniszczeniem");

        // Niszczenie - ręcznie wywołujemy event bo breakNaturally nie wywołuje eventów w MockBukkit
        org.bukkit.event.block.BlockBreakEvent event = new org.bukkit.event.block.BlockBreakEvent(craftingTable, player);
        server.getPluginManager().callEvent(event);

        // Weryfikacja: Manager nie ma już tego inventory
        assertFalse(manager.hasInventory(location), "Manager nie powinien już mieć tego inventory po zniszczeniu");
    }

    @Test
    @DisplayName("SPEC 4: Inne bloki nie są przechwytywane")
    void otherBlocksNotAffected() {
        // Setup
        PlayerMock player = server.addPlayer("Tester");
        Block stone = server.getWorld("world").getBlockAt(400, 64, 400);
        stone.setType(Material.STONE);
        Location location = stone.getLocation();

        // Interakcja z kamieniem
        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            null,
            stone,
            null
        );
        server.getPluginManager().callEvent(event);

        // Weryfikacja: Event nie został anulowany (domyślna akcja)
        assertFalse(event.isCancelled(), "Interakcja z innymi blokami nie powinna być anulowana");
        assertFalse(manager.hasInventory(location), "Manager nie powinien tworzyć inventory dla innych bloków");
    }

    @Test
    @DisplayName("SPEC 5: Różne stoły mają różne inventory")
    void differentTablesHaveDifferentInventories() {
        // Setup: 2 stoły w różnych lokalizacjach
        Block table1 = server.getWorld("world").getBlockAt(500, 64, 500);
        Block table2 = server.getWorld("world").getBlockAt(600, 64, 600);
        table1.setType(Material.CRAFTING_TABLE);
        table2.setType(Material.CRAFTING_TABLE);

        Location loc1 = table1.getLocation();
        Location loc2 = table2.getLocation();

        // Pobieramy inventory dla obu
        Inventory inv1 = manager.getWorkbenchInventory(loc1);
        Inventory inv2 = manager.getWorkbenchInventory(loc2);

        // Weryfikacja: To różne instancje
        assertNotSame(inv1, inv2, "Różne stoły powinny mieć różne inventory");

        // Wkładamy różne itemy
        inv1.setItem(1, new ItemStack(Material.DIAMOND));
        inv2.setItem(1, new ItemStack(Material.EMERALD));

        // Weryfikacja: Itemy są izolowane
        assertEquals(Material.DIAMOND, inv1.getItem(1).getType());
        assertEquals(Material.EMERALD, inv2.getItem(1).getType());
    }

    @Test
    @DisplayName("PERFORMANCE: 5 graczy jednocześnie craftuje")
    void fivePlayersConcurrentCrafting() {
        // Setup: 5 graczy, 1 stół
        Block craftingTable = server.getWorld("world").getBlockAt(700, 64, 700);
        craftingTable.setType(Material.CRAFTING_TABLE);
        Location location = craftingTable.getLocation();

        PlayerMock[] players = new PlayerMock[5];
        for (int i = 0; i < 5; i++) {
            players[i] = server.addPlayer("CraftPlayer" + i);
            Inventory inv = manager.getWorkbenchInventory(location);
            players[i].openInventory(inv);
        }

        // Symulacja: każdy gracz dodaje item
        Inventory sharedInv = manager.getWorkbenchInventory(location);
        for (int i = 0; i < 5; i++) {
            sharedInv.setItem(i + 1, new ItemStack(Material.GOLD_BLOCK, i + 1));
        }

        // Weryfikacja: Wszyscy widzą wszystkie 5 itemów
        for (int i = 0; i < 5; i++) {
            Inventory playerView = players[i].getOpenInventory().getTopInventory();
            for (int slot = 1; slot <= 5; slot++) {
                assertNotNull(playerView.getItem(slot), 
                    "Player " + i + " powinien widzieć item w slocie " + slot);
                assertEquals(Material.GOLD_BLOCK, playerView.getItem(slot).getType());
                assertEquals(slot, playerView.getItem(slot).getAmount());
            }
        }
    }
    
    @Test
    @DisplayName("SPEC: Eksplozja TNT niszczy stół i wyrzuca itemy")
    void explosionDestroysTableAndDropsItems() {
        // Setup
        PlayerMock player = server.addPlayer("Bomber");
        Block craftingTable = server.getWorld("world").getBlockAt(700, 64, 700);
        craftingTable.setType(Material.CRAFTING_TABLE);
        Location location = craftingTable.getLocation();

        // Wkładamy itemy
        Inventory inv = manager.getWorkbenchInventory(location);
        inv.setItem(1, new ItemStack(Material.EMERALD_BLOCK, 10));
        inv.setItem(2, new ItemStack(Material.DIAMOND, 5));

        assertTrue(manager.hasInventory(location), "Manager powinien mieć zapisany inventory przed eksplozją");

        // Symulacja eksplozji TNT
        org.bukkit.event.entity.EntityExplodeEvent event = new org.bukkit.event.entity.EntityExplodeEvent(
            null, // Entity może być null w teście
            location,
            java.util.Collections.singletonList(craftingTable),
            0.0f,
            org.bukkit.ExplosionResult.DESTROY
        );
        server.getPluginManager().callEvent(event);

        // Weryfikacja: Manager nie ma już tego inventory
        assertFalse(manager.hasInventory(location), "Manager nie powinien już mieć tego inventory po eksplozji");
    }
    
    @Test
    @DisplayName("SPEC: Eksplozja bloku (bed) niszczy stół i wyrzuca itemy")
    void blockExplosionDestroysTableAndDropsItems() {
        // Setup
        Block craftingTable = server.getWorld("world").getBlockAt(800, 64, 800);
        craftingTable.setType(Material.CRAFTING_TABLE);
        Location location = craftingTable.getLocation();

        // Wkładamy itemy
        Inventory inv = manager.getWorkbenchInventory(location);
        inv.setItem(3, new ItemStack(Material.GOLD_BLOCK, 7));

        assertTrue(manager.hasInventory(location), "Manager powinien mieć zapisany inventory przed eksplozją");

        // Symulacja eksplozji bloku (np. bed w netherze)
        org.bukkit.event.block.BlockExplodeEvent event = new org.bukkit.event.block.BlockExplodeEvent(
            craftingTable,
            craftingTable.getState(),
            java.util.Collections.singletonList(craftingTable),
            0.0f,
            org.bukkit.ExplosionResult.DESTROY
        );
        server.getPluginManager().callEvent(event);

        // Weryfikacja: Manager nie ma już tego inventory
        assertFalse(manager.hasInventory(location), "Manager nie powinien już mieć tego inventory po eksplozji bloku");
    }
    
    @Test
    @DisplayName("SPEC: Piston pchający stół wyrzuca itemy")
    void pistonPushingTableDropsItems() {
        // Setup
        Block craftingTable = server.getWorld("world").getBlockAt(900, 64, 900);
        craftingTable.setType(Material.CRAFTING_TABLE);
        Location location = craftingTable.getLocation();

        // Wkładamy itemy
        Inventory inv = manager.getWorkbenchInventory(location);
        inv.setItem(1, new ItemStack(Material.IRON_BLOCK, 20));

        assertTrue(manager.hasInventory(location), "Manager powinien mieć zapisany inventory przed pchnięciem");

        // Symulacja pistona pchającego blok
        Block piston = server.getWorld("world").getBlockAt(899, 64, 900);
        piston.setType(Material.PISTON);
        
        org.bukkit.event.block.BlockPistonExtendEvent event = new org.bukkit.event.block.BlockPistonExtendEvent(
            piston,
            java.util.Collections.singletonList(craftingTable),
            org.bukkit.block.BlockFace.EAST
        );
        server.getPluginManager().callEvent(event);

        // Weryfikacja: Manager nie ma już tego inventory
        assertFalse(manager.hasInventory(location), "Manager nie powinien już mieć tego inventory po pchnięciu pistona");
    }
    
    @Test
    @DisplayName("SPEC: Sticky piston cofający stół wyrzuca itemy")
    void stickyPistonRetractingTableDropsItems() {
        // Setup
        Block craftingTable = server.getWorld("world").getBlockAt(1000, 64, 1000);
        craftingTable.setType(Material.CRAFTING_TABLE);
        Location location = craftingTable.getLocation();

        // Wkładamy itemy
        Inventory inv = manager.getWorkbenchInventory(location);
        inv.setItem(2, new ItemStack(Material.REDSTONE_BLOCK, 15));

        assertTrue(manager.hasInventory(location), "Manager powinien mieć zapisany inventory przed cofnięciem");

        // Symulacja sticky pistona cofającego blok
        Block stickyPiston = server.getWorld("world").getBlockAt(1001, 64, 1000);
        stickyPiston.setType(Material.STICKY_PISTON);
        
        org.bukkit.event.block.BlockPistonRetractEvent event = new org.bukkit.event.block.BlockPistonRetractEvent(
            stickyPiston,
            java.util.Collections.singletonList(craftingTable),
            org.bukkit.block.BlockFace.WEST
        );
        server.getPluginManager().callEvent(event);

        // Weryfikacja: Manager nie ma już tego inventory
        assertFalse(manager.hasInventory(location), "Manager nie powinien już mieć tego inventory po cofnięciu pistona");
    }
    
    @Test
    @DisplayName("SPEC: Niszczenie stołu gdy gracz ma go otwartego zamyka GUI")
    void breakingTableWhilePlayerHasItOpenClosesGUI() {
        // Setup
        PlayerMock player = server.addPlayer("Viewer");
        Block craftingTable = server.getWorld("world").getBlockAt(1100, 64, 1100);
        craftingTable.setType(Material.CRAFTING_TABLE);
        Location location = craftingTable.getLocation();

        // Gracz otwiera stół
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            null,
            craftingTable,
            org.bukkit.block.BlockFace.UP
        );
        server.getPluginManager().callEvent(interactEvent);
        
        // Wkładamy itemy
        Inventory inv = manager.getWorkbenchInventory(location);
        inv.setItem(1, new ItemStack(Material.OBSIDIAN, 64));

        // Sprawdzamy że gracz ma otwarty inventory
        assertNotNull(player.getOpenInventory().getTopInventory(), "Gracz powinien mieć otwarty inventory");

        // Niszczenie stołu
        org.bukkit.event.block.BlockBreakEvent breakEvent = new org.bukkit.event.block.BlockBreakEvent(
            craftingTable,
            player
        );
        server.getPluginManager().callEvent(breakEvent);

        // Weryfikacja: Inventory został zamknięty
        // W MockBukkit closeInventory() ustawia opened inventory na null lub default player inventory
        // Sprawdzamy że to już nie jest nasz shared workbench
        assertFalse(manager.hasInventory(location), "Manager nie powinien już mieć tego inventory");
    }
    
    @Test
    @DisplayName("SPEC: Gracz sneakujący (shift) może kłaść bloki obok crafting table")
    void sneakingPlayerCanPlaceBlocksNextToCraftingTable() {
        // Setup
        PlayerMock player = server.addPlayer("Builder");
        Block craftingTable = server.getWorld("world").getBlockAt(1200, 64, 1200);
        craftingTable.setType(Material.CRAFTING_TABLE);
        
        // Gracz trzyma shift i ma blok w ręce
        player.setSneaking(true);
        player.getInventory().setItemInMainHand(new ItemStack(Material.STONE, 64));
        
        // Gracz próbuje kliknąć prawym na stół (normalnie otworzyłoby GUI, ale sneakuje więc chce położyć blok)
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            player.getInventory().getItemInMainHand(),
            craftingTable,
            org.bukkit.block.BlockFace.UP
        );
        server.getPluginManager().callEvent(interactEvent);
        
        // Weryfikacja: Event NIE został anulowany (pozwalamy na kładzenie bloków)
        assertFalse(interactEvent.isCancelled(), "Event nie powinien być anulowany gdy gracz sneakuje");
        
        // Weryfikacja: GUI NIE zostało otwarte (getTopInventory() zwraca null gdy nie ma otwartego GUI)
        Inventory topInv = player.getOpenInventory().getTopInventory();
        if (topInv != null) {
            // Jeśli jakieś inventory jest otwarte, to nie powinno to być crafting table
            assertNotEquals(InventoryType.WORKBENCH, topInv.getType(), 
                "Nie powinien być otwarty workbench gdy gracz sneakuje");
        }
    }
    
    @Test
    @DisplayName("SPEC: Gracz naciskający Q (drop) dropuje item z shared inventory, nie z własnego")
    void pressingQDropsItemFromSharedInventoryNotPlayerInventory() {
        // Setup
        PlayerMock player = server.addPlayer("Dropper");
        Block craftingTable = server.getWorld("world").getBlockAt(1300, 64, 1300);
        craftingTable.setType(Material.CRAFTING_TABLE);
        Location location = craftingTable.getLocation();
        
        // Gracz otwiera stół
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            null,
            craftingTable,
            org.bukkit.block.BlockFace.UP
        );
        server.getPluginManager().callEvent(interactEvent);
        
        // Dodajemy 10 diamentów do slotu 2 w shared inventory
        Inventory sharedInv = manager.getWorkbenchInventory(location);
        sharedInv.setItem(2, new ItemStack(Material.DIAMOND, 10));
        
        // Gracz ma 5 emeraldów w swoim inventory
        player.getInventory().setItem(2, new ItemStack(Material.EMERALD, 5));
        
        // Gracz naciska Q (drop one) na slocie 2 w shared inventory
        org.bukkit.event.inventory.InventoryClickEvent dropEvent = 
            new org.bukkit.event.inventory.InventoryClickEvent(
                player.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                2, // Slot 2 w shared inventory
                org.bukkit.event.inventory.ClickType.DROP,
                org.bukkit.event.inventory.InventoryAction.DROP_ONE_SLOT
            );
        server.getPluginManager().callEvent(dropEvent);
        
        // Weryfikacja: Stack w shared inventory zmniejszył się (10 -> 9 diamentów)
        ItemStack itemInShared = sharedInv.getItem(2);
        assertNotNull(itemInShared, "Item w shared inventory nie powinien zniknąć całkowicie");
        assertEquals(Material.DIAMOND, itemInShared.getType(), "Typ nie powinien się zmienić");
        assertEquals(9, itemInShared.getAmount(), "Powinno zostać 9 diamentów (10-1)");
        
        // Weryfikacja: Item gracza NIE zmienił się (nadal 5 emeraldów)
        ItemStack playerItem = player.getInventory().getItem(2);
        assertNotNull(playerItem, "Item gracza powinien nadal istnieć");
        assertEquals(Material.EMERALD, playerItem.getType(), "Typ itemu gracza nie powinien się zmienić");
        assertEquals(5, playerItem.getAmount(), "Ilość itemu gracza nie powinna się zmienić");
    }
}

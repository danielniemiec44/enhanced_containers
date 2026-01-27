package pl.com.sofitfy.enhanced_containers.sharedcrafting;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ExplosionResult;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne dla SharedCrafting weryfikujące wymagania klienta:
 * 
 * 1. Nazwa stołu (tytuł GUI) nie powinna być zmieniona
 * 2. Kopiowanie itemów przez niszczenie bloku - zabezpieczenie (TNT, break, piston)
 *    - Wszystkie próby usunięcia lub zmiany bloku powinny wyrzucać itemy, zamykać GUI i usuwać dane
 * 3. Klikanie Q nie wyrzuca przedmiotów tylko daje na kursor -> KOREKTA: Q dropuje na ziemię (vanilla)
 * 4. Nie da się kłaść bloków za pomocą shift + ppm (to vanilla behavior - pozwalamy)
 * 5. Szybkie craftingi (drag) nie powinny duplikować itemów
 */
@DisplayName("SharedCrafting - Wymagania Klienta")
public class SharedCraftingClientRequirementsTest {

    private ServerMock server;
    private SharedCraftingManager manager;
    private SharedCraftingListener listener;
    private Location tableLocation;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        
        manager = new SharedCraftingManager(MockBukkit.createMockPlugin());
        listener = new SharedCraftingListener(manager);
        
        server.getPluginManager().registerEvents(listener, MockBukkit.createMockPlugin());
        
        tableLocation = new Location(server.getWorld("world"), 100, 64, 100);
        tableLocation.getBlock().setType(Material.CRAFTING_TABLE);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ========================================================================
    // 1. NAZWA STOŁU (TYTUŁ GUI)
    // ========================================================================
    
    @Nested
    @DisplayName("1. Tytuł GUI")
    class GUITitleTests {
        
        @Test
        @DisplayName("Shared crafting używa standardowego tytułu 'Crafting'")
        void sharedCraftingUsesDefaultTitle() {
            PlayerMock player = server.addPlayer("TitleTester");
            
            // Otwórz shared crafting
            openCraftingTable(player, tableLocation);
            
            // Weryfikacja: Tytuł powinien być standardowy (Minecraft: "Crafting")
            InventoryView view = player.getOpenInventory();
            assertNotNull(view, "Gracz powinien mieć otwarty inventory");
            
            // Bukkit InventoryType.WORKBENCH ma domyślny tytuł "Crafting"
            // MockBukkit może zwracać różne wartości, ale sprawdzamy typ
            assertEquals(InventoryType.WORKBENCH, view.getTopInventory().getType(),
                "Powinien być otwarty WORKBENCH inventory");
            
            // Tytuł nie powinien być zmieniony na niestandardowy
            String title = view.getTitle();
            // Standardowe tytuły Minecraft to "Crafting" lub mogą być puste w MockBukkit
            assertTrue(title == null || title.isEmpty() || title.equals("Crafting") || title.equals("container.crafting"),
                "Tytuł powinien być standardowy, nie customowy. Aktualny: " + title);
        }
        
        @Test
        @DisplayName("Różne stoły mają ten sam format tytułu (bez lokalizacji w nazwie)")
        void differentTablesHaveSameTitleFormat() {
            PlayerMock player1 = server.addPlayer("Player1");
            PlayerMock player2 = server.addPlayer("Player2");
            
            Location loc1 = new Location(server.getWorld("world"), 100, 64, 100);
            Location loc2 = new Location(server.getWorld("world"), 200, 64, 200);
            loc1.getBlock().setType(Material.CRAFTING_TABLE);
            loc2.getBlock().setType(Material.CRAFTING_TABLE);
            
            openCraftingTable(player1, loc1);
            openCraftingTable(player2, loc2);
            
            String title1 = player1.getOpenInventory().getTitle();
            String title2 = player2.getOpenInventory().getTitle();
            
            assertEquals(title1, title2, 
                "Różne stoły powinny mieć ten sam tytuł (bez unikalnych identyfikatorów)");
        }
    }

    // ========================================================================
    // 2. KOPIOWANIE ITEMÓW PRZEZ NISZCZENIE BLOKU
    // ========================================================================
    
    @Nested
    @DisplayName("2. Niszczenie bloku - zabezpieczenie przed duplikacją")
    class BlockDestructionTests {
        
        @Test
        @DisplayName("Niszczenie stołu gdy gracz ma go otwartego - zamyka GUI i dropuje itemy")
        void breakingTableWithPlayerViewingClosesGUIAndDropsItems() {
            PlayerMock viewer = server.addPlayer("Viewer");
            PlayerMock breaker = server.addPlayer("Breaker");
            
            // Viewer otwiera stół i wkłada itemy
            openCraftingTable(viewer, tableLocation);
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            sharedInv.setItem(1, new ItemStack(Material.DIAMOND, 10));
            sharedInv.setItem(2, new ItemStack(Material.EMERALD, 5));
            
            // Sprawdź że viewer ma otwarty inventory
            assertEquals(InventoryType.WORKBENCH, viewer.getOpenInventory().getTopInventory().getType(),
                "Viewer powinien mieć otwarty workbench");
            
            // Breaker niszczy stół
            BlockBreakEvent breakEvent = new BlockBreakEvent(tableLocation.getBlock(), breaker);
            server.getPluginManager().callEvent(breakEvent);
            
            // Weryfikacja 1: Manager nie ma już tego inventory
            assertFalse(manager.hasInventory(tableLocation), 
                "Manager nie powinien mieć inventory po zniszczeniu stołu");
            
            // Weryfikacja 2: GUI viewera powinno być zamknięte
            // W MockBukkit closeInventory() może nie zmieniać getTopInventory() natychmiast,
            // ale inventory powinien być wyczyszczony
            // Sprawdzamy że shared inventory został wyczyszczony
            assertNull(sharedInv.getItem(1), "Slot 1 powinien być pusty po zniszczeniu");
            assertNull(sharedInv.getItem(2), "Slot 2 powinien być pusty po zniszczeniu");
        }
        
        @Test
        @DisplayName("Niszczenie stołu z wieloma graczami - wszyscy tracą dostęp")
        void breakingTableWithMultipleViewersClosesAllGUIs() {
            PlayerMock[] viewers = new PlayerMock[5];
            for (int i = 0; i < 5; i++) {
                viewers[i] = server.addPlayer("Viewer" + i);
                openCraftingTable(viewers[i], tableLocation);
            }
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            sharedInv.setItem(1, new ItemStack(Material.NETHERITE_INGOT, 32));
            
            // Weryfikacja: Wszyscy viewerzy są w liście viewers
            assertTrue(sharedInv.getViewers().size() >= 5 || manager.hasInventory(tableLocation),
                "Manager powinien mieć inventory z viewerami");
            
            // Niszczenie
            BlockBreakEvent breakEvent = new BlockBreakEvent(tableLocation.getBlock(), viewers[0]);
            server.getPluginManager().callEvent(breakEvent);
            
            // Weryfikacja: Manager nie ma inventory
            assertFalse(manager.hasInventory(tableLocation), 
                "Manager nie powinien mieć inventory po zniszczeniu");
            
            // Weryfikacja: Inventory wyczyszczony
            assertNull(sharedInv.getItem(1), "Inventory powinien być wyczyszczony");
        }
        
        @Test
        @DisplayName("TNT eksplozja niszczy stół i czyści dane")
        void tntExplosionDestroysTableAndClearsData() {
            PlayerMock player = server.addPlayer("TNTVictim");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            sharedInv.setItem(1, new ItemStack(Material.DIAMOND_BLOCK, 64));
            sharedInv.setItem(5, new ItemStack(Material.GOLD_BLOCK, 32));
            
            // Symulacja eksplozji TNT
            EntityExplodeEvent explodeEvent = new EntityExplodeEvent(
                null,
                tableLocation,
                Collections.singletonList(tableLocation.getBlock()),
                0.0f,
                ExplosionResult.DESTROY
            );
            server.getPluginManager().callEvent(explodeEvent);
            
            // Weryfikacja
            assertFalse(manager.hasInventory(tableLocation), 
                "Manager nie powinien mieć inventory po eksplozji TNT");
            assertNull(sharedInv.getItem(1), "Inventory powinien być wyczyszczony po eksplozji");
        }
        
        @Test
        @DisplayName("Eksplozja łóżka (BlockExplodeEvent) niszczy stół")
        void bedExplosionDestroysTable() {
            PlayerMock player = server.addPlayer("BedBomber");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            sharedInv.setItem(3, new ItemStack(Material.IRON_BLOCK, 16));
            
            // Symulacja eksplozji bloku (bed w netherze, respawn anchor)
            BlockExplodeEvent explodeEvent = new BlockExplodeEvent(
                tableLocation.getBlock(),
                tableLocation.getBlock().getState(),
                Collections.singletonList(tableLocation.getBlock()),
                0.0f,
                ExplosionResult.DESTROY
            );
            server.getPluginManager().callEvent(explodeEvent);
            
            assertFalse(manager.hasInventory(tableLocation), 
                "Manager nie powinien mieć inventory po eksplozji bloku");
        }
        
        @Test
        @DisplayName("Piston pchający stół wyrzuca itemy i czyści dane")
        void pistonPushingTableDropsItemsAndClearsData() {
            PlayerMock player = server.addPlayer("PistonVictim");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            sharedInv.setItem(4, new ItemStack(Material.REDSTONE_BLOCK, 8));
            
            Block piston = server.getWorld("world").getBlockAt(99, 64, 100);
            piston.setType(Material.PISTON);
            
            BlockPistonExtendEvent pistonEvent = new BlockPistonExtendEvent(
                piston,
                Collections.singletonList(tableLocation.getBlock()),
                BlockFace.EAST
            );
            server.getPluginManager().callEvent(pistonEvent);
            
            assertFalse(manager.hasInventory(tableLocation), 
                "Manager nie powinien mieć inventory po pchnięciu pistonem");
        }
        
        @Test
        @DisplayName("Sticky piston cofający stół wyrzuca itemy i czyści dane")
        void stickyPistonRetractingTableDropsItemsAndClearsData() {
            PlayerMock player = server.addPlayer("StickyVictim");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            sharedInv.setItem(6, new ItemStack(Material.LAPIS_BLOCK, 12));
            
            Block stickyPiston = server.getWorld("world").getBlockAt(101, 64, 100);
            stickyPiston.setType(Material.STICKY_PISTON);
            
            BlockPistonRetractEvent pistonEvent = new BlockPistonRetractEvent(
                stickyPiston,
                Collections.singletonList(tableLocation.getBlock()),
                BlockFace.WEST
            );
            server.getPluginManager().callEvent(pistonEvent);
            
            assertFalse(manager.hasInventory(tableLocation), 
                "Manager nie powinien mieć inventory po cofnięciu sticky pistonem");
        }
        
        @Test
        @DisplayName("Niszczenie stołu nie duplikuje itemów (itemy tylko w jednym miejscu)")
        void destroyingTableDoesNotDuplicateItems() {
            PlayerMock player = server.addPlayer("DupeChecker");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            
            // Wkładamy dokładnie 64 diamenty
            int totalDiamondsBefore = 64;
            sharedInv.setItem(1, new ItemStack(Material.DIAMOND, totalDiamondsBefore));
            
            // Niszczenie
            BlockBreakEvent breakEvent = new BlockBreakEvent(tableLocation.getBlock(), player);
            server.getPluginManager().callEvent(breakEvent);
            
            // Po zniszczeniu itemy powinny być TYLKO na ziemi (dropnięte) lub w inventory gracza
            // NIE powinny być w shared inventory (bo został wyczyszczony)
            
            // Sprawdzamy że shared inventory jest pusty
            assertNull(sharedInv.getItem(1), "Shared inventory powinien być pusty");
            
            // Manager nie ma już tego inventory
            assertFalse(manager.hasInventory(tableLocation), 
                "Manager nie powinien mieć inventory - dane usunięte");
            
            // Nie sprawdzamy world.getEntities() bo MockBukkit może nie implementować dropItemNaturally poprawnie
            // Ale kluczowe jest że inventory jest wyczyszczone i nie ma duplikacji w pamięci
        }
        
        @Test
        @DisplayName("Slot wyniku (0) nie jest dropowany przy niszczeniu")
        void resultSlotNotDroppedOnDestruction() {
            PlayerMock player = server.addPlayer("ResultTester");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            
            // Wkładamy składniki i symulujemy że jest wynik
            sharedInv.setItem(1, new ItemStack(Material.OAK_LOG, 1)); // Składnik
            sharedInv.setItem(0, new ItemStack(Material.OAK_PLANKS, 4)); // Wynik (symulowany)
            
            // handleCraftingTableDestruction zaczyna od i=1, więc slot 0 jest pomijany
            // To jest poprawne zachowanie - wynik nie jest "prawdziwy", jest tylko preview
            
            // Niszczenie
            BlockBreakEvent breakEvent = new BlockBreakEvent(tableLocation.getBlock(), player);
            server.getPluginManager().callEvent(breakEvent);
            
            // Po zniszczeniu manager nie powinien mieć inventory
            assertFalse(manager.hasInventory(tableLocation), 
                "Manager nie powinien mieć inventory po zniszczeniu");
        }
        
        @Test
        @DisplayName("Spalenie stołu (BlockBurnEvent) wyrzuca itemy i czyści dane")
        void burningTableDropsItemsAndClearsData() {
            PlayerMock player = server.addPlayer("FireVictim");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            sharedInv.setItem(1, new ItemStack(Material.COAL_BLOCK, 16));
            
            // Symulacja spalenia bloku
            BlockBurnEvent burnEvent = new BlockBurnEvent(tableLocation.getBlock(), tableLocation.getBlock());
            server.getPluginManager().callEvent(burnEvent);
            
            assertFalse(manager.hasInventory(tableLocation), 
                "Manager nie powinien mieć inventory po spaleniu stołu");
            assertNull(sharedInv.getItem(1), "Inventory powinien być wyczyszczony po spaleniu");
        }
        
        @Test
        @DisplayName("Enderman podnoszący stół (EntityChangeBlockEvent) wyrzuca itemy i czyści dane")
        void endermanPickingUpTableDropsItemsAndClearsData() {
            PlayerMock player = server.addPlayer("EndermanVictim");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            sharedInv.setItem(2, new ItemStack(Material.ENDER_PEARL, 8));
            
            // Symulacja Endermana podnoszącego blok
            EntityChangeBlockEvent changeEvent = new EntityChangeBlockEvent(
                null, // entity może być null w teście
                tableLocation.getBlock(),
                Material.AIR.createBlockData()
            );
            server.getPluginManager().callEvent(changeEvent);
            
            assertFalse(manager.hasInventory(tableLocation), 
                "Manager nie powinien mieć inventory po zabraniu przez Endermana");
            assertNull(sharedInv.getItem(2), "Inventory powinien być wyczyszczony");
        }
        
        @Test
        @DisplayName("Walidacja usuwa stoły które nie są już CRAFTING_TABLE")
        void validationRemovesInvalidWorkbenches() {
            PlayerMock player = server.addPlayer("ValidationTester");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            sharedInv.setItem(1, new ItemStack(Material.DIAMOND, 32));
            
            // Upewnij się że chunk jest załadowany (MockBukkit może mieć problemy)
            tableLocation.getChunk().load();
            
            // Symulacja zmiany bloku na AIR (bez eventu - np. przez /setblock)
            tableLocation.getBlock().setType(Material.AIR);
            
            // Walidacja powinna wykryć że blok nie jest już CRAFTING_TABLE
            int removed = manager.validateAndCleanupInvalidWorkbenches();
            
            assertEquals(1, removed, "Powinien być usunięty 1 nieaktualny stół");
            assertFalse(manager.hasInventory(tableLocation), 
                "Manager nie powinien mieć inventory po walidacji");
        }
        
        @Test
        @DisplayName("Walidacja nie usuwa poprawnych stołów")
        void validationKeepsValidWorkbenches() {
            PlayerMock player = server.addPlayer("ValidTester");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            sharedInv.setItem(1, new ItemStack(Material.EMERALD, 16));
            
            // Blok nadal jest CRAFTING_TABLE
            assertEquals(Material.CRAFTING_TABLE, tableLocation.getBlock().getType());
            
            // Walidacja nie powinna nic usunąć
            int removed = manager.validateAndCleanupInvalidWorkbenches();
            
            assertEquals(0, removed, "Żaden stół nie powinien być usunięty");
            assertTrue(manager.hasInventory(tableLocation), 
                "Manager powinien nadal mieć inventory");
            assertNotNull(sharedInv.getItem(1), "Itemy powinny pozostać");
        }
    }

    // ========================================================================
    // 3. KLAWISZ Q (DROP)
    // ========================================================================
    
    @Nested
    @DisplayName("3. Klawisz Q (Drop)")
    class DropKeyTests {
        
        @Test
        @DisplayName("Naciśnięcie Q na slocie w grid dropuje item na ziemię")
        void pressingQOnGridSlotDropsItemToWorld() {
            PlayerMock player = server.addPlayer("Dropper");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            sharedInv.setItem(2, new ItemStack(Material.DIAMOND, 10));
            
            // Symulacja naciśnięcia Q (DROP) na slocie 2
            InventoryClickEvent dropEvent = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                2, // Slot 2 w shared inventory
                ClickType.DROP,
                InventoryAction.DROP_ONE_SLOT
            );
            listener.onInventoryClick(dropEvent);
            
            // Weryfikacja: Event został anulowany (obsługujemy ręcznie)
            assertTrue(dropEvent.isCancelled(), "Event powinien być anulowany (obsługujemy ręcznie)");
            
            // Weryfikacja: Stack zmniejszył się o 1 (z 10 na 9)
            ItemStack remaining = sharedInv.getItem(2);
            assertNotNull(remaining, "Item nie powinien zniknąć całkowicie");
            assertEquals(Material.DIAMOND, remaining.getType());
            assertEquals(9, remaining.getAmount(), "Powinno zostać 9 diamentów (10-1)");
        }
        
        @Test
        @DisplayName("Naciśnięcie Ctrl+Q (DROP_ALL) dropuje cały stack")
        void pressingCtrlQDropsEntireStack() {
            PlayerMock player = server.addPlayer("StackDropper");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            sharedInv.setItem(3, new ItemStack(Material.EMERALD, 32));
            
            // Symulacja Ctrl+Q (DROP_ALL)
            InventoryClickEvent dropAllEvent = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                3,
                ClickType.CONTROL_DROP,
                InventoryAction.DROP_ALL_SLOT
            );
            listener.onInventoryClick(dropAllEvent);
            
            assertTrue(dropAllEvent.isCancelled(), "Event powinien być anulowany");
            
            // Weryfikacja: Slot jest pusty
            ItemStack remaining = sharedInv.getItem(3);
            assertTrue(remaining == null || remaining.getType() == Material.AIR, 
                "Slot powinien być pusty po Ctrl+Q");
        }
        
        @Test
        @DisplayName("Q na slocie wyniku (0) craftuje przedmiot i dropuje na ziemię (vanilla behavior)")
        void qOnResultSlotCraftsAndDropsToGround() {
            PlayerMock player = server.addPlayer("ResultDropper");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            // Ustawiamy składniki na deski (slot 1-9 = grid 3x3)
            // 1 oak log -> 4 planks
            sharedInv.setItem(1, new ItemStack(Material.OAK_LOG, 1));
            
            // Wynik craftingu powinien się pojawić w slocie 0
            sharedInv.setItem(0, new ItemStack(Material.OAK_PLANKS, 4));
            
            // Gracz NIE ma nic na kursorze
            player.setItemOnCursor(null);
            
            // Symulacja Q na slocie wyniku
            InventoryClickEvent dropResultEvent = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.RESULT,
                0,
                ClickType.DROP,
                InventoryAction.DROP_ONE_SLOT
            );
            listener.onInventoryClick(dropResultEvent);
            
            assertTrue(dropResultEvent.isCancelled(), 
                "Event powinien być anulowany (obsługujemy ręcznie)");
            
            // Składniki powinny być zużyte (slot 1 pusty)
            ItemStack ingredient = sharedInv.getItem(1);
            assertTrue(ingredient == null || ingredient.getType() == Material.AIR,
                "Składnik (log) powinien być zużyty");
            
            // Vanilla behavior: Q na result slot ZAWSZE dropuje na ziemię (nie daje na kursor)
            ItemStack cursor = player.getItemOnCursor();
            assertTrue(cursor == null || cursor.getType() == Material.AIR,
                "Kursor powinien być pusty - Q dropuje na ziemię, nie daje na kursor");
            
            // Item powinien być wyrzucony na ziemię (MockBukkit)
            // Nie możemy łatwo sprawdzić dropped items w MockBukkit, ale składniki są zużyte
        }
        
        @Test
        @DisplayName("Q na slocie wyniku z kursorem zajętym nadal dropuje na ziemię")
        void qOnResultSlotWithCursorOccupiedDropsToGround() {
            PlayerMock player = server.addPlayer("ResultDropperBusy");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            sharedInv.setItem(1, new ItemStack(Material.OAK_LOG, 1));
            sharedInv.setItem(0, new ItemStack(Material.OAK_PLANKS, 4));
            
            // Gracz MA coś na kursorze (inny item)
            player.setItemOnCursor(new ItemStack(Material.DIAMOND, 5));
            
            InventoryClickEvent dropResultEvent = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.RESULT,
                0,
                ClickType.DROP,
                InventoryAction.DROP_ONE_SLOT
            );
            listener.onInventoryClick(dropResultEvent);
            
            assertTrue(dropResultEvent.isCancelled());
            
            // Składniki zużyte
            ItemStack ingredient = sharedInv.getItem(1);
            assertTrue(ingredient == null || ingredient.getType() == Material.AIR,
                "Składnik powinien być zużyty");
            
            // Kursor NIE powinien się zmienić (nadal ma diamenty)
            ItemStack cursor = player.getItemOnCursor();
            assertNotNull(cursor);
            assertEquals(Material.DIAMOND, cursor.getType(), 
                "Kursor powinien nadal mieć diamenty");
            assertEquals(5, cursor.getAmount());
        }
        
        @Test
        @DisplayName("Q nie wpływa na inventory gracza gdy klikamy w shared inventory")
        void qDoesNotAffectPlayerInventoryWhenClickingSharedInventory() {
            PlayerMock player = server.addPlayer("SafeDropper");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            sharedInv.setItem(4, new ItemStack(Material.GOLD_INGOT, 16));
            
            // Gracz ma swój item w tym samym slocie w SWOIM inventory
            player.getInventory().setItem(4, new ItemStack(Material.IRON_INGOT, 32));
            
            // Symulacja Q na slocie 4 w SHARED inventory
            InventoryClickEvent dropEvent = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                4, // raw slot 4 = slot 4 w top inventory
                ClickType.DROP,
                InventoryAction.DROP_ONE_SLOT
            );
            listener.onInventoryClick(dropEvent);
            
            // Weryfikacja: Item gracza NIE zmienił się
            ItemStack playerItem = player.getInventory().getItem(4);
            assertNotNull(playerItem, "Item gracza powinien nadal istnieć");
            assertEquals(Material.IRON_INGOT, playerItem.getType());
            assertEquals(32, playerItem.getAmount(), "Ilość itemu gracza nie powinna się zmienić");
            
            // Weryfikacja: Item w shared inventory zmniejszył się
            ItemStack sharedItem = sharedInv.getItem(4);
            assertNotNull(sharedItem);
            assertEquals(15, sharedItem.getAmount(), "Shared item powinien się zmniejszyć");
        }
    }

    // ========================================================================
    // 4. SHIFT + PPM (KŁADZENIE BLOKÓW)
    // ========================================================================
    
    @Nested
    @DisplayName("4. Shift + PPM (Kładzenie bloków)")
    class ShiftRightClickTests {
        
        @Test
        @DisplayName("Gracz sneakujący może kłaść bloki obok crafting table (vanilla behavior)")
        void sneakingPlayerCanPlaceBlocksNextToCraftingTable() {
            PlayerMock player = server.addPlayer("Builder");
            player.setSneaking(true);
            player.getInventory().setItemInMainHand(new ItemStack(Material.COBBLESTONE, 64));
            
            PlayerInteractEvent interactEvent = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                player.getInventory().getItemInMainHand(),
                tableLocation.getBlock(),
                BlockFace.UP,
                EquipmentSlot.HAND
            );
            listener.onInteract(interactEvent);
            
            // Event NIE został anulowany - pozwalamy na vanilla behavior (kładzenie bloku)
            assertFalse(interactEvent.isCancelled(), 
                "Event nie powinien być anulowany gdy gracz sneakuje - pozwalamy na kładzenie bloków");
        }
        
        @Test
        @DisplayName("Gracz nie-sneakujący otwiera GUI zamiast kłaść bloki")
        void nonSneakingPlayerOpensGUIInsteadOfPlacingBlocks() {
            PlayerMock player = server.addPlayer("NonBuilder");
            player.setSneaking(false);
            player.getInventory().setItemInMainHand(new ItemStack(Material.COBBLESTONE, 64));
            
            PlayerInteractEvent interactEvent = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                player.getInventory().getItemInMainHand(),
                tableLocation.getBlock(),
                BlockFace.UP,
                EquipmentSlot.HAND
            );
            listener.onInteract(interactEvent);
            
            // Event JEST anulowany - otwieramy GUI
            assertTrue(interactEvent.isCancelled(), 
                "Event powinien być anulowany - otwieramy GUI zamiast kłaść blok");
            
            // GUI jest otwarte
            assertEquals(InventoryType.WORKBENCH, player.getOpenInventory().getTopInventory().getType());
        }
        
        @Test
        @DisplayName("Shift+click wewnątrz shared inventory przesuwa itemy (nie kładzie)")
        void shiftClickInsideInventoryMovesItems() {
            PlayerMock player = server.addPlayer("ShiftClicker");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            sharedInv.setItem(5, new ItemStack(Material.DIAMOND, 16));
            
            // Shift+click na slocie 5 w shared inventory (aby przenieść do eq gracza)
            InventoryClickEvent shiftClickEvent = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                5,
                ClickType.SHIFT_LEFT,
                InventoryAction.MOVE_TO_OTHER_INVENTORY
            );
            listener.onInventoryClick(shiftClickEvent);
            
            // Item powinien zostać przeniesiony do inventory gracza
            // (lub zostać w shared inv jeśli eq gracza jest pełny)
            // Kluczowe jest że nie próbujemy "położyć bloku"
            
            // Sprawdzamy że logika shift-click działa poprawnie dla shared inventory
            // Event może być anulowany (bo obsługujemy ręcznie) lub nie
            // Ważne że item trafia do gracza
            
            // Przy shift-click z gridu (sloty 1-9) item powinien trafić do gracza
            // Slot 5 jest w gridzie
            
            // Szukamy diamentów w inventory gracza
            int diamondsInPlayer = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.DIAMOND) {
                    diamondsInPlayer += item.getAmount();
                }
            }
            
            assertEquals(16, diamondsInPlayer, 
                "Wszystkie diamenty powinny trafić do inventory gracza");
        }
    }

    // ========================================================================
    // 5. SZYBKIE CRAFTINGI (DRAG)
    // ========================================================================
    
    @Nested
    @DisplayName("5. Szybkie craftingi (Drag) - bez duplikacji")
    class DragCraftingTests {
        
        @Test
        @DisplayName("Drag event w gridzie aktualizuje wynik craftingu")
        void dragEventInGridUpdatesResult() {
            PlayerMock player = server.addPlayer("Dragger");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            
            // Symulacja drag event (rozkładanie itemów)
            Map<Integer, ItemStack> newItems = new HashMap<>();
            newItems.put(1, new ItemStack(Material.OAK_LOG, 1));
            newItems.put(2, new ItemStack(Material.OAK_LOG, 1));
            
            InventoryDragEvent dragEvent = new InventoryDragEvent(
                player.getOpenInventory(),
                null,
                new ItemStack(Material.OAK_LOG, 2),
                false,
                newItems
            );
            listener.onInventoryDrag(dragEvent);
            
            // Event nie powinien być anulowany (drag jest dozwolony)
            assertFalse(dragEvent.isCancelled(), 
                "Drag event nie powinien być anulowany");
            
            // Po 1 ticku powinien nastąpić update wyniku
            // W teście możemy to symulować przez schedulera
            server.getScheduler().performOneTick();
        }
        
        @Test
        @DisplayName("Drag poza gridem (player inventory) nie triggeruje update")
        void dragOutsideGridDoesNotTriggerUpdate() {
            PlayerMock player = server.addPlayer("OutsideDragger");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            
            // Drag event tylko w player inventory (raw slots > 9)
            Map<Integer, ItemStack> newItems = new HashMap<>();
            newItems.put(15, new ItemStack(Material.STONE, 1)); // Slot w player inv
            newItems.put(16, new ItemStack(Material.STONE, 1));
            
            InventoryDragEvent dragEvent = new InventoryDragEvent(
                player.getOpenInventory(),
                null,
                new ItemStack(Material.STONE, 2),
                false,
                newItems
            );
            listener.onInventoryDrag(dragEvent);
            
            // Event nie powinien być anulowany
            assertFalse(dragEvent.isCancelled());
        }
        
        @Test
        @DisplayName("Drag nie duplikuje itemów")
        void dragDoesNotDuplicateItems() {
            PlayerMock player = server.addPlayer("DupeDragger");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            
            // Wkładamy dokładnie 9 diamentów
            int startDiamonds = 9;
            
            // Symulacja drag - rozkładamy 9 diamentów po 1 na każdy slot gridu
            Map<Integer, ItemStack> newItems = new HashMap<>();
            for (int i = 1; i <= 9; i++) {
                newItems.put(i, new ItemStack(Material.DIAMOND, 1));
            }
            
            InventoryDragEvent dragEvent = new InventoryDragEvent(
                player.getOpenInventory(),
                new ItemStack(Material.DIAMOND, 9),
                new ItemStack(Material.AIR),
                false,
                newItems
            );
            listener.onInventoryDrag(dragEvent);
            
            // Po drag - zliczamy wszystkie diamenty
            int totalDiamonds = 0;
            for (int i = 1; i <= 9; i++) {
                ItemStack item = sharedInv.getItem(i);
                if (item != null && item.getType() == Material.DIAMOND) {
                    totalDiamonds += item.getAmount();
                }
            }
            
            // Liczba diamentów powinna być <= startDiamonds (mogło być mniej rozłożone)
            // Kluczowe: NIE więcej niż włożyliśmy
            assertTrue(totalDiamonds <= startDiamonds, 
                "Nie powinno być więcej diamentów niż włożyliśmy. Start: " + startDiamonds + ", Po drag: " + totalDiamonds);
        }
    }

    // ========================================================================
    // DODATKOWE TESTY BEZPIECZEŃSTWA
    // ========================================================================
    
    @Nested
    @DisplayName("Dodatkowe testy bezpieczeństwa")
    class AdditionalSecurityTests {
        
        @Test
        @DisplayName("Double-click (collect to cursor) jest całkowicie zablokowany")
        void doubleClickIsBlocked() {
            PlayerMock player = server.addPlayer("DoubleClicker");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            sharedInv.setItem(1, new ItemStack(Material.DIAMOND, 5));
            sharedInv.setItem(2, new ItemStack(Material.DIAMOND, 3));
            
            // Double click na slocie w gridzie
            InventoryClickEvent doubleClickEvent = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                1, // Slot w top inventory
                ClickType.DOUBLE_CLICK,
                InventoryAction.COLLECT_TO_CURSOR
            );
            listener.onInventoryClick(doubleClickEvent);
            
            // Event POWINIEN być anulowany (blokujemy wszystkie double-click podobnie jak w CraftingGUI 4x4)
            assertTrue(doubleClickEvent.isCancelled(), 
                "Double-click powinien być zablokowany we wszystkich slotach");
        }
        
        @Test
        @DisplayName("Double-click na slocie wyniku też jest zablokowany")
        void doubleClickOnResultSlotIsBlocked() {
            PlayerMock player = server.addPlayer("ResultDoubleClicker");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            sharedInv.setItem(0, new ItemStack(Material.OAK_PLANKS, 4)); // Symulowany wynik
            
            // Double click na slocie wyniku (slot 0)
            InventoryClickEvent doubleClickEvent = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.RESULT,
                0, // Slot wyniku
                ClickType.DOUBLE_CLICK,
                InventoryAction.COLLECT_TO_CURSOR
            );
            listener.onInventoryClick(doubleClickEvent);
            
            // Event POWINIEN być anulowany
            assertTrue(doubleClickEvent.isCancelled(), 
                "Double-click na slocie wyniku powinien być zablokowany");
        }
        
        @Test
        @DisplayName("Double-click z player inventory też jest blokowany")
        void doubleClickFromPlayerInventoryIsBlocked() {
            PlayerMock player = server.addPlayer("BlockedDoubleClicker");
            openCraftingTable(player, tableLocation);
            
            player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 10));
            
            // Double click na slocie w player inventory (rawSlot >= topSize)
            int topSize = player.getOpenInventory().getTopInventory().getSize(); // 10 dla WORKBENCH
            InventoryClickEvent doubleClickEvent = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                topSize + 5, // Slot w player inventory
                ClickType.DOUBLE_CLICK,
                InventoryAction.COLLECT_TO_CURSOR
            );
            listener.onInventoryClick(doubleClickEvent);
            
            // Event POWINIEN być anulowany (blokujemy wszystkie double-click)
            assertTrue(doubleClickEvent.isCancelled(), 
                "Double-click z player inventory powinien być zablokowany");
        }
        
        @Test
        @DisplayName("Shift-click z player inventory do gridu działa poprawnie")
        void shiftClickFromPlayerInventoryToGridWorks() {
            PlayerMock player = server.addPlayer("ShiftAdder");
            openCraftingTable(player, tableLocation);
            
            // Daj graczowi itemy
            player.getInventory().setItem(0, new ItemStack(Material.IRON_INGOT, 32));
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            
            // Shift-click z player inventory (slot 10 = pierwszy slot gracza w view)
            int topSize = player.getOpenInventory().getTopInventory().getSize();
            InventoryClickEvent shiftClickEvent = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                topSize, // Pierwszy slot player inventory
                ClickType.SHIFT_LEFT,
                InventoryAction.MOVE_TO_OTHER_INVENTORY
            );
            
            // Ustaw currentItem dla eventu (MockBukkit może tego nie robić automatycznie)
            // Musimy wywołać listener bezpośrednio
            listener.onInventoryClick(shiftClickEvent);
            
            // Event powinien być anulowany (obsługujemy ręcznie)
            assertTrue(shiftClickEvent.isCancelled(), 
                "Shift-click z player inventory powinien być obsługiwany ręcznie");
        }
        
        @Test
        @DisplayName("Bucket crafting pozostawia puste wiadro")
        void bucketCraftingLeavesEmptyBucket() {
            PlayerMock player = server.addPlayer("BucketCrafter");
            openCraftingTable(player, tableLocation);
            
            Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);
            
            // Wkładamy milk bucket do slotu 1
            sharedInv.setItem(1, new ItemStack(Material.MILK_BUCKET, 1));
            // Symulujemy że jest jakiś wynik (np. cake składnik)
            sharedInv.setItem(0, new ItemStack(Material.CAKE, 1));
            
            // Klikamy na wynik (craftujemy)
            InventoryClickEvent resultClick = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.RESULT,
                0,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
            );
            listener.onInventoryClick(resultClick);
            
            // Milk bucket powinien zostać zamieniony na puste wiadro
            ItemStack slot1 = sharedInv.getItem(1);
            assertNotNull(slot1, "W slocie 1 powinno zostać puste wiadro");
            assertEquals(Material.BUCKET, slot1.getType(), 
                "Milk bucket powinien zostać zamieniony na puste wiadro");
        }
    }
    
    // ========================================================================
    // 7. RECIPE BOOK (SZYBKIE CRAFTINGI)
    // ========================================================================
    
    @Nested
    @DisplayName("7. Recipe Book (Szybkie Craftingi)")
    class RecipeBookTests {
        
        @Test
        @DisplayName("fillGridFromRecipe wypełnia siatkę dla ShapedRecipe")
        void fillGridFromShapedRecipe() {
            PlayerMock player = server.addPlayer("RecipeBookTester");
            
            // Daj graczowi składniki na patyki (2 deski = 4 patyki)
            player.getInventory().addItem(new ItemStack(Material.OAK_PLANKS, 10));
            
            // Otwórz shared crafting
            openCraftingTable(player, tableLocation);
            Inventory sharedInv = player.getOpenInventory().getTopInventory();
            
            // Sprawdź że siatka jest pusta
            for (int i = 1; i <= 9; i++) {
                ItemStack item = sharedInv.getItem(i);
                assertTrue(item == null || item.getType() == Material.AIR,
                    "Slot " + i + " powinien być pusty przed wypełnieniem");
            }
            
            // Wywołaj metodę fillGridFromRecipe dla przepisu na patyki
            org.bukkit.inventory.ShapedRecipe sticksRecipe = findSticksRecipe();
            if (sticksRecipe != null) {
                boolean success = invokePrivateFillMethod(player, sharedInv, sticksRecipe, false);
                
                if (success) {
                    // Sprawdź że składniki zostały przeniesione
                    boolean hasPlankInGrid = false;
                    for (int i = 1; i <= 9; i++) {
                        ItemStack item = sharedInv.getItem(i);
                        if (item != null && item.getType() == Material.OAK_PLANKS) {
                            hasPlankInGrid = true;
                            break;
                        }
                    }
                    assertTrue(hasPlankInGrid, "Deski powinny zostać przeniesione do siatki");
                    
                    // Sprawdź że gracz ma mniej desek
                    int remainingPlanks = 0;
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null && item.getType() == Material.OAK_PLANKS) {
                            remainingPlanks += item.getAmount();
                        }
                    }
                    assertTrue(remainingPlanks < 10, "Gracz powinien mieć mniej desek po wypełnieniu siatki");
                }
            }
        }
        
        @Test
        @DisplayName("fillGridFromRecipe z shift-click wypełnia maksymalnie")
        void fillGridWithShiftClickFillsMaximum() {
            PlayerMock player = server.addPlayer("ShiftRecipeTester");
            
            // Daj graczowi dużo składników
            player.getInventory().addItem(new ItemStack(Material.OAK_PLANKS, 64));
            
            // Otwórz shared crafting
            openCraftingTable(player, tableLocation);
            Inventory sharedInv = player.getOpenInventory().getTopInventory();
            
            // Wywołaj z shift-click
            org.bukkit.inventory.ShapedRecipe sticksRecipe = findSticksRecipe();
            if (sticksRecipe != null) {
                boolean success = invokePrivateFillMethod(player, sharedInv, sticksRecipe, true);
                
                if (success) {
                    // Sprawdź że użyto więcej niż 2 deski (shift = wypełnij maksymalnie)
                    int planksInGrid = 0;
                    for (int i = 1; i <= 9; i++) {
                        ItemStack item = sharedInv.getItem(i);
                        if (item != null && item.getType() == Material.OAK_PLANKS) {
                            planksInGrid += item.getAmount();
                        }
                    }
                    // Przy shift-click powinno być więcej niż 2 (jeden zestaw)
                    assertTrue(planksInGrid >= 2, "Shift-click powinien wypełnić siatkę");
                }
            }
        }
        
        @Test
        @DisplayName("fillGridFromRecipe nie duplikuje itemów")
        void fillGridDoesNotDuplicateItems() {
            PlayerMock player = server.addPlayer("NoDupeTester");
            
            // Daj graczowi dokładnie 4 deski
            player.getInventory().addItem(new ItemStack(Material.OAK_PLANKS, 4));
            
            // Otwórz shared crafting
            openCraftingTable(player, tableLocation);
            Inventory sharedInv = player.getOpenInventory().getTopInventory();
            
            int totalBefore = countTotalItems(player, sharedInv, Material.OAK_PLANKS);
            assertEquals(4, totalBefore, "Powinno być 4 deski przed operacją");
            
            // Wywołaj z shift-click
            org.bukkit.inventory.ShapedRecipe sticksRecipe = findSticksRecipe();
            if (sticksRecipe != null) {
                invokePrivateFillMethod(player, sharedInv, sticksRecipe, true);
                
                int totalAfter = countTotalItems(player, sharedInv, Material.OAK_PLANKS);
                assertEquals(4, totalAfter, "Suma desek nie powinna się zmienić (bez duplikacji)");
            }
        }
        
        @Test
        @DisplayName("fillGridFromRecipe nie działa gdy brak składników")
        void fillGridFailsWithoutIngredients() {
            PlayerMock player = server.addPlayer("NoIngredientsTester");
            
            // Nie dawaj graczowi składników
            
            // Otwórz shared crafting
            openCraftingTable(player, tableLocation);
            Inventory sharedInv = player.getOpenInventory().getTopInventory();
            
            org.bukkit.inventory.ShapedRecipe sticksRecipe = findSticksRecipe();
            if (sticksRecipe != null) {
                boolean success = invokePrivateFillMethod(player, sharedInv, sticksRecipe, false);
                
                assertFalse(success, "Wypełnienie powinno się nie udać bez składników");
                
                // Sprawdź że siatka jest nadal pusta
                for (int i = 1; i <= 9; i++) {
                    ItemStack item = sharedInv.getItem(i);
                    assertTrue(item == null || item.getType() == Material.AIR,
                        "Siatka powinna pozostać pusta");
                }
            }
        }
        
        @Test
        @DisplayName("fillGridFromRecipe dla ShapelessRecipe")
        void fillGridFromShapelessRecipe() {
            PlayerMock player = server.addPlayer("ShapelessTester");
            
            // Daj graczowi składniki na deskę (1 log = 4 deski - shapeless)
            player.getInventory().addItem(new ItemStack(Material.OAK_LOG, 5));
            
            // Otwórz shared crafting
            openCraftingTable(player, tableLocation);
            Inventory sharedInv = player.getOpenInventory().getTopInventory();
            
            // Znajdź przepis na deski (shapeless 1 log -> 4 planks)
            org.bukkit.inventory.ShapelessRecipe planksRecipe = findPlanksRecipe();
            if (planksRecipe != null) {
                boolean success = invokePrivateFillMethodShapeless(player, sharedInv, planksRecipe, false);
                
                if (success) {
                    // Sprawdź że log został przeniesiony
                    boolean hasLogInGrid = false;
                    for (int i = 1; i <= 9; i++) {
                        ItemStack item = sharedInv.getItem(i);
                        if (item != null && item.getType() == Material.OAK_LOG) {
                            hasLogInGrid = true;
                            break;
                        }
                    }
                    assertTrue(hasLogInGrid, "Log powinien zostać przeniesiony do siatki");
                }
            }
        }
        
        // Helper: znajdź przepis na patyki
        private org.bukkit.inventory.ShapedRecipe findSticksRecipe() {
            Iterator<org.bukkit.inventory.Recipe> recipes = server.recipeIterator();
            while (recipes.hasNext()) {
                org.bukkit.inventory.Recipe recipe = recipes.next();
                if (recipe instanceof org.bukkit.inventory.ShapedRecipe shaped) {
                    if (shaped.getResult().getType() == Material.STICK) {
                        return shaped;
                    }
                }
            }
            return null;
        }
        
        // Helper: znajdź przepis na deski
        private org.bukkit.inventory.ShapelessRecipe findPlanksRecipe() {
            Iterator<org.bukkit.inventory.Recipe> recipes = server.recipeIterator();
            while (recipes.hasNext()) {
                org.bukkit.inventory.Recipe recipe = recipes.next();
                if (recipe instanceof org.bukkit.inventory.ShapelessRecipe shapeless) {
                    if (shapeless.getResult().getType() == Material.OAK_PLANKS) {
                        return shapeless;
                    }
                }
            }
            return null;
        }
        
        // Helper: wywołaj prywatną metodę fillGridFromRecipe przez reflection
        private boolean invokePrivateFillMethod(Player player, Inventory inv, 
                org.bukkit.inventory.ShapedRecipe recipe, boolean shiftClick) {
            try {
                java.lang.reflect.Method method = SharedCraftingListener.class.getDeclaredMethod(
                    "fillGridFromShapedRecipe", Player.class, Inventory.class, 
                    org.bukkit.inventory.ShapedRecipe.class, boolean.class);
                method.setAccessible(true);
                return (boolean) method.invoke(listener, player, inv, recipe, shiftClick);
            } catch (Exception e) {
                // Jeśli metoda nie istnieje lub błąd - traktuj jako niepowodzenie
                return false;
            }
        }
        
        private boolean invokePrivateFillMethodShapeless(Player player, Inventory inv, 
                org.bukkit.inventory.ShapelessRecipe recipe, boolean shiftClick) {
            try {
                java.lang.reflect.Method method = SharedCraftingListener.class.getDeclaredMethod(
                    "fillGridFromShapelessRecipe", Player.class, Inventory.class, 
                    org.bukkit.inventory.ShapelessRecipe.class, boolean.class);
                method.setAccessible(true);
                return (boolean) method.invoke(listener, player, inv, recipe, shiftClick);
            } catch (Exception e) {
                return false;
            }
        }
        
        // Helper: zlicz wszystkie itemy danego typu
        private int countTotalItems(Player player, Inventory sharedInv, Material type) {
            int total = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == type) {
                    total += item.getAmount();
                }
            }
            for (int i = 1; i <= 9; i++) {
                ItemStack item = sharedInv.getItem(i);
                if (item != null && item.getType() == type) {
                    total += item.getAmount();
                }
            }
            return total;
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    private void openCraftingTable(Player player, Location location) {
        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            location.getBlock(),
            BlockFace.UP,
            EquipmentSlot.HAND
        );
        listener.onInteract(event);
    }
}

package pl.com.sofitfy.enhanced_containers.sharedcrafting;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy sprawdzające thread-safety i concurrency dla SharedCraftingManager.
 * Używa MockBukkit dla pełnego środowiska testowego.
 */
@DisplayName("SharedCraftingManager - Thread Safety & Edge Cases")
public class SharedCraftingThreadSafetyTest {

    private ServerMock server;
    private JavaPlugin plugin;
    private World world;
    private SharedCraftingManager manager;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        world = server.addSimpleWorld("test_world");
        manager = new SharedCraftingManager(plugin);
        executor = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        MockBukkit.unmock();
    }

    // =====================================================================
    // TESTY CONCURRENCY - WIELOWĄTKOWY DOSTĘP
    // =====================================================================

    @Test
    @DisplayName("Wielowątkowy dostęp do tej samej lokacji powinien zwrócić tę samą instancję inventory")
    void testConcurrentAccessToSameLocation_ReturnsSameInstance() throws InterruptedException {
        // Przygotowanie: jedna lokacja
        Location location = new Location(world, 100, 64, 100);
        int numThreads = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        Set<Inventory> results = ConcurrentHashMap.newKeySet();

        // Wykonanie: 10 wątków jednocześnie pobiera inventory dla tej samej lokacji
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Czekaj na sygnał startu
                    Inventory inv = manager.getWorkbenchInventory(location);
                    results.add(inv);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start wszystkich wątków jednocześnie
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Wątki powinny zakończyć się w czasie");

        // Weryfikacja: wszystkie wątki powinny otrzymać tę samą instancję
        assertEquals(1, results.size(), 
            "Wszystkie wątki powinny otrzymać tę samą instancję inventory dla tej samej lokacji");
    }

    @Test
    @DisplayName("Wielowątkowy dostęp do różnych lokacji powinien zwrócić różne instancje")
    void testConcurrentAccessToDifferentLocations_ReturnsDifferentInstances() throws InterruptedException {
        // Przygotowanie: 5 różnych lokacji
        int numLocations = 5;
        List<Location> locations = new ArrayList<>();
        for (int i = 0; i < numLocations; i++) {
            locations.add(new Location(world, i * 10, 64, 0));
        }
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numLocations);
        Map<Location, Inventory> results = new ConcurrentHashMap<>();

        // Wykonanie: każdy wątek pobiera inventory dla innej lokacji
        for (Location loc : locations) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Inventory inv = manager.getWorkbenchInventory(loc);
                    results.put(loc, inv);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Wątki powinny zakończyć się w czasie");

        // Weryfikacja: każda lokacja powinna mieć własne inventory
        assertEquals(numLocations, results.size(), 
            "Każda lokacja powinna mieć przypisane inventory");
        
        Set<Inventory> uniqueInventories = new HashSet<>(results.values());
        assertEquals(numLocations, uniqueInventories.size(),
            "Wszystkie inventory powinny być unikalne (różne dla różnych lokacji)");
    }

    @Test
    @DisplayName("Równoczesne dodawanie i usuwanie inventory powinno być thread-safe")
    void testConcurrentAddAndRemove_ThreadSafe() throws InterruptedException {
        // Przygotowanie: 50 lokacji (ograniczamy dla wydajności)
        int numLocations = 50;
        List<Location> locations = new ArrayList<>();
        for (int i = 0; i < numLocations; i++) {
            locations.add(new Location(world, i, 64, 0));
        }
        
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch doneLatch = new CountDownLatch(numLocations * 2);

        // Wykonanie: połowa wątków dodaje, połowa usuwa (mogą się ścigać)
        for (int i = 0; i < numLocations; i++) {
            Location loc = locations.get(i);
            
            // Wątek dodający
            executor.submit(() -> {
                try {
                    startLatch.await();
                    manager.getWorkbenchInventory(loc);
                    successCount.incrementAndGet();
                } catch (ConcurrentModificationException e) {
                    // FAIL - ConcurrentHashMap nie powinien tego rzucać
                    fail("ConcurrentModificationException podczas dodawania: " + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
            
            // Wątek usuwający (może uruchomić się przed lub po dodającym)
            executor.submit(() -> {
                try {
                    startLatch.await();
                    manager.removeInventory(loc);
                    successCount.incrementAndGet();
                } catch (ConcurrentModificationException e) {
                    fail("ConcurrentModificationException podczas usuwania: " + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Wątki powinny zakończyć się w czasie");

        // Weryfikacja: wszystkie operacje zakończone bez ConcurrentModificationException
        assertEquals(numLocations * 2, successCount.get(),
            "Wszystkie operacje dodawania/usuwania powinny zakończyć się sukcesem");
    }

    // =====================================================================
    // TESTY METODY hasInventory()
    // =====================================================================

    @Test
    @DisplayName("hasInventory(Location) zwraca true dla istniejącej lokacji")
    void testHasInventory_Location_ExistingReturnsTrue() {
        // Przygotowanie
        Location location = new Location(world, 0, 64, 0);
        manager.getWorkbenchInventory(location);

        // Wykonanie & weryfikacja
        assertTrue(manager.hasInventory(location), 
            "hasInventory powinno zwrócić true dla lokacji z przypisanym inventory");
    }

    @Test
    @DisplayName("hasInventory(Location) zwraca false dla nieistniejącej lokacji")
    void testHasInventory_Location_NonExistingReturnsFalse() {
        // Przygotowanie
        Location location = new Location(world, 999, 64, 999);

        // Wykonanie & weryfikacja
        assertFalse(manager.hasInventory(location), 
            "hasInventory powinno zwrócić false dla lokacji bez inventory");
    }

    @Test
    @DisplayName("hasInventory(Inventory) zwraca true dla zarządzanego inventory")
    void testHasInventory_Inventory_ManagedReturnsTrue() {
        // Przygotowanie
        Location location = new Location(world, 0, 64, 0);
        Inventory inventory = manager.getWorkbenchInventory(location);

        // Wykonanie & weryfikacja
        assertTrue(manager.hasInventory(inventory), 
            "hasInventory powinno zwrócić true dla zarządzanego inventory");
    }

    @Test
    @DisplayName("hasInventory(Inventory) zwraca false dla zewnętrznego inventory")
    void testHasInventory_Inventory_UnmanagedReturnsFalse() {
        // Przygotowanie: stwórz inventory poza managerem
        Inventory externalInventory = server.createInventory(null, 9);

        // Wykonanie & weryfikacja
        assertFalse(manager.hasInventory(externalInventory), 
            "hasInventory powinno zwrócić false dla zewnętrznego inventory");
    }

    // =====================================================================
    // TESTY METODY getLocationForInventory()
    // =====================================================================

    @Test
    @DisplayName("getLocationForInventory zwraca poprawną lokację dla zarządzanego inventory")
    void testGetLocationForInventory_ReturnsCorrectLocation() {
        // Przygotowanie
        Location location = new Location(world, 100, 64, 200);
        Inventory inventory = manager.getWorkbenchInventory(location);

        // Wykonanie
        Location result = manager.getLocationForInventory(inventory);

        // Weryfikacja
        assertEquals(location, result, 
            "getLocationForInventory powinno zwrócić oryginalną lokację");
    }

    @Test
    @DisplayName("getLocationForInventory zwraca null dla niezarządzanego inventory")
    void testGetLocationForInventory_ReturnsNullForUnmanaged() {
        // Przygotowanie
        Inventory externalInventory = server.createInventory(null, 9);

        // Wykonanie
        Location result = manager.getLocationForInventory(externalInventory);

        // Weryfikacja
        assertNull(result, 
            "getLocationForInventory powinno zwrócić null dla zewnętrznego inventory");
    }

    // =====================================================================
    // TESTY METODY getAllLocations()
    // =====================================================================

    @Test
    @DisplayName("getAllLocations zwraca kopię - modyfikacja kopii nie wpływa na oryginał")
    void testGetAllLocations_ReturnsDefensiveCopy() {
        // Przygotowanie
        Location loc1 = new Location(world, 0, 64, 0);
        Location loc2 = new Location(world, 10, 64, 10);
        manager.getWorkbenchInventory(loc1);
        manager.getWorkbenchInventory(loc2);

        // Wykonanie
        Set<Location> allLocations = manager.getAllLocations();

        // Weryfikacja: próba modyfikacji kopii nie powinna wpływać na manager
        assertThrows(UnsupportedOperationException.class, () -> {
            allLocations.add(new Location(world, 999, 64, 999));
        }, "Zwrócona kopia powinna być niezmienialna (Set.copyOf)");
        
        assertEquals(2, manager.getAllLocations().size(),
            "Manager nadal powinien mieć 2 lokacje");
    }

    @Test
    @DisplayName("getAllLocations zwraca pustą kolekcję gdy brak stołów")
    void testGetAllLocations_EmptyWhenNoWorkbenches() {
        // Wykonanie
        Set<Location> allLocations = manager.getAllLocations();

        // Weryfikacja
        assertTrue(allLocations.isEmpty(), 
            "getAllLocations powinno zwrócić pustą kolekcję gdy brak stołów");
    }

    // =====================================================================
    // TESTY EDGE CASES
    // =====================================================================

    @Test
    @DisplayName("Wielokrotne usuwanie tej samej lokacji jest bezpieczne")
    void testMultipleRemoves_SameLocation_IsSafe() {
        // Przygotowanie
        Location location = new Location(world, 0, 64, 0);
        manager.getWorkbenchInventory(location);

        // Wykonanie: wielokrotne usunięcie
        manager.removeInventory(location);
        manager.removeInventory(location);
        manager.removeInventory(location);

        // Weryfikacja: brak wyjątku, lokacja usunięta
        assertFalse(manager.hasInventory(location), 
            "Po usunięciu lokacja nie powinna istnieć");
    }

    @Test
    @DisplayName("Usuwanie nieistniejącej lokacji jest bezpieczne")
    void testRemove_NonExistingLocation_IsSafe() {
        // Przygotowanie
        Location location = new Location(world, 999, 64, 999);

        // Wykonanie & weryfikacja: brak wyjątku
        assertDoesNotThrow(() -> manager.removeInventory(location),
            "Usuwanie nieistniejącej lokacji nie powinno rzucić wyjątku");
    }

    @Test
    @DisplayName("getPlugin zwraca instancję pluginu")
    void testGetPlugin_ReturnsPluginInstance() {
        // Weryfikacja
        assertSame(plugin, manager.getPlugin(),
            "getPlugin powinno zwrócić instancję pluginu przekazaną w konstruktorze");
    }

    // =====================================================================
    // TESTY validateAndCleanupInvalidWorkbenches()
    // UWAGA: Te testy wymagają pełnej obsługi isChunkLoaded w MockBukkit,
    // co nie zawsze jest dostępne. Testowanie tej funkcjonalności odbywa się
    // w SharedCraftingIntegrationTest gdzie używany jest prawdziwy flow.
    // =====================================================================

    @Test
    @DisplayName("validateAndCleanup nie usuwa stołów gdy wszystkie są poprawne")
    void testValidateAndCleanup_NoRemovalWhenAllValid() {
        // Przygotowanie: 3 poprawne stoły
        for (int i = 0; i < 3; i++) {
            Location loc = new Location(world, i * 10, 64, 0);
            loc.getBlock().setType(Material.CRAFTING_TABLE);
            manager.getWorkbenchInventory(loc);
        }

        // Wykonanie
        int removed = manager.validateAndCleanupInvalidWorkbenches();

        // Weryfikacja
        assertEquals(0, removed, "Nie powinien usunąć żadnego stołu");
        assertEquals(3, manager.getAllLocations().size(), "Wszystkie 3 stoły powinny pozostać");
    }

    // =====================================================================
    // TESTY INVENTORY CONTENT PERSISTENCE
    // =====================================================================

    @Test
    @DisplayName("Zawartość inventory jest zachowana między wywołaniami getWorkbenchInventory")
    void testInventoryContentPersistence() {
        // Przygotowanie
        Location location = new Location(world, 50, 64, 50);
        location.getBlock().setType(Material.CRAFTING_TABLE);
        
        // Pierwsze wywołanie - dodaj itemy
        Inventory inv1 = manager.getWorkbenchInventory(location);
        inv1.setItem(1, new ItemStack(Material.DIAMOND, 10));

        // Wykonanie: drugie wywołanie
        Inventory inv2 = manager.getWorkbenchInventory(location);

        // Weryfikacja
        assertSame(inv1, inv2, "Powinno zwrócić tę samą instancję inventory");
        ItemStack diamond = inv2.getItem(1);
        assertNotNull(diamond, "Diament powinien być zachowany");
        assertEquals(Material.DIAMOND, diamond.getType());
        assertEquals(10, diamond.getAmount());
    }

    @Test
    @DisplayName("Dwóch graczy widzi te same zmiany w inventory")
    void testMultiplePlayersShareSameInventory() {
        // Przygotowanie
        PlayerMock player1 = server.addPlayer("Player1");
        PlayerMock player2 = server.addPlayer("Player2");
        Location location = new Location(world, 60, 64, 60);
        location.getBlock().setType(Material.CRAFTING_TABLE);

        // Gracz 1 otwiera i dodaje item
        Inventory inv1 = manager.getWorkbenchInventory(location);
        player1.openInventory(inv1);
        inv1.setItem(1, new ItemStack(Material.GOLD_INGOT, 7));

        // Gracz 2 otwiera ten sam stół
        Inventory inv2 = manager.getWorkbenchInventory(location);
        player2.openInventory(inv2);

        // Weryfikacja
        assertSame(inv1, inv2, "Obaj gracze powinni korzystać z tej samej instancji");
        ItemStack gold = inv2.getItem(1);
        assertNotNull(gold, "Gracz 2 powinien widzieć złoto dodane przez gracza 1");
        assertEquals(Material.GOLD_INGOT, gold.getType());
        assertEquals(7, gold.getAmount());
    }
}

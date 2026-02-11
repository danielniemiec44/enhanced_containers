package pl.com.sofitfy.enhanced_containers.sharedcrafting;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy weryfikujące problem "szybkiego craftingu" w shared crafting.
 * 
 * BUG REPORT: Klient zgłosił, że szybkie klikanie shift+click na wynik 
 * craftingu w shared crafting table powoduje "zjadanie" itemów - składniki
 * są konsumowane, ale wynik nie jest dodawany do ekwipunku gracza.
 * 
 * Scenariusze testowe:
 * 1. Szybkie wielokrotne shift-clicki jednego gracza
 * 2. Równoczesne shift-clicki dwóch graczy na ten sam stół
 * 3. Shift-click gdy inny gracz modyfikuje składniki
 * 4. Race condition: klik gdy składniki są w trakcie konsumowania
 */
@DisplayName("SharedCrafting - Rapid Click Bug Tests (Item Eating)")
public class SharedCraftingRapidClickTest {

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

    // =====================================================================
    // TEST 1: Szybkie wielokrotne shift-clicki jednego gracza
    // =====================================================================

    @Test
    @DisplayName("BUG: Szybkie shift-clicki nie powinny zjadać itemów")
    void testRapidShiftClicks_ShouldNotEatItems() {
        // Przygotowanie
        PlayerMock player = server.addPlayer("RapidClicker");
        openSharedInventory(player);
        Inventory sharedInv = player.getOpenInventory().getTopInventory();

        // Setup: 8 desek w slocie 1 -> powinno dać 8 craftów (8 * 4 = 32 patyki)
        // Używamy receptury: 2 deski pionowo = 4 patyki
        sharedInv.setItem(1, new ItemStack(Material.OAK_PLANKS, 8)); // Górny
        sharedInv.setItem(4, new ItemStack(Material.OAK_PLANKS, 8)); // Środkowy (pod górnym)
        sharedInv.setItem(0, new ItemStack(Material.STICK, 4)); // Wynik

        int initialPlanks = 16; // 8 + 8
        int expectedCrafts = 8;
        int expectedSticks = expectedCrafts * 4; // 32

        // Symulacja: 5 szybkich shift-clicków (jakby gracz spamował)
        for (int i = 0; i < 5; i++) {
            InventoryClickEvent event = createShiftClickEvent(player, sharedInv);
            listener.onInventoryClick(event);
        }

        // Weryfikacja: policz wszystkie patyki
        int totalSticks = countItems(player.getInventory(), Material.STICK);
        
        // Policz pozostałe deski
        int remainingPlanks = 0;
        ItemStack slot1 = sharedInv.getItem(1);
        ItemStack slot4 = sharedInv.getItem(4);
        if (slot1 != null && slot1.getType() == Material.OAK_PLANKS) {
            remainingPlanks += slot1.getAmount();
        }
        if (slot4 != null && slot4.getType() == Material.OAK_PLANKS) {
            remainingPlanks += slot4.getAmount();
        }

        // ASERCJA KLUCZOWA: suma itemów powinna się zgadzać!
        // consumed = initialPlanks - remainingPlanks
        // expectedOutput = (consumed / 2) * 4 (bo 2 deski = 4 patyki)
        int consumed = initialPlanks - remainingPlanks;
        int expectedOutput = (consumed / 2) * 4;
        
        assertEquals(expectedOutput, totalSticks, 
            "Liczba patyków powinna odpowiadać zużytym deskom! " +
            "Zużyto " + consumed + " desek, oczekiwano " + expectedOutput + " patyków, otrzymano " + totalSticks);
        
        // Dodatkowa weryfikacja: nie powinno być więcej craftów niż możliwe
        assertTrue(totalSticks <= expectedSticks, 
            "Nie powinno być więcej patyków niż maksymalnie możliwe (" + expectedSticks + ")");
    }

    @Test
    @DisplayName("BUG: Shift-click podczas gdy inventory jest puste nie powinien crashować")
    void testShiftClickOnEmptyResult_ShouldBeNoOp() {
        // Przygotowanie
        PlayerMock player = server.addPlayer("EmptyClicker");
        openSharedInventory(player);
        Inventory sharedInv = player.getOpenInventory().getTopInventory();

        // Grid pusty, wynik pusty
        sharedInv.clear();
        
        // Szybkie clicki na pusty wynik
        for (int i = 0; i < 10; i++) {
            InventoryClickEvent event = createShiftClickEvent(player, sharedInv);
            assertDoesNotThrow(() -> listener.onInventoryClick(event),
                "Klik na pusty wynik nie powinien rzucić wyjątku");
        }

        // Weryfikacja: nic nie zostało dodane
        assertTrue(player.getInventory().isEmpty(), 
            "Ekwipunek gracza powinien być pusty po klikach na pusty wynik");
    }

    // =====================================================================
    // TEST 2: Równoczesne shift-clicki dwóch graczy
    // =====================================================================

    @Test
    @DisplayName("BUG: Dwóch graczy klikających jednocześnie - konserwacja itemów")
    void testTwoPlayersSimultaneousClicks_ItemConservation() {
        // Przygotowanie
        PlayerMock player1 = server.addPlayer("Player1");
        PlayerMock player2 = server.addPlayer("Player2");
        
        // Obaj otwierają ten sam stół
        openSharedInventory(player1);
        openSharedInventory(player2);
        
        Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);

        // Setup: 16 desek w każdym slocie = 32 total
        // Maksymalnie 16 craftów = 64 patyki
        sharedInv.setItem(1, new ItemStack(Material.OAK_PLANKS, 16));
        sharedInv.setItem(4, new ItemStack(Material.OAK_PLANKS, 16));
        sharedInv.setItem(0, new ItemStack(Material.STICK, 4));

        int initialPlanks = 32;
        int maxPossibleSticks = 64;

        // Symulacja: obaj gracze klikają naprzemiennie
        for (int i = 0; i < 10; i++) {
            InventoryClickEvent event1 = createShiftClickEvent(player1, sharedInv);
            InventoryClickEvent event2 = createShiftClickEvent(player2, sharedInv);
            
            listener.onInventoryClick(event1);
            listener.onInventoryClick(event2);
        }

        // Policz wyniki
        int sticksPlayer1 = countItems(player1.getInventory(), Material.STICK);
        int sticksPlayer2 = countItems(player2.getInventory(), Material.STICK);
        int totalSticks = sticksPlayer1 + sticksPlayer2;

        // Policz pozostałe deski
        int remainingPlanks = 0;
        ItemStack slot1 = sharedInv.getItem(1);
        ItemStack slot4 = sharedInv.getItem(4);
        if (slot1 != null && slot1.getType() == Material.OAK_PLANKS) {
            remainingPlanks += slot1.getAmount();
        }
        if (slot4 != null && slot4.getType() == Material.OAK_PLANKS) {
            remainingPlanks += slot4.getAmount();
        }

        // ASERCJA KLUCZOWA: bilans musi się zgadzać!
        int consumed = initialPlanks - remainingPlanks;
        int expectedOutput = (consumed / 2) * 4;
        
        assertEquals(expectedOutput, totalSticks, 
            "Suma patyków obu graczy (" + totalSticks + ") powinna odpowiadać zużytym deskom (" + consumed + " -> " + expectedOutput + " patyków)");
        
        assertTrue(totalSticks <= maxPossibleSticks, 
            "Suma patyków nie może przekroczyć maksymalnej możliwej ilości (" + maxPossibleSticks + ")");
    }

    // =====================================================================
    // TEST 3: Shift-click gdy inny gracz modyfikuje składniki
    // =====================================================================

    @Test
    @DisplayName("BUG: Klik gdy składniki są modyfikowane przez innego gracza")
    void testClickWhileIngredientsBeingModified() {
        // Przygotowanie
        PlayerMock crafter = server.addPlayer("Crafter");
        PlayerMock modifier = server.addPlayer("Modifier");
        
        openSharedInventory(crafter);
        openSharedInventory(modifier);
        
        Inventory sharedInv = manager.getWorkbenchInventory(tableLocation);

        // Setup: crafting gotowy
        sharedInv.setItem(1, new ItemStack(Material.OAK_PLANKS, 10));
        sharedInv.setItem(4, new ItemStack(Material.OAK_PLANKS, 10));
        sharedInv.setItem(0, new ItemStack(Material.STICK, 4));

        int initialPlanks = 20;

        // Crafter klika shift
        InventoryClickEvent craftEvent = createShiftClickEvent(crafter, sharedInv);
        listener.onInventoryClick(craftEvent);

        // Modifier zabiera część składników (symulacja - bezpośrednia modyfikacja)
        ItemStack slot1 = sharedInv.getItem(1);
        if (slot1 != null && slot1.getAmount() > 5) {
            slot1.setAmount(slot1.getAmount() - 5);
            modifier.getInventory().addItem(new ItemStack(Material.OAK_PLANKS, 5));
        }

        // Crafter ponownie klika
        InventoryClickEvent craftEvent2 = createShiftClickEvent(crafter, sharedInv);
        listener.onInventoryClick(craftEvent2);

        // Weryfikacja bilansu
        int sticksPlayer = countItems(crafter.getInventory(), Material.STICK);
        int planksModifier = countItems(modifier.getInventory(), Material.OAK_PLANKS);
        
        int remainingPlanks = 0;
        slot1 = sharedInv.getItem(1);
        ItemStack slot4 = sharedInv.getItem(4);
        if (slot1 != null && slot1.getType() == Material.OAK_PLANKS) {
            remainingPlanks += slot1.getAmount();
        }
        if (slot4 != null && slot4.getType() == Material.OAK_PLANKS) {
            remainingPlanks += slot4.getAmount();
        }

        // Bilans: initialPlanks = remainingPlanks + consumedForCrafting + takenByModifier
        // consumedForCrafting = sticksPlayer / 4 * 2 (2 deski na 4 patyki)
        int consumedForCrafting = (sticksPlayer / 4) * 2;
        int totalAccountedPlanks = remainingPlanks + consumedForCrafting + planksModifier;

        assertEquals(initialPlanks, totalAccountedPlanks, 
            "Bilans desek musi się zgadzać! Initial=" + initialPlanks + 
            ", remaining=" + remainingPlanks + ", consumed=" + consumedForCrafting + 
            ", taken=" + planksModifier + ", total=" + totalAccountedPlanks);
    }

    // =====================================================================
    // TEST 4: Pojedynczy normalny klik (weryfikacja bazowej funkcjonalności)
    // =====================================================================

    @Test
    @DisplayName("Pojedynczy normalny klik powinien działać poprawnie")
    void testSingleNormalClick_Works() {
        // Przygotowanie
        PlayerMock player = server.addPlayer("NormalClicker");
        openSharedInventory(player);
        Inventory sharedInv = player.getOpenInventory().getTopInventory();

        // Setup: 1 drewno = 4 deski (prosta receptura)
        sharedInv.setItem(1, new ItemStack(Material.OAK_PLANKS, 2));
        sharedInv.setItem(4, new ItemStack(Material.OAK_PLANKS, 2));
        sharedInv.setItem(0, new ItemStack(Material.STICK, 4));

        // Pojedynczy NORMALNY klik (nie shift)
        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            org.bukkit.event.inventory.InventoryType.SlotType.RESULT,
            0,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );
        listener.onInventoryClick(event);

        // Weryfikacja: 4 patyki na kursorze (lub w inventory zależnie od implementacji)
        ItemStack cursor = player.getOpenInventory().getCursor();
        int cursorSticks = (cursor != null && cursor.getType() == Material.STICK) ? cursor.getAmount() : 0;
        int invSticks = countItems(player.getInventory(), Material.STICK);
        
        assertEquals(4, cursorSticks + invSticks, 
            "Powinno być dokładnie 4 patyki po pojedynczym kliknięciu");

        // Składniki powinny być zużyte (1 z każdego slotu)
        ItemStack slot1 = sharedInv.getItem(1);
        ItemStack slot4 = sharedInv.getItem(4);
        int remaining1 = (slot1 != null && slot1.getType() == Material.OAK_PLANKS) ? slot1.getAmount() : 0;
        int remaining4 = (slot4 != null && slot4.getType() == Material.OAK_PLANKS) ? slot4.getAmount() : 0;
        
        assertEquals(1, remaining1, "Slot 1 powinien mieć 1 deskę (2-1)");
        assertEquals(1, remaining4, "Slot 4 powinien mieć 1 deskę (2-1)");
    }

    // =====================================================================
    // TEST 5: Sprawdzenie że crafting ZAWSZE konsumuje składniki atomowo
    // =====================================================================

    @Test
    @DisplayName("Crafting powinien być atomowy - albo pełny sukces, albo brak zmian")
    void testCraftingIsAtomic() {
        // Przygotowanie
        PlayerMock player = server.addPlayer("AtomicTester");
        openSharedInventory(player);
        Inventory sharedInv = player.getOpenInventory().getTopInventory();

        // Wypełnij ekwipunek gracza - zostaw miejsce tylko na 8 patyków (2 crafty)
        for (int i = 0; i < 35; i++) {
            player.getInventory().setItem(i, new ItemStack(Material.DIRT, 64));
        }
        // Slot 35 pusty - miejsce na 64 patyki, ale chcemy ograniczyć do 8
        // Dajemy 56 patyków, więc zostaje miejsce na 8
        player.getInventory().setItem(35, new ItemStack(Material.STICK, 56));

        // Setup: dużo składników (10 w każdym slocie = 10 możliwych craftów = 40 patyków)
        sharedInv.setItem(1, new ItemStack(Material.OAK_PLANKS, 10));
        sharedInv.setItem(4, new ItemStack(Material.OAK_PLANKS, 10));
        sharedInv.setItem(0, new ItemStack(Material.STICK, 4));

        int initialPlanks = 20;
        int initialSticks = 56; // Już w inventory

        // Shift-click
        InventoryClickEvent event = createShiftClickEvent(player, sharedInv);
        listener.onInventoryClick(event);

        // Policz patyki (odejmij początkowe 56)
        int totalSticks = countItems(player.getInventory(), Material.STICK);
        int craftedSticks = totalSticks - initialSticks;
        
        // Policz pozostałe deski
        int remainingPlanks = 0;
        ItemStack slot1 = sharedInv.getItem(1);
        ItemStack slot4 = sharedInv.getItem(4);
        if (slot1 != null && slot1.getType() == Material.OAK_PLANKS) {
            remainingPlanks += slot1.getAmount();
        }
        if (slot4 != null && slot4.getType() == Material.OAK_PLANKS) {
            remainingPlanks += slot4.getAmount();
        }

        // Bilans MUSI się zgadzać
        int consumed = initialPlanks - remainingPlanks;
        int expectedSticks = (consumed / 2) * 4;

        assertEquals(expectedSticks, craftedSticks, 
            "Atomowość: zużyte deski (" + consumed + ") muszą odpowiadać scraftowanym patykom (" + craftedSticks + "), oczekiwano " + expectedSticks);
        
        // Dodatkowa asercja: nie powinno być więcej niż 2 crafty (8 patyków) bo brak miejsca
        assertTrue(craftedSticks <= 8, 
            "Przy ograniczonym miejscu (8 patyków) nie powinno być więcej craftów. Otrzymano: " + craftedSticks);
    }

    // =====================================================================
    // HELPER METHODS
    // =====================================================================

    private void openSharedInventory(Player player) {
        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            tableLocation.getBlock(),
            org.bukkit.block.BlockFace.UP,
            EquipmentSlot.HAND
        );
        listener.onInteract(event);
    }

    private InventoryClickEvent createShiftClickEvent(Player player, Inventory topInventory) {
        return new InventoryClickEvent(
            player.getOpenInventory(),
            org.bukkit.event.inventory.InventoryType.SlotType.RESULT,
            0,
            ClickType.SHIFT_LEFT,
            InventoryAction.MOVE_TO_OTHER_INVENTORY
        );
    }

    private int countItems(org.bukkit.inventory.PlayerInventory inventory, Material material) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    // =====================================================================
    // TEST 6: Scenariusz z bug reportu - szybkie klikanie "zjada" itemy
    // =====================================================================

    @Test
    @DisplayName("BUG REPORT: Szybkie klikanie nie powinno gubić itemów - weryfikacja bilansu")
    void testRapidClicking_NoItemLoss_BalanceCheck() {
        // Ten test symuluje scenariusz z bug reportu:
        // Gracz szybko klika shift na wynik, a niektóre crafty "zjadają" składniki
        // bez dodania wyniku do ekwipunku
        
        PlayerMock player = server.addPlayer("BugReporter");
        openSharedInventory(player);
        Inventory sharedInv = player.getOpenInventory().getTopInventory();

        // Setup: 64 drewna -> crafting desek (1 drewno = 4 deski)
        // Używamy prostej receptury gdzie łatwo policzyć bilans
        sharedInv.setItem(1, new ItemStack(Material.OAK_LOG, 64));
        sharedInv.setItem(0, new ItemStack(Material.OAK_PLANKS, 4)); // Wynik

        int initialLogs = 64;
        int planksPerLog = 4;

        // Symulacja BARDZO szybkiego klikania - 20 klików w krótkim czasie
        for (int i = 0; i < 20; i++) {
            // Każdy klik powinien:
            // 1. Sprawdzić czy jest wynik
            // 2. Zjeść składniki
            // 3. Dodać wynik do inventory
            // 4. Przeliczyć nowy wynik
            
            InventoryClickEvent event = createShiftClickEvent(player, sharedInv);
            listener.onInventoryClick(event);
            
            // Symulacja minimalnego opóźnienia między klikami (w rzeczywistości async)
            // W MockBukkit wszystko jest synchroniczne, więc to powinno działać
        }

        // WERYFIKACJA BILANSU
        int remainingLogs = 0;
        ItemStack logSlot = sharedInv.getItem(1);
        if (logSlot != null && logSlot.getType() == Material.OAK_LOG) {
            remainingLogs = logSlot.getAmount();
        }

        int totalPlanks = countItems(player.getInventory(), Material.OAK_PLANKS);
        int consumedLogs = initialLogs - remainingLogs;
        int expectedPlanks = consumedLogs * planksPerLog;

        // KLUCZOWA ASERCJA: bilans musi się zgadzać!
        assertEquals(expectedPlanks, totalPlanks,
            "CRITICAL BUG CHECK: Zużyte drewno (" + consumedLogs + ") * " + planksPerLog + 
            " = " + expectedPlanks + " desek, ale gracz ma " + totalPlanks + " desek. " +
            "Różnica = " + (expectedPlanks - totalPlanks) + " zgubionych desek!");

        // Dodatkowa asercja: wszystko powinno być zużyte (64 crafty)
        assertEquals(0, remainingLogs, 
            "Wszystkie drewna powinny być zużyte przy 20+ klikach shift");
        assertEquals(256, totalPlanks, 
            "Gracz powinien mieć 256 desek (64 * 4)");
    }

    @Test
    @DisplayName("BUG REPORT: Wielokrotne kliknięcia gdy brak składników")
    void testMultipleClicks_WhenNoIngredients() {
        // Test sprawdza czy klikanie gdy nie ma już składników jest bezpieczne
        
        PlayerMock player = server.addPlayer("EmptyTester");
        openSharedInventory(player);
        Inventory sharedInv = player.getOpenInventory().getTopInventory();

        // Setup: tylko 1 drewno
        sharedInv.setItem(1, new ItemStack(Material.OAK_LOG, 1));
        sharedInv.setItem(0, new ItemStack(Material.OAK_PLANKS, 4));

        // Pierwszy klik - zużyje jedyne drewno
        InventoryClickEvent event1 = createShiftClickEvent(player, sharedInv);
        listener.onInventoryClick(event1);

        int planksAfterFirst = countItems(player.getInventory(), Material.OAK_PLANKS);
        assertEquals(4, planksAfterFirst, "Po pierwszym kliku powinno być 4 deski");

        // Kolejne 10 klików na pusty stół
        for (int i = 0; i < 10; i++) {
            InventoryClickEvent event = createShiftClickEvent(player, sharedInv);
            listener.onInventoryClick(event);
        }

        int planksAfterMany = countItems(player.getInventory(), Material.OAK_PLANKS);
        assertEquals(4, planksAfterMany, 
            "Po wielu klikach na pusty stół nadal powinno być tylko 4 deski");

        // Sprawdź że grid jest pusty
        ItemStack logSlot = sharedInv.getItem(1);
        assertTrue(logSlot == null || logSlot.getType() == Material.AIR,
            "Slot 1 powinien być pusty");
    }
}

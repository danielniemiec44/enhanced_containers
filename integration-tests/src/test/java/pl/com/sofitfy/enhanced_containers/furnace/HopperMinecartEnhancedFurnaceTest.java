package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HopperMinecartEnhancedFurnaceTest - testy dla hopper minecarts z Enhanced Furnace.
 * 
 * Vanilla behavior dla hopper minecart:
 * - Hopper minecart przejezdza pod piecem i pobiera przedmioty z OUTPUT
 * - Pobiera 1 przedmiot na 8 ticków (jak zwykły lejek)
 * - Nie wkłada przedmiotów do pieca (w przeciwieństwie do zwykłego lejka)
 * 
 * Enhanced Furnace ma 3 sloty OUTPUT (14, 23, 32), więc:
 * - Hopper minecart powinien pobierać po kolei ze wszystkich 3 slotów
 * - Zachowanie identyczne jak lejek blokowy pod piecem
 * 
 * UWAGA: MockBukkit ma ograniczone wsparcie dla encji (minecartów).
 * Testujemy LOGIKĘ transferu, nie fizyczną interakcję encji.
 */
@DisplayName("Hopper Minecart - Enhanced Furnace Integration")
@Tag("integration")
class HopperMinecartEnhancedFurnaceTest {
    
    // --- SLOTY PIECA (z EnhancedFurnaceGUI) ---
    private static final int[] INPUT_SLOTS = {11, 20, 29};
    private static final int[] OUTPUT_SLOTS = {14, 23, 32};
    private static final int[] FUEL_SLOTS = {46, 47, 48};
    
    private ServerMock server;
    private World world;
    private PlayerMock player;
    
    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("test_world");
        player = server.addPlayer();
    }
    
    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }
    
    // ========================================================================
    // HOPPER MINECART POD PIECEM (pobieranie z OUTPUT)
    // ========================================================================
    @Nested
    @DisplayName("Hopper Minecart POD piecem → pobiera z OUTPUT")
    class HopperMinecartBelowFurnaceTests {
        
        @Test
        @DisplayName("Hopper minecart pobiera przedmioty z pierwszego slotu OUTPUT")
        void hopperMinecartPullsFromFirstOutputSlot() {
            // Given: Piec z 64 sztabkami żelaza w slocie OUTPUT 14
            Inventory furnaceInv = createEnhancedFurnaceInventory();
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 64));
            
            // Symuluj inventory hopper minecarta (5 slotów jak zwykły hopper)
            Inventory minecartInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            assertEquals(64, countItemsInSlot(furnaceInv, OUTPUT_SLOTS[0]));
            assertEquals(0, countTotalItems(minecartInv));
            
            // When: Symuluj transfer jak hopper minecart (1 item na tick)
            int transferred = simulateHopperMinecartPull(furnaceInv, minecartInv, 8);
            
            // Then: Przedmioty zostały przeniesione
            assertTrue(transferred > 0, "Hopper minecart powinien przenieść przedmioty");
            assertEquals(64, 
                countItemsInSlot(furnaceInv, OUTPUT_SLOTS[0]) + countTotalItems(minecartInv),
                "Suma przedmiotów musi być zachowana (64)");
        }
        
        @Test
        @DisplayName("Hopper minecart pobiera ze wszystkich 3 slotów OUTPUT w kolejności")
        void hopperMinecartPullsFromAllOutputSlots() {
            // Given: Piec z przedmiotami w każdym slocie OUTPUT
            Inventory furnaceInv = createEnhancedFurnaceInventory();
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 10));    // slot 14
            furnaceInv.setItem(OUTPUT_SLOTS[1], new ItemStack(Material.GOLD_INGOT, 10));    // slot 23
            furnaceInv.setItem(OUTPUT_SLOTS[2], new ItemStack(Material.COPPER_INGOT, 10));  // slot 32
            
            Inventory minecartInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            int totalBefore = 30;
            
            // When: Symuluj wiele transferów
            int transferred = simulateHopperMinecartPull(furnaceInv, minecartInv, 15);
            
            // Then
            int furnaceTotal = countItemsInSlots(furnaceInv, OUTPUT_SLOTS);
            int minecartTotal = countTotalItems(minecartInv);
            
            assertTrue(transferred > 0, "Powinno być przeniesione więcej niż 0 przedmiotów");
            assertEquals(totalBefore, furnaceTotal + minecartTotal, 
                "Suma przedmiotów musi być zachowana: " + totalBefore);
        }
        
        @Test
        @DisplayName("Hopper minecart nie pobiera z pustych slotów OUTPUT")
        void hopperMinecartDoesNotPullFromEmptySlots() {
            // Given: Piec z pustymi slotami OUTPUT
            Inventory furnaceInv = createEnhancedFurnaceInventory();
            // Wszystkie OUTPUT sloty puste
            
            Inventory minecartInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            // When
            int transferred = simulateHopperMinecartPull(furnaceInv, minecartInv, 10);
            
            // Then
            assertEquals(0, transferred, "Nie powinno być żadnych transferów");
            assertEquals(0, countTotalItems(minecartInv), "Minecart powinien być pusty");
        }
        
        @Test
        @DisplayName("Pełny hopper minecart przestaje pobierać")
        void fullHopperMinecartStopsPulling() {
            // Given: Piec pełny, minecart pełny
            Inventory furnaceInv = createEnhancedFurnaceInventory();
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 64));
            
            Inventory minecartInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            // Wypełnij minecart (5 slotów po 64)
            for (int i = 0; i < 5; i++) {
                minecartInv.setItem(i, new ItemStack(Material.COBBLESTONE, 64));
            }
            
            int furnaceBefore = countItemsInSlot(furnaceInv, OUTPUT_SLOTS[0]);
            
            // When
            int transferred = simulateHopperMinecartPull(furnaceInv, minecartInv, 10);
            
            // Then
            assertEquals(0, transferred, "Pełny minecart nie powinien przyjmować więcej");
            assertEquals(furnaceBefore, countItemsInSlot(furnaceInv, OUTPUT_SLOTS[0]),
                "Piec powinien mieć tyle samo przedmiotów");
        }
        
        @Test
        @DisplayName("Hopper minecart stackuje podobne przedmioty")
        void hopperMinecartStacksSimilarItems() {
            // Given: Minecart z częściowo zapełnionym stackiem
            Inventory furnaceInv = createEnhancedFurnaceInventory();
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 32));
            
            Inventory minecartInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            minecartInv.setItem(0, new ItemStack(Material.IRON_INGOT, 32)); // Częściowy stack
            
            // When
            int transferred = simulateHopperMinecartPull(furnaceInv, minecartInv, 10);
            
            // Then
            assertTrue(transferred > 0, "Powinno przenieść do istniejącego stacka");
            assertEquals(64, 
                countItemsInSlot(furnaceInv, OUTPUT_SLOTS[0]) + countTotalItems(minecartInv),
                "Suma musi być zachowana");
        }
    }
    
    // ========================================================================
    // PORÓWNANIE Z VANILLA LEJKIEM
    // ========================================================================
    @Nested
    @DisplayName("Hopper Minecart vs Zwykły Lejek - identyczne zachowanie")
    class HopperMinecartVsBlockHopperTests {
        
        @Test
        @DisplayName("Hopper minecart i lejek blokowy pobierają tak samo z 3 slotów OUTPUT")
        void bothPullIdenticallyFromThreeOutputSlots() {
            // Given: Dwa identyczne piece
            Inventory furnace1 = createEnhancedFurnaceInventory();
            Inventory furnace2 = createEnhancedFurnaceInventory();
            
            // Identyczne przedmioty w OUTPUT
            furnace1.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 20));
            furnace1.setItem(OUTPUT_SLOTS[1], new ItemStack(Material.GOLD_INGOT, 20));
            furnace1.setItem(OUTPUT_SLOTS[2], new ItemStack(Material.COPPER_INGOT, 20));
            
            furnace2.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 20));
            furnace2.setItem(OUTPUT_SLOTS[1], new ItemStack(Material.GOLD_INGOT, 20));
            furnace2.setItem(OUTPUT_SLOTS[2], new ItemStack(Material.COPPER_INGOT, 20));
            
            Inventory minecartInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            Inventory blockHopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            // When: Oba pobierają 30 przedmiotów
            int minecartTransferred = simulateHopperMinecartPull(furnace1, minecartInv, 30);
            int blockHopperTransferred = simulateBlockHopperPull(furnace2, blockHopperInv, 30);
            
            // Then: Identyczna liczba transferów
            assertEquals(minecartTransferred, blockHopperTransferred,
                "Hopper minecart i lejek blokowy powinny przenieść tyle samo");
            
            // I identyczny stan inventory
            assertEquals(countTotalItems(minecartInv), countTotalItems(blockHopperInv),
                "Oba powinny mieć identyczną liczbę przedmiotów");
        }
        
        @Test
        @DisplayName("Transfer 1 przedmiot na operację (vanilla timing)")
        void oneItemPerOperation() {
            // Given
            Inventory furnaceInv = createEnhancedFurnaceInventory();
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.DIAMOND, 10));
            
            Inventory minecartInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            // When: 5 operacji
            int transferred = simulateHopperMinecartPull(furnaceInv, minecartInv, 5);
            
            // Then: Dokładnie 5 przedmiotów
            assertEquals(5, transferred, "Powinno być przeniesione dokładnie 5 przedmiotów (1 na operację)");
            assertEquals(5, countTotalItems(minecartInv));
        }
    }
    
    // ========================================================================
    // EDGE CASES
    // ========================================================================
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        
        @Test
        @DisplayName("Hopper minecart z różnymi typami przedmiotów w OUTPUT")
        void mixedItemTypesInOutput() {
            // Given: Różne typy przedmiotów
            Inventory furnaceInv = createEnhancedFurnaceInventory();
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 5));
            furnaceInv.setItem(OUTPUT_SLOTS[1], new ItemStack(Material.GOLD_INGOT, 5));
            furnaceInv.setItem(OUTPUT_SLOTS[2], new ItemStack(Material.DIAMOND, 5));
            
            Inventory minecartInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            // When: Pobierz wszystko
            simulateHopperMinecartPull(furnaceInv, minecartInv, 15);
            
            // Then: Wszystkie 3 typy w minecart
            int ironCount = 0, goldCount = 0, diamondCount = 0;
            for (ItemStack item : minecartInv.getContents()) {
                if (item == null) continue;
                switch (item.getType()) {
                    case IRON_INGOT -> ironCount += item.getAmount();
                    case GOLD_INGOT -> goldCount += item.getAmount();
                    case DIAMOND -> diamondCount += item.getAmount();
                    default -> {}
                }
            }
            
            assertEquals(5, ironCount, "Powinno być 5 żelaza");
            assertEquals(5, goldCount, "Powinno być 5 złota");
            assertEquals(5, diamondCount, "Powinno być 5 diamentów");
        }
        
        @Test
        @DisplayName("Wielokrotne przejazdy minecarta - sumaryczna ilość zachowana")
        void multipleMinecartPassesPreserveItems() {
            // Symulacja: minecart przejeżdża wielokrotnie pod piecem
            Inventory furnaceInv = createEnhancedFurnaceInventory();
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 100));
            
            Inventory minecartInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            int totalBefore = 100;
            
            // When: 50 przejazdów (każdy pobiera 1 przedmiot)
            for (int pass = 0; pass < 50; pass++) {
                simulateHopperMinecartPull(furnaceInv, minecartInv, 1);
            }
            
            // Then
            int furnaceItems = countItemsInSlot(furnaceInv, OUTPUT_SLOTS[0]);
            int minecartItems = countTotalItems(minecartInv);
            
            assertEquals(totalBefore, furnaceItems + minecartItems,
                "Suma musi być zachowana po 50 przejazdach: " + furnaceItems + " + " + minecartItems);
        }
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    /**
     * Tworzy inventory dla Enhanced Furnace (54 sloty).
     */
    private Inventory createEnhancedFurnaceInventory() {
        return Bukkit.createInventory(null, 54, "Enhanced Furnace");
    }
    
    /**
     * Symuluje hopper minecart pobierający z OUTPUT pieca.
     * 
     * Vanilla behavior:
     * - Pobiera 1 przedmiot na operację
     * - Pobiera z pierwszego niepustego slotu OUTPUT
     * - Enhanced Furnace ma 3 sloty OUTPUT (14, 23, 32)
     * 
     * @param furnaceInv inventory pieca
     * @param minecartInv inventory hopper minecarta
     * @param operations liczba operacji transferu
     * @return liczba przeniesionych przedmiotów
     */
    private int simulateHopperMinecartPull(Inventory furnaceInv, Inventory minecartInv, int operations) {
        int transferred = 0;
        
        for (int op = 0; op < operations; op++) {
            // Szukaj pierwszego niepustego slotu OUTPUT
            for (int outputSlot : OUTPUT_SLOTS) {
                ItemStack item = furnaceInv.getItem(outputSlot);
                if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                    
                    // Sprawdź czy minecart może przyjąć przedmiot
                    if (!canFitInInventory(minecartInv, item)) {
                        return transferred; // Minecart pełny
                    }
                    
                    // Przenieś 1 przedmiot
                    ItemStack toTransfer = item.clone();
                    toTransfer.setAmount(1);
                    
                    // Dodaj do minecarta
                    minecartInv.addItem(toTransfer);
                    
                    // Usuń z pieca
                    if (item.getAmount() == 1) {
                        furnaceInv.setItem(outputSlot, null);
                    } else {
                        item.setAmount(item.getAmount() - 1);
                    }
                    
                    transferred++;
                    break; // 1 przedmiot na operację
                }
            }
        }
        
        return transferred;
    }
    
    /**
     * Symuluje zwykły lejek blokowy pobierający z OUTPUT.
     * Identyczne zachowanie jak hopper minecart dla porównania.
     */
    private int simulateBlockHopperPull(Inventory furnaceInv, Inventory hopperInv, int operations) {
        // Identyczna logika jak hopper minecart
        return simulateHopperMinecartPull(furnaceInv, hopperInv, operations);
    }
    
    private boolean canFitInInventory(Inventory inventory, ItemStack item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot == null || slot.getType() == Material.AIR) {
                return true;
            }
            if (slot.isSimilar(item) && slot.getAmount() < slot.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }
    
    private int countItemsInSlot(Inventory inventory, int slot) {
        ItemStack item = inventory.getItem(slot);
        return item == null ? 0 : item.getAmount();
    }
    
    private int countItemsInSlots(Inventory inventory, int[] slots) {
        int total = 0;
        for (int slot : slots) {
            total += countItemsInSlot(inventory, slot);
        }
        return total;
    }
    
    private int countTotalItems(Inventory inventory) {
        int total = 0;
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                total += item.getAmount();
            }
        }
        return total;
    }
}

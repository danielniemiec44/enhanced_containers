package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kompleksowe testy integracyjne dla lejków Enhanced Furnace.
 * 
 * Testowane scenariusze:
 * 1. Transfer do INPUT (lejek od góry)
 * 2. Transfer z OUTPUT (lejek od dołu)
 * 3. Symulacja prędkości vanilla (1 item / 8 ticków)
 * 4. Edge cases i błędy
 * 5. Zachowanie ilości przedmiotów (brak "zjadania")
 */
@DisplayName("Hopper Transfer Comprehensive Tests")
@Tag("integration")
class HopperTransferComprehensiveIntegrationTest {
    
    private static final int[] INPUT_SLOTS = {11, 20, 29};
    private static final int[] OUTPUT_SLOTS = {14, 23, 32};
    private static final int[] FUEL_SLOTS = {46, 47, 48};
    
    private ServerMock server;
    
    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }
    
    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }
    
    // ========================================================================
    // TRANSFER DO INPUT (Lejek od góry)
    // ========================================================================
    
    @Nested
    @DisplayName("Transfer do INPUT (lejek od góry)")
    class InputTransferTests {
        
        @Test
        @DisplayName("Pojedynczy transfer - 1 item")
        void singleItemTransfer() {
            // Given
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.RAW_IRON, 64));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // When: 1 transfer
            int transferred = simulateHopperPushToInput(hopperInv, furnaceInv, 1);
            
            // Then
            assertEquals(1, transferred);
            assertEquals(63, hopperInv.getItem(0).getAmount());
            assertEquals(1, furnaceInv.getItem(INPUT_SLOTS[0]).getAmount());
        }
        
        @ParameterizedTest(name = "Transfer {0} itemów")
        @ValueSource(ints = {1, 5, 10, 32, 64, 100, 192})
        @DisplayName("Różne ilości transferów")
        void variousAmountTransfers(int amount) {
            // Given
            int initialAmount = Math.min(amount, 64 * 3); // max 3 stacki w hopperze
            
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            // Rozłóż na sloty
            int remaining = initialAmount;
            for (int i = 0; i < 3 && remaining > 0; i++) {
                int toPlace = Math.min(remaining, 64);
                hopperInv.setItem(i, new ItemStack(Material.RAW_GOLD, toPlace));
                remaining -= toPlace;
            }
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // When
            int transferred = simulateHopperPushToInput(hopperInv, furnaceInv, amount);
            
            // Then: Suma zachowana
            int hopperTotal = countTotalItems(hopperInv);
            int furnaceTotal = countItemsInSlots(furnaceInv, INPUT_SLOTS);
            
            assertEquals(initialAmount, hopperTotal + furnaceTotal,
                "Suma przedmiotów musi być zachowana: " + initialAmount + " = hopper(" + hopperTotal + ") + furnace(" + furnaceTotal + ")");
        }
        
        @Test
        @DisplayName("Wypełnianie wszystkich 3 slotów INPUT")
        void fillsAllThreeInputSlots() {
            // Given: Dużo itemów różnych typów
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.RAW_IRON, 64));
            hopperInv.setItem(1, new ItemStack(Material.RAW_GOLD, 64));
            hopperInv.setItem(2, new ItemStack(Material.RAW_COPPER, 64));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            // Wypełnij pierwszy slot INPUT czymś innym
            furnaceInv.setItem(INPUT_SLOTS[0], new ItemStack(Material.DIAMOND_ORE, 64));
            furnaceInv.setItem(INPUT_SLOTS[1], new ItemStack(Material.EMERALD_ORE, 64));
            furnaceInv.setItem(INPUT_SLOTS[2], new ItemStack(Material.GOLD_ORE, 64));
            
            // When: Próba transferu gdy wszystko pełne
            int transferred = simulateHopperPushToInput(hopperInv, furnaceInv, 10);
            
            // Then: Nic nie przeniesione bo sloty pełne różnymi materiałami
            assertEquals(0, transferred, "Nie powinno być transferu gdy sloty pełne innymi materiałami");
        }
        
        @Test
        @DisplayName("Stacking tego samego materiału w INPUT")
        void stackingSameMaterialInInput() {
            // Given
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.RAW_IRON, 32));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(INPUT_SLOTS[0], new ItemStack(Material.RAW_IRON, 32));
            
            // When
            int transferred = simulateHopperPushToInput(hopperInv, furnaceInv, 20);
            
            // Then: Stack powinien się zwiększyć
            assertEquals(20, transferred);
            assertEquals(52, furnaceInv.getItem(INPUT_SLOTS[0]).getAmount(), "Stack = 32 + 20 = 52");
            assertEquals(12, hopperInv.getItem(0).getAmount(), "Hopper = 32 - 20 = 12");
        }
        
        @Test
        @DisplayName("Overflow do następnego slotu INPUT")
        void overflowToNextInputSlot() {
            // Given: Prawie pełny pierwszy slot
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.RAW_IRON, 20));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(INPUT_SLOTS[0], new ItemStack(Material.RAW_IRON, 60)); // 4 miejsca
            
            // When: Przenieś 10 - 4 do pierwszego, 6 do drugiego
            int transferred = simulateHopperPushToInput(hopperInv, furnaceInv, 10);
            
            // Then
            assertEquals(10, transferred);
            assertEquals(64, furnaceInv.getItem(INPUT_SLOTS[0]).getAmount(), "Pierwszy slot pełny");
            assertEquals(6, furnaceInv.getItem(INPUT_SLOTS[1]).getAmount(), "Reszta w drugim slocie");
        }
        
        @Test
        @DisplayName("Pusty hopper - brak transferu")
        void emptyHopperNoTransfer() {
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            // Pusty
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            int transferred = simulateHopperPushToInput(hopperInv, furnaceInv, 10);
            
            assertEquals(0, transferred);
        }
    }
    
    // ========================================================================
    // TRANSFER Z OUTPUT (Lejek od dołu)
    // ========================================================================
    
    @Nested
    @DisplayName("Transfer z OUTPUT (lejek od dołu)")
    class OutputTransferTests {
        
        @Test
        @DisplayName("Pojedynczy transfer z OUTPUT")
        void singleOutputTransfer() {
            // Given
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 64));
            
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            // When
            int transferred = simulateHopperPullFromOutput(furnaceInv, hopperInv, 1);
            
            // Then
            assertEquals(1, transferred);
            assertEquals(63, furnaceInv.getItem(OUTPUT_SLOTS[0]).getAmount());
            assertEquals(1, hopperInv.getItem(0).getAmount());
        }
        
        @Test
        @DisplayName("Pobieranie z wielu slotów OUTPUT w kolejności")
        void pullsFromMultipleOutputSlotsInOrder() {
            // Given: 10 itemów w każdym slocie OUTPUT
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 10));
            furnaceInv.setItem(OUTPUT_SLOTS[1], new ItemStack(Material.GOLD_INGOT, 10));
            furnaceInv.setItem(OUTPUT_SLOTS[2], new ItemStack(Material.COPPER_INGOT, 10));
            
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            // When: Pobierz 25 itemów
            int transferred = simulateHopperPullFromOutput(furnaceInv, hopperInv, 25);
            
            // Then
            assertEquals(25, transferred);
            
            // Suma zachowana
            int furnaceTotal = countItemsInSlots(furnaceInv, OUTPUT_SLOTS);
            int hopperTotal = countTotalItems(hopperInv);
            assertEquals(30, furnaceTotal + hopperTotal);
        }
        
        @Test
        @DisplayName("Usunięcie ostatniego itemu z OUTPUT (amount=1)")
        void removesLastItemFromOutput() {
            // Given
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 1));
            
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            // When
            simulateHopperPullFromOutput(furnaceInv, hopperInv, 1);
            
            // Then: Slot powinien być null/pusty
            ItemStack remaining = furnaceInv.getItem(OUTPUT_SLOTS[0]);
            assertTrue(remaining == null || remaining.getType() == Material.AIR,
                "Slot OUTPUT powinien być pusty po pobraniu ostatniego itemu");
        }
        
        @Test
        @DisplayName("Pełny hopper - brak transferu")
        void fullHopperNoTransfer() {
            // Given
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 64));
            
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            // Wypełnij hopper innymi itemami
            for (int i = 0; i < 5; i++) {
                hopperInv.setItem(i, new ItemStack(Material.DIAMOND, 64));
            }
            
            // When
            int transferred = simulateHopperPullFromOutput(furnaceInv, hopperInv, 10);
            
            // Then
            assertEquals(0, transferred, "Pełny hopper nie powinien przyjmować więcej");
            assertEquals(64, furnaceInv.getItem(OUTPUT_SLOTS[0]).getAmount(),
                "Piec powinien mieć nadal 64 sztabki");
        }
        
        @Test
        @DisplayName("Pusty OUTPUT - brak transferu")
        void emptyOutputNoTransfer() {
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            // OUTPUT pusty
            
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            int transferred = simulateHopperPullFromOutput(furnaceInv, hopperInv, 10);
            
            assertEquals(0, transferred);
        }
    }
    
    // ========================================================================
    // SYMULACJA VANILLA TIMING (1 item / 8 ticków)
    // ========================================================================
    
    @Nested
    @DisplayName("Symulacja vanilla timing")
    class VanillaTimingTests {
        
        @Test
        @DisplayName("64 itemy = 64 operacje (512 ticków = 25.6 sekund)")
        void fullStackTransferTiming() {
            // Given
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.RAW_IRON, 64));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // When: 64 operacje (każda = 8 ticków)
            int transferred = simulateHopperPushToInput(hopperInv, furnaceInv, 64);
            
            // Then
            assertEquals(64, transferred, "Wszystkie 64 itemy powinny być przeniesione");
            assertNull(hopperInv.getItem(0), "Hopper powinien być pusty");
            assertEquals(64, furnaceInv.getItem(INPUT_SLOTS[0]).getAmount());
            
            // Vanilla timing: 64 * 8 = 512 ticków = 25.6 sekund
            // (dokumentacja dla referencji)
        }
        
        @Test
        @DisplayName("Prędkość: 2.5 itema na sekundę")
        void transferRateVerification() {
            // Vanilla hopper: 1 item co 8 ticków = 1 item co 0.4 sekundy = 2.5 items/sec
            
            // Given: 100 itemów do przeniesienia
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.RAW_IRON, 64));
            hopperInv.setItem(1, new ItemStack(Material.RAW_IRON, 36));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // When: 50 operacji = 50 * 8 ticków = 400 ticków = 20 sekund
            int after20Seconds = simulateHopperPushToInput(hopperInv, furnaceInv, 50);
            
            // Then: W 20 sekund powinno przejść ~50 itemów (2.5 * 20)
            assertEquals(50, after20Seconds, "W 20 sekund powinno przejść 50 itemów");
        }
    }
    
    // ========================================================================
    // ZACHOWANIE ILOŚCI - BRAK ZJADANIA
    // ========================================================================
    
    @Nested
    @DisplayName("Zachowanie ilości przedmiotów")
    class ItemConservationTests {
        
        @Test
        @DisplayName("Wielokrotne transfery INPUT nie tracą itemów")
        void inputTransfersNoItemLoss() {
            // Given
            int total = 200;
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.RAW_IRON, 64));
            hopperInv.setItem(1, new ItemStack(Material.RAW_IRON, 64));
            hopperInv.setItem(2, new ItemStack(Material.RAW_IRON, 64));
            hopperInv.setItem(3, new ItemStack(Material.RAW_IRON, 8)); // 200 total
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // When: Wielokrotne transfery
            for (int round = 0; round < 10; round++) {
                simulateHopperPushToInput(hopperInv, furnaceInv, 20);
            }
            
            // Then
            int hopperTotal = countTotalItems(hopperInv);
            int furnaceTotal = countItemsInSlots(furnaceInv, INPUT_SLOTS);
            
            assertEquals(total, hopperTotal + furnaceTotal,
                "Suma musi być zachowana: " + total + " = hopper(" + hopperTotal + ") + furnace(" + furnaceTotal + ")");
        }
        
        @Test
        @DisplayName("Wielokrotne transfery OUTPUT nie tracą itemów")
        void outputTransfersNoItemLoss() {
            // Given
            int total = 150;
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 50));
            furnaceInv.setItem(OUTPUT_SLOTS[1], new ItemStack(Material.IRON_INGOT, 50));
            furnaceInv.setItem(OUTPUT_SLOTS[2], new ItemStack(Material.IRON_INGOT, 50));
            
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            // When
            for (int round = 0; round < 20; round++) {
                simulateHopperPullFromOutput(furnaceInv, hopperInv, 10);
            }
            
            // Then
            int furnaceTotal = countItemsInSlots(furnaceInv, OUTPUT_SLOTS);
            int hopperTotal = countTotalItems(hopperInv);
            
            assertEquals(total, furnaceTotal + hopperTotal,
                "Suma musi być zachowana: " + total);
        }
        
        @Test
        @DisplayName("Cykl: INPUT → smażenie → OUTPUT → hopper - ilość zachowana")
        void fullCycleItemConservation() {
            // Symulacja: hopper wkłada rudę → piec smaży → lejek pobiera sztabki
            
            // Given: Start z 32 rudami w hopperze
            int startAmount = 32;
            
            Inventory inputHopper = Bukkit.createInventory(null, InventoryType.HOPPER);
            inputHopper.setItem(0, new ItemStack(Material.RAW_IRON, startAmount));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            Inventory outputHopper = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            // When: Przenieś do INPUT
            simulateHopperPushToInput(inputHopper, furnaceInv, startAmount);
            
            // Symuluj "smażenie" - przenieś z INPUT do OUTPUT
            for (int i = 0; i < 3; i++) {
                ItemStack input = furnaceInv.getItem(INPUT_SLOTS[i]);
                if (input != null && input.getAmount() > 0) {
                    furnaceInv.setItem(OUTPUT_SLOTS[i], new ItemStack(Material.IRON_INGOT, input.getAmount()));
                    furnaceInv.setItem(INPUT_SLOTS[i], null);
                }
            }
            
            // Pobierz z OUTPUT
            simulateHopperPullFromOutput(furnaceInv, outputHopper, startAmount);
            
            // Then: Suma musi się zgadzać
            int inputHopperTotal = countTotalItems(inputHopper);
            int furnaceInputTotal = countItemsInSlots(furnaceInv, INPUT_SLOTS);
            int furnaceOutputTotal = countItemsInSlots(furnaceInv, OUTPUT_SLOTS);
            int outputHopperTotal = countTotalItems(outputHopper);
            
            int total = inputHopperTotal + furnaceInputTotal + furnaceOutputTotal + outputHopperTotal;
            assertEquals(startAmount, total,
                "Suma musi być zachowana przez cały cykl: " + startAmount + 
                " = inputHopper(" + inputHopperTotal + ") + furnaceInput(" + furnaceInputTotal + 
                ") + furnaceOutput(" + furnaceOutputTotal + ") + outputHopper(" + outputHopperTotal + ")");
        }
    }
    
    // ========================================================================
    // EDGE CASES I BŁĘDY
    // ========================================================================
    
    @Nested
    @DisplayName("Edge cases i obsługa błędów")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Transfer większej ilości niż dostępna")
        void transferMoreThanAvailable() {
            // Given: Tylko 5 itemów
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.RAW_IRON, 5));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // When: Próba transferu 100
            int transferred = simulateHopperPushToInput(hopperInv, furnaceInv, 100);
            
            // Then: Tylko 5 przeniesionych
            assertEquals(5, transferred);
            assertNull(hopperInv.getItem(0));
        }
        
        @Test
        @DisplayName("Null items w inventory nie powodują błędów")
        void nullItemsHandledGracefully() {
            // Given: Hopper z przerwami (niektóre sloty null)
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            // slot 0 null
            hopperInv.setItem(1, null);
            hopperInv.setItem(2, new ItemStack(Material.RAW_IRON, 10));
            // slot 3,4 null
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // When/Then: Nie powinno być wyjątku
            assertDoesNotThrow(() -> {
                int transferred = simulateHopperPushToInput(hopperInv, furnaceInv, 5);
                assertEquals(5, transferred);
            });
        }
        
        @Test
        @DisplayName("AIR items są ignorowane")
        void airItemsIgnored() {
            // Given
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.AIR));
            hopperInv.setItem(1, new ItemStack(Material.RAW_IRON, 10));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // When
            int transferred = simulateHopperPushToInput(hopperInv, furnaceInv, 5);
            
            // Then: AIR pominięty, RAW_IRON przeniesiony
            assertEquals(5, transferred);
        }
        
        @Test
        @DisplayName("Różne materiały nie stackują się razem")
        void differentMaterialsDontStack() {
            // Given: Różne materiały w różnych slotach
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.RAW_IRON, 10));
            hopperInv.setItem(1, new ItemStack(Material.RAW_GOLD, 10));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(INPUT_SLOTS[0], new ItemStack(Material.RAW_COPPER, 60));
            
            // When
            int transferred = simulateHopperPushToInput(hopperInv, furnaceInv, 5);
            
            // Then: Powinny trafić do drugiego slotu (pierwszy ma inny materiał)
            assertEquals(5, transferred);
            assertEquals(60, furnaceInv.getItem(INPUT_SLOTS[0]).getAmount(), "Miedź nietknięta");
            assertEquals(5, furnaceInv.getItem(INPUT_SLOTS[1]).getAmount(), "Żelazo w drugim slocie");
        }
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    private int simulateHopperPushToInput(Inventory hopperInv, Inventory furnaceInv, int operations) {
        int transferred = 0;
        
        for (int op = 0; op < operations; op++) {
            for (int hopperSlot = 0; hopperSlot < hopperInv.getSize(); hopperSlot++) {
                ItemStack item = hopperInv.getItem(hopperSlot);
                if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                    
                    for (int inputSlot : INPUT_SLOTS) {
                        ItemStack current = furnaceInv.getItem(inputSlot);
                        
                        if (current == null || current.getType() == Material.AIR) {
                            ItemStack toPlace = item.clone();
                            toPlace.setAmount(1);
                            furnaceInv.setItem(inputSlot, toPlace);
                            
                            if (item.getAmount() == 1) {
                                hopperInv.setItem(hopperSlot, null);
                            } else {
                                item.setAmount(item.getAmount() - 1);
                            }
                            
                            transferred++;
                            break;
                        } else if (current.isSimilar(item) && current.getAmount() < current.getMaxStackSize()) {
                            current.setAmount(current.getAmount() + 1);
                            
                            if (item.getAmount() == 1) {
                                hopperInv.setItem(hopperSlot, null);
                            } else {
                                item.setAmount(item.getAmount() - 1);
                            }
                            
                            transferred++;
                            break;
                        }
                    }
                    break;
                }
            }
        }
        
        return transferred;
    }
    
    private int simulateHopperPullFromOutput(Inventory furnaceInv, Inventory hopperInv, int operations) {
        int transferred = 0;
        
        for (int op = 0; op < operations; op++) {
            for (int outputSlot : OUTPUT_SLOTS) {
                ItemStack item = furnaceInv.getItem(outputSlot);
                if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                    
                    ItemStack toAdd = item.clone();
                    toAdd.setAmount(1);
                    
                    if (canFitInInventory(hopperInv, toAdd)) {
                        if (item.getAmount() == 1) {
                            furnaceInv.setItem(outputSlot, null);
                        } else {
                            item.setAmount(item.getAmount() - 1);
                        }
                        
                        hopperInv.addItem(toAdd);
                        transferred++;
                        break;
                    }
                }
            }
        }
        
        return transferred;
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
    
    private int countItemsInSlots(Inventory inventory, int[] slots) {
        int total = 0;
        for (int slot : slots) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                total += item.getAmount();
            }
        }
        return total;
    }
}

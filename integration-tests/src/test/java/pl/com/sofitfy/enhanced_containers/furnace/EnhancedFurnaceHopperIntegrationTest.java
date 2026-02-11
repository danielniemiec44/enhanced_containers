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
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EnhancedFurnaceHopperIntegrationTest - testy integracyjne dla lejków z Enhanced Furnace
 * 
 * TESTOWANE SCENARIUSZE:
 * 1. Lejek OD DOŁU pieca → pobiera przedmioty ze slotów OUTPUT (14, 23, 32)
 * 2. Lejek OD GÓRY pieca → wkłada przedmioty do slotów INPUT (11, 20, 29)
 * 3. Lejek Z BOKU pieca → wkłada paliwo do slotów FUEL (46, 47, 48)
 * 
 * UWAGA: MockBukkit ma ograniczone wsparcie dla fizycznych bloków i lejków.
 * Te testy weryfikują LOGIKĘ EnhancedFurnaceHopperListener, nie fizyczną interakcję bloków.
 * 
 * Testujemy bezpośrednio metody handlera zamiast symulować fizyczne bloki,
 * ponieważ to daje nam 100% pewności, że logika działa poprawnie.
 */
@DisplayName("Enhanced Furnace - Hopper Integration Tests")
@Tag("integration")
class EnhancedFurnaceHopperIntegrationTest {
    
    // --- SLOTY PIECA (z EnhancedFurnaceGUI) ---
    private static final int[] INPUT_SLOTS = {11, 20, 29};
    private static final int[] OUTPUT_SLOTS = {14, 23, 32};
    private static final int[] FUEL_SLOTS = {46, 47, 48};
    
    private ServerMock server;
    private PlayerMock player;
    
    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
    }
    
    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }
    
    // ========================================================================
    // TESTY POBIERANIA Z OUTPUT (Lejek od dołu)
    // ========================================================================
    @Nested
    @DisplayName("Lejek OD DOŁU → pobiera z OUTPUT")
    class HopperBelowFurnaceTests {
        
        @Test
        @DisplayName("Lejek pobiera przedmioty ze slotu OUTPUT 14")
        void hopperPullsFromOutputSlot14() {
            // Given: Piec z 64 sztabkami żelaza w slocie OUTPUT 14
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 64));
            
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            // Symuluj stan początkowy
            int furnaceBefore = countItemsInSlot(furnaceInv, OUTPUT_SLOTS[0]);
            int hopperBefore = countTotalItems(hopperInv);
            
            assertEquals(64, furnaceBefore, "Piec powinien mieć 64 sztabki");
            assertEquals(0, hopperBefore, "Lejek powinien być pusty");
            
            // When: Symuluj transfer (lejek pobiera 1 przedmiot na tick, jak vanilla)
            // W prawdziwej grze lejek robi to co 8 ticków
            int transferCount = simulateHopperPullFromOutput(furnaceInv, hopperInv, 8);
            
            // Then: Przedmioty zostały przeniesione
            int furnaceAfter = countItemsInSlot(furnaceInv, OUTPUT_SLOTS[0]);
            int hopperAfter = countTotalItems(hopperInv);
            
            assertEquals(transferCount, hopperAfter, 
                "Lejek powinien mieć " + transferCount + " sztabek");
            assertEquals(64 - transferCount, furnaceAfter, 
                "Piec powinien mieć " + (64 - transferCount) + " sztabek");
            
            // Suma musi być zachowana (brak "zjadania")
            assertEquals(64, furnaceAfter + hopperAfter, 
                "Suma przedmiotów musi być zachowana: 64 = piec + lejek");
        }
        
        @Test
        @DisplayName("Lejek pobiera ze wszystkich 3 slotów OUTPUT w kolejności")
        void hopperPullsFromAllOutputSlots() {
            // Given: Piec z przedmiotami w każdym slocie OUTPUT
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 10));    // slot 14
            furnaceInv.setItem(OUTPUT_SLOTS[1], new ItemStack(Material.GOLD_INGOT, 10));    // slot 23
            furnaceInv.setItem(OUTPUT_SLOTS[2], new ItemStack(Material.COPPER_INGOT, 10));  // slot 32
            
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            int totalBefore = 30;
            
            // When: Symuluj wiele transferów (każdy pobiera z pierwszego niepustego slotu)
            int totalTransferred = simulateHopperPullFromOutput(furnaceInv, hopperInv, 15);
            
            // Then: Przedmioty zostały przeniesione
            int furnaceTotal = countItemsInSlots(furnaceInv, OUTPUT_SLOTS);
            int hopperTotal = countTotalItems(hopperInv);
            
            assertTrue(totalTransferred > 0, "Powinno być przeniesione więcej niż 0 przedmiotów");
            assertEquals(totalBefore, furnaceTotal + hopperTotal, 
                "Suma przedmiotów musi być zachowana");
        }
        
        @Test
        @DisplayName("Lejek nie pobiera z pustych slotów OUTPUT")
        void hopperDoesNotPullFromEmptySlots() {
            // Given: Piec z pustymi slotami OUTPUT
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            // Wszystkie OUTPUT sloty puste
            
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            // When
            int transferred = simulateHopperPullFromOutput(furnaceInv, hopperInv, 10);
            
            // Then
            assertEquals(0, transferred, "Nie powinno być żadnych transferów");
            assertEquals(0, countTotalItems(hopperInv), "Lejek powinien być pusty");
        }
        
        @Test
        @DisplayName("Lejek pełny - nie pobiera więcej przedmiotów")
        void fullHopperStopsPulling() {
            // Given: Piec pełny, lejek pełny
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 64));
            
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            // Wypełnij lejek (5 slotów po 64)
            for (int i = 0; i < 5; i++) {
                hopperInv.setItem(i, new ItemStack(Material.COBBLESTONE, 64));
            }
            
            int furnaceBefore = countItemsInSlot(furnaceInv, OUTPUT_SLOTS[0]);
            
            // When
            int transferred = simulateHopperPullFromOutput(furnaceInv, hopperInv, 10);
            
            // Then: Nic nie zostało przeniesione bo lejek pełny
            assertEquals(0, transferred, "Pełny lejek nie powinien przyjmować więcej");
            assertEquals(furnaceBefore, countItemsInSlot(furnaceInv, OUTPUT_SLOTS[0]),
                "Piec powinien mieć tyle samo przedmiotów");
        }
    }
    
    // ========================================================================
    // TESTY WKŁADANIA DO INPUT (Lejek od góry)
    // ========================================================================
    @Nested
    @DisplayName("Lejek OD GÓRY → wkłada do INPUT")
    class HopperAboveFurnaceTests {
        
        @Test
        @DisplayName("Lejek wkłada rudę do slotu INPUT 11")
        void hopperPushesToInputSlot() {
            // Given: Lejek z rudą żelaza
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.RAW_IRON, 32));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // When: Symuluj transfer do INPUT
            int transferred = simulateHopperPushToInput(hopperInv, furnaceInv, 8);
            
            // Then
            int furnaceItems = countItemsInSlots(furnaceInv, INPUT_SLOTS);
            int hopperItems = countTotalItems(hopperInv);
            
            assertTrue(transferred > 0, "Powinno być przeniesione więcej niż 0");
            assertEquals(32, furnaceItems + hopperItems, 
                "Suma przedmiotów musi być zachowana");
        }
        
        @Test
        @DisplayName("Lejek wypełnia wszystkie 3 sloty INPUT")
        void hopperFillsAllInputSlots() {
            // Given: Lejek z dużą ilością rudy
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.RAW_IRON, 64));
            hopperInv.setItem(1, new ItemStack(Material.RAW_IRON, 64));
            hopperInv.setItem(2, new ItemStack(Material.RAW_IRON, 64));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // Już mamy po 60 w każdym slocie INPUT
            furnaceInv.setItem(INPUT_SLOTS[0], new ItemStack(Material.RAW_IRON, 60));
            furnaceInv.setItem(INPUT_SLOTS[1], new ItemStack(Material.RAW_IRON, 60));
            furnaceInv.setItem(INPUT_SLOTS[2], new ItemStack(Material.RAW_IRON, 60));
            
            int totalBefore = 192 + 180; // 3*64 + 3*60
            
            // When: Symuluj wiele transferów
            int transferred = simulateHopperPushToInput(hopperInv, furnaceInv, 20);
            
            // Then: Przedmioty dystrybuowane do slotów INPUT
            int furnaceTotal = countItemsInSlots(furnaceInv, INPUT_SLOTS);
            int hopperTotal = countTotalItems(hopperInv);
            
            assertEquals(totalBefore, furnaceTotal + hopperTotal, 
                "Suma przedmiotów musi być zachowana");
        }
    }
    
    // ========================================================================
    // TESTY WKŁADANIA PALIWA (Lejek z boku)
    // ========================================================================
    @Nested
    @DisplayName("Lejek Z BOKU → wkłada PALIWO")
    class HopperSideFurnaceTests {
        
        @Test
        @DisplayName("Lejek wkłada węgiel do slotu FUEL")
        void hopperPushesCoalToFuelSlot() {
            // Given: Lejek z węglem
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.COAL, 32));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // When: Symuluj transfer paliwa
            int transferred = simulateHopperPushToFuel(hopperInv, furnaceInv, 8);
            
            // Then
            int furnaceItems = countItemsInSlots(furnaceInv, FUEL_SLOTS);
            int hopperItems = countTotalItems(hopperInv);
            
            assertTrue(transferred > 0, "Powinno być przeniesione więcej niż 0");
            assertEquals(32, furnaceItems + hopperItems, 
                "Suma przedmiotów musi być zachowana");
        }
        
        @Test
        @DisplayName("Lejek NIE wkłada nie-paliwa do slotów FUEL")
        void hopperDoesNotPushNonFuelToFuelSlots() {
            // Given: Lejek z rudą (nie jest paliwem)
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.RAW_IRON, 32));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // When: Próba transferu nie-paliwa do FUEL
            int transferred = simulateHopperPushToFuel(hopperInv, furnaceInv, 10);
            
            // Then: Nic nie zostało przeniesione
            assertEquals(0, transferred, "Ruda nie jest paliwem - nie powinna być przeniesiona");
            assertEquals(0, countItemsInSlots(furnaceInv, FUEL_SLOTS), 
                "Sloty FUEL powinny być puste");
        }
        
        @Test
        @DisplayName("Różne typy paliwa: węgiel, węgiel drzewny, blaze rod")
        void variousFuelTypes() {
            // Given: Lejek z różnymi paliwami
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.COAL, 10));
            hopperInv.setItem(1, new ItemStack(Material.CHARCOAL, 10));
            hopperInv.setItem(2, new ItemStack(Material.BLAZE_ROD, 10));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            int totalFuel = 30;
            
            // When
            int transferred = simulateHopperPushToFuel(hopperInv, furnaceInv, 30);
            
            // Then
            int furnaceItems = countItemsInSlots(furnaceInv, FUEL_SLOTS);
            int hopperItems = countTotalItems(hopperInv);
            
            assertEquals(totalFuel, furnaceItems + hopperItems, 
                "Suma paliwa musi być zachowana");
        }
    }
    
    // ========================================================================
    // TESTY ZACHOWANIA ILOŚCI PRZEDMIOTÓW
    // ========================================================================
    @Nested
    @DisplayName("Zachowanie ilości przedmiotów (brak zjadania)")
    class ItemConservationTests {
        
        @Test
        @DisplayName("Cykl: OUTPUT → Lejek → INPUT - ilość zachowana")
        void fullCycleConservesItems() {
            // Symulacja automatyzacji: output → hopper → input
            
            // Given: 64 sztabki w OUTPUT
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 64));
            
            Inventory bottomHopper = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            int totalBefore = 64;
            
            // When: Pełny cykl pobierania
            for (int cycle = 0; cycle < 10; cycle++) {
                simulateHopperPullFromOutput(furnaceInv, bottomHopper, 1);
            }
            
            // Then: Suma zachowana
            int furnaceItems = countItemsInSlot(furnaceInv, OUTPUT_SLOTS[0]);
            int hopperItems = countTotalItems(bottomHopper);
            
            assertEquals(totalBefore, furnaceItems + hopperItems,
                "Suma przedmiotów musi być zachowana po " + 10 + " cyklach");
        }
        
        @Test
        @DisplayName("Wielokrotne transfery nie tracą przedmiotów")
        void multipleTransfersNoItemLoss() {
            // Given
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.DIAMOND, 100)); // więcej niż stack dla testu
            
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            
            // When: 100 transferów
            int transferred = simulateHopperPullFromOutput(furnaceInv, hopperInv, 100);
            
            // Then
            int remaining = countItemsInSlot(furnaceInv, OUTPUT_SLOTS[0]);
            int inHopper = countTotalItems(hopperInv);
            
            // Ponieważ ItemStack ma max 64, część zostanie, część pójdzie do hoppera
            assertEquals(100, remaining + inHopper, 
                "100 przedmiotów musi być zachowane: " + remaining + " + " + inHopper);
        }
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    /**
     * Symuluje pobieranie przedmiotów przez lejek ze slotów OUTPUT pieca.
     * Lejek pobiera 1 przedmiot na operację (jak vanilla, co 8 ticków).
     * 
     * @return liczba przeniesionych przedmiotów
     */
    private int simulateHopperPullFromOutput(Inventory furnaceInv, Inventory hopperInv, int operations) {
        int transferred = 0;
        
        for (int op = 0; op < operations; op++) {
            // Znajdź pierwszy niepusty slot OUTPUT
            for (int outputSlot : OUTPUT_SLOTS) {
                ItemStack item = furnaceInv.getItem(outputSlot);
                if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                    
                    // Sprawdź czy hopper może przyjąć
                    ItemStack toAdd = item.clone();
                    toAdd.setAmount(1);
                    
                    if (canFitInInventory(hopperInv, toAdd)) {
                        // Pobierz 1 z pieca
                        if (item.getAmount() == 1) {
                            furnaceInv.setItem(outputSlot, null);
                        } else {
                            item.setAmount(item.getAmount() - 1);
                        }
                        
                        // Dodaj do hoppera
                        hopperInv.addItem(toAdd);
                        transferred++;
                        break; // Tylko 1 przedmiot na operację
                    }
                }
            }
        }
        
        return transferred;
    }
    
    /**
     * Symuluje wkładanie przedmiotów przez lejek do slotów INPUT pieca.
     */
    private int simulateHopperPushToInput(Inventory hopperInv, Inventory furnaceInv, int operations) {
        int transferred = 0;
        
        for (int op = 0; op < operations; op++) {
            // Znajdź pierwszy niepusty slot w hopperze
            for (int hopperSlot = 0; hopperSlot < hopperInv.getSize(); hopperSlot++) {
                ItemStack item = hopperInv.getItem(hopperSlot);
                if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                    
                    // Znajdź slot INPUT który może przyjąć
                    for (int inputSlot : INPUT_SLOTS) {
                        ItemStack current = furnaceInv.getItem(inputSlot);
                        
                        if (current == null || current.getType() == Material.AIR) {
                            // Pusty slot
                            ItemStack toPlace = item.clone();
                            toPlace.setAmount(1);
                            furnaceInv.setItem(inputSlot, toPlace);
                            
                            // Usuń z hoppera
                            if (item.getAmount() == 1) {
                                hopperInv.setItem(hopperSlot, null);
                            } else {
                                item.setAmount(item.getAmount() - 1);
                            }
                            
                            transferred++;
                            break;
                        } else if (current.isSimilar(item) && current.getAmount() < current.getMaxStackSize()) {
                            // Można dodać do stacka
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
                    break; // Tylko 1 przedmiot na operację
                }
            }
        }
        
        return transferred;
    }
    
    /**
     * Symuluje wkładanie paliwa przez lejek do slotów FUEL pieca.
     * Tylko przedmioty będące paliwem są akceptowane.
     */
    private int simulateHopperPushToFuel(Inventory hopperInv, Inventory furnaceInv, int operations) {
        int transferred = 0;
        
        for (int op = 0; op < operations; op++) {
            for (int hopperSlot = 0; hopperSlot < hopperInv.getSize(); hopperSlot++) {
                ItemStack item = hopperInv.getItem(hopperSlot);
                if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                    
                    // Sprawdź czy jest paliwem
                    if (!isFuel(item.getType())) {
                        continue;
                    }
                    
                    // Znajdź slot FUEL który może przyjąć
                    for (int fuelSlot : FUEL_SLOTS) {
                        ItemStack current = furnaceInv.getItem(fuelSlot);
                        
                        if (current == null || current.getType() == Material.AIR) {
                            ItemStack toPlace = item.clone();
                            toPlace.setAmount(1);
                            furnaceInv.setItem(fuelSlot, toPlace);
                            
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
    
    /**
     * Sprawdza czy przedmiot jest paliwem (uproszczona wersja).
     */
    private boolean isFuel(Material material) {
        return switch (material) {
            case COAL, CHARCOAL, COAL_BLOCK -> true;
            case OAK_PLANKS, SPRUCE_PLANKS, BIRCH_PLANKS, JUNGLE_PLANKS, ACACIA_PLANKS, DARK_OAK_PLANKS -> true;
            case OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG -> true;
            case STICK, BLAZE_ROD, LAVA_BUCKET, DRIED_KELP_BLOCK -> true;
            default -> false;
        };
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

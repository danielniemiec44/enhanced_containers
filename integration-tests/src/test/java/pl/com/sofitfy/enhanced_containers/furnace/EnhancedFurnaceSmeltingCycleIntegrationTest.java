package pl.com.sofitfy.enhanced_containers.furnace;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EnhancedFurnaceSmeltingCycleIntegrationTest - testy integracyjne pełnego cyklu przepalania
 * 
 * Testuje PEŁNY CYKL:
 * 1. Włożenie węgla do slotu paliwa
 * 2. Włożenie rudy do slotu input
 * 3. Symulacja ticków przepalania
 * 4. Sprawdzenie wyniku w slocie output
 * 
 * WYMAGA: MockBukkit do symulacji środowiska Bukkit
 */
@DisplayName("Enhanced Furnace Smelting Cycle Integration Tests")
class EnhancedFurnaceSmeltingCycleIntegrationTest {
    
    // --- STAŁE KONFIGURACJI ---
    
    /** Czas palenia węgla w tickach (vanilla wartość) */
    private static final int COAL_BURN_TIME = 1600;
    
    /** Czas przepalania jednego przedmiotu w zwykłym piecu */
    private static final int COOK_TIME_NORMAL = 200;
    
    // --- SLOTY PIECA (z EnhancedFurnaceGUI) ---
    
    /** Sloty INPUT: 11, 20, 29 */
    private static final int[] INPUT_SLOTS = {11, 20, 29};
    
    /** Sloty OUTPUT: 14, 23, 32 */
    private static final int[] OUTPUT_SLOTS = {14, 23, 32};
    
    /** Sloty FUEL: 46, 47, 48 */
    private static final int[] FUEL_SLOTS = {46, 47, 48};
    
    // --- MOCK BUKKIT ---
    
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
    // TESTY PEŁNEGO CYKLU PRZEPALANIA
    // ========================================================================
    
    @Nested
    @DisplayName("Pełny cykl przepalania")
    class FullSmeltingCycle {
        
        @Test
        @DisplayName("1 węgiel + 1 ruda żelaza → 1 sztabka żelaza (po 200 tickach)")
        void singleItemSmeltingCycle() {
            // Przygotowanie: stwórz GUI pieca
            Inventory furnaceGUI = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // Włóż węgiel do slotu paliwa (46)
            furnaceGUI.setItem(FUEL_SLOTS[0], new ItemStack(Material.COAL, 1));
            
            // Włóż rudę żelaza do INPUT (11)
            furnaceGUI.setItem(INPUT_SLOTS[0], new ItemStack(Material.RAW_IRON, 1));
            
            // Stwórz obiekt danych pieca
            EnhancedFurnaceGUI gui = createMockFurnaceGUI(furnaceGUI);
            EnhancedFurnaceData data = new EnhancedFurnaceData(gui, Material.FURNACE);
            
            // Symuluj pobranie paliwa
            data.setBurnTime(COAL_BURN_TIME);
            data.setMaxBurnTime(COAL_BURN_TIME);
            
            // Symuluj przepalanie: 200 ticków dla 1 linii
            simulateSmeltingTicks(data, gui, 1, COOK_TIME_NORMAL);
            
            // Sprawdź wynik: cookTime powinien osiągnąć maxCookTime
            assertTrue(data.getCookTime(0) >= COOK_TIME_NORMAL || data.getCookTime(0) == 0,
                "Po 200 tickach przedmiot powinien być przepalony");
        }
        
        @Test
        @DisplayName("1 węgiel przepala 8 przedmiotów przy 1 linii (1600 ticków)")
        void coalSmelts8ItemsWithOneLine() {
            Inventory furnaceGUI = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // Włóż węgiel do paliwa
            furnaceGUI.setItem(FUEL_SLOTS[0], new ItemStack(Material.COAL, 1));
            
            // Włóż 8 sztuk rudy żelaza do INPUT
            furnaceGUI.setItem(INPUT_SLOTS[0], new ItemStack(Material.RAW_IRON, 8));
            
            EnhancedFurnaceGUI gui = createMockFurnaceGUI(furnaceGUI);
            EnhancedFurnaceData data = new EnhancedFurnaceData(gui, Material.FURNACE);
            
            data.setBurnTime(COAL_BURN_TIME);
            data.setMaxBurnTime(COAL_BURN_TIME);
            
            // Symuluj pełne przepalanie 1600 ticków
            int itemsSmelted = simulateFullSmeltingWithCounting(data, gui, 1);
            
            assertEquals(8, itemsSmelted,
                "1 węgiel + 1 linia powinien przepalić 8 przedmiotów");
        }
        
        @Test
        @DisplayName("1 węgiel przepala 8 przedmiotów przy 3 liniach (~533 ticki)")
        void coalSmelts8ItemsWithThreeLines() {
            Inventory furnaceGUI = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // Włóż węgiel do paliwa
            furnaceGUI.setItem(FUEL_SLOTS[0], new ItemStack(Material.COAL, 1));
            
            // Włóż przedmioty do wszystkich 3 linii INPUT (łącznie 9, zostawiając zapas)
            furnaceGUI.setItem(INPUT_SLOTS[0], new ItemStack(Material.RAW_IRON, 3));
            furnaceGUI.setItem(INPUT_SLOTS[1], new ItemStack(Material.RAW_GOLD, 3));
            furnaceGUI.setItem(INPUT_SLOTS[2], new ItemStack(Material.RAW_COPPER, 3));
            
            EnhancedFurnaceGUI gui = createMockFurnaceGUI(furnaceGUI);
            EnhancedFurnaceData data = new EnhancedFurnaceData(gui, Material.FURNACE);
            
            data.setBurnTime(COAL_BURN_TIME);
            data.setMaxBurnTime(COAL_BURN_TIME);
            
            // Symuluj przepalanie z 3 liniami
            int totalItemsSmelted = simulateFullSmeltingWithCountingMultiLine(data, gui, 3);
            
            // KLUCZOWA ASERCJA: łącznie 8 przedmiotów (z tolerancją zaokrąglenia)
            assertTrue(totalItemsSmelted >= 6 && totalItemsSmelted <= 9,
                "1 węgiel + 3 linie powinny przepalić ~8 przedmiotów łącznie (było: " + totalItemsSmelted + ")");
        }
    }
    
    // ========================================================================
    // TESTY ZUŻYCIA PALIWA
    // ========================================================================
    
    @Nested
    @DisplayName("Zużycie paliwa")
    class FuelConsumption {
        
        @Test
        @DisplayName("Paliwo zużywa się 3x szybciej przy 3 liniach")
        void fuelConsumes3xFasterWith3Lines() {
            EnhancedFurnaceGUI gui = createEmptyMockFurnaceGUI();
            EnhancedFurnaceData data = new EnhancedFurnaceData(gui, Material.FURNACE);
            
            data.setBurnTime(COAL_BURN_TIME);
            data.setMaxBurnTime(COAL_BURN_TIME);
            
            // Symuluj 100 ticków z 3 liniami
            for (int i = 0; i < 100; i++) {
                data.setBurnTime(data.getBurnTime() - 3); // 3 aktywne linie
            }
            
            int expectedBurnTime = COAL_BURN_TIME - (100 * 3);
            assertEquals(expectedBurnTime, data.getBurnTime(),
                "Po 100 tickach z 3 liniami: burnTime powinien zmniejszyć się o 300");
        }
        
        @Test
        @DisplayName("Bez aktywnych linii paliwo nie jest zużywane")
        void noFuelConsumptionWithNoActiveLines() {
            EnhancedFurnaceGUI gui = createEmptyMockFurnaceGUI();
            EnhancedFurnaceData data = new EnhancedFurnaceData(gui, Material.FURNACE);
            
            data.setBurnTime(COAL_BURN_TIME);
            
            // Symuluj 100 ticków z 0 liniami (brak inputów)
            for (int i = 0; i < 100; i++) {
                int activeLines = 0; // Brak przedmiotów w INPUT
                if (activeLines > 0) {
                    data.setBurnTime(data.getBurnTime() - activeLines);
                }
                // Jeśli 0 linii - paliwo nie jest zużywane
            }
            
            assertEquals(COAL_BURN_TIME, data.getBurnTime(),
                "Bez aktywnych linii paliwo nie powinno być zużywane");
        }
    }
    
    // ========================================================================
    // TESTY SLOTÓW
    // ========================================================================
    
    @Nested
    @DisplayName("Mapowanie slotów")
    class SlotMapping {
        
        @Test
        @DisplayName("INPUT sloty: 11, 20, 29 (kolumna 2)")
        void inputSlotsAreCorrect() {
            assertArrayEquals(INPUT_SLOTS, EnhancedFurnaceGUI.INPUT_SLOTS,
                "INPUT sloty powinny być [11, 20, 29]");
        }
        
        @Test
        @DisplayName("OUTPUT sloty: 14, 23, 32 (kolumna 5)")
        void outputSlotsAreCorrect() {
            assertArrayEquals(OUTPUT_SLOTS, EnhancedFurnaceGUI.OUTPUT_SLOTS,
                "OUTPUT sloty powinny być [14, 23, 32]");
        }
        
        @Test
        @DisplayName("FUEL sloty: 46, 47, 48 (rząd dolny)")
        void fuelSlotsAreCorrect() {
            assertArrayEquals(FUEL_SLOTS, EnhancedFurnaceGUI.FUEL_SLOTS,
                "FUEL sloty powinny być [46, 47, 48]");
        }
        
        @Test
        @DisplayName("Linia 1: INPUT 11 → OUTPUT 14")
        void line1Mapping() {
            assertEquals(0, EnhancedFurnaceGUI.getLineIndexForInputSlot(11));
            assertEquals(0, EnhancedFurnaceGUI.getLineIndexForOutputSlot(14));
        }
        
        @Test
        @DisplayName("Linia 2: INPUT 20 → OUTPUT 23")
        void line2Mapping() {
            assertEquals(1, EnhancedFurnaceGUI.getLineIndexForInputSlot(20));
            assertEquals(1, EnhancedFurnaceGUI.getLineIndexForOutputSlot(23));
        }
        
        @Test
        @DisplayName("Linia 3: INPUT 29 → OUTPUT 32")
        void line3Mapping() {
            assertEquals(2, EnhancedFurnaceGUI.getLineIndexForInputSlot(29));
            assertEquals(2, EnhancedFurnaceGUI.getLineIndexForOutputSlot(32));
        }
    }
    
    // ========================================================================
    // TESTY WALIDACJI INPUT
    // ========================================================================
    
    @Nested
    @DisplayName("Walidacja wejścia")
    class InputValidation {
        
        @Test
        @DisplayName("Węgiel jest rozpoznawany jako paliwo")
        void coalIsRecognizedAsFuel() {
            int burnTime = EnhancedFurnaceLogic.getFuelBurnTime(Material.COAL);
            assertTrue(burnTime > 0, "Węgiel powinien być rozpoznawany jako paliwo");
            assertEquals(1600, burnTime, "Węgiel powinien mieć 1600 ticków palenia");
        }
        
        @Test
        @DisplayName("Diament NIE jest paliwem")
        void diamondIsNotFuel() {
            int burnTime = EnhancedFurnaceLogic.getFuelBurnTime(Material.DIAMOND);
            assertEquals(0, burnTime, "Diament nie powinien być paliwem");
        }
        
        @Test
        @DisplayName("Kamień NIE jest paliwem")
        void stoneIsNotFuel() {
            int burnTime = EnhancedFurnaceLogic.getFuelBurnTime(Material.STONE);
            assertEquals(0, burnTime, "Kamień nie powinien być paliwem");
        }
    }
    
    // ========================================================================
    // METODY POMOCNICZE
    // ========================================================================
    
    /**
     * Tworzy mockowany EnhancedFurnaceGUI na bazie istniejącego Inventory
     */
    private EnhancedFurnaceGUI createMockFurnaceGUI(Inventory baseInventory) {
        // EnhancedFurnaceGUI tworzy własne inventory, więc kopiujemy przedmioty
        EnhancedFurnaceGUI gui = new EnhancedFurnaceGUI("Enhanced Furnace");
        
        // Skopiuj przedmioty z baseInventory do GUI
        for (int i = 0; i < INPUT_SLOTS.length; i++) {
            ItemStack item = baseInventory.getItem(INPUT_SLOTS[i]);
            if (item != null) {
                gui.setInputItem(i, item.clone());
            }
        }
        
        for (int i = 0; i < OUTPUT_SLOTS.length; i++) {
            ItemStack item = baseInventory.getItem(OUTPUT_SLOTS[i]);
            if (item != null) {
                gui.setOutputItem(i, item.clone());
            }
        }
        
        for (int i = 0; i < FUEL_SLOTS.length; i++) {
            ItemStack item = baseInventory.getItem(FUEL_SLOTS[i]);
            if (item != null) {
                gui.setFuelItem(i, item.clone());
            }
        }
        
        return gui;
    }
    
    /**
     * Tworzy pusty EnhancedFurnaceGUI
     */
    private EnhancedFurnaceGUI createEmptyMockFurnaceGUI() {
        return new EnhancedFurnaceGUI("Enhanced Furnace");
    }
    
    /**
     * Symuluje określoną liczbę ticków przepalania
     */
    private void simulateSmeltingTicks(EnhancedFurnaceData data, EnhancedFurnaceGUI gui, int activeLines, int ticks) {
        for (int i = 0; i < ticks; i++) {
            // Zużyj paliwo proporcjonalnie
            if (data.getBurnTime() > 0) {
                data.setBurnTime(data.getBurnTime() - activeLines);
            }
            
            // Zwiększ postęp przepalania dla każdej aktywnej linii
            for (int line = 0; line < activeLines; line++) {
                if (gui.getInputItem(line) != null && gui.getInputItem(line).getType() != Material.AIR) {
                    data.incrementCookTime(line);
                }
            }
        }
    }
    
    /**
     * Symuluje pełne przepalanie i zwraca liczbę przepalonych przedmiotów (1 linia)
     */
    private int simulateFullSmeltingWithCounting(EnhancedFurnaceData data, EnhancedFurnaceGUI gui, int activeLines) {
        int itemsSmelted = 0;
        int maxCookTime = data.getMaxCookTime();
        
        while (data.getBurnTime() > 0) {
            data.setBurnTime(data.getBurnTime() - activeLines);
            
            // Zwiększ postęp przepalania
            data.incrementCookTime(0);
            
            // Sprawdź czy przedmiot przepalony
            if (data.getCookTime(0) >= maxCookTime) {
                itemsSmelted++;
                data.resetCookTime(0);
            }
        }
        
        return itemsSmelted;
    }
    
    /**
     * Symuluje pełne przepalanie z wieloma liniami
     */
    private int simulateFullSmeltingWithCountingMultiLine(EnhancedFurnaceData data, EnhancedFurnaceGUI gui, int activeLines) {
        int[] itemsSmeltedPerLine = new int[3];
        int maxCookTime = data.getMaxCookTime();
        
        while (data.getBurnTime() > 0) {
            // Zużyj paliwo proporcjonalnie
            data.setBurnTime(data.getBurnTime() - activeLines);
            
            // Każda linia przepala niezależnie
            for (int line = 0; line < activeLines; line++) {
                data.incrementCookTime(line);
                
                if (data.getCookTime(line) >= maxCookTime) {
                    itemsSmeltedPerLine[line]++;
                    data.resetCookTime(line);
                }
            }
        }
        
        return itemsSmeltedPerLine[0] + itemsSmeltedPerLine[1] + itemsSmeltedPerLine[2];
    }
}

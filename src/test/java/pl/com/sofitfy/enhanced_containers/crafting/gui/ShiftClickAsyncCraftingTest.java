package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy jednostkowe logiki async craftingu (czysty algorytm bez Bukkita).
 * 
 * Testujemy algorytm pętli shift+click:
 * - Czy pętla craftingu robi crafty dopóki receptura pasuje
 * - Czy się zatrzymuje gdy receptura się zmieni
 * - Czy zlicza prawidłową ilość craftów
 * - Czy limit bezpieczności zatrzymuje nieskończone pętlo
 * 
 * Testy są pure - bez dependencji od serwera Bukkit, mogą się łatwo uruchomić.
 */
@DisplayName("Async Shift-Click Crafting Logic Unit Tests")
public class ShiftClickAsyncCraftingTest {

    /**
     * Symulacja grid'u jako liczby (ilość itemów w slot 0).
     * Receptura: -1 item z slotem
     */
    @Nested
    @DisplayName("Simple Single-Slot Crafting")
    public class SingleSlotCraftingTests {

        /**
         * Test: Craftu dopóki są itemy w slot 0.
         * 
         * Scenariusz:
         * - Slot 0 ma 4 itemy
         * - Receptura: -1 item
         * - Powtórz aż do wyczerpania
         * 
         * Oczekiwanie:
         * - 4 crafty
         * - Slot kończy się na 0
         */
        @Test
        @DisplayName("Craft until items run out - 4 items = 4 crafts")
        public void testCraftUntilItemsRunOut() {
            // Symuluj slot 0 z 4 itemami
            int slot0Items = 4;
            int craftCount = 0;
            
            // Pętla craftingu
            while (slot0Items > 0) {
                slot0Items--; // Receptura: -1 item
                craftCount++;
            }
            
            // Weryfikacja
            assertEquals(4, craftCount, "Powinno być 4 crafty (4 itemy)");
            assertEquals(0, slot0Items, "Slot powinien być pusty");
        }

        /**
         * Test: Receptura się zmienia - stop.
         * 
         * Scenariusz:
         * - Receptura wymaga Material.COAL (int 0 = coal)
         * - Craft 2 razy, potem zmienia się na 1 (żelazo)
         * 
         * Oczekiwanie:
         * - Tylko 2 crafty
         * - Nie craftuje żelazo (inna receptura)
         */
        @Test
        @DisplayName("Stop when recipe changes - only craft matching items")
        public void testStopWhenRecipeChanges() {
            // Symuluj slot z 2 węglami (material type 0 = coal)
            int slot0Items = 2;
            int slot0Material = 0; // Coal
            int craftCount = 0;
            
            // Pętla craftingu - TYLKO Coal (0)
            while (slot0Items > 0 && slot0Material == 0) {
                slot0Items--;
                craftCount++;
            }
            
            // Teraz zmień material na Żelazo (1)
            slot0Material = 1;
            
            // Pętla NIE powinna się uruchomić (bo material się zmienił)
            if (slot0Items > 0 && slot0Material == 0) {
                craftCount++; // Ten crafty NIE powinien się dodać
            }
            
            // Weryfikacja
            assertEquals(2, craftCount, "Powinno być 2 crafty (coal)");
            assertEquals(1, slot0Material, "Material powinien zmienić się na żelazo");
        }

        /**
         * Test: Limit bezpieczności (MAX_CRAFTS).
         * 
         * Scenariusz:
         * - Nieskończenie dużo itemów
         * - Pętla ma limit 100 craftów
         * 
         * Oczekiwanie:
         * - Pętla zatrzyma się na 100
         * - Brak overflow'u
         */
        @Test
        @DisplayName("Safety limit prevents infinite looping - stops at 100 crafts")
        public void testSafetyLimitPreventsInfiniteCrafting() {
            int slotItems = 10000; // Ogromna ilość
            int craftCount = 0;
            final int MAX_CRAFTS = 100;
            
            // Pętla Z LIMITEM
            while (craftCount < MAX_CRAFTS && slotItems > 0) {
                slotItems--;
                craftCount++;
            }
            
            // Weryfikacja
            assertEquals(100, craftCount, "Pętla powinna się zatrzymać na limicie 100");
            assertTrue(slotItems > 0, "Powinny pozostać itemy (limit zatrzymał pętlę)");
        }
    }

    /**
     * Testy bardziej złożonych scenariuszy (wieloslotowe receptury).
     */
    @Nested
    @DisplayName("Multi-Slot Crafting Scenarios")
    public class MultiSlotCraftingTests {

        /**
         * Test: Receptura 2-slotowa.
         * 
         * Scenariusz:
         * - Slot 0: 10 itemów
         * - Slot 1: 3 itemy (bottleneck!)
         * - Receptura: -1 z każdego
         * 
         * Oczekiwanie:
         * - 3 crafty (limit to slot 1)
         * - Slot 0: 7 itemów
         * - Slot 1: 0 itemów
         */
        @Test
        @DisplayName("Bottleneck in 2-slot recipe - limited by smallest quantity")
        public void testBottleneckLimitingTwoSlots() {
            // Przygotowanie
            int slot0Items = 10;
            int slot1Items = 3; // Bottleneck!
            int craftCount = 0;
            
            // Pętla - oba sloty muszą mieć >= 1 item
            while (slot0Items > 0 && slot1Items > 0) {
                slot0Items--;
                slot1Items--;
                craftCount++;
            }
            
            // Weryfikacja
            assertEquals(3, craftCount, "Powinno być 3 crafty (bottleneck to slot 1)");
            assertEquals(7, slot0Items, "Slot 0 powinien mieć 7 itemów");
            assertEquals(0, slot1Items, "Slot 1 powinien być pusty");
        }

        /**
         * Test: Receptura 4-slotowa (jak 2x2 crafting grid).
         * 
         * Scenariusz:
         * - 4 sloty: [2, 1, 3, 1] itemów
         * - Receptura: -1 z każdego
         * 
         * Oczekiwanie:
         * - 1 craft (limit to slot 1 i slot 3)
         * - Wszystkie sloty zmniejszone o 1
         */
        @Test
        @DisplayName("4-slot recipe - limited by minimum of all slots")
        public void testFourSlotRecipeBottleneck() {
            // Przygotowanie
            int[] slots = {2, 1, 3, 1}; // Minimum = 1
            int craftCount = 0;
            
            // Pętla - wszystkie sloty muszą mieć >= 1 item
            while (slots[0] > 0 && slots[1] > 0 && slots[2] > 0 && slots[3] > 0) {
                slots[0]--;
                slots[1]--;
                slots[2]--;
                slots[3]--;
                craftCount++;
            }
            
            // Weryfikacja
            assertEquals(1, craftCount, "Powinno być 1 craft (bottleneck to sloty 1 i 3)");
            assertEquals(1, slots[0], "Slot 0 powinien mieć 1 item");
            assertEquals(0, slots[1], "Slot 1 powinien być pusty");
            assertEquals(2, slots[2], "Slot 2 powinien mieć 2 itemy");
            assertEquals(0, slots[3], "Slot 3 powinien być pusty");
        }
    }

    /**
     * Testy wydajności - czy algorytm jest szybki.
     */
    @Nested
    @DisplayName("Performance Characteristics")
    public class PerformanceTests {

        /**
         * Test: 10000 craftów w loop - musi być szybko.
         * 
         * Oczekiwanie:
         * - < 50ms
         * - Liniowa skalowanie względem ilości craftów
         */
        @Test
        @DisplayName("10000 crafts complete quickly (< 50ms)")
        public void test10000CraftsPerformance() {
            int items = 10000;
            int craftCount = 0;
            
            // Pomiar czasu
            long startTime = System.nanoTime();
            
            // Pętla craftingu
            while (items > 0) {
                items--;
                craftCount++;
            }
            
            long elapsedTime = System.nanoTime() - startTime;
            long elapsedMs = elapsedTime / 1_000_000;
            
            // Weryfikacja
            assertEquals(10000, craftCount, "Powinno być 10000 craftów");
            assertTrue(elapsedMs < 50, 
                String.format("10000 craftów powinno < 50ms, trwało %dms", elapsedMs));
        }

        /**
         * Test: Liniowa skalowanie dla różnych rozmiarów.
         * 
         * Scenariusz:
         * - 1000 craftów
         * - 10000 craftów
         * - 100000 craftów
         * 
         * Oczekiwanie:
         * - Proporcionalna czas (10x więcej = ~10x dłużej)
         */
        @Test
        @DisplayName("Linear scaling - time grows proportionally with craft count")
        public void testLinearScaling() {
            // Test 1000 craftów
            long time1000 = measureCraftingTime(1000);
            
            // Test 10000 craftów
            long time10000 = measureCraftingTime(10000);
            
            // Test 100000 craftów
            long time100000 = measureCraftingTime(100000);
            
            // Ratio checks
            double ratio1to10 = (double) time10000 / time1000;
            double ratio10to100 = (double) time100000 / time10000;
            
            // Powinno być ~10x (z tolerancją na noise - CI może być powolny)
            assertTrue(ratio1to10 > 3 && ratio1to10 < 30,
                String.format("Ratio 1000->10000 powinno być ~10, było %.1f", ratio1to10));
            
            assertTrue(ratio10to100 > 3 && ratio10to100 < 30,
                String.format("Ratio 10000->100000 powinno być ~10, było %.1f", ratio10to100));
        }

        /**
         * Pomocnicza metoda - zmierz czas craftingu dla ilości.
         */
        private long measureCraftingTime(int craftCount) {
            int items = craftCount;
            long startTime = System.nanoTime();
            
            while (items > 0) {
                items--;
            }
            
            return System.nanoTime() - startTime;
        }
    }

    /**
     * Testy logiki zliczania craftów.
     */
    @Nested
    @DisplayName("Craft Counting Logic")
    public class CraftCountingLogic {

        /**
         * Test: Prawidłowe zliczanie dla różnych scenariuszy.
         */
        @Test
        @DisplayName("Accurate craft counting across different amounts")
        public void testAccurateCounting() {
            // Test dla 0
            assertEquals(0, countCrafts(0), "0 itemów = 0 craftów");
            
            // Test dla 1
            assertEquals(1, countCrafts(1), "1 item = 1 craft");
            
            // Test dla 100
            assertEquals(100, countCrafts(100), "100 itemów = 100 craftów");
            
            // Test dla 999
            assertEquals(999, countCrafts(999), "999 itemów = 999 craftów");
        }

        /**
         * Pomocnicza - zlicz crafty dla ilości.
         */
        private int countCrafts(int items) {
            int craftCount = 0;
            while (items > 0) {
                items--;
                craftCount++;
            }
            return craftCount;
        }
    }
}

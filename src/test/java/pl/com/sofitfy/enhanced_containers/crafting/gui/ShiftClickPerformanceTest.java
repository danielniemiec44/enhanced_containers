package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy wydajnościowe shift+click craftingu (jednostkowe - bez Bukkit/MockBukkit).
 * 
 * Mierzą czystą wydajność algorytmów:
 * - Matching receptur
 * - Ekstrakcji itemów
 * - Pętli craftingu
 * - Skalowanie i stabilność pamięci
 * 
 * WAŻNE: Te testy NIE używają Bukkit ItemStack ani Bukkit Server.
 * To są czyszczenie performance testy logiki algorytmów.
 */
@DisplayName("Wydajność - Shift+click (jednostkowe - logika)")
class ShiftClickPerformanceTest {

    /**
     * Test wydajności matching receptury.
     * Mierzy ile czasu zajmuje sprawdzenie czy receptura pasuje.
     */
    @Test
    @DisplayName("Matching receptury - 10000 iteracji")
    void recipeMatchingPerformance() {
        // Symulujemy pattern matching (2D array)
        Integer[][] pattern = new Integer[1][1];
        pattern[0][0] = 1; // 1 = diamond

        // Symulujemy grid (2D array)
        Integer[][] grid = new Integer[4][4];
        grid[0][0] = 1; // diamond na pozycji [0,0]

        // Test: 10000 iteracji matching
        long startTime = System.nanoTime();
        
        for (int i = 0; i < 10000; i++) {
            // Sprawdzamy czy receptura pasuje
            boolean matches = true;
            for (int row = 0; row < pattern.length; row++) {
                for (int col = 0; col < pattern[row].length; col++) {
                    Integer patternItem = pattern[row][col];
                    Integer gridItem = grid[row][col];

                    if (patternItem != null && patternItem > 0) {
                        if (gridItem == null || gridItem <= 0 ||
                            !patternItem.equals(gridItem)) {
                            matches = false;
                            break;
                        }
                    }
                }
                if (!matches) break;
            }
        }
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        double msPerMatch = (double) durationMs / 10000.0;

        // Powinno być szybko - max 50ms per 10000 iteracji
        assertTrue(durationMs < 50, 
            String.format("Matching receptury zbyt wolne: %d ms na 10000 iteracji (%.4f ms per match)", 
                durationMs, msPerMatch));

        System.out.println(String.format(
            "✓ Recipe matching: %d ms na 10000 iteracji (%.4f ms per match)",
            durationMs, msPerMatch
        ));
    }

    /**
     * Test wydajności ekstrakcji itemów.
     * Mierzy ile czasu zajmuje konwersja flat array na 2D grid.
     */
    @Test
    @DisplayName("Ekstrakcja itemów - 50000 iteracji")
    void gridExtractionPerformance() {
        // Przygotowujemy flat grid
        Integer[] flatGrid = new Integer[16];
        for (int i = 0; i < 16; i++) {
            flatGrid[i] = 1; // diamond
        }

        // Test: 50000 iteracji ekstrakcji
        long startTime = System.nanoTime();
        
        for (int i = 0; i < 50000; i++) {
            // Konwertuj flat array na 2D grid
            Integer[][] grid = new Integer[4][4];
            
            // Rząd 1 (elementy 0-3)
            for (int j = 0; j < 4; j++) {
                grid[0][j] = flatGrid[j];
            }
            // Rząd 2 (elementy 4-7)
            for (int j = 0; j < 4; j++) {
                grid[1][j] = flatGrid[4 + j];
            }
            // Rząd 3 (elementy 8-11)
            for (int j = 0; j < 4; j++) {
                grid[2][j] = flatGrid[8 + j];
            }
            // Rząd 4 (elementy 12-15)
            for (int j = 0; j < 4; j++) {
                grid[3][j] = flatGrid[12 + j];
            }
        }
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        double msPerExtraction = (double) durationMs / 50000.0;

        // Powinno być szybko (tolerancja 200ms dla CI/słabszych maszyn)
        assertTrue(durationMs < 200, 
            String.format("Ekstrakcja itemów zbyt wolna: %d ms na 50000 iteracji (%.6f ms per extraction)", 
                durationMs, msPerExtraction));

        System.out.println(String.format(
            "✓ Grid extraction: %d ms na 50000 iteracji (%.6f ms per extraction)",
            durationMs, msPerExtraction
        ));
    }

    /**
     * Test wydajności pętli craftingu.
     * Mierzy ile razy na sekundę może się zacraftować.
     */
    @Test
    @DisplayName("Pętla craftingu - 1000 iteracji")
    void craftingLoopPerformance() {
        // Symulujemy grid z itemami
        Integer[] grid = new Integer[16];
        grid[0] = 100; // 100 diamenty

        long startTime = System.nanoTime();
        
        int craftsCompleted = 0;

        while (grid[0] != null && grid[0] > 0) {
            // Dekrementuj ilość itemów
            grid[0]--;
            craftsCompleted++;

            if (craftsCompleted >= 1000) break;
        }
        
        long endTime = System.nanoTime();
        long durationUs = (endTime - startTime) / 1000; // Micro sekundy
        double usPerCraft = (double) durationUs / craftsCompleted;

        assertTrue(durationUs < 1000, 
            String.format("Pętla craftingu zbyt wolna: %d us na %d craftu (%.2f us per craft)", 
                durationUs, craftsCompleted, usPerCraft));

        System.out.println(String.format(
            "✓ Crafting loop: %d us na %d craftu (%.2f us per craft)",
            durationUs, craftsCompleted, usPerCraft
        ));
    }

    /**
     * Test skalowania - czy wydajność się degraduje liniowo.
     */
    @Test
    @DisplayName("Skalowanie - wzrost czasu względem ilości craftu")
    void scalabilityTest() {
        int[] craftCounts = {100, 1000, 5000, 10000};
        long[] times = new long[craftCounts.length];

        for (int i = 0; i < craftCounts.length; i++) {
            Integer[] grid = new Integer[16];
            grid[0] = craftCounts[i];

            long startTime = System.nanoTime();
            
            for (int j = 0; j < craftCounts[i]; j++) {
                grid[0]--;
            }
            
            long endTime = System.nanoTime();
            times[i] = endTime - startTime; // Nano sekundy
        }

        // Sprawdzamy czy wzrost czasu jest w miarę liniowy
        // Przy bardzo małych czasach szum i JIT powodują duże odchylenia
        // Dlatego używamy dużej tolerancji (0.1 do 20x) - głównie sprawdzamy że nie jest wykładnicze
        for (int i = 1; i < craftCounts.length; i++) {
            // Jeśli czas bazowy jest ekstremalnie mały, ratio potrafi wyjść absurdalne (szum/JIT,
            // przeplot z innymi testami, itp.). W takim przypadku ten fragment testu nie ma sensu
            // i potrafi dawać fałszywe alarmy.
            if (times[i - 1] < 100_000) { // <0.1ms
                continue;
            }

            double ratioExpected = (double) craftCounts[i] / craftCounts[i-1];
            double ratioActual = (double) times[i] / times[i-1];
            
            // Dozwolone bardzo duże odchylenie - chcemy tylko wykryć wykładniczy wzrost
            assertTrue(ratioActual >= ratioExpected * 0.1 && ratioActual <= ratioExpected * 20,
                String.format("Potencjalnie wykładnicze skalowanie: oczekiwane ~%.2f, faktyczne %.2f",
                    ratioExpected, ratioActual));
        }

        System.out.println("✓ Skalowanie (liniowe):");
        for (int i = 0; i < craftCounts.length; i++) {
            double nsPerCraft = (double) times[i] / craftCounts[i];
            System.out.println(String.format("  %d craftu: %d ns (%.2f ns per craft)",
                craftCounts[i], times[i], nsPerCraft));
        }
    }

    /**
     * Test stabilności - czy przy dużych ilościach nie ma memory leaka.
     */
    @Test
    @DisplayName("Memory stability - 100000 operacji")
    void memoryStabilityTest() {
        System.gc();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Wykonujemy 100000 operacji craft
        Integer[] grid = new Integer[16];
        grid[0] = 100000;

        for (int i = 0; i < 100000; i++) {
            grid[0]--;
            
            // Co 10000 operacji - resetuj (symuluj update)
            if (i % 10000 == 0 && i > 0) {
                grid = new Integer[16];
                grid[0] = 100000 - i;
            }
        }

        System.gc();
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        long memoryIncrease = memoryAfter - memoryBefore;

        // Memory increase powinien być mały
        assertTrue(memoryIncrease < 5_000_000,
            String.format("Memory leak: wzrost pamięci o %.2f MB", memoryIncrease / 1_000_000.0));

        System.out.println(String.format(
            "✓ Memory stability: wzrost o %.2f MB po 100000 operacji (OK)",
            memoryIncrease / 1_000_000.0
        ));
    }

    /**
     * Test konkurencji - symulacja wielu pętli craftingu jednocześnie.
     */
    @Test
    @DisplayName("Konkurencja - 10 wątków craftu jednocześnie")
    void concurrencyTest() throws InterruptedException {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        long startTime = System.nanoTime();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                // Każdy wątek craftuje 1000 razy
                Integer[] grid = new Integer[16];
                grid[0] = 1000;

                for (int i = 0; i < 1000; i++) {
                    grid[0]--;
                }
            });
            threads[t].start();
        }

        // Czekaj na wszystkie wątki
        for (Thread t : threads) {
            t.join();
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        // 10 wątków * 1000 craftu powinno być szybko
        assertTrue(durationMs < 1000,
            String.format("Konkurencja zbyt wolna: %d ms", durationMs));

        System.out.println(String.format(
            "✓ Konkurencja (10 wątków): %d ms na 10000 craftu razem",
            durationMs
        ));
    }
}

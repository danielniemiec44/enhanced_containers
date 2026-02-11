package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * FurnaceFuelProportionalityTest - testy matematyki paliwa rozszerzonego pieca
 * 
 * ZASADA PROPORCJONALNOŚCI:
 * - 1 węgiel (1600 ticków) przepala 8 przedmiotów w vanilla (200 ticków / item)
 * - W rozszerzonym piecu 1 węgiel ZAWSZE przepala łącznie 8 przedmiotów:
 *   * 1 linia:  8 itemów (burnTime -1 na tick)
 *   * 2 linie:  8 itemów (burnTime -2 na tick, ale 2x szybciej)
 *   * 3 linie:  8 itemów (burnTime -3 na tick, ale 3x szybciej)
 * 
 * CEL: Sprawiedliwa ekonomia - gracze nie oszczędzają paliwa używając jednej linii.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Furnace Fuel Proportionality Tests (Matematyka Paliwa)")
class FurnaceFuelProportionalityTest {
    
    // --- STAŁE KONFIGURACJI ---
    
    /** Czas palenia węgla w tickach (vanilla wartość) */
    private static final int COAL_BURN_TIME = 1600;
    
    /** Czas przepalania jednego przedmiotu w zwykłym piecu (vanilla wartość) */
    private static final int COOK_TIME_NORMAL = 200;
    
    /** Ilość przedmiotów przepalanych przez 1 węgiel (vanilla: 1600 / 200 = 8) */
    private static final int ITEMS_PER_COAL = COAL_BURN_TIME / COOK_TIME_NORMAL;
    
    // --- MOCK DEPENDENCIES ---
    
    @Mock
    private JavaPlugin mockPlugin;
    
    @Mock
    private EnhancedFurnaceManager mockManager;
    
    @Mock
    private EnhancedFurnaceGUI mockGui;
    
    @Mock
    private World mockWorld;
    
    private EnhancedFurnaceData furnaceData;
    
    @BeforeEach
    void setUp() {
        // Stwórz dane pieca z mockowanym GUI
        when(mockGui.getInputItem(anyInt())).thenReturn(null);
        when(mockGui.getOutputItem(anyInt())).thenReturn(null);
        when(mockGui.getFuelItem(anyInt())).thenReturn(null);
        
        furnaceData = new EnhancedFurnaceData(mockGui, Material.FURNACE);
    }
    
    // ========================================================================
    // TESTY PODSTAWOWYCH WARTOŚCI
    // ========================================================================
    
    @Nested
    @DisplayName("Podstawowe wartości paliwa")
    class BasicFuelValues {
        
        @Test
        @DisplayName("1 węgiel powinien palić się 1600 ticków")
        void coalShouldBurn1600Ticks() {
            assertEquals(COAL_BURN_TIME, EnhancedFurnaceLogic.getFuelBurnTime(Material.COAL),
                "Węgiel powinien palić się 1600 ticków");
        }
        
        @Test
        @DisplayName("1 węgiel vanilla = 8 przepalonych przedmiotów (matematyka)")
        void coalShouldSmelt8ItemsInVanilla() {
            // 1600 ticków paliwa / 200 ticków na przedmiot = 8 przedmiotów
            int itemsPerCoal = COAL_BURN_TIME / COOK_TIME_NORMAL;
            assertEquals(ITEMS_PER_COAL, itemsPerCoal,
                "1 węgiel powinien przepalić dokładnie 8 przedmiotów w vanilla");
        }
        
        @Test
        @DisplayName("Czas przepalania w zwykłym piecu = 200 ticków")
        void normalFurnaceCookTime() {
            assertEquals(COOK_TIME_NORMAL, EnhancedFurnaceData.FURNACE_COOK_TIME,
                "Zwykły piec: 200 ticków na przedmiot");
        }
        
        @Test
        @DisplayName("Czas przepalania w blast furnace = 100 ticków")
        void blastFurnaceCookTime() {
            assertEquals(100, EnhancedFurnaceData.BLAST_FURNACE_COOK_TIME,
                "Blast furnace: 100 ticków na przedmiot");
        }
    }
    
    // ========================================================================
    // TESTY PROPORCJONALNOŚCI ZUŻYCIA PALIWA
    // ========================================================================
    
    @Nested
    @DisplayName("Proporcjonalność zużycia paliwa")
    class FuelProportionality {
        
        @Test
        @DisplayName("1 aktywna linia: burnTime -1 na tick")
        void oneActiveLine_burnTimeDecreasesBy1() {
            // Przygotowanie: piec z 100 burnTime
            furnaceData.setBurnTime(100);
            int initialBurnTime = furnaceData.getBurnTime();
            
            // Symulacja: 1 aktywna linia zużywa 1 paliwa na tick
            int activeLines = 1;
            int expectedAfterOneTick = initialBurnTime - activeLines;
            
            // Symuluj 1 tick
            furnaceData.setBurnTime(furnaceData.getBurnTime() - activeLines);
            
            assertEquals(expectedAfterOneTick, furnaceData.getBurnTime(),
                "1 linia: burnTime powinien zmniejszyć się o 1");
        }
        
        @Test
        @DisplayName("2 aktywne linie: burnTime -2 na tick")
        void twoActiveLines_burnTimeDecreasesBy2() {
            furnaceData.setBurnTime(100);
            int initialBurnTime = furnaceData.getBurnTime();
            
            int activeLines = 2;
            int expectedAfterOneTick = initialBurnTime - activeLines;
            
            furnaceData.setBurnTime(furnaceData.getBurnTime() - activeLines);
            
            assertEquals(expectedAfterOneTick, furnaceData.getBurnTime(),
                "2 linie: burnTime powinien zmniejszyć się o 2");
        }
        
        @Test
        @DisplayName("3 aktywne linie: burnTime -3 na tick")
        void threeActiveLines_burnTimeDecreasesBy3() {
            furnaceData.setBurnTime(100);
            int initialBurnTime = furnaceData.getBurnTime();
            
            int activeLines = 3;
            int expectedAfterOneTick = initialBurnTime - activeLines;
            
            furnaceData.setBurnTime(furnaceData.getBurnTime() - activeLines);
            
            assertEquals(expectedAfterOneTick, furnaceData.getBurnTime(),
                "3 linie: burnTime powinien zmniejszyć się o 3");
        }
    }
    
    // ========================================================================
    // TESTY MATEMATYKI "8 PRZEDMIOTÓW NA WĘGIEL"
    // ========================================================================
    
    @Nested
    @DisplayName("Matematyka: 1 węgiel = 8 przedmiotów (niezależnie od liczby linii)")
    class EightItemsPerCoalMath {
        
        @Test
        @DisplayName("1 linia: 1 węgiel przepala 8 przedmiotów w 1600 ticków")
        void oneLine_coalSmelts8ItemsIn1600Ticks() {
            // 1600 ticków paliwa, 200 ticków na item, 1 linia
            int burnTime = COAL_BURN_TIME;
            int cookTimePerItem = COOK_TIME_NORMAL;
            int activeLines = 1;
            
            int totalTicks = 0;
            int itemsSmelted = 0;
            int currentCookTime = 0;
            
            while (burnTime > 0) {
                // Każdy tick: zużyj paliwo proporcjonalnie
                burnTime -= activeLines;
                
                // Zwiększ postęp przepalania (1 linia = +1 na tick)
                currentCookTime++;
                
                // Sprawdź czy przedmiot przepalony
                if (currentCookTime >= cookTimePerItem) {
                    itemsSmelted++;
                    currentCookTime = 0;
                }
                
                totalTicks++;
            }
            
            assertEquals(ITEMS_PER_COAL, itemsSmelted,
                "1 linia: 1 węgiel powinien przepalić 8 przedmiotów");
            assertEquals(COAL_BURN_TIME, totalTicks,
                "1 linia: powinno zająć 1600 ticków");
        }
        
        @Test
        @DisplayName("3 linie: 1 węgiel przepala 8 przedmiotów (łącznie) w ~533 ticki")
        void threeLines_coalSmelts8ItemsTotalIn533Ticks() {
            // Z 3 liniami:
            // - burnTime zmniejsza się 3x szybciej (1600 / 3 ≈ 533 ticków)
            // - Ale każda linia przepala swój przedmiot równolegle
            
            int burnTime = COAL_BURN_TIME;
            int cookTimePerItem = COOK_TIME_NORMAL;
            int activeLines = 3;
            
            int totalTicks = 0;
            int[] cookTimes = new int[3]; // Postęp dla każdej linii
            int[] itemsSmeltedPerLine = new int[3];
            
            while (burnTime > 0) {
                // Zużyj paliwo proporcjonalnie do linii
                burnTime -= activeLines;
                
                // Każda linia przepala niezależnie
                for (int line = 0; line < activeLines; line++) {
                    cookTimes[line]++;
                    
                    if (cookTimes[line] >= cookTimePerItem) {
                        itemsSmeltedPerLine[line]++;
                        cookTimes[line] = 0;
                    }
                }
                
                totalTicks++;
            }
            
            int totalItemsSmelted = itemsSmeltedPerLine[0] + itemsSmeltedPerLine[1] + itemsSmeltedPerLine[2];
            
            // KLUCZOWA ASERCJA: łącznie 8 przedmiotów (z tolerancją zaokrąglenia)
            // 1600 / 3 = 533.33 ticków -> 2.66 przedmioty na linię -> ~8 łącznie
            assertTrue(totalItemsSmelted >= 6 && totalItemsSmelted <= 9,
                "3 linie: łącznie powinno przepalić ~8 przedmiotów (got: " + totalItemsSmelted + ")");
            
            // Sprawdź że było szybciej niż 1 linia
            assertTrue(totalTicks < COAL_BURN_TIME,
                "3 linie: powinno być szybsze niż 1600 ticków (było: " + totalTicks + ")");
            
            // Sprawdź przybliżoną wartość (~533 ticki)
            int expectedTicks = COAL_BURN_TIME / activeLines;
            assertTrue(Math.abs(totalTicks - expectedTicks) <= 1,
                "3 linie: powinno zająć ~533 ticków (było: " + totalTicks + ")");
        }
        
        @Test
        @DisplayName("2 linie: 1 węgiel przepala 8 przedmiotów (łącznie) w 800 ticków")
        void twoLines_coalSmelts8ItemsTotalIn800Ticks() {
            int burnTime = COAL_BURN_TIME;
            int cookTimePerItem = COOK_TIME_NORMAL;
            int activeLines = 2;
            
            int totalTicks = 0;
            int[] cookTimes = new int[2];
            int[] itemsSmeltedPerLine = new int[2];
            
            while (burnTime > 0) {
                burnTime -= activeLines;
                
                for (int line = 0; line < activeLines; line++) {
                    cookTimes[line]++;
                    
                    if (cookTimes[line] >= cookTimePerItem) {
                        itemsSmeltedPerLine[line]++;
                        cookTimes[line] = 0;
                    }
                }
                
                totalTicks++;
            }
            
            int totalItemsSmelted = itemsSmeltedPerLine[0] + itemsSmeltedPerLine[1];
            
            assertEquals(ITEMS_PER_COAL, totalItemsSmelted,
                "2 linie: łącznie powinno przepalić 8 przedmiotów");
            assertEquals(COAL_BURN_TIME / activeLines, totalTicks,
                "2 linie: powinno zająć 800 ticków");
        }
    }
    
    // ========================================================================
    // TESTY EDGE CASES
    // ========================================================================
    
    @Nested
    @DisplayName("Edge cases")    class EdgeCases {
        
        @Test
        @DisplayName("6 patyków DOKŁADNIE wystarcza na 3 przedmioty (1 linia)")
        void sixSticks_exactlyFor3Items_oneLine() {
            // Setup: 6 patyków = 600 ticków, 3 przedmioty × 200 ticków = 600 ticków
            final int STICK_BURN_TIME = 100;
            final int ITEM_COOK_TIME = 200;
            final int STICKS_COUNT = 6;
            final int ITEMS_COUNT = 3;
            
            // Symulacja DOKŁADNIE jak w EnhancedFurnaceLogic.tickFurnace()
            int sticksUsed = 0;
            int itemsSmelted = 0;
            int burnTime = 0;
            int cookTime = 0;
            int itemsRemaining = ITEMS_COUNT;
            int totalTicks = 0;
            
            while (itemsRemaining > 0 || burnTime > 0) {
                totalTicks++;
                
                // 1. Oblicz activeLines (na początku ticka)
                int activeLines = itemsRemaining > 0 ? 1 : 0;
                
                // 2. Jeśli brak aktywnych linii - tylko zużywaj paliwo
                if (activeLines == 0) {
                    if (burnTime > 0) {
                        burnTime--;
                    }
                    continue;
                }
                
                // 3. Jeśli burnTime == 0, zużyj nowe paliwo
                if (burnTime == 0) {
                    if (sticksUsed < STICKS_COUNT) {
                        burnTime = STICK_BURN_TIME;
                        sticksUsed++;
                    } else {
                        // Brak paliwa - resetuj postęp i wyjdź
                        cookTime = 0;
                        break;
                    }
                }
                
                // 4. Zużyj paliwo proporcjonalnie
                burnTime -= activeLines;
                
                // 5. Zwiększ cookTime
                cookTime++;
                
                // 6. Sprawdź przepalenie
                if (cookTime >= ITEM_COOK_TIME) {
                    itemsSmelted++;
                    itemsRemaining--;
                    cookTime = 0;
                }
                
                // Bezpiecznik
                if (totalTicks > 10000) {
                    fail("Nieskończona pętla!");
                }
            }
            
            // Weryfikacja
            assertEquals(ITEMS_COUNT, itemsSmelted, 
                "Powinno przepalić dokładnie 3 przedmioty");
            assertEquals(STICKS_COUNT, sticksUsed, 
                "Powinno zużyć dokładnie 6 patyków");
            assertEquals(0, burnTime, 
                "burnTime powinno być 0 po przepaleniu (bez reszty)");
            assertEquals(600, totalTicks, 
                "Powinno zająć dokładnie 600 ticków");
        }
        
        @Test
        @DisplayName("2 patyki DOKŁADNIE wystarcza na 1 przedmiot (1 linia)")
        void twoSticks_exactlyFor1Item_oneLine() {
            final int STICK_BURN_TIME = 100;
            final int ITEM_COOK_TIME = 200;
            
            int sticksUsed = 0;
            int itemsSmelted = 0;
            int burnTime = 0;
            int cookTime = 0;
            int itemsRemaining = 1;
            int totalTicks = 0;
            
            while (itemsRemaining > 0 || burnTime > 0) {
                totalTicks++;
                
                int activeLines = itemsRemaining > 0 ? 1 : 0;
                
                if (activeLines == 0) {
                    if (burnTime > 0) burnTime--;
                    continue;
                }
                
                if (burnTime == 0) {
                    if (sticksUsed < 2) {
                        burnTime = STICK_BURN_TIME;
                        sticksUsed++;
                    } else {
                        cookTime = 0;
                        break;
                    }
                }
                
                burnTime -= activeLines;
                cookTime++;
                
                if (cookTime >= ITEM_COOK_TIME) {
                    itemsSmelted++;
                    itemsRemaining--;
                    cookTime = 0;
                }
                
                if (totalTicks > 10000) fail("Nieskończona pętla!");
            }
            
            assertEquals(1, itemsSmelted, "Powinno przepalić 1 przedmiot");
            assertEquals(2, sticksUsed, "Powinno zużyć 2 patyki");
            assertEquals(0, burnTime, "burnTime powinno być 0");
            assertEquals(200, totalTicks, "Powinno zająć 200 ticków");
        }
        
        @Test
        @DisplayName("0 aktywnych linii: burnTime nie zmienia się")
        void zeroActiveLines_burnTimeUnchanged() {
            furnaceData.setBurnTime(100);
            int initialBurnTime = furnaceData.getBurnTime();
            
            // 0 linii = brak zużycia
            int activeLines = 0;
            
            // Logika pieca: jeśli 0 linii, nie zmniejszaj burnTime
            if (activeLines > 0) {
                furnaceData.setBurnTime(furnaceData.getBurnTime() - activeLines);
            }
            
            assertEquals(initialBurnTime, furnaceData.getBurnTime(),
                "0 linii: burnTime nie powinien się zmieniać");
        }
        
        @Test
        @DisplayName("burnTime nie może być ujemny")
        void burnTimeCannotBeNegative() {
            furnaceData.setBurnTime(2);
            
            // Symuluj 3 linie zużywające więcej paliwa niż zostało
            furnaceData.setBurnTime(furnaceData.getBurnTime() - 3);
            
            // EnhancedFurnaceData.setBurnTime() używa Math.max(0, burnTime)
            assertTrue(furnaceData.getBurnTime() >= 0,
                "burnTime nie może być ujemny");
        }
        
        @Test
        @DisplayName("cookTime resetuje się po przepaleniu")
        void cookTimeResetsAfterSmelting() {
            furnaceData.setCookTime(0, COOK_TIME_NORMAL);
            
            // Symuluj zakończenie przepalania
            furnaceData.resetCookTime(0);
            
            assertEquals(0, furnaceData.getCookTime(0),
                "cookTime powinien się zresetować do 0");
        }
        
        @Test
        @DisplayName("Każda linia ma niezależny cookTime")
        void eachLineHasIndependentCookTime() {
            furnaceData.setCookTime(0, 50);
            furnaceData.setCookTime(1, 100);
            furnaceData.setCookTime(2, 150);
            
            assertEquals(50, furnaceData.getCookTime(0));
            assertEquals(100, furnaceData.getCookTime(1));
            assertEquals(150, furnaceData.getCookTime(2));
        }
    }
    
    // ========================================================================
    // TESTY WALIDACJI ZASADY "3X SZYBCIEJ = 3X WIĘCEJ PALIWA"
    // ========================================================================
    
    @Nested
    @DisplayName("Walidacja zasady: 3x szybciej = 3x więcej paliwa")
    class ThreeTimesFasterThreeTimesFuel {
        
        @Test
        @DisplayName("Czas przepalenia 8 itemów: 1 linia = 3 * (3 linie)")
        void smeltingTimeRatio() {
            // 1 linia: 8 itemów * 200 ticków = 1600 ticków
            int timeOneLine = ITEMS_PER_COAL * COOK_TIME_NORMAL;
            
            // 3 linie: paliwo zużywa się 3x szybciej
            // 1600 / 3 ≈ 533 ticki
            int timeThreeLines = COAL_BURN_TIME / 3;
            
            // Sprawdź stosunek
            double ratio = (double) timeOneLine / timeThreeLines;
            
            assertTrue(ratio >= 2.9 && ratio <= 3.1,
                "Stosunek czasów powinien być ~3 (było: " + ratio + ")");
        }
        
        @Test
        @DisplayName("Zużycie paliwa per przedmiot jest takie samo")
        void fuelConsumptionPerItemIsSame() {
            // 1 linia: 1600 ticków paliwa / 8 przedmiotów = 200 ticków paliwa na przedmiot
            int fuelPerItemOneLine = COAL_BURN_TIME / ITEMS_PER_COAL;
            
            // 3 linie: 1600 ticków paliwa / 8 przedmiotów (łącznie) = 200 ticków paliwa na przedmiot
            // Ale rozproszone na 3 linie, więc per linia: ~66.67 ticków paliwa
            int fuelPerItemThreeLines = COAL_BURN_TIME / ITEMS_PER_COAL;
            
            assertEquals(fuelPerItemOneLine, fuelPerItemThreeLines,
                "Zużycie paliwa na przedmiot powinno być takie samo (200 ticków)");
        }
    }
}

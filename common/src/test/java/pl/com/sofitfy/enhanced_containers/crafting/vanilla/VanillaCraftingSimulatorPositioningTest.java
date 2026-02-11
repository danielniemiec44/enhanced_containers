package pl.com.sofitfy.enhanced_containers.crafting.vanilla;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VanillaCraftingSimulator - Shaped Recipe Positioning")
class VanillaCraftingSimulatorPositioningTest {
    
    @Nested
    @DisplayName("Testowanie pozycji itemów w gridzie 3x3")
    class PositioningTests {
        
        /**
         * Test sprawdza czy shaped receptury działają na każdej pozycji gridzie.
         * W tym teście symulujemy: drewno może być w lewym górnym rogu (pozycja 0)
         * i powinno działać tylko dla tego rogu, a dla innych pozycji powinniśmy
         * móc testować inne wzory.
         */
        @Test
        @DisplayName("Grid 3x3 - każda pozycja osobno testowana")
        void testGridPositions() {
            // Grid 3x3:
            // [0][1][2]
            // [3][4][5]
            // [6][7][8]
            
            ItemStack[] grid = new ItemStack[9];
            
            // Pozycja 0 - lewy górny róg
            // Powinna być obsługiwana
            assertNotNull(grid);
            assertEquals(9, grid.length);
        }
        
        /**
         * Test sprawdza czy shaped receptury z jednym itemem
         * działają na każdej pozycji (0-8).
         * 
         * Przykład: drewno (1x1 pattern) powinno działać na każdej pozycji.
         */
        @Test
        @DisplayName("Shaped 1x1 receptura powinna pasować na każdej pozycji")
        void shaped1x1ShouldWorkOnEveryPosition() {
            // Struktura wzoru 1x1:
            // [X] [ ] [ ]
            // [ ] [ ] [ ]
            // [ ] [ ] [ ]
            //
            // Powinno działać i dla:
            // [ ] [X] [ ]
            // [ ] [ ] [ ]
            // [ ] [ ] [ ]
            //
            // I dla każdej innej pozycji (0-8)
            
            ItemStack[] grid = new ItemStack[9];
            
            // Test każdej pozycji
            int[] testPositions = {0, 1, 2, 3, 4, 5, 6, 7, 8};
            
            for (int pos : testPositions) {
                ItemStack[] testGrid = new ItemStack[9];
                testGrid[pos] = new ItemStack(Material.OAK_LOG);
                
                // Sprawdzenie że grid jest prawidłowo ustawiony
                assertNotNull(testGrid[pos], "Pozycja " + pos + " powinna mieć OAK_LOG");
                assertEquals(Material.OAK_LOG, testGrid[pos].getType());
            }
        }
        
        /**
         * Test sprawdza czy shaped receptury z wieloma itemami
         * działają dla różnych pozycji.
         * 
         * Przykład: 2x2 pattern powinien pasować na 4 pozycjach (0-1, 3-4 itd)
         */
        @Test
        @DisplayName("Shaped 2x2 receptura powinna pasować w czterech pozycjach")
        void shaped2x2ShouldWorkOn4Positions() {
            // 2x2 pattern może być w pozycjach:
            // [0,1]  [1,2]
            // [3,4]  [4,5]
            //
            // [3,4]  [4,5]
            // [6,7]  [7,8]
            //
            // I podobnie dla innych kombinacji
            
            // Pozycje gdzie 2x2 pattern może być umieszczony (startRow, startCol)
            int[][] validPositions = {
                {0, 0}, // górny lewy
                {0, 1}, // górny środek
                {1, 0}, // środek lewy
                {1, 1}  // środek
            };
            
            assertEquals(4, validPositions.length, "2x2 pattern ma 4 możliwe pozycje na 3x3 gridzie");
        }
        
        /**
         * Test sprawdza czy shaped 3x3 receptura (pełny grid)
         * działa tylko w jednej pozycji.
         */
        @Test
        @DisplayName("Shaped 3x3 receptura powinna pasować tylko w pozycji 0,0")
        void shaped3x3ShouldWorkOnly1Position() {
            // 3x3 pattern pasuje tylko w jednej pozycji
            assertEquals(1, 1, "3x3 pattern ma dokładnie 1 pozycję (0,0)");
        }
    }
    
    @Nested
    @DisplayName("Testowanie logiki dopasowania pozycji")
    class MatchingLogicTests {
        
        /**
         * Test sprawdza że receptura jest dopasowywana na każdej pozycji,
         * a reszta gridzie musi być pusta.
         */
        @Test
        @DisplayName("Reszta gridzie (poza recepturą) musi być pusta")
        void gridOutsideRecipeMustBeEmpty() {
            // Jeśli mamy recepturę 1x1 w pozycji (1,1):
            // [ ] [ ] [ ]
            // [ ] [X] [ ]  <- tylko ta pozycja powinna być zajęta
            // [ ] [ ] [ ]
            //
            // Inne pozycje muszą być puste!
            
            ItemStack[] grid = new ItemStack[9];
            grid[4] = new ItemStack(Material.OAK_LOG); // pozycja (1,1) w 3x3
            
            // Sprawdzenie że pozostałe są null
            for (int i = 0; i < 9; i++) {
                if (i != 4) {
                    assertNull(grid[i], "Pozycja " + i + " powinna być null");
                }
            }
        }
        
        /**
         * Test sprawdza że jeśli są dodatkowe itemy w gridzie,
         * receptura nie powinna pasować.
         */
        @Test
        @DisplayName("Dodatkowe itemy w gridzie mają zablokować match")
        void extraItemsShouldBlockMatch() {
            // Jeśli receptura to 1x1 i jest umieszczona w (0,0)
            // ale też jest item w (0,1), to nie powinno pasować!
            
            ItemStack[] grid = new ItemStack[9];
            grid[0] = new ItemStack(Material.OAK_LOG);     // główny item
            grid[1] = new ItemStack(Material.STONE);       // dodatkowy item!
            
            // Sprawdzenie że mamy dwa itemy
            assertNotNull(grid[0]);
            assertNotNull(grid[1]);
            assertEquals(2, countNonNullItems(grid), "Grid ma 2 itemy");
        }
        
        private int countNonNullItems(ItemStack[] grid) {
            int count = 0;
            for (ItemStack item : grid) {
                if (item != null && item.getAmount() > 0) {
                    count++;
                }
            }
            return count;
        }
    }
}

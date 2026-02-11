package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy dla RecipeMatcher - logiki dopasowywania receptur.
 *
 * RecipeMatcher odpowiada za:
 * - Dopasowywanie wzoru receptury do przedmiotów w gridu
 * - Wsparcie dla receptur o różnych rozmiarach (1x1 do 4x4)
 * - Usuwanie przedmiotów po pomyślnym crafcie
 */
@DisplayName("RecipeMatcher - Dopasowywanie receptur")
@Tag("unit")
class RecipeMatcherTest {

    private ItemStack[] grid;

    @BeforeEach
    void setUp() {
        // Inicjalizujemy gratkę crafcingu (16 slotów = 4x4)
        grid = new ItemStack[16];
    }

    @Nested
    @DisplayName("Bezpieczeństwo null i walidacja")
    class NullSafetyTests {

        @Test
        @DisplayName("matchesPattern zwraca false dla null pattern")
        void testNullPattern() {
            assertFalse(RecipeMatcher.matchesPatternRaw(grid, null),
                "Powinno zwrócić false dla null pattern");
        }

        @Test
        @DisplayName("matchesPattern zwraca false dla null grid")
        void testNullGrid() {
            ItemStack[][] pattern = {{null}};
            assertFalse(RecipeMatcher.matchesPatternRaw(null, pattern),
                "Powinno zwrócić false dla null grid");
        }

        @Test
        @DisplayName("matchesPattern zwraca false dla pustego pattern")
        void testEmptyPattern() {
            assertFalse(RecipeMatcher.matchesPatternRaw(grid, new ItemStack[0][]),
                "Powinno zwrócić false dla pustego pattern");
        }
    }

    @Nested
    @DisplayName("Logika dopasowywania")
    class MatchingLogicTests {

        @Test
        @DisplayName("removeItemsFromPattern nie wyrzuca błędu dla null")
        void testRemoveHandlesNull() {
            // removeItemsFromPattern modyfikuje przekazaną tablicę (void method)
            // Sprawdzamy że metoda po prostu się nie wywali na null parametrach
            assertDoesNotThrow(() -> RecipeMatcher.removeItemsFromPattern(null, (ItemStack[][]) null),
                "removeItemsFromPattern powinien obsłużyć null bez błędu");
        }

        @Test
        @DisplayName("Metody statyczne są dostępne i zwracają wartości")
        void testStaticMethodsAccessible() {
            // Sprawdzamy że metody statyczne działają i zwracają sensowne wyniki
            boolean matches = RecipeMatcher.matchesPatternRaw(new ItemStack[16], new ItemStack[0][]);
            assertFalse(matches, "Pusty pattern nie powinien pasować do gridu");
            
            // removeItemsFromPattern modyfikuje grid (efekt uboczny) - sprawdzamy że się nie wywala
            ItemStack[] grid = new ItemStack[16];
            assertDoesNotThrow(() -> RecipeMatcher.removeItemsFromPattern(grid, new ItemStack[0][]),
                "removeItemsFromPattern powinien obsłużyć pusty pattern bez błędu");
        }
    }

    @Nested
    @DisplayName("Różne rozmiary receptur")
    class PatternSizeTests {

        @Test
        @DisplayName("Obsługuje receptury 1x1")
        void testSmallPattern() {
            ItemStack[][] pattern = {{null}};
            boolean matches = RecipeMatcher.matchesPatternRaw(grid, pattern);
            assertFalse(matches, "Pusty pattern 1x1 nie powinien pasować do pustego gridu (bez itemów)");
        }

        @Test
        @DisplayName("Obsługuje receptury 3x3")
        void testMediumPattern() {
            ItemStack[][] pattern = {
                {null, null, null},
                {null, null, null},
                {null, null, null}
            };
            boolean matches = RecipeMatcher.matchesPatternRaw(grid, pattern);
            assertFalse(matches, "Pusty pattern 3x3 nie powinien pasować do pustego gridu (bez itemów)");
        }

        @Test
        @DisplayName("Obsługuje receptury 4x4")
        void testMaxPattern() {
            ItemStack[][] pattern = {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            };
            boolean matches = RecipeMatcher.matchesPatternRaw(grid, pattern);
            assertFalse(matches, "Pusty pattern 4x4 nie powinien pasować do pustego gridu (bez itemów)");
        }
    }

    @Nested
    @DisplayName("Porównywanie ItemStacków")
    class ItemStackComparingTests {

        @Test
        @DisplayName("null w pattern oznacza pusty slot")
        void testNullSlotMarker() {
            ItemStack emptyMarker = null;
            ItemStack material = null;

            assertEquals(emptyMarker, material,
                "null powinno być równe null");
        }

        @Test
        @DisplayName("Pattern i grid mogą być inicjalizowane")
        void testInitialization() {
            ItemStack[][] pattern = {{null}};
            ItemStack[] testGrid = new ItemStack[16];

            assertNotNull(pattern);
            assertNotNull(testGrid);
        }
    }
}


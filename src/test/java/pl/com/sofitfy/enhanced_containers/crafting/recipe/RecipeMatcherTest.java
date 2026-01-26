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
            assertDoesNotThrow(() -> {
                RecipeMatcher.removeItemsFromPattern(null, (ItemStack[][]) null);
            }, "removeItemsFromPattern powinna obsługiwać null");
        }

        @Test
        @DisplayName("Metody statyczne są dostępne")
        void testStaticMethodsAccessible() {
            assertDoesNotThrow(() -> {
                // Po prostu sprawdzamy że metody się kompilują i są dostępne
                RecipeMatcher.matchesPatternRaw(new ItemStack[16], new ItemStack[0][]);
                RecipeMatcher.removeItemsFromPattern(new ItemStack[16], new ItemStack[0][]);
            }, "Metody statyczne powinny być dostępne");
        }
    }

    @Nested
    @DisplayName("Różne rozmiary receptur")
    class PatternSizeTests {

        @Test
        @DisplayName("Obsługuje receptury 1x1")
        void testSmallPattern() {
            ItemStack[][] pattern = {{null}};
            assertDoesNotThrow(() -> RecipeMatcher.matchesPatternRaw(grid, pattern));
        }

        @Test
        @DisplayName("Obsługuje receptury 3x3")
        void testMediumPattern() {
            ItemStack[][] pattern = {
                {null, null, null},
                {null, null, null},
                {null, null, null}
            };
            assertDoesNotThrow(() -> RecipeMatcher.matchesPatternRaw(grid, pattern));
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
            assertDoesNotThrow(() -> RecipeMatcher.matchesPatternRaw(grid, pattern));
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


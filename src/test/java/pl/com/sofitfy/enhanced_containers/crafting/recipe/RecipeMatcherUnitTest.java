package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testy jednostkowe RecipeMatchera.
 * Testuje:
 * - Walidacja wejściowych parametrów
 * - Prawidłowe zachowanie na null i puste dane
 * 
 * Testy czystej logiki - używamy Mockito do mockowania ItemStack'ów.
 */
@DisplayName("RecipeMatcher - validacja parametrów i null handling")
class RecipeMatcherUnitTest {

    @Mock
    private ItemStack mockItem1;

    @Mock
    private ItemStack mockItem2;

    @Mock
    private ItemStack mockItem3;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ========== Testy validacji ==========

    @Test
    @DisplayName("Powinno obsługiwać null pattern bez błędu")
    void testNullPatternHandling() {
        // Arrange
        ItemStack[] gridItems = new ItemStack[16];

        // Act
        PatternMatchResult result = RecipeMatcher.findPatternMatchRaw(gridItems, null);

        // Assert: Nie powinno zrzucić wyjątku
        assertFalse(result.isMatched());
    }

    @Test
    @DisplayName("Powinno obsługiwać null gridItems bez błędu")
    void testNullGridItemsHandling() {
        // Arrange
        ItemStack[][] pattern = new ItemStack[][]{
            {mockItem1, mockItem2}
        };

        // Act
        PatternMatchResult result = RecipeMatcher.findPatternMatchRaw(null, pattern);

        // Assert: Nie powinno zrzucić wyjątku
        assertFalse(result.isMatched());
    }

    @Test
    @DisplayName("Powinno obsługiwać pusty pattern")
    void testEmptyPattern() {
        // Arrange: Pusta tablica
        ItemStack[] gridItems = new ItemStack[16];
        ItemStack[][] pattern = new ItemStack[][]{};

        // Act
        PatternMatchResult result = RecipeMatcher.findPatternMatchRaw(gridItems, pattern);

        // Assert
        assertFalse(result.isMatched());
    }

    @Test
    @DisplayName("Powinno obsługiwać gridItems z mniej niż 16 elementami")
    void testTooSmallGridItems() {
        // Arrange: Za mała tablica
        ItemStack[] gridItems = new ItemStack[8]; // Za mała
        ItemStack[][] pattern = new ItemStack[][]{
            {mockItem1, mockItem2}
        };

        // Act
        PatternMatchResult result = RecipeMatcher.findPatternMatchRaw(gridItems, pattern);

        // Assert
        assertFalse(result.isMatched());
    }

    // ========== Testy matchesPattern wrapper ==========

    @Test
    @DisplayName("matchesPatternRaw powinno zwracać false dla null")
    void testMatchesPatternRawWithNull() {
        // Arrange
        ItemStack[] gridItems = new ItemStack[16];
        ItemStack[][] pattern = null;

        // Act
        boolean result = RecipeMatcher.matchesPatternRaw(gridItems, pattern);

        // Assert
        assertFalse(result);
    }

    // ========== Testy shapeless matching ==========

    @Test
    @DisplayName("matchesShapeless powinno zwracać false dla null ingredients")
    void testShapelessWithNullIngredients() {
        // Arrange
        ItemStack[] gridItems = new ItemStack[16];
        List<ItemStack> ingredients = null;

        // Act
        boolean result = RecipeMatcher.matchesShapeless(gridItems, ingredients);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("matchesShapeless powinno zwracać false dla pustych ingredients")
    void testShapelessWithEmptyIngredients() {
        // Arrange
        ItemStack[] gridItems = new ItemStack[16];
        List<ItemStack> ingredients = Arrays.asList();

        // Act
        boolean result = RecipeMatcher.matchesShapeless(gridItems, ingredients);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("matchesShapeless powinno zwracać false dla null gridItems")
    void testShapelessWithNullGridItems() {
        // Arrange
        List<ItemStack> ingredients = Arrays.asList(mockItem1);

        // Act
        boolean result = RecipeMatcher.matchesShapeless(null, ingredients);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("matchesShapeless nie powinno tolerować dodatkowego zajętego slotu (jak crafting table)")
    void testShapelessRejectsExtraOccupiedSlot() {
        // Arrange: Receptura shapeless wymaga 1 deski
        ItemStack required = mock(ItemStack.class);
        when(required.getType()).thenReturn(Material.OAK_PLANKS);
        when(required.getAmount()).thenReturn(1);
        when(required.hasItemMeta()).thenReturn(false);

        // Grid ma 2 zajęte sloty, oba z deskami
        ItemStack slot0 = createMutableMockStack(Material.OAK_PLANKS, 1);
        ItemStack slot1 = createMutableMockStack(Material.OAK_PLANKS, 1);

        ItemStack[] grid = new ItemStack[16];
        grid[0] = slot0;
        grid[1] = slot1; // "extra" slot

        // Act
        boolean matched = RecipeMatcher.matchesShapeless(grid, List.of(required));

        // Assert
        assertFalse(matched, "Dodatkowy zajęty slot powinien unieważnić match");
    }

    @Test
    @DisplayName("matchesShapeless powinno pozwolić na większy stack w tym samym slocie")
    void testShapelessAllowsBiggerStackInUsedSlot() {
        // Arrange: Receptura shapeless wymaga 1 deski
        ItemStack required = mock(ItemStack.class);
        when(required.getType()).thenReturn(Material.OAK_PLANKS);
        when(required.getAmount()).thenReturn(1);
        when(required.hasItemMeta()).thenReturn(false);

        // Grid ma 1 slot zajęty stackiem 3 desek
        ItemStack slot0 = createMutableMockStack(Material.OAK_PLANKS, 3);

        ItemStack[] grid = new ItemStack[16];
        grid[0] = slot0;

        // Act
        boolean matched = RecipeMatcher.matchesShapeless(grid, List.of(required));

        // Assert
        assertTrue(matched, "Większy stack w tym samym slocie jest poprawny jak w crafting table");
    }

    // ========== Helper do mockowania ItemStack z ilością ==========

    private static ItemStack createMutableMockStack(Material type, int amount) {
        AtomicInteger currentAmount = new AtomicInteger(amount);
        ItemStack stack = mock(ItemStack.class);

        when(stack.getType()).thenReturn(type);
        when(stack.hasItemMeta()).thenReturn(false);
        when(stack.getAmount()).thenAnswer(inv -> currentAmount.get());
        doAnswer(inv -> {
            int newAmount = inv.getArgument(0);
            currentAmount.set(newAmount);
            return null;
        }).when(stack).setAmount(anyInt());

        // clone() w RecipeMatcher jest wymagany, więc zwracamy nowy mock z tą samą logiką
        when(stack.clone()).thenAnswer(inv -> createMutableMockStack(type, currentAmount.get()));

        return stack;
    }

    // ========== Testy removeItemsFromMatch ==========

    @Test
    @DisplayName("removeItemsFromMatch powinno obsługiwać null matchResult")
    void testRemoveItemsWithNullMatch() {
        // Arrange
        ItemStack[] gridItems = new ItemStack[16];
        PatternMatchResult unmatchedResult = new PatternMatchResult();

        // Act: Powinna obsłużyć gracefully
        RecipeMatcher.removeItemsFromMatch(gridItems, unmatchedResult);

        // Assert: Grid powinien pozostać niezmienionym
        for (int i = 0; i < 16; i++) {
            assertNull(gridItems[i]);
        }
    }

    @Test
    @DisplayName("removeItemsFromMatch powinno obsługiwać null gridItems")
    void testRemoveItemsWithNullGridItems() {
        // Arrange
        PatternMatchResult matchResult = new PatternMatchResult();

        // Act: Powinna obsłużyć gracefully
        RecipeMatcher.removeItemsFromMatch(null, matchResult);

        // Assert: Nie powinno zrzucić wyjątku
    }

    // ========== Testy PatternMatchResult ==========

    @Test
    @DisplayName("PatternMatchResult dla no-match powinno mieć -1 dla pozycji")
    void testPatternMatchResultNoMatch() {
        // Act
        PatternMatchResult result = new PatternMatchResult();

        // Assert
        assertFalse(result.isMatched());
        assertEquals(-1, result.getStartRow());
        assertEquals(-1, result.getStartCol());
        assertNull(result.getTransformedPattern());
    }

    @Test
    @DisplayName("PatternMatchResult dla match powinno przechowywać pozycję")
    void testPatternMatchResultWithMatch() {
        // Arrange
        ItemStack[][] pattern = new ItemStack[][]{
            {mockItem1}
        };
        int row = 2;
        int col = 3;

        // Act
        PatternMatchResult result = new PatternMatchResult(pattern, row, col);

        // Assert
        assertTrue(result.isMatched());
        assertEquals(row, result.getStartRow());
        assertEquals(col, result.getStartCol());
        assertSame(pattern, result.getTransformedPattern());
    }

    // ========== Testy zachowania jak crafting table (shaped = brak "extra" itemów) ==========

    @Test
    @DisplayName("Shaped pattern 1x1 powinien pasować tylko gdy w gridzie nie ma dodatkowych itemów")
    void testShapedPatternRejectsExtraItemsOutsidePattern() {
        // Arrange: wzór 1x1
        ItemStack required = mock(ItemStack.class);
        when(required.getType()).thenReturn(Material.OAK_PLANKS);

        ItemStack[][] pattern = new ItemStack[][]{
            {required}
        };

        // Grid: 2 itemy (jeden pasuje do wzoru, drugi jest "extra")
        ItemStack matching = mock(ItemStack.class);
        when(matching.getType()).thenReturn(Material.OAK_PLANKS);
        when(matching.isSimilar(required)).thenReturn(true);

        ItemStack extra = mock(ItemStack.class);
        when(extra.getType()).thenReturn(Material.OAK_PLANKS);

        ItemStack[] gridItems = new ItemStack[16];
        gridItems[0] = matching; // (0,0)
        gridItems[1] = extra;    // (0,1) -> powinno zablokować match

        // Act
        PatternMatchResult result = RecipeMatcher.findPatternMatchRaw(gridItems, pattern);

        // Assert
        assertFalse(result.isMatched(), "Dodatkowy item poza patternem powinien unieważnić match");
    }

    @Test
    @DisplayName("Shaped pattern 1x1 powinien pasować gdy grid ma tylko wymagany item")
    void testShapedPatternMatchesWithoutExtraItems() {
        // Arrange
        ItemStack required = mock(ItemStack.class);
        when(required.getType()).thenReturn(Material.OAK_PLANKS);

        ItemStack[][] pattern = new ItemStack[][]{
            {required}
        };

        ItemStack matching = mock(ItemStack.class);
        when(matching.getType()).thenReturn(Material.OAK_PLANKS);
        when(matching.isSimilar(required)).thenReturn(true);

        ItemStack[] gridItems = new ItemStack[16];
        gridItems[0] = matching;

        // Act
        PatternMatchResult result = RecipeMatcher.findPatternMatchRaw(gridItems, pattern);

        // Assert
        assertTrue(result.isMatched(), "Brak dodatkowych itemów - match powinien być true");
    }
}

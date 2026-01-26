package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy dla RecipeDraft - tymczasowa receptura podczas edycji
 *
 * RecipeDraft odpowiada za:
 * - Przechowywanie danych edycji receptury
 * - Walidację danych (czy są input i output)
 * - Konwersję do CraftingRecipe
 */
@DisplayName("RecipeDraft - Edycja receptury")
@Tag("unit")
class RecipeDraftTest {

    private RecipeDraft draft;

    @BeforeEach
    void setUp() {
        // Stwórz nowy draft dla każdego testu
        draft = new RecipeDraft("enhanced_crafting_1");
    }

    @Nested
    @DisplayName("Tworzenie i inicjalizacja")
    class CreationTests {

        @Test
        @DisplayName("RecipeDraft jest tworzony z prawidłowym ID")
        void testDraftCreation() {
            assertNotNull(draft);
            assertEquals("enhanced_crafting_1", draft.getId());
        }

        @Test
        @DisplayName("Nowy draft ma domyślną permissję")
        void testDefaultPermission() {
            assertEquals("plethoras.tools.use", draft.getPermission());
        }

        @Test
        @DisplayName("Nowy draft ma domyślną nazwę")
        void testDefaultName() {
            // Tekst, który widać w GUI, trzymamy po angielsku.
            assertEquals("New Recipe", draft.getName());
        }

        @Test
        @DisplayName("Nowy draft ma pusty output")
        void testEmptyOutput() {
            assertNull(draft.getOutput());
        }

        @Test
        @DisplayName("Nowy draft ma grid 4x4")
        void testGridSize() {
            ItemStack[][] input = draft.getInputPattern();
            assertNotNull(input);
            assertEquals(4, input.length); // 4 rzędy
            for (ItemStack[] row : input) {
                assertEquals(4, row.length); // 4 kolumny
            }
        }
    }

    @Nested
    @DisplayName("Gettery i settery")
    class GettersSettersTests {

        @Test
        @DisplayName("setName() zmienia nazwę")
        void testSetName() {
            draft.setName("Moja Receptura");
            assertEquals("Moja Receptura", draft.getName());
        }

        @Test
        @DisplayName("setPermission() zmienia uprawnienie")
        void testSetPermission() {
            draft.setPermission("plethoras.tools.custom");
            assertEquals("plethoras.tools.custom", draft.getPermission());
        }

        @Test
        @DisplayName("setId() zmienia ID")
        void testSetId() {
            draft.setId("new_recipe_id");
            assertEquals("new_recipe_id", draft.getId());
        }

        @Test
        @DisplayName("Settery działają bez błędów")
        void testSettersWork() {
            assertDoesNotThrow(() -> {
                draft.setName("Test");
                draft.setPermission("test.perm");
                draft.setId("test_id");
            });
        }
    }

    @Nested
    @DisplayName("Walidacja - isEmpty()")
    class IsEmptyTests {

        @Test
        @DisplayName("Pusty draft zwraca isEmpty() = true")
        void testEmptyDraftIsEmpty() {
            assertTrue(draft.isEmpty(), "Pusty draft powinien być pusty");
        }

        @Test
        @DisplayName("Draft z null output zwraca isEmpty() = true")
        void testNullOutputIsEmpty() {
            draft.setOutput(null);
            assertTrue(draft.isEmpty());
        }

        @Test
        @DisplayName("Draft ze zmienioną nazwą ale bez input/output to still empty")
        void testOnlyNameIsEmpty() {
            draft.setName("Moja Receptura");
            assertTrue(draft.isEmpty(), "Sama nazwa to nie wystarczy - muszą być itemy");
        }
    }

    @Nested
    @DisplayName("Konwersja na CraftingRecipe")
    class ConversionTests {

        @Test
        @DisplayName("Pusty draft nie konwertuje się na CraftingRecipe (zwraca null)")
        void testEmptyDraftConversionFails() {
            CraftingRecipe recipe = draft.toCraftingRecipe();
            assertNull(recipe, "Pusty draft powinien zwrócić null");
        }
    }

    @Nested
    @DisplayName("Logika biznesowa")
    class LogicTests {

        @Test
        @DisplayName("Draft przechowuje dane bez utraty")
        void testDataPersistence() {
            String newName = "Custom Recipe";
            String newPerm = "plethoras.tools.custom";

            draft.setName(newName);
            draft.setPermission(newPerm);

            assertEquals(newName, draft.getName());
            assertEquals(newPerm, draft.getPermission());
        }

        @Test
        @DisplayName("toString() zwraca reprezentację")
        void testToString() {
            String str = draft.toString();
            assertNotNull(str);
            assertTrue(str.contains("enhanced_crafting_1"));
        }
    }
}


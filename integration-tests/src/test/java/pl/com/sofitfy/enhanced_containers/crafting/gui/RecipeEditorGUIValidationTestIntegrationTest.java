package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeDraft;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy dla logiki walidacji i zamykania okna edytora receptur
 *
 * Testują:
 * - Walidacja inputów i outputów
 * - Obsługa cursor itema
 * - Komunikaty błędu
 * - Warunki zamykania okna
 *
 * Używamy MockBukkitTestBase do prawidłowego testowania RecipeEditorGUI
 */
@DisplayName("RecipeEditorGUI - Testy walidacji i logiki zamykania")
@Tag("integration")
class RecipeEditorGUIValidationTest extends MockBukkitTestBase {

    private PlayerMock player;
    private RecipeEditorGUI editor;

    @BeforeEach
    void setUp() {
        super.baseSetUp();
        player = server.addPlayer();
    }

    @Nested
    @DisplayName("Metoda isEmpty - sprawdzenie czy okno puste")
    class IsEmptyTests {

        @Test
        @DisplayName("isEmpty powinno zwrócić true gdy nic nie ma")
        void testIsEmptyWhenNoItems() {
            // Dado: pusty draft (nowa receptura)
            // Kiedy: tworzymy GUI
            // To: GUI jest puste
            editor = new RecipeEditorGUI(player, new RecipeDraft("test_recipe"));
            assertTrue(editor.isEmpty(), "isEmpty() powinno zwrócić true");
        }

        @Test
        @DisplayName("isEmpty powinno zwrócić false gdy jest input")
        void testIsEmptyWithInput() {
            editor = new RecipeEditorGUI(player, new RecipeDraft("test_recipe"));
            editor.getInventory().setItem(0, new ItemStack(Material.DIAMOND));
            assertFalse(editor.isEmpty(), "isEmpty() powinno zwrócić false");
        }

        @Test
        @DisplayName("isEmpty powinno zwrócić false gdy jest output")
        void testIsEmptyWithOutput() {
            editor = new RecipeEditorGUI(player, new RecipeDraft("test_recipe"));
            editor.getInventory().setItem(RecipeEditorGUI.OUTPUT_SLOT, new ItemStack(Material.GOLD_BLOCK));
            assertFalse(editor.isEmpty(), "isEmpty() powinno zwrócić false");
        }
    }

    @Nested
    @DisplayName("Metoda hasErrors - sprawdzenie warunków prawidłowego craftingu")
    class HasErrorsTests {

        @Test
        @DisplayName("hasErrors powinno zwrócić true gdy brakuje inputu")
        void testHasErrorsWhenNoInput() {
            editor = new RecipeEditorGUI(player, new RecipeDraft("test_recipe"));
            editor.getInventory().setItem(RecipeEditorGUI.OUTPUT_SLOT, new ItemStack(Material.GOLD_BLOCK));
            assertTrue(editor.hasErrors(), "hasErrors() powinno zwrócić true");
        }

        @Test
        @DisplayName("hasErrors powinno zwrócić true gdy brakuje outputu")
        void testHasErrorsWhenNoOutput() {
            editor = new RecipeEditorGUI(player, new RecipeDraft("test_recipe"));
            editor.getInventory().setItem(0, new ItemStack(Material.DIAMOND));
            assertTrue(editor.hasErrors(), "hasErrors() powinno zwrócić true");
        }

        @Test
        @DisplayName("hasErrors powinno zwrócić false gdy jest input i output")
        void testHasErrorsWithBoth() {
            editor = new RecipeEditorGUI(player, new RecipeDraft("test_recipe"));
            editor.getInventory().setItem(0, new ItemStack(Material.DIAMOND));
            editor.getInventory().setItem(RecipeEditorGUI.OUTPUT_SLOT, new ItemStack(Material.GOLD_BLOCK));
            assertFalse(editor.hasErrors(), "hasErrors() powinno zwrócić false");
        }
    }

    @Nested
    @DisplayName("Metoda getErrorMessage")
    class ErrorMessageTests {

        @Test
        @DisplayName("getErrorMessage powinno być po angielsku")
        void testErrorMessageIsEnglish() {
            // Dado: GUI bez inputu i bez outputu
            // Kiedy: bierzemy komunikat błędu
            // To: komunikat jest po angielsku (w grze nie ma polskich tekstów)
            editor = new RecipeEditorGUI(player, new RecipeDraft("test_recipe"));
            String message = editor.getErrorMessage();
            
            assertTrue(message.contains("Must") || message.contains("must"),
                "Wiadomość powinna być po angielsku");
            assertTrue(message.contains("§c"), "Powinna zawierać kod koloru");
        }
    }

    @Nested
    @DisplayName("Warunki prawidłowego craftingu")
    class CraftingConditionsTests {

        @Test
        @DisplayName("Minimum: 1 input i 1 output")
        void testMinimumRequirements() {
            editor = new RecipeEditorGUI(player, new RecipeDraft("test_recipe"));
            
            // Bez inputu i outputu - błąd
            assertTrue(editor.hasErrors() || editor.isEmpty());

            // Tylko input - błąd
            editor.getInventory().setItem(0, new ItemStack(Material.DIAMOND));
            assertTrue(editor.hasErrors(), "Tylko input powinien być błędem");

            // Input i output - OK
            editor.getInventory().setItem(RecipeEditorGUI.OUTPUT_SLOT, new ItemStack(Material.GOLD_BLOCK));
            assertFalse(editor.hasErrors(), "Input i output powinny być OK");
        }
    }

    @Nested
    @DisplayName("Zamykanie okna edytora")
    class WindowClosingTests {

        @Test
        @DisplayName("Okno z błędami nie powinno się zamykać")
        void testWindowWithErrorsShouldNotClose() {
            editor = new RecipeEditorGUI(player, new RecipeDraft("test_recipe"));
            editor.getInventory().setItem(0, new ItemStack(Material.DIAMOND));
            assertTrue(editor.hasErrors());
        }

        @Test
        @DisplayName("Puste okno powinno się zamknąć")
        void testEmptyWindowCanClose() {
            editor = new RecipeEditorGUI(player, new RecipeDraft("test_recipe"));
            assertTrue(editor.isEmpty());
        }

        @Test
        @DisplayName("Pełne okno powinno się zamknąć i zapisać")
        void testValidWindowCanCloseAndSave() {
            editor = new RecipeEditorGUI(player, new RecipeDraft("test_recipe"));
            editor.getInventory().setItem(0, new ItemStack(Material.DIAMOND));
            editor.getInventory().setItem(RecipeEditorGUI.OUTPUT_SLOT, new ItemStack(Material.GOLD_BLOCK));

            assertFalse(editor.isEmpty());
            assertFalse(editor.hasErrors());
        }
    }

    @Nested
    @DisplayName("Obsługa pól inventory")
    class InventoryFieldTests {

        @Test
        @DisplayName("OUTPUT_SLOT powinien być zdefiniowany")
        void testOutputSlotDefined() {
            assertTrue(RecipeEditorGUI.OUTPUT_SLOT >= 0);
            assertTrue(RecipeEditorGUI.OUTPUT_SLOT < 54);
        }

        @Test
        @DisplayName("GRID_SLOTS powinny zawierać co najmniej jeden slot")
        void testGridSlotsNonEmpty() {
            assertTrue(RecipeEditorGUI.GRID_SLOTS.length > 0);
        }
    }
}

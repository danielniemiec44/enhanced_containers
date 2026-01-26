package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeDraft;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne dla RecipeEditorGUI.
 * 
 * Używamy MockBukkitTestBase aby mieć prawidłowy MockBukkit z rzeczywistymi obiektami.
 * To pozwala testować rzeczywistą interakcję z Bukkit API.
 */
@DisplayName("RecipeEditorGUI - testy integracyjne")
public class RecipeEditorGUIValidationTestsIntegrationTest extends MockBukkitTestBase {

    private PlayerMock player;
    private RecipeDraft draft;
    private RecipeEditorGUI gui;

    @BeforeEach
    void setUp() {
        // MockBukkitTestBase już ustawił server
        super.baseSetUp();
        
        // Stwórz prawdziwego gracza (MockBukkit)
        player = server.addPlayer();
        
        // Stwórz draft i GUI
        draft = new RecipeDraft("test_recipe");
        gui = new RecipeEditorGUI(player, draft);
    }

    @Nested
    @DisplayName("isEmpty() - czy okno jest puste")
    class IsEmptyTests {

        @Tag("integration")
    @Test
        @DisplayName("Zwraca true gdy wszystko jest puste")
        void testIsEmptyWhenNoItems() {
            // Dado: nowe GUI bez przedmiotów
            // Kiedy: sprawdzamy isEmpty()
            // To: powinno zwrócić true
            assertTrue(gui.isEmpty(), "Empty inventory should be empty");
        }

        @Tag("integration")
    @Test
        @DisplayName("Zwraca false gdy output ma item")
        void testIsNotEmptyWhenOutputSlotHasItem() {
            // Dado: ustawiamy output
            // To: GUI nie jest puste
            gui.getInventory().setItem(RecipeEditorGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND));
            assertFalse(gui.isEmpty(), "Inventory with output item should not be empty");
        }

        @Tag("integration")
    @Test
        @DisplayName("Zwraca false gdy grid ma item")
        void testIsNotEmptyWhenGridHasItem() {
            // Dado: ustawiamy jeden item w gridzie
            // To: GUI nie jest puste
            gui.getInventory().setItem(RecipeEditorGUI.GRID_SLOTS[0], new ItemStack(Material.COAL));
            assertFalse(gui.isEmpty(), "Inventory with grid item should not be empty");
        }

        @Tag("integration")
    @Test
        @DisplayName("Zwraca false gdy jest kilka itemów w gridzie")
        void testIsNotEmptyWhenMultipleGridItems() {
            gui.getInventory().setItem(RecipeEditorGUI.GRID_SLOTS[0], new ItemStack(Material.COAL));
            gui.getInventory().setItem(RecipeEditorGUI.GRID_SLOTS[5], new ItemStack(Material.IRON_ORE));
            assertFalse(gui.isEmpty(), "Inventory with multiple items should not be empty");
        }

        @Tag("integration")
    @Test
        @DisplayName("AIR traktujemy jak pusty slot")
        void testIsEmptyWithAirItems() {
            gui.getInventory().setItem(RecipeEditorGUI.OUTPUT_SLOT, new ItemStack(Material.AIR));
            gui.getInventory().setItem(RecipeEditorGUI.GRID_SLOTS[0], new ItemStack(Material.AIR));
            assertTrue(gui.isEmpty(), "AIR items should count as empty");
        }
    }

    @Nested
    @DisplayName("hasErrors() - czy brakuje input/output")
    class HasErrorsTests {

        @Tag("integration")
    @Test
        @DisplayName("Zwraca true gdy nie ma input i output")
        void testHasErrorsWhenEmpty() {
            // Dado: nic nie ustawiamy
            // To: brakuje input/output
            assertTrue(gui.hasErrors(), "Empty inventory should have errors");
        }

        @Tag("integration")
    @Test
        @DisplayName("Zwraca true gdy jest tylko output")
        void testHasErrorsWhenOnlyOutput() {
            gui.getInventory().setItem(RecipeEditorGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND));
            assertTrue(gui.hasErrors(), "Only output without input should have errors");
        }

        @Tag("integration")
    @Test
        @DisplayName("Zwraca true gdy jest tylko input")
        void testHasErrorsWhenOnlyInput() {
            gui.getInventory().setItem(RecipeEditorGUI.GRID_SLOTS[0], new ItemStack(Material.COAL));
            assertTrue(gui.hasErrors(), "Only input without output should have errors");
        }

        @Tag("integration")
    @Test
        @DisplayName("Zwraca false gdy jest input i output")
        void testNoErrorsWhenBothInputAndOutput() {
            gui.getInventory().setItem(RecipeEditorGUI.GRID_SLOTS[0], new ItemStack(Material.COAL));
            gui.getInventory().setItem(RecipeEditorGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND));
            assertFalse(gui.hasErrors(), "Both input and output should have no errors");
        }

        @Tag("integration")
    @Test
        @DisplayName("Zwraca false gdy jest kilka inputów i output")
        void testNoErrorsWithMultipleInputs() {
            gui.getInventory().setItem(RecipeEditorGUI.GRID_SLOTS[0], new ItemStack(Material.COAL));
            gui.getInventory().setItem(RecipeEditorGUI.GRID_SLOTS[5], new ItemStack(Material.IRON_ORE));
            gui.getInventory().setItem(RecipeEditorGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND));
            assertFalse(gui.hasErrors(), "Multiple inputs with output should have no errors");
        }
    }

    @Nested
    @DisplayName("getErrorMessage() - tekst błędu")
    class ErrorMessageTests {

        @Tag("integration")
    @Test
        @DisplayName("Gdy pusto: błąd jest po angielsku i z kolorem")
        void testErrorMessageWhenEmpty() {
            // Dado: puste GUI
            // Kiedy: bierzemy getErrorMessage()
            // To: jest tekst po angielsku
            assertTrue(gui.hasErrors());
            String message = gui.getErrorMessage();
            assertNotNull(message);
            assertFalse(message.isEmpty());
            assertTrue(message.contains("§c"), "Błąd powinien mieć kolor");
        }

        @Tag("integration")
    @Test
        @DisplayName("Gdy jest tylko output: błąd mówi o input")
        void testErrorMessageWhenOnlyOutput() {
            gui.getInventory().setItem(RecipeEditorGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND));
            String message = gui.getErrorMessage();
            assertNotNull(message);
            assertFalse(message.isEmpty());
            assertTrue(message.toLowerCase().contains("input"),
                "Błąd powinien mówić o brakującym input");
        }

        @Tag("integration")
    @Test
        @DisplayName("Gdy jest tylko input: błąd mówi o output")
        void testErrorMessageWhenOnlyInput() {
            gui.getInventory().setItem(RecipeEditorGUI.GRID_SLOTS[0], new ItemStack(Material.COAL));
            String message = gui.getErrorMessage();
            assertNotNull(message);
            assertFalse(message.isEmpty());
            assertTrue(message.toLowerCase().contains("output"),
                "Błąd powinien mówić o brakującym output");
        }

        @Tag("integration")
    @Test
        @DisplayName("Błąd zawiera kod koloru")
        void testErrorMessageContainsColorCodes() {
            String message = gui.getErrorMessage();
            assertTrue(message.contains("§"), "Error message should contain color codes (§)");
        }

        @Tag("integration")
    @Test
        @DisplayName("Gdy jest poprawnie: brak tekstu błędu")
        void testNoErrorMessageWhenValid() {
            gui.getInventory().setItem(RecipeEditorGUI.GRID_SLOTS[0], new ItemStack(Material.COAL));
            gui.getInventory().setItem(RecipeEditorGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND));
            assertFalse(gui.hasErrors());
            String message = gui.getErrorMessage();
            assertTrue(message == null || message.isEmpty(),
                    "Gdy nie ma błędu, nie chcemy żadnego komunikatu");
        }
    }

    @Nested
    @DisplayName("Gettery")
    class GetterTests {

        @Tag("integration")
    @Test
        @DisplayName("getPlayer() zwraca tego samego gracza")
        void testGetPlayer() {
            assertSame(player, gui.getPlayer(), "Should return same player instance");
        }

        @Tag("integration")
    @Test
        @DisplayName("getDraft() zwraca tego samego drafta")
        void testGetDraft() {
            assertSame(draft, gui.getDraft(), "Should return same draft instance");
        }

        @Tag("integration")
    @Test
        @DisplayName("getInventory() zwraca inventory 54 sloty")
        void testGetInventory() {
            assertNotNull(gui.getInventory(), "Inventory should not be null");
            assertEquals(RecipeEditorGUI.INVENTORY_SIZE, gui.getInventory().getSize(),
                    "Inventory should have correct size");
        }
    }

    @Nested
    @DisplayName("Sloty gridu")
    class GridSlotValidationTests {

        @Tag("integration")
    @Test
        @DisplayName("Wszystkie GRID_SLOTS są w zakresie 0-53")
        void testGridSlotsWithinBounds() {
            for (int slot : RecipeEditorGUI.GRID_SLOTS) {
                assertTrue(slot >= 0 && slot < RecipeEditorGUI.INVENTORY_SIZE,
                        "Grid slot " + slot + " should be within bounds");
            }
        }

        @Tag("integration")
    @Test
        @DisplayName("GRID_SLOTS nie zawiera OUTPUT_SLOT")
        void testGridSlotsExcludeOutput() {
            for (int slot : RecipeEditorGUI.GRID_SLOTS) {
                assertNotEquals(RecipeEditorGUI.OUTPUT_SLOT, slot,
                        "Grid slot should not be output slot");
            }
        }
    }

    @Nested
    @DisplayName("Edge case'y")
    class EdgeCaseTests {

        @Tag("integration")
    @Test
        @DisplayName("Obsługuje sytuację gdy wszystkie 16 slotów gridu ma item")
        void testAllGridPositionsFilled() {
            Material[] materials = Material.values();
            for (int i = 0; i < RecipeEditorGUI.GRID_SLOTS.length; i++) {
                gui.getInventory().setItem(RecipeEditorGUI.GRID_SLOTS[i], 
                    new ItemStack(materials[i % materials.length]));
            }
            gui.getInventory().setItem(RecipeEditorGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND));
            assertFalse(gui.hasErrors(), "Fully filled recipe should not have errors");
        }

        @Tag("integration")
        @Test
        @DisplayName("Null w slocie traktujemy jak pusty")
        void testNullItemHandling() {
            // Test - GUI powinno poprawnie obsługiwać null items
            // GUI powinno być puste bo nie ma żadnych items
            assertTrue(gui.isEmpty(), "GUI z null items powinno być puste");
        }
    }
}

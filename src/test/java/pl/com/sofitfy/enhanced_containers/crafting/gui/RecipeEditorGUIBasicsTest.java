package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RecipeEditorGUI - testing constants and static methods
 */
class RecipeEditorGUIBasicsTest {

    @Nested
    class OutputSlotConstantTests {
        @Test
        void testOutputSlotValue() {
            assertEquals(24, RecipeEditorGUI.OUTPUT_SLOT);
        }
    }

    @Nested
    class GridSlotsConstantTests {
        @Test
        void testGridSlotsExists() {
            assertNotNull(RecipeEditorGUI.GRID_SLOTS);
        }

        @Test
        void testGridSlotsLength() {
            assertEquals(16, RecipeEditorGUI.GRID_SLOTS.length);
        }

        @Test
        void testGridSlotsSequence() {
            int[] expected = {0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30};
            assertArrayEquals(expected, RecipeEditorGUI.GRID_SLOTS);
        }

        @Test
        void testGridSlotsNoNegativeValues() {
            for (int slot : RecipeEditorGUI.GRID_SLOTS) {
                assertTrue(slot >= 0, "Grid slot should not be negative: " + slot);
            }
        }

        @Test
        void testGridSlotsValidForInventory() {
            for (int slot : RecipeEditorGUI.GRID_SLOTS) {
                assertTrue(slot < 54, "Grid slot should be valid inventory slot: " + slot);
            }
        }
    }

    @Nested
    class IsGridSlotTests {
        @Test
        void testIsGridSlotFirstSlot() {
            assertTrue(RecipeEditorGUI.isGridSlot(0));
        }

        @Test
        void testIsGridSlotLastGridSlot() {
            assertTrue(RecipeEditorGUI.isGridSlot(30));
        }

        @Test
        void testIsGridSlotMiddleSlot() {
            assertTrue(RecipeEditorGUI.isGridSlot(10));
        }

        @Test
        void testIsNotGridSlotOutputSlot() {
            assertFalse(RecipeEditorGUI.isGridSlot(24));
        }

        @Test
        void testIsNotGridSlotRandomSlot() {
            assertFalse(RecipeEditorGUI.isGridSlot(4));
            assertFalse(RecipeEditorGUI.isGridSlot(8));
            assertFalse(RecipeEditorGUI.isGridSlot(53));
        }

        @Test
        void testIsGridSlotAllGridSlots() {
            for (int slot : RecipeEditorGUI.GRID_SLOTS) {
                assertTrue(RecipeEditorGUI.isGridSlot(slot), "Slot " + slot + " should be grid slot");
            }
        }
    }

    @Nested
    class IsOutputSlotTests {
        @Test
        void testIsOutputSlotCorrect() {
            assertTrue(RecipeEditorGUI.isOutputSlot(24));
        }

        @Test
        void testIsNotOutputSlotGridSlots() {
            for (int slot : RecipeEditorGUI.GRID_SLOTS) {
                assertFalse(RecipeEditorGUI.isOutputSlot(slot), "Slot " + slot + " should not be output slot");
            }
        }

        @Test
        void testIsNotOutputSlotRandomSlots() {
            assertFalse(RecipeEditorGUI.isOutputSlot(0));
            assertFalse(RecipeEditorGUI.isOutputSlot(1));
            assertFalse(RecipeEditorGUI.isOutputSlot(25));
            assertFalse(RecipeEditorGUI.isOutputSlot(53));
        }
    }

    @Nested
    class InventorySizeTests {
        @Test
        void testInventorySizeConstant() {
            assertEquals(54, RecipeEditorGUI.INVENTORY_SIZE);
        }
    }

    @Nested
    class SlotDistributionTests {
        @Test
        void testOutputSlotNotInGridSlots() {
            for (int slot : RecipeEditorGUI.GRID_SLOTS) {
                assertNotEquals(slot, RecipeEditorGUI.OUTPUT_SLOT, 
                    "Output slot should not be in grid slots");
            }
        }

        @Test
        void testAllSlotsAreValid() {
            for (int slot : RecipeEditorGUI.GRID_SLOTS) {
                assertTrue(slot >= 0 && slot < RecipeEditorGUI.INVENTORY_SIZE,
                    "Slot " + slot + " should be valid for inventory");
            }
            assertTrue(RecipeEditorGUI.OUTPUT_SLOT >= 0 && 
                      RecipeEditorGUI.OUTPUT_SLOT < RecipeEditorGUI.INVENTORY_SIZE,
                      "Output slot should be valid for inventory");
        }
    }

    @Nested
    class LayoutTests {
        @Test
        void testGridSlots4x4Layout() {
            // Grid slots should form a 4x4 pattern
            // Rząd 0: slots 0,1,2,3 (w inventory: 0,1,2,3)
            // Rząd 1: slots 9,10,11,12 (w inventory: 9,10,11,12)
            // Rząd 2: slots 18,19,20,21 (w inventory: 18,19,20,21)
            // Rząd 3: slots 27,28,29,30 (w inventory: 27,28,29,30)

            int[] row0 = {0, 1, 2, 3};
            int[] row1 = {9, 10, 11, 12};
            int[] row2 = {18, 19, 20, 21};
            int[] row3 = {27, 28, 29, 30};

            for (int slot : row0) assertTrue(RecipeEditorGUI.isGridSlot(slot));
            for (int slot : row1) assertTrue(RecipeEditorGUI.isGridSlot(slot));
            for (int slot : row2) assertTrue(RecipeEditorGUI.isGridSlot(slot));
            for (int slot : row3) assertTrue(RecipeEditorGUI.isGridSlot(slot));
        }

        @Test
        void testOutputSlotPosition() {
            // Output slot should be at slot 24 (close to grid)
            assertEquals(24, RecipeEditorGUI.OUTPUT_SLOT);
            assertFalse(RecipeEditorGUI.isGridSlot(24));
        }
    }
}

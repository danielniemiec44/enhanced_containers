package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy dla EnhancedFurnaceGUI
 * Weryfikuje sloty inventory dla rozszerzonego pieca
 */
@DisplayName("EnhancedFurnaceGUI Tests")
class EnhancedFurnaceGUITest {
    
    @Nested
    @DisplayName("Slot Configuration")
    class SlotConfiguration {
        
        @Test
        @DisplayName("INPUT_SLOTS should have 3 slots")
        void inputSlotsShouldHave3Slots() {
            assertEquals(3, EnhancedFurnaceGUI.INPUT_SLOTS.length);
        }
        
        @Test
        @DisplayName("OUTPUT_SLOTS should have 3 slots")
        void outputSlotsShouldHave3Slots() {
            assertEquals(3, EnhancedFurnaceGUI.OUTPUT_SLOTS.length);
        }
        
        @Test
        @DisplayName("FUEL_SLOTS should have 3 slots")
        void fuelSlotsShouldHave3Slots() {
            assertEquals(3, EnhancedFurnaceGUI.FUEL_SLOTS.length);
        }
        
        @Test
        @DisplayName("Inventory size should be 54")
        void inventorySizeShouldBe54() {
            assertEquals(54, EnhancedFurnaceGUI.INVENTORY_SIZE);
        }
    }
    
    @Nested
    @DisplayName("Slot Detection")
    class SlotDetection {
        
        @Test
        @DisplayName("isInputSlot should return true for input slots")
        void isInputSlotShouldReturnTrueForInputSlots() {
            for (int slot : EnhancedFurnaceGUI.INPUT_SLOTS) {
                assertTrue(EnhancedFurnaceGUI.isInputSlot(slot), 
                    "Slot " + slot + " should be input slot");
            }
        }
        
        @Test
        @DisplayName("isInputSlot should return false for non-input slots")
        void isInputSlotShouldReturnFalseForNonInputSlots() {
            // Output slots
            for (int slot : EnhancedFurnaceGUI.OUTPUT_SLOTS) {
                assertFalse(EnhancedFurnaceGUI.isInputSlot(slot),
                    "Output slot " + slot + " should not be input slot");
            }
            
            // Fuel slots
            for (int slot : EnhancedFurnaceGUI.FUEL_SLOTS) {
                assertFalse(EnhancedFurnaceGUI.isInputSlot(slot),
                    "Fuel slot " + slot + " should not be input slot");
            }
        }
        
        @Test
        @DisplayName("isOutputSlot should return true for output slots")
        void isOutputSlotShouldReturnTrueForOutputSlots() {
            for (int slot : EnhancedFurnaceGUI.OUTPUT_SLOTS) {
                assertTrue(EnhancedFurnaceGUI.isOutputSlot(slot),
                    "Slot " + slot + " should be output slot");
            }
        }
        
        @Test
        @DisplayName("isFuelSlot should return true for fuel slots")
        void isFuelSlotShouldReturnTrueForFuelSlots() {
            for (int slot : EnhancedFurnaceGUI.FUEL_SLOTS) {
                assertTrue(EnhancedFurnaceGUI.isFuelSlot(slot),
                    "Slot " + slot + " should be fuel slot");
            }
        }
        
        @Test
        @DisplayName("isInteractiveSlot should return true for all interactive slots")
        void isInteractiveSlotShouldReturnTrueForAllInteractiveSlots() {
            // Input slots
            for (int slot : EnhancedFurnaceGUI.INPUT_SLOTS) {
                assertTrue(EnhancedFurnaceGUI.isInteractiveSlot(slot),
                    "Input slot " + slot + " should be interactive");
            }
            
            // Output slots
            for (int slot : EnhancedFurnaceGUI.OUTPUT_SLOTS) {
                assertTrue(EnhancedFurnaceGUI.isInteractiveSlot(slot),
                    "Output slot " + slot + " should be interactive");
            }
            
            // Fuel slots
            for (int slot : EnhancedFurnaceGUI.FUEL_SLOTS) {
                assertTrue(EnhancedFurnaceGUI.isInteractiveSlot(slot),
                    "Fuel slot " + slot + " should be interactive");
            }
        }
        
        @Test
        @DisplayName("isInteractiveSlot should return false for filler slots")
        void isInteractiveSlotShouldReturnFalseForFillerSlots() {
            // Testuj kilka slotów które nie są interaktywne
            // (sloty z GRAY_STAINED_GLASS_PANE)
            // Uwaga: 46, 47, 48 to teraz FUEL_SLOTS więc są interaktywne
            int[] fillerSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 49, 50, 51, 52, 53};
            
            for (int slot : fillerSlots) {
                assertFalse(EnhancedFurnaceGUI.isInteractiveSlot(slot),
                    "Filler slot " + slot + " should not be interactive");
            }
        }
    }
    
    @Nested
    @DisplayName("Slot Index Mapping")
    class SlotIndexMapping {
        
        @Test
        @DisplayName("getLineIndexForInputSlot should return correct index for input slots")
        void getLineIndexForInputSlotShouldReturnCorrectIndex() {
            for (int i = 0; i < EnhancedFurnaceGUI.INPUT_SLOTS.length; i++) {
                int slot = EnhancedFurnaceGUI.INPUT_SLOTS[i];
                assertEquals(i, EnhancedFurnaceGUI.getLineIndexForInputSlot(slot),
                    "Input slot " + slot + " should have index " + i);
            }
        }
        
        @Test
        @DisplayName("getLineIndexForInputSlot should return -1 for non-input slots")
        void getLineIndexForInputSlotShouldReturnMinus1ForNonInputSlots() {
            assertEquals(-1, EnhancedFurnaceGUI.getLineIndexForInputSlot(0));
            assertEquals(-1, EnhancedFurnaceGUI.getLineIndexForInputSlot(24));
            assertEquals(-1, EnhancedFurnaceGUI.getLineIndexForInputSlot(53));
        }
        
        @Test
        @DisplayName("getLineIndexForOutputSlot should return correct index for output slots")
        void getLineIndexForOutputSlotShouldReturnCorrectIndex() {
            for (int i = 0; i < EnhancedFurnaceGUI.OUTPUT_SLOTS.length; i++) {
                int slot = EnhancedFurnaceGUI.OUTPUT_SLOTS[i];
                assertEquals(i, EnhancedFurnaceGUI.getLineIndexForOutputSlot(slot),
                    "Output slot " + slot + " should have index " + i);
            }
        }
        
        @Test
        @DisplayName("getFuelIndexForSlot should return correct index for fuel slots")
        void getFuelIndexForSlotShouldReturnCorrectIndex() {
            for (int i = 0; i < EnhancedFurnaceGUI.FUEL_SLOTS.length; i++) {
                int slot = EnhancedFurnaceGUI.FUEL_SLOTS[i];
                assertEquals(i, EnhancedFurnaceGUI.getFuelIndexForSlot(slot),
                    "Fuel slot " + slot + " should have index " + i);
            }
        }
    }
    
    @Nested
    @DisplayName("Slot Layout")
    class SlotLayout {
        
        @Test
        @DisplayName("Input slots should be in left area")
        void inputSlotsShouldBeInLeftArea() {
            // Input: 11, 20, 29 (kolumna 2, rzędy 1,2,3)
            for (int slot : EnhancedFurnaceGUI.INPUT_SLOTS) {
                int column = slot % 9;
                assertTrue(column <= 3, "Input slot " + slot + " should be in left area (column " + column + ")");
            }
        }
        
        @Test
        @DisplayName("Output slots should be in right area")
        void outputSlotsShouldBeInRightArea() {
            // Output: 15, 24, 33 (kolumna 6, rzędy 1,2,3)
            for (int slot : EnhancedFurnaceGUI.OUTPUT_SLOTS) {
                int column = slot % 9;
                assertTrue(column >= 5, "Output slot " + slot + " should be in right area (column " + column + ")");
            }
        }
        
        @Test
        @DisplayName("Fuel slots should be in bottom row")
        void fuelSlotsShouldBeInBottomRow() {
            // Fuel: 46, 47, 48 (rząd 5, kolumny 1-3)
            for (int slot : EnhancedFurnaceGUI.FUEL_SLOTS) {
                int row = slot / 9;
                int column = slot % 9;
                assertEquals(5, row, "Fuel slot " + slot + " should be in row 5 (found row " + row + ")");
                assertTrue(column >= 1 && column <= 3, "Fuel slot " + slot + " should be in columns 1-3 (column " + column + ")");
            }
        }
    }
}

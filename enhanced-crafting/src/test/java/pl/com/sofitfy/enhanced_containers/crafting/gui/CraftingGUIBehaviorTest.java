package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy jednostkowe dla CraftingGUI.
 *
 * WAŻNE: bez MockBukkit.
 * W tym module nie uruchamiamy Bukkit'a, więc nie tworzymy prawdziwego inventory.
 * Sprawdzamy tylko stałe i układ slotów (inwarianty), które nie wymagają serwera.
 */
@DisplayName("CraftingGUI - stałe i układ slotów (unit)")
@Tag("unit")
class CraftingGUIBehaviorTest {

    @Nested
    @DisplayName("Stałe")
    class Constants {

        @Test
        @DisplayName("GUI ma zawsze 54 sloty")
        void inventorySizeIs54() {
            assertEquals(54, CraftingGUI.INVENTORY_SIZE);
        }

        @Test
        @DisplayName("Output slot jest zawsze w slocie 24")
        void outputSlotIs24() {
            assertEquals(24, CraftingGUI.OUTPUT_SLOT);
        }

        @Test
        @DisplayName("Grid ma 16 slotów (4x4)")
        void gridHas16Slots() {
            assertNotNull(CraftingGUI.GRID_SLOTS);
            assertEquals(16, CraftingGUI.GRID_SLOTS.length);
        }
    }

    @Nested
    @DisplayName("Układ")
    class Layout {

        @Test
        @DisplayName("Grid sloty mają dokładnie oczekiwaną sekwencję")
        void gridSlotsSequence() {
            int[] expected = {0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30};
            assertArrayEquals(expected, CraftingGUI.GRID_SLOTS);
        }

        @Test
        @DisplayName("Output slot nie należy do gridu")
        void outputNotInGrid() {
            for (int slot : CraftingGUI.GRID_SLOTS) {
                assertNotEquals(CraftingGUI.OUTPUT_SLOT, slot);
            }
        }

        @Test
        @DisplayName("Wszystkie sloty (grid + output) mieszczą się w 0..53")
        void slotsAreWithinInventoryBounds() {
            for (int slot : CraftingGUI.GRID_SLOTS) {
                assertTrue(slot >= 0 && slot < CraftingGUI.INVENTORY_SIZE, "Slot gridu poza zakresem: " + slot);
            }
            assertTrue(CraftingGUI.OUTPUT_SLOT >= 0 && CraftingGUI.OUTPUT_SLOT < CraftingGUI.INVENTORY_SIZE);
        }
    }
}


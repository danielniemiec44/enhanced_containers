package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy jednostkowe dla read-only output slotu.
 * 
 * Output slot powinien być dostępny TYLKO do:
 * - Shift+click (crafting wielokrotnie)
 * - Normalny LEFT/RIGHT/DOUBLE click (zabranie craftu)
 * 
 * Powinny być ZABLOKOWANE:
 * - Drag (przenoszenie)
 * - Shift+right click
 * - Wszystkie inne operacje
 * 
 * NIE WOLNO: Zastąpić output innym itemem!
 */
public class OutputSlotReadOnlyTest {

    /**
     * Symuluje ClickType z Bukkit
     */
    enum ClickType {
        LEFT,           // Normalny click lewym przyciskiem
        RIGHT,          // Normalny click prawym przyciskiem
        SHIFT_LEFT,     // Shift + lewy przycisk
        SHIFT_RIGHT,    // Shift + prawy przycisk
        DOUBLE_CLICK,   // Podwójny click
        MIDDLE,         // Środkowy przycisk (item swap)
        DROP,           // Q - wyrzucenie
        WINDOW_BORDER_LEFT,
        WINDOW_BORDER_RIGHT
    }

    /**
     * Symuluje operacje które mogą się dziać na output slocie
     */
    static class OutputSlotSecurityModel {
        
        /**
         * Sprawdza czy operacja powinna być dozwolona na output slocie
         * @param clickType typ klikania
         * @param hasOutput czy slot ma item output
         * @return true jeśli operacja powinna być DOZWOLONA, false jeśli ZABLOKOWANA
         */
        static boolean shouldAllowClick(ClickType clickType, boolean hasOutput) {
            // Jeśli brak outputu - zawsze blokuj
            if (!hasOutput) {
                return false;
            }
            
            // Dozwolone: Shift+click do craftingu wielokrotnie
            if (clickType == ClickType.SHIFT_LEFT) {
                return true;
            }
            
            // Dozwolone: Normalny click aby zabrać output (LEFT, RIGHT, DOUBLE)
            if (clickType == ClickType.LEFT || clickType == ClickType.RIGHT || clickType == ClickType.DOUBLE_CLICK) {
                return true;
            }
            
            // Wszystko inne: ZABLOKOWANE
            return false;
        }
    }

    @Nested
    @DisplayName("Output slot powinien być read-only")
    class OutputSlotReadOnly {

        @Test
        @DisplayName("Shift+click - DOZWOLONE (crafting wielokrotnie)")
        void testShiftClickAllowed() {
            boolean result = OutputSlotSecurityModel.shouldAllowClick(ClickType.SHIFT_LEFT, true);
            assertTrue(result, "Shift+click powinien być dozwolony do craftingu wielokrotnie");
        }

        @Test
        @DisplayName("Normalny LEFT click - DOZWOLONE (zabranie craftu)")
        void testLeftClickAllowed() {
            boolean result = OutputSlotSecurityModel.shouldAllowClick(ClickType.LEFT, true);
            assertTrue(result, "LEFT click powinien być dozwolony aby zabrać output");
        }

        @Test
        @DisplayName("Normalny RIGHT click - DOZWOLONE (zabranie craftu)")
        void testRightClickAllowed() {
            boolean result = OutputSlotSecurityModel.shouldAllowClick(ClickType.RIGHT, true);
            assertTrue(result, "RIGHT click powinien być dozwolony aby zabrać output");
        }

        @Test
        @DisplayName("DOUBLE click - DOZWOLONE (zabranie całego stacku)")
        void testDoubleClickAllowed() {
            boolean result = OutputSlotSecurityModel.shouldAllowClick(ClickType.DOUBLE_CLICK, true);
            assertTrue(result, "DOUBLE click powinien być dozwolony do zabrania całego stacku");
        }

        @Test
        @DisplayName("Shift+right click - ZABLOKOWANE (nie wolno go zastępować!)")
        void testShiftRightClickBlocked() {
            boolean result = OutputSlotSecurityModel.shouldAllowClick(ClickType.SHIFT_RIGHT, true);
            assertFalse(result, "Shift+right click powinien być ZABLOKOWANY - to może zastąpić output!");
        }

        @Test
        @DisplayName("MIDDLE click (item swap) - ZABLOKOWANE (nie wolno zamienić output!)")
        void testMiddleClickBlocked() {
            boolean result = OutputSlotSecurityModel.shouldAllowClick(ClickType.MIDDLE, true);
            assertFalse(result, "MIDDLE click (swap) powinien być ZABLOKOWANY");
        }

        @Test
        @DisplayName("DROP - ZABLOKOWANE (wyrzucenie przez Q)")
        void testDropBlocked() {
            boolean result = OutputSlotSecurityModel.shouldAllowClick(ClickType.DROP, true);
            assertFalse(result, "DROP (Q) powinien być ZABLOKOWANY na output slocie");
        }

        @Test
        @DisplayName("WINDOW_BORDER_LEFT - ZABLOKOWANE (drag)")
        void testWindowBorderBlocked() {
            boolean result = OutputSlotSecurityModel.shouldAllowClick(ClickType.WINDOW_BORDER_LEFT, true);
            assertFalse(result, "WINDOW_BORDER (drag) powinien być ZABLOKOWANY");
        }

        @Test
        @DisplayName("Pusty output - zawsze ZABLOKUJ (nic się nie może stać)")
        void testEmptyOutputBlocked() {
            // Gdy brak outputu - wszystkie kliki zablokowane
            assertFalse(OutputSlotSecurityModel.shouldAllowClick(ClickType.LEFT, false),
                    "LEFT click na pusty output powinien być ZABLOKOWANY");
            assertFalse(OutputSlotSecurityModel.shouldAllowClick(ClickType.SHIFT_LEFT, false),
                    "SHIFT+click na pusty output powinien być ZABLOKOWANY");
            assertFalse(OutputSlotSecurityModel.shouldAllowClick(ClickType.RIGHT, false),
                    "RIGHT click na pusty output powinien być ZABLOKOWANY");
        }
    }

    @Nested
    @DisplayName("Scenariusze bezpieczeństwa output slotu")
    class SecurityScenarios {

        @Test
        @DisplayName("Gracz NIE MOŻE zastąpić output innym itemem")
        void testCannotReplaceOutputItem() {
            // Scenariusz: gracz ma 64x iron w dłoni, output slot ma diamond
            // Jeśli gracz shift+right click, to by przefakował swap
            // To powinno być ZABLOKOWANE!
            
            boolean shiftRightAllowed = OutputSlotSecurityModel.shouldAllowClick(ClickType.SHIFT_RIGHT, true);
            assertFalse(shiftRightAllowed,
                    "SHIFT+RIGHT powinien być ZABLOKOWANY - zapobiega zastąpieniu output itemem!");
        }

        @Test
        @DisplayName("Gracz NIE MOŻE przenosić output przez drag")
        void testCannotDragOutput() {
            // Scenariusz: gracz draga output do dolnego inventory
            boolean dragAllowed = OutputSlotSecurityModel.shouldAllowClick(ClickType.WINDOW_BORDER_LEFT, true);
            assertFalse(dragAllowed,
                    "Drag powinien być ZABLOKOWANY na output slocie!");
        }

        @Test
        @DisplayName("Gracz MOŻE zabrać output lewym clickiem")
        void testCanTakeOutputWithLeftClick() {
            boolean leftClickAllowed = OutputSlotSecurityModel.shouldAllowClick(ClickType.LEFT, true);
            assertTrue(leftClickAllowed,
                    "LEFT click powinien być dozwolony - normalny sposób zabrania craftu");
        }

        @Test
        @DisplayName("Gracz MOŻE craftować wielokrotnie shift+click")
        void testCanShiftClickCraft() {
            boolean shiftClickAllowed = OutputSlotSecurityModel.shouldAllowClick(ClickType.SHIFT_LEFT, true);
            assertTrue(shiftClickAllowed,
                    "SHIFT+LEFT powinien być dozwolony dla craftingu wielokrotnie");
        }
    }

    @Nested
    @DisplayName("Porównanie z vanilla crafting table")
    class VanillaBehavior {

        @Test
        @DisplayName("Behawior identyczny jak vanilla - output read-only")
        void testVanillaBehavior() {
            // W vanilla crafting table output slot (prawy górny):
            // - Shift+click = crafting wielokrotnie ✅
            // - LEFT click = zabranie craftu ✅
            // - Nie można go zastąpić drag/drop ✅
            // - Nie można shift+right aby swap ✅
            
            assertTrue(OutputSlotSecurityModel.shouldAllowClick(ClickType.SHIFT_LEFT, true),
                    "Vanilla: Shift+click = craftuj wielokrotnie");
            assertTrue(OutputSlotSecurityModel.shouldAllowClick(ClickType.LEFT, true),
                    "Vanilla: LEFT click = weź output");
            assertFalse(OutputSlotSecurityModel.shouldAllowClick(ClickType.WINDOW_BORDER_LEFT, true),
                    "Vanilla: Drag jest zablokowany");
            assertFalse(OutputSlotSecurityModel.shouldAllowClick(ClickType.SHIFT_RIGHT, true),
                    "Vanilla: Shift+right jest zablokowany");
        }
    }

    @Nested
    @DisplayName("Zagrożenia bezpieczeństwa - co chcemy zablokować")
    class SecurityThreats {

        @Test
        @DisplayName("ZAGROŻENIE: Duplicating output przez shift+right swap")
        void testThreatDuplicateViaSwap() {
            // Gracz ma coś w dłoni, shift+right click na output
            // Bez ochrony: swap item z output slotem
            // Z ochroną: operacja zablokowana
            
            boolean shiftRightAllowed = OutputSlotSecurityModel.shouldAllowClick(ClickType.SHIFT_RIGHT, true);
            assertFalse(shiftRightAllowed,
                    "ZAGROŻENIE DUPLIKACJI: Shift+right swap powinien być zablokowany!");
        }

        @Test
        @DisplayName("ZAGROŻENIE: Replacing output itemem z dolnego inventory")
        void testThreatReplaceViaDrag() {
            // Gracz draga item z dolnego inventory do output slotu
            // Bez ochrony: output zostaje zastąpiony
            // Z ochroną: drag jest zablokowany
            
            boolean dragAllowed = OutputSlotSecurityModel.shouldAllowClick(ClickType.WINDOW_BORDER_LEFT, true);
            assertFalse(dragAllowed,
                    "ZAGROŻENIE ZMIANY: Drag powinien być zablokowany!");
        }

        @Test
        @DisplayName("BEZPIECZNE: Normalny click bezpieczny")
        void testSafeNormalClick() {
            // Gracz LEFT click na output
            // Bez obaw: item jest brany z output slotu, składniki usuwane
            boolean leftClickAllowed = OutputSlotSecurityModel.shouldAllowClick(ClickType.LEFT, true);
            assertTrue(leftClickAllowed,
                    "BEZPIECZEŃSTWO: Normalny LEFT click jest bezpieczny!");
        }

        @Test
        @DisplayName("BEZPIECZNE: Shift+click tylko dla craftingu")
        void testSafeShiftClick() {
            // Gracz SHIFT+click na output
            // Bez obaw: craftuje wielokrotnie (jak vanilla)
            boolean shiftClickAllowed = OutputSlotSecurityModel.shouldAllowClick(ClickType.SHIFT_LEFT, true);
            assertTrue(shiftClickAllowed,
                    "BEZPIECZŃSTWO: Shift+click jest bezpieczny!");
        }
    }
}

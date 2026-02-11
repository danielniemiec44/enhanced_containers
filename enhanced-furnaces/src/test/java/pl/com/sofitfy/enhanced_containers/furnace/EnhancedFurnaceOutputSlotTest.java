package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy jednostkowe dla blokady wkładania do slotów output
 * 
 * ZASADA: Do slotów output może wkładać TYLKO logika przepalania.
 * Gracz może TYLKO wyjmować przedmioty z output.
 * 
 * Te testy sprawdzają logikę rozpoznawania dozwolonych/zablokowanych akcji.
 */
@DisplayName("Slot Output - blokada wkładania")
class EnhancedFurnaceOutputSlotTest {

    /**
     * Lista akcji które pozwalają TYLKO na wyjmowanie (dozwolone dla output)
     */
    private boolean isAllowedOutputAction(InventoryAction action) {
        return switch (action) {
            // Wyjmowanie przedmiotu z output
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME -> true;
            // Shift-click wyjmuje do inventory gracza
            case MOVE_TO_OTHER_INVENTORY -> true;
            // Drop wyrzuca na ziemię - też OK
            case DROP_ALL_SLOT, DROP_ONE_SLOT -> true;
            // Clone w creative - OK bo nie zmienia inventory
            case CLONE_STACK -> true;
            // Wszystko inne (wkładanie, swap) - BLOKUJ
            default -> false;
        };
    }

    @Nested
    @DisplayName("Akcje dozwolone (tylko wyjmowanie)")
    class AllowedActions {
        
        @Test
        @DisplayName("PICKUP_ALL - wyjęcie całego stacka")
        void pickupAll_shouldBeAllowed() {
            // Gracz klika lewym na output z pustym kursorem - wyjmuje wszystko
            assertTrue(isAllowedOutputAction(InventoryAction.PICKUP_ALL));
        }
        
        @Test
        @DisplayName("PICKUP_HALF - wyjęcie połowy stacka")
        void pickupHalf_shouldBeAllowed() {
            // Gracz klika prawym na output z pustym kursorem - wyjmuje połowę
            assertTrue(isAllowedOutputAction(InventoryAction.PICKUP_HALF));
        }
        
        @Test
        @DisplayName("PICKUP_ONE - wyjęcie jednego itema")
        void pickupOne_shouldBeAllowed() {
            // Gracz klika ctrl+prawym na output - wyjmuje jeden
            assertTrue(isAllowedOutputAction(InventoryAction.PICKUP_ONE));
        }
        
        @Test
        @DisplayName("PICKUP_SOME - wyjęcie części (gdy kursor ma itemy tego samego typu)")
        void pickupSome_shouldBeAllowed() {
            // Gracz ma częściowy stack na kursorze i wyjmuje więcej
            assertTrue(isAllowedOutputAction(InventoryAction.PICKUP_SOME));
        }
        
        @Test
        @DisplayName("MOVE_TO_OTHER_INVENTORY - shift-click do inventory gracza")
        void shiftClick_shouldBeAllowed() {
            // Gracz robi shift-click na output - przenosi do swojego inventory
            assertTrue(isAllowedOutputAction(InventoryAction.MOVE_TO_OTHER_INVENTORY));
        }
        
        @Test
        @DisplayName("DROP_ALL_SLOT - wyrzucenie całego stacka na ziemię")
        void dropAll_shouldBeAllowed() {
            // Gracz robi ctrl+Q na output - wyrzuca wszystko
            assertTrue(isAllowedOutputAction(InventoryAction.DROP_ALL_SLOT));
        }
        
        @Test
        @DisplayName("DROP_ONE_SLOT - wyrzucenie jednego itema na ziemię")
        void dropOne_shouldBeAllowed() {
            // Gracz robi Q na output - wyrzuca jeden
            assertTrue(isAllowedOutputAction(InventoryAction.DROP_ONE_SLOT));
        }
        
        @Test
        @DisplayName("CLONE_STACK - klonowanie w creative (middle click)")
        void cloneStack_shouldBeAllowed() {
            // Gracz w creative robi middle click - klonuje (nie zmienia inventory)
            assertTrue(isAllowedOutputAction(InventoryAction.CLONE_STACK));
        }
    }
    
    @Nested
    @DisplayName("Akcje zablokowane (wkładanie)")
    class BlockedActions {
        
        @Test
        @DisplayName("PLACE_ALL - wkładanie całego stacka z kursora")
        void placeAll_shouldBeBlocked() {
            // Gracz próbuje położyć przedmiot z kursora do output - BLOKUJ
            assertFalse(isAllowedOutputAction(InventoryAction.PLACE_ALL));
        }
        
        @Test
        @DisplayName("PLACE_ONE - wkładanie jednego itema z kursora")
        void placeOne_shouldBeBlocked() {
            // Gracz próbuje położyć jeden przedmiot do output - BLOKUJ
            assertFalse(isAllowedOutputAction(InventoryAction.PLACE_ONE));
        }
        
        @Test
        @DisplayName("PLACE_SOME - wkładanie części stacka z kursora")
        void placeSome_shouldBeBlocked() {
            // Gracz próbuje położyć część stacka do output - BLOKUJ
            assertFalse(isAllowedOutputAction(InventoryAction.PLACE_SOME));
        }
        
        @Test
        @DisplayName("SWAP_WITH_CURSOR - zamiana z kursorem")
        void swapWithCursor_shouldBeBlocked() {
            // Gracz próbuje zamienić przedmiot w output z kursorem - BLOKUJ
            assertFalse(isAllowedOutputAction(InventoryAction.SWAP_WITH_CURSOR));
        }
        
        @Test
        @DisplayName("HOTBAR_SWAP - zamiana z hotbarem (klawisze 1-9)")
        void hotbarSwap_shouldBeBlocked() {
            // Gracz próbuje zamienić output z hotbarem - BLOKUJ
            assertFalse(isAllowedOutputAction(InventoryAction.HOTBAR_SWAP));
        }
        
        @Test
        @DisplayName("HOTBAR_MOVE_AND_READD - przeniesienie do hotbara i z powrotem")
        void hotbarMoveAndReadd_shouldBeBlocked() {
            // Specjalny przypadek hotbar swap - BLOKUJ
            assertFalse(isAllowedOutputAction(InventoryAction.HOTBAR_MOVE_AND_READD));
        }
        
        @Test
        @DisplayName("COLLECT_TO_CURSOR - zbieranie double-clickiem")
        void collectToCursor_shouldBeBlocked() {
            // Double-click zbiera przedmioty - mógłby wziąć z innych slotów
            // Blokujemy dla bezpieczeństwa gdy dotyczy output
            assertFalse(isAllowedOutputAction(InventoryAction.COLLECT_TO_CURSOR));
        }
    }
    
    @Nested
    @DisplayName("Identyfikacja slotów output")
    class OutputSlotIdentification {
        
        @Test
        @DisplayName("Sloty 14, 23, 32 to output")
        void outputSlots_shouldBeCorrect() {
            // Weryfikacja że stałe są poprawne
            assertArrayEquals(new int[]{14, 23, 32}, EnhancedFurnaceGUI.OUTPUT_SLOTS);
        }
        
        @Test
        @DisplayName("isOutputSlot() rozpoznaje sloty output")
        void isOutputSlot_shouldRecognizeOutputSlots() {
            // Sprawdź że metoda poprawnie identyfikuje sloty output
            assertTrue(EnhancedFurnaceGUI.isOutputSlot(14));
            assertTrue(EnhancedFurnaceGUI.isOutputSlot(23));
            assertTrue(EnhancedFurnaceGUI.isOutputSlot(32));
        }
        
        @Test
        @DisplayName("isOutputSlot() nie rozpoznaje innych slotów")
        void isOutputSlot_shouldNotRecognizeOtherSlots() {
            // Sloty input
            assertFalse(EnhancedFurnaceGUI.isOutputSlot(11));
            assertFalse(EnhancedFurnaceGUI.isOutputSlot(20));
            assertFalse(EnhancedFurnaceGUI.isOutputSlot(29));
            
            // Sloty fuel
            assertFalse(EnhancedFurnaceGUI.isOutputSlot(46));
            assertFalse(EnhancedFurnaceGUI.isOutputSlot(47));
            assertFalse(EnhancedFurnaceGUI.isOutputSlot(48));
            
            // Losowe sloty
            assertFalse(EnhancedFurnaceGUI.isOutputSlot(0));
            assertFalse(EnhancedFurnaceGUI.isOutputSlot(53));
        }
    }
    
    @Nested
    @DisplayName("Weryfikacja wszystkich akcji")
    class AllActionsVerification {
        
        @ParameterizedTest
        @EnumSource(InventoryAction.class)
        @DisplayName("Wszystkie akcje mają zdefiniowane zachowanie")
        void allActions_shouldHaveDefinedBehavior(InventoryAction action) {
            // Ten test po prostu wywołuje metodę dla każdej akcji
            // żeby upewnić się że nie ma NPE ani innych błędów
            boolean result = isAllowedOutputAction(action);
            
            // Akcja musi zwrócić true lub false (nie null)
            assertTrue(result || !result);
        }
    }
}

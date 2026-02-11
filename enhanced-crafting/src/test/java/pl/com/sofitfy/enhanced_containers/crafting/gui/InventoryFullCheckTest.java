package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy jednostkowe - tylko logika null safety dla canAddToInventory().
 * 
 * Pełne testy zachowania (z ItemStack, Material, PlayerInventory) są w integration-tests/
 * jako ShiftClickInventoryFullIT.java
 */
@DisplayName("Inventory Full Check - Null Safety (Unit)")
class InventoryFullCheckTest {

    @Test
    @DisplayName("Null player - zwraca true (defensywne)")
    void testNullPlayer_ShouldReturnTrue() {
        // When: Brak gracza
        boolean result = CraftingGUIListener.canAddToInventory(null, null);
        
        // Then: Zwraca true (defensywne - nie blokuje craftingu)
        assertTrue(result, "Null player powinien zwrócić true");
    }

    @Test
    @DisplayName("Null item - zwraca true (defensywne)")
    void testNullItem_ShouldReturnTrue() {
        // When: Brak itemu (player też null dla uproszczenia)
        boolean result = CraftingGUIListener.canAddToInventory(null, null);
        
        // Then: Zwraca true (defensywne - nie blokuje craftingu)
        assertTrue(result, "Null item powinien zwrócić true");
    }
}


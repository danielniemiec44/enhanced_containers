package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testy jednostkowe (bez MockBukkit) dla logiki dokładania outputu craftingu na kursor.
 *
 * Cel:
 * - Upewnić się, że pozwalamy na "vanilla-like" dokładanie tylko wtedy,
 *   gdy item na kursorze jest taki sam i jest miejsce w stacku.
 */
@DisplayName("Cursor stacking logic - Unit Test")
class CraftingGUICursorStackingUnitTest {

    @Test
    @DisplayName("canAddToCursor: pusty kursor zawsze OK")
    void canAddToCursor_emptyCursor_ok() {
        ItemStack output = mock(ItemStack.class);
        when(output.getType()).thenReturn(Material.IRON_INGOT);

        assertTrue(CraftingGUIListener.canAddToCursor(null, output, 9));

        ItemStack airCursor = mock(ItemStack.class);
        when(airCursor.getType()).thenReturn(Material.AIR);
        assertTrue(CraftingGUIListener.canAddToCursor(airCursor, output, 9));
    }

    @Test
    @DisplayName("canAddToCursor: inny item na kursorze blokuje")
    void canAddToCursor_differentItem_blocked() {
        ItemStack cursor = mock(ItemStack.class);
        when(cursor.getType()).thenReturn(Material.COBBLESTONE);

        ItemStack output = mock(ItemStack.class);
        when(output.getType()).thenReturn(Material.DIAMOND_BLOCK);

        when(cursor.isSimilar(output)).thenReturn(false);

        assertFalse(CraftingGUIListener.canAddToCursor(cursor, output, 1));
    }

    @Test
    @DisplayName("canAddToCursor: brak miejsca w stacku blokuje")
    void canAddToCursor_noSpace_blocked() {
        ItemStack cursor = mock(ItemStack.class);
        when(cursor.getType()).thenReturn(Material.DIAMOND_BLOCK);
        when(cursor.getAmount()).thenReturn(64);
        when(cursor.getMaxStackSize()).thenReturn(64);

        ItemStack output = mock(ItemStack.class);
        when(output.getType()).thenReturn(Material.DIAMOND_BLOCK);

        when(cursor.isSimilar(output)).thenReturn(true);

        assertFalse(CraftingGUIListener.canAddToCursor(cursor, output, 1));
    }

    @Test
    @DisplayName("buildNewCursorAfterTaking: pusty kursor -> wynik o odpowiedniej ilości")
    void buildNewCursorAfterTaking_emptyCursor_returnsPiece() {
        ItemStack output = mock(ItemStack.class);
        when(output.getType()).thenReturn(Material.IRON_INGOT);

        ItemStack outputClone = mock(ItemStack.class);
        when(output.clone()).thenReturn(outputClone);

        ItemStack newCursor = CraftingGUIListener.buildNewCursorAfterTaking(null, output, 9);
        assertNotNull(newCursor);
        assertSame(outputClone, newCursor, "Powinien wrócić klon outputu");
        verify(outputClone).setAmount(9);
    }

    @Test
    @DisplayName("buildNewCursorAfterTaking: ten sam item -> dokładamy ilość")
    void buildNewCursorAfterTaking_sameItem_merges() {
        ItemStack cursor = mock(ItemStack.class);
        when(cursor.getType()).thenReturn(Material.IRON_INGOT);
        when(cursor.getAmount()).thenReturn(9);
        when(cursor.getMaxStackSize()).thenReturn(64);

        ItemStack output = mock(ItemStack.class);
        when(output.getType()).thenReturn(Material.IRON_INGOT);

        ItemStack outputClone = mock(ItemStack.class);
        when(output.clone()).thenReturn(outputClone);

        when(cursor.isSimilar(output)).thenReturn(true);

        ItemStack cursorClone = mock(ItemStack.class);
        when(cursor.clone()).thenReturn(cursorClone);

        ItemStack newCursor = CraftingGUIListener.buildNewCursorAfterTaking(cursor, output, 9);
        assertNotNull(newCursor);
        assertSame(cursorClone, newCursor, "Powinien wrócić klon kursora (z doklejoną ilością)");
        verify(cursorClone).setAmount(18);
    }

    @Test
    @DisplayName("buildNewCursorAfterTaking: inny item -> null (blokada)")
    void buildNewCursorAfterTaking_differentItem_returnsNull() {
        ItemStack cursor = mock(ItemStack.class);
        when(cursor.getType()).thenReturn(Material.COBBLESTONE);

        ItemStack output = mock(ItemStack.class);
        when(output.getType()).thenReturn(Material.DIAMOND_BLOCK);

        ItemStack outputClone = mock(ItemStack.class);
        when(output.clone()).thenReturn(outputClone);

        when(cursor.isSimilar(output)).thenReturn(false);

        assertNull(CraftingGUIListener.buildNewCursorAfterTaking(cursor, output, 1));
    }
}

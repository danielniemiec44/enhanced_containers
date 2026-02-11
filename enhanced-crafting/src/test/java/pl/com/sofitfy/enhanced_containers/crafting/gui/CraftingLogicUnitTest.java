package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testy jednostkowe logiki craftingu.
 * Testuje wyodrębnianie przedmiotów z gridu i podstawowe operacje.
 * 
 * Uwaga: Bez MockBukkit - używamy Mockito do mockowania Bukkit objektów.
 */
@DisplayName("Logika wyodrębniania przedmiotów z gridu craftingu")
class CraftingLogicUnitTest {

    @Mock
    private CraftingGUI craftingGUI;

    @Mock
    private Inventory inventory;

    @Mock
    private Player player;

    @Mock
    private ItemStack mockItem1;

    @Mock
    private ItemStack mockItem2;

    private CraftingGUIListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new CraftingGUIListener();

        // Podstawowa konfiguracja mocków
        when(craftingGUI.getInventory()).thenReturn(inventory);
    }

    // ========== Testy wyodrębniania przedmiotów z gridu ==========

    @Test
    @DisplayName("Powinno wyodrębić pusty grid (16 null slotów)")
    void testExtractEmptyGrid() {
        // Arrange: Wszystkie sloty są puste
        for (int slot : CraftingGUI.GRID_SLOTS) {
            when(inventory.getItem(slot)).thenReturn(null);
        }

        // Act: Wyodrębni grid
        ItemStack[] gridItems = extractGridItems();

        // Assert: Wszystkie 16 slotów powinno być null
        for (int i = 0; i < 16; i++) {
            assertNull(gridItems[i], "Slot " + i + " powinno być null");
        }
    }

    @Test
    @DisplayName("Powinno wyodrębić grid z itemami w pierwszym rzędzie")
    void testExtractFirstRowItems() {
        // Arrange: Ustawiamy mock itemy w pierwszym rzędzie (sloty 0-3)
        when(inventory.getItem(0)).thenReturn(mockItem1);
        when(inventory.getItem(1)).thenReturn(mockItem2);
        when(inventory.getItem(2)).thenReturn(null);
        when(inventory.getItem(3)).thenReturn(null);

        // Pozostałe sloty puste
        for (int slot : CraftingGUI.GRID_SLOTS) {
            if (slot > 3) {
                when(inventory.getItem(slot)).thenReturn(null);
            }
        }

        // Act
        ItemStack[] gridItems = extractGridItems();

        // Assert: Pierwsze dwa sloty powinny mieć itemy
        assertSame(mockItem1, gridItems[0]);
        assertSame(mockItem2, gridItems[1]);
        assertNull(gridItems[2]);
        assertNull(gridItems[3]);
    }

    @Test
    @DisplayName("Powinno wyodrębić grid z itemami we wszystkich czterech rzędach")
    void testExtractFullGrid() {
        // Arrange: Ustawiamy mock itemy w każdym rzędzie

        // Rząd 1: sloty 0-3 (pozycje 0-3 w gridItems)
        when(inventory.getItem(0)).thenReturn(mockItem1);
        when(inventory.getItem(1)).thenReturn(mockItem2);
        when(inventory.getItem(2)).thenReturn(null);
        when(inventory.getItem(3)).thenReturn(null);

        // Rząd 2: sloty 9-12 (pozycje 4-7 w gridItems)
        ItemStack item3 = mock(ItemStack.class);
        ItemStack item4 = mock(ItemStack.class);
        when(inventory.getItem(9)).thenReturn(item3);
        when(inventory.getItem(10)).thenReturn(item4);
        when(inventory.getItem(11)).thenReturn(null);
        when(inventory.getItem(12)).thenReturn(null);

        // Rząd 3: sloty 18-21 (pozycje 8-11 w gridItems)
        when(inventory.getItem(18)).thenReturn(null);
        when(inventory.getItem(19)).thenReturn(null);
        when(inventory.getItem(20)).thenReturn(null);
        when(inventory.getItem(21)).thenReturn(null);

        // Rząd 4: sloty 27-30 (pozycje 12-15 w gridItems)
        when(inventory.getItem(27)).thenReturn(null);
        when(inventory.getItem(28)).thenReturn(null);
        when(inventory.getItem(29)).thenReturn(null);
        when(inventory.getItem(30)).thenReturn(null);

        // Act
        ItemStack[] gridItems = extractGridItems();

        // Assert: Powinien mieć itemy w pierwszych dwóch slotach każdego z dwóch pierwszych rzędów
        // Rząd 1
        assertSame(mockItem1, gridItems[0]);
        assertSame(mockItem2, gridItems[1]);

        // Rząd 2
        assertSame(item3, gridItems[4]);
        assertSame(item4, gridItems[5]);

        // Pozostałe powinny być null
        assertNull(gridItems[2]);
        assertNull(gridItems[3]);
        assertNull(gridItems[6]);
        assertNull(gridItems[7]);
    }

    @Test
    @DisplayName("Powinno mapować prawidłowo wszystkie 16 slotów w poprawnej kolejności")
    void testGridMappingOrder() {
        // Arrange: Ustawiamy różne mock itemy aby śledzić mapowanie
        ItemStack[] mockItems = new ItemStack[16];
        for (int i = 0; i < 16; i++) {
            mockItems[i] = mock(ItemStack.class);
        }

        int[] slotPositions = CraftingGUI.GRID_SLOTS;

        // Ustawiamy mockowe itemy w konkretnych slotach
        for (int i = 0; i < 16; i++) {
            when(inventory.getItem(slotPositions[i])).thenReturn(mockItems[i]);
        }

        // Act
        ItemStack[] gridItems = extractGridItems();

        // Assert: Każdy slot powinien być na właściwej pozycji w tej samej kolejności
        for (int i = 0; i < 16; i++) {
            assertSame(mockItems[i], gridItems[i], "gridItems[" + i + "] nie zgadza się z oczekiwanym");
        }
    }

    @Test
    @DisplayName("Powinno obsługiwać mieszane sloty puste i pełne")
    void testMixedEmptyAndFilledSlots() {
        // Arrange: Mieszane sloty
        when(inventory.getItem(0)).thenReturn(mockItem1);  // Pełny
        when(inventory.getItem(1)).thenReturn(null);       // Pusty
        when(inventory.getItem(2)).thenReturn(mockItem2);  // Pełny
        when(inventory.getItem(3)).thenReturn(null);       // Pusty

        // Reszta pusta
        for (int slot : CraftingGUI.GRID_SLOTS) {
            if (slot > 3) {
                when(inventory.getItem(slot)).thenReturn(null);
            }
        }

        // Act
        ItemStack[] gridItems = extractGridItems();

        // Assert
        assertSame(mockItem1, gridItems[0]);
        assertNull(gridItems[1]);
        assertSame(mockItem2, gridItems[2]);
        assertNull(gridItems[3]);
    }

    // ========== Helper methods ==========

    /**
     * Pomocnicza metoda do wyodrębniania grid itemów.
     * Używa reflection aby wezwać prywatną metodę extractGridItems z listenera.
     */
    private ItemStack[] extractGridItems() {
        try {
            // Używamy reflection aby wywołać extractGridItems
            java.lang.reflect.Method method = CraftingGUIListener.class.getDeclaredMethod("extractGridItems", CraftingGUI.class);
            method.setAccessible(true);
            return (ItemStack[]) method.invoke(listener, craftingGUI);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Nie mogę wywołać extractGridItems: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return null;
        }
    }
}

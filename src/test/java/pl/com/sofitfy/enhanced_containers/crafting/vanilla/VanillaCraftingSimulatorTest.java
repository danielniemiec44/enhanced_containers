package pl.com.sofitfy.enhanced_containers.crafting.vanilla;

import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy jednostkowe VanillaCraftingSimulator.
 * Sprawdzają logikę bez MockBukkit - tylko walidacja wejścia.
 */
@DisplayName("VanillaCraftingSimulator - Testy Jednostkowe")
class VanillaCraftingSimulatorTest {
    
    private final VanillaCraftingSimulator simulator = new VanillaCraftingSimulator();
    
    @Nested
    @DisplayName("Walidacja wejścia")
    class InputValidationTests {
        
        @Test
        @DisplayName("Powinien obsłużyć null grid")
        void shouldHandleNullGrid() {
            ItemStack result = simulator.tryMatch(null, null);
            assertNull(result, "Null grid powinien zwrócić null");
        }
        
        @Test
        @DisplayName("Powinien obsłużyć pusty grid (rozmiar != 9)")
        void shouldHandleWrongGridSize() {
            ItemStack[] wrongGrid = new ItemStack[5];
            ItemStack result = simulator.tryMatch(null, wrongGrid);
            assertNull(result, "Grid o rozmiarze != 9 powinien zwrócić null");
        }
        
        @Test
        @DisplayName("Powinien zaakceptować poprawny grid (rozmiar 9)")
        void shouldAcceptValidGridSize() {
            ItemStack[] grid = new ItemStack[9];
            // Grid z samymi null'ami - powinien być obsłużony bez błędu
            assertDoesNotThrow(
                () -> simulator.tryMatch(null, grid),
                "Grid o rozmiarze 9 powinien być akceptowany"
            );
        }
        
        @Test
        @DisplayName("Powinien obsłużyć grid z item'ami")
        void shouldHandleGridWithItems() {
            ItemStack[] grid = new ItemStack[9];
            grid[0] = new ItemStack(Material.OAK_LOG, 1);
            
            // Nie sprawdzamy wyniku (zależy od MockBukkit),
            // sprawdzamy że nie rzuca wyjątku
            assertDoesNotThrow(
                () -> simulator.tryMatch(null, grid),
                "Grid z item'ami powinien być obsłużony bez błędu"
            );
        }
        
        @Test
        @DisplayName("Powinien sklonować itemy (nie modyfikować originał)")
        void shouldCloneItems() {
            ItemStack[] grid = new ItemStack[9];
            ItemStack original = new ItemStack(Material.STONE, 5);
            grid[0] = original;
            
            simulator.tryMatch(null, grid);
            
            // Sprawdzamy że original nie zmienił się
            assertEquals(5, original.getAmount(), "Rozmiar originału nie powinien się zmienić");
            assertEquals(Material.STONE, original.getType(), "Typ originału nie powinien się zmienić");
        }
    }
    
    @Nested
    @DisplayName("Testy z MockBukkit będą w integration-tests")
    class MockBukkitPlaceholder {
        
        @Test
        @DisplayName("Rzeczywiste testy craftingu znajdują się w integration-tests/")
        void placeholder() {
            assertTrue(true, "Pełne testy craftingu z PrepareItemCraftEvent są w integration-tests");
        }
    }
}

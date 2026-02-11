package pl.com.sofitfy.enhanced_containers.crafting.nms;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Testy jednostkowe (bez MockBukkit).
 *
 * Tu świadomie NIE testujemy samego NMS craftingu, bo do tego potrzeba Paper serwera / dev bundle.
 * W tych testach sprawdzamy tylko kontrakt metody i zachowanie w środowisku bez NMS.
 */
class NmsCraftingEngineTest {

    @Test
    void shouldThrowWhenMatrixIsNull() {
        NmsCraftingEngine engine = new NmsCraftingEngine();
        World world = mock(World.class);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> engine.getNMSCraftingResult(null, world)
        );

        assertEquals("matrix must not be null", ex.getMessage());
    }

    @Test
    void shouldThrowWhenMatrixHasWrongSize() {
        NmsCraftingEngine engine = new NmsCraftingEngine();
        World world = mock(World.class);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> engine.getNMSCraftingResult(new ItemStack[16], world)
        );

        assertEquals("matrix must be length 9", ex.getMessage());
    }

    @Test
    void shouldThrowWhenWorldIsNull() {
        NmsCraftingEngine engine = new NmsCraftingEngine();

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> engine.getNMSCraftingResult(new ItemStack[9], null)
        );

        assertEquals("world must not be null", ex.getMessage());
    }

    @Test
    void shouldReturnNullWhenDirectNmsClassIsNotPresent() {
        // W Mavenowym buildzie (bez paperweight) klasy NmsCraftingEngineDirect nie ma.
        // Engine powinien wtedy po prostu zwrócić null, bez rzucania wyjątku.
        NmsCraftingEngine engine = new NmsCraftingEngine();
        World world = mock(World.class);

        ItemStack[] matrix = new ItemStack[9];
        matrix[0] = new ItemStack(Material.DIRT);

        ItemStack result = engine.getNMSCraftingResult(matrix, world);
        assertNull(result, "NMS engine bez dostępnej klasy CraftingManager powinien zwrócić null");
    }
}

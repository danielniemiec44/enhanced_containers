package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InputItemStackNormalizer - normalizacja inputów do amount=1")
class InputItemStackNormalizerUnitTest {

    @Test
    @DisplayName("Null i AIR mają dać null (pusty slot)")
    void nullAndAirReturnNull() {
        assertNull(InputItemStackNormalizer.toSingleOrNull(null));

        // Prosty przypadek: AIR w slocie ma być traktowany jak pusty.
        assertNull(InputItemStackNormalizer.toSingleOrNull(new ItemStack(Material.AIR, 64)));
    }

    @Test
    @DisplayName("Normalny item ma zostać skopiowany i ustawiony na amount=1")
    void itemIsCopiedAndAmountSetToOne() {
        ItemStack original = new ItemStack(Material.DIAMOND, 64);

        ItemStack normalized = InputItemStackNormalizer.toSingleOrNull(original);
        assertNotNull(normalized);

        // W inputach ilość nie ma znaczenia - zawsze chcemy 1.
        assertEquals(1, normalized.getAmount());
        assertEquals(Material.DIAMOND, normalized.getType());

        // Ważne: nie zmieniamy oryginału (żeby nie robić dziwnych side-effectów).
        assertEquals(64, original.getAmount());
    }

    @Test
    @DisplayName("Normalizacja całego wzoru ustawia amount=1 w każdym niepustym slocie")
    void patternNormalizationWorks() {
        ItemStack[][] pattern = new ItemStack[][] {
            { new ItemStack(Material.COAL, 10), null },
            { new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.AIR, 1) }
        };

        ItemStack[][] normalized = InputItemStackNormalizer.normalizePatternToSingle(pattern);
        assertNotNull(normalized);

        assertNotNull(normalized[0][0]);
        assertEquals(Material.COAL, normalized[0][0].getType());
        assertEquals(1, normalized[0][0].getAmount());

        assertNull(normalized[0][1]);

        assertNotNull(normalized[1][0]);
        assertEquals(Material.IRON_INGOT, normalized[1][0].getType());
        assertEquals(1, normalized[1][0].getAmount());

        // AIR ma być traktowany jak pusty.
        assertNull(normalized[1][1]);
    }
}

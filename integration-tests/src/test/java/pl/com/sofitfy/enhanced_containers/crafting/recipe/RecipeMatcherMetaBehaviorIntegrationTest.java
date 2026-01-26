package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integracyjne testy zachowania matchowania receptur dla itemów z meta/NBT.
 *
 * Cel:
 * - Vanilla match ignoruje meta (porównuje tylko material).
 * - Custom match (isSimilar) wymaga zgodności meta.
 *
 * To jest ważne, bo gracze mogą używać nazwanych/enchantowanych itemów.
 */
@DisplayName("RecipeMatcher: meta/NBT - integracja")
class RecipeMatcherMetaBehaviorIntegrationTest extends MockBukkitPluginTestBase {

    @Test
    @DisplayName("Vanilla match ignoruje meta, a custom match wymaga meta")
    void vanillaIgnoresMeta_customRequiresMeta() {
        // Named DIAMOND (ma meta)
        ItemStack namedDiamond = new ItemStack(Material.DIAMOND, 1);
        ItemMeta meta = namedDiamond.getItemMeta();
        assertNotNull(meta, "Meta powinna istnieć w teście (MockBukkit)");
        meta.displayName(Component.text("Special Diamond"));
        namedDiamond.setItemMeta(meta);

        // Grid 4x4 w postaci tablicy 16 elementów
        ItemStack[] grid = new ItemStack[16];
        grid[0] = namedDiamond;

        // VANILLA: wzór wymaga DIAMOND bez meta, ale i tak ma pasować
        ItemStack[][] vanillaPattern = new ItemStack[][] { { new ItemStack(Material.DIAMOND, 1) } };
        PatternMatchResult vanillaResult = RecipeMatcher.findPatternMatchVanilla(grid, vanillaPattern);
        assertTrue(vanillaResult.isMatched(), "Vanilla match powinien pasować po materiale, ignorując meta");

        // CUSTOM: wzór wymaga DIAMOND bez meta -> nie powinno pasować, bo meta się różni
        RecipePattern customPattern = new RecipePattern(new ItemStack[][] { { new ItemStack(Material.DIAMOND, 1) } });
        assertFalse(
            RecipeMatcher.matchesPatternExactly(grid, customPattern),
            "Custom match (exact) nie powinien pasować, jeśli meta się różni"
        );
    }

    @Test
    @DisplayName("Custom match pasuje, gdy meta jest identyczne")
    void customMatchesWhenMetaIsSame() {
        // Grid: named DIAMOND
        ItemStack namedDiamond = new ItemStack(Material.DIAMOND, 1);
        ItemMeta meta = namedDiamond.getItemMeta();
        assertNotNull(meta);
        meta.displayName(Component.text("Special Diamond"));
        namedDiamond.setItemMeta(meta);

        ItemStack[] grid = new ItemStack[16];
        grid[0] = namedDiamond;

        // Pattern: też named DIAMOND (taka sama meta)
        ItemStack required = new ItemStack(Material.DIAMOND, 1);
        ItemMeta requiredMeta = required.getItemMeta();
        assertNotNull(requiredMeta);
        requiredMeta.displayName(Component.text("Special Diamond"));
        required.setItemMeta(requiredMeta);

        RecipePattern customPattern = new RecipePattern(new ItemStack[][] { { required } });
        assertTrue(
            RecipeMatcher.matchesPatternExactly(grid, customPattern),
            "Custom match powinien pasować, gdy meta jest takie samo"
        );
    }
}

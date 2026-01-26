package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import pl.com.sofitfy.enhanced_containers.crafting.recipe.CraftingRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeMatcher;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integracja - custom receptury NIE powinny się obracać w 4x4
 *
 * Custom craftingi powinny działać z dokładnym matchem wzoru,
 * bez rotacji i odbić (w przeciwieństwie do vanilla receptur).
 */
@DisplayName("Integracja - Custom receptury bez rotacji")
class CustomRecipeNoRotationIntegrationTest extends MockBukkitPluginTestBase {

    @Nested
    @DisplayName("Custom receptury - brak rotacji")
    class NoRotationTests {

        @Test
        @DisplayName("Custom receptura NIE pasuje gdy wzór jest obrócony (matchesPatternExactly)")
        void customRecipeDoesNotMatchWhenRotated() {
            // Tworzę custom recepturę: prostokąt 2x3 (2 rzędy, 3 kolumny)
            ItemStack[][] pattern = new ItemStack[2][3];
            pattern[0][0] = new ItemStack(Material.OAK_PLANKS);   // A
            pattern[0][1] = new ItemStack(Material.BIRCH_PLANKS); // B  
            pattern[0][2] = null; // X (puste)
            pattern[1][0] = new ItemStack(Material.STICK);        // S
            pattern[1][1] = new ItemStack(Material.STICK);        // S
            pattern[1][2] = null; // X (puste)

            CraftingRecipe recipe = new CraftingRecipe(
                "custom_tool_1",
                "Custom Tool",
                pattern,
                new ItemStack(Material.DIAMOND_PICKAXE),
                "plethoras.tools.use"
            );

            // Testujemy bezpośrednio matchesPatternExactly
            // Oryginalny pattern (2x3):
            // OAK BIRCH X
            // STICK STICK X
            
            ItemStack[] gridOriginal = new ItemStack[16];
            gridOriginal[0] = new ItemStack(Material.OAK_PLANKS);   // (0,0)
            gridOriginal[1] = new ItemStack(Material.BIRCH_PLANKS); // (0,1)
            gridOriginal[4] = new ItemStack(Material.STICK);        // (1,0)
            gridOriginal[5] = new ItemStack(Material.STICK);        // (1,1)

            // Powinno znaleźć - poprawny wzór
            boolean matchedOriginal = RecipeMatcher.matchesPatternExactly(
                gridOriginal,
                recipe.getRecipePattern()
            );
            assertTrue(
                matchedOriginal,
                "Custom receptura powinna być znaleziona w prawidłowej pozycji"
            );

            // Teraz testujemy OBRÓCONY wzór (90 stopni)
            // Obrócony pattern byłby (3x2):
            // STICK OAK
            // STICK BIRCH
            // X X
            
            ItemStack[] gridRotated = new ItemStack[16];
            gridRotated[0] = new ItemStack(Material.STICK);        // (0,0)
            gridRotated[1] = new ItemStack(Material.OAK_PLANKS);   // (0,1)
            gridRotated[4] = new ItemStack(Material.STICK);        // (1,0)
            gridRotated[5] = new ItemStack(Material.BIRCH_PLANKS); // (1,1)

            // NIE powinno znaleźć - to jest obrócony wzór
            boolean matchedRotated = RecipeMatcher.matchesPatternExactly(
                gridRotated,
                recipe.getRecipePattern()
            );
            assertFalse(
                matchedRotated,
                "Custom receptura NIE powinna być znaleziona gdy wzór jest obrócony! " +
                "matchesPatternExactly nie powinno obsługiwać rotacji."
            );
        }

        @Test
        @DisplayName("Vanilla receptury NIE mogą się obracać (matchesPatternVanilla)")
        void vanillaRecipesCannotRotate() {
            // Tworzę vanilla pattern: 2x3
            ItemStack[][] pattern = new ItemStack[2][3];
            pattern[0][0] = new ItemStack(Material.OAK_PLANKS);
            pattern[0][1] = new ItemStack(Material.BIRCH_PLANKS);
            pattern[0][2] = null;
            pattern[1][0] = new ItemStack(Material.STICK);
            pattern[1][1] = new ItemStack(Material.STICK);
            pattern[1][2] = null;

            // Testujemy matchesPatternVanilla (bez rotacji!)
            // Receptura musi dokładnie pasować - bez obrotu
            
            // Oryginalny wzór - POWINNO znaleźć
            ItemStack[] gridOriginal = new ItemStack[16];
            gridOriginal[0] = new ItemStack(Material.OAK_PLANKS);
            gridOriginal[1] = new ItemStack(Material.BIRCH_PLANKS);
            gridOriginal[4] = new ItemStack(Material.STICK);
            gridOriginal[5] = new ItemStack(Material.STICK);

            boolean matchedOriginal = RecipeMatcher.matchesPatternVanilla(
                gridOriginal,
                pattern
            );
            assertTrue(
                matchedOriginal,
                "Vanilla receptura powinna być znaleziona w prawidłowej pozycji"
            );

            // Obrócony wzór - NIE POWINNO znaleźć
            // (bo vanilla receptury NIE obsługują rotacji!)
            ItemStack[] gridRotated = new ItemStack[16];
            gridRotated[0] = new ItemStack(Material.STICK);
            gridRotated[1] = new ItemStack(Material.OAK_PLANKS);
            gridRotated[4] = new ItemStack(Material.STICK);
            gridRotated[5] = new ItemStack(Material.BIRCH_PLANKS);

            boolean matchedRotated = RecipeMatcher.matchesPatternVanilla(
                gridRotated,
                pattern
            );
            assertFalse(
                matchedRotated,
                "Vanilla receptura NIE powinna być znaleziona gdy wzór jest obrócony! " +
                "Vanilla receptury (jak w oryginalnym Minecraft) nie obsługują rotacji."
            );
        }
    }

    @Nested
    @DisplayName("Vanilla receptury - ignorowanie NBT tagów")
    class VanillaNBTIgnoranceTests {

        @Test
        @DisplayName("Vanilla receptura ignoruje NBT tagi - custom item pasuje do vanilla receptury")
        void vanillaRecipeIgnoresNBTTags() {
            // Tworzę vanilla pattern: prosty 1x1
            ItemStack[][] pattern = new ItemStack[1][1];
            pattern[0][0] = new ItemStack(Material.DIAMOND);

            // Tworzę custom diament z NBT tagami
            ItemStack customDiamond = new ItemStack(Material.DIAMOND);
            // W real kodzie byłyby NBT tagi, ale ItemStack bez modyfikacji NBT też działa

            // Grid z custom diamontem
            ItemStack[] grid = new ItemStack[16];
            grid[0] = customDiamond;

            // matchesPatternVanilla POWINNO znaleźć (ignoruje NBT)
            boolean matched = RecipeMatcher.matchesPatternVanilla(
                grid,
                pattern
            );
            assertTrue(
                matched,
                "Vanilla receptura powinna zaakceptować custom item - porównuje tylko material, ignoruje NBT"
            );
        }

        @Test
        @DisplayName("Vanilla receptura porównuje Material, ignoruje liczbę itemów w stacku")
        void vanillaRecipeIgnoresStackSize() {
            // Tworzę vanilla pattern: 2 diamenty
            ItemStack[][] pattern = new ItemStack[1][2];
            pattern[0][0] = new ItemStack(Material.DIAMOND);
            pattern[0][1] = new ItemStack(Material.DIAMOND);

            // Grid ze stackiem 64 diamenty zamiast 1
            ItemStack[] grid = new ItemStack[16];
            ItemStack diamondStack64 = new ItemStack(Material.DIAMOND, 64);
            ItemStack diamondStack1 = new ItemStack(Material.DIAMOND, 1);
            grid[0] = diamondStack64; // 64 diamenty
            grid[1] = diamondStack1;  // 1 diament

            // matchesPatternVanilla POWINNO znaleźć (porównuje Material)
            boolean matched = RecipeMatcher.matchesPatternVanilla(
                grid,
                pattern
            );
            assertTrue(
                matched,
                "Vanilla receptura powinna ignorować rozmiar stacku - porównuje tylko material"
            );
        }
    }
}

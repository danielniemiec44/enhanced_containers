package pl.com.sofitfy.enhanced_containers.crafting.suggestions;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.CraftingRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.VanillaRecipe;

import java.util.List;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RecipeSuggestionEngineUnitTest {

    @Test
    void canCraftOnce_sumsAcrossMultipleStacks() {
        ItemStack invA = mockStack(Material.DIAMOND, 1);
        ItemStack invB = mockStack(Material.DIAMOND, 2);
        ItemStack[] inv = new ItemStack[] {invA, invB};

        ItemStack req1 = mockStack(Material.DIAMOND, 2);

        BiPredicate<ItemStack, ItemStack> matches = (have, need) -> have.getType() == need.getType();

        assertTrue(RecipeSuggestionEngine.canCraftOnce(inv, List.of(req1), matches));
    }

    @Test
    void canCraftOnce_returnsFalseWhenMissing() {
        ItemStack invA = mockStack(Material.IRON_INGOT, 1);
        ItemStack[] inv = new ItemStack[] {invA};

        ItemStack req1 = mockStack(Material.DIAMOND, 1);

        BiPredicate<ItemStack, ItemStack> matches = (have, need) -> have.getType() == need.getType();

        assertFalse(RecipeSuggestionEngine.canCraftOnce(inv, List.of(req1), matches));
    }

    @Test
    void computeSuggestions_limitsAndDedupesOutputs() {
        // Ekwipunek: 2x DIAMOND
        ItemStack inv = mockStack(Material.DIAMOND, 2);

        // Dwie receptury dają ten sam output (dedupe)
        CraftingRecipe r1 = mockCustomRecipe(
            "id1",
            patternOf(mockStack(Material.DIAMOND, 1)),
            mockStack(Material.EMERALD, 1)
        );
        CraftingRecipe r2 = mockCustomRecipe(
            "id2",
            patternOf(mockStack(Material.DIAMOND, 1)),
            mockStack(Material.EMERALD, 1)
        );

        // Trzecia receptura daje inny output
        CraftingRecipe r3 = mockCustomRecipe(
            "id3",
            patternOf(mockStack(Material.DIAMOND, 1)),
            mockStack(Material.GOLD_INGOT, 1)
        );

        // Vanilla nie potrzebne w tym teście
        List<VanillaRecipe> vanilla = List.of();

        List<ItemStack> suggestions = RecipeSuggestionEngine.computeSuggestions(
            new ItemStack[] {inv},
            List.of(r1, r2, r3),
            vanilla,
            1,
            (have, need) -> have.getType() == need.getType()
        );

        assertEquals(1, suggestions.size());
        assertEquals(Material.EMERALD, suggestions.get(0).getType());
    }

    private static ItemStack mockStack(Material type, int amount) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(type);
        when(stack.getAmount()).thenReturn(amount);
        when(stack.clone()).thenReturn(stack);
        when(stack.hasItemMeta()).thenReturn(false);
        when(stack.getItemMeta()).thenReturn(null);
        return stack;
    }

    private static CraftingRecipe mockCustomRecipe(String id, ItemStack[][] pattern, ItemStack output) {
        // Używamy realnej klasy, ale ItemStacki mogą być mockami.
        // To daje prosty i stabilny test bez MockBukkit.
        return new CraftingRecipe(id, "name", pattern, output, "*");
    }

    private static ItemStack[][] patternOf(ItemStack item) {
        return new ItemStack[][] { new ItemStack[] { item } };
    }
}

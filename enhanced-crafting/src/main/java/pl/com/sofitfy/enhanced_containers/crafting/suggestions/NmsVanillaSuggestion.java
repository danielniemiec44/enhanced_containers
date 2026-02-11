package pl.com.sofitfy.enhanced_containers.crafting.suggestions;

import org.bukkit.inventory.ItemStack;

/**
 * Jedna vanilla podpowiedź policzona przez NMS.
 *
 * Zawiera:
 * - id receptury (resource location jako string)
 * - ikonę wyniku (Bukkit ItemStack)
 * - macierz 3x3 (9 elementów) z wybranymi składnikami do auto-układania
 */
public final class NmsVanillaSuggestion {

    private final String recipeId;
    private final ItemStack output;
    private final ItemStack[] matrix3x3;

    public NmsVanillaSuggestion(String recipeId, ItemStack output, ItemStack[] matrix3x3) {
        this.recipeId = recipeId;
        this.output = output;
        this.matrix3x3 = matrix3x3;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public ItemStack getOutput() {
        return output == null ? null : output.clone();
    }

    public ItemStack[] getMatrix3x3() {
        if (matrix3x3 == null) {
            return null;
        }
        ItemStack[] copy = new ItemStack[matrix3x3.length];
        for (int i = 0; i < matrix3x3.length; i++) {
            ItemStack it = matrix3x3[i];
            copy[i] = it == null ? null : it.clone();
        }
        return copy;
    }
}

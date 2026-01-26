package pl.com.sofitfy.enhanced_containers.crafting.suggestions;

import org.bukkit.inventory.ItemStack;

/**
 * Wewnętrzna reprezentacja podpowiedzi dla naszego GUI.
 *
 * Dwa typy:
 * - CUSTOM: receptury z naszej mechaniki (CraftingRecipe)
 * - VANILLA_NMS: vanilla policzona przez NMS (z matrycą 3x3 do auto-układania)
 */
public final class CraftingSuggestion {

    public enum Kind {
        CUSTOM,
        VANILLA_NMS
    }

    private final Kind kind;
    private final String id;
    private final ItemStack icon;
    private final ItemStack[] vanillaMatrix3x3;

    private CraftingSuggestion(Kind kind, String id, ItemStack icon, ItemStack[] vanillaMatrix3x3) {
        this.kind = kind;
        this.id = id;
        this.icon = icon;
        this.vanillaMatrix3x3 = vanillaMatrix3x3;
    }

    public static CraftingSuggestion custom(String recipeId, ItemStack icon) {
        return new CraftingSuggestion(Kind.CUSTOM, recipeId, icon, null);
    }

    public static CraftingSuggestion vanillaNms(String recipeId, ItemStack icon, ItemStack[] matrix3x3) {
        return new CraftingSuggestion(Kind.VANILLA_NMS, recipeId, icon, matrix3x3);
    }

    public Kind getKind() {
        return kind;
    }

    public String getId() {
        return id;
    }

    public ItemStack getIcon() {
        return icon == null ? null : icon.clone();
    }

    public ItemStack[] getVanillaMatrix3x3() {
        if (vanillaMatrix3x3 == null) {
            return null;
        }
        ItemStack[] copy = new ItemStack[vanillaMatrix3x3.length];
        for (int i = 0; i < vanillaMatrix3x3.length; i++) {
            ItemStack it = vanillaMatrix3x3[i];
            copy[i] = it == null ? null : it.clone();
        }
        return copy;
    }
}

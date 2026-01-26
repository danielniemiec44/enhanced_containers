package pl.com.sofitfy.enhanced_containers.crafting.suggestions;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.CraftingRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipePattern;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.VanillaRecipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Buduje "podpowiedzi" craftingu na podstawie itemów w ekwipunku gracza.
 *
 * Cel: zachowanie jak książka receptur (tylko pokazuje co da się zrobić).
 *
 * Prosto i bezpiecznie:
 * - NIE zmienia gridu
 * - NIE daje żadnych itemów
 * - tylko sprawdza czy z ekwipunku da się zrobić 1 sztukę receptury
 */
public final class RecipeSuggestionEngine {

    private RecipeSuggestionEngine() {
    }

    /**
     * Zwraca listę outputów receptur, które da się zrobić z ekwipunku.
     *
     * @param inventoryContents zawartość ekwipunku (player.getInventory().getContents())
     * @param customRecipes custom receptury 4x4
     * @param vanillaRecipes vanilla receptury (3x3)
     * @param limit max liczba podpowiedzi
     * @param itemMatches jak porównać ingredient vs item z ekwipunku (zwykle ItemStack::isSimilar)
     */
    public static List<ItemStack> computeSuggestions(
        ItemStack[] inventoryContents,
        List<CraftingRecipe> customRecipes,
        List<VanillaRecipe> vanillaRecipes,
        int limit,
        BiPredicate<ItemStack, ItemStack> itemMatches
    ) {
        if (limit <= 0) {
            return List.of();
        }
        if (inventoryContents == null || inventoryContents.length == 0) {
            return List.of();
        }
        if (itemMatches == null) {
            itemMatches = RecipeSuggestionEngine::defaultMatches;
        }

        List<ItemStack> result = new ArrayList<>();
        // Proste dedupe: nie chcemy 10x tej samej ikonki.
        Set<Integer> seenSignatures = new HashSet<>();

        // Najpierw custom (ważniejsze dla naszego GUI 4x4).
        if (customRecipes != null) {
            for (CraftingRecipe recipe : customRecipes) {
                if (recipe == null) {
                    continue;
                }

                ItemStack output = safeClone(recipe.getOutput());
                if (isAir(output)) {
                    continue;
                }

                int sig = signature(output);
                if (seenSignatures.contains(sig)) {
                    continue;
                }

                List<ItemStack> required = requiredItemsForCustomRecipe(recipe);
                if (required.isEmpty()) {
                    continue;
                }

                if (!canCraftOnce(inventoryContents, required, itemMatches)) {
                    continue;
                }

                result.add(output);
                seenSignatures.add(sig);
                if (result.size() >= limit) {
                    return result;
                }
            }
        }

        // Potem vanilla.
        if (vanillaRecipes != null) {
            for (VanillaRecipe recipe : vanillaRecipes) {
                if (recipe == null) {
                    continue;
                }

                ItemStack output = safeClone(recipe.getOutput());
                if (isAir(output)) {
                    continue;
                }

                int sig = signature(output);
                if (seenSignatures.contains(sig)) {
                    continue;
                }

                List<ItemStack> required = requiredItemsForVanillaRecipe(recipe);
                if (required.isEmpty()) {
                    continue;
                }

                if (!canCraftOnce(inventoryContents, required, itemMatches)) {
                    continue;
                }

                result.add(output);
                seenSignatures.add(sig);
                if (result.size() >= limit) {
                    return result;
                }
            }
        }

        return result;
    }

    /**
     * Sprawdza czy da się zrobić recepturę 1 raz z itemów w inventory.
     * Obsługuje sumowanie z wielu stacków.
     */
    public static boolean canCraftOnce(
        ItemStack[] inventoryContents,
        List<ItemStack> required,
        BiPredicate<ItemStack, ItemStack> itemMatches
    ) {
        if (required == null || required.isEmpty()) {
            return false;
        }
        if (inventoryContents == null || inventoryContents.length == 0) {
            return false;
        }
        if (itemMatches == null) {
            itemMatches = RecipeSuggestionEngine::defaultMatches;
        }

        // Robimy kopię liczników, żeby "zużycie" jednego składnika nie liczyło się drugi raz.
        List<MutableStack> available = new ArrayList<>();
        for (ItemStack stack : inventoryContents) {
            if (isAir(stack)) {
                continue;
            }
            int amount = Math.max(0, stack.getAmount());
            if (amount <= 0) {
                continue;
            }
            available.add(new MutableStack(stack, amount));
        }

        for (ItemStack ingredient : required) {
            if (isAir(ingredient)) {
                continue;
            }

            int needed = Math.max(1, ingredient.getAmount());
            int remainingNeed = needed;

            for (MutableStack have : available) {
                if (have.remaining <= 0) {
                    continue;
                }
                if (!itemMatches.test(have.stack, ingredient)) {
                    continue;
                }

                int take = Math.min(have.remaining, remainingNeed);
                have.remaining -= take;
                remainingNeed -= take;

                if (remainingNeed <= 0) {
                    break;
                }
            }

            if (remainingNeed > 0) {
                return false;
            }
        }

        return true;
    }

    private static List<ItemStack> requiredItemsForCustomRecipe(CraftingRecipe recipe) {
        RecipePattern pattern = recipe.getRecipePattern();
        if (pattern == null || pattern.isEmpty()) {
            return List.of();
        }

        ItemStack[][] p = pattern.getPattern();
        return requiredItemsFromPattern(p);
    }

    private static List<ItemStack> requiredItemsForVanillaRecipe(VanillaRecipe recipe) {
        if (recipe.isShapeless()) {
            List<ItemStack> ing = recipe.getShapelessIngredients();
            if (ing == null || ing.isEmpty()) {
                return List.of();
            }
            List<ItemStack> result = new ArrayList<>();
            for (ItemStack i : ing) {
                if (!isAir(i)) {
                    result.add(i);
                }
            }
            return result;
        }

        ItemStack[][] pattern = recipe.getPattern();
        return requiredItemsFromPattern(pattern);
    }

    private static List<ItemStack> requiredItemsFromPattern(ItemStack[][] pattern) {
        if (pattern == null || pattern.length == 0) {
            return List.of();
        }

        List<ItemStack> required = new ArrayList<>();
        for (ItemStack[] row : pattern) {
            if (row == null) {
                continue;
            }
            for (ItemStack item : row) {
                if (isAir(item)) {
                    continue;
                }
                required.add(item);
            }
        }
        return required;
    }

    private static boolean isAir(ItemStack item) {
        if (item == null) {
            return true;
        }
        Material type = item.getType();
        return type == null || type.isAir();
    }

    private static boolean defaultMatches(ItemStack a, ItemStack b) {
        if (a == null || b == null) {
            return false;
        }
        return a.isSimilar(b);
    }

    private static ItemStack safeClone(ItemStack item) {
        if (item == null) {
            return null;
        }
        try {
            return item.clone();
        } catch (Exception e) {
            return item;
        }
    }

    private static int signature(ItemStack item) {
        // Dedupe tylko na podstawie "podobieństwa" uproszczonego.
        // To ma być tylko UI; nie wymaga kryptograficznej unikatowości.
        if (item == null) {
            return 0;
        }
        int typeHash = (item.getType() != null) ? item.getType().hashCode() : 0;
        int metaHash = 0;
        try {
            if (item.hasItemMeta() && item.getItemMeta() != null) {
                metaHash = item.getItemMeta().hashCode();
            }
        } catch (Exception ignored) {
        }
        return 31 * typeHash + metaHash;
    }

    private static final class MutableStack {
        private final ItemStack stack;
        private int remaining;

        private MutableStack(ItemStack stack, int remaining) {
            this.stack = stack;
            this.remaining = remaining;
        }
    }
}

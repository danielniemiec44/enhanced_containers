package pl.com.sofitfy.enhanced_containers.crafting.suggestions;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.List;

/**
 * Prawdziwe NMS podpowiedzi vanilla dla Paper 1.21.11.
 *
 * Zasada: chcemy tylko "pierwsze" craftowalne rzeczy z ekwipunku.
 * - iterujemy po NMS recepturach craftingu
 * - próbujemy zbudować prostą matrycę 3x3 z itemów z ekwipunku
 * - jeśli recipe.matches(...) == true, to bierzemy assemble(...) jako ikonę
 *
 * To jest odpowiednik zachowania "recipe book":
 * pokazuj tylko to, co da się zrobić i daj plan ułożenia.
 */
public class NmsVanillaSuggestionEngineDirect {

    public List<NmsVanillaSuggestion> computeSuggestions(ItemStack[] inventoryContents, World world, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        if (inventoryContents == null || world == null) {
            return List.of();
        }
        if (!(world instanceof CraftWorld craftWorld)) {
            // MockBukkit / testy: brak NMS level.
            return List.of();
        }

        ServerLevel level = craftWorld.getHandle();

        // Zamiast polegać na niestabilnych API RecipeManager (różne mappings),
        // enumerujemy Bukkitowe receptury craftingu i weryfikujemy je NMS-em (getRecipeFor + assemble).
        List<NmsVanillaSuggestion> result = new ArrayList<>();

        for (var it = Bukkit.recipeIterator(); it.hasNext(); ) {
            if (result.size() >= limit) {
                break;
            }

            Recipe r = it.next();
            if (r == null) {
                continue;
            }

            NamespacedKey key = null;
            if (r instanceof ShapedRecipe sr) {
                key = sr.getKey();
            } else if (r instanceof ShapelessRecipe slr) {
                key = slr.getKey();
            }

            // Tylko vanilla (minecraft:*). Customy obsługujemy osobno w pluginie.
            if (key == null || !"minecraft".equalsIgnoreCase(key.getNamespace())) {
                continue;
            }

            ItemStack[] matrixBukkit = tryBuildMatrix3x3ForBukkitRecipe(r, inventoryContents);
            if (matrixBukkit == null) {
                continue;
            }

            // NMS weryfikacja + wynik jak w vanilla.
            CraftingContainer container = create3x3Container();
            for (int i = 0; i < 9; i++) {
                ItemStack bukkit = matrixBukkit[i];
                net.minecraft.world.item.ItemStack nms = (bukkit == null || bukkit.getType().isAir())
                    ? net.minecraft.world.item.ItemStack.EMPTY
                    : CraftItemStack.asNMSCopy(bukkit);
                container.setItem(i, nms);
            }

            var found = level.getServer().getRecipeManager().getRecipeFor(
                RecipeType.CRAFTING,
                container.asCraftInput(),
                level
            );
            if (found.isEmpty()) {
                continue;
            }

            net.minecraft.world.item.ItemStack nmsOut;
            try {
                nmsOut = found.get().value().assemble(container.asCraftInput(), level.registryAccess());
            } catch (Throwable t) {
                nmsOut = net.minecraft.world.item.ItemStack.EMPTY;
            }

            ItemStack icon = CraftItemStack.asBukkitCopy(nmsOut);
            if (icon == null || icon.getType().isAir() || icon.getAmount() <= 0) {
                continue;
            }

            ItemStack iconOne = icon.clone();
            iconOne.setAmount(1);
            result.add(new NmsVanillaSuggestion(key.toString(), iconOne, matrixBukkit));
        }

        return result;
    }

    private ItemStack[] tryBuildMatrix3x3ForBukkitRecipe(Recipe recipe, ItemStack[] inventoryContents) {
        if (recipe instanceof ShapedRecipe shaped) {
            return tryBuildMatrix3x3ForShaped(shaped, inventoryContents);
        }
        if (recipe instanceof ShapelessRecipe shapeless) {
            return tryBuildMatrix3x3ForShapeless(shapeless, inventoryContents);
        }
        return null;
    }

    private ItemStack[] tryBuildMatrix3x3ForShaped(ShapedRecipe shaped, ItemStack[] inventoryContents) {
        String[] shape = shaped.getShape();
        if (shape == null || shape.length <= 0 || shape.length > 3) {
            return null;
        }

        int h = shape.length;
        int w = 0;
        for (String row : shape) {
            if (row == null) {
                return null;
            }
            w = Math.max(w, row.length());
        }
        if (w <= 0 || w > 3) {
            return null;
        }

        List<BukkitAvailableStack> available = buildAvailableBukkit(inventoryContents);
        ItemStack[] matrix = new ItemStack[9];

        var map = shaped.getChoiceMap();
        for (int y = 0; y < h; y++) {
            String row = shape[y];
            for (int x = 0; x < w; x++) {
                char c = x < row.length() ? row.charAt(x) : ' ';
                if (c == ' ') {
                    continue;
                }

                RecipeChoice choice = map.get(c);
                if (choice == null) {
                    return null;
                }

                ItemStack chosen = takeFirstMatchingBukkit(available, choice);
                if (chosen == null) {
                    return null;
                }

                ItemStack one = chosen.clone();
                one.setAmount(1);
                matrix[y * 3 + x] = one;
            }
        }

        return matrix;
    }

    private ItemStack[] tryBuildMatrix3x3ForShapeless(ShapelessRecipe shapeless, ItemStack[] inventoryContents) {
        List<RecipeChoice> list = shapeless.getChoiceList();
        if (list == null || list.isEmpty()) {
            return null;
        }

        List<BukkitAvailableStack> available = buildAvailableBukkit(inventoryContents);
        ItemStack[] matrix = new ItemStack[9];

        int pos = 0;
        for (RecipeChoice choice : list) {
            if (pos >= 9) {
                return null;
            }
            if (choice == null) {
                return null;
            }

            ItemStack chosen = takeFirstMatchingBukkit(available, choice);
            if (chosen == null) {
                return null;
            }

            ItemStack one = chosen.clone();
            one.setAmount(1);
            matrix[pos] = one;
            pos++;
        }

        return matrix;
    }

    private static final class BukkitAvailableStack {
        private final ItemStack stack;
        private int remaining;

        private BukkitAvailableStack(ItemStack stack, int remaining) {
            this.stack = stack;
            this.remaining = remaining;
        }
    }

    private List<BukkitAvailableStack> buildAvailableBukkit(ItemStack[] inventoryContents) {
        List<BukkitAvailableStack> out = new ArrayList<>();
        for (ItemStack bukkit : inventoryContents) {
            if (bukkit == null || bukkit.getType().isAir() || bukkit.getAmount() <= 0) {
                continue;
            }
            out.add(new BukkitAvailableStack(bukkit, bukkit.getAmount()));
        }
        return out;
    }

    private ItemStack takeFirstMatchingBukkit(List<BukkitAvailableStack> available, RecipeChoice choice) {
        for (BukkitAvailableStack a : available) {
            if (a.remaining <= 0) {
                continue;
            }
            if (a.stack == null || a.stack.getType().isAir()) {
                continue;
            }

            boolean ok;
            try {
                ok = choice.test(a.stack);
            } catch (Throwable t) {
                ok = false;
            }

            if (!ok) {
                continue;
            }

            a.remaining -= 1;
            return a.stack;
        }
        return null;
    }

    private ItemStack[] tryBuildMatrix3x3ForRecipe(net.minecraft.world.item.crafting.CraftingRecipe recipe, List<AvailableStack> available) {
        List<Ingredient> ingredients = getIngredientsReflective(recipe);
        if (ingredients == null || ingredients.isEmpty()) {
            return null;
        }

        ItemStack[] matrix = new ItemStack[9];

        // Shaped: chcemy zachować układ.
        if (recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped) {
            Integer wObj = tryCallInt(shaped, "getWidth");
            Integer hObj = tryCallInt(shaped, "getHeight");
            int w = wObj == null ? -1 : wObj;
            int h = hObj == null ? -1 : hObj;
            if (w <= 0 || h <= 0 || w > 3 || h > 3) {
                return null;
            }

            // ingredients w shaped są w układzie w*h.
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int idx = y * w + x;
                    if (idx < 0 || idx >= ingredients.size()) {
                        return null;
                    }

                    Ingredient ing = ingredients.get(idx);
                    if (ing == null || ing.isEmpty()) {
                        continue;
                    }

                    net.minecraft.world.item.ItemStack chosen = takeFirstMatching(available, ing);
                    if (chosen == null) {
                        return null;
                    }

                    matrix[y * 3 + x] = CraftItemStack.asBukkitCopy(chosen);
                    if (matrix[y * 3 + x] != null) {
                        matrix[y * 3 + x].setAmount(1);
                    }
                }
            }

            return matrix;
        }

        // Shapeless i inne: wkładamy od lewej do prawej.
        int pos = 0;
        for (Ingredient ing : ingredients) {
            if (pos >= 9) {
                return null;
            }
            if (ing == null || ing.isEmpty()) {
                continue;
            }

            net.minecraft.world.item.ItemStack chosen = takeFirstMatching(available, ing);
            if (chosen == null) {
                return null;
            }

            matrix[pos] = CraftItemStack.asBukkitCopy(chosen);
            if (matrix[pos] != null) {
                matrix[pos].setAmount(1);
            }
            pos++;
        }

        return matrix;
    }

    private net.minecraft.world.item.ItemStack takeFirstMatching(List<AvailableStack> available, Ingredient ing) {
        for (AvailableStack a : available) {
            if (a.remaining <= 0) {
                continue;
            }
            if (a.stack == null || a.stack.isEmpty()) {
                continue;
            }

            boolean ok;
            try {
                ok = ing.test(a.stack);
            } catch (Throwable t) {
                ok = false;
            }

            if (!ok) {
                continue;
            }

            a.remaining -= 1;
            return a.stack;
        }

        return null;
    }

    private List<AvailableStack> buildAvailable(ItemStack[] inventoryContents) {
        List<AvailableStack> out = new ArrayList<>();
        for (ItemStack bukkit : inventoryContents) {
            if (bukkit == null || bukkit.getType().isAir() || bukkit.getAmount() <= 0) {
                continue;
            }

            net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(bukkit);
            if (nms == null || nms.isEmpty()) {
                continue;
            }

            out.add(new AvailableStack(nms, bukkit.getAmount()));
        }
        return out;
    }

    private List<AvailableStack> cloneAvailable(List<AvailableStack> base) {
        List<AvailableStack> out = new ArrayList<>(base.size());
        for (AvailableStack a : base) {
            out.add(new AvailableStack(a.stack, a.remaining));
        }
        return out;
    }

    private CraftingContainer create3x3Container() {
        AbstractContainerMenu dummy = new AbstractContainerMenu(null, -1) {
            @Override
            public InventoryView getBukkitView() {
                return null;
            }

            @Override
            public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                return false;
            }

            @Override
            public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slot) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
        };

        return new TransientCraftingContainer(dummy, 3, 3);
    }

    private static final class AvailableStack {
        private final net.minecraft.world.item.ItemStack stack;
        private int remaining;

        private AvailableStack(net.minecraft.world.item.ItemStack stack, int remaining) {
            this.stack = stack;
            this.remaining = remaining;
        }
    }

    @SuppressWarnings("unchecked")
    private List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> getAllCraftingRecipes(ServerLevel level) {
        Object server;
        try {
            server = level.getServer();
        } catch (Throwable t) {
            return List.of();
        }
        if (server == null) {
            return List.of();
        }

        Object recipeManager;
        try {
            Method m = server.getClass().getMethod("getRecipeManager");
            recipeManager = m.invoke(server);
        } catch (Throwable t) {
            return List.of();
        }
        if (recipeManager == null) {
            return List.of();
        }

        // W różnych wersjach/mappings metoda może mieć inną nazwę.
        // 1) Spróbuj metod z RecipeType.
        Object out = tryInvoke(recipeManager, new String[] {
            "getAllRecipesFor",
            "getRecipesFor",
            "byType",
            "recipesByType"
        }, new Class<?>[] {RecipeType.class}, new Object[] {RecipeType.CRAFTING});
        List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> fromTyped = coerceToCraftingRecipeHolderList(out);
        if (!fromTyped.isEmpty()) {
            return fromTyped;
        }

        // 2) Fallback: pobierz wszystkie przepisy i odfiltruj te od craftingu.
        Object outAll = tryInvoke(recipeManager, new String[] {"getRecipes", "getAllRecipes"}, new Class<?>[] {}, new Object[] {});
        List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> fromAll = coerceToCraftingRecipeHolderList(outAll);
        if (!fromAll.isEmpty()) {
            return fromAll;
        }

        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> coerceToCraftingRecipeHolderList(Object out) {
        if (out == null) {
            return List.of();
        }

        List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> result = new ArrayList<>();

        // Najłatwiejszy przypadek: już jest List.
        if (out instanceof List<?> list) {
            for (Object o : list) {
                addIfCraftingHolder(result, o);
            }
            return result;
        }

        // Czasem jest Collection/Iterable.
        if (out instanceof Iterable<?> iterable) {
            for (Object o : iterable) {
                addIfCraftingHolder(result, o);
            }
            return result;
        }

        // Czasem jest Map (np. id -> RecipeHolder).
        if (out instanceof Map<?, ?> map) {
            for (Object v : map.values()) {
                addIfCraftingHolder(result, v);
            }
            return result;
        }

        // Czasem byType(...) zwraca Map<RecipeType, Map<...>> lub podobne.
        if (out instanceof Collection<?> col) {
            for (Object v : col) {
                addIfCraftingHolder(result, v);
            }
            return result;
        }

        return List.of();
    }

    @SuppressWarnings("unchecked")
    private void addIfCraftingHolder(List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> out, Object maybeHolder) {
        if (!(maybeHolder instanceof RecipeHolder<?> holder)) {
            return;
        }
        Object value;
        try {
            value = holder.value();
        } catch (Throwable t) {
            return;
        }
        if (!(value instanceof net.minecraft.world.item.crafting.CraftingRecipe)) {
            return;
        }
        out.add((RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>) holder);
    }

    @SuppressWarnings("unchecked")
    private List<Ingredient> getIngredientsReflective(Object recipe) {
        if (recipe == null) {
            return null;
        }

        // Najczęściej jest metoda getIngredients() bez parametrów.
        Object out = tryInvoke(recipe, new String[] {"getIngredients", "ingredients"}, new Class<?>[] {}, new Object[] {});
        if (out instanceof List<?> list) {
            return (List<Ingredient>) list;
        }

        // Fallback: szukamy publicznej metody bez parametrów, której nazwa zawiera "ingredient".
        try {
            for (Method m : recipe.getClass().getMethods()) {
                if (m.getParameterCount() != 0) {
                    continue;
                }
                String name = m.getName().toLowerCase();
                if (!name.contains("ingredient")) {
                    continue;
                }
                Object v = m.invoke(recipe);
                if (v instanceof List<?> list2) {
                    return (List<Ingredient>) list2;
                }
            }
        } catch (Throwable ignored) {
        }

        // Ostateczny fallback: szukamy pola, które wygląda jak lista Ingredientów.
        // W niektórych wersjach/metodach nie ma publicznego getIngredients().
        try {
            Class<?> c = recipe.getClass();
            while (c != null && c != Object.class) {
                for (var f : c.getDeclaredFields()) {
                    if (!List.class.isAssignableFrom(f.getType())) {
                        continue;
                    }

                    f.setAccessible(true);
                    Object v = f.get(recipe);
                    if (!(v instanceof List<?> list3)) {
                        continue;
                    }

                    boolean ok = true;
                    for (Object e : list3) {
                        if (e == null) {
                            continue;
                        }
                        if (!(e instanceof Ingredient)) {
                            ok = false;
                            break;
                        }
                    }

                    if (ok) {
                        return (List<Ingredient>) list3;
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private Integer tryCallInt(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            return (v instanceof Integer i) ? i : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private Object tryInvoke(Object target, String[] methodNames, Class<?>[] paramTypes, Object[] args) {
        if (target == null) {
            return null;
        }
        if (methodNames == null) {
            return null;
        }
        for (String name : methodNames) {
            if (name == null || name.isBlank()) {
                continue;
            }
            try {
                Method m = target.getClass().getMethod(name, paramTypes);
                return m.invoke(target, args);
            } catch (Throwable ignored) {
            }
        }

        // Dodatkowy fallback: spróbuj znaleźć metodę po nazwie bez weryfikacji sygnatury.
        try {
            for (Method m : target.getClass().getMethods()) {
                if (!Arrays.asList(methodNames).contains(m.getName())) {
                    continue;
                }
                if (m.getParameterCount() != args.length) {
                    continue;
                }
                return m.invoke(target, args);
            }
        } catch (Throwable ignored) {
        }

        return null;
    }
}

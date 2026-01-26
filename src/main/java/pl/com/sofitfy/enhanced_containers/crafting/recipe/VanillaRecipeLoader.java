package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ładuje vanilla receptury z Minecrafta (crafting table) do naszego systemu.
 *
 * Obsługiwane typy:
 * - ShapedRecipe (np. crafting table, stół zaklęć) - mają konkretny kształt
 * - ShapelessRecipe (np. farbniki) - mogą być w dowolnym porządku
 *
 * Konwersja:
 * - ShapedRecipe -> ItemStack[][] (2D tablica)
 * - ShapelessRecipe -> seznam składników
 */
public class VanillaRecipeLoader {

    /**
     * Załaduj wszystkie vanilla receptury z serwera.
     *
     * @return lista vanilla receptur w formacie naszego systemu
     */
    public static List<VanillaRecipe> loadAllVanillaRecipes() {
        List<VanillaRecipe> vanillaRecipes = new ArrayList<>();

        // Iterujemy po wszystkich recepturach zarejestrowanych na serwerze
        for (Recipe recipe : Bukkit.getRecipesFor(new ItemStack(org.bukkit.Material.DIAMOND))) {
            // Przeszukujemy wszystkie receptury - to będzie drogi sposób ale bezpieczny
        }

        // Lepiej: iterujemy bezpośrednio po wszystkich recepturach
        java.util.Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();

            if (recipe instanceof ShapedRecipe) {
                VanillaRecipe vanillaRecipe = convertShapedRecipe((ShapedRecipe) recipe);
                if (vanillaRecipe != null) {
                    vanillaRecipes.add(vanillaRecipe);
                }
            } else if (recipe instanceof ShapelessRecipe) {
                VanillaRecipe vanillaRecipe = convertShapelessRecipe((ShapelessRecipe) recipe);
                if (vanillaRecipe != null) {
                    vanillaRecipes.add(vanillaRecipe);
                }
            }
        }

        return vanillaRecipes;
    }

    /**
     * Konwertuj ShapedRecipe na nasz format.
     *
     * ShapedRecipe ma konkretny kształt (np. 3x3, 2x2).
     * Konwertujemy go na ItemStack[][] gdzie każda komórka to element receptury.
     *
     * @param shapedRecipe receptura ze zdefiniowanym kształtem
     * @return vanilla receptura w naszym formacie
     */
    private static VanillaRecipe convertShapedRecipe(ShapedRecipe shapedRecipe) {
        try {
            // Pobierz output (wynik craftingu)
            ItemStack output = shapedRecipe.getResult();
            if (output == null) {
                return null;
            }

            // Pobierz rozmiary receptury (szerokość i wysokość)
            String[] shape = shapedRecipe.getShape();
            if (shape == null || shape.length == 0) {
                return null;
            }

            int height = shape.length;
            int width = shape[0].length();

            // Pobierz mapowanie liter na ItemStack'i
            Map<Character, RecipeChoice> choiceMap = shapedRecipe.getChoiceMap();
            if (choiceMap == null) {
                return null;
            }

            // Buduj wzór receptury (2D tablica)
            ItemStack[][] pattern = new ItemStack[height][width];
            for (int row = 0; row < height; row++) {
                String line = shape[row];
                for (int col = 0; col < width; col++) {
                    if (col >= line.length()) {
                        // Brak znaku = puste pole
                        pattern[row][col] = null;
                        continue;
                    }

                    char key = line.charAt(col);

                    // Spacja oznacza puste pole
                    if (key == ' ') {
                        pattern[row][col] = null;
                        continue;
                    }

                    // Pobierz RecipeChoice dla tego znaku
                    RecipeChoice choice = choiceMap.get(key);
                    if (choice == null) {
                        pattern[row][col] = null;
                        continue;
                    }

                    // Konwertuj RecipeChoice na ItemStack
                    // RecipeChoice może być MaterialChoice lub ExactChoice
                    ItemStack item = recipeChoiceToItemStack(choice);
                    pattern[row][col] = item;
                }
            }

            // Stwórz wewnętrzną reprezentację vanilla receptury
            // Używamy generowanego ID aby uniknąć konfliktu z custom recepturami
            String id = "vanilla_" + System.identityHashCode(shapedRecipe);
            String name = output.getType().toString();

            return new VanillaRecipe(id, name, pattern, output.clone(), false);
        } catch (Exception e) {
            // Ignoruj błędy - niektóre vanilla receptury mogą być niespodziewane
            return null;
        }
    }

    /**
     * Konwertuj ShapelessRecipe na nasz format.
     *
     * ShapelessRecipe nie ma konkretnego kształtu - to po prostu lista składników.
     * Przechowujemy ją jako flaga ShapelessRecipe.
     *
     * @param shapelessRecipe receptura bez konkretnego kształtu
     * @return vanilla receptura w naszym formacie (flagą shapeless)
     */
    private static VanillaRecipe convertShapelessRecipe(ShapelessRecipe shapelessRecipe) {
        try {
            // Pobierz output
            ItemStack output = shapelessRecipe.getResult();
            if (output == null) {
                return null;
            }

            // Pobierz listę składników
            List<RecipeChoice> ingredients = shapelessRecipe.getChoiceList();
            if (ingredients == null || ingredients.isEmpty()) {
                return null;
            }

            // Konwertuj składniki na ItemStack'i
            List<ItemStack> ingredientItems = new ArrayList<>();
            for (RecipeChoice choice : ingredients) {
                ItemStack item = recipeChoiceToItemStack(choice);
                if (item != null) {
                    ingredientItems.add(item);
                }
            }

            if (ingredientItems.isEmpty()) {
                return null;
            }

            // Dla ShapelessRecipe nie mamy wzoru 2D - to jest przechowywane inaczej
            // Pattern będzie null, ale będziemy mieć ingredientsList
            String id = "vanilla_shapeless_" + System.identityHashCode(shapelessRecipe);
            String name = output.getType().toString();

            VanillaRecipe vanillaRecipe = new VanillaRecipe(id, name, null, output.clone(), true);
            vanillaRecipe.setShapelessIngredients(ingredientItems);

            return vanillaRecipe;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Konwertuj RecipeChoice na ItemStack.
     *
     * RecipeChoice może być:
     * - MaterialChoice - lista materiałów
     * - ExactChoice - lista dokładnych ItemStack'ów
     *
     * @param choice wybór elementu receptury
     * @return ItemStack reprezentujący ten wybór
     */
    private static ItemStack recipeChoiceToItemStack(RecipeChoice choice) {
        if (choice == null) {
            return null;
        }

        try {
            // Dla MaterialChoice - weź pierwszy material
            if (choice instanceof org.bukkit.inventory.RecipeChoice.MaterialChoice) {
                org.bukkit.inventory.RecipeChoice.MaterialChoice materialChoice =
                    (org.bukkit.inventory.RecipeChoice.MaterialChoice) choice;

                // Pobierz pierwsze dostępne materiały
                java.util.List<org.bukkit.Material> materials = materialChoice.getChoices();
                if (!materials.isEmpty()) {
                    return new ItemStack(materials.get(0));
                }
            }
            // Dla ExactChoice - weź pierwszy ItemStack
            else if (choice instanceof org.bukkit.inventory.RecipeChoice.ExactChoice) {
                org.bukkit.inventory.RecipeChoice.ExactChoice exactChoice =
                    (org.bukkit.inventory.RecipeChoice.ExactChoice) choice;

                java.util.List<ItemStack> choices = exactChoice.getChoices();
                if (!choices.isEmpty()) {
                    return choices.get(0).clone();
                }
            }
        } catch (Exception e) {
            // Ignoruj błędy
        }

        return null;
    }
}

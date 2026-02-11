package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipeLoader {
    private final FileConfiguration config;

    public RecipeLoader(FileConfiguration config) {
        this.config = config;
    }

    public List<CraftingRecipe> loadRecipes() {
        List<CraftingRecipe> recipes = new ArrayList<>();
        List<?> recipeList = config.getList("crafting.recipes");

        if (recipeList == null) {
            return recipes;
        }

        for (Object obj : recipeList) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> recipeMap = (Map<String, Object>) obj;
                String id = (String) recipeMap.get("id");

                if (id != null) {
                    CraftingRecipe recipe = CraftingRecipe.fromConfig(id, recipeMap);
                    if (recipe != null) {
                        recipes.add(recipe);
                    }
                }
            }
        }

        return recipes;
    }
}


package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Reprezentacja vanilla receptury (crafting table).
 *
 * Obsługuje dwa typy:
 * - ShapedRecipe - ma konkretny kształt (2D tablica)
 * - ShapelessRecipe - brak konkretnego kształtu (lista składników)
 */
public class VanillaRecipe {
    private final String id;
    private final String name;
    private final ItemStack[][] pattern;  // null dla ShapelessRecipe
    private final ItemStack output;
    private final boolean isShapeless;

    // Dla ShapelessRecipe - lista składników
    private List<ItemStack> shapelessIngredients;

    /**
     * Stwórz vanilla recepturę.
     *
     * @param id identyfikator receptury (wewnętrzny)
     * @param name nazwa receptury
     * @param pattern wzór receptury (2D tablica) - null dla shapeless
     * @param output wynik craftingu
     * @param isShapeless czy to receptura bez kształtu
     */
    public VanillaRecipe(String id, String name, ItemStack[][] pattern, ItemStack output, boolean isShapeless) {
        this.id = id;
        this.name = name;
        this.pattern = pattern;
        this.output = output;
        this.isShapeless = isShapeless;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ItemStack[][] getPattern() {
        return pattern;
    }

    public ItemStack getOutput() {
        return output.clone();
    }

    public boolean isShapeless() {
        return isShapeless;
    }

    public List<ItemStack> getShapelessIngredients() {
        return shapelessIngredients;
    }

    public void setShapelessIngredients(List<ItemStack> ingredients) {
        this.shapelessIngredients = ingredients;
    }
}

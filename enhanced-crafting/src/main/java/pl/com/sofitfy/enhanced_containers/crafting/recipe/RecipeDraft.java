package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import org.bukkit.inventory.ItemStack;

/**
 * RecipeDraft - tymczasowa receptura podczas edycji
 *
 * Klasa przechowuje dane edycji receptury przez admina.
 * Kiedy admin zamyka edytor - RecipeDraft jest konwertowany na CraftingRecipe
 * i zapisywany do config.yml
 *
 * Używanie: gracz/admin otwiera /4x4 new lub /4x4 edit (alias: /ecraft)
 * System tworzy RecipeDraft, admin go edytuje w GUI,
 * przy zamykaniu (Escape) - RecipeDraft idzie do config.yml
 */
public class RecipeDraft {
    // Identyfikator receptury (wygenerowany automatycznie lub podany)
    private String id;

    // Nazwa receptury (brana z display name outputu lub podana ręcznie)
    private String name;

    // Wzór craftu - grid 4x4 (16 slotów na input)
    private ItemStack[][] inputPattern;

    // Wynik craftu - jeden item z ilością
    private ItemStack output;

    // Uprawnienie wymagane do craftowania (domyślnie: "plethoras.tools.use")
    private String permission;

    /**
     * Konstruktor - tworzy pusty draft receptury
     * @param id - identyfikator receptury (np. "enhanced_crafting_1")
     */
    public RecipeDraft(String id) {
        this.id = id;
        // Tekst w grze ma być po angielsku (nazwa może pojawić się graczowi)
        this.name = "New Recipe";
        this.permission = "plethoras.tools.use";

        // Inicjalizuj pusty grid 4x4
        this.inputPattern = new ItemStack[4][4];

        // output będzie null - dopóki admin go nie ustawi
        this.output = null;
    }

    // ===== GETTERY I SETTERY =====

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ItemStack[][] getInputPattern() {
        return inputPattern;
    }

    public void setInputPattern(ItemStack[][] inputPattern) {
        this.inputPattern = inputPattern;
    }

    public ItemStack getOutput() {
        return output;
    }

    public void setOutput(ItemStack output) {
        this.output = output;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    /**
     * Sprawdź czy draft jest gotowy do zapisu
     * (musi mieć co najmniej 1 input i 1 output)
     *
     * @return true jeśli draft jest pełny i można go zapisać
     */
    public boolean isValid() {
        // Musi być output
        if (output == null || output.getType().isAir()) {
            return false;
        }

        // Musi być co najmniej jeden item w input
        if (inputPattern == null) {
            return false;
        }

        boolean hasInput = false;
        for (ItemStack[] row : inputPattern) {
            if (row != null) {
                for (ItemStack item : row) {
                    if (item != null && !item.getType().isAir()) {
                        hasInput = true;
                        break;
                    }
                }
            }
            if (hasInput) break;
        }

        return hasInput;
    }

    /**
     * Sprawdź czy draft jest całkowicie pusty (anulacja)
     *
     * @return true jeśli nic nie ma w input i output
     */
    public boolean isEmpty() {
        // Brak outputu
        if (output == null || output.getType().isAir()) {
            // Sprawdź czy w input jest coś
            if (inputPattern != null) {
                for (ItemStack[] row : inputPattern) {
                    if (row != null) {
                        for (ItemStack item : row) {
                            if (item != null && !item.getType().isAir()) {
                                return false; // Coś tam jest
                            }
                        }
                    }
                }
            }
            return true; // Nic tam nie ma
        }
        return false; // Output jest, więc nie puste
    }

    /**
     * Konwertuj 4x4 grid inputów na RecipePattern
     * Automatycznie oblicza najmniejszy prostokąt zawierający przedmioty.
     *
     * Na przykład:
     * - Jeśli gracz postawi 2x2 przedmioty - zwróci RecipePattern 2x2
     * - Jeśli postawi 3x3 - zwróci RecipePattern 3x3
     *
     * @return RecipePattern z automatycznie obliczonymi wymiarami
     */
    public RecipePattern getRecipePattern() {
        if (inputPattern == null) {
            return new RecipePattern(new ItemStack[0][0]);
        }
        return new RecipePattern(inputPattern);
    }

    /**
     * Konwertuj RecipeDraft na CraftingRecipe
     * (kiedy admin skończy edycję)
     *
     * @return nowa CraftingRecipe lub null jeśli draft jest nieprawidłowy
     */
    public CraftingRecipe toCraftingRecipe() {
        if (!isValid()) {
            return null;
        }

        return new CraftingRecipe(
            this.id,
            this.name,
            this.inputPattern,
            this.output,
            this.permission
        );
    }

    @Override
    public String toString() {
        return "RecipeDraft{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", permission='" + permission + '\'' +
            ", hasOutput=" + (output != null && !output.getType().isAir()) +
            '}';
    }
}


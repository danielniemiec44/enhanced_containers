package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import java.util.*;

/**
 * RecipeManager - menadżer receptur na serwerze
 *
 * To jest singleton - tylko jedna instancja na cały serwer
 * Przechowuje wszystkie załadowane receptury i daje dostęp z każdego miejsca
 *
 * Odpowiada za:
 * - Przechowywanie receptur w pamięci
 * - Rejestrowanie nowych receptur
 * - Usuwanie receptur
 * - Wygenerowanie nowych ID dla receptur
 */
public class RecipeManager {
    private static RecipeManager instance;
    // WAŻNE: LinkedHashMap zachowuje kolejność rejestracji.
    // To daje deterministyczne zachowanie gdy kilka custom receptur pasuje naraz.
    private final Map<String, CraftingRecipe> recipes = new LinkedHashMap<>();
    private final List<VanillaRecipe> vanillaRecipes = new ArrayList<>();

    // Licznik do generowania unikatowych ID receptur (enhanced_crafting_1, enhanced_crafting_2 itd)
    private int nextRecipeNumber = 1;

    private RecipeManager() {}

    /**
     * Zwraca jedyną instancję RecipeManager'a
     */
    public static RecipeManager getInstance() {
        if (instance == null) {
            instance = new RecipeManager();
        }
        return instance;
    }

    /**
     * Dodaj recepturę do menadżera (przechowuje pod ID)
     */
    public void registerRecipe(CraftingRecipe recipe) {
        if (recipe != null) {
            recipes.put(recipe.getId(), recipe);
            // Aktualizuj licznik jeśli ID jest nowy
            if (recipe.getId().startsWith("enhanced_crafting_")) {
                try {
                    int num = Integer.parseInt(recipe.getId().replace("enhanced_crafting_", ""));
                    if (num >= nextRecipeNumber) {
                        nextRecipeNumber = num + 1;
                    }
                } catch (NumberFormatException e) {
                    // Ignoruj - ID nie ma formatu liczby
                }
            }
        }
    }

    /**
     * Dodaj wiele receptur naraz
     */
    public void registerRecipes(List<CraftingRecipe> recipeList) {
        if (recipeList != null) {
            recipeList.forEach(this::registerRecipe);
        }
    }

    /**
     * Pobierz recepturę po ID (zwraca Optional bo może nie być)
     */
    public Optional<CraftingRecipe> getRecipe(String id) {
        return Optional.ofNullable(recipes.get(id));
    }

    /**
     * Zwróć wszystkie receptury
     */
    public List<CraftingRecipe> getAllRecipes() {
        return new ArrayList<>(recipes.values());
    }

    /**
     * Wyczyść wszystkie receptury (używane do reload'a)
     */
    public void clearRecipes() {
        recipes.clear();
        nextRecipeNumber = 1;
    }

    /**
     * Usuń konkretną recepturę
     */
    public void unregisterRecipe(String id) {
        recipes.remove(id);
    }

    /**
     * Wygeneruj nowy unikatowy ID dla receptury
     * Format: enhanced_crafting_<liczba>
     *
     * @return nowy ID (np. "enhanced_crafting_5")
     */
    public String generateNewRecipeId() {
        String newId = "enhanced_crafting_" + nextRecipeNumber;
        nextRecipeNumber++;
        return newId;
    }

    /**
     * Załaduj wszystkie vanilla receptury z serwera.
     * Wykonaj tylko raz przy starcie pluginu!
     */
    public void loadVanillaRecipes() {
        vanillaRecipes.clear();
        List<VanillaRecipe> loaded = VanillaRecipeLoader.loadAllVanillaRecipes();
        vanillaRecipes.addAll(loaded);
    }

    /**
     * Zwróć wszystkie vanilla receptury
     */
    public List<VanillaRecipe> getAllVanillaRecipes() {
        return new ArrayList<>(vanillaRecipes);
    }

    /**
     * Wyczyść vanilla receptury
     */
    public void clearVanillaRecipes() {
        vanillaRecipes.clear();
    }
}

package pl.com.sofitfy.enhanced_containers.crafting.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.CraftingRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * RecipeSelectionGUI - lista receptur do edycji
 *
 * Wyświetla wszystkie dostępne receptury na stronie (do 45 na stronę).
 * Admin klika w recepturę aby ją edytować.
 * Na dole są przyciski do nawigacji między stronami.
 *
 * Layout: 54 sloty (6 rzędów × 9 kolumn)
 * - Sloty 0-44: Receptury (5 rzędów × 9 kolumn)
 * - Sloty 45-53: Przyciski nawigacji i informacje (ostatni rząd)
 */
public class RecipeSelectionGUI {
    private final Player player;
    private final Inventory inventory;
    private final int currentPage;
    private final int totalPages;

    // Rozmiar inventoria - ZAWSZE 54 sloty
    public static final int INVENTORY_SIZE = 54;
    
    // Ile receptur na jedną stronę (5 rzędów × 9 kolumn)
    public static final int RECIPES_PER_PAGE = 45;
    
    // Sloty do receptur (0-44)
    public static final int FIRST_RECIPE_SLOT = 0;
    public static final int LAST_RECIPE_SLOT = 44;
    
    // Sloty do przycisków nawigacji (ostatni rząd)
    public static final int NAV_ROW_START = 45;
    public static final int PREV_PAGE_SLOT = 45;  // Poprzednia strona
    public static final int PAGE_INFO_SLOT = 49;  // Informacja o stronie
    public static final int NEXT_PAGE_SLOT = 53;  // Następna strona

    /**
     * Konstruktor - stwórz selektor receptur dla admina
     *
     * @param player admin który edytuje receptury
     * @param page numer strony (zaczynam od 1)
     */
    public RecipeSelectionGUI(Player player, int page) {
        this.player = player;
        
        // Oblicz liczbę stron
        List<CraftingRecipe> allRecipes = RecipeManager.getInstance().getAllRecipes();
        int totalRecipes = allRecipes.size();
        this.totalPages = (totalRecipes + RECIPES_PER_PAGE - 1) / RECIPES_PER_PAGE; // Zaokrąglij w górę
        
        // Sprawdź czy strona jest prawidłowa
        this.currentPage = Math.max(1, Math.min(page, Math.max(1, totalPages)));
        
        // Stwórz inventory z normalnym tytułem (bez custom czcionki - to miejsce do wyboru, nie do symulacji crafcingu)
        String selectionTitle = GUITitleFormatter.getRecipeSelectionTitle();
        this.inventory = Bukkit.createInventory(null, INVENTORY_SIZE, selectionTitle);
        
        // Załaduj receptury na tę stronę
        setupPage();
    }

    /**
     * Załaduj receptury na bieżącą stronę
     */
    private void setupPage() {
        // Pobierz wszystkie receptury
        List<CraftingRecipe> allRecipes = RecipeManager.getInstance().getAllRecipes();
        
        // Oblicz indeksy dla tej strony
        int startIndex = (currentPage - 1) * RECIPES_PER_PAGE;
        int endIndex = Math.min(startIndex + RECIPES_PER_PAGE, allRecipes.size());
        
        // Wyświetl receptury na tej stronie
        int slotIndex = FIRST_RECIPE_SLOT;
        for (int i = startIndex; i < endIndex; i++) {
            CraftingRecipe recipe = allRecipes.get(i);
            ItemStack recipeItem = createRecipeIcon(recipe);
            inventory.setItem(slotIndex, recipeItem);
            slotIndex++;
        }
        
        // Wypełnij pozostałe sloty puste (null)
        for (int i = slotIndex; i <= LAST_RECIPE_SLOT; i++) {
            inventory.setItem(i, null);
        }
        
        // Ustaw przyciski nawigacji
        setupNavigation();
    }

    /**
     * Stwórz ikonę receptury - wyświetli output item z metadanymi
     *
     * @param recipe receptura do wyświetlenia
     * @return ItemStack z ikoną receptury
     */
    private ItemStack createRecipeIcon(CraftingRecipe recipe) {
        // Pobierz output item z receptury
        ItemStack output = recipe.getOutput();
        
        if (output == null || output.getType().isAir()) {
            // Jeśli nie ma output - wyświetl pustą ikonę
            return createEmptyRecipeIcon();
        }
        
        // Sklonuj output aby nie modyfikować oryginalny
        ItemStack icon = output.clone();
        icon.setAmount(1); // Wyświetl tylko 1 item
        
        // Dodaj custom lore aby pokazać ID receptury
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            
            // Dodaj istniejące lore jeśli jest
            if (meta.hasLore() && meta.lore() != null) {
                lore.addAll(meta.lore());
            }
            
            // Dodaj ID receptury na końcu
            lore.add(Component.text("")); // Pusta linia
            lore.add(Component.text("§7ID: " + recipe.getId()));
            // Tekst w grze ma być po angielsku
            lore.add(Component.text("§7Click to edit"));
            
            meta.lore(lore);
            icon.setItemMeta(meta);
        }
        
        return icon;
    }

    /**
     * Stwórz pustą ikonę gdy receptura nie ma output'u
     *
     * @return pusty ItemStack
     */
    private ItemStack createEmptyRecipeIcon() {
        ItemStack empty = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = empty.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§7No output"));
            empty.setItemMeta(meta);
        }
        return empty;
    }

    /**
     * Ustaw przyciski nawigacji (poprzednia strona, info, następna strona)
     */
    private void setupNavigation() {
        // Przycisk poprzednia strona
        if (currentPage > 1) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            if (prevMeta != null) {
                prevMeta.displayName(Component.text("§c◄ Previous page"));
                prevMeta.lore(List.of(Component.text("§7Page: " + (currentPage - 1))));
                prevButton.setItemMeta(prevMeta);
            }
            inventory.setItem(PREV_PAGE_SLOT, prevButton);
        } else {
            // Brak poprzedniej strony - puste
            inventory.setItem(PREV_PAGE_SLOT, null);
        }
        
        // Informacja o stronie
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta pageInfoMeta = pageInfo.getItemMeta();
        if (pageInfoMeta != null) {
            pageInfoMeta.displayName(Component.text("§6Page " + currentPage + " / " + totalPages));
            pageInfoMeta.lore(List.of(Component.text("§7Recipes: " + RecipeManager.getInstance().getAllRecipes().size())));
            pageInfo.setItemMeta(pageInfoMeta);
        }
        inventory.setItem(PAGE_INFO_SLOT, pageInfo);
        
        // Przycisk następna strona
        if (currentPage < totalPages) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            if (nextMeta != null) {
                nextMeta.displayName(Component.text("§aNext page ►"));
                nextMeta.lore(List.of(Component.text("§7Page: " + (currentPage + 1))));
                nextButton.setItemMeta(nextMeta);
            }
            inventory.setItem(NEXT_PAGE_SLOT, nextButton);
        } else {
            // Brak następnej strony - puste
            inventory.setItem(NEXT_PAGE_SLOT, null);
        }
    }

    /**
     * Otwórz selektor dla gracza
     */
    public void open() {
        player.openInventory(inventory);
    }

    /**
     * Pobierz inventory selectora
     */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Pobierz gracza który edytuje
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Pobierz bieżący numer strony
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Pobierz recepturę na podstawie slotu (jeśli slot ma recepturę)
     *
     * @param slot numer slotu
     * @return receptura lub null jeśli slot nie zawiera receptury
     */
    public CraftingRecipe getRecipeFromSlot(int slot) {
        if (slot < FIRST_RECIPE_SLOT || slot > LAST_RECIPE_SLOT) {
            return null; // Slot nie zawiera receptury
        }
        
        // Oblicz index receptury na tej stronie
        int slotIndexOnPage = slot - FIRST_RECIPE_SLOT;
        int recipeIndexGlobal = (currentPage - 1) * RECIPES_PER_PAGE + slotIndexOnPage;
        
        // Pobierz recepturę z RecipeManager'a
        List<CraftingRecipe> allRecipes = RecipeManager.getInstance().getAllRecipes();
        if (recipeIndexGlobal >= 0 && recipeIndexGlobal < allRecipes.size()) {
            return allRecipes.get(recipeIndexGlobal);
        }
        
        return null;
    }

    /**
     * Sprawdź czy slot jest przyciskiem nawigacji
     *
     * @param slot numer slotu
     * @return true jeśli to przycisk nawigacji
     */
    public boolean isNavigationButton(int slot) {
        return slot == PREV_PAGE_SLOT || slot == PAGE_INFO_SLOT || slot == NEXT_PAGE_SLOT;
    }

    /**
     * Sprawdź czy slot to przycisk następna strona
     *
     * @param slot numer slotu
     * @return true jeśli to przycisk następna strona
     */
    public boolean isNextPageButton(int slot) {
        return slot == NEXT_PAGE_SLOT && currentPage < totalPages;
    }

    /**
     * Sprawdź czy slot to przycisk poprzednia strona
     *
     * @param slot numer slotu
     * @return true jeśli to przycisk poprzednia strona
     */
    public boolean isPrevPageButton(int slot) {
        return slot == PREV_PAGE_SLOT && currentPage > 1;
    }
}

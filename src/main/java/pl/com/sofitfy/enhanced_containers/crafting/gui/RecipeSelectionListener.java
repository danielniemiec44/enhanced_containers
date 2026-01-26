package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import pl.com.sofitfy.enhanced_containers.Main;
import pl.com.sofitfy.enhanced_containers.crafting.gui.RecipeSelectionGUI;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.CraftingRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeDraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RecipeSelectionListener - słucha zdarzeń w selektorze receptur
 *
 * Obsługuje:
 * - Kliknięcie na recepturę (otwarcie edytora)
 * - Kliknięcie na przyciski nawigacji (zmiana strony)
 * - Zamknięcie okna (wyrejestrowanie)
 */
public class RecipeSelectionListener implements Listener {
    // Przechowuje które selektory są otwarte u których graczy
    private final Map<UUID, RecipeSelectionGUI> activeSelectors = new HashMap<>();
    // Przechowuje ostatnią stronę dla każdego gracza
    private final Map<UUID, Integer> lastPages = new HashMap<>();
    
    // Referencja do listenera edytora aby zarejestrować edytor
    private final RecipeEditorListener editorListener;

    /**
     * Konstruktor
     *
     * @param editorListener listener edytora receptur (do rejestracji nowego edytora)
     */
    public RecipeSelectionListener(RecipeEditorListener editorListener) {
        this.editorListener = editorListener;
    }

    /**
     * Zarejestruj selektor - aby wiedzieć że gracz ma otwarty selektor
     */
    public void registerSelector(RecipeSelectionGUI selector) {
        if (selector != null && selector.getPlayer() != null) {
            activeSelectors.put(selector.getPlayer().getUniqueId(), selector);
            lastPages.put(selector.getPlayer().getUniqueId(), selector.getCurrentPage());
        }
    }

    /**
     * Wyrejestruj selektor - gracz zamknął okno
     */
    public void unregisterSelector(UUID playerId) {
        activeSelectors.remove(playerId);
        lastPages.remove(playerId);
    }

    /**
     * Słucha kiedy gracz klika w selektor receptur
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        RecipeSelectionGUI selector = activeSelectors.get(playerId);

        // Sprawdzenie czy to nasz selektor
        if (selector == null || !event.getInventory().equals(selector.getInventory())) {
            return;
        }

        // Blokuj normalne eventy - wszystko obsługujemy sami
        event.setCancelled(true);

        int slot = event.getRawSlot();

        // Sprawdzenie czy to kliknięcie w recepturę
        if (slot >= RecipeSelectionGUI.FIRST_RECIPE_SLOT && slot <= RecipeSelectionGUI.LAST_RECIPE_SLOT) {
            handleRecipeClick(player, selector, slot);
        }
        // Sprawdzenie czy to kliknięcie w przycisk nawigacji
        else if (selector.isNavigationButton(slot)) {
            handleNavigationClick(player, selector, slot);
        }
    }

    /**
     * Obsługa kliknięcia na recepturę
     *
     * Otwiera edytor dla wybranej receptury
     */
    private void handleRecipeClick(Player player, RecipeSelectionGUI selector, int slot) {
        // Pobierz recepturę z tego slotu
        CraftingRecipe recipe = selector.getRecipeFromSlot(slot);

        if (recipe == null) {
            player.sendMessage(Main.PLUGIN_PREFIX + "§cThat recipe does not exist!");
            return;
        }

        // Zamknij selektor (player zamknie okno sam)
        player.closeInventory();
        unregisterSelector(player.getUniqueId());

        // Stwórz draft dla istniejącej receptury (załaduje jej dane)
        RecipeDraft draft = new RecipeDraft(recipe.getId());
        draft.setInputPattern(recipe.getInputPattern());
        draft.setOutput(recipe.getOutput());

        // Stwórz GUI edytora
        RecipeEditorGUI editor = new RecipeEditorGUI(player, draft);

        // Zarejestruj edytor w listenera
        if (editorListener != null) {
            editorListener.registerEditor(editor);
        }

        // Otwórz edytor
        editor.open();

        // Powiadomienie
        player.sendMessage(Main.PLUGIN_PREFIX + "§aEditing recipe: " + recipe.getId());
        player.sendMessage("§7Place input items on the left, output on the right");
        player.sendMessage("§7Press Escape to save changes");
    }

    /**
     * Obsługa kliknięcia na przyciski nawigacji
     *
     * Przełącza między stronami selektora
     */
    private void handleNavigationClick(Player player, RecipeSelectionGUI selector, int slot) {
        RecipeSelectionGUI newSelector = null;

        // Kliknięcie na poprzednią stronę
        if (selector.isPrevPageButton(slot)) {
            newSelector = new RecipeSelectionGUI(player, selector.getCurrentPage() - 1);
        }
        // Kliknięcie na następną stronę
        else if (selector.isNextPageButton(slot)) {
            newSelector = new RecipeSelectionGUI(player, selector.getCurrentPage() + 1);
        }

        if (newSelector != null) {
            // Wyrejestruj stary selektor
            unregisterSelector(player.getUniqueId());

            // Zarejestruj nowy selektor
            registerSelector(newSelector);

            // Otwórz nową stronę
            newSelector.open();
        }
    }

    /**
     * Słucha kiedy gracz zamyka selektor receptur
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            UUID playerId = player.getUniqueId();
            RecipeSelectionGUI selector = activeSelectors.get(playerId);

            // Jeśli to nasz selektor - wyrejestruj go
            if (selector != null && event.getInventory().equals(selector.getInventory())) {
                unregisterSelector(playerId);
            }
        }
    }
}

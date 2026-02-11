package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import pl.com.sofitfy.enhanced_containers.Main;
import pl.com.sofitfy.enhanced_containers.config.ConfigManager;
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
    // Przechowuje gracza oczekującego na potwierdzenie usunięcia receptury
    private final Map<UUID, String> pendingDeletion = new HashMap<>();
    
    // Referencja do listenera edytora aby zarejestrować edytor
    private final RecipeEditorListener editorListener;

    private final ConfigManager configManager;

    /**
     * Konstruktor
     *
     * @param editorListener listener edytora receptur (do rejestracji nowego edytora)
     */
    public RecipeSelectionListener(RecipeEditorListener editorListener, ConfigManager configManager) {
        this.editorListener = editorListener;
        this.configManager = configManager;
    }

    /**
     * Fabryka selectora - używa normalnego tytułu bez custom czcionki.
     * Custom czcionka jest używana tylko w CraftingGUI (4x4 crafting), nie w selektorze receptur.
     */
    public RecipeSelectionGUI createSelector(Player player, int page) {
        if (configManager != null) {
            String title = configManager.getRecipeSelectionTitle();
            return new RecipeSelectionGUI(player, page, title);
        }

        return new RecipeSelectionGUI(player, page);
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
            // RIGHT CLICK - pytaj o usunięcie
            if (event.getClick().isRightClick() && !event.getClick().isShiftClick()) {
                handleRecipeRightClick(player, selector, slot);
            }
            // LEFT CLICK - otwórz edytor
            else if (event.getClick().isLeftClick() && !event.getClick().isShiftClick()) {
                handleRecipeClick(player, selector, slot);
            }
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
        RecipeEditorGUI editor = (editorListener != null)
            ? editorListener.createEditor(player, draft)
            : new RecipeEditorGUI(player, draft);

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
            newSelector = createSelector(player, selector.getCurrentPage() - 1);
        }
        // Kliknięcie na następną stronę
        else if (selector.isNextPageButton(slot)) {
            newSelector = createSelector(player, selector.getCurrentPage() + 1);
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

            if (selector != null && event.getInventory().equals(selector.getInventory())) {
                unregisterSelector(playerId);
            }
        }
    }
    
    /**
     * Obsługa right-click na recepturze - pytaj o usunięcie
     */
    private void handleRecipeRightClick(Player player, RecipeSelectionGUI selector, int slot) {
        // Pobierz recepturę z tego slotu
        CraftingRecipe recipe = selector.getRecipeFromSlot(slot);

        if (recipe == null) {
            player.sendMessage(Main.PLUGIN_PREFIX + "§cThat recipe does not exist!");
            return;
        }

        String recipeId = recipe.getId();
        
        // Pokaż GUI potwierdzenia
        openDeleteConfirmationGUI(player, recipeId, selector.getCurrentPage());
    }
    
    /**
     * Otwórz GUI potwierdzenia usunięcia receptury
     */
    private void openDeleteConfirmationGUI(Player player, String recipeId, int currentPage) {
        // Zamknij selektor tymczasowo
        player.closeInventory();
        
        // Zapamiętaj że czeka na potwierdzenie
        pendingDeletion.put(player.getUniqueId(), recipeId);
        
        // Stwórz GUI 27 slotów (3 rzędy)
        org.bukkit.inventory.Inventory confirmGUI = org.bukkit.Bukkit.createInventory(null, 27, "§cDelete recipe: " + recipeId + "?");
        
        // Slot 11: CONFIRM (zielone szkło)
        org.bukkit.inventory.ItemStack confirmItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LIME_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName("§a§lCONFIRM DELETE");
            java.util.List<String> confirmLore = new java.util.ArrayList<>();
            confirmLore.add("§7Click to delete this recipe");
            confirmLore.add("§cThis action cannot be undone!");
            confirmMeta.setLore(confirmLore);
            confirmItem.setItemMeta(confirmMeta);
        }
        confirmGUI.setItem(11, confirmItem);
        
        // Slot 15: CANCEL (czerwone szkło)
        org.bukkit.inventory.ItemStack cancelItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.RED_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName("§c§lCANCEL");
            java.util.List<String> cancelLore = new java.util.ArrayList<>();
            cancelLore.add("§7Return to recipe list");
            cancelMeta.setLore(cancelLore);
            cancelItem.setItemMeta(cancelMeta);
        }
        confirmGUI.setItem(15, cancelItem);
        
        player.openInventory(confirmGUI);
    }
    
    /**
     * Obsługa kliknięcia w GUI potwierdzenia
     */
    @EventHandler
    public void onDeleteConfirmationClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        // Sprawdź czy to GUI potwierdzenia
        String title = event.getView().getTitle();
        if (!title.startsWith("§cDelete recipe:")) {
            return;
        }
        
        event.setCancelled(true);
        
        UUID playerId = player.getUniqueId();
        String recipeId = pendingDeletion.get(playerId);
        if (recipeId == null) {
            player.closeInventory();
            return;
        }
        
        int slot = event.getRawSlot();
        
        // Slot 11 = CONFIRM
        if (slot == 11) {
            player.closeInventory();
            pendingDeletion.remove(playerId);
            
            // Usuń recepturę z config.yml
            boolean deleted = deleteRecipeFromConfig(recipeId);
            
            if (deleted) {
                player.sendMessage(Main.PLUGIN_PREFIX + "§a✓ Recipe '" + recipeId + "' deleted!");
                
                // WAŻNE: Przeładuj ConfigManager aby wczytać nowy config z dysku
                configManager.reload();
                
                // Przeładuj receptury
                reloadRecipes();
                
                // Otwórz selektor ponownie
                org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(
                    org.bukkit.Bukkit.getPluginManager().getPlugin("PlethorasExtendedTools"), 
                    () -> {
                        int page = lastPages.getOrDefault(playerId, 1);
                        RecipeSelectionGUI newSelector = createSelector(player, page);
                        registerSelector(newSelector);
                        newSelector.open();
                    }, 
                    2L
                );
            } else {
                player.sendMessage(Main.PLUGIN_PREFIX + "§c✗ Error deleting recipe!");
            }
            
            return;
        }
        
        // Slot 15 = CANCEL
        if (slot == 15) {
            player.closeInventory();
            pendingDeletion.remove(playerId);
            
            // Wróć do selektora
            org.bukkit.Bukkit.getScheduler().scheduleSyncDelayedTask(
                org.bukkit.Bukkit.getPluginManager().getPlugin("PlethorasExtendedTools"),
                () -> {
                    int page = lastPages.getOrDefault(playerId, 1);
                    RecipeSelectionGUI newSelector = createSelector(player, page);
                    registerSelector(newSelector);
                    newSelector.open();
                }, 
                1L
            );
            
            return;
        }
    }
    
    /**
     * Usuń recepturę z config.yml
     */
    private boolean deleteRecipeFromConfig(String recipeId) {
        try {
            // Załaduj config BEZPOŚREDNIO z pliku (nie z ConfigManager cache)
            java.io.File configFile = new java.io.File(
                org.bukkit.Bukkit.getPluginManager().getPlugin("PlethorasExtendedTools").getDataFolder(), 
                "config.yml"
            );
            
            if (!configFile.exists()) {
                return false;
            }
            
            org.bukkit.configuration.file.YamlConfiguration config = 
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
            
            java.util.List<?> recipesList = config.getList("crafting.recipes");
            
            if (recipesList == null) {
                return false;
            }
            
            // Znajdź i usuń WSZYSTKIE wystąpienia receptury (obsługa duplikatów)
            java.util.List<Object> recipes = new java.util.ArrayList<>();
            int removedCount = 0;
            
            for (int i = 0; i < recipesList.size(); i++) {
                Object obj = recipesList.get(i);
                if (obj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> recipeMap = (java.util.Map<String, Object>) obj;
                    String id = (String) recipeMap.get("id");
                    
                    if (recipeId.equals(id)) {
                        removedCount++;
                    } else {
                        recipes.add(obj);
                    }
                }
            }
            
            if (removedCount == 0) {
                return false;
            }
            
            // Zapisz z powrotem do config.yml
            config.set("crafting.recipes", recipes);
            config.save(configFile);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Przeładuj receptury z pliku config
     */
    private void reloadRecipes() {
        pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager.getInstance().clearRecipes();
        
        org.bukkit.configuration.file.FileConfiguration config = configManager.getConfig();
        if (config != null) {
            var recipeLoader = new pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeLoader(config);
            var recipes = recipeLoader.loadRecipes();
            if (recipes != null) {
                pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager.getInstance().registerRecipes(recipes);
            }
        }
    }
}

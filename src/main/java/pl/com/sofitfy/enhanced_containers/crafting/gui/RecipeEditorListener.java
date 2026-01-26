package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import pl.com.sofitfy.enhanced_containers.Main;
import pl.com.sofitfy.enhanced_containers.config.ConfigManager;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeDraft;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * RecipeEditorListener - słucha zdarzeń edytora receptur
 *
 * Obsługuje:
 * - Zamykanie okna edytora (Escape)
 * - Validację danych
 * - Zapis do config.yml
 * - Przeładowanie receptur
 */
public class RecipeEditorListener implements Listener {
    // Przechowuje które edytory są aktywne dla danego gracza
    private final Map<UUID, RecipeEditorGUI> activeEditors = new HashMap<>();

    // Przechowuje ostatni stan GUI (do przywrócenia po błędzie)
    private final Map<UUID, Map<Integer, org.bukkit.inventory.ItemStack>> lastGUIState = new HashMap<>();

    private final ConfigManager configManager;
    private final Plugin plugin;

    public RecipeEditorListener(Plugin plugin, ConfigManager configManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configManager = configManager;
    }

    /**
     * Fabryka edytora - pozwala zachować spójny tytuł (tekstura + normalny tytuł z configu)
     * dla /4x4 new oraz /4x4 edit.
     */
    public RecipeEditorGUI createEditor(Player player, RecipeDraft draft) {
        if (configManager != null) {
            String title = GUITitleFormatter.withTextureAndVanillaTitle(configManager.getRecipeEditorTitle());
            return new RecipeEditorGUI(player, draft, title);
        }

        // Fallback: stary tytuł z formattera (może być sama tekstura)
        return new RecipeEditorGUI(player, draft);
    }

    /**
     * Zarejestruj edytor dla gracza (żeby wiedzieć że edytuje)
     */
    public void registerEditor(RecipeEditorGUI editor) {
        UUID playerId = editor.getPlayer().getUniqueId();
        activeEditors.put(playerId, editor);

        // Zapisz stan GUI na wypadek błędu
        saveGUIState(editor);
    }

    /**
     * Wyrejestruj edytor gracza
     */
    public void unregisterEditor(UUID playerId) {
        activeEditors.remove(playerId);
        lastGUIState.remove(playerId);
    }

    /**
     * Zapisz stan GUI (do przywrócenia po błędzie)
     */
    private void saveGUIState(RecipeEditorGUI editor) {
        UUID playerId = editor.getPlayer().getUniqueId();
        Map<Integer, org.bukkit.inventory.ItemStack> state = new HashMap<>();

        // Zapisz wszystkie itemy z gridu i outputu
        for (int slot : RecipeEditorGUI.GRID_SLOTS) {
            org.bukkit.inventory.ItemStack item = editor.getInventory().getItem(slot);
            if (item != null) {
                state.put(slot, item.clone());
            }
        }

        // Zapisz output
        org.bukkit.inventory.ItemStack output = editor.getInventory().getItem(RecipeEditorGUI.OUTPUT_SLOT);
        if (output != null) {
            state.put(RecipeEditorGUI.OUTPUT_SLOT, output.clone());
        }

        lastGUIState.put(playerId, state);
    }

    /**
     * Przywróć ostatni stan GUI (po błędzie)
     */
    private void restoreGUIState(RecipeEditorGUI editor) {
        UUID playerId = editor.getPlayer().getUniqueId();
        Map<Integer, org.bukkit.inventory.ItemStack> state = lastGUIState.get(playerId);

        if (state != null) {
            for (Map.Entry<Integer, org.bukkit.inventory.ItemStack> entry : state.entrySet()) {
                editor.getInventory().setItem(entry.getKey(), entry.getValue().clone());
            }
        }
    }

    /**
     * Event - gracz zamyka okno edytora (wciska Escape)
     * 
     * Logika:
     * 1. Sprawdź czy to nasz edytor
     * 2. Sprawdzenie cursor itema - jeśli gracz trzyma item, zwróć go
     * 3. Sprawdzenie czy GUI puste - anulacja
     * 4. Walidacja (musi być input i output)
     * 5. Zapis do config.yml
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Sprawdzenie czy to nasz edytor
        RecipeEditorGUI editor = activeEditors.get(playerId);
        if (editor == null) {
            return; // To nie nasz edytor
        }

        // Jeśli gracz trzyma item w kursorze, zwróć go do inventorium gracza
        if (player.getItemOnCursor() != null && !player.getItemOnCursor().getType().isAir()) {
            org.bukkit.inventory.ItemStack cursorItem = player.getItemOnCursor();
            player.setItemOnCursor(new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
            
            // Spróbuj dodać do gracza, resztę wyrzuć
            Map<Integer, org.bukkit.inventory.ItemStack> leftover = 
                player.getInventory().addItem(cursorItem);
            for (org.bukkit.inventory.ItemStack item : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), item);
            }
            
            player.sendMessage(Main.PLUGIN_PREFIX + "§7Item returned to your inventory!");
            
            // Ponownie otwórz edytor
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                RecipeEditorGUI newEditor = createEditor(player, editor.getDraft());
                restoreGUIState(newEditor);
                registerEditor(newEditor);
                newEditor.open();
            }, 1L);
            
            return; // Nie zamykaj okna
        }

        // Wyrejestruj edytor
        unregisterEditor(playerId);

        // Sprawdzenie czy GUI jest puste (anulacja)
        if (editor.isEmpty()) {
            player.sendMessage(Main.PLUGIN_PREFIX + "§7Recipe creation cancelled.");
            return;
        }

        // Zaktualizuj draft z zawartością GUI
        editor.updateDraftFromGUI();

        // Sprawdzenie czy draft ma błędy (brakuje input lub output)
        if (editor.hasErrors()) {
            // BŁĄD - otwórz edytor znowu z poprzednim stanem
            player.sendMessage(Main.PLUGIN_PREFIX + "§c✗ " + editor.getErrorMessage());

            // Otwórz edytor znowu - gracz ma szansę poprawić
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                RecipeEditorGUI newEditor = createEditor(player, editor.getDraft());
                restoreGUIState(newEditor);
                registerEditor(newEditor);
                newEditor.open();
            }, 2L); // Opóźnienie żeby gracz widział wiadomość

            return;
        }

        // SUKCES - zapisz do config.yml
        boolean saved = saveRecipeToConfig(editor.getDraft());

        if (saved) {
            player.sendMessage(Main.PLUGIN_PREFIX + "§a✓ Success!");

            // Przeładuj receptury
            reloadRecipes();
        } else {
            player.sendMessage(Main.PLUGIN_PREFIX + "§c✗ Error saving to file!");

            // Otwórz edytor znowu
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                RecipeEditorGUI newEditor = createEditor(player, editor.getDraft());
                restoreGUIState(newEditor);
                registerEditor(newEditor);
                newEditor.open();
            }, 2L);
        }
    }

    /**
     * Event - kliknięcie w edytorze receptur.
     *
     * Wymaganie:
     * - W inputach receptury (grid 4x4) nie można mieć stacków > 1.
     * - Nadmiar oddajemy graczowi do ekwipunku (a jeśli brak miejsca - drop na ziemię).
     */
    @EventHandler
    public void onEditorInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        RecipeEditorGUI editor = activeEditors.get(playerId);
        if (editor == null) {
            return;
        }

        // Pilnujemy tylko wtedy, gdy górny inventory to nasz edytor.
        if (event.getView() == null || event.getView().getTopInventory() != editor.getInventory()) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        int rawSlot = event.getRawSlot();

        // SHIFT+CLICK z dołu (ekwipunek gracza): NIE pozwalamy, żeby itemy wpadały w puste sloty tła.
        // Zamiast tego wkładamy je tylko do gridu (po 1 na slot).
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
            && event.getClickedInventory() != null
            && event.getClickedInventory() == event.getView().getBottomInventory()) {
            moveShiftClickedItemIntoEditorGrid(event, player, editor);
            return;
        }

        // Blokada: w górnym inventory można klikać tylko input grid i output.
        boolean isTopSlot = rawSlot >= 0 && rawSlot < topSize;
        if (isTopSlot && !isAllowedEditorTopSlot(rawSlot)) {
            event.setCancelled(true);
            return;
        }

        // Najprościej i najbezpieczniej: po wykonaniu kliknięcia normalizujemy stan gridu.
        // To łapie też shift-click, hotbar swap, merge itp.
        Bukkit.getScheduler().scheduleSyncDelayedTask(
            plugin,
            () -> enforceInputStacksAreSingle(player, editor),
            1L
        );
    }

    /**
     * Event - przeciąganie itemów (drag) w edytorze receptur.
     * Też może wstawić stacki > 1, więc pilnujemy identycznie.
     */
    @EventHandler
    public void onEditorInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        RecipeEditorGUI editor = activeEditors.get(playerId);
        if (editor == null) {
            return;
        }

        if (event.getView() == null || event.getView().getTopInventory() != editor.getInventory()) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();

        // Blokada: drag nie może dotykać slotów tła w górnym inventory.
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < topSize && !isAllowedEditorTopSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }

        // Jeśli drag w ogóle nie dotyka slotów gridu - nie ma co robić.
        boolean touchesGrid = false;
        for (int rawSlot : event.getRawSlots()) {
            if (RecipeEditorGUI.isGridSlot(rawSlot)) {
                touchesGrid = true;
                break;
            }
        }
        if (!touchesGrid) {
            return;
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(
            plugin,
            () -> enforceInputStacksAreSingle(player, editor),
            1L
        );
    }

    private static void enforceInputStacksAreSingle(Player player, RecipeEditorGUI editor) {
        for (int slot : RecipeEditorGUI.GRID_SLOTS) {
            ItemStack item = editor.getInventory().getItem(slot);
            if (item == null) {
                continue;
            }

            Material type = item.getType();
            if (type == null || type.isAir()) {
                editor.getInventory().setItem(slot, null);
                continue;
            }

            int amount = item.getAmount();
            if (amount <= 1) {
                continue;
            }

            int extra = amount - 1;
            item.setAmount(1);
            editor.getInventory().setItem(slot, item);

            ItemStack toReturn;
            try {
                toReturn = item.clone();
            } catch (Exception ignored) {
                toReturn = new ItemStack(type, 1);
            }
            toReturn.setAmount(extra);

            // Zwróć nadmiar graczowi.
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(toReturn);
            for (ItemStack left : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), left);
            }
        }
    }

    private static boolean isAllowedEditorTopSlot(int rawSlot) {
        return RecipeEditorGUI.isGridSlot(rawSlot) || RecipeEditorGUI.isOutputSlot(rawSlot);
    }

    private void moveShiftClickedItemIntoEditorGrid(InventoryClickEvent event, Player player, RecipeEditorGUI editor) {
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == null || current.getType().isAir()) {
            return;
        }

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return;
        }

        event.setCancelled(true);

        int remaining = Math.max(0, current.getAmount());
        if (remaining == 0) {
            return;
        }

        ItemStack single;
        try {
            single = current.clone();
        } catch (Exception ignored) {
            single = new ItemStack(current.getType(), 1);
        }
        single.setAmount(1);

        int moved = 0;
        for (int slot : RecipeEditorGUI.GRID_SLOTS) {
            if (remaining <= 0) {
                break;
            }

            ItemStack existing = editor.getInventory().getItem(slot);
            if (existing != null && existing.getType() != null && !existing.getType().isAir()) {
                continue;
            }

            editor.getInventory().setItem(slot, single.clone());
            remaining--;
            moved++;
        }

        if (moved == 0) {
            return;
        }

        if (remaining <= 0) {
            clicked.setItem(event.getSlot(), null);
        } else {
            ItemStack newStack;
            try {
                newStack = current.clone();
            } catch (Exception ignored) {
                newStack = new ItemStack(current.getType(), remaining);
            }
            newStack.setAmount(remaining);
            clicked.setItem(event.getSlot(), newStack);
        }

        // Dla pewności (np. gdy inne akcje zmienią sloty): dopnij normalizację w następnym ticku.
        Bukkit.getScheduler().scheduleSyncDelayedTask(
            plugin,
            () -> enforceInputStacksAreSingle(player, editor),
            1L
        );
    }

    /**
     * Zapisz recepturę do config.yml
     *
     * @param draft - dane receptury do zapisu
     * @return true jeśli sukces, false jeśli błąd
     */
    private boolean saveRecipeToConfig(RecipeDraft draft) {
        try {
            // Pobierz plik config
            File configFile = new File(
                plugin.getDataFolder(),
                "config.yml"
            );

            if (!configFile.exists()) {
                return false; // Plik nie istnieje
            }

            // Załaduj YAML
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // Pobierz listę receptur
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recipes = (List<Map<String, Object>>) config.getList("crafting.recipes");
            if (recipes == null) {
                recipes = new ArrayList<>();
            }

            // Stwórz mapę dla nowej receptury
            Map<String, Object> recipeMap = new LinkedHashMap<>();
            recipeMap.put("id", draft.getId());
            recipeMap.put("name", draft.getName());
            recipeMap.put("permission", draft.getPermission());

            // Serializuj input pattern - lista rzędów (każdy rząd to lista ItemStacków)
            List<List<Object>> inputList = new ArrayList<>();
            for (org.bukkit.inventory.ItemStack[] row : draft.getInputPattern()) {
                List<Object> rowList = new ArrayList<>();
                for (org.bukkit.inventory.ItemStack item : row) {
                    if (item == null || item.getType().isAir()) {
                        rowList.add(null);
                    } else {
                        // Serializuj ItemStack - Bukkit zrobi to automatycznie
                        rowList.add(item);
                    }
                }
                inputList.add(rowList);
            }
            recipeMap.put("input", inputList);

            // Serializuj output - pełny ItemStack
            recipeMap.put("output", draft.getOutput());

            // Dodaj do listy
            recipes.add(recipeMap);

            // Ustaw z powrotem i zapisz
            config.set("crafting.recipes", recipes);
            config.save(configFile);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Przeładuj receptury z pliku config
     */
    private void reloadRecipes() {
        // Przeładuj config
        configManager.reload();

        // Wyczyść stare receptury
        RecipeManager.getInstance().clearRecipes();

        // Załaduj nowe receptury
        var config = configManager.getConfig();
        if (config != null) {
            var recipeLoader = new pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeLoader(config);
            var recipes = recipeLoader.loadRecipes();
            if (recipes != null) {
                RecipeManager.getInstance().registerRecipes(recipes);
            }
        }
    }
}


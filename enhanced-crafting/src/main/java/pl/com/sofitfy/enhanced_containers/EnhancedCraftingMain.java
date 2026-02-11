package pl.com.sofitfy.enhanced_containers;

import org.bukkit.plugin.java.JavaPlugin;
import pl.com.sofitfy.enhanced_containers.commands.CraftCommand;
import pl.com.sofitfy.enhanced_containers.config.ConfigManager;
import pl.com.sofitfy.enhanced_containers.crafting.gui.CraftingGUIListener;
import pl.com.sofitfy.enhanced_containers.crafting.gui.RecipeEditorListener;
import pl.com.sofitfy.enhanced_containers.crafting.gui.RecipeSelectionListener;
import pl.com.sofitfy.enhanced_containers.crafting.nms.NmsCraftingSelfTestRunner;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeLoader;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager;

/**
 * Główna klasa pluginu EnhancedCrafting (4x4 crafting).
 * Dodaje rozszerzony stół rzemieślniczy z 4x4 gridem, sugestiami i edytorem receptur.
 */
public class EnhancedCraftingMain extends JavaPlugin {

    // Prefix do wiadomości w chacie
    public static final String PLUGIN_PREFIX = "§6[EnhancedCrafting] §r";

    private ConfigManager configManager;
    private CraftingGUIListener guiListener;

    @Override
    public void onEnable() {
        getLogger().info(PLUGIN_PREFIX + "Plugin is starting!");

        try {
            // Czytamy config - bez tego nic nie będzie działać
            configManager = new ConfigManager(this);
            if (configManager == null) {
                getLogger().severe(PLUGIN_PREFIX + "Error: Cannot load ConfigManager!");
                return;
            }

            var config = configManager.getConfig();
            if (config == null) {
                getLogger().severe(PLUGIN_PREFIX + "Error: Config is empty!");
                return;
            }

            // Ładujemy receptury z config'a i dodajemy do menadżera
            RecipeLoader recipeLoader = new RecipeLoader(config);
            var recipes = recipeLoader.loadRecipes();
            if (recipes != null) {
                RecipeManager.getInstance().registerRecipes(recipes);
                getLogger().info(PLUGIN_PREFIX + "Loaded " +
                    RecipeManager.getInstance().getAllRecipes().size() + " custom recipes!");
            } else {
                getLogger().warning(PLUGIN_PREFIX + "No custom recipes loaded (list is empty)");
            }

            // Ładujemy vanilla receptury z serwera (crafting table)
            RecipeManager.getInstance().loadVanillaRecipes();
            getLogger().info(PLUGIN_PREFIX + "Loaded " +
                RecipeManager.getInstance().getAllVanillaRecipes().size() + " vanilla recipes!");

            // Listener zdarzeń GUI craftingu
            guiListener = new CraftingGUIListener(this);
            if (guiListener == null) {
                getLogger().severe(PLUGIN_PREFIX + "Error: Cannot load CraftingGUIListener!");
                return;
            }
            getServer().getPluginManager().registerEvents(guiListener, this);

            // Listener edytora receptur
            RecipeEditorListener editorListener = new RecipeEditorListener(this, configManager);
            getServer().getPluginManager().registerEvents(editorListener, this);

            // Listener selektora receptur
            RecipeSelectionListener selectionListener =
                new RecipeSelectionListener(editorListener, configManager);
            getServer().getPluginManager().registerEvents(selectionListener, this);

            // Rejestracja komendy /4x4 (alias: /ecraft)
            var craftCommand = getCommand("4x4");
            if (craftCommand == null) {
                getLogger().severe(PLUGIN_PREFIX + "Command '4x4' is not registered in plugin.yml!");
                return;
            }

            CraftCommand commandExecutor = new CraftCommand(
                this, configManager, guiListener, editorListener, selectionListener);
            craftCommand.setExecutor(commandExecutor);

            // Self-test NMS (tylko pod prawdziwy Paper serwer)
            // Włączane przez: -Dplethoras.nmsSelfTest=true
            try {
                new NmsCraftingSelfTestRunner(this).maybeRun();
            } catch (NoClassDefFoundError e) {
                // NMS niedostępny - pomijamy
            }

            getLogger().info(PLUGIN_PREFIX + "§aPlugin fully loaded!");
        } catch (Exception e) {
            getLogger().severe(PLUGIN_PREFIX + "Startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info(PLUGIN_PREFIX + "Plugin is shutting down!");
    }

    /**
     * Zwraca menedżera konfiguracji.
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
}

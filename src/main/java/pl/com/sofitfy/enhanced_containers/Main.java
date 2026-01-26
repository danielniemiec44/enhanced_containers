package pl.com.sofitfy.enhanced_containers;

import org.bukkit.plugin.java.JavaPlugin;
import pl.com.sofitfy.enhanced_containers.config.ConfigManager;
import pl.com.sofitfy.enhanced_containers.commands.CraftCommand;
import pl.com.sofitfy.enhanced_containers.crafting.gui.CraftingGUIListener;
import pl.com.sofitfy.enhanced_containers.crafting.gui.RecipeEditorListener;
import pl.com.sofitfy.enhanced_containers.crafting.gui.RecipeSelectionListener;
import pl.com.sofitfy.enhanced_containers.crafting.nms.NmsCraftingSelfTestRunner;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeLoader;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager;

public class Main extends JavaPlugin {
    // Nazwa pluginu i prefix do wiadomości
    public static String PLUGIN_NAME = "Plethoras Extended Tools";
    public static String PLUGIN_PREFIX = "§6[Plethoras] §r";

    // Rozmiar inventoria - ZAWSZE 54 sloty (9 kolumn × 6 rzędów)
    // Nie można tego zmieniać!
    public static final int INVENTORY_SIZE = 54;

    // Sloty gridu inputu (4×4)
    public static final int[] GRID_SLOTS = {
        0, 1, 2, 3,
        9, 10, 11, 12,
        18, 19, 20, 21,
        27, 28, 29, 30
    };

    // Slot na output - ZAWSZE 24
    public static final int OUTPUT_SLOT = 24;

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

            // Ładujemy config plik
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
                getLogger().info(PLUGIN_PREFIX + "Loaded " + RecipeManager.getInstance().getAllRecipes().size() + " custom recipes!");
            } else {
                getLogger().warning(PLUGIN_PREFIX + "No custom recipes were loaded (list is empty)");
            }

            // Ładujemy vanilla receptury z serwera (crafting table)
            RecipeManager.getInstance().loadVanillaRecipes();
            getLogger().info(PLUGIN_PREFIX + "Loaded " + RecipeManager.getInstance().getAllVanillaRecipes().size() + " vanilla recipes!");

            // Słuchamy zdarzeń z GUI - kiedy gracz coś robi w okienku
            guiListener = new CraftingGUIListener(this);
            if (guiListener == null) {
                getLogger().severe(PLUGIN_PREFIX + "Error: Cannot load CraftingGUIListener!");
                return;
            }

            // Rejestrujemy listener eventów na serwerze
            getServer().getPluginManager().registerEvents(guiListener, this);

            // Słuchamy zdarzeń edytora receptur - kiedy admin edytuje recepturę
            RecipeEditorListener editorListener = new RecipeEditorListener(this, configManager);

            // Rejestrujemy listener edytora
            getServer().getPluginManager().registerEvents(editorListener, this);

            // Słuchamy zdarzeń selektora receptur - kiedy admin wybiera recepturę do edycji
            RecipeSelectionListener selectionListener = new RecipeSelectionListener(editorListener);

            // Rejestrujemy listener selectora
            getServer().getPluginManager().registerEvents(selectionListener, this);

            // Rejestrujemy komendę /4x4 (alias: /ecraft)
            var craftCommand = getCommand("4x4");
            if (craftCommand == null) {
                getLogger().severe(PLUGIN_PREFIX + "Command '4x4' is not registered in plugin.yml!");
                return;
            }

            // Stwórz executor komendy i przekaż mu wszystkie listenery
            CraftCommand commandExecutor = new CraftCommand(this, configManager, guiListener, editorListener, selectionListener);
            craftCommand.setExecutor(commandExecutor);

            // Self-test pod prawdziwy Paper serwer (tylko dla testów automatycznych).
            // Włączane przez: -Dplethoras.nmsSelfTest=true
            new NmsCraftingSelfTestRunner(this).maybeRun();


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
}



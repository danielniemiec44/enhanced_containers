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
import pl.com.sofitfy.enhanced_containers.furnace.EnhancedFurnaceListener;
import pl.com.sofitfy.enhanced_containers.furnace.nms.EnhancedFurnaceHopperHandler;
import pl.com.sofitfy.enhanced_containers.furnace.nms.EnhancedFurnaceHopperTickHandler;
import pl.com.sofitfy.enhanced_containers.furnace.nms.NmsFurnaceEngineDirect;
import pl.com.sofitfy.enhanced_containers.furnace.EnhancedFurnaceLogic;
import pl.com.sofitfy.enhanced_containers.furnace.EnhancedFurnaceManager;
import pl.com.sofitfy.enhanced_containers.furnace.FurnaceCommand;
import pl.com.sofitfy.enhanced_containers.integration.WorldEditIntegration;
import pl.com.sofitfy.enhanced_containers.sharedcrafting.SharedCraftingCommand;
import pl.com.sofitfy.enhanced_containers.sharedcrafting.SharedCraftingListener;
import pl.com.sofitfy.enhanced_containers.sharedcrafting.SharedCraftingManager;
import pl.com.sofitfy.enhanced_containers.sharedcrafting.nms.RecipeBookPacketHandler;
import pl.com.sofitfy.enhanced_containers.sharedcrafting.nms.RecipeBookPacketListener;

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
    private SharedCraftingManager sharedCraftingManager;
    private RecipeBookPacketHandler recipeBookPacketHandler;
    private EnhancedFurnaceManager enhancedFurnaceManager;
    private EnhancedFurnaceLogic enhancedFurnaceLogic;

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
            RecipeSelectionListener selectionListener = new RecipeSelectionListener(editorListener, configManager);

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

            // Inicjalizacja Współdzielonego Craftingu (Shared Crafting)
            this.sharedCraftingManager = new SharedCraftingManager(this);
            this.sharedCraftingManager.loadData();
            SharedCraftingListener sharedCraftingListener = new SharedCraftingListener(this.sharedCraftingManager);
            getServer().getPluginManager().registerEvents(sharedCraftingListener, this);
            
            // Inicjalizacja NMS packet handlera dla Recipe Book
            // W środowisku testowym (MockBukkit) NMS nie jest dostępny - pomijamy inicjalizację
            // Sprawdzamy czy to MockBukkit po nazwie klasy serwera
            boolean isMockBukkit = getServer().getClass().getName().contains("MockBukkit") || 
                                   getServer().getClass().getName().contains("ServerMock");
            
            if (!isMockBukkit) {
                try {
                    this.recipeBookPacketHandler = new RecipeBookPacketHandler(this, this.sharedCraftingManager);
                    RecipeBookPacketListener recipeBookListener = new RecipeBookPacketListener(this.recipeBookPacketHandler);
                    getServer().getPluginManager().registerEvents(recipeBookListener, this);
                    // Zarejestruj handlery dla graczy którzy już są online (hot-reload pluginu)
                    this.recipeBookPacketHandler.registerAllPlayers();
                    getLogger().info(PLUGIN_PREFIX + "Recipe Book NMS packet handler initialized!");
                } catch (NoClassDefFoundError | Exception e) {
                    // NMS niedostępny - kontynuuj bez Recipe Book handlera
                    getLogger().warning(PLUGIN_PREFIX + "Recipe Book NMS packet handler failed to initialize: " + e.getMessage());
                    this.recipeBookPacketHandler = null;
                }
            } else {
                // MockBukkit - pomijamy NMS packet handler
                getLogger().info(PLUGIN_PREFIX + "Recipe Book NMS packet handler skipped (MockBukkit detected)");
                this.recipeBookPacketHandler = null;
            }
            
            // Okresowa walidacja stołów rzemieślniczych - co 1 sekundę (20 ticków)
            // Sprawdza czy bloki nadal są CRAFTING_TABLE, jeśli nie - dropuje itemy
            // Częsta walidacja łapie zmiany bloków przez pluginy (WorldEdit, /setblock, itp.)
            getServer().getScheduler().runTaskTimer(this, () -> {
                this.sharedCraftingManager.validateAndCleanupInvalidWorkbenches();
                // Nie logujemy - za często by spamowało
            }, 20L, 20L); // Start po 1 sek, powtarzaj co 1 sek
            
            getLogger().info(PLUGIN_PREFIX + "Shared Crafting module initialized!");

            // === INICJALIZACJA ROZSZERZONEGO PIECA (Enhanced Furnace) ===
            this.enhancedFurnaceManager = new EnhancedFurnaceManager(this);
            this.enhancedFurnaceManager.loadData();
            this.enhancedFurnaceManager.initializeActiveFurnaces(); // Optymalizacja: tylko aktywne piece są tickowane
            
            // Wyczyść cache przepisów przy starcie - zapobiega staym danym z poprzedniej wersji pluginu
            try {
                NmsFurnaceEngineDirect.clearCache();
                getLogger().info(PLUGIN_PREFIX + "NMS recipe cache cleared!");
            } catch (NoClassDefFoundError e) {
                // NMS nie jest dostępne (np. w testach MockBukkit)
            }
            
            // Listener interakcji z piecem
            EnhancedFurnaceListener furnaceListener = new EnhancedFurnaceListener(this, this.enhancedFurnaceManager);
            getServer().getPluginManager().registerEvents(furnaceListener, this);
            
            // NMS Handlers dla lejków - tylko na prawdziwym serwerze (nie w MockBukkit)
            try {
                // NMS Handler dla lejków - blokuje vanilla transfer do naszych pieców
                EnhancedFurnaceHopperHandler hopperHandler = new EnhancedFurnaceHopperHandler(this, this.enhancedFurnaceManager);
                getServer().getPluginManager().registerEvents(hopperHandler, this);
                
                // NMS Tick Handler dla lejków - wykonuje transfer co 8 ticków (jak vanilla)
                EnhancedFurnaceHopperTickHandler hopperTickHandler = new EnhancedFurnaceHopperTickHandler(this, this.enhancedFurnaceManager);
                hopperTickHandler.start();
                
                getLogger().info(PLUGIN_PREFIX + "NMS Hopper handlers initialized!");
            } catch (NoClassDefFoundError | UnsupportedOperationException e) {
                // NMS nie jest dostępne (np. w testach MockBukkit)
                getLogger().warning(PLUGIN_PREFIX + "NMS not available - hopper integration disabled. This is normal in test environments.");
            }
            
            // Logika smażenia - uruchamiana co tick
            this.enhancedFurnaceLogic = new EnhancedFurnaceLogic(this, this.enhancedFurnaceManager);
            getServer().getScheduler().runTaskTimer(this, () -> {
                this.enhancedFurnaceLogic.tickAllFurnaces();
            }, 1L, 1L); // Start po 1 ticku, powtarzaj co tick
            
            // Okresowa walidacja pieców - co 1 sekundę
            // Sprawdza czy bloki nadal są piecami, jeśli nie - dropuje itemy
            getServer().getScheduler().runTaskTimer(this, () -> {
                this.enhancedFurnaceManager.validateAndCleanupInvalidFurnaces();
            }, 20L, 20L);
            
            // Rejestracja komendy /efurnace
            var furnaceCmd = getCommand("efurnace");
            if (furnaceCmd != null) {
                FurnaceCommand furnaceCommand = new FurnaceCommand(this);
                furnaceCommand.setFurnaceManager(this.enhancedFurnaceManager);
                furnaceCmd.setExecutor(furnaceCommand);
                furnaceCmd.setTabCompleter(furnaceCommand);
            } else {
                getLogger().warning(PLUGIN_PREFIX + "Command 'efurnace' is not registered in plugin.yml!");
            }
            
            // Rejestracja komendy /sharedcrafting (tylko jeśli WorldEdit jest dostępny)
            var sharedCraftingCmd = getCommand("sharedcrafting");
            if (sharedCraftingCmd != null) {
                SharedCraftingCommand sharedCraftingCommand = new SharedCraftingCommand(this, this.sharedCraftingManager);
                sharedCraftingCmd.setExecutor(sharedCraftingCommand);
                sharedCraftingCmd.setTabCompleter(sharedCraftingCommand);
                
                // Loguj informację o WorldEdit
                if (WorldEditIntegration.isWorldEditAvailable()) {
                    getLogger().info(PLUGIN_PREFIX + "WorldEdit detected - fill commands enabled!");
                } else {
                    getLogger().info(PLUGIN_PREFIX + "WorldEdit not found - fill commands disabled.");
                }
            }
            
            getLogger().info(PLUGIN_PREFIX + "Enhanced Furnace module initialized!");


            getLogger().info(PLUGIN_PREFIX + "§aPlugin fully loaded!");
        } catch (Exception e) {
            getLogger().severe(PLUGIN_PREFIX + "Startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        // Wyrejestruj packet handlery dla wszystkich graczy
        if (this.recipeBookPacketHandler != null) {
            this.recipeBookPacketHandler.unregisterAllPlayers();
        }
        
        if (this.sharedCraftingManager != null) {
            this.sharedCraftingManager.closeAllInventories();
            this.sharedCraftingManager.saveData();
        }
        
        // Zapisz dane rozszerzonych pieców
        if (this.enhancedFurnaceManager != null) {
            this.enhancedFurnaceManager.closeAllInventories();
            this.enhancedFurnaceManager.saveData();
        }
        
        getLogger().info(PLUGIN_PREFIX + "Plugin is shutting down!");
    }
}



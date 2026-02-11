package pl.com.sofitfy.enhanced_containers.sharedcrafting;

import org.bukkit.plugin.java.JavaPlugin;
import pl.com.sofitfy.enhanced_containers.integration.WorldEditIntegration;

/**
 * Główna klasa pluginu SharedCrafting.
 * Zarządza współdzielonymi stołami rzemieślniczymi (persistent crafting tables).
 */
public class SharedCraftingMain extends JavaPlugin {

    // Prefix do wiadomości w chacie
    public static final String PLUGIN_PREFIX = "§6[SharedCrafting] §r";

    private SharedCraftingManager sharedCraftingManager;
    private Object recipeBookPacketHandler; // NMS - ładowany refleksyjnie

    @Override
    public void onEnable() {
        getLogger().info(PLUGIN_PREFIX + "Plugin is starting!");

        try {
            // Inicjalizacja menedżera współdzielonych stołów
            this.sharedCraftingManager = new SharedCraftingManager(this);
            this.sharedCraftingManager.loadData();

            // Listener zdarzeń (kliknięcia, otwarcia, zniszczenie bloku)
            SharedCraftingListener sharedCraftingListener = new SharedCraftingListener(this.sharedCraftingManager);
            getServer().getPluginManager().registerEvents(sharedCraftingListener, this);

            // Inicjalizacja NMS packet handlera dla Recipe Book
            // W środowisku testowym (MockBukkit) NMS nie jest dostępny - pomijamy
            boolean isMockBukkit = getServer().getClass().getName().contains("MockBukkit") ||
                                   getServer().getClass().getName().contains("ServerMock");

            if (!isMockBukkit) {
                try {
                    // Ładujemy NMS handler refleksyjnie - żeby plugin kompilował się bez NMS
                    Class<?> handlerClass = Class.forName(
                        "pl.com.sofitfy.enhanced_containers.sharedcrafting.nms.RecipeBookPacketHandler");
                    var constructor = handlerClass.getConstructor(JavaPlugin.class, SharedCraftingManager.class);
                    this.recipeBookPacketHandler = constructor.newInstance(this, this.sharedCraftingManager);

                    // Rejestruj listener pakietów
                    Class<?> listenerClass = Class.forName(
                        "pl.com.sofitfy.enhanced_containers.sharedcrafting.nms.RecipeBookPacketListener");
                    var listenerConstructor = listenerClass.getConstructor(handlerClass);
                    var recipeBookListener = listenerConstructor.newInstance(this.recipeBookPacketHandler);
                    getServer().getPluginManager().registerEvents(
                        (org.bukkit.event.Listener) recipeBookListener, this);

                    // Zarejestruj handlery dla graczy którzy już są online (hot-reload)
                    handlerClass.getMethod("registerAllPlayers").invoke(this.recipeBookPacketHandler);
                    getLogger().info(PLUGIN_PREFIX + "Recipe Book NMS packet handler initialized!");
                } catch (Exception | NoClassDefFoundError e) {
                    getLogger().warning(PLUGIN_PREFIX + "Recipe Book NMS handler failed: " + e.getMessage());
                    this.recipeBookPacketHandler = null;
                }
            } else {
                getLogger().info(PLUGIN_PREFIX + "Recipe Book NMS handler skipped (MockBukkit detected)");
                this.recipeBookPacketHandler = null;
            }

            // Okresowa walidacja stołów - co 1 sekundę (20 ticków)
            // Sprawdza czy bloki nadal są CRAFTING_TABLE, jeśli nie - dropuje itemy
            getServer().getScheduler().runTaskTimer(this, () -> {
                this.sharedCraftingManager.validateAndCleanupInvalidWorkbenches();
            }, 20L, 20L);

            // Rejestracja komendy /sharedcrafting
            var sharedCraftingCmd = getCommand("sharedcrafting");
            if (sharedCraftingCmd != null) {
                SharedCraftingCommand sharedCraftingCommand =
                    new SharedCraftingCommand(this, this.sharedCraftingManager);
                sharedCraftingCmd.setExecutor(sharedCraftingCommand);
                sharedCraftingCmd.setTabCompleter(sharedCraftingCommand);

                if (WorldEditIntegration.isWorldEditAvailable()) {
                    getLogger().info(PLUGIN_PREFIX + "WorldEdit detected - fill commands enabled!");
                } else {
                    getLogger().info(PLUGIN_PREFIX + "WorldEdit not found - fill commands disabled.");
                }
            }

            getLogger().info(PLUGIN_PREFIX + "§aPlugin fully loaded!");
        } catch (Exception e) {
            getLogger().severe(PLUGIN_PREFIX + "Startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        // Wyrejestruj NMS packet handlery
        if (this.recipeBookPacketHandler != null) {
            try {
                this.recipeBookPacketHandler.getClass()
                    .getMethod("unregisterAllPlayers")
                    .invoke(this.recipeBookPacketHandler);
            } catch (Exception e) {
                getLogger().warning("Failed to unregister packet handlers: " + e.getMessage());
            }
        }

        if (this.sharedCraftingManager != null) {
            this.sharedCraftingManager.closeAllInventories();
            this.sharedCraftingManager.saveData();
        }

        getLogger().info(PLUGIN_PREFIX + "Plugin is shutting down!");
    }

    /**
     * Zwraca menedżera współdzielonych stołów (do użytku w testach i komendach).
     */
    public SharedCraftingManager getSharedCraftingManager() {
        return sharedCraftingManager;
    }
}

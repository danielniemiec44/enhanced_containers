package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.plugin.java.JavaPlugin;
import pl.com.sofitfy.enhanced_containers.integration.WorldEditIntegration;

/**
 * Główna klasa pluginu EnhancedFurnaces.
 * Dodaje rozszerzone piece z 3 liniami smażenia, obsługą lejków i GUI z postępem.
 */
public class EnhancedFurnacesMain extends JavaPlugin {

    // Prefix do wiadomości w chacie
    public static final String PLUGIN_PREFIX = "§6[EnhancedFurnaces] §r";

    private EnhancedFurnaceManager enhancedFurnaceManager;
    private EnhancedFurnaceLogic enhancedFurnaceLogic;

    @Override
    public void onEnable() {
        getLogger().info(PLUGIN_PREFIX + "Plugin is starting!");

        try {
            // Inicjalizacja menedżera pieców
            this.enhancedFurnaceManager = new EnhancedFurnaceManager(this);
            this.enhancedFurnaceManager.loadData();
            this.enhancedFurnaceManager.initializeActiveFurnaces();

            // Wyczyść cache przepisów NMS przy starcie
            try {
                Class<?> nmsEngine = Class.forName(
                    "pl.com.sofitfy.enhanced_containers.furnace.nms.NmsFurnaceEngineDirect");
                nmsEngine.getMethod("clearCache").invoke(null);
                getLogger().info(PLUGIN_PREFIX + "NMS recipe cache cleared!");
            } catch (Exception | NoClassDefFoundError e) {
                // NMS nie jest dostępne (np. w testach MockBukkit) - ignorujemy
            }

            // Listener interakcji z piecem
            EnhancedFurnaceListener furnaceListener =
                new EnhancedFurnaceListener(this, this.enhancedFurnaceManager);
            getServer().getPluginManager().registerEvents(furnaceListener, this);

            // NMS Handlers dla lejków - tylko na prawdziwym serwerze
            try {
                // NMS Handler - blokuje vanilla transfer do naszych pieców
                Class<?> hopperHandlerClass = Class.forName(
                    "pl.com.sofitfy.enhanced_containers.furnace.nms.EnhancedFurnaceHopperHandler");
                var hopperHandler = hopperHandlerClass
                    .getConstructor(JavaPlugin.class, EnhancedFurnaceManager.class)
                    .newInstance(this, this.enhancedFurnaceManager);
                getServer().getPluginManager().registerEvents(
                    (org.bukkit.event.Listener) hopperHandler, this);

                // NMS Tick Handler - transfer co 8 ticków (jak vanilla)
                Class<?> tickHandlerClass = Class.forName(
                    "pl.com.sofitfy.enhanced_containers.furnace.nms.EnhancedFurnaceHopperTickHandler");
                var tickHandler = tickHandlerClass
                    .getConstructor(JavaPlugin.class, EnhancedFurnaceManager.class)
                    .newInstance(this, this.enhancedFurnaceManager);
                tickHandlerClass.getMethod("start").invoke(tickHandler);

                getLogger().info(PLUGIN_PREFIX + "NMS Hopper handlers initialized!");
            } catch (Exception | NoClassDefFoundError e) {
                getLogger().warning(PLUGIN_PREFIX +
                    "NMS not available - hopper integration disabled. Normal in test environments.");
            }

            // Logika smażenia - uruchamiana co tick
            this.enhancedFurnaceLogic = new EnhancedFurnaceLogic(this, this.enhancedFurnaceManager);
            getServer().getScheduler().runTaskTimer(this, () -> {
                this.enhancedFurnaceLogic.tickAllFurnaces();
            }, 1L, 1L);

            // Okresowa walidacja pieców - co 1 sekundę
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

                if (WorldEditIntegration.isWorldEditAvailable()) {
                    getLogger().info(PLUGIN_PREFIX + "WorldEdit detected - fill commands enabled!");
                } else {
                    getLogger().info(PLUGIN_PREFIX + "WorldEdit not found - fill commands disabled.");
                }
            } else {
                getLogger().warning(PLUGIN_PREFIX + "Command 'efurnace' not registered in plugin.yml!");
            }

            getLogger().info(PLUGIN_PREFIX + "§aPlugin fully loaded!");
        } catch (Exception e) {
            getLogger().severe(PLUGIN_PREFIX + "Startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        // Zapisz dane rozszerzonych pieców
        if (this.enhancedFurnaceManager != null) {
            this.enhancedFurnaceManager.closeAllInventories();
            this.enhancedFurnaceManager.saveData();
        }

        getLogger().info(PLUGIN_PREFIX + "Plugin is shutting down!");
    }

    /**
     * Zwraca menedżera pieców (do użytku w testach i komendach).
     */
    public EnhancedFurnaceManager getEnhancedFurnaceManager() {
        return enhancedFurnaceManager;
    }

    /**
     * Zwraca logikę smażenia (do użytku w testach).
     */
    public EnhancedFurnaceLogic getEnhancedFurnaceLogic() {
        return enhancedFurnaceLogic;
    }
}

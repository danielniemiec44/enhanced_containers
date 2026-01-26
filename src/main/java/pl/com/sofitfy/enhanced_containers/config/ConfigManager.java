package pl.com.sofitfy.enhanced_containers.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        loadConfig();
        plugin.getLogger().info("Config reloaded!");
    }

    @SuppressWarnings("unchecked")
    public CraftingConfig getCraftingConfig() {
        String title = config.getString("crafting.gui.title", "Crafting Station");
        int size = config.getInt("crafting.gui.size", 45);
        List<?> recipesList = config.getList("crafting.recipes");
        List<Map<String, Object>> recipes = recipesList != null ?
            (List<Map<String, Object>>) recipesList : List.of();

        return new CraftingConfig(title, size, recipes);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}


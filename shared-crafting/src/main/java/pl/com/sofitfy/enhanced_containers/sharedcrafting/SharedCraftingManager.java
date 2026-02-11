package pl.com.sofitfy.enhanced_containers.sharedcrafting;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SharedCraftingManager {

    private final JavaPlugin plugin;
    private final Map<Location, Inventory> persistentWorkbenches = new ConcurrentHashMap<>();
    private final File dataFile;

    public SharedCraftingManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "shared_workbenches.yml");
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Pobiera istniejący ekwipunek dla stołu rzemieślniczego lub tworzy nowy.
     * Metoda zapewnia, że wszyscy gracze korzystają z tej samej instancji w pamięci.
     * Thread-safe dzięki użyciu computeIfAbsent.
     */
    public Inventory getWorkbenchInventory(Location location) {
        return persistentWorkbenches.computeIfAbsent(location, 
            loc -> Bukkit.createInventory(null, InventoryType.WORKBENCH));
    }

    public boolean hasInventory(Location location) {
        return persistentWorkbenches.containsKey(location);
    }

    public boolean hasInventory(Inventory inventory) {
        return persistentWorkbenches.containsValue(inventory);
    }

    public void removeInventory(Location location) {
        persistentWorkbenches.remove(location);
    }
    
    /**
     * Zwraca lokalizację dla danego inventory (odwrotne wyszukiwanie).
     * @param inventory Inventory do wyszukania
     * @return Lokalizacja lub null jeśli nie znaleziono
     */
    public Location getLocationForInventory(Inventory inventory) {
        for (Map.Entry<Location, Inventory> entry : persistentWorkbenches.entrySet()) {
            if (entry.getValue() == inventory) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Zwraca kopię wszystkich lokacji stołów rzemieślniczych.
     * Używane do walidacji czy bloki nadal istnieją.
     */
    public Set<Location> getAllLocations() {
        return Set.copyOf(persistentWorkbenches.keySet());
    }
    
    /**
     * Waliduje wszystkie zapisane stoły rzemieślnicze.
     * Jeśli blok na danej lokacji nie jest już CRAFTING_TABLE, dropuje itemy i usuwa z mapy.
     * @return liczba usuniętych stołów
     */
    public int validateAndCleanupInvalidWorkbenches() {
        List<Location> toRemove = new ArrayList<>();
        
        for (Map.Entry<Location, Inventory> entry : persistentWorkbenches.entrySet()) {
            Location loc = entry.getKey();
            
            // Sprawdź czy świat jest załadowany
            if (loc.getWorld() == null) {
                toRemove.add(loc);
                continue;
            }
            
            // Sprawdź czy chunk jest załadowany (nie ładuj na siłę)
            // UWAGA: W niektórych implementacjach (MockBukkit) isChunkLoaded może zwracać false
            // nawet gdy chunk jest faktycznie załadowany, więc sprawdzamy też getBlock()
            try {
                if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    // Chunk nie załadowany - pomijamy (sprawdzimy później)
                    continue;
                }
            } catch (Exception e) {
                // Ignoruj błędy - spróbuj sprawdzić blok bezpośrednio
            }
            
            // Sprawdź czy blok nadal jest CRAFTING_TABLE
            if (loc.getBlock().getType() != Material.CRAFTING_TABLE) {
                Inventory inv = entry.getValue();
                
                // Dropuj wszystkie itemy na ziemię (slot 0 = wynik, pomijamy)
                for (int i = 1; i < inv.getContents().length; i++) {
                    ItemStack item = inv.getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        // Użyj clone() Location żeby nie modyfikować klucza mapy
                        Location dropLoc = loc.clone().add(0.5, 0.5, 0.5);
                        loc.getWorld().dropItemNaturally(dropLoc, item);
                    }
                }
                
                // Wyczyść inventory
                inv.clear();
                
                // Zamknij inventory dla wszystkich viewerów
                new ArrayList<>(inv.getViewers()).forEach(human -> human.closeInventory());
                
                toRemove.add(loc);
            }
        }
        
        // Usuń nieaktualne wpisy
        for (Location loc : toRemove) {
            persistentWorkbenches.remove(loc);
        }
        
        if (!toRemove.isEmpty()) {
            plugin.getLogger().info("Usunięto " + toRemove.size() + " nieaktualnych stołów rzemieślniczych (blok zmieniony).");
        }
        
        return toRemove.size();
    }

    /**
     * Wczytuje dane stołów rzemieślniczych z pliku przy starcie serwera.
     */
    public void loadData() {
        if (!dataFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = config.getConfigurationSection("workbenches");

        if (root == null) return;

        Set<String> keys = root.getKeys(false);
        for (String key : keys) {
            try {
                // Parsowanie klucza lokalizacji (format: world_x_y_z)
                String[] parts = key.split("_");
                if (parts.length < 4) continue;

                String worldName = parts[0];
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Pominięto stół rzemieślniczy: Świat " + worldName + " nie został odnaleziony.");
                    continue;
                }

                Location loc = new Location(world, x, y, z);
                
                // WALIDACJA: Sprawdź czy blok nadal jest CRAFTING_TABLE
                if (loc.getBlock().getType() != Material.CRAFTING_TABLE) {
                    plugin.getLogger().info("Pominięto stół rzemieślniczy na " + key + " - blok nie jest już CRAFTING_TABLE");
                    continue;
                }
                
                // Wczytywanie przedmiotów
                List<ItemStack> items = (List<ItemStack>) root.get(key);
                if (items != null) {
                    Inventory inv = Bukkit.createInventory(null, InventoryType.WORKBENCH);
                    ItemStack[] contents = items.toArray(new ItemStack[0]);
                    
                    // Upewniamy się, że tablica pasuje do rozmiaru (10 slotów dla WORKBENCH)
                    if (contents.length <= inv.getSize()) {
                        inv.setContents(contents);
                    } else {
                        // Jeśli z jakiegoś powodu jest więcej, docinamy
                        for (int i=0; i < inv.getSize(); i++) {
                            inv.setItem(i, contents[i]);
                        }
                    }
                    persistentWorkbenches.put(loc, inv);
                }

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas wczytywania stołu rzemieślniczego: " + key, e);
            }
        }
        plugin.getLogger().info("Wczytano " + persistentWorkbenches.size() + " współdzielonych stołów rzemieślniczych.");
    }

    /**
     * Zapisuje wszystkie aktywne stoły rzemieślnicze do pliku przy wyłączeniu serwera.
     * UWAGA: Ta metoda powinna być wywoływana tylko z głównego wątku serwera.
     */
    public void saveData() {
        FileConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection("workbenches");

        // Tworzymy kopię snapshot mapy, aby uniknąć problemów z concurrent modification
        // ConcurrentHashMap.entrySet() zwraca weakly consistent iterator, ale dla pewności kopiujemy
        Map<Location, Inventory> snapshot = new ConcurrentHashMap<>(persistentWorkbenches);
        
        for (Map.Entry<Location, Inventory> entry : snapshot.entrySet()) {
            Location loc = entry.getKey();
            Inventory inv = entry.getValue();

            // Tworzymy unikalny klucz dla lokalizacji
            if (loc.getWorld() == null) continue;
            
            String key = loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
            
            // Zapisujemy zawartość ekwipunku bezpośrednio
            // Bukkit automatycznie serializuje List<ItemStack>
            root.set(key, inv.getContents());
        }

        try {
            config.save(dataFile);
            plugin.getLogger().info("Zapisano dane współdzielonych stołów rzemieślniczych.");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie udało się zapisać danych stołów rzemieślniczych!", e);
        }
    }
    
    /**
     * Metoda pomocnicza zamykająca ekwipunki wszystkim graczom view
     * Użyteczna przy przeładowaniu pluginu.
     * UWAGA: Ta metoda powinna być wywoływana tylko z głównego wątku serwera.
     */
    public void closeAllInventories() {
        // Tworzymy kopię values(), aby uniknąć ConcurrentModificationException
        // podczas gdy closeInventory() modyfikuje viewers
        List<Inventory> inventoriesCopy = new ArrayList<>(persistentWorkbenches.values());
        
        for (Inventory inv : inventoriesCopy) {
            // Kopiujemy listę viewerów, aby uniknąć ConcurrentModificationException
            new ArrayList<>(inv.getViewers()).forEach(human -> human.closeInventory());
        }
    }
}

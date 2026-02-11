package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EnhancedFurnaceManager - zarządza rozszerzonymi piecami
 * 
 * Odpowiada za:
 * - Tworzenie i przechowywanie GUI pieców
 * - Persystencję danych pieców (zapis/odczyt)
 * - Sprawdzanie czy blok jest rozszerzonym piecem
 * - Dropowanie przedmiotów przy zniszczeniu pieca
 * - Optymalizacja: tylko piece w załadowanych chunkach są tickowane
 */
public class EnhancedFurnaceManager {
    
    private final JavaPlugin plugin;
    
    // Mapa lokalizacja -> GUI pieca (WSZYSTKIE piece)
    private final Map<Location, EnhancedFurnaceData> furnaces = new ConcurrentHashMap<>();
    
    // Set AKTYWNYCH pieców (tylko w załadowanych chunkach) - tickowane co tick
    // Aktualizowany przy ChunkLoad/ChunkUnload - zero iteracji po wszystkich piecach
    private final Set<Location> activeFurnaces = ConcurrentHashMap.newKeySet();
    
    // Klucz do oznaczania bloków jako enhanced furnace (w PersistentDataContainer)
    // Używamy BlockState.getTileState() dla bloków furnace
    private final NamespacedKey enhancedFurnaceKey;
    
    // Plik do przechowywania danych pieców
    private File dataFile;
    
    public EnhancedFurnaceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.enhancedFurnaceKey = new NamespacedKey(plugin, "enhanced_furnace");
        this.dataFile = new File(plugin.getDataFolder(), "furnaces.yml");
    }
    
    /**
     * Sprawdza czy blok jest rozszerzonym piecem (ma flagę)
     */
    public boolean isEnhancedFurnace(Block block) {
        if (block == null) return false;
        
        // Musi być piecem (FURNACE, BLAST_FURNACE lub SMOKER)
        Material type = block.getType();
        if (type != Material.FURNACE && type != Material.BLAST_FURNACE && type != Material.SMOKER) {
            return false;
        }
        
        // Sprawdź czy mamy dane dla tej lokalizacji
        Location loc = block.getLocation();
        return furnaces.containsKey(loc);
    }
    
    /**
     * Oznacza blok pieca jako rozszerzony piec
     */
    public void markAsEnhancedFurnace(Block block) {
        if (block == null) return;
        
        Material type = block.getType();
        if (type != Material.FURNACE && type != Material.BLAST_FURNACE && type != Material.SMOKER) {
            return;
        }
        
        Location loc = block.getLocation();
        if (!furnaces.containsKey(loc)) {
            // Stwórz nowe dane pieca
            String title = FurnaceGUITitleFormatter.createFurnaceTitle();
            EnhancedFurnaceGUI gui = new EnhancedFurnaceGUI(title);
            EnhancedFurnaceData data = new EnhancedFurnaceData(gui, block.getType());
            furnaces.put(loc, data);
            
            // Dodaj do aktywnych jeśli chunk jest załadowany
            if (loc.isChunkLoaded()) {
                activeFurnaces.add(loc);
            }
            
            plugin.getLogger().info("[EnhancedFurnace] Marked furnace at " + formatLocation(loc));
        }
    }
    
    /**
     * Usuwa oznaczenie rozszerzonego pieca (np. przy zniszczeniu)
     * Zwraca przedmioty do upuszczenia WRAZ z enhanced furnace itemem
     * 
     * WAŻNE: Zamyka GUI wszystkim graczom którzy je mają otwarte!
     * To zapobiega duplikacji przedmiotów.
     */
    public ItemStack[] removeEnhancedFurnace(Block block) {
        if (block == null) return new ItemStack[0];
        
        Location loc = block.getLocation();
        EnhancedFurnaceData data = furnaces.remove(loc);
        activeFurnaces.remove(loc); // Usuń też z aktywnych
        
        if (data != null) {
            plugin.getLogger().info("[EnhancedFurnace] Removed furnace at " + formatLocation(loc));
            
            // WAŻNE: Zamknij GUI wszystkim graczom którzy je mają otwarte
            // To zapobiega duplikacji przedmiotów!
            closeInventoryForAllViewers(data.getGui().getInventory());
            
            // Pobierz przedmioty z inventory
            ItemStack[] inventoryItems = data.getGui().getAllItems();
            
            // Stwórz enhanced furnace item z flagą PDC (żeby po postawieniu znowu był enhanced)
            ItemStack enhancedFurnaceItem = createEnhancedFurnaceItem(data.getFurnaceType());
            
            // Połącz przedmioty: enhanced furnace + przedmioty z inventory
            ItemStack[] allItems = new ItemStack[inventoryItems.length + 1];
            allItems[0] = enhancedFurnaceItem;
            System.arraycopy(inventoryItems, 0, allItems, 1, inventoryItems.length);
            
            return allItems;
        }
        
        return new ItemStack[0];
    }
    
    /**
     * Usuwa piec po lokalizacji (bez dropowania przedmiotów)
     * Używane gdy piec został usunięty przez WorldEdit, komendy, itp.
     * Zamyka GUI wszystkim graczom i czyści dane.
     * 
     * @param loc lokalizacja pieca
     */
    public void removeFurnaceByLocation(Location loc) {
        if (loc == null) return;
        
        EnhancedFurnaceData data = furnaces.remove(loc);
        activeFurnaces.remove(loc);
        
        if (data != null) {
            plugin.getLogger().info("[EnhancedFurnace] Furnace removed externally at " + formatLocation(loc));
            
            // Zamknij GUI wszystkim graczom
            closeInventoryForAllViewers(data.getGui().getInventory());
        }
    }
    
    /**
     * Tworzy ItemStack enhanced furnace z flagą PDC
     * Po postawieniu tego przedmiotu, piec automatycznie stanie się enhanced
     */
    public ItemStack createEnhancedFurnaceItem(Material furnaceType) {
        ItemStack item = new ItemStack(furnaceType, 1);
        var meta = item.getItemMeta();
        if (meta != null) {
            // Oznacz jako enhanced furnace
            meta.getPersistentDataContainer().set(enhancedFurnaceKey, 
                org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            
            // Dodaj wyróżnik wizualny - nazwa i lore
            meta.setDisplayName("§6Enhanced Furnace");
            meta.setLore(java.util.Arrays.asList(
                "§7A special furnace with",
                "§73 input, 3 output and 3 fuel slots"
            ));
            
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Pobiera GUI pieca dla danej lokalizacji
     * Tworzy nowe jeśli nie istnieje
     */
    public EnhancedFurnaceGUI getOrCreateFurnaceGUI(Location location) {
        EnhancedFurnaceData data = furnaces.get(location);
        if (data != null) {
            return data.getGui();
        }
        return null;
    }
    
    /**
     * Otwiera GUI pieca dla gracza
     */
    public void openFurnaceGUI(Player player, Block block) {
        if (!isEnhancedFurnace(block)) {
            return;
        }
        
        Location loc = block.getLocation();
        EnhancedFurnaceData data = furnaces.get(loc);
        
        if (data != null) {
            data.getGui().open(player);
        }
    }
    
    /**
     * Sprawdza czy inventory należy do rozszerzonego pieca
     */
    public boolean hasInventory(Inventory inventory) {
        if (inventory == null) return false;
        
        // Sprawdź czy holder to EnhancedFurnaceGUI
        if (inventory.getHolder() instanceof EnhancedFurnaceGUI) {
            return true;
        }
        
        // Sprawdź przez porównanie inventory
        for (EnhancedFurnaceData data : furnaces.values()) {
            if (data.getGui().getInventory().equals(inventory)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Pobiera lokalizację pieca dla danego inventory
     */
    public Location getLocationForInventory(Inventory inventory) {
        if (inventory == null) return null;
        
        for (Map.Entry<Location, EnhancedFurnaceData> entry : furnaces.entrySet()) {
            if (entry.getValue().getGui().getInventory().equals(inventory)) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    /**
     * Zamyka inventory dla wszystkich graczy którzy je mają otwarte
     * WAŻNE: Zapobiega duplikacji przedmiotów gdy piec jest niszczony!
     * 
     * @param inventory inventory do zamknięcia
     */
    public void closeInventoryForAllViewers(Inventory inventory) {
        if (inventory == null) return;
        
        // Kopiujemy listę viewerów bo closeInventory modyfikuje ją
        java.util.List<org.bukkit.entity.HumanEntity> viewers = 
            new java.util.ArrayList<>(inventory.getViewers());
        
        for (org.bukkit.entity.HumanEntity viewer : viewers) {
            viewer.closeInventory();
        }
    }
    
    /**
     * Pobiera dane pieca dla danego inventory
     */
    public EnhancedFurnaceData getDataByInventory(Inventory inventory) {
        if (inventory == null) return null;
        
        for (EnhancedFurnaceData data : furnaces.values()) {
            if (data.getGui().getInventory().equals(inventory)) {
                return data;
            }
        }
        
        return null;
    }
    
    /**
     * Pobiera dane pieca dla danej lokalizacji
     */
    public EnhancedFurnaceData getFurnaceData(Location location) {
        return furnaces.get(location);
    }
    
    /**
     * Sprawdza czy pod daną lokalizacją jest Enhanced Furnace
     */
    public boolean hasFurnaceAt(Location location) {
        return furnaces.containsKey(location);
    }
    
    /**
     * Pobiera wszystkie lokalizacje pieców (do zapisu/odczytu)
     */
    public Set<Location> getAllFurnaceLocations() {
        return furnaces.keySet();
    }
    
    /**
     * Pobiera TYLKO aktywne piece (w załadowanych chunkach) - do tickowania
     * Ta metoda jest O(1) - zwraca gotowy Set, bez iteracji
     */
    public Set<Location> getActiveFurnaceLocations() {
        return activeFurnaces;
    }
    
    /**
     * Wywołaj gdy chunk zostanie załadowany
     * Dodaje piece z tego chunka do aktywnych
     */
    public void onChunkLoad(Chunk chunk) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        
        // Znajdź wszystkie piece w tym chunku
        for (Location loc : furnaces.keySet()) {
            if (loc.getWorld() != null && loc.getWorld().equals(world) &&
                (loc.getBlockX() >> 4) == chunkX && (loc.getBlockZ() >> 4) == chunkZ) {
                activeFurnaces.add(loc);
            }
        }
    }
    
    /**
     * Wywołaj gdy chunk zostanie odładowany
     * Usuwa piece z tego chunka z aktywnych
     */
    public void onChunkUnload(Chunk chunk) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        
        // Usuń piece z tego chunka z aktywnych
        activeFurnaces.removeIf(loc -> 
            loc.getWorld() != null && loc.getWorld().equals(world) &&
            (loc.getBlockX() >> 4) == chunkX && (loc.getBlockZ() >> 4) == chunkZ
        );
    }
    
    /**
     * Inicjalizuje aktywne piece na podstawie aktualnie załadowanych chunków
     * Wywołaj po załadowaniu danych pieców
     */
    public void initializeActiveFurnaces() {
        activeFurnaces.clear();
        for (Location loc : furnaces.keySet()) {
            if (loc.isChunkLoaded()) {
                activeFurnaces.add(loc);
            }
        }
        plugin.getLogger().info("[EnhancedFurnace] Initialized " + activeFurnaces.size() + " active furnaces (of " + furnaces.size() + " total)");
    }
    
    /**
     * Zapisuje dane wszystkich pieców do pliku
     */
    public void saveData() {
        FileConfiguration config = new YamlConfiguration();
        
        int index = 0;
        for (Map.Entry<Location, EnhancedFurnaceData> entry : furnaces.entrySet()) {
            Location loc = entry.getKey();
            EnhancedFurnaceData data = entry.getValue();
            
            String path = "furnaces." + index;
            
            // Zapisz lokalizację
            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getBlockX());
            config.set(path + ".y", loc.getBlockY());
            config.set(path + ".z", loc.getBlockZ());
            config.set(path + ".type", data.getFurnaceType().name());
            
            // Zapisz przedmioty z inventory (Bukkit ItemStack.serialize())
            EnhancedFurnaceGUI gui = data.getGui();
            
            // Input slots
            for (int i = 0; i < EnhancedFurnaceGUI.INPUT_SLOTS.length; i++) {
                ItemStack item = gui.getInputItem(i);
                if (item != null && !item.getType().isAir()) {
                    config.set(path + ".input." + i, item.serialize());
                }
            }
            
            // Output slots
            for (int i = 0; i < EnhancedFurnaceGUI.OUTPUT_SLOTS.length; i++) {
                ItemStack item = gui.getOutputItem(i);
                if (item != null && !item.getType().isAir()) {
                    config.set(path + ".output." + i, item.serialize());
                }
            }
            
            // Fuel slots
            for (int i = 0; i < EnhancedFurnaceGUI.FUEL_SLOTS.length; i++) {
                ItemStack item = gui.getFuelItem(i);
                if (item != null && !item.getType().isAir()) {
                    config.set(path + ".fuel." + i, item.serialize());
                }
            }
            
            // Zapisz stan przepalania
            config.set(path + ".burnTime", data.getBurnTime());
            config.set(path + ".maxBurnTime", data.getMaxBurnTime());
            
            for (int i = 0; i < 3; i++) {
                config.set(path + ".cookTime." + i, data.getCookTime(i));
            }
            
            index++;
        }
        
        config.set("count", index);
        
        try {
            config.save(dataFile);
            plugin.getLogger().info("[EnhancedFurnace] Saved " + index + " furnaces");
        } catch (IOException e) {
            plugin.getLogger().severe("[EnhancedFurnace] Error saving furnaces: " + e.getMessage());
        }
    }
    
    /**
     * Wczytuje dane pieców z pliku
     */
    public void loadData() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("[EnhancedFurnace] No furnace data file found");
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        int count = config.getInt("count", 0);
        
        for (int index = 0; index < count; index++) {
            String path = "furnaces." + index;
            
            try {
                String worldName = config.getString(path + ".world");
                if (worldName == null) continue;
                
                World world = plugin.getServer().getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("[EnhancedFurnace] World not found: " + worldName);
                    continue;
                }
                
                int x = config.getInt(path + ".x");
                int y = config.getInt(path + ".y");
                int z = config.getInt(path + ".z");
                Location loc = new Location(world, x, y, z);
                
                // Sprawdź czy blok nadal jest piecem
                Block block = loc.getBlock();
                Material type = block.getType();
                if (type != Material.FURNACE && type != Material.BLAST_FURNACE && type != Material.SMOKER) {
                    plugin.getLogger().warning("[EnhancedFurnace] Block at " + formatLocation(loc) + " is no longer a furnace");
                    continue;
                }
                
                // Odtwórz GUI
                String title = FurnaceGUITitleFormatter.createFurnaceTitle();
                EnhancedFurnaceGUI gui = new EnhancedFurnaceGUI(title);
                
                // Wczytaj przedmioty (Bukkit ItemStack.deserialize())
                for (int i = 0; i < EnhancedFurnaceGUI.INPUT_SLOTS.length; i++) {
                    var section = config.getConfigurationSection(path + ".input." + i);
                    if (section != null) {
                        Map<String, Object> map = section.getValues(false);
                        ItemStack item = ItemStack.deserialize(map);
                        gui.setInputItem(i, item);
                    }
                }
                
                for (int i = 0; i < EnhancedFurnaceGUI.OUTPUT_SLOTS.length; i++) {
                    var section = config.getConfigurationSection(path + ".output." + i);
                    if (section != null) {
                        Map<String, Object> map = section.getValues(false);
                        ItemStack item = ItemStack.deserialize(map);
                        gui.setOutputItem(i, item);
                    }
                }
                
                for (int i = 0; i < EnhancedFurnaceGUI.FUEL_SLOTS.length; i++) {
                    var section = config.getConfigurationSection(path + ".fuel." + i);
                    if (section != null) {
                        Map<String, Object> map = section.getValues(false);
                        ItemStack item = ItemStack.deserialize(map);
                        gui.setFuelItem(i, item);
                    }
                }
                
                // Wczytaj typ pieca
                String typeStr = config.getString(path + ".type", "FURNACE");
                Material furnaceType = Material.valueOf(typeStr);
                
                // Stwórz dane pieca
                EnhancedFurnaceData data = new EnhancedFurnaceData(gui, furnaceType);
                
                // Wczytaj stan przepalania
                data.setBurnTime(config.getInt(path + ".burnTime", 0));
                data.setMaxBurnTime(config.getInt(path + ".maxBurnTime", 0));
                
                for (int i = 0; i < 3; i++) {
                    data.setCookTime(i, config.getInt(path + ".cookTime." + i, 0));
                }
                
                furnaces.put(loc, data);
                
            } catch (Exception e) {
                plugin.getLogger().warning("[EnhancedFurnace] Error loading furnace " + index + ": " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("[EnhancedFurnace] Loaded " + furnaces.size() + " furnaces");
    }
    
    /**
     * Waliduje piece - sprawdza czy bloki nadal istnieją
     * Usuwa nieprawidłowe i dropuje przedmioty
     */
    public void validateAndCleanupInvalidFurnaces() {
        var toRemove = new java.util.ArrayList<Location>();
        
        for (Location loc : furnaces.keySet()) {
            Block block = loc.getBlock();
            Material type = block.getType();
            
            // Jeśli blok nie jest już piecem - usuń
            if (type != Material.FURNACE && type != Material.BLAST_FURNACE && type != Material.SMOKER) {
                toRemove.add(loc);
            }
        }
        
        for (Location loc : toRemove) {
            EnhancedFurnaceData data = furnaces.remove(loc);
            if (data != null) {
                // Dropuj przedmioty
                ItemStack[] items = data.getGui().getAllItems();
                World world = loc.getWorld();
                if (world != null) {
                    for (ItemStack item : items) {
                        if (item != null && item.getType() != Material.AIR) {
                            world.dropItemNaturally(loc, item);
                        }
                    }
                }
                plugin.getLogger().info("[EnhancedFurnace] Removed invalid furnace at " + formatLocation(loc));
            }
        }
    }
    
    /**
     * Zamyka wszystkie otwarte inventory pieców
     * Używane przy wyłączaniu pluginu
     */
    public void closeAllInventories() {
        for (EnhancedFurnaceData data : furnaces.values()) {
            Inventory inv = data.getGui().getInventory();
            // Zamknij dla wszystkich graczy którzy mają otwarte to inventory
            for (var viewer : new java.util.ArrayList<>(inv.getViewers())) {
                viewer.closeInventory();
            }
        }
    }
    
    private String formatLocation(Location loc) {
        return String.format("%s: %d, %d, %d", 
            loc.getWorld() != null ? loc.getWorld().getName() : "null",
            loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}

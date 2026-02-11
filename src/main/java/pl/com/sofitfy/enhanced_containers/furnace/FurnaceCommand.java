package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pl.com.sofitfy.enhanced_containers.integration.WorldEditIntegration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * FurnaceCommand - komenda do dawania rozszerzonego pieca i masowego wypełniania
 * 
 * Komendy:
 * - /efurnace [furnace|blast|smoker] - daje graczowi piec z flagą
 * - /efurnace fill <input> [fuel] [amount] - wypełnia wszystkie piece w selekcji WorldEdit
 * 
 * Uprawnienia:
 * - plethoras.furnace.give - daje piec
 * - plethoras.furnace.fill - masowe wypełnianie (wymaga WorldEdit)
 */
public class FurnaceCommand implements CommandExecutor, TabCompleter {
    
    private final JavaPlugin plugin;
    private final NamespacedKey enhancedFurnaceKey;
    
    // Manager pieców - potrzebny do fill
    private EnhancedFurnaceManager furnaceManager;
    
    // Lore przedmiotu który jest enhanced furnace
    private static final String ENHANCED_LORE = "§6Enhanced Furnace";
    
    public FurnaceCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.enhancedFurnaceKey = new NamespacedKey(plugin, "enhanced_furnace");
    }
    
    /**
     * Ustawia manager pieców - potrzebny do komendy fill
     */
    public void setFurnaceManager(EnhancedFurnaceManager manager) {
        this.furnaceManager = manager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        // Obsługa subkomendy fill
        if (args.length > 0 && args[0].equalsIgnoreCase("fill")) {
            return handleFillCommand(player, args);
        }
        
        // Standardowa komenda give
        if (!player.hasPermission("plethoras.furnace.give")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        
        // Domyślnie lub z argumentem
        Material furnaceType = Material.FURNACE;
        
        if (args.length > 0) {
            String typeArg = args[0].toLowerCase();
            furnaceType = switch (typeArg) {
                case "blast", "blast_furnace" -> Material.BLAST_FURNACE;
                case "smoker" -> Material.SMOKER;
                case "furnace" -> Material.FURNACE;
                default -> {
                    sendHelp(player);
                    yield null;
                }
            };
            
            if (furnaceType == null) {
                return true;
            }
        }
        
        // Stwórz przedmiot z flagą
        ItemStack furnaceItem = new ItemStack(furnaceType, 1);
        ItemMeta meta = furnaceItem.getItemMeta();
        
        if (meta != null) {
            // Dodaj lore żeby gracz wiedział że to enhanced
            meta.setLore(List.of(ENHANCED_LORE));
            
            // Dodaj flagę w PersistentDataContainer
            meta.getPersistentDataContainer().set(enhancedFurnaceKey, PersistentDataType.BYTE, (byte) 1);
            
            furnaceItem.setItemMeta(meta);
        }
        
        // Daj graczowi przedmiot
        player.getInventory().addItem(furnaceItem);
        player.sendMessage("§aYou received an Enhanced " + formatMaterial(furnaceType) + "!");
        
        return true;
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("§6=== Enhanced Furnace ===");
        player.sendMessage("§e/efurnace [furnace|blast|smoker] §7- Get an enhanced furnace");
        
        // Pokaż fill tylko jeśli WorldEdit jest dostępny
        if (WorldEditIntegration.isWorldEditAvailable()) {
            player.sendMessage("§e/efurnace fill <input> [fuel] [amount] §7- Fill furnaces in WorldEdit selection");
        }
    }
    
    /**
     * Obsługa komendy /efurnace fill <input> [fuel] [amount]
     * Wypełnia wszystkie piece w selekcji WorldEdit
     */
    private boolean handleFillCommand(Player player, String[] args) {
        // Sprawdź permisję
        if (!player.hasPermission("plethoras.furnace.fill")) {
            player.sendMessage("§cYou don't have permission to fill furnaces!");
            return true;
        }
        
        // Sprawdź czy WorldEdit jest dostępny
        if (!WorldEditIntegration.isWorldEditAvailable()) {
            player.sendMessage("§cWorldEdit is not installed! The fill command requires WorldEdit.");
            return true;
        }
        
        // Sprawdź czy manager jest ustawiony
        if (furnaceManager == null) {
            player.sendMessage("§cInternal error: FurnaceManager not initialized!");
            return true;
        }
        
        // Parsuj argumenty: fill <input> [fuel] [amount]
        if (args.length < 2) {
            player.sendMessage("§cUsage: /efurnace fill <input_material> [fuel_material] [amount]");
            player.sendMessage("§7Example: /efurnace fill raw_iron coal 64");
            return true;
        }
        
        // Parsuj materiał wejściowy
        Material inputMaterial = Material.matchMaterial(args[1].toUpperCase());
        if (inputMaterial == null) {
            player.sendMessage("§cUnknown material: " + args[1]);
            return true;
        }
        
        // Parsuj paliwo (domyślnie COAL)
        Material fuelMaterial = Material.COAL;
        if (args.length >= 3) {
            Material parsed = Material.matchMaterial(args[2].toUpperCase());
            if (parsed != null) {
                fuelMaterial = parsed;
            } else {
                // Może to być amount, nie fuel
                try {
                    Integer.parseInt(args[2]);
                    // To jest liczba - zostaw domyślne paliwo
                } catch (NumberFormatException e) {
                    player.sendMessage("§cUnknown fuel material: " + args[2]);
                    return true;
                }
            }
        }
        
        // Parsuj ilość (domyślnie 64)
        int amount = 64;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                amount = Math.max(1, Math.min(64, amount)); // Clamp 1-64
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount: " + args[3]);
                return true;
            }
        } else if (args.length >= 3) {
            // Może amount jest w args[2] zamiast fuel
            try {
                amount = Integer.parseInt(args[2]);
                amount = Math.max(1, Math.min(64, amount));
            } catch (NumberFormatException e) {
                // To nie była liczba - ignoruj
            }
        }
        
        // Finalne zmienne dla lambdy
        final Material finalInputMaterial = inputMaterial;
        final Material finalFuelMaterial = fuelMaterial;
        final int finalAmount = amount;
        
        player.sendMessage("§aStarting fill operation...");
        player.sendMessage("§7Input: §e" + inputMaterial.name() + " §7Fuel: §e" + fuelMaterial.name() + " §7Amount: §e" + amount);
        
        // Counter dla wypełnionych pieców
        final int[] filledCount = {0};
        
        // Iteruj przez selekcję asynchronicznie
        WorldEditIntegration.iterateSelectionAsync(
            player,
            1000, // 1000 bloków na tick - szybkie bez lagów
            (location, progress) -> {
                Block block = location.getBlock();
                Material type = block.getType();
                
                // Sprawdź czy to piec (zwykły, blast lub smoker)
                if (type == Material.FURNACE || type == Material.BLAST_FURNACE || type == Material.SMOKER) {
                    // Oznacz jako Enhanced Furnace jeśli jeszcze nie jest
                    if (!furnaceManager.isEnhancedFurnace(block)) {
                        furnaceManager.markAsEnhancedFurnace(block);
                    }
                    
                    // Wypełnij przedmiotami
                    fillFurnace(location, finalInputMaterial, finalFuelMaterial, finalAmount);
                    filledCount[0]++;
                }
            },
            (totalProcessed) -> {
                player.sendMessage("§a§lDone! §aFilled §e" + filledCount[0] + " §afurnaces.");
            },
            plugin
        );
        
        return true;
    }
    
    /**
     * Wypełnia Enhanced Furnace przedmiotami
     */
    private void fillFurnace(Location location, Material inputMaterial, Material fuelMaterial, int amount) {
        EnhancedFurnaceGUI gui = furnaceManager.getOrCreateFurnaceGUI(location);
        if (gui == null) return;
        
        // Wypełnij sloty INPUT (3 sloty)
        for (int slot : EnhancedFurnaceGUI.INPUT_SLOTS) {
            gui.getInventory().setItem(slot, new ItemStack(inputMaterial, amount));
        }
        
        // Wypełnij sloty FUEL (3 sloty)
        for (int slot : EnhancedFurnaceGUI.FUEL_SLOTS) {
            gui.getInventory().setItem(slot, new ItemStack(fuelMaterial, amount));
        }
    }
    
    private String formatMaterial(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Pierwszy argument: typ pieca lub fill
            List<String> options = new ArrayList<>(Arrays.asList("furnace", "blast", "smoker"));
            
            // Dodaj fill tylko jeśli WorldEdit jest dostępny i gracz ma permisję
            if (WorldEditIntegration.isWorldEditAvailable() && 
                sender.hasPermission("plethoras.furnace.fill")) {
                options.add("fill");
            }
            
            String partial = args[0].toLowerCase();
            for (String option : options) {
                if (option.startsWith(partial)) {
                    completions.add(option);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("fill")) {
            // Drugi argument dla fill: materiał wejściowy
            String partial = args[1].toLowerCase();
            // Sugeruj popularne materiały do przetapiania
            List<String> smeltables = Arrays.asList(
                "raw_iron", "raw_gold", "raw_copper", "cobblestone", 
                "sand", "clay_ball", "cactus", "kelp", "potato", "beef"
            );
            for (String mat : smeltables) {
                if (mat.startsWith(partial)) {
                    completions.add(mat);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("fill")) {
            // Trzeci argument dla fill: paliwo lub ilość
            String partial = args[2].toLowerCase();
            List<String> fuels = Arrays.asList("coal", "charcoal", "lava_bucket", "blaze_rod", "64", "32", "16");
            for (String fuel : fuels) {
                if (fuel.startsWith(partial)) {
                    completions.add(fuel);
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("fill")) {
            // Czwarty argument dla fill: ilość
            completions.addAll(Arrays.asList("1", "16", "32", "64"));
        }
        
        return completions;
    }
    
    /**
     * Pobiera klucz do PersistentDataContainer
     */
    public NamespacedKey getEnhancedFurnaceKey() {
        return enhancedFurnaceKey;
    }
}

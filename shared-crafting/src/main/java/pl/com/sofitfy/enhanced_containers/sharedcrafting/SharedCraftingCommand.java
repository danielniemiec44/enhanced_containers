package pl.com.sofitfy.enhanced_containers.sharedcrafting;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import pl.com.sofitfy.enhanced_containers.integration.WorldEditIntegration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SharedCraftingCommand - komenda do masowego wypełniania stołów craftingu
 * 
 * Komendy:
 * - /sharedcrafting fill <item> [amount] - wypełnia wszystkie crafting table w selekcji WorldEdit
 * 
 * Uprawnienia:
 * - plethoras.sharedcrafting.fill - masowe wypełnianie (wymaga WorldEdit), domyślnie dla OP
 */
public class SharedCraftingCommand implements CommandExecutor, TabCompleter {
    
    private final JavaPlugin plugin;
    private final SharedCraftingManager craftingManager;
    
    public SharedCraftingCommand(JavaPlugin plugin, SharedCraftingManager craftingManager) {
        this.plugin = plugin;
        this.craftingManager = craftingManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        // Wyświetl pomoc jeśli brak argumentów
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "fill":
                return handleFillCommand(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }
    
    /**
     * Wyświetla pomoc dla komendy
     */
    private void sendHelp(Player player) {
        player.sendMessage("§6=== Shared Crafting ===");
        
        // Pokaż fill tylko jeśli WorldEdit jest dostępny
        if (WorldEditIntegration.isWorldEditAvailable()) {
            player.sendMessage("§e/sharedcrafting fill <item> [amount] §7- Fill crafting tables in WorldEdit selection");
        } else {
            player.sendMessage("§7WorldEdit not installed - fill command unavailable.");
        }
    }
    
    /**
     * Obsługa komendy /sharedcrafting fill <item> [amount]
     * Wypełnia wszystkie crafting table w selekcji WorldEdit
     */
    private boolean handleFillCommand(Player player, String[] args) {
        // Sprawdź permisję
        if (!player.hasPermission("plethoras.sharedcrafting.fill")) {
            player.sendMessage("§cYou don't have permission to fill crafting tables!");
            return true;
        }
        
        // Sprawdź czy WorldEdit jest dostępny
        if (!WorldEditIntegration.isWorldEditAvailable()) {
            player.sendMessage("§cWorldEdit is not installed! The fill command requires WorldEdit.");
            return true;
        }
        
        // Parsuj argumenty: fill <item> [amount]
        if (args.length < 2) {
            player.sendMessage("§cUsage: /sharedcrafting fill <material> [amount]");
            player.sendMessage("§7Example: /sharedcrafting fill diamond 64");
            return true;
        }
        
        // Parsuj materiał
        Material material = Material.matchMaterial(args[1].toUpperCase());
        if (material == null) {
            player.sendMessage("§cUnknown material: " + args[1]);
            return true;
        }
        
        // Parsuj ilość (domyślnie 64)
        int amount = 64;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                amount = Math.max(1, Math.min(64, amount)); // Clamp 1-64
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount: " + args[2]);
                return true;
            }
        }
        
        // Finalne zmienne dla lambdy
        final Material finalMaterial = material;
        final int finalAmount = amount;
        
        player.sendMessage("§aStarting fill operation...");
        player.sendMessage("§7Material: §e" + material.name() + " §7Amount: §e" + amount);
        
        // Counter dla wypełnionych stołów
        final int[] filledCount = {0};
        
        // Iteruj przez selekcję asynchronicznie
        WorldEditIntegration.iterateSelectionAsync(
            player,
            1000, // 1000 bloków na tick - szybkie bez lagów
            (location, progress) -> {
                Block block = location.getBlock();
                
                // Sprawdź czy to crafting table
                if (block.getType() == Material.CRAFTING_TABLE) {
                    // Pobierz lub stwórz inventory dla tego stołu
                    Inventory inv = craftingManager.getWorkbenchInventory(location);
                    
                    // Wypełnij sloty 1-9 (slot 0 to wynik craftingu - nie wypełniamy)
                    // W WORKBENCH inventory: slot 0 = wynik, sloty 1-9 = grid craftingu
                    fillCraftingTable(inv, finalMaterial, finalAmount);
                    filledCount[0]++;
                }
            },
            (totalProcessed) -> {
                player.sendMessage("§a§lDone! §aFilled §e" + filledCount[0] + " §acrafting tables.");
            },
            plugin
        );
        
        return true;
    }
    
    /**
     * Wypełnia inventory stołu craftingu przedmiotami
     * 
     * @param inventory inventory stołu (WORKBENCH type)
     * @param material materiał do wypełnienia
     * @param amount ilość w każdym slocie
     */
    private void fillCraftingTable(Inventory inventory, Material material, int amount) {
        // WORKBENCH ma 10 slotów:
        // - slot 0 = wynik craftingu (nie wypełniamy!)
        // - sloty 1-9 = grid craftingu 3x3
        for (int slot = 1; slot <= 9; slot++) {
            inventory.setItem(slot, new ItemStack(material, amount));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Pierwszy argument: subkomenda
            if (WorldEditIntegration.isWorldEditAvailable() && 
                sender.hasPermission("plethoras.sharedcrafting.fill")) {
                if ("fill".startsWith(args[0].toLowerCase())) {
                    completions.add("fill");
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("fill")) {
            // Drugi argument dla fill: materiał
            String partial = args[1].toLowerCase();
            // Sugeruj popularne materiały
            List<String> materials = Arrays.asList(
                "diamond", "iron_ingot", "gold_ingot", "emerald", 
                "oak_planks", "cobblestone", "stick", "string", "leather"
            );
            for (String mat : materials) {
                if (mat.startsWith(partial)) {
                    completions.add(mat);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("fill")) {
            // Trzeci argument dla fill: ilość
            completions.addAll(Arrays.asList("1", "16", "32", "64"));
        }
        
        return completions;
    }
}

package pl.com.sofitfy.enhanced_containers.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import pl.com.sofitfy.enhanced_containers.Main;
import pl.com.sofitfy.enhanced_containers.config.ConfigManager;
import pl.com.sofitfy.enhanced_containers.crafting.gui.CraftingGUI;
import pl.com.sofitfy.enhanced_containers.crafting.gui.CraftingGUIListener;
import pl.com.sofitfy.enhanced_containers.crafting.gui.GUITitleFormatter;
import pl.com.sofitfy.enhanced_containers.crafting.gui.RecipeEditorGUI;
import pl.com.sofitfy.enhanced_containers.crafting.gui.RecipeEditorListener;
import pl.com.sofitfy.enhanced_containers.crafting.gui.RecipeSelectionGUI;
import pl.com.sofitfy.enhanced_containers.crafting.gui.RecipeSelectionListener;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeDraft;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager;
import net.kyori.adventure.text.Component;

/**
 * CraftCommand - obsługa komendy /4x4 (alias: /ecraft)
 *
 * Komenda główna do craftingu i edycji receptur
 * Obsługuje subkomendy:
 * - /4x4 - otwiera GUI craftingu 4x4
 * - /4x4 new - tworzy nową recepturę (edytor)
 * - /4x4 edit - edytuje istniejącą recepturę
 * - /4x4 sign_item <nazwa> - zmienia display name item'u w ręce
 */
public class CraftCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final CraftingGUIListener guiListener;
    private final RecipeEditorListener editorListener;
    private final RecipeSelectionListener selectionListener;

    public CraftCommand(JavaPlugin plugin, ConfigManager configManager, CraftingGUIListener guiListener, RecipeEditorListener editorListener, RecipeSelectionListener selectionListener) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.guiListener = guiListener;
        this.editorListener = editorListener;
        this.selectionListener = selectionListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Tylko gracze mogą użyć tej komendy - nie konsola
        if (!(sender instanceof Player)) {
            sender.sendMessage(Main.PLUGIN_PREFIX + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        // Sprawdzenie czy gracz ma podstawową permissję
        if (!player.hasPermission("plethoras.tools.use")) {
            player.sendMessage(Main.PLUGIN_PREFIX + "§cYou don't have permission!");
            return true;
        }

        // Brak argumentów - otwórz GUI gry
        if (args == null || args.length == 0) {
            return handlePlayCraft(player);
        }

        // Obsługa subkomend
        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "new":
                // Tylko admini mogą tworzyć nowe receptury
                if (!player.hasPermission("plethoras.tools.admin")) {
                    player.sendMessage(Main.PLUGIN_PREFIX + "§cYou don't have permission to create recipes!");
                    return true;
                }
                return handleNewRecipe(player);

            case "edit":
                // Tylko admini mogą edytować receptury
                if (!player.hasPermission("plethoras.tools.admin")) {
                    player.sendMessage(Main.PLUGIN_PREFIX + "§cYou don't have permission to edit recipes!");
                    return true;
                }
                return handleEditRecipe(player, args);

            case "sign_item":
                // Sprawdź czy gracz ma permisję do komendy sign_item
                if (!player.hasPermission("plethoras.tools.sign_item") && !player.isOp()) {
                    player.sendMessage(Main.PLUGIN_PREFIX + "§cYou don't have permission to rename items!");
                    return true;
                }
                // Sprawdź czy podano argument - nazwę item'u
                if (args.length < 2) {
                    player.sendMessage(Main.PLUGIN_PREFIX + "§cUsage: /4x4 sign_item <name>");
                    return true;
                }
                return handleSignItem(player, args);

            default:
                player.sendMessage(Main.PLUGIN_PREFIX + "§cUnknown subcommand!");
                player.sendMessage("§7Available commands: /4x4, /4x4 new, /4x4 edit, /4x4 sign_item <name>");
                return true;
        }
    }

    /**
    * Obsługa /4x4 - otwórz GUI craftingu (gracz craftuje)
     */
    private boolean handlePlayCraft(Player player) {
        if (configManager == null) {
            player.sendMessage(Main.PLUGIN_PREFIX + "§cError: ConfigManager is not initialized!");
            return true;
        }

        // Pobierz konfigurację GUI
        var craftingConfig = configManager.getCraftingConfig();
        if (craftingConfig == null) {
            player.sendMessage(Main.PLUGIN_PREFIX + "§cError: Crafting config is not loaded!");
            return true;
        }

        // Listener musi być zarejestrowany
        if (guiListener == null) {
            player.sendMessage(Main.PLUGIN_PREFIX + "§cError: Listener is not initialized!");
            return true;
        }

        // Stwórz nowe GUI dla gracza z Custom Font teksturą w tytule
        // Tytuł będzie zawierać \uE001 które wyświetli ex_crafting.png z resource packa
        // GUI ZAWSZE ma 54 sloty (9x6), bez możliwości zmiany!
        String guiTitle = GUITitleFormatter.getCraftingTitle();
        CraftingGUI gui = new CraftingGUI(player, guiTitle);

        // Zarejestruj GUI w listenera (żeby wiedział że to okienko jest otwarte)
        guiListener.registerGUI(gui);
        // Otwórz okienko graczowi
        player.openInventory(gui.getInventory());
        
        // Zaktualizuj output slot przy otwarciu (pokaż czy jest jakaś receptura dla pustego gridu)
        Bukkit.getScheduler().runTask(
            plugin,
            () -> guiListener.updateOutput(player, gui)
        );

        return true;
    }

    /**
    * Obsługa /4x4 new - stwórz nową recepturę (edytor)
     *
     * Generuje nowe ID receptury i otwiera edytor
     */
    private boolean handleNewRecipe(Player player) {
        // Wygeneruj nowe ID dla receptury
        String newRecipeId = RecipeManager.getInstance().generateNewRecipeId();

        // Stwórz draft nowej receptury
        RecipeDraft draft = new RecipeDraft(newRecipeId);

        // Stwórz GUI edytora
        RecipeEditorGUI editor = new RecipeEditorGUI(player, draft);

        // Zarejestruj edytor w listenera (żeby wiedział że edytor jest otwarty)
        if (editorListener != null) {
            editorListener.registerEditor(editor);
        }

        // Otwórz edytor graczowi
        editor.open();

        player.sendMessage(Main.PLUGIN_PREFIX + "§aOpened new recipe editor (ID: " + newRecipeId + ")");
        player.sendMessage("§7Put input items on the left, output on the right");
        player.sendMessage("§7Press Escape to save");

        return true;
    }

    /**
    * Obsługa /4x4 edit [numer_strony] - edytuj recepturę z selektora
     *
     * Wyświetla listę wszystkich receptur do edycji w GUI (z paginacją).
     * Admin klika na recepturę aby ją edytować.
     * Jeśli podany numer strony - otwiera tę stronę (zaczynam od 1).
     */
    private boolean handleEditRecipe(Player player, String[] args) {
        // Pobierz wszystkie receptury
        var recipes = RecipeManager.getInstance().getAllRecipes();

        // Sprawdzenie czy są jakieś receptury
        if (recipes.isEmpty()) {
            player.sendMessage(Main.PLUGIN_PREFIX + "§cNo recipes to edit!");
            return true;
        }

        // Spróbuj pobrać numer strony z argumentów (jeśli podany)
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(Main.PLUGIN_PREFIX + "§cInvalid page number!");
                return true;
            }
        }

        // Stwórz selektor receptur
        RecipeSelectionGUI selector = new RecipeSelectionGUI(player, page);

        // Zarejestruj selektor w listenera
        if (selectionListener != null) {
            selectionListener.registerSelector(selector);
        }

        // Otwórz selektor
        selector.open();

        player.sendMessage(Main.PLUGIN_PREFIX + "§aOpened recipe selector");
        player.sendMessage("§7Click a recipe to edit it");
        return true;
    }

    /**
    * Obsługa /4x4 sign_item <nazwa> - zmień display name item'u w ręce
     *
     * Gracz może wpisać dowolną nazwę z formatowaniem:
     * - Kody kolorów: &c (czerwony), &a (zielony), &6 (złoty) itd
     * - Automatycznie konwertuje & na § (format Minecrafta)
     */
    private boolean handleSignItem(Player player, String[] args) {
        // Pobierz item z głównej ręki gracza
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // Sprawdzenie czy gracz ma coś w ręce
        if (itemInHand == null || itemInHand.getAmount() == 0) {
            player.sendMessage(Main.PLUGIN_PREFIX + "§cYou must hold an item in your hand!");
            return true;
        }

        // Pobierz nazwę z argumentów - wszystko od drugiego argumentu
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) nameBuilder.append(" ");
            nameBuilder.append(args[i]);
        }
        String newName = nameBuilder.toString();

        // Konwertuj & na § (format Minecrafta) dla kolorów i formatowania
        newName = newName.replace("&", "§");

        // Ustaw display name na item
        ItemMeta meta = itemInHand.getItemMeta();
        if (meta == null) {
            // Jeśli meta nie istnieje - stwórz nową
            player.sendMessage(Main.PLUGIN_PREFIX + "§cThis item can't have a display name!");
            return true;
        }

        meta.displayName(Component.text(newName));
        itemInHand.setItemMeta(meta);

        player.sendMessage(Main.PLUGIN_PREFIX + "§a✓ Item renamed to: " + newName);
        return true;
    }
}



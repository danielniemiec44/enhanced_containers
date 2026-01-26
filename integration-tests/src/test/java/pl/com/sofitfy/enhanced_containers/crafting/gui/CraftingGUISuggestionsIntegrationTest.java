package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.CraftingRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CraftingGUI - recipe suggestions")
class CraftingGUISuggestionsIntegrationTest extends MockBukkitPluginTestBase {

    @BeforeEach
    void cleanRecipes() {
        RecipeManager.getInstance().clearRecipes();
        RecipeManager.getInstance().clearVanillaRecipes();
    }

    @Test
    @DisplayName("Pokazuje podpowiedź gdy receptura jest craftowalna z ekwipunku")
    void showsSuggestionWhenCraftableFromInventory() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        // Receptura: 1x DIAMOND -> 1x DIAMOND_BLOCK
        ItemStack[][] pattern = new ItemStack[][] { { new ItemStack(Material.DIAMOND) } };
        CraftingRecipe recipe = new CraftingRecipe(
            "test_diamond_block",
            "Test Diamond Block",
            pattern,
            new ItemStack(Material.DIAMOND_BLOCK, 1),
            "*"
        );
        RecipeManager.getInstance().registerRecipe(recipe);

        // Gracz ma DIAMOND w ekwipunku (nie w gridzie)
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 1));

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener((org.bukkit.plugin.java.JavaPlugin) plugin);
        listener.registerGUI(gui);

        // Trigger update
        listener.updateOutput(player, gui);

        boolean found = false;
        for (int slot : CraftingGUI.SUGGESTION_SLOTS) {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item != null && item.getType() == Material.DIAMOND_BLOCK) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Powinna pojawić się podpowiedź DIAMOND_BLOCK");
    }

    @Test
    @DisplayName("Kliknięcie w slot podpowiedzi jest anulowane (read-only)")
    void clickingSuggestionSlotIsCancelled() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        // Receptura craftowalna
        RecipeManager.getInstance().registerRecipe(new CraftingRecipe(
            "test",
            "Test",
            new ItemStack[][] { { new ItemStack(Material.DIAMOND) } },
            new ItemStack(Material.DIAMOND_BLOCK, 1),
            "*"
        ));
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 1));

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener((org.bukkit.plugin.java.JavaPlugin) plugin);
        listener.registerGUI(gui);

        listener.updateOutput(player, gui);

        // Znajdź slot z podpowiedzią
        Integer suggestionSlot = null;
        for (int slot : CraftingGUI.SUGGESTION_SLOTS) {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item != null && item.getType() == Material.DIAMOND_BLOCK) {
                suggestionSlot = slot;
                break;
            }
        }
        assertNotNull(suggestionSlot, "W teście musi być podpowiedź");

        player.openInventory(gui.getInventory());
        player.setItemOnCursor(new ItemStack(Material.COBBLESTONE, 64));

        InventoryClickEvent click = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            suggestionSlot,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        listener.onInventoryClick(click);

        assertTrue(click.isCancelled(), "Slot podpowiedzi powinien być read-only");
        assertEquals(Material.COBBLESTONE, player.getItemOnCursor().getType(), "Kursor nie powinien się zmienić");

        ItemStack stillThere = gui.getInventory().getItem(suggestionSlot);
        assertNotNull(stillThere, "Ikonka podpowiedzi powinna zostać");
        assertEquals(Material.DIAMOND_BLOCK, stillThere.getType(), "Ikonka podpowiedzi powinna zostać");
    }

    @Test
    @DisplayName("Zamykanie GUI nie może oddać ikonek podpowiedzi graczowi (anti-dupe)")
    void closingGuiDoesNotGiveSuggestionIconsToPlayer() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        // Receptura craftowalna
        RecipeManager.getInstance().registerRecipe(new CraftingRecipe(
            "test",
            "Test",
            new ItemStack[][] { { new ItemStack(Material.DIAMOND) } },
            new ItemStack(Material.DIAMOND_BLOCK, 1),
            "*"
        ));
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 1));

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener((org.bukkit.plugin.java.JavaPlugin) plugin);
        listener.registerGUI(gui);

        listener.updateOutput(player, gui);

        // Sanity: w GUI jest ikonka podpowiedzi
        boolean found = false;
        for (int slot : CraftingGUI.SUGGESTION_SLOTS) {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item != null && item.getType() == Material.DIAMOND_BLOCK) {
                found = true;
                break;
            }
        }
        assertTrue(found, "W teście musi być ikonka podpowiedzi");

        player.openInventory(gui.getInventory());

        // Symuluj zamknięcie
        InventoryCloseEvent closeEvent = new InventoryCloseEvent(player.getOpenInventory());
        listener.onInventoryClose(closeEvent);

        // Gracz NIE może dostać DIAMOND_BLOCK z UI
        assertFalse(player.getInventory().contains(Material.DIAMOND_BLOCK), "Gracz nie może dostać ikonki podpowiedzi");
    }
}

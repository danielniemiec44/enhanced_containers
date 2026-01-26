package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
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

@DisplayName("CraftingGUI - click suggestion autofill (custom)")
class CraftingGUISuggestionsAutofillCustomIT extends MockBukkitPluginTestBase {

    @BeforeEach
    void cleanRecipes() {
        RecipeManager.getInstance().clearRecipes();
        RecipeManager.getInstance().clearVanillaRecipes();
    }

    @Test
    @DisplayName("Klik w podpowiedź custom układa recepturę w lewym górnym rogu i zabiera itemy")
    void clickingCustomSuggestionFillsGridAndConsumesInventory() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        // Receptura: 2x2 COAL -> DIAMOND (prosta)
        ItemStack[][] pattern = new ItemStack[][] {
            { new ItemStack(Material.COAL), new ItemStack(Material.COAL) },
            { new ItemStack(Material.COAL), new ItemStack(Material.COAL) }
        };
        RecipeManager.getInstance().registerRecipe(new CraftingRecipe(
            "test",
            "Test",
            pattern,
            new ItemStack(Material.DIAMOND, 1),
            "*"
        ));

        // Gracz ma 4x COAL w ekwipunku
        player.getInventory().addItem(new ItemStack(Material.COAL, 4));

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener((org.bukkit.plugin.java.JavaPlugin) plugin);
        listener.registerGUI(gui);

        // Trigger suggestions
        listener.updateOutput(player, gui);

        // Znajdź slot podpowiedzi z DIAMOND
        Integer suggestionSlot = null;
        for (int slot : CraftingGUI.SUGGESTION_SLOTS) {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item != null && item.getType() == Material.DIAMOND) {
                suggestionSlot = slot;
                break;
            }
        }
        assertNotNull(suggestionSlot, "W teście musi być podpowiedź DIAMOND");

        player.openInventory(gui.getInventory());

        InventoryClickEvent click = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            suggestionSlot,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        listener.onInventoryClick(click);
        assertTrue(click.isCancelled(), "Klik w podpowiedź jest anulowany, a my robimy własną akcję");

        // Grid powinien mieć 2x2 COAL w lewym górnym rogu
        assertEquals(Material.COAL, gui.getInventory().getItem(0).getType());
        assertEquals(Material.COAL, gui.getInventory().getItem(1).getType());
        assertEquals(Material.COAL, gui.getInventory().getItem(9).getType());
        assertEquals(Material.COAL, gui.getInventory().getItem(10).getType());

        // Ekwipunek gracza powinien stracić 4 coal
        assertFalse(player.getInventory().contains(Material.COAL), "COAL powinien zostać zużyty");

        // Output powinien się pojawić
        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(output);
        assertEquals(Material.DIAMOND, output.getType());
    }

    @Test
    @DisplayName("Klik w podpowiedź custom wypełnia sloty na max (żeby dało się shift-clickać wynik wiele razy)")
    void clickingCustomSuggestionFillsMaxStacksAndLeavesRemainderInInventory() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        // Receptura: 2x2 COAL -> DIAMOND
        ItemStack[][] pattern = new ItemStack[][] {
            { new ItemStack(Material.COAL), new ItemStack(Material.COAL) },
            { new ItemStack(Material.COAL), new ItemStack(Material.COAL) }
        };
        RecipeManager.getInstance().registerRecipe(new CraftingRecipe(
            "test2",
            "Test2",
            pattern,
            new ItemStack(Material.DIAMOND, 1),
            "*"
        ));

        // Gracz ma dużo COAL.
        // Oczekujemy: każdy z 4 slotów dostanie max 64, reszta zostanie w ekwipunku.
        player.getInventory().addItem(new ItemStack(Material.COAL, 300));

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener((org.bukkit.plugin.java.JavaPlugin) plugin);
        listener.registerGUI(gui);

        // Trigger suggestions
        listener.updateOutput(player, gui);

        Integer suggestionSlot = null;
        for (int slot : CraftingGUI.SUGGESTION_SLOTS) {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item != null && item.getType() == Material.DIAMOND) {
                suggestionSlot = slot;
                break;
            }
        }
        assertNotNull(suggestionSlot, "W teście musi być podpowiedź DIAMOND");

        player.openInventory(gui.getInventory());

        InventoryClickEvent click = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            suggestionSlot,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        listener.onInventoryClick(click);
        assertTrue(click.isCancelled());

        // Grid powinien mieć 2x2 COAL w lewym górnym rogu, ale z max stackami.
        assertEquals(64, gui.getInventory().getItem(0).getAmount());
        assertEquals(64, gui.getInventory().getItem(1).getAmount());
        assertEquals(64, gui.getInventory().getItem(9).getAmount());
        assertEquals(64, gui.getInventory().getItem(10).getAmount());

        // Zużyliśmy 256 (4*64), powinno zostać 44 w ekwipunku.
        int remaining = 0;
        for (ItemStack s : player.getInventory().getStorageContents()) {
            if (s != null && s.getType() == Material.COAL) {
                remaining += s.getAmount();
            }
        }
        assertEquals(44, remaining, "Reszta COAL powinna zostać w ekwipunku");

        // Output powinien się pojawić
        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(output);
        assertEquals(Material.DIAMOND, output.getType());
    }
}

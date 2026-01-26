package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.crafting.gui.CraftingGUI;
import pl.com.sofitfy.enhanced_containers.crafting.gui.CraftingGUIListener;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Priorytet receptur: custom > vanilla")
class RecipePriorityIntegrationTest extends MockBukkitPluginTestBase {

    @Test
    @DisplayName("Gdy pasuje custom i vanilla jednocześnie, output musi być z custom")
    void customRecipeMustOverrideVanillaWhenBothMatch() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        RecipeManager.getInstance().clearRecipes();
        RecipeManager.getInstance().loadVanillaRecipes();

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Setup: 1x OAK_LOG w gridzie.
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.OAK_LOG, 1));

        // Sanity check: bez custom receptury vanilla powinna dać OAK_PLANKS.
        listener.updateOutput(player, gui);
        ItemStack vanillaOutput = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);

        // W razie gdyby MockBukkit w danej wersji nie miał tej receptury, nie robimy false-negative.
        Assumptions.assumeTrue(
            vanillaOutput != null && vanillaOutput.getType() == Material.OAK_PLANKS,
            "MockBukkit powinien mieć vanilla recepturę OAK_LOG -> OAK_PLANKS"
        );

        // Custom receptura: 1x OAK_LOG -> 1x DIAMOND (ten sam input co vanilla, inny output).
        CraftingRecipe custom = new CraftingRecipe(
            "custom_log_to_diamond",
            "Custom Log To Diamond",
            new ItemStack[][] { { new ItemStack(Material.OAK_LOG, 1) } },
            new ItemStack(Material.DIAMOND, 1),
            "*"
        );
        RecipeManager.getInstance().registerRecipe(custom);

        // Act: ponowna aktualizacja outputu.
        listener.updateOutput(player, gui);

        // Assert: custom ma być zawsze ważniejszy.
        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(output, "Output powinien istnieć");
        assertEquals(Material.DIAMOND, output.getType(), "Custom receptura powinna nadpisać vanilla output");
    }

    @Test
    @DisplayName("Gdy pasują 2 custom receptury naraz, wybieramy tę zarejestrowaną wcześniej")
    void whenTwoCustomRecipesMatch_chooseFirstRegistered() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        RecipeManager.getInstance().clearRecipes();

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Setup: 1x OAK_LOG w gridzie.
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.OAK_LOG, 1));

        // Dwie custom receptury pasujące do tego samego inputu.
        CraftingRecipe first = new CraftingRecipe(
            "custom_1",
            "First",
            new ItemStack[][] { { new ItemStack(Material.OAK_LOG, 1) } },
            new ItemStack(Material.COAL, 1),
            "*"
        );
        CraftingRecipe second = new CraftingRecipe(
            "custom_2",
            "Second",
            new ItemStack[][] { { new ItemStack(Material.OAK_LOG, 1) } },
            new ItemStack(Material.DIAMOND, 1),
            "*"
        );

        RecipeManager.getInstance().registerRecipe(first);
        RecipeManager.getInstance().registerRecipe(second);

        listener.updateOutput(player, gui);
        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);

        assertNotNull(output, "Output powinien istnieć");
        assertEquals(Material.COAL, output.getType(), "Powinna wygrać receptura zarejestrowana jako pierwsza");
    }
}

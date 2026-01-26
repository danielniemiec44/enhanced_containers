package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.crafting.gui.CraftingGUI;
import pl.com.sofitfy.enhanced_containers.crafting.gui.CraftingGUIListener;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test integracyjny:
 * - shapeless vanilla receptura powinna działać, nawet jeśli składniki są rozrzucone po 4x4,
 * - a w środku traktujemy to jak "upakowane" do 3x3.
 */
@DisplayName("Integracja - vanilla shapeless działa w dowolnych slotach 4x4")
class VanillaShapelessAnywhereIn4x4IT extends MockBukkitPluginTestBase {

    @Test
    @DisplayName("Shapeless: PAPER + GUNPOWDER matchuje nawet gdy leżą w rogach 4x4")
    void shapelessMatchesWhenItemsAreScatteredIn4x4() {
        // W MockBukkit domyślnie nie ma receptur, więc dorzucamy dwie proste shapeless.
        loadVanillaRecipes();

        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Składniki kładziemy w dwóch dalekich miejscach:
        // - lewy górny róg 4x4
        // - prawy dolny róg 4x4
        // Bounding box ma wtedy 4x4, więc stary shrink-wrap nie przechodzi.
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.PAPER, 1));
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[15], new ItemStack(Material.GUNPOWDER, 1));

        listener.updateOutput(player, gui);

        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(output, "Output powinien się pojawić dla shapeless nawet przy rozrzuceniu w 4x4");
        assertEquals(Material.FIREWORK_ROCKET, output.getType(), "Wynik powinien być FIREWORK_ROCKET");
        assertEquals(3, output.getAmount(), "Vanilla: PAPER + GUNPOWDER daje 3 FIREWORK_ROCKET");
    }

    @Test
    @DisplayName("Shapeless (3 składniki): matchuje nawet gdy leżą w 3 rogach 4x4")
    void shapelessThreeIngredientsMatchesWhenScatteredIn4x4() {
        loadVanillaRecipes();

        // Dodatkowa receptura shapeless z 3 składnikami.
        // To przypadek, który wcześniej potrafił się wyłożyć ("działa tylko jeden item na rogu").
        ShapelessRecipe triple = new ShapelessRecipe(
            new NamespacedKey("test", "triple_shapeless"),
            new ItemStack(Material.DIAMOND, 1)
        );
        triple.addIngredient(Material.STICK);
        triple.addIngredient(Material.STICK);
        triple.addIngredient(Material.COAL);
        Bukkit.getServer().addRecipe(triple);

        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Trzy składniki rozrzucone po 4x4 (3 różne rogi).
        // Bounding box = 4x4, więc shrink-wrap (shaped) nie powinien mieć tu znaczenia.
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.STICK, 1));
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[3], new ItemStack(Material.COAL, 1));
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[15], new ItemStack(Material.STICK, 1));

        listener.updateOutput(player, gui);

        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(output, "Output powinien się pojawić dla shapeless (3 składniki) nawet przy rozrzuceniu w 4x4");
        assertEquals(Material.DIAMOND, output.getType(), "Wynik powinien być DIAMOND");
        assertEquals(1, output.getAmount(), "W testowej recepturze wynik ma amount=1");
    }
}

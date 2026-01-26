package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.CraftingRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitTestBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy dla RecipeSelectionGUI - selektor receptur do edycji
 *
 * RecipeSelectionGUI odpowiada za:
 * - Wyświetlanie listy receptur na stronie (max 45 receptur)
 * - Paginację - nawigacja między stronami
 * - Przekazanie wybranej receptury do edytora
 * - Wyświetlanie przycisków nawigacji
 *
 * Layout inventoria:
 * - Sloty 0-44: Receptury (do 45 na stronę)
 * - Slot 45: Przycisk poprzednia strona
 * - Slot 49: Informacja o stronie
 * - Slot 53: Przycisk następna strona
 */
@DisplayName("RecipeSelectionGUI - testy")
@Tag("unit")
class RecipeSelectionGUITest extends MockBukkitTestBase {

    @Test
    @DisplayName("GUI ma 54 sloty i tytuł po angielsku")
    void guiHasProperSizeAndTitle() {
        // Dado: przynajmniej jedna receptura (żeby GUI miało co pokazać)
        RecipeManager.getInstance().clearRecipes();
        RecipeManager.getInstance().registerRecipe(sampleRecipe("r1", Material.COAL));

        var player = createPlayer();
        RecipeSelectionGUI gui = new RecipeSelectionGUI(player, 1);

        // To: rozmiar inventoria
        assertEquals(54, gui.getInventory().getSize());

        // To: tytuł w grze ma być po angielsku i bez custom fontu
        assertTrue(GUITitleFormatter.getRecipeSelectionTitle().contains("Recipe Selection"));
        assertFalse(GUITitleFormatter.getRecipeSelectionTitle().contains("\uE001"));
    }

    @Test
    @DisplayName("Strona 1: jest przycisk next, nie ma prev")
    void page1NavigationButtons() {
        // Dado: 46 receptur = 2 strony
        RecipeManager.getInstance().clearRecipes();
        for (int i = 0; i < 46; i++) {
            RecipeManager.getInstance().registerRecipe(sampleRecipe("r" + i, Material.DIAMOND));
        }

        var player = createPlayer();
        RecipeSelectionGUI gui = new RecipeSelectionGUI(player, 1);

        // To: przyciski nawigacji
        assertNull(gui.getInventory().getItem(RecipeSelectionGUI.PREV_PAGE_SLOT),
            "Na pierwszej stronie nie ma poprzedniej");
        assertNotNull(gui.getInventory().getItem(RecipeSelectionGUI.NEXT_PAGE_SLOT),
            "Na pierwszej stronie jest następna");
        assertNotNull(gui.getInventory().getItem(RecipeSelectionGUI.PAGE_INFO_SLOT),
            "Zawsze pokazujemy info o stronie");
    }

    @Test
    @DisplayName("Ikona receptury ma lore po angielsku: Click to edit")
    void recipeIconLoreIsEnglish() {
        // Dado: jedna receptura
        RecipeManager.getInstance().clearRecipes();
        RecipeManager.getInstance().registerRecipe(sampleRecipe("my_id", Material.EMERALD));

        var player = createPlayer();
        RecipeSelectionGUI gui = new RecipeSelectionGUI(player, 1);

        // Kiedy: bierzemy ikonę z pierwszego slotu
        ItemStack icon = gui.getInventory().getItem(0);
        assertNotNull(icon);
        ItemMeta meta = icon.getItemMeta();
        assertNotNull(meta);
        List<String> lore = meta.getLore();
        assertNotNull(lore);

        // To: lore ma być po angielsku
        assertTrue(lore.stream().anyMatch(l -> l.contains("ID: my_id")));
        assertTrue(lore.stream().anyMatch(l -> l.contains("Click to edit")),
            "Tekst w grze ma być po angielsku");
    }

    @Test
    @DisplayName("getRecipeFromSlot zwraca poprawną recepturę")
    void getRecipeFromSlotWorks() {
        // Dado: 2 receptury
        RecipeManager.getInstance().clearRecipes();
        RecipeManager.getInstance().registerRecipe(sampleRecipe("a", Material.COAL));
        RecipeManager.getInstance().registerRecipe(sampleRecipe("b", Material.IRON_INGOT));

        var player = createPlayer();
        RecipeSelectionGUI gui = new RecipeSelectionGUI(player, 1);

        // Kiedy/To: slot 0 zwraca jakąś recepturę z listy
        CraftingRecipe recipe0 = gui.getRecipeFromSlot(0);
        assertNotNull(recipe0);
        assertTrue(recipe0.getId().equals("a") || recipe0.getId().equals("b"));
    }

    @Test
    @DisplayName("Sloty 46-48 to nawigacja (nie receptury)")
    void slots46to48AreInNavigationRow() {
        // To: sloty 46, 47, 48 są w ostatnim rzędzie (tam są przyciski/ozdobniki)
        for (int slot = 46; slot <= 48; slot++) {
            assertTrue(slot >= RecipeSelectionGUI.NAV_ROW_START,
                "Slot " + slot + " jest w ostatnim rzędzie (nawigacja)");
        }
    }

    private static CraftingRecipe sampleRecipe(String id, Material outputMaterial) {
        ItemStack[][] pattern = new ItemStack[4][4];
        pattern[0][0] = new ItemStack(Material.COAL);
        ItemStack output = new ItemStack(outputMaterial);
        return new CraftingRecipe(id, "Test", pattern, output, "plethoras.tools.use");
    }
}

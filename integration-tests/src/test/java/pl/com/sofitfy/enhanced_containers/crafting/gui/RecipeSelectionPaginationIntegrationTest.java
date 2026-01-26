package pl.com.sofitfy.enhanced_containers.crafting.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.CraftingRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitTestBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integracja (MockBukkit) - paginacja selektora receptur.
 *
 * Testujemy "edit selector" - czyli listę receptur, gdzie admin wybiera recepturę do edycji.
 *
 * Wymagania:
 * - 45 receptur na stronę (sloty 0-44)
 * - przyciski: prev=45, info=49, next=53
 * - klik w next/prev przełącza stronę
 */
@Tag("integration")
@DisplayName("RecipeSelectionGUI - paginacja (MockBukkit)")
class RecipeSelectionPaginationIntegrationTest extends MockBukkitTestBase {

    @BeforeEach
    void resetRecipes() {
        RecipeManager.getInstance().clearRecipes();
    }

    @Test
    @DisplayName("Next/Prev przełącza strony i zmienia zawartość listy")
    void nextPrevSwitchesPagesAndContent() {
        // Dado: 46 receptur => 2 strony (45 + 1)
        for (int i = 0; i < 46; i++) {
            RecipeManager.getInstance().registerRecipe(sampleRecipe("r" + i, Material.STONE));
        }

        Player player = createPlayer();
        RecipeEditorListener editorListener = mock(RecipeEditorListener.class);
        RecipeSelectionListener selectionListener = new RecipeSelectionListener(editorListener, null);

        // Otwieramy stronę 1
        RecipeSelectionGUI page1 = new RecipeSelectionGUI(player, 1);
        selectionListener.registerSelector(page1);
        page1.open();

        assertPageInfo(player, "§6Page 1 / 2");
        assertRecipeIdInSlot(player, 0, "r0");
        assertRecipeIdInSlot(player, 44, "r44");

        // Kiedy: klik w "next"
        selectionListener.onInventoryClick(mockClick(player, page1.getInventory(), RecipeSelectionGUI.NEXT_PAGE_SLOT));

        // To: otwarta jest strona 2 i zawiera tylko ostatnią recepturę
        assertPageInfo(player, "§6Page 2 / 2");
        assertRecipeIdInSlot(player, 0, "r45");
        assertNull(player.getOpenInventory().getTopInventory().getItem(1), "Na stronie 2 powinno być tylko 1 receptura");

        // Kiedy: klik w "prev"
        // (używamy aktualnie otwartego inventory - to musi zgadzać się z selektorem w listenerze)
        Inventory openTop = player.getOpenInventory().getTopInventory();
        selectionListener.onInventoryClick(mockClick(player, openTop, RecipeSelectionGUI.PREV_PAGE_SLOT));

        // To: wracamy na stronę 1
        assertPageInfo(player, "§6Page 1 / 2");
        assertRecipeIdInSlot(player, 0, "r0");
    }

    private static CraftingRecipe sampleRecipe(String id, Material outputMaterial) {
        ItemStack[][] pattern = new ItemStack[4][4];
        pattern[0][0] = new ItemStack(Material.COBBLESTONE);

        ItemStack output = new ItemStack(outputMaterial);
        return new CraftingRecipe(id, "Test", pattern, output, "plethoras.tools.use");
    }

    private static void assertPageInfo(Player player, String expectedDisplayName) {
        ItemStack pageInfo = player.getOpenInventory().getTopInventory().getItem(RecipeSelectionGUI.PAGE_INFO_SLOT);
        assertNotNull(pageInfo, "Page info item powinien istnieć");

        ItemMeta meta = pageInfo.getItemMeta();
        assertNotNull(meta, "Page info item powinien mieć meta");

        Component displayName = meta.displayName();
        assertNotNull(displayName, "Page info item powinien mieć displayName");

        String plain = PlainTextComponentSerializer.plainText().serialize(displayName);
        // expectedDisplayName zawiera kody kolorów, więc porównujemy tylko treść.
        assertTrue(
            plain.contains(expectedDisplayName.replace("§6", "")),
            "Display name powinien zawierać poprawną stronę (było: " + plain + ")"
        );
    }

    private static void assertRecipeIdInSlot(Player player, int slot, String expectedId) {
        ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot);
        assertNotNull(item, "Slot " + slot + " powinien zawierać recepturę");

        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);
        List<Component> lore = meta.lore();
        assertNotNull(lore, "Ikona receptury powinna mieć lore");

        String joined = lore.stream()
            .map(c -> PlainTextComponentSerializer.plainText().serialize(c))
            .reduce("", (a, b) -> a + "\n" + b);

        assertTrue(
            joined.contains("ID: " + expectedId),
            "Lore powinno zawierać ID: " + expectedId + " (było: " + joined + ")"
        );
    }

    private static InventoryClickEvent mockClick(Player player, Inventory inventory, int rawSlot) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getRawSlot()).thenReturn(rawSlot);
        return event;
    }
}

package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.config.ConfigManager;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeDraft;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RecipeEditor - limit amount=1 w inputach")
class RecipeEditorInputAmountLimitIT extends MockBukkitPluginTestBase {

    @Test
    @DisplayName("Jeśli w gridzie pojawi się stack > 1, plugin redukuje go do 1 i oddaje resztę graczowi")
    void editorGridNeverKeepsMoreThanOne() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        // Przygotowanie: gracz ma 64 diamenty, które "przeniesie" do edytora.
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 64));

        RecipeDraft draft = new RecipeDraft("test");
        RecipeEditorGUI editor = new RecipeEditorGUI(player, draft);
        RecipeEditorListener listener = new RecipeEditorListener(plugin, new ConfigManager((org.bukkit.plugin.java.JavaPlugin) plugin));
        listener.registerEditor(editor);
        editor.open();

        // Symulacja tego co robi Minecraft: stack ląduje w slocie edytora, a z ekwipunku znika.
        // To jest ważne, bo inaczej test mógłby "dodupować" itemy.
        player.getInventory().setItem(0, null);
        editor.getInventory().setItem(RecipeEditorGUI.GRID_SLOTS[0], new ItemStack(Material.DIAMOND, 64));

        // Wywołujemy handler kliknięcia (nie testujemy mechaniki samego kliknięcia,
        // tylko reakcję pluginu: w edytorze nie wolno mieć amount>1 w inputach).
        InventoryClickEvent click = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            RecipeEditorGUI.GRID_SLOTS[0],
            ClickType.LEFT,
            InventoryAction.PLACE_ALL
        );
        listener.onEditorInventoryClick(click);

        // Listener robi korektę tick później (scheduler), więc musimy zrobić tick.
        tick(1);

        ItemStack inGrid = editor.getInventory().getItem(RecipeEditorGUI.GRID_SLOTS[0]);
        assertNotNull(inGrid);
        assertEquals(Material.DIAMOND, inGrid.getType());
        assertEquals(1, inGrid.getAmount(), "W slocie input ma zostać tylko 1 sztuka");

        // Reszta (63) ma wrócić do ekwipunku gracza.
        int diamondsInInv = 0;
        for (ItemStack s : player.getInventory().getStorageContents()) {
            if (s != null && s.getType() == Material.DIAMOND) {
                diamondsInInv += s.getAmount();
            }
        }
        assertEquals(63, diamondsInInv, "Nadmiar ma wrócić do gracza");

        // Output nie powinien być dotykany przez ten mechanizm.
        editor.getInventory().setItem(RecipeEditorGUI.OUTPUT_SLOT, new ItemStack(Material.GOLD_INGOT, 5));
        listener.onEditorInventoryClick(click);
        tick(1);
        assertEquals(5, editor.getInventory().getItem(RecipeEditorGUI.OUTPUT_SLOT).getAmount(), "Output ma zachować swoją ilość");
    }

    @Test
    @DisplayName("Przy zapisie draftu ilość inputów jest normalizowana do 1, a output zostaje bez zmian")
    void updateDraftFromGuiNormalizesInputAmounts() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        RecipeDraft draft = new RecipeDraft("test2");
        RecipeEditorGUI editor = new RecipeEditorGUI(player, draft);
        editor.open();

        // Symulujemy stare / niepoprawne dane w GUI: input ma amount>1.
        editor.getInventory().setItem(RecipeEditorGUI.GRID_SLOTS[0], new ItemStack(Material.COAL, 10));
        editor.getInventory().setItem(RecipeEditorGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND, 7));

        editor.updateDraftFromGUI();

        assertNotNull(draft.getInputPattern());
        assertNotNull(draft.getInputPattern()[0][0]);
        assertEquals(1, draft.getInputPattern()[0][0].getAmount(), "W draftcie input ma mieć zawsze 1");

        assertNotNull(draft.getOutput());
        assertEquals(7, draft.getOutput().getAmount(), "Output ma zachować ilość");
    }
}

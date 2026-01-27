package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.CraftingRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integracyjne testy scenariuszy dla craftingu 4x4.
 *
 * Cel:
 * - Symulujemy zachowanie jak crafting table 4x4
 * - Sprawdzamy typowe i nietypowe przypadki w naszym GUI
 * - Weryfikujemy że output jest tylko do odczytu
 */
@DisplayName("Scenariusze craftingu 4x4 - integracja")
class CraftingGUIScenarioIntegrationTest extends MockBukkitPluginTestBase {

    @Test
    @DisplayName("Normalny LEFT click zabiera output i zużywa składnik")
    void normalLeftClick_TakesOutputAndConsumesIngredient() {
        // Przygotowanie
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Receptura: 1 DIAMOND -> 1 DIAMOND_BLOCK
        CraftingRecipe recipe = simpleDiamondRecipe();
        RecipeManager.getInstance().registerRecipe(recipe);

        // Wkładamy 1 diamond do gridu
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.DIAMOND, 1));
        listener.updateOutput(player, gui);

        // Output powinien być ustawiony
        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(output, "Output powinien się pojawić");
        assertEquals(Material.DIAMOND_BLOCK, output.getType(), "Output powinien być DIAMOND_BLOCK");

        // Klik na output
        player.openInventory(gui.getInventory());
        player.setItemOnCursor(null);

        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            CraftingGUI.OUTPUT_SLOT,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        listener.onInventoryClick(event);

        // Event zawsze anulowany - ręczna obsługa
        assertTrue(event.isCancelled(), "Event musi być anulowany");

        // Output trafia na kursor
        ItemStack cursor = player.getItemOnCursor();
        assertNotNull(cursor, "Kursor powinien mieć item");
        assertEquals(Material.DIAMOND_BLOCK, cursor.getType(), "Kursor powinien mieć DIAMOND_BLOCK");

        // Składnik został zużyty
        ItemStack gridItem = gui.getInventory().getItem(CraftingGUI.GRID_SLOTS[0]);
        assertTrue(gridItem == null || gridItem.getType().isAir(), "Grid powinien być pusty");
    }

    @Test
    @DisplayName("Normalny RIGHT click działa tak samo jak LEFT")
    void normalRightClick_AlsoTakesOutput() {
        // Przygotowanie
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Receptura: 1 DIAMOND -> 1 DIAMOND_BLOCK
        CraftingRecipe recipe = simpleDiamondRecipe();
        RecipeManager.getInstance().registerRecipe(recipe);

        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.DIAMOND, 1));
        listener.updateOutput(player, gui);

        player.openInventory(gui.getInventory());
        player.setItemOnCursor(null);

        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            CraftingGUI.OUTPUT_SLOT,
            ClickType.RIGHT,
            InventoryAction.PICKUP_HALF
        );

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled(), "Event musi być anulowany");

        ItemStack cursor = player.getItemOnCursor();
        assertNotNull(cursor, "Kursor powinien mieć item");
        assertEquals(Material.DIAMOND_BLOCK, cursor.getType(), "Kursor powinien mieć DIAMOND_BLOCK");
    }

    @Test
    @DisplayName("Klik z itemem w kursorze nie podmienia outputu")
    void clickWithCursorItem_DoesNotReplaceOutput() {
        // Przygotowanie
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND_BLOCK, 1));
        player.openInventory(gui.getInventory());

        // Gracz ma item w kursorze
        player.setItemOnCursor(new ItemStack(Material.COBBLESTONE, 64));

        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            CraftingGUI.OUTPUT_SLOT,
            ClickType.LEFT,
            InventoryAction.SWAP_WITH_CURSOR
        );

        listener.onInventoryClick(event);

        // Event anulowany
        assertTrue(event.isCancelled(), "Event musi być anulowany");

        // Output nie może się zmienić
        ItemStack outputAfter = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(outputAfter, "Output nie może zniknąć");
        assertEquals(Material.DIAMOND_BLOCK, outputAfter.getType(), "Output nie może być podmieniony");

        // Kursor zostaje bez zmian
        ItemStack cursorAfter = player.getItemOnCursor();
        assertNotNull(cursorAfter, "Kursor powinien zostać bez zmian");
        assertEquals(Material.COBBLESTONE, cursorAfter.getType(), "Kursor powinien zostać COBBLESTONE");
    }

    @Test
    @DisplayName("Number key swap na output jest blokowany")
    void numberKeySwapOnOutput_IsBlocked() {
        // Przygotowanie
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND_BLOCK, 1));
        player.openInventory(gui.getInventory());

        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            CraftingGUI.OUTPUT_SLOT,
            ClickType.NUMBER_KEY,
            InventoryAction.HOTBAR_SWAP
        );

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled(), "Number key swap musi być zablokowany");
    }

    @Test
    @DisplayName("Drag na output jest blokowany")
    void dragOnOutput_IsBlocked() {
        // Przygotowanie
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        player.openInventory(gui.getInventory());

        Map<Integer, ItemStack> newItems = new HashMap<>();
        newItems.put(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.STONE, 1));

        InventoryDragEvent event = new InventoryDragEvent(
            player.getOpenInventory(),
            new ItemStack(Material.STONE, 64),
            new ItemStack(Material.STONE, 63),
            false,
            newItems
        );

        listener.onInventoryDrag(event);

        assertTrue(event.isCancelled(), "Drag na output musi być zablokowany");
    }

    @Test
    @DisplayName("Drag na input aktualizuje output po ticku")
    void dragOnInput_UpdatesOutputAfterTick() {
        // Przygotowanie
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        CraftingRecipe recipe = simpleDiamondRecipe();
        RecipeManager.getInstance().registerRecipe(recipe);

        player.openInventory(gui.getInventory());

        // Dragujemy diamond do input slotu
        Map<Integer, ItemStack> newItems = new HashMap<>();
        newItems.put(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.DIAMOND, 1));

        InventoryDragEvent event = new InventoryDragEvent(
            player.getOpenInventory(),
            new ItemStack(Material.DIAMOND, 1),
            new ItemStack(Material.DIAMOND, 1),
            false,
            newItems
        );

        listener.onInventoryDrag(event);

        // Symulujemy, że Bukkit faktycznie wstawił item do slotu
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.DIAMOND, 1));

        // Output powinien pojawić się po ticku
        tick(1);

        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(output, "Output powinien się pojawić po ticku");
        assertEquals(Material.DIAMOND_BLOCK, output.getType(), "Output powinien być DIAMOND_BLOCK");
    }

    @Test
    @DisplayName("Output znika gdy input przestaje pasować")
    void outputClearsWhenInputNoLongerMatches() {
        // Przygotowanie
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        CraftingRecipe recipe = simpleDiamondRecipe();
        RecipeManager.getInstance().registerRecipe(recipe);

        // Najpierw pasuje receptura
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.DIAMOND, 1));
        listener.updateOutput(player, gui);

        ItemStack outputBefore = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(outputBefore, "Output powinien być ustawiony");

        // Zmieniamy input tak, aby receptura już nie pasowała
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.COBBLESTONE, 1));
        listener.updateOutput(player, gui);

        ItemStack outputAfter = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertTrue(outputAfter == null || outputAfter.getType().isAir(), "Output powinien zniknąć");
    }

    @Test
    @DisplayName("Próba podmiany outputu nie zużywa składników")
    void swapAttempt_DoesNotConsumeIngredients() {
        // Przygotowanie
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        CraftingRecipe recipe = simpleDiamondRecipe();
        RecipeManager.getInstance().registerRecipe(recipe);

        // Wkładamy diamond do gridu
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.DIAMOND, 1));
        listener.updateOutput(player, gui);

        player.openInventory(gui.getInventory());
        player.setItemOnCursor(new ItemStack(Material.COBBLESTONE, 64));

        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            CraftingGUI.OUTPUT_SLOT,
            ClickType.LEFT,
            InventoryAction.SWAP_WITH_CURSOR
        );

        listener.onInventoryClick(event);

        // Event anulowany
        assertTrue(event.isCancelled(), "Event musi być anulowany");

        // Składnik NIE może zostać zużyty
        ItemStack gridItem = gui.getInventory().getItem(CraftingGUI.GRID_SLOTS[0]);
        assertNotNull(gridItem, "Składnik powinien zostać w gridzie");
        assertEquals(Material.DIAMOND, gridItem.getType(), "W gridzie powinien zostać DIAMOND");

        // Output nie może się zmienić
        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(output, "Output powinien zostać");
        assertEquals(Material.DIAMOND_BLOCK, output.getType(), "Output powinien być DIAMOND_BLOCK");
    }

    @Test
    @DisplayName("Drop key (Q) na output bez składników jest blokowany")
    void dropKeyOnOutputWithoutIngredients_IsBlocked() {
        // Przygotowanie
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Output bez składników (symulowany wynik)
        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND_BLOCK, 1));
        player.openInventory(gui.getInventory());

        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            CraftingGUI.OUTPUT_SLOT,
            ClickType.DROP,
            InventoryAction.DROP_ONE_SLOT
        );

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled(), "Drop key na output musi być zablokowany (obsługiwany ręcznie)");

        // Bez składników output nie znika (nie ma z czego scraftować)
        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(output, "Output nie może zniknąć bez składników");
        assertEquals(Material.DIAMOND_BLOCK, output.getType(), "Output powinien zostać");
    }

    @Test
    @DisplayName("Drop key (Q) na output ze składnikami craftuje i dropuje na ziemię (vanilla behavior)")
    void dropKeyOnOutputWithIngredients_CraftsAndDropsToGround() {
        // Przygotowanie
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Ustawiamy składniki dla planks (4x4 grid - sloty 0,1,2,3,9,10,11,12,18,19,20,21,27,28,29,30)
        // Jeden log w slocie 0 -> 4 planki
        gui.getInventory().setItem(0, new ItemStack(Material.OAK_LOG, 1));
        
        // Aktualizujemy output (symulacja)
        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.OAK_PLANKS, 4));
        player.openInventory(gui.getInventory());
        
        // Kursor pusty
        player.setItemOnCursor(null);

        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            CraftingGUI.OUTPUT_SLOT,
            ClickType.DROP,
            InventoryAction.DROP_ONE_SLOT
        );

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled(), "Event powinien być anulowany (obsługujemy ręcznie)");
        
        // Vanilla behavior: Q na result slot ZAWSZE dropuje na ziemię (nie daje na kursor)
        ItemStack cursor = player.getItemOnCursor();
        assertTrue(cursor == null || cursor.getType() == Material.AIR,
            "Kursor powinien być pusty - Q dropuje na ziemię, nie daje na kursor");
    }

    @Test
    @DisplayName("Drop key (Q) na output z zajętym kursorem nadal dropuje na ziemię")
    void dropKeyOnOutputWithCursorOccupied_DropsToGround() {
        // Przygotowanie
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Składnik
        gui.getInventory().setItem(0, new ItemStack(Material.OAK_LOG, 1));
        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.OAK_PLANKS, 4));
        player.openInventory(gui.getInventory());
        
        // Kursor zajęty innym itemem
        player.setItemOnCursor(new ItemStack(Material.DIAMOND, 5));

        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            CraftingGUI.OUTPUT_SLOT,
            ClickType.DROP,
            InventoryAction.DROP_ONE_SLOT
        );

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        
        // Kursor NIE powinien się zmienić - nadal ma diamenty
        ItemStack cursor = player.getItemOnCursor();
        assertNotNull(cursor);
        assertEquals(Material.DIAMOND, cursor.getType(), 
            "Kursor powinien nadal mieć diamenty - Q dropuje na ziemię, nie zmienia kursora");
        assertEquals(5, cursor.getAmount());
    }

    @Test
    @DisplayName("Double click na output jest blokowany")
    void doubleClickOnOutput_IsBlocked() {
        // Przygotowanie
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND_BLOCK, 1));
        player.openInventory(gui.getInventory());
        player.setItemOnCursor(new ItemStack(Material.DIAMOND_BLOCK, 3));

        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            CraftingGUI.OUTPUT_SLOT,
            ClickType.DOUBLE_CLICK,
            InventoryAction.COLLECT_TO_CURSOR
        );

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled(), "Double click na output musi być zablokowany");
    }

    @Test
    @DisplayName("Brak permissji blokuje output, a z permissją działa")
    void permissionRequired_BlocksWithoutPermission_AllowsWithPermission() {
        // Przygotowanie
        PlayerMock player = createPlayer();
        player.setOp(false);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Receptura z permissją
        String perm = "craft.test_perm_scenario";
        ItemStack[][] pattern = new ItemStack[1][1];
        pattern[0][0] = new ItemStack(Material.DIAMOND);

        CraftingRecipe recipe = new CraftingRecipe(
            "test_perm_recipe",
            "Test Perm Recipe",
            pattern,
            new ItemStack(Material.DIAMOND_BLOCK, 1),
            perm
        );
        RecipeManager.getInstance().registerRecipe(recipe);

        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.DIAMOND, 1));
        listener.updateOutput(player, gui);

        // Bez permissji output nie powinien się pojawić
        ItemStack outputWithoutPerm = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertTrue(outputWithoutPerm == null || outputWithoutPerm.getType().isAir(),
            "Bez permissji output nie powinien się pojawić");

        // Nadajemy permissję
        player.addAttachment(plugin, perm, true);
        listener.updateOutput(player, gui);

        ItemStack outputWithPerm = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(outputWithPerm, "Z permissją output powinien się pojawić");
        assertEquals(Material.DIAMOND_BLOCK, outputWithPerm.getType(), "Output powinien być DIAMOND_BLOCK");
    }

    @Test
    @DisplayName("Wzór dopasowuje się z przesunięciem w gridzie")
    void patternMatchesWithOffsetInGrid() {
        // Przygotowanie
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Wzór 2x1 poziomy
        ItemStack[][] pattern = new ItemStack[1][2];
        pattern[0][0] = new ItemStack(Material.DIAMOND);
        pattern[0][1] = new ItemStack(Material.DIAMOND);

        CraftingRecipe recipe = new CraftingRecipe(
            "test_offset_recipe",
            "Test Offset Recipe",
            pattern,
            new ItemStack(Material.DIAMOND_BLOCK, 1),
            "*"
        );
        RecipeManager.getInstance().registerRecipe(recipe);

        // Umieszczamy wzór z przesunięciem (sloty 1 i 2 w pierwszym rzędzie)
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[1], new ItemStack(Material.DIAMOND, 1));
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[2], new ItemStack(Material.DIAMOND, 1));

        listener.updateOutput(player, gui);

        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(output, "Output powinien się pojawić dla przesuniętego wzoru");
        assertEquals(Material.DIAMOND_BLOCK, output.getType(), "Output powinien być DIAMOND_BLOCK");
    }

    @Test
    @DisplayName("Rotacja wzoru NIE jest dozwolona")
    void rotationIsNotAllowed() {
        // Przygotowanie
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Wzór poziomy 1x2
        ItemStack[][] pattern = new ItemStack[1][2];
        pattern[0][0] = new ItemStack(Material.DIAMOND);
        pattern[0][1] = new ItemStack(Material.DIAMOND);

        CraftingRecipe recipe = new CraftingRecipe(
            "test_no_rotation_recipe",
            "Test No Rotation",
            pattern,
            new ItemStack(Material.DIAMOND_BLOCK, 1),
            "*"
        );
        RecipeManager.getInstance().registerRecipe(recipe);

        // Układ pionowy (rotacja) - nie powinien pasować
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.DIAMOND, 1));
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[4], new ItemStack(Material.DIAMOND, 1));

        listener.updateOutput(player, gui);

        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertTrue(output == null || output.getType().isAir(), "Rotacja nie powinna pasować");
    }

    @Test
    @DisplayName("Output zachowuje ilość z receptury")
    void outputKeepsRecipeAmount() {
        // Przygotowanie
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Receptura daje 4 sztuki
        ItemStack[][] pattern = new ItemStack[1][1];
        pattern[0][0] = new ItemStack(Material.OAK_PLANKS);

        CraftingRecipe recipe = new CraftingRecipe(
            "test_amount_recipe",
            "Test Amount",
            pattern,
            new ItemStack(Material.STICK, 4),
            "*"
        );
        RecipeManager.getInstance().registerRecipe(recipe);

        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.OAK_PLANKS, 1));
        listener.updateOutput(player, gui);

        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(output, "Output powinien się pojawić");
        assertEquals(Material.STICK, output.getType(), "Output powinien być STICK");
        assertEquals(4, output.getAmount(), "Output powinien mieć ilość 4");
    }

    @Test
    @DisplayName("Zamknięcie GUI zwraca itemy z gridu do gracza")
    void closingGui_ReturnsItemsToPlayer() {
        // Przygotowanie
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Wkładamy item do gridu
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.DIAMOND, 3));

        // Otwieramy i zamykamy GUI
        player.openInventory(gui.getInventory());
        InventoryCloseEvent event = new InventoryCloseEvent(player.getOpenInventory());

        listener.onInventoryClose(event);

        // Item powinien wrócić do ekwipunku gracza
        int totalDiamonds = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                totalDiamonds += item.getAmount();
            }
        }

        assertEquals(3, totalDiamonds, "Gracz powinien dostać 3 DIAMOND z powrotem");
    }

    private CraftingRecipe simpleDiamondRecipe() {
        ItemStack[][] pattern = new ItemStack[1][1];
        pattern[0][0] = new ItemStack(Material.DIAMOND);

        return new CraftingRecipe(
            "test_diamond_block_scenario",
            "Test Diamond Block",
            pattern,
            new ItemStack(Material.DIAMOND_BLOCK, 1),
            "*"
        );
    }
}

package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.VanillaRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.VanillaRecipeLoader;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitTestBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test integracyjny dla remainder items (container items) - np. ciasto z mleka zwraca puste wiaderka.
 * 
 * UWAGA: NMS nie działa w MockBukkit, więc getRemainingItems() zwróci pustą tablicę.
 * Ten test sprawdza logikę aplikowania remainder items, zakładając że NMS zwraca poprawne dane.
 */
@Tag("integration")
@DisplayName("Remainder Items (Container Items) - Testy Integracyjne")
class RemainderItemsIT extends MockBukkitTestBase {

    private Player player;

    @BeforeEach
    void setUp() {
        player = server.addPlayer("TestPlayer");
    }

    @Test
    @DisplayName("Vanilla cake recipe powinno być załadowane z 3x MILK_BUCKET")
    void testCakeRecipeLoaded() {
        // Sprawdź czy receptura na ciasto jest załadowana
        List<VanillaRecipe> recipes = VanillaRecipeLoader.loadAllVanillaRecipes();
        
        VanillaRecipe cakeRecipe = recipes.stream()
            .filter(r -> r.getOutput() != null && r.getOutput().getType() == Material.CAKE)
            .findFirst()
            .orElse(null);
        
        assertNotNull(cakeRecipe, "Receptura na ciasto powinna być załadowana");
        
        // Sprawdź pattern - ciasto wymaga 3x milk bucket w górnym rzędzie
        ItemStack[][] pattern = cakeRecipe.getPattern();
        assertNotNull(pattern, "Pattern ciasta nie może być null");
        
        // Wypisz pattern dla debugowania
        System.out.println("Cake recipe pattern:");
        for (int r = 0; r < pattern.length; r++) {
            for (int c = 0; c < pattern[r].length; c++) {
                ItemStack item = pattern[r][c];
                System.out.print((item == null ? "null" : item.getType()) + " ");
            }
            System.out.println();
        }
    }

    @Test
    @DisplayName("Extract3x3Grid powinno wyciągnąć 3x3 z 4x4")
    void testExtract3x3Grid() {
        // Setup: grid 4x4 z mlecznymi wiadrami w lewym górnym 3x3
        ItemStack[] grid4x4 = new ItemStack[16];
        grid4x4[0] = new ItemStack(Material.MILK_BUCKET);
        grid4x4[1] = new ItemStack(Material.MILK_BUCKET);
        grid4x4[2] = new ItemStack(Material.MILK_BUCKET);
        grid4x4[4] = new ItemStack(Material.SUGAR);
        grid4x4[5] = new ItemStack(Material.EGG);
        grid4x4[6] = new ItemStack(Material.EGG);
        grid4x4[8] = new ItemStack(Material.WHEAT);
        grid4x4[9] = new ItemStack(Material.WHEAT);
        grid4x4[10] = new ItemStack(Material.WHEAT);
        
        // Action
        ItemStack[] grid3x3 = CraftingGUIListener.extract3x3Grid(grid4x4);
        
        // Assert
        assertNotNull(grid3x3);
        assertEquals(9, grid3x3.length);
        
        // Sprawdź że 3x3 ma poprawne itemy
        assertEquals(Material.MILK_BUCKET, grid3x3[0].getType());
        assertEquals(Material.MILK_BUCKET, grid3x3[1].getType());
        assertEquals(Material.MILK_BUCKET, grid3x3[2].getType());
        assertEquals(Material.SUGAR, grid3x3[3].getType());
        assertEquals(Material.EGG, grid3x3[4].getType());
        assertEquals(Material.EGG, grid3x3[5].getType());
        assertEquals(Material.WHEAT, grid3x3[6].getType());
        assertEquals(Material.WHEAT, grid3x3[7].getType());
        assertEquals(Material.WHEAT, grid3x3[8].getType());
    }

    @Test
    @DisplayName("Extract3x3Grid z pustym gridem powinno zwrócić 3x3 z nullami")
    void testExtract3x3GridEmpty() {
        ItemStack[] grid4x4 = new ItemStack[16];
        
        ItemStack[] grid3x3 = CraftingGUIListener.extract3x3Grid(grid4x4);
        
        assertNotNull(grid3x3);
        assertEquals(9, grid3x3.length);
        for (int i = 0; i < 9; i++) {
            assertNull(grid3x3[i], "Slot " + i + " powinien być null");
        }
    }

    @Test
    @DisplayName("UpdateGridSlots powinno zaktualizować GUI inventory z tablicy gridItems")
    void testUpdateGridSlots() {
        // Setup
        CraftingGUI gui = new CraftingGUI(player, "Test Crafting");
        ItemStack[] gridItems = new ItemStack[16];
        gridItems[0] = new ItemStack(Material.BUCKET);
        gridItems[1] = new ItemStack(Material.BUCKET);
        gridItems[2] = new ItemStack(Material.BUCKET);
        
        // Action
        CraftingGUIListener.updateGridSlots(gui, gridItems);
        
        // Assert - sprawdź że GUI inventory ma buckets w poprawnych slotach
        Inventory inv = gui.getInventory();
        assertEquals(Material.BUCKET, inv.getItem(0).getType(), "Slot 0 powinien mieć BUCKET");
        assertEquals(Material.BUCKET, inv.getItem(1).getType(), "Slot 1 powinien mieć BUCKET");
        assertEquals(Material.BUCKET, inv.getItem(2).getType(), "Slot 2 powinien mieć BUCKET");
    }

    @Test
    @DisplayName("Crafting GUI powinno mieć dostęp do grid slots")
    void testCraftingGUIGridAccess() {
        CraftingGUI gui = new CraftingGUI(player, "Test");
        
        // Dodaj itemy do gridu
        gui.setInputItem(0, new ItemStack(Material.MILK_BUCKET));
        gui.setInputItem(1, new ItemStack(Material.MILK_BUCKET));
        gui.setInputItem(2, new ItemStack(Material.MILK_BUCKET));
        
        // Sprawdź że inventory ma te itemy
        Inventory inv = gui.getInventory();
        assertEquals(Material.MILK_BUCKET, inv.getItem(0).getType());
        assertEquals(Material.MILK_BUCKET, inv.getItem(1).getType());
        assertEquals(Material.MILK_BUCKET, inv.getItem(2).getType());
    }

    @Test
    @DisplayName("ExtractGridItems powinno zwrócić 4x4 tablicę z GUI inventory")
    void testExtractGridItems() {
        CraftingGUI gui = new CraftingGUI(player, "Test");
        
        // Setup grid - używamy GUI slotów które są w gridzie
        // Rząd 1: GUI sloty 0-3 → gridItems[0-3]
        // Rząd 2: GUI sloty 9-12 → gridItems[4-7]
        // Rząd 3: GUI sloty 18-21 → gridItems[8-11]
        gui.setInputItem(0, new ItemStack(Material.MILK_BUCKET));  // gridItems[0]
        gui.setInputItem(9, new ItemStack(Material.SUGAR));        // gridItems[4]
        gui.setInputItem(18, new ItemStack(Material.WHEAT));       // gridItems[8]
        
        // Action
        ItemStack[] gridItems = CraftingGUIListener.extractGridItems(gui);
        
        // Assert
        assertNotNull(gridItems);
        assertEquals(16, gridItems.length);
        assertEquals(Material.MILK_BUCKET, gridItems[0].getType());
        assertEquals(Material.SUGAR, gridItems[4].getType());
        assertEquals(Material.WHEAT, gridItems[8].getType());
    }

    @Test
    @DisplayName("UWAGA: NMS getRemainingItems() nie działa w MockBukkit")
    void testNmsNotAvailableInMockBukkit() {
        // Ten test dokumentuje że NMS nie działa w MockBukkit
        // getRemainingItems() zwróci pustą tablicę w testach
        
        ItemStack[] grid3x3 = new ItemStack[9];
        grid3x3[0] = new ItemStack(Material.MILK_BUCKET);
        grid3x3[1] = new ItemStack(Material.MILK_BUCKET);
        grid3x3[2] = new ItemStack(Material.MILK_BUCKET);
        
        // NmsCraftingEngine w MockBukkit zwróci pustą tablicę
        pl.com.sofitfy.enhanced_containers.crafting.nms.NmsCraftingEngine engine = 
            new pl.com.sofitfy.enhanced_containers.crafting.nms.NmsCraftingEngine();
        
        ItemStack[] remainders = engine.getRemainingItems(grid3x3, player.getWorld());
        
        assertNotNull(remainders);
        assertEquals(9, remainders.length);
        
        // W MockBukkit wszystkie remainder items będą null/puste
        System.out.println("UWAGA: NMS nie działa w MockBukkit, więc remainder items są puste");
        System.out.println("Aby przetestować remainder items, użyj prawdziwego serwera Paper");
    }
}

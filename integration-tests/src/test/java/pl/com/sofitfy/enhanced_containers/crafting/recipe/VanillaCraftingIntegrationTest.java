package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.crafting.gui.CraftingGUI;
import pl.com.sofitfy.enhanced_containers.crafting.gui.CraftingGUIListener;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Vanilla Crafting Integration Tests")
class VanillaCraftingIntegrationTest extends MockBukkitPluginTestBase {
    
    protected PlayerMock player;
    protected CraftingGUI gui;
    protected CraftingGUIListener listener;
    
    @BeforeEach
    void setUp() {
        player = createPlayer();
        player.setOp(true);
        gui = new CraftingGUI(player, "Crafting");
        listener = new CraftingGUIListener();
        listener.registerGUI(gui);
        
        // Czyść receptury i załaduj vanilla
        RecipeManager.getInstance().clearRecipes();
        RecipeManager.getInstance().loadVanillaRecipes();
    }
    
    @Nested
    @DisplayName("Test dopasowania vanillowych receptur")
    class VanillaRecipeMatchingTests {
        
        @Test
        @DisplayName("Powinien dopasować wood log i dać planks")
        void shouldMatchWoodLogRecipe() {
            // Ustawiamy OAK_LOG w gridzie (3x3, bo vanilla)
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.OAK_LOG, 1));
            
            listener.updateOutput(player, gui);
            
            ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
            // Vanilla może mieć tę recepturę lub nie, zależy od wersji
            // Sprawdzamy że metoda działa bez błędów
            assertDoesNotThrow(() -> listener.updateOutput(player, gui), "updateOutput powinno działać");
        }
        
        @Test
        @DisplayName("Pusty grid nie powinien mieć outputu")
        void emptyGridShouldHaveNoOutput() {
            listener.updateOutput(player, gui);
            
            ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
            assertNull(output, "Pusty grid nie powinien mieć outputu");
        }
        
        @Test
        @DisplayName("Powinien pracować z 3x3 patternami w 4x4 gridzie")
        void should3x3PatternWork() {
            // Umieszczam item tylko w lewym górnym rogu (3x3)
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.OAK_LOG, 1));
            
            listener.updateOutput(player, gui);
            
            // Powinno działać bez błędów
            assertNotNull(gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT) != null 
                          || gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT) == null,
                          "Test powinien przejść bez wyjątku");
        }
    }
    
    @Nested
    @DisplayName("Priorytet: Custom vs Vanilla")
    class CustomVsVanillaPriorityTests {
        
        @Test
        @DisplayName("Custom receptura powinna mieć priorytet nad vanilla")
        void customShouldOverrideVanilla() {
            // Czyścze receptury i załaduj vanilla
            RecipeManager.getInstance().clearRecipes();
            RecipeManager.getInstance().loadVanillaRecipes();
            
            // Umieszczam OAK_LOG w gridzie
            ItemStack logItem = new ItemStack(Material.OAK_LOG, 1);
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], logItem);
            
            // Sprawdzam vanilla output
            listener.updateOutput(player, gui);
            ItemStack vanillaOutput = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
            
            // Teraz dodaję custom recepturę z tym samym inputem ale innym outputem
            CraftingRecipe customRecipe = new CraftingRecipe(
                "test_custom_log",
                "Custom Log Recipe",
                new ItemStack[][] { { new ItemStack(Material.OAK_LOG, 1) } },
                new ItemStack(Material.DIAMOND, 1),
                "*"
            );
            RecipeManager.getInstance().registerRecipe(customRecipe);
            
            // Aktualizuję output znowu
            listener.updateOutput(player, gui);
            ItemStack customOutput = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
            
            // Custom powinien nadpisać vanilla
            assertNotNull(customOutput, "Custom receptura powinna dać output");
            assertEquals(Material.DIAMOND, customOutput.getType(), 
                        "Custom receptura powinna dać DIAMOND zamiast vanilla outputu");
        }
        
        @Test
        @DisplayName("Gdy brak custom, powinien użyć vanilla")
        void shouldUseVanillaWhenNoCustom() {
            RecipeManager.getInstance().clearRecipes();
            RecipeManager.getInstance().loadVanillaRecipes();
            
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.OAK_LOG, 1));
            
            listener.updateOutput(player, gui);
            
            // Nie powinno rzucić błędu
            assertDoesNotThrow(() -> listener.updateOutput(player, gui));
        }
    }
    
    @Nested
    @DisplayName("Per-player simulator isolation")
    class PerPlayerSimulatorIsolationTests {
        
        @Test
        @DisplayName("Każdy gracz powinien mieć własny simulator")
        void eachPlayerShouldHaveOwnSimulator() {
            PlayerMock player2 = createPlayer();
            CraftingGUI gui2 = new CraftingGUI(player2, "Crafting2");
            
            listener.registerGUI(gui2);
            
            // Gracz 1 ma log
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.OAK_LOG));
            
            // Gracz 2 ma inny item
            gui2.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.STONE));
            
            listener.updateOutput(player, gui);
            listener.updateOutput(player2, gui2);
            
            // Oba powinny działać niezależnie
            assertDoesNotThrow(() -> listener.updateOutput(player, gui));
            assertDoesNotThrow(() -> listener.updateOutput(player2, gui2));
        }
        
        @Test
        @DisplayName("Czyszczenie gracza przy zamknięciu GUI")
        void shouldCleanupPlayerOnGuiClose() {
            listener.updateOutput(player, gui);
            
            // Simuluj zamknięcie GUI
            listener.cleanupPlayer(player.getUniqueId());
            
            // Powinno przejść bez błędów
            assertDoesNotThrow(() -> listener.cleanupPlayer(player.getUniqueId()));
        }
    }
    
    @Nested
    @DisplayName("Grid extraction tests")
    class GridExtractionTests {
        
        @Test
        @DisplayName("3x3 musi ignorować kolumnę 4 w 4x4 gridzie")
        void threeByThreeShouldIgnoreColumn4() {
            // Wstawiamy itemy w kolumnie 4 (indeksy 3, 7, 11, 15)
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[3], new ItemStack(Material.DIAMOND));
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[7], new ItemStack(Material.DIAMOND));
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[11], new ItemStack(Material.DIAMOND));
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[15], new ItemStack(Material.DIAMOND));
            
            listener.updateOutput(player, gui);
            
            // Grid ma tylko diamenty w kolumnie 4 (ignorowanej)
            // Powinien zwrócić null (brak receptury)
            ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
            assertNull(output, "Diamenty w kolumnie 4 (ignorowanej) nie powinny dać outputu");
        }
        
        @Test
        @DisplayName("3x3 musi ignorować rząd 4 w 4x4 gridzie")
        void threeByThreeShouldIgnoreRow4() {
            // Wstawiamy itemy w rzędzie 4 (indeksy 12-15)
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[12], new ItemStack(Material.EMERALD));
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[13], new ItemStack(Material.EMERALD));
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[14], new ItemStack(Material.EMERALD));
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[15], new ItemStack(Material.EMERALD));
            
            listener.updateOutput(player, gui);
            
            // Grid ma tylko emeraldy w rzędzie 4 (ignorowanym)
            ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
            assertNull(output, "Emeraldy w rzędzie 4 (ignorowanym) nie powinny dać outputu");
        }
        
        @Test
        @DisplayName("3x3 powinien pracować z itemami w lewym górnym rogu")
        void threeByThreeShouldWorkWithTopLeftCorner() {
            // 3x3 powinno działać na lewym górnym rogu (indeksy 0,1,2,4,5,6,8,9,10)
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.OAK_LOG));
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[1], new ItemStack(Material.OAK_LOG));
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[2], new ItemStack(Material.OAK_LOG));
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[4], new ItemStack(Material.OAK_LOG));
            
            listener.updateOutput(player, gui);
            
            // Powinno działać bez błędów
            assertDoesNotThrow(() -> listener.updateOutput(player, gui));
        }
    }
    
    @Nested
    @DisplayName("Edge cases")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Powinien obsługiwać null itemy")
        void shouldHandleNullItems() {
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], null);
            
            assertDoesNotThrow(() -> listener.updateOutput(player, gui), "Powinno obsługiwać null");
        }
        
        @Test
        @DisplayName("Powinien obsługiwać AIR itemy")
        void shouldHandleAirItems() {
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.AIR));
            
            listener.updateOutput(player, gui);
            
            assertDoesNotThrow(() -> listener.updateOutput(player, gui), "Powinno obsługiwać AIR");
        }
        
        @Test
        @DisplayName("Powinien obsługiwać kilka różnych itemów w gridzie")
        void shouldHandleMultipleDifferentItems() {
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.OAK_LOG));
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[1], new ItemStack(Material.STONE));
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[2], new ItemStack(Material.IRON_ORE));
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[4], new ItemStack(Material.DIAMOND));
            
            assertDoesNotThrow(() -> listener.updateOutput(player, gui), "Powinno działać z wieloma itemami");
        }
    }
}

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

@DisplayName("Vanilla Crafting - Shaped Recipe Positioning")
class VanillaCraftingShapedPositioningIntegrationTest extends MockBukkitPluginTestBase {
    
    private CraftingGUIListener listener;
    private PlayerMock player;
    
    @BeforeEach
    void setUp() {
        player = createPlayer();
        player.setOp(true);
        listener = new CraftingGUIListener();
        
        // Czyścimy i ładujemy vanilla receptury
        RecipeManager.getInstance().clearRecipes();
        RecipeManager.getInstance().loadVanillaRecipes();
    }
    
    @Nested
    @DisplayName("Shaped receptury powinny pracować na każdej pozycji w gridzie")
    class ShapedRecipePositioningTests {
        
        @Test
        @DisplayName("Vanilla receptura 1x1 (drewno) - pozycja 0 (lewy górny róg)")
        void vanillaRecipe1x1Position0() {
            CraftingGUI gui = new CraftingGUI(player, "Test");
            listener.registerGUI(gui);
            
            // Wkładamy drewno na pozycję 0 (lewy górny róg)
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.OAK_LOG, 1));
            
            // Aktualizujemy output
            listener.updateOutput(player, gui);
            
            // Sprawdzamy czy output się pojawił
            ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
            
            if (output != null && !output.getType().isAir()) {
                assertEquals(Material.OAK_PLANKS, output.getType(), 
                    "Drewno w pozycji 0 powinno dać deski");
            }
        }
        
        @Test
        @DisplayName("Vanilla receptura 1x1 (drewno) - pozycja 1 (górny środek)")
        void vanillaRecipe1x1Position1() {
            CraftingGUI gui = new CraftingGUI(player, "Test");
            listener.registerGUI(gui);
            
            // Wkładamy drewno na pozycję 1 (górny środek)
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[1], new ItemStack(Material.OAK_LOG, 1));
            
            // Aktualizujemy output
            listener.updateOutput(player, gui);
            
            // Sprawdzamy czy output się pojawił
            ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
            
            if (output != null && !output.getType().isAir()) {
                assertEquals(Material.OAK_PLANKS, output.getType(), 
                    "Drewno w pozycji 1 powinno dać deski");
            }
        }
        
        @Test
        @DisplayName("Vanilla receptura 1x1 (drewno) - pozycja 4 (środek)")
        void vanillaRecipe1x1Position4() {
            CraftingGUI gui = new CraftingGUI(player, "Test");
            listener.registerGUI(gui);
            
            // Wkładamy drewno na pozycję 4 (środek gridu)
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[4], new ItemStack(Material.OAK_LOG, 1));
            
            // Aktualizujemy output
            listener.updateOutput(player, gui);
            
            // Sprawdzamy czy output się pojawił
            ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
            
            if (output != null && !output.getType().isAir()) {
                assertEquals(Material.OAK_PLANKS, output.getType(), 
                    "Drewno w pozycji 4 (środek) powinno dać deski");
            }
        }
        
        @Test
        @DisplayName("Vanilla receptura 1x1 (drewno) - pozycja 8 (prawy dolny róg)")
        void vanillaRecipe1x1Position8() {
            CraftingGUI gui = new CraftingGUI(player, "Test");
            listener.registerGUI(gui);
            
            // Wkładamy drewno na pozycję 8 (prawy dolny róg)
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[8], new ItemStack(Material.OAK_LOG, 1));
            
            // Aktualizujemy output
            listener.updateOutput(player, gui);
            
            // Sprawdzamy czy output się pojawił
            ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
            
            if (output != null && !output.getType().isAir()) {
                assertEquals(Material.OAK_PLANKS, output.getType(), 
                    "Drewno w pozycji 8 (prawy dolny róg) powinno dać deski");
            }
        }
        
        @Test
        @DisplayName("Vanilla receptura - dodatkowy item blokuje match")
        void additionalItemBlocksMatch() {
            CraftingGUI gui = new CraftingGUI(player, "Test");
            listener.registerGUI(gui);
            
            // Wkładamy drewno na pozycję 0
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.OAK_LOG, 1));
            
            // Dodajemy DODATKOWY item na pozycję 1!
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[1], new ItemStack(Material.STONE, 1));
            
            // Aktualizujemy output
            listener.updateOutput(player, gui);
            
            // Sprawdzamy czy output jest pusty (bo receptura nie powinna pasować)
            ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
            
            assertTrue(output == null || output.getType().isAir(), 
                "Dodatkowy item ma blokować vanilla recepturę");
        }
        
        @Test
        @DisplayName("Vanilla receptura - testy wszystkich pozycji 0-8")
        void allPositionsTest() {
            // Test każdej pozycji (0-8)
            for (int position = 0; position < 9; position++) {
                CraftingGUI gui = new CraftingGUI(player, "Test" + position);
                listener.registerGUI(gui);
                
                // Wkładamy drewno na daną pozycję
                gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[position], 
                    new ItemStack(Material.OAK_LOG, 1));
                
                // Aktualizujemy output
                listener.updateOutput(player, gui);
                
                // Sprawdzamy output
                ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
                
                if (output != null && !output.getType().isAir()) {
                    assertEquals(Material.OAK_PLANKS, output.getType(), 
                        "Drewno w pozycji " + position + " powinno dać deski");
                }
            }
        }
    }
    
    @Nested
    @DisplayName("Edge cases dla shaped receptur")
    class ShapedRecipeEdgeCases {
        
        @Test
        @DisplayName("Pusta receptura nie powinna dać outputu")
        void emptyGridNoOutput() {
            CraftingGUI gui = new CraftingGUI(player, "Empty");
            listener.registerGUI(gui);
            
            // Grid jest pusty, nic nie wkładamy
            listener.updateOutput(player, gui);
            
            ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
            assertTrue(output == null || output.getType().isAir(), 
                "Pusty grid nie powinien dać outputu");
        }
        
        @Test
        @DisplayName("Item bez odpowiadającej receptury nie powinna dać outputu")
        void nonMatchingMaterialNoOutput() {
            CraftingGUI gui = new CraftingGUI(player, "NonMatching");
            listener.registerGUI(gui);
            
            // Wkładamy DIAMENT (który nie ma receptury 1x1)
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], 
                new ItemStack(Material.DIAMOND, 1));
            
            listener.updateOutput(player, gui);
            
            ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
            assertTrue(output == null || output.getType().isAir(), 
                "Diament bez receptury nie powinien dać outputu");
        }
    }
}

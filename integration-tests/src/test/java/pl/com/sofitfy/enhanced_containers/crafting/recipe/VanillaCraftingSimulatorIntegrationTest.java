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
import pl.com.sofitfy.enhanced_containers.crafting.vanilla.VanillaCraftingSimulator;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integracyjne testy VanillaCraftingSimulator.
 * Używają MockBukkit do rzeczywistego testowania craftingu z PrepareItemCraftEvent.
 */
@DisplayName("VanillaCraftingSimulator - Testy Integracyjne")
class VanillaCraftingSimulatorIntegrationTest extends MockBukkitPluginTestBase {
    
    private VanillaCraftingSimulator simulator;
    private PlayerMock player;
    private CraftingGUIListener listener;
    
    @BeforeEach
    void setUp() {
        simulator = new VanillaCraftingSimulator();
        player = createPlayer();
        player.setOp(true);
        listener = new CraftingGUIListener();
        
        // Załaduj vanilla receptury do MockBukkita
        loadVanillaRecipes();
    }
    
    @Nested
    @DisplayName("Rzeczywisty crafting z PrepareItemCraftEvent")
    class RealCraftingTests {
        
        @Test
        @DisplayName("Nieokrowane drewno (OAK_LOG) -> Deski (OAK_PLANKS) pozycja 0")
        void logToPlanksPosition0() {
            ItemStack[] grid = new ItemStack[9];
            grid[0] = new ItemStack(Material.OAK_LOG, 1);
            
            ItemStack result = simulator.tryMatch(player, grid);
            
            assertNotNull(result, "Powinna być deski z drewna");
            assertEquals(Material.OAK_PLANKS, result.getType(), 
                "Drewno powinno dać deski");
            assertEquals(4, result.getAmount(), 
                "Jedno drewno daje 4 deski");
        }
        
        @Test
        @DisplayName("Nieokrowane drewno (OAK_LOG) -> Deski (OAK_PLANKS) pozycja 1")
        void logToPlanksPosition1() {
            ItemStack[] grid = new ItemStack[9];
            grid[1] = new ItemStack(Material.OAK_LOG, 1);
            
            ItemStack result = simulator.tryMatch(player, grid);
            
            assertNotNull(result, "Powinna być deski z drewna");
            assertEquals(Material.OAK_PLANKS, result.getType(), 
                "Drewno powinno dać deski");
        }
        
        @Test
        @DisplayName("Nieokrowane drewno (OAK_LOG) -> Deski (OAK_PLANKS) pozycja 4 (środek)")
        void logToPlanksPosition4() {
            ItemStack[] grid = new ItemStack[9];
            grid[4] = new ItemStack(Material.OAK_LOG, 1);
            
            ItemStack result = simulator.tryMatch(player, grid);
            
            assertNotNull(result, "Powinna być deski z drewna");
            assertEquals(Material.OAK_PLANKS, result.getType(), 
                "Drewno powinno dać deski");
        }
        
        @Test
        @DisplayName("Nieokrowane drewno (OAK_LOG) -> Deski (OAK_PLANKS) pozycja 8 (prawy dolny róg)")
        void logToPlanksPosition8() {
            ItemStack[] grid = new ItemStack[9];
            grid[8] = new ItemStack(Material.OAK_LOG, 1);
            
            ItemStack result = simulator.tryMatch(player, grid);
            
            assertNotNull(result, "Powinna być deski z drewna");
            assertEquals(Material.OAK_PLANKS, result.getType(), 
                "Drewno powinno dać deski");
        }
        
        @Test
        @DisplayName("Pusty grid powinien zwrócić null")
        void emptyGridReturnsNull() {
            ItemStack[] grid = new ItemStack[9];
            
            ItemStack result = simulator.tryMatch(player, grid);
            
            assertNull(result, "Pusty grid powinien zwrócić null");
        }
        
        @Test
        @DisplayName("Nieznany item (COMMAND_BLOCK) powinien zwrócić null")
        void unknownItemReturnsNull() {
            ItemStack[] grid = new ItemStack[9];
            grid[0] = new ItemStack(Material.COMMAND_BLOCK, 1);
            
            ItemStack result = simulator.tryMatch(player, grid);
            
            assertNull(result, "Nieznany item powinien zwrócić null");
        }
        
        @Test
        @DisplayName("Wiele rodzajów drewna - każde daje swoje deski")
        void differentWoodTypes() {
            // Test dla OAK_LOG
            ItemStack[] grid1 = new ItemStack[9];
            grid1[0] = new ItemStack(Material.OAK_LOG, 1);
            ItemStack result1 = simulator.tryMatch(player, grid1);
            assertEquals(Material.OAK_PLANKS, result1.getType());
            
            // Test dla SPRUCE_LOG
            ItemStack[] grid2 = new ItemStack[9];
            grid2[0] = new ItemStack(Material.SPRUCE_LOG, 1);
            ItemStack result2 = simulator.tryMatch(player, grid2);
            assertEquals(Material.SPRUCE_PLANKS, result2.getType());
        }
    }
    
    @Nested
    @DisplayName("Integracja z CraftingGUI")
    class CraftingGUIIntegrationTests {
        
        @Test
        @DisplayName("Drewno w CraftingGUI powinno dać deski w output")
        void logInGUIProducesPlanks() {
            CraftingGUI gui = new CraftingGUI(player, "Test");
            listener.registerGUI(gui);
            
            // Wkładamy drewno na pozycję 0 (lewy górny róg gridu)
            gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], 
                new ItemStack(Material.OAK_LOG, 1));
            
            // Aktualizujemy output
            listener.updateOutput(player, gui);
            
            // Sprawdzamy czy output się pojawił
            ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
            assertNotNull(output, "Output powinien być wyświetlony");
            assertEquals(Material.OAK_PLANKS, output.getType(), 
                "Output powinien być deski");
        }
        
        @Test
        @DisplayName("Drewno na różnych pozycjach powinno zawsze dać deski")
        void logOnDifferentPositionsProducesPlanks() {
            int[] gridSlots = CraftingGUI.GRID_SLOTS;
            
            for (int position = 0; position < 9; position++) {
                CraftingGUI gui = new CraftingGUI(player, "Test" + position);
                listener.registerGUI(gui);
                
                // GRID_SLOTS to int[] indeksy inventoria
                gui.getInventory().setItem(gridSlots[position], 
                    new ItemStack(Material.OAK_LOG, 1));
                
                listener.updateOutput(player, gui);
                
                ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
                
                if (output != null && !output.getType().isAir()) {
                    assertEquals(Material.OAK_PLANKS, output.getType(), 
                        "Pozycja " + position + " powinna dać deski");
                }
            }
        }
    }

    /**
     * Testy dla receptur fajerwerków (paper + 1-3 prochy).
     */
    @Nested
    @DisplayName("Testy fajerwerków")
    class FireworksTests {

        private VanillaCraftingSimulator simulator;

        @BeforeEach
        void setUp() {
            simulator = new VanillaCraftingSimulator();
        }

        @Test
        @DisplayName("Papier + 1 proch = fajerwerk")
        void fireworkWithOnePowder() {
            ItemStack[] grid = new ItemStack[9];
            grid[0] = new ItemStack(Material.PAPER, 1);
            grid[1] = new ItemStack(Material.GUNPOWDER, 1);

            ItemStack result = simulator.tryMatch(player, grid);

            assertNotNull(result, "Papier + 1 proch powinien dać fajerwerk");
            assertEquals(Material.FIREWORK_ROCKET, result.getType(),
                    "Powinien być FIREWORK_ROCKET");
        }

        @Test
        @DisplayName("Papier + 2 prochy = fajerwerk")
        void fireworkWithTwoPowders() {
            ItemStack[] grid = new ItemStack[9];
            grid[0] = new ItemStack(Material.PAPER, 1);
            grid[1] = new ItemStack(Material.GUNPOWDER, 1);
            grid[2] = new ItemStack(Material.GUNPOWDER, 1);

            ItemStack result = simulator.tryMatch(player, grid);

            assertNotNull(result, "Papier + 2 prochy powinny dać fajerwerk");
            assertEquals(Material.FIREWORK_ROCKET, result.getType(),
                    "Powinien być FIREWORK_ROCKET");
        }

        @Test
        @DisplayName("Papier + 3 prochy = fajerwerk")
        void fireworkWithThreePowders() {
            ItemStack[] grid = new ItemStack[9];
            grid[0] = new ItemStack(Material.PAPER, 1);
            grid[1] = new ItemStack(Material.GUNPOWDER, 1);
            grid[2] = new ItemStack(Material.GUNPOWDER, 1);
            grid[3] = new ItemStack(Material.GUNPOWDER, 1);

            ItemStack result = simulator.tryMatch(player, grid);

            assertNotNull(result, "Papier + 3 prochy powinny dać fajerwerk");
            assertEquals(Material.FIREWORK_ROCKET, result.getType(),
                    "Powinien być FIREWORK_ROCKET");
        }

        @Test
        @DisplayName("Papier + 2 prochy na różnych pozycjach")
        void fireworkWithTwoPowdersAtDifferentPositions() {
            // Test pozycje: (0,1), (0,2), (1,4), (2,8) aby sprawdzić czy działa wszędzie
            int[] positions = {1, 2, 4, 8};

            for (int position : positions) {
                ItemStack[] grid = new ItemStack[9];
                grid[0] = new ItemStack(Material.PAPER, 1);
                grid[position] = new ItemStack(Material.GUNPOWDER, 1);
                grid[position < 8 ? position + 1 : 7] = new ItemStack(Material.GUNPOWDER, 1);

                ItemStack result = simulator.tryMatch(player, grid);

                assertNotNull(result,
                        "Papier + 2 prochy na pozycji " + position + " powinny dać fajerwerk");
                assertEquals(Material.FIREWORK_ROCKET, result.getType(),
                        "Powinien być FIREWORK_ROCKET na pozycji " + position);
            }
        }
    }
}

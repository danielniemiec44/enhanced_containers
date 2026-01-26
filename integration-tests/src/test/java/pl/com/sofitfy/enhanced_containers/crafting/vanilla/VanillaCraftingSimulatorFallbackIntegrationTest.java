package pl.com.sofitfy.enhanced_containers.crafting.vanilla;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Tests dla VanillaCraftingSimulator - Fallback Mechanism.
 * 
 * Testuje:
 * 1. Obsługę Vanilla receptur z Tagami (RecipeChoice)
 * 2. Obsługę ComplexRecipes (naprawianie, fajerwerki)
 * 3. Działanie "Shrink Wrap" + fallback dla 4x4
 * 4. Interakcję z Paper/Bukkit API
 * 
 * WAŻNE: Te testy używają MockBukkit do pełnej symulacji serwera Bukkit.
 */
@DisplayName("VanillaCraftingSimulator - Fallback Mechanism Integration Tests")
public class VanillaCraftingSimulatorFallbackIntegrationTest {

    private ServerMock serverMock;
    private PlayerMock player;
    private VanillaCraftingSimulator simulator;

    @BeforeEach
    void setUp() {
        serverMock = MockBukkit.mock();
        player = serverMock.addPlayer();
        simulator = new VanillaCraftingSimulator();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Nested
    @DisplayName("3x3 Vanilla Crafting Tests (Basic)")
    class BasicVanillaCrafting3x3 {

        @Test
        @DisplayName("Deska drewna (1x OAK_LOG -> 4x OAK_PLANKS)")
        void oakLogToPlanks() {
            ItemStack[] grid3x3 = new ItemStack[9];
            grid3x3[0] = new ItemStack(Material.OAK_LOG);
            
            ItemStack result = simulator.tryMatch(player, grid3x3);
            
            assertNotNull(result, "Vanilla receptura deska powinna pasować");
            assertEquals(Material.OAK_PLANKS, result.getType());
            assertEquals(4, result.getAmount(), "Powinno być 4 desek");
        }

        @Test
        @DisplayName("Deseczki z drewna (1x1 OAK_WOOD -> OAK_PLANKS)")
        void planksCrafting() {
            ItemStack[] grid3x3 = new ItemStack[9];
            // Drewno w slocie [0,0]
            grid3x3[0] = new ItemStack(Material.OAK_WOOD);
            
            ItemStack result = simulator.tryMatch(player, grid3x3);
            
            assertNotNull(result, "Crafting desek z drewna powinien pasować");
            assertEquals(Material.OAK_PLANKS, result.getType());
        }

        @Test
        @DisplayName("Furnace (8x stone -> Furnace) - CRAFT TABLE")
        void furnaceCrafting() {
            ItemStack[] grid3x3 = new ItemStack[9];
            // Crafting table: 4x oak planks (2x2)
            grid3x3[0] = new ItemStack(Material.OAK_PLANKS);
            grid3x3[1] = new ItemStack(Material.OAK_PLANKS);
            grid3x3[3] = new ItemStack(Material.OAK_PLANKS);
            grid3x3[4] = new ItemStack(Material.OAK_PLANKS);
            
            ItemStack result = simulator.tryMatch(player, grid3x3);
            
            assertNotNull(result, "Craft table powinien być dostępny");
            assertEquals(Material.CRAFTING_TABLE, result.getType());
        }

        @Test
        @DisplayName("Pusta siatka -> null")
        void emptyGrid_returnsNull() {
            ItemStack[] grid3x3 = new ItemStack[9];
            
            ItemStack result = simulator.tryMatch(player, grid3x3);
            
            assertNull(result, "Pusta siatka nie powinna zwrócić nic");
        }

        @Test
        @DisplayName("Brak receptury -> null")
        void noRecipe_returnsNull() {
            ItemStack[] grid3x3 = new ItemStack[9];
            grid3x3[0] = new ItemStack(Material.BEDROCK);  // Bedrock nie ma receptury
            
            ItemStack result = simulator.tryMatch(player, grid3x3);
            
            assertNull(result, "Bedrock nie ma receptury - powinno być null");
        }
    }

    @Nested
    @DisplayName("4x4 Fallback Mechanism Tests")
    class Fallback4x4Mechanism {

        @Test
        @DisplayName("Deska w 4x4 (1 OAK_LOG w różnych pozycjach -> zawsze 4x PLANKS)")
        void oakLogIn4x4_variousPositions() {
            // Test 1: Log w pozycji [0] (górny-lewy)
            ItemStack[] grid4x4_topLeft = new ItemStack[16];
            grid4x4_topLeft[0] = new ItemStack(Material.OAK_LOG);
            
            ItemStack result1 = simulator.tryMatch4x4Fallback(player, grid4x4_topLeft);
            assertNotNull(result1, "Log w górnym-lewym rogu");
            assertEquals(Material.OAK_PLANKS, result1.getType());
            
            // Test 2: Log w pozycji [6] (row 1, col 2)
            ItemStack[] grid4x4_middle = new ItemStack[16];
            grid4x4_middle[6] = new ItemStack(Material.OAK_LOG);
            
            ItemStack result2 = simulator.tryMatch4x4Fallback(player, grid4x4_middle);
            assertNotNull(result2, "Log w środku siatki");
            assertEquals(Material.OAK_PLANKS, result2.getType());
            
            // Test 3: Log w pozycji [15] (dolny-prawy)
            ItemStack[] grid4x4_bottomRight = new ItemStack[16];
            grid4x4_bottomRight[15] = new ItemStack(Material.OAK_LOG);
            
            ItemStack result3 = simulator.tryMatch4x4Fallback(player, grid4x4_bottomRight);
            assertNotNull(result3, "Log w dolnym-prawym rogu");
            assertEquals(Material.OAK_PLANKS, result3.getType());
        }

        @Test
        @DisplayName("Receptura 2x2 w 4x4 fallback -> prawidłowy craft")
        void recipe2x2InGrid4x4_fallback() {
            ItemStack[] grid4x4 = new ItemStack[16];
            // 2x2 desk w pozycji [5,6,9,10]
            grid4x4[5] = new ItemStack(Material.OAK_PLANKS);
            grid4x4[6] = new ItemStack(Material.OAK_PLANKS);
            grid4x4[9] = new ItemStack(Material.OAK_PLANKS);
            grid4x4[10] = new ItemStack(Material.OAK_PLANKS);
            
            ItemStack result = simulator.tryMatch4x4Fallback(player, grid4x4);
            
            assertNotNull(result, "Fallback powinien znaleźć craft table");
            assertEquals(Material.CRAFTING_TABLE, result.getType());
        }

        @Test
        @DisplayName("Receptura 3x3 w 4x4 fallback -> prawidłowy craft")
        void recipe3x3InGrid4x4_fallback() {
            ItemStack[] grid4x4 = new ItemStack[16];
            // Wood to Planks: 1x wood block
            // Umieszczenie w pozycji [0] (górny-lewy)
            grid4x4[0] = new ItemStack(Material.OAK_WOOD);
            
            ItemStack result = simulator.tryMatch4x4Fallback(player, grid4x4);
            
            assertNotNull(result, "Fallback powinien znaleźć planks");
            assertEquals(Material.OAK_PLANKS, result.getType());
        }

        @Test
        @DisplayName("Receptura za duża (4x4 -> nie pasuje do vanilla 3x3)")
        void recipe4x4_tooLarge_returnsNull() {
            ItemStack[] grid4x4 = new ItemStack[16];
            // Cała siatka 4x4 - za duża
            for (int i = 0; i < 16; i++) {
                grid4x4[i] = new ItemStack(Material.STONE);
            }
            
            ItemStack result = simulator.tryMatch4x4Fallback(player, grid4x4);
            
            assertNull(result, "Receptura 4x4 nie powinna pasować do vanilla 3x3");
        }

        @Test
        @DisplayName("Fallback z pustą siatką -> null")
        void fallbackEmptyGrid_returnsNull() {
            ItemStack[] grid4x4 = new ItemStack[16];
            
            ItemStack result = simulator.tryMatch4x4Fallback(player, grid4x4);
            
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Shrink Wrap Integration Tests")
    class ShrinkWrapIntegration {

        @Test
        @DisplayName("Shrink Wrap: Receptura przesunięta się normalnie mapuje")
        void shrinkWrapMapping() {
            // Receptura w pozycji [5,6,9,10] -> po shrink wrap będzie [0,1,2,3] (2x2)
            ItemStack[] grid4x4 = new ItemStack[16];
            grid4x4[5] = new ItemStack(Material.OAK_LOG);
            grid4x4[6] = new ItemStack(Material.OAK_LOG);
            grid4x4[9] = new ItemStack(Material.OAK_LOG);
            grid4x4[10] = new ItemStack(Material.OAK_LOG);
            
            ItemStack[] shrunken = GridShrinkWrapper.shrinkWrap4x4(grid4x4);
            
            assertNotNull(shrunken);
            assertEquals(4, shrunken.length, "Po shrink wrap: 2x2 = 4 itemów");
            for (ItemStack item : shrunken) {
                assertEquals(Material.OAK_LOG, item.getType());
            }
        }

        @Test
        @DisplayName("Shrink Wrap: Wymiary się zgadzają")
        void shrinkWrapDimensions() {
            ItemStack[] grid4x4 = new ItemStack[16];
            // 3x1 receptura w pozycji [2,3,6,7,10,11]
            // To będzie: row 0, cols 2-3
            //           row 1, cols 2-3
            //           row 2, cols 2-3
            grid4x4[2] = new ItemStack(Material.STONE);
            grid4x4[3] = new ItemStack(Material.STONE);
            grid4x4[6] = new ItemStack(Material.STONE);
            grid4x4[7] = new ItemStack(Material.STONE);
            grid4x4[10] = new ItemStack(Material.STONE);
            grid4x4[11] = new ItemStack(Material.STONE);
            
            int[] dims = GridShrinkWrapper.getShrunkDimensions(grid4x4);
            
            assertNotNull(dims);
            assertEquals(3, dims[0], "Wysokość = 3");
            assertEquals(2, dims[1], "Szerokość = 2");
        }
    }

    @Nested
    @DisplayName("Error Handling & Edge Cases")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Null player -> null result")
        void nullPlayer_returnsNull() {
            ItemStack[] grid3x3 = new ItemStack[9];
            grid3x3[0] = new ItemStack(Material.OAK_LOG);
            
            ItemStack result = simulator.tryMatch(null, grid3x3);
            
            assertNull(result);
        }

        @Test
        @DisplayName("Null grid -> null result")
        void nullGrid_returnsNull() {
            ItemStack result = simulator.tryMatch(player, null);
            assertNull(result);
        }

        @Test
        @DisplayName("Błędny rozmiar grid (6 zamiast 9) -> null result")
        void wrongGridSize_returnsNull() {
            ItemStack[] wrongGrid = new ItemStack[6];
            
            ItemStack result = simulator.tryMatch(player, wrongGrid);
            
            assertNull(result);
        }

        @Test
        @DisplayName("Błędny rozmiar grid 4x4 (10 zamiast 16) -> null result")
        void wrongGrid4x4Size_returnsNull() {
            ItemStack[] wrongGrid = new ItemStack[10];
            
            ItemStack result = simulator.tryMatch4x4Fallback(player, wrongGrid);
            
            assertNull(result);
        }

        @Test
        @DisplayName("Siatka z null itemami (zamiast AIR) -> ignoruje i craft'uje")
        void nullItemsInGrid_handled() {
            ItemStack[] grid3x3 = new ItemStack[9];
            grid3x3[0] = new ItemStack(Material.OAK_LOG);
            // Reszta to null
            
            ItemStack result = simulator.tryMatch(player, grid3x3);
            
            assertNotNull(result, "Null items powinny być traktowane jak puste");
            assertEquals(Material.OAK_PLANKS, result.getType());
        }
    }

    @Nested
    @DisplayName("Special Vanilla Recipes")
    class SpecialVanillaRecipes {

        @Test
        @DisplayName("Fajerwerk: 1 papier + 1 proch -> 3 rakiety (power=1)")
        void fireworkRocket1Gunpowder() {
            ItemStack[] grid3x3 = new ItemStack[9];
            grid3x3[0] = new ItemStack(Material.PAPER);
            grid3x3[1] = new ItemStack(Material.GUNPOWDER);
            
            ItemStack result = simulator.tryMatch(player, grid3x3);
            
            assertNotNull(result, "Fajerwerk 1 proch powinien pasować");
            assertEquals(Material.FIREWORK_ROCKET, result.getType());
            assertEquals(3, result.getAmount());
        }

        @Test
        @DisplayName("Fajerwerk: 1 papier + 3 prochy -> 3 rakiety (power=3)")
        void fireworkRocket3Gunpowder() {
            ItemStack[] grid3x3 = new ItemStack[9];
            grid3x3[0] = new ItemStack(Material.PAPER);
            grid3x3[1] = new ItemStack(Material.GUNPOWDER);
            grid3x3[2] = new ItemStack(Material.GUNPOWDER);
            grid3x3[3] = new ItemStack(Material.GUNPOWDER);
            
            ItemStack result = simulator.tryMatch(player, grid3x3);
            
            assertNotNull(result);
            assertEquals(Material.FIREWORK_ROCKET, result.getType());
            // Sprawdzenie mocy
            if (result.getItemMeta() instanceof org.bukkit.inventory.meta.FireworkMeta meta) {
                assertEquals(3, meta.getPower());
            }
        }

        @Test
        @DisplayName("Fajerwerk: 4+ prochy -> nie pasuje")
        void fireworkRocket4Gunpowder_noMatch() {
            ItemStack[] grid3x3 = new ItemStack[9];
            grid3x3[0] = new ItemStack(Material.PAPER);
            grid3x3[1] = new ItemStack(Material.GUNPOWDER);
            grid3x3[2] = new ItemStack(Material.GUNPOWDER);
            grid3x3[3] = new ItemStack(Material.GUNPOWDER);
            grid3x3[4] = new ItemStack(Material.GUNPOWDER);
            
            ItemStack result = simulator.tryMatch(player, grid3x3);
            
            assertNull(result, "4 prochy to za dużo - nie powinna być receptura");
        }

        @Test
        @DisplayName("Naprawa: 2x IRON_PICKAXE o różnym damage -> wynik naprawiony")
        void repairTwoDamagedItems_works() {
            int max = Material.IRON_PICKAXE.getMaxDurability();

            ItemStack a = new ItemStack(Material.IRON_PICKAXE);
            Damageable metaA = (Damageable) a.getItemMeta();
            metaA.setDamage((int) Math.floor(max * 0.50));
            a.setItemMeta(metaA);

            ItemStack b = new ItemStack(Material.IRON_PICKAXE);
            Damageable metaB = (Damageable) b.getItemMeta();
            metaB.setDamage((int) Math.floor(max * 0.60));
            b.setItemMeta(metaB);

            ItemStack[] grid3x3 = new ItemStack[9];
            grid3x3[0] = a;
            grid3x3[1] = b;

            ItemStack result = simulator.tryMatch(player, grid3x3);

            assertNotNull(result, "Naprawa powinna dać wynik");
            assertEquals(Material.IRON_PICKAXE, result.getType());
            assertEquals(1, result.getAmount());

            assertTrue(result.getItemMeta() instanceof Damageable);
            int resultDamage = ((Damageable) result.getItemMeta()).getDamage();

            int life1 = max - metaA.getDamage();
            int life2 = max - metaB.getDamage();
            int bonus = (int) Math.floor(max * 0.05);
            int expectedLife = Math.min(max, life1 + life2 + bonus);
            int expectedDamage = Math.max(0, max - expectedLife);

            assertEquals(expectedDamage, resultDamage);
        }

        @Test
        @DisplayName("Naprawa: różne nazwy (displayName) -> brak naprawy")
        void repairDifferentNames_noMatch() {
            ItemStack a = new ItemStack(Material.IRON_PICKAXE);
            Damageable metaA = (Damageable) a.getItemMeta();
            metaA.setDamage(10);
            metaA.setDisplayName("A");
            a.setItemMeta(metaA);

            ItemStack b = new ItemStack(Material.IRON_PICKAXE);
            Damageable metaB = (Damageable) b.getItemMeta();
            metaB.setDamage(20);
            metaB.setDisplayName("B");
            b.setItemMeta(metaB);

            ItemStack[] grid3x3 = new ItemStack[9];
            grid3x3[0] = a;
            grid3x3[1] = b;

            ItemStack result = simulator.tryMatch(player, grid3x3);

            assertNull(result, "Różne nazwy powinny blokować naprawę");
        }

        @Test
        @DisplayName("Naprawa w 4x4 fallback: 2x IRON_PICKAXE -> wynik naprawiony")
        void repairIn4x4Fallback_works() {
            ItemStack a = new ItemStack(Material.IRON_PICKAXE);
            Damageable metaA = (Damageable) a.getItemMeta();
            metaA.setDamage(50);
            a.setItemMeta(metaA);

            ItemStack b = new ItemStack(Material.IRON_PICKAXE);
            Damageable metaB = (Damageable) b.getItemMeta();
            metaB.setDamage(80);
            b.setItemMeta(metaB);

            ItemStack[] grid4x4 = new ItemStack[16];
            grid4x4[5] = a;
            grid4x4[10] = b;

            ItemStack result = simulator.tryMatch4x4Fallback(player, grid4x4);

            assertNotNull(result);
            assertEquals(Material.IRON_PICKAXE, result.getType());
            assertEquals(1, result.getAmount());
        }
    }
}

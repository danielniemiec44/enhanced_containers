package pl.com.sofitfy.enhanced_containers.sharedcrafting;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.crafting.util.GridFillService;
import pl.com.sofitfy.enhanced_containers.crafting.util.GridFillService.FillResult;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitTestBase;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne dla GridFillService - wspólnej logiki wypełniania siatki craftingu.
 * 
 * Testuje:
 * 1. Poprawne obliczanie maxFills dla pojedynczego składnika
 * 2. Poprawne obliczanie maxFills dla wielu składników tego samego typu
 * 3. Poprawne grupowanie składników i rozdzielanie itemów
 * 4. Zachowanie ilości itemów (brak "zjadania")
 * 5. Edge cases (puste inventory, brak składników, etc.)
 * 
 * Ta sama logika jest używana przez:
 * - /4x4 Fast Crafting (CraftingGUI)
 * - SharedCrafting Recipe Book
 */
@DisplayName("GridFillService - Integration Tests")
@Tag("integration")
public class GridFillServiceIntegrationTest extends MockBukkitTestBase {

    // ========================================
    // TESTY OBLICZANIA maxFills
    // ========================================
    @Nested
    @DisplayName("Obliczanie maxFills dla ShapedRecipe")
    class MaxFillsCalculationTests {

        @Test
        @DisplayName("Stone Stairs: 6 slotów, 256 stone -> maxFills = 42 (256/6)")
        void stoneStairs_sixSlots_calculatesCorrectly() {
            // Given: Gracz ma 4 stacki stone (256)
            PlayerMock player = createPlayer();
            player.getInventory().addItem(
                new ItemStack(Material.STONE, 64),
                new ItemStack(Material.STONE, 64),
                new ItemStack(Material.STONE, 64),
                new ItemStack(Material.STONE, 64)
            );
            
            // Stone Stairs: 6 slotów stone
            // [S][ ][ ]
            // [S][S][ ]
            // [S][S][S]
            ShapedRecipe stairsRecipe = createStoneStairsRecipe();
            
            // When
            FillResult result = GridFillService.planForShapedRecipe(
                player.getInventory().getStorageContents(),
                stairsRecipe,
                true  // useMaxItems
            );
            
            // Then
            assertTrue(result.success(), "Powinno się udać wypełnić");
            
            // maxFills = 256 / 6 = 42
            int expectedFills = 256 / 6;
            for (ItemStack item : result.gridItems().values()) {
                assertEquals(expectedFills, item.getAmount(),
                    "Każdy slot powinien mieć " + expectedFills + " itemów");
            }
            
            // Łączna ilość do usunięcia = 42 * 6 = 252
            int totalToRemove = result.consumeOps().values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(expectedFills * 6, totalToRemove,
                "Łączna ilość do usunięcia powinna być " + (expectedFills * 6));
        }

        @Test
        @DisplayName("Stone Brick: 4 sloty, 256 stone -> maxFills = 64 (256/4, ale max 64)")
        void stoneBrick_fourSlots_maxStackSize() {
            // Given: Gracz ma 4 stacki stone (256)
            PlayerMock player = createPlayer();
            player.getInventory().addItem(
                new ItemStack(Material.STONE, 64),
                new ItemStack(Material.STONE, 64),
                new ItemStack(Material.STONE, 64),
                new ItemStack(Material.STONE, 64)
            );
            
            // Stone Brick: 4 sloty stone w kwadrat 2x2
            ShapedRecipe brickRecipe = createStoneBrickRecipe();
            
            // When
            FillResult result = GridFillService.planForShapedRecipe(
                player.getInventory().getStorageContents(),
                brickRecipe,
                true
            );
            
            // Then
            assertTrue(result.success());
            
            // 256 / 4 = 64, ale maxStackSize = 64, więc maxFills = 64
            for (ItemStack item : result.gridItems().values()) {
                assertEquals(64, item.getAmount(),
                    "Każdy slot powinien mieć 64 itemów (max stack size)");
            }
            
            // Łączna ilość do usunięcia = 64 * 4 = 256
            int totalToRemove = result.consumeOps().values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(256, totalToRemove);
        }

        @Test
        @DisplayName("useMaxItems=false -> zawsze 1 item na slot")
        void useMaxItemsFalse_alwaysOneItem() {
            // Given
            PlayerMock player = createPlayer();
            player.getInventory().addItem(new ItemStack(Material.STONE, 64));
            
            ShapedRecipe brickRecipe = createStoneBrickRecipe();
            
            // When
            FillResult result = GridFillService.planForShapedRecipe(
                player.getInventory().getStorageContents(),
                brickRecipe,
                false  // useMaxItems = false
            );
            
            // Then
            assertTrue(result.success());
            for (ItemStack item : result.gridItems().values()) {
                assertEquals(1, item.getAmount(),
                    "Bez shift-click każdy slot powinien mieć 1 item");
            }
        }
    }

    // ========================================
    // TESTY GRUPOWANIA SKŁADNIKÓW
    // ========================================
    @Nested
    @DisplayName("Grupowanie składników tego samego typu")
    class IngredientGroupingTests {

        @Test
        @DisplayName("Przepis z wieloma identycznymi składnikami - poprawne grupowanie")
        void multipleIdenticalIngredients_groupedCorrectly() {
            // Given: 128 stone
            PlayerMock player = createPlayer();
            player.getInventory().addItem(
                new ItemStack(Material.STONE, 64),
                new ItemStack(Material.STONE, 64)
            );
            
            // Stone Brick: 4 sloty tego samego składnika
            ShapedRecipe recipe = createStoneBrickRecipe();
            
            // When
            FillResult result = GridFillService.planForShapedRecipe(
                player.getInventory().getStorageContents(),
                recipe,
                true
            );
            
            // Then: 128 / 4 = 32 per slot
            assertTrue(result.success());
            assertEquals(4, result.gridItems().size(), "Powinny być 4 sloty");
            
            for (ItemStack item : result.gridItems().values()) {
                assertEquals(32, item.getAmount());
            }
        }

        @Test
        @DisplayName("Przepis z różnymi składnikami - każdy grupowany osobno")
        void differentIngredients_separateGroups() {
            // Given: Miecz diamentowy wymaga diamentów i patyków
            PlayerMock player = createPlayer();
            player.getInventory().addItem(
                new ItemStack(Material.DIAMOND, 10),  // 2 potrzebne
                new ItemStack(Material.STICK, 5)      // 1 potrzebny
            );
            
            // Diamond Sword: 2 diamenty + 1 patyk
            ShapedRecipe swordRecipe = createDiamondSwordRecipe();
            
            // When
            FillResult result = GridFillService.planForShapedRecipe(
                player.getInventory().getStorageContents(),
                swordRecipe,
                true
            );
            
            // Then
            assertTrue(result.success());
            
            // maxFills = min(10/2, 5/1) = min(5, 5) = 5
            int expectedFills = 5;
            
            int diamondSlots = 0;
            int stickSlots = 0;
            for (ItemStack item : result.gridItems().values()) {
                if (item.getType() == Material.DIAMOND) {
                    diamondSlots++;
                    assertEquals(expectedFills, item.getAmount());
                } else if (item.getType() == Material.STICK) {
                    stickSlots++;
                    assertEquals(expectedFills, item.getAmount());
                }
            }
            
            assertEquals(2, diamondSlots, "Powinny być 2 sloty diamentów");
            assertEquals(1, stickSlots, "Powinien być 1 slot patyka");
        }
    }

    // ========================================
    // TESTY POBIERANIA Z WIELU SLOTÓW
    // ========================================
    @Nested
    @DisplayName("Pobieranie itemów z wielu slotów ekwipunku")
    class MultiSlotConsumptionTests {

        @Test
        @DisplayName("Itemy pobierane z wielu slotów gdy jeden nie wystarcza")
        void itemsFromMultipleSlots_whenOneNotEnough() {
            // Given: 3 stacki po 30 stone (90 total)
            PlayerMock player = createPlayer();
            player.getInventory().setItem(0, new ItemStack(Material.STONE, 30));
            player.getInventory().setItem(1, new ItemStack(Material.STONE, 30));
            player.getInventory().setItem(2, new ItemStack(Material.STONE, 30));
            
            // Stone Brick: 4 sloty, 90/4 = 22 maxFills
            ShapedRecipe recipe = createStoneBrickRecipe();
            
            // When
            FillResult result = GridFillService.planForShapedRecipe(
                player.getInventory().getStorageContents(),
                recipe,
                true
            );
            
            // Then
            assertTrue(result.success());
            
            // 90 / 4 = 22 per slot
            for (ItemStack item : result.gridItems().values()) {
                assertEquals(22, item.getAmount());
            }
            
            // Łącznie pobieramy 22 * 4 = 88 stone z 3 slotów
            int totalConsumed = result.consumeOps().values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(88, totalConsumed);
            
            // Powinno być więcej niż 1 slot źródłowy
            assertTrue(result.consumeOps().size() >= 2, 
                "Powinno pobierać z co najmniej 2 slotów");
        }

        @Test
        @DisplayName("Wszystkie itemy z jednego slotu gdy wystarczą")
        void allItemsFromOneSlot_whenEnough() {
            // Given: 1 stack 64 stone
            PlayerMock player = createPlayer();
            player.getInventory().setItem(0, new ItemStack(Material.STONE, 64));
            
            // Stone Brick: 4 sloty
            ShapedRecipe recipe = createStoneBrickRecipe();
            
            // When
            FillResult result = GridFillService.planForShapedRecipe(
                player.getInventory().getStorageContents(),
                recipe,
                true
            );
            
            // Then: 64/4 = 16 per slot
            assertTrue(result.success());
            for (ItemStack item : result.gridItems().values()) {
                assertEquals(16, item.getAmount());
            }
            
            // Wszystko z jednego slotu
            assertEquals(1, result.consumeOps().size());
            assertEquals(64, result.consumeOps().get(0));
        }
    }

    // ========================================
    // TESTY ZACHOWANIA ILOŚCI (BEZ "ZJADANIA")
    // ========================================
    @Nested
    @DisplayName("Zachowanie ilości itemów - brak 'zjadania'")
    class ItemConservationTests {

        @Test
        @DisplayName("Suma itemów przed i po wypełnieniu jest taka sama")
        void totalItemCount_conservedAfterFill() {
            // Given
            PlayerMock player = createPlayer();
            player.getInventory().addItem(
                new ItemStack(Material.STONE, 64),
                new ItemStack(Material.STONE, 64),
                new ItemStack(Material.STONE, 64),
                new ItemStack(Material.STONE, 64)
            );
            
            int totalBefore = countMaterial(player.getInventory().getStorageContents(), Material.STONE);
            assertEquals(256, totalBefore);
            
            Inventory workbench = server.createInventory(null, InventoryType.WORKBENCH);
            ShapedRecipe recipe = createStoneBrickRecipe();
            
            // When
            FillResult result = GridFillService.planForShapedRecipe(
                player.getInventory().getStorageContents(),
                recipe,
                true
            );
            
            assertTrue(result.success());
            GridFillService.applyFillResult(player, workbench, result);
            
            // Then
            int inInventory = countMaterial(player.getInventory().getStorageContents(), Material.STONE);
            int inGrid = countMaterialInWorkbench(workbench, Material.STONE);
            int totalAfter = inInventory + inGrid;
            
            assertEquals(totalBefore, totalAfter,
                "Suma itemów powinna być zachowana: było " + totalBefore + 
                ", po wypełnieniu: inv=" + inInventory + " + grid=" + inGrid + " = " + totalAfter);
        }

        @Test
        @DisplayName("Wielokrotne wypełnianie i czyszczenie - ilość zawsze zachowana")
        void repeatedFillAndClear_itemsConserved() {
            // Given
            PlayerMock player = createPlayer();
            player.getInventory().addItem(new ItemStack(Material.STONE, 100));
            Inventory workbench = server.createInventory(null, InventoryType.WORKBENCH);
            
            int totalBefore = 100;
            
            ShapedRecipe recipe = createStoneBrickRecipe();
            
            // Wykonaj 5 cykli wypełniania i zwracania
            for (int cycle = 0; cycle < 5; cycle++) {
                // Fill
                FillResult result = GridFillService.planForShapedRecipe(
                    player.getInventory().getStorageContents(),
                    recipe,
                    true
                );
                
                if (result.success()) {
                    GridFillService.applyFillResult(player, workbench, result);
                    
                    // Zwróć do gracza
                    returnGridItemsToPlayer(player, workbench);
                }
                
                // Verify
                int total = countMaterial(player.getInventory().getStorageContents(), Material.STONE);
                assertEquals(totalBefore, total, 
                    "Po cyklu " + (cycle + 1) + " ilość powinna być zachowana");
            }
        }
    }

    // ========================================
    // TESTY SHAPELESS RECIPE
    // ========================================
    @Nested
    @DisplayName("ShapelessRecipe")
    class ShapelessRecipeTests {

        @Test
        @DisplayName("Shapeless: składniki układane od slotu 1")
        void shapelessRecipe_ingredientsFromSlot1() {
            // Given
            PlayerMock player = createPlayer();
            player.getInventory().addItem(
                new ItemStack(Material.BONE, 10)
            );
            
            // Bone Meal: 1 bone -> 3 bone meal (shapeless)
            ShapelessRecipe recipe = createBoneMealRecipe();
            
            // When
            FillResult result = GridFillService.planForShapelessRecipe(
                player.getInventory().getStorageContents(),
                recipe,
                true
            );
            
            // Then
            assertTrue(result.success());
            assertEquals(1, result.gridItems().size());
            assertTrue(result.gridItems().containsKey(1), "Składnik powinien być w slocie 1");
            assertEquals(10, result.gridItems().get(1).getAmount());
        }

        @Test
        @DisplayName("Shapeless z wieloma składnikami")
        void shapelessRecipe_multipleIngredients() {
            // Given
            PlayerMock player = createPlayer();
            player.getInventory().addItem(
                new ItemStack(Material.RED_DYE, 20),
                new ItemStack(Material.YELLOW_DYE, 30)
            );
            
            // Orange Dye: red + yellow
            ShapelessRecipe recipe = createOrangeDyeRecipe();
            
            // When
            FillResult result = GridFillService.planForShapelessRecipe(
                player.getInventory().getStorageContents(),
                recipe,
                true
            );
            
            // Then
            assertTrue(result.success());
            assertEquals(2, result.gridItems().size());
            
            // maxFills = min(20/1, 30/1) = 20
            for (ItemStack item : result.gridItems().values()) {
                assertEquals(20, item.getAmount());
            }
        }
    }

    // ========================================
    // TESTY EDGE CASES
    // ========================================
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Puste inventory -> failure")
        void emptyInventory_returnsFail() {
            // Given
            PlayerMock player = createPlayer();
            ShapedRecipe recipe = createStoneBrickRecipe();
            
            // When
            FillResult result = GridFillService.planForShapedRecipe(
                player.getInventory().getStorageContents(),
                recipe,
                true
            );
            
            // Then
            assertFalse(result.success());
        }

        @Test
        @DisplayName("Brak jednego składnika -> failure")
        void missingOneIngredient_returnsFail() {
            // Given: Miecz wymaga diamentów i patyków, mamy tylko diamenty
            PlayerMock player = createPlayer();
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, 64));
            
            ShapedRecipe swordRecipe = createDiamondSwordRecipe();
            
            // When
            FillResult result = GridFillService.planForShapedRecipe(
                player.getInventory().getStorageContents(),
                swordRecipe,
                true
            );
            
            // Then
            assertFalse(result.success(), "Powinno zwrócić failure - brak patyków");
        }

        @Test
        @DisplayName("Za mało itemów na jeden pełny cykl -> failure")
        void notEnoughForOneCycle_returnsFail() {
            // Given: Stone Brick wymaga 4 stone, mamy 3
            PlayerMock player = createPlayer();
            player.getInventory().addItem(new ItemStack(Material.STONE, 3));
            
            ShapedRecipe recipe = createStoneBrickRecipe();
            
            // When
            FillResult result = GridFillService.planForShapedRecipe(
                player.getInventory().getStorageContents(),
                recipe,
                true
            );
            
            // Then
            assertFalse(result.success(), "3 stone nie wystarczy na 4 sloty");
        }

        @Test
        @DisplayName("Dokładnie wystarczająca ilość -> success")
        void exactlyEnough_returnsSuccess() {
            // Given: Stone Brick wymaga 4 stone, mamy 4
            PlayerMock player = createPlayer();
            player.getInventory().addItem(new ItemStack(Material.STONE, 4));
            
            ShapedRecipe recipe = createStoneBrickRecipe();
            
            // When
            FillResult result = GridFillService.planForShapedRecipe(
                player.getInventory().getStorageContents(),
                recipe,
                true
            );
            
            // Then
            assertTrue(result.success());
            for (ItemStack item : result.gridItems().values()) {
                assertEquals(1, item.getAmount());
            }
        }

        @Test
        @DisplayName("Niestackowalne itemy (narzędzia) -> maxFills = 1")
        void unstackableItems_maxFillsOne() {
            // Given: Gracz ma kilka kilofów
            PlayerMock player = createPlayer();
            player.getInventory().addItem(
                new ItemStack(Material.DIAMOND_PICKAXE, 1),
                new ItemStack(Material.DIAMOND_PICKAXE, 1),
                new ItemStack(Material.DIAMOND_PICKAXE, 1)
            );
            
            // Uwaga: W prawdziwej grze nie ma przepisu na kilof z kilofów,
            // ale testujemy mechanikę maxStackSize = 1
            // Użyjemy prostego shapeless recipe dla testu
            ShapelessRecipe recipe = new ShapelessRecipe(
                new NamespacedKey("test", "pickaxe_test"),
                new ItemStack(Material.DIAMOND)
            );
            recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.DIAMOND_PICKAXE));
            
            // When
            FillResult result = GridFillService.planForShapelessRecipe(
                player.getInventory().getStorageContents(),
                recipe,
                true
            );
            
            // Then
            assertTrue(result.success());
            // maxStackSize = 1, więc maxFills = 1
            assertEquals(1, result.gridItems().get(1).getAmount());
        }
    }

    // ========================================
    // HELPER METHODS
    // ========================================
    
    private ShapedRecipe createStoneBrickRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(
            new NamespacedKey("test", "stone_brick"),
            new ItemStack(Material.STONE_BRICKS, 4)
        );
        recipe.shape("SS", "SS");
        recipe.setIngredient('S', Material.STONE);
        return recipe;
    }
    
    private ShapedRecipe createStoneStairsRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(
            new NamespacedKey("test", "stone_stairs"),
            new ItemStack(Material.STONE_STAIRS, 4)
        );
        recipe.shape("S  ", "SS ", "SSS");
        recipe.setIngredient('S', Material.STONE);
        return recipe;
    }
    
    private ShapedRecipe createDiamondSwordRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(
            new NamespacedKey("test", "diamond_sword"),
            new ItemStack(Material.DIAMOND_SWORD, 1)
        );
        recipe.shape("D", "D", "S");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('S', Material.STICK);
        return recipe;
    }
    
    private ShapelessRecipe createBoneMealRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(
            new NamespacedKey("test", "bone_meal"),
            new ItemStack(Material.BONE_MEAL, 3)
        );
        recipe.addIngredient(Material.BONE);
        return recipe;
    }
    
    private ShapelessRecipe createOrangeDyeRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(
            new NamespacedKey("test", "orange_dye"),
            new ItemStack(Material.ORANGE_DYE, 2)
        );
        recipe.addIngredient(Material.RED_DYE);
        recipe.addIngredient(Material.YELLOW_DYE);
        return recipe;
    }
    
    private int countMaterial(ItemStack[] contents, Material material) {
        return Arrays.stream(contents)
            .filter(item -> item != null && item.getType() == material)
            .mapToInt(ItemStack::getAmount)
            .sum();
    }
    
    private int countMaterialInWorkbench(Inventory workbench, Material material) {
        int count = 0;
        for (int slot = 1; slot <= 9; slot++) {
            ItemStack item = workbench.getItem(slot);
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }
    
    /**
     * Zwraca przedmioty z siatki do gracza (vanilla behavior).
     */
    private void returnGridItemsToPlayer(PlayerMock player, Inventory workbench) {
        for (int slot = 1; slot <= 9; slot++) {
            ItemStack item = workbench.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item.clone());
                workbench.setItem(slot, null);
            }
        }
    }
}

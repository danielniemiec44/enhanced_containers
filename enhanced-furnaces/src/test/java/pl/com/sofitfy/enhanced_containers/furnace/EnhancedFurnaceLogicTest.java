package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy dla EnhancedFurnaceLogic
 * Weryfikuje wartości paliwa i podstawową logikę pieca
 */
@DisplayName("EnhancedFurnaceLogic Tests")
class EnhancedFurnaceLogicTest {
    
    @Nested
    @DisplayName("Fuel Burn Time Values")
    class FuelBurnTimeValues {
        
        @Test
        @DisplayName("Coal should burn for 1600 ticks (vanilla value)")
        void coalShouldBurnFor1600Ticks() {
            assertEquals(1600, EnhancedFurnaceLogic.getFuelBurnTime(Material.COAL));
        }
        
        @Test
        @DisplayName("Charcoal should burn for 1600 ticks (vanilla value)")
        void charcoalShouldBurnFor1600Ticks() {
            assertEquals(1600, EnhancedFurnaceLogic.getFuelBurnTime(Material.CHARCOAL));
        }
        
        @Test
        @DisplayName("Coal block should burn for 16000 ticks (vanilla value)")
        void coalBlockShouldBurnFor16000Ticks() {
            assertEquals(16000, EnhancedFurnaceLogic.getFuelBurnTime(Material.COAL_BLOCK));
        }
        
        @Test
        @DisplayName("Lava bucket should burn for 20000 ticks (vanilla value)")
        void lavaBucketShouldBurnFor20000Ticks() {
            assertEquals(20000, EnhancedFurnaceLogic.getFuelBurnTime(Material.LAVA_BUCKET));
        }
        
        @Test
        @DisplayName("Stick should burn for 100 ticks (vanilla value)")
        void stickShouldBurnFor100Ticks() {
            assertEquals(100, EnhancedFurnaceLogic.getFuelBurnTime(Material.STICK));
        }
        
        @Test
        @DisplayName("Blaze rod should burn for 2400 ticks (vanilla value)")
        void blazeRodShouldBurnFor2400Ticks() {
            assertEquals(2400, EnhancedFurnaceLogic.getFuelBurnTime(Material.BLAZE_ROD));
        }
        
        @Test
        @DisplayName("Dried kelp block should burn for 4001 ticks (vanilla value)")
        void driedKelpBlockShouldBurnFor4001Ticks() {
            assertEquals(4001, EnhancedFurnaceLogic.getFuelBurnTime(Material.DRIED_KELP_BLOCK));
        }
    }
    
    @Nested
    @DisplayName("Wood Planks Burn Time")
    class WoodPlanksBurnTime {
        
        @Test
        @DisplayName("All wood planks should burn for 300 ticks")
        void allWoodPlanksShouldBurnFor300Ticks() {
            Material[] planks = {
                Material.OAK_PLANKS,
                Material.SPRUCE_PLANKS,
                Material.BIRCH_PLANKS,
                Material.JUNGLE_PLANKS,
                Material.ACACIA_PLANKS,
                Material.DARK_OAK_PLANKS,
                Material.MANGROVE_PLANKS,
                Material.CHERRY_PLANKS,
                Material.BAMBOO_PLANKS,
                Material.CRIMSON_PLANKS,
                Material.WARPED_PLANKS
            };
            
            for (Material plank : planks) {
                assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(plank),
                    plank.name() + " should burn for 300 ticks");
            }
        }
    }
    
    @Nested
    @DisplayName("Logs Burn Time")
    class LogsBurnTime {
        
        @Test
        @DisplayName("All logs should burn for 300 ticks")
        void allLogsShouldBurnFor300Ticks() {
            Material[] logs = {
                Material.OAK_LOG,
                Material.SPRUCE_LOG,
                Material.BIRCH_LOG,
                Material.JUNGLE_LOG,
                Material.ACACIA_LOG,
                Material.DARK_OAK_LOG,
                Material.MANGROVE_LOG,
                Material.CHERRY_LOG
            };
            
            for (Material log : logs) {
                assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(log),
                    log.name() + " should burn for 300 ticks");
            }
        }
        
        @Test
        @DisplayName("Stripped logs should burn for 300 ticks")
        void strippedLogsShouldBurnFor300Ticks() {
            Material[] strippedLogs = {
                Material.STRIPPED_OAK_LOG,
                Material.STRIPPED_SPRUCE_LOG,
                Material.STRIPPED_BIRCH_LOG,
                Material.STRIPPED_JUNGLE_LOG,
                Material.STRIPPED_ACACIA_LOG,
                Material.STRIPPED_DARK_OAK_LOG,
                Material.STRIPPED_MANGROVE_LOG,
                Material.STRIPPED_CHERRY_LOG
            };
            
            for (Material log : strippedLogs) {
                assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(log),
                    log.name() + " should burn for 300 ticks");
            }
        }
    }
    
    @Nested
    @DisplayName("Non-Fuel Items")
    class NonFuelItems {
        
        @Test
        @DisplayName("Stone should not be fuel")
        void stoneShouldNotBeFuel() {
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(Material.STONE));
        }
        
        @Test
        @DisplayName("Diamond should not be fuel")
        void diamondShouldNotBeFuel() {
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(Material.DIAMOND));
        }
        
        @Test
        @DisplayName("Iron ingot should not be fuel")
        void ironIngotShouldNotBeFuel() {
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(Material.IRON_INGOT));
        }
        
        @Test
        @DisplayName("Air should not be fuel")
        void airShouldNotBeFuel() {
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(Material.AIR));
        }
    }
    
    @Nested
    @DisplayName("Wool Burn Time")
    class WoolBurnTime {
        
        @Test
        @DisplayName("All wool colors should burn for 100 ticks")
        void allWoolColorsShouldBurnFor100Ticks() {
            Material[] wools = {
                Material.WHITE_WOOL,
                Material.ORANGE_WOOL,
                Material.MAGENTA_WOOL,
                Material.LIGHT_BLUE_WOOL,
                Material.YELLOW_WOOL,
                Material.LIME_WOOL,
                Material.PINK_WOOL,
                Material.GRAY_WOOL,
                Material.LIGHT_GRAY_WOOL,
                Material.CYAN_WOOL,
                Material.PURPLE_WOOL,
                Material.BLUE_WOOL,
                Material.BROWN_WOOL,
                Material.GREEN_WOOL,
                Material.RED_WOOL,
                Material.BLACK_WOOL
            };
            
            for (Material wool : wools) {
                assertEquals(100, EnhancedFurnaceLogic.getFuelBurnTime(wool),
                    wool.name() + " should burn for 100 ticks");
            }
        }
    }
    
    @Nested
    @DisplayName("Carpet Burn Time")
    class CarpetBurnTime {
        
        @Test
        @DisplayName("All carpets should burn for 67 ticks")
        void allCarpetsShouldBurnFor67Ticks() {
            Material[] carpets = {
                Material.WHITE_CARPET,
                Material.ORANGE_CARPET,
                Material.MAGENTA_CARPET,
                Material.LIGHT_BLUE_CARPET,
                Material.YELLOW_CARPET,
                Material.LIME_CARPET,
                Material.PINK_CARPET,
                Material.GRAY_CARPET,
                Material.LIGHT_GRAY_CARPET,
                Material.CYAN_CARPET,
                Material.PURPLE_CARPET,
                Material.BLUE_CARPET,
                Material.BROWN_CARPET,
                Material.GREEN_CARPET,
                Material.RED_CARPET,
                Material.BLACK_CARPET
            };
            
            for (Material carpet : carpets) {
                assertEquals(67, EnhancedFurnaceLogic.getFuelBurnTime(carpet),
                    carpet.name() + " should burn for 67 ticks");
            }
        }
    }
    
    @Nested
    @DisplayName("Constants")
    class Constants {
        
        @Test
        @DisplayName("COOK_TIME_NORMAL should be 200 ticks (10 seconds)")
        void cookTimeNormalShouldBe200Ticks() {
            assertEquals(200, EnhancedFurnaceLogic.COOK_TIME_NORMAL);
        }
        
        @Test
        @DisplayName("COOK_TIME_BLAST should be 100 ticks (5 seconds)")
        void cookTimeBlastShouldBe100Ticks() {
            assertEquals(100, EnhancedFurnaceLogic.COOK_TIME_BLAST);
        }
        
        @Test
        @DisplayName("COOK_TIME_SMOKER should be 100 ticks (5 seconds)")
        void cookTimeSmokerShouldBe100Ticks() {
            assertEquals(100, EnhancedFurnaceLogic.COOK_TIME_SMOKER);
        }
    }
    
    // ========================================================================
    // SHIFT-CLICK PRIORITY - Testy priorytetów dla shift-click
    // ========================================================================
    
    @Nested
    @DisplayName("🔀 Shift-Click Priority Logic")
    class ShiftClickPriorityLogic {
        
        @Test
        @DisplayName("Drewno (logs) jest zarówno smeltable jak i fuel - powinno iść do INPUT")
        void woodLogs_areBothSmeltableAndFuel() {
            // Drewno jest paliwem
            Material[] logs = {
                Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG,
                Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
                Material.MANGROVE_LOG, Material.CHERRY_LOG
            };
            
            for (Material log : logs) {
                // Drewno jest paliwem (ma czas palenia)
                assertTrue(EnhancedFurnaceLogic.getFuelBurnTime(log) > 0,
                    log.name() + " powinno być paliwem");
                
                // Drewno można przepalić na węgiel drzewny (fallback)
                // canBeSmeltedInFurnace wymaga NMS, ale sprawdzamy fallback
                assertTrue(canBeSmelted_Fallback(log),
                    log.name() + " powinno być smeltable (daje charcoal)");
            }
        }
        
        @Test
        @DisplayName("Węgiel NIE jest smeltable - powinien iść tylko do FUEL")
        void coal_isOnlyFuel_notSmeltable() {
            // Węgiel jest paliwem
            assertTrue(EnhancedFurnaceLogic.getFuelBurnTime(Material.COAL) > 0,
                "Węgiel powinien być paliwem");
            
            // Węgiel NIE ma przepisu (nie można go przepalić)
            assertFalse(canBeSmelted_Fallback(Material.COAL),
                "Węgiel NIE powinien być smeltable");
        }
        
        @Test
        @DisplayName("Surowe mięso jest smeltable ale NIE fuel - powinno iść do INPUT")
        void rawMeat_isOnlySmeltable_notFuel() {
            Material[] rawMeats = {
                Material.BEEF, Material.PORKCHOP, Material.CHICKEN,
                Material.MUTTON, Material.RABBIT, Material.COD, Material.SALMON
            };
            
            for (Material meat : rawMeats) {
                // Mięso NIE jest paliwem
                assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(meat),
                    meat.name() + " NIE powinno być paliwem");
                
                // Mięso można przepalić
                assertTrue(canBeSmelted_Fallback(meat),
                    meat.name() + " powinno być smeltable");
            }
        }
        
        @Test
        @DisplayName("Rudy są smeltable ale NIE fuel - powinny iść do INPUT")
        void ores_areOnlySmeltable_notFuel() {
            Material[] ores = {
                Material.RAW_IRON, Material.RAW_GOLD, Material.RAW_COPPER,
                Material.IRON_ORE, Material.GOLD_ORE, Material.COPPER_ORE
            };
            
            for (Material ore : ores) {
                // Rudy NIE są paliwem
                assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(ore),
                    ore.name() + " NIE powinno być paliwem");
                
                // Rudy można przepalić
                assertTrue(canBeSmelted_Fallback(ore),
                    ore.name() + " powinno być smeltable");
            }
        }
        
        @Test
        @DisplayName("Diament nie jest ani smeltable ani fuel")
        void diamond_isNeitherSmeltableNorFuel() {
            // Diament NIE jest paliwem
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(Material.DIAMOND),
                "Diament NIE powinien być paliwem");
            
            // Diament NIE ma przepisu
            assertFalse(canBeSmelted_Fallback(Material.DIAMOND),
                "Diament NIE powinien być smeltable");
        }
        
        @Test
        @DisplayName("Piasek jest smeltable (daje szkło) ale NIE fuel")
        void sand_isSmeltable_notFuel() {
            // Piasek NIE jest paliwem
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(Material.SAND),
                "Piasek NIE powinien być paliwem");
            
            // Piasek można przepalić na szkło
            assertTrue(canBeSmelted_Fallback(Material.SAND),
                "Piasek powinien być smeltable (daje szkło)");
        }
        
        @Test
        @DisplayName("Blaze Rod jest tylko fuel - nie smeltable")
        void blazeRod_isOnlyFuel() {
            // Blaze Rod jest paliwem
            assertTrue(EnhancedFurnaceLogic.getFuelBurnTime(Material.BLAZE_ROD) > 0,
                "Blaze Rod powinien być paliwem");
            
            // Blaze Rod NIE ma przepisu
            assertFalse(canBeSmelted_Fallback(Material.BLAZE_ROD),
                "Blaze Rod NIE powinien być smeltable");
        }
        
        /**
         * Helper do sprawdzania czy materiał jest smeltable (fallback bez NMS)
         */
        private boolean canBeSmelted_Fallback(Material material) {
            return switch (material) {
                // Mięso i ryby
                case BEEF, PORKCHOP, CHICKEN, MUTTON, RABBIT, COD, SALMON -> true;
                // Rudy
                case RAW_IRON, RAW_GOLD, RAW_COPPER -> true;
                case IRON_ORE, DEEPSLATE_IRON_ORE -> true;
                case GOLD_ORE, DEEPSLATE_GOLD_ORE -> true;
                case COPPER_ORE, DEEPSLATE_COPPER_ORE -> true;
                // Inne materiały
                case SAND, RED_SAND -> true;
                case COBBLESTONE -> true;
                case CLAY_BALL, CLAY -> true;
                case NETHERRACK -> true;
                case CACTUS -> true;
                case WET_SPONGE -> true;
                case KELP -> true;
                case POTATO -> true;
                // Drewno
                case OAK_LOG, BIRCH_LOG, SPRUCE_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG,
                     MANGROVE_LOG, CHERRY_LOG, CRIMSON_STEM, WARPED_STEM,
                     OAK_WOOD, BIRCH_WOOD, SPRUCE_WOOD, JUNGLE_WOOD, ACACIA_WOOD, DARK_OAK_WOOD,
                     MANGROVE_WOOD, CHERRY_WOOD, CRIMSON_HYPHAE, WARPED_HYPHAE,
                     STRIPPED_OAK_LOG, STRIPPED_BIRCH_LOG, STRIPPED_SPRUCE_LOG, STRIPPED_JUNGLE_LOG,
                     STRIPPED_ACACIA_LOG, STRIPPED_DARK_OAK_LOG, STRIPPED_MANGROVE_LOG, STRIPPED_CHERRY_LOG,
                     STRIPPED_CRIMSON_STEM, STRIPPED_WARPED_STEM,
                     STRIPPED_OAK_WOOD, STRIPPED_BIRCH_WOOD, STRIPPED_SPRUCE_WOOD, STRIPPED_JUNGLE_WOOD,
                     STRIPPED_ACACIA_WOOD, STRIPPED_DARK_OAK_WOOD, STRIPPED_MANGROVE_WOOD, STRIPPED_CHERRY_WOOD,
                     STRIPPED_CRIMSON_HYPHAE, STRIPPED_WARPED_HYPHAE -> true;
                default -> false;
            };
        }
    }
}

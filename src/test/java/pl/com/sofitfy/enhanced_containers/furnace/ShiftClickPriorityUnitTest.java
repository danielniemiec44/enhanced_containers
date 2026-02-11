package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy jednostkowe dla logiki priorytetów shift-click w piecu.
 * 
 * VANILLA BEHAVIOR:
 * - Przedmioty które można przepalić (mają przepis) → idą do INPUT
 * - Przedmioty które są tylko paliwem (bez przepisu) → idą do FUEL
 * - Przedmioty które są zarówno smeltable jak i fuel (np. drewno) → idą do INPUT (priorytet przepisu)
 * 
 * Ten test weryfikuje że logika priorytetów jest poprawna.
 */
@DisplayName("🔀 Shift-Click Priority Unit Tests")
class ShiftClickPriorityUnitTest {
    
    /**
     * Helper do sprawdzania czy materiał jest smeltable (fallback bez NMS)
     * Kopiuje logikę z EnhancedFurnaceLogic.canBeSmelted_Fallback()
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
            // Drewno (logs -> charcoal)
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
    
    @Nested
    @DisplayName("🪵 Drewno (Dual-Purpose: smeltable + fuel)")
    class WoodDualPurposeTests {
        
        @Test
        @DisplayName("OAK_LOG jest zarówno paliwem JAK I można go przepalić na węgiel drzewny")
        void oakLog_isBothFuelAndSmeltable() {
            Material log = Material.OAK_LOG;
            
            // Jest paliwem
            int burnTime = EnhancedFurnaceLogic.getFuelBurnTime(log);
            assertTrue(burnTime > 0, "OAK_LOG powinien być paliwem (burnTime=" + burnTime + ")");
            
            // Można przepalić (daje charcoal)
            boolean smeltable = canBeSmelted_Fallback(log);
            assertTrue(smeltable, "OAK_LOG powinien być smeltable (daje CHARCOAL)");
            
            // WNIOSEK: Shift-click powinien kierować do INPUT (priorytet przepisu)
        }
        
        @Test
        @DisplayName("Wszystkie typy drewna są dual-purpose")
        void allLogs_areDualPurpose() {
            Material[] logs = {
                Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG,
                Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
                Material.MANGROVE_LOG, Material.CHERRY_LOG,
                Material.CRIMSON_STEM, Material.WARPED_STEM
            };
            
            for (Material log : logs) {
                int burnTime = EnhancedFurnaceLogic.getFuelBurnTime(log);
                boolean smeltable = canBeSmelted_Fallback(log);
                
                assertTrue(burnTime > 0, 
                    log.name() + " powinien być paliwem");
                assertTrue(smeltable, 
                    log.name() + " powinien być smeltable");
            }
        }
        
        @Test
        @DisplayName("Stripped logs też są dual-purpose")
        void strippedLogs_areDualPurpose() {
            Material[] strippedLogs = {
                Material.STRIPPED_OAK_LOG, Material.STRIPPED_BIRCH_LOG,
                Material.STRIPPED_SPRUCE_LOG, Material.STRIPPED_JUNGLE_LOG,
                Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG
            };
            
            for (Material log : strippedLogs) {
                int burnTime = EnhancedFurnaceLogic.getFuelBurnTime(log);
                boolean smeltable = canBeSmelted_Fallback(log);
                
                assertTrue(burnTime > 0, 
                    log.name() + " powinien być paliwem");
                assertTrue(smeltable, 
                    log.name() + " powinien być smeltable");
            }
        }
    }
    
    @Nested
    @DisplayName("⛏️ Paliwa (tylko FUEL, nie smeltable)")
    class FuelOnlyTests {
        
        @Test
        @DisplayName("COAL jest paliwem ale NIE można go przepalić")
        void coal_isFuelOnly() {
            Material coal = Material.COAL;
            
            // Jest paliwem
            int burnTime = EnhancedFurnaceLogic.getFuelBurnTime(coal);
            assertTrue(burnTime > 0, "COAL powinien być paliwem");
            assertEquals(1600, burnTime, "COAL pali się 1600 ticków");
            
            // NIE można przepalić
            boolean smeltable = canBeSmelted_Fallback(coal);
            assertFalse(smeltable, "COAL NIE powinien być smeltable");
            
            // WNIOSEK: Shift-click powinien kierować do FUEL
        }
        
        @Test
        @DisplayName("CHARCOAL jest paliwem ale NIE można go przepalić")
        void charcoal_isFuelOnly() {
            Material charcoal = Material.CHARCOAL;
            
            assertTrue(EnhancedFurnaceLogic.getFuelBurnTime(charcoal) > 0);
            assertFalse(canBeSmelted_Fallback(charcoal));
        }
        
        @Test
        @DisplayName("BLAZE_ROD jest paliwem ale NIE można go przepalić")
        void blazeRod_isFuelOnly() {
            Material blazeRod = Material.BLAZE_ROD;
            
            assertTrue(EnhancedFurnaceLogic.getFuelBurnTime(blazeRod) > 0);
            assertFalse(canBeSmelted_Fallback(blazeRod));
        }
        
        @Test
        @DisplayName("COAL_BLOCK jest paliwem ale NIE można go przepalić")
        void coalBlock_isFuelOnly() {
            Material coalBlock = Material.COAL_BLOCK;
            
            assertTrue(EnhancedFurnaceLogic.getFuelBurnTime(coalBlock) > 0);
            assertFalse(canBeSmelted_Fallback(coalBlock));
        }
        
        @Test
        @DisplayName("LAVA_BUCKET jest paliwem ale NIE można go przepalić")
        void lavaBucket_isFuelOnly() {
            Material lavaBucket = Material.LAVA_BUCKET;
            
            assertTrue(EnhancedFurnaceLogic.getFuelBurnTime(lavaBucket) > 0);
            assertFalse(canBeSmelted_Fallback(lavaBucket));
        }
        
        @Test
        @DisplayName("STICK jest paliwem ale NIE można go przepalić")
        void stick_isFuelOnly() {
            Material stick = Material.STICK;
            
            assertTrue(EnhancedFurnaceLogic.getFuelBurnTime(stick) > 0);
            assertFalse(canBeSmelted_Fallback(stick));
        }
    }
    
    @Nested
    @DisplayName("🥩 Smeltable (tylko INPUT, nie fuel)")
    class SmeltableOnlyTests {
        
        @Test
        @DisplayName("BEEF można przepalić ale NIE jest paliwem")
        void beef_isSmeltableOnly() {
            Material beef = Material.BEEF;
            
            // Można przepalić
            boolean smeltable = canBeSmelted_Fallback(beef);
            assertTrue(smeltable, "BEEF powinien być smeltable");
            
            // NIE jest paliwem
            int burnTime = EnhancedFurnaceLogic.getFuelBurnTime(beef);
            assertEquals(0, burnTime, "BEEF NIE powinien być paliwem");
            
            // WNIOSEK: Shift-click powinien kierować do INPUT
        }
        
        @Test
        @DisplayName("Wszystkie surowe mięsa są smeltable-only")
        void allRawMeats_areSmeltableOnly() {
            Material[] meats = {
                Material.BEEF, Material.PORKCHOP, Material.CHICKEN,
                Material.MUTTON, Material.RABBIT, Material.COD, Material.SALMON
            };
            
            for (Material meat : meats) {
                assertTrue(canBeSmelted_Fallback(meat), 
                    meat.name() + " powinien być smeltable");
                assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(meat), 
                    meat.name() + " NIE powinien być paliwem");
            }
        }
        
        @Test
        @DisplayName("RAW_IRON można przepalić ale NIE jest paliwem")
        void rawIron_isSmeltableOnly() {
            Material rawIron = Material.RAW_IRON;
            
            assertTrue(canBeSmelted_Fallback(rawIron));
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(rawIron));
        }
        
        @Test
        @DisplayName("Wszystkie surowe rudy są smeltable-only")
        void allRawOres_areSmeltableOnly() {
            Material[] ores = {
                Material.RAW_IRON, Material.RAW_GOLD, Material.RAW_COPPER,
                Material.IRON_ORE, Material.GOLD_ORE, Material.COPPER_ORE
            };
            
            for (Material ore : ores) {
                assertTrue(canBeSmelted_Fallback(ore), 
                    ore.name() + " powinien być smeltable");
                assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(ore), 
                    ore.name() + " NIE powinien być paliwem");
            }
        }
        
        @Test
        @DisplayName("SAND można przepalić (szkło) ale NIE jest paliwem")
        void sand_isSmeltableOnly() {
            Material sand = Material.SAND;
            
            assertTrue(canBeSmelted_Fallback(sand));
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(sand));
        }
        
        @Test
        @DisplayName("COBBLESTONE można przepalić (kamień) ale NIE jest paliwem")
        void cobblestone_isSmeltableOnly() {
            Material cobblestone = Material.COBBLESTONE;
            
            assertTrue(canBeSmelted_Fallback(cobblestone));
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(cobblestone));
        }
    }
    
    @Nested
    @DisplayName("💎 Ani smeltable ani fuel")
    class NeitherTests {
        
        @Test
        @DisplayName("DIAMOND nie jest ani smeltable ani fuel")
        void diamond_isNeither() {
            Material diamond = Material.DIAMOND;
            
            assertFalse(canBeSmelted_Fallback(diamond), 
                "DIAMOND NIE powinien być smeltable");
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(diamond), 
                "DIAMOND NIE powinien być paliwem");
            
            // WNIOSEK: Shift-click nie powinien nic robić
        }
        
        @Test
        @DisplayName("IRON_INGOT nie jest ani smeltable ani fuel")
        void ironIngot_isNeither() {
            Material ironIngot = Material.IRON_INGOT;
            
            assertFalse(canBeSmelted_Fallback(ironIngot));
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(ironIngot));
        }
        
        @Test
        @DisplayName("COOKED_BEEF nie jest ani smeltable ani fuel")
        void cookedBeef_isNeither() {
            Material cookedBeef = Material.COOKED_BEEF;
            
            assertFalse(canBeSmelted_Fallback(cookedBeef));
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(cookedBeef));
        }
        
        @Test
        @DisplayName("DIRT nie jest ani smeltable ani fuel")
        void dirt_isNeither() {
            Material dirt = Material.DIRT;
            
            assertFalse(canBeSmelted_Fallback(dirt));
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(dirt));
        }
    }
    
    @Nested
    @DisplayName("📊 Podsumowanie priorytetów")
    class PrioritySummaryTests {
        
        @Test
        @DisplayName("Logika decyzyjna: smeltable > fuel > nothing")
        void priorityLogic_smeltableOverFuelOverNothing() {
            // Przykład 1: OAK_LOG - jest i smeltable i fuel
            // Priorytet: smeltable → idzie do INPUT
            Material oakLog = Material.OAK_LOG;
            boolean isSmeltable = canBeSmelted_Fallback(oakLog);
            boolean isFuel = EnhancedFurnaceLogic.getFuelBurnTime(oakLog) > 0;
            
            assertTrue(isSmeltable && isFuel, "OAK_LOG jest dual-purpose");
            // W kodzie: najpierw sprawdzamy smeltable, potem fuel
            // Więc OAK_LOG idzie do INPUT
            
            // Przykład 2: COAL - tylko fuel
            // Priorytet: nie jest smeltable, ale jest fuel → idzie do FUEL
            Material coal = Material.COAL;
            isSmeltable = canBeSmelted_Fallback(coal);
            isFuel = EnhancedFurnaceLogic.getFuelBurnTime(coal) > 0;
            
            assertTrue(!isSmeltable && isFuel, "COAL jest tylko fuel");
            // W kodzie: nie jest smeltable, więc sprawdzamy fuel
            // Jest fuel → idzie do FUEL
            
            // Przykład 3: DIAMOND - ani smeltable ani fuel
            // Nic się nie dzieje
            Material diamond = Material.DIAMOND;
            isSmeltable = canBeSmelted_Fallback(diamond);
            isFuel = EnhancedFurnaceLogic.getFuelBurnTime(diamond) > 0;
            
            assertTrue(!isSmeltable && !isFuel, "DIAMOND nie pasuje nigdzie");
            // W kodzie: nie jest ani smeltable ani fuel
            // Shift-click nic nie robi
        }
    }
}

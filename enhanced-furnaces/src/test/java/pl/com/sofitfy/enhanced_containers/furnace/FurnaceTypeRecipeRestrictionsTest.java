package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Material;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testy dla logiki fallback sprawdzania przepisów w różnych typach pieców.
 * 
 * VANILLA BEHAVIOR:
 * - Smoker: TYLKO jedzenie (mięso, ryby, ziemniaki, kelp) - NIE przepali rud ani cobblestone!
 * - Blast Furnace: TYLKO rudy i metale - NIE przepali jedzenia ani drewna!
 * - Zwykły piec: uniwersalny - przepali wszystko co ma przepis
 * 
 * Te testy weryfikują że enhanced furnace zachowuje się jak vanilla.
 */
@DisplayName("Furnace Type Recipe Restrictions - Fallback Logic Unit Tests")
class FurnaceTypeRecipeRestrictionsTest {
    
    // ========================================================================
    // SMOKER - TYLKO JEDZENIE
    // ========================================================================
    
    @Nested
    @DisplayName("Smoker - tylko jedzenie")
    class SmokerTests {
        
        @Test
        @DisplayName("Smoker MOŻE przepalić surowe mięso (BEEF)")
        void smoker_canSmelt_rawBeef() {
            // Surowa wołowina powinna być przepalana w smokerze
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.BEEF, 
                Material.SMOKER
            );
            
            assertTrue(canSmelt, "Smoker powinien móc przepalić surową wołowinę");
        }
        
        @Test
        @DisplayName("Smoker MOŻE przepalić surową rybę (COD)")
        void smoker_canSmelt_rawFish() {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.COD, 
                Material.SMOKER
            );
            
            assertTrue(canSmelt, "Smoker powinien móc przepalić surowego dorsza");
        }
        
        @Test
        @DisplayName("Smoker MOŻE przepalić ziemniaka (POTATO)")
        void smoker_canSmelt_potato() {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.POTATO, 
                Material.SMOKER
            );
            
            assertTrue(canSmelt, "Smoker powinien móc przepalić ziemniaka");
        }
        
        @Test
        @DisplayName("Smoker MOŻE przepalić kelp")
        void smoker_canSmelt_kelp() {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.KELP, 
                Material.SMOKER
            );
            
            assertTrue(canSmelt, "Smoker powinien móc przepalić kelp");
        }
        
        @Test
        @DisplayName("Smoker NIE MOŻE przepalić rudy żelaza (RAW_IRON)")
        void smoker_cannotSmelt_rawIron() {
            // KLUCZOWY TEST: smoker NIE powinien przepalać rud!
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.RAW_IRON, 
                Material.SMOKER
            );
            
            assertFalse(canSmelt, "Smoker NIE powinien móc przepalić surowego żelaza - to nie jest jedzenie!");
        }
        
        @Test
        @DisplayName("Smoker NIE MOŻE przepalić cobblestone")
        void smoker_cannotSmelt_cobblestone() {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.COBBLESTONE, 
                Material.SMOKER
            );
            
            assertFalse(canSmelt, "Smoker NIE powinien móc przepalić cobblestone - to nie jest jedzenie!");
        }
        
        @Test
        @DisplayName("Smoker NIE MOŻE przepalić piasku (SAND)")
        void smoker_cannotSmelt_sand() {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.SAND, 
                Material.SMOKER
            );
            
            assertFalse(canSmelt, "Smoker NIE powinien móc przepalić piasku - to nie jest jedzenie!");
        }
        
        @Test
        @DisplayName("Smoker NIE MOŻE przepalić drewna (OAK_LOG)")
        void smoker_cannotSmelt_wood() {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.OAK_LOG, 
                Material.SMOKER
            );
            
            assertFalse(canSmelt, "Smoker NIE powinien móc przepalić drewna - to nie jest jedzenie!");
        }
        
        /**
         * Test parametryzowany - wszystkie rodzaje surowego mięsa powinny działać w smokerze
         */
        @ParameterizedTest(name = "Smoker może przepalić {0}")
        @EnumSource(value = Material.class, names = {
            "BEEF", "PORKCHOP", "CHICKEN", "MUTTON", "RABBIT", "COD", "SALMON"
        })
        @DisplayName("Smoker może przepalić wszystkie rodzaje surowego mięsa/ryb")
        void smoker_canSmelt_allRawMeats(Material meat) {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                meat, 
                Material.SMOKER
            );
            
            assertTrue(canSmelt, "Smoker powinien móc przepalić " + meat.name());
        }
    }
    
    // ========================================================================
    // BLAST FURNACE - TYLKO RUDY I METALE
    // ========================================================================
    
    @Nested
    @DisplayName("Blast Furnace - tylko rudy i metale")
    class BlastFurnaceTests {
        
        @Test
        @DisplayName("Blast Furnace MOŻE przepalić surowe żelazo (RAW_IRON)")
        void blastFurnace_canSmelt_rawIron() {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.RAW_IRON, 
                Material.BLAST_FURNACE
            );
            
            assertTrue(canSmelt, "Blast Furnace powinien móc przepalić surowe żelazo");
        }
        
        @Test
        @DisplayName("Blast Furnace MOŻE przepalić rudę żelaza (IRON_ORE)")
        void blastFurnace_canSmelt_ironOre() {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.IRON_ORE, 
                Material.BLAST_FURNACE
            );
            
            assertTrue(canSmelt, "Blast Furnace powinien móc przepalić rudę żelaza");
        }
        
        @Test
        @DisplayName("Blast Furnace MOŻE przepalić ancient debris")
        void blastFurnace_canSmelt_ancientDebris() {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.ANCIENT_DEBRIS, 
                Material.BLAST_FURNACE
            );
            
            assertTrue(canSmelt, "Blast Furnace powinien móc przepalić ancient debris");
        }
        
        @Test
        @DisplayName("Blast Furnace NIE MOŻE przepalić surowego mięsa (BEEF)")
        void blastFurnace_cannotSmelt_rawBeef() {
            // KLUCZOWY TEST: blast furnace NIE powinien przepalać jedzenia!
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.BEEF, 
                Material.BLAST_FURNACE
            );
            
            assertFalse(canSmelt, "Blast Furnace NIE powinien móc przepalić mięsa - to nie jest ruda!");
        }
        
        @Test
        @DisplayName("Blast Furnace NIE MOŻE przepalić cobblestone")
        void blastFurnace_cannotSmelt_cobblestone() {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.COBBLESTONE, 
                Material.BLAST_FURNACE
            );
            
            assertFalse(canSmelt, "Blast Furnace NIE powinien móc przepalić cobblestone - to nie jest ruda!");
        }
        
        @Test
        @DisplayName("Blast Furnace NIE MOŻE przepalić piasku (SAND)")
        void blastFurnace_cannotSmelt_sand() {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.SAND, 
                Material.BLAST_FURNACE
            );
            
            assertFalse(canSmelt, "Blast Furnace NIE powinien móc przepalić piasku - to nie jest ruda!");
        }
        
        @Test
        @DisplayName("Blast Furnace NIE MOŻE przepalić drewna (OAK_LOG)")
        void blastFurnace_cannotSmelt_wood() {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.OAK_LOG, 
                Material.BLAST_FURNACE
            );
            
            assertFalse(canSmelt, "Blast Furnace NIE powinien móc przepalić drewna - to nie jest ruda!");
        }
        
        /**
         * Test parametryzowany - wszystkie rodzaje surowych metali powinny działać w blast furnace
         */
        @ParameterizedTest(name = "Blast Furnace może przepalić {0}")
        @EnumSource(value = Material.class, names = {
            "RAW_IRON", "RAW_GOLD", "RAW_COPPER", 
            "IRON_ORE", "GOLD_ORE", "COPPER_ORE",
            "DEEPSLATE_IRON_ORE", "DEEPSLATE_GOLD_ORE", "DEEPSLATE_COPPER_ORE"
        })
        @DisplayName("Blast Furnace może przepalić wszystkie rodzaje rud/metali")
        void blastFurnace_canSmelt_allOres(Material ore) {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                ore, 
                Material.BLAST_FURNACE
            );
            
            assertTrue(canSmelt, "Blast Furnace powinien móc przepalić " + ore.name());
        }
    }
    
    // ========================================================================
    // ZWYKŁY PIEC - UNIWERSALNY
    // ========================================================================
    
    @Nested
    @DisplayName("Zwykły Piec - uniwersalny")
    class RegularFurnaceTests {
        
        @Test
        @DisplayName("Zwykły piec MOŻE przepalić surowe mięso (BEEF)")
        void furnace_canSmelt_rawBeef() {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.BEEF, 
                Material.FURNACE
            );
            
            assertTrue(canSmelt, "Zwykły piec powinien móc przepalić surową wołowinę");
        }
        
        @Test
        @DisplayName("Zwykły piec MOŻE przepalić surowe żelazo (RAW_IRON)")
        void furnace_canSmelt_rawIron() {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.RAW_IRON, 
                Material.FURNACE
            );
            
            assertTrue(canSmelt, "Zwykły piec powinien móc przepalić surowe żelazo");
        }
        
        @Test
        @DisplayName("Zwykły piec MOŻE przepalić cobblestone")
        void furnace_canSmelt_cobblestone() {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.COBBLESTONE, 
                Material.FURNACE
            );
            
            assertTrue(canSmelt, "Zwykły piec powinien móc przepalić cobblestone");
        }
        
        @Test
        @DisplayName("Zwykły piec MOŻE przepalić piasek (SAND)")
        void furnace_canSmelt_sand() {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.SAND, 
                Material.FURNACE
            );
            
            assertTrue(canSmelt, "Zwykły piec powinien móc przepalić piasek");
        }
        
        @Test
        @DisplayName("Zwykły piec MOŻE przepalić drewno (OAK_LOG)")
        void furnace_canSmelt_wood() {
            boolean canSmelt = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                Material.OAK_LOG, 
                Material.FURNACE
            );
            
            assertTrue(canSmelt, "Zwykły piec powinien móc przepalić drewno na węgiel drzewny");
        }
    }
    
    // ========================================================================
    // TESTY PORÓWNAWCZE - RÓŻNICE MIĘDZY TYPAMI PIECÓW
    // ========================================================================
    
    @Nested
    @DisplayName("Porównanie typów pieców")
    class ComparisonTests {
        
        @Test
        @DisplayName("Surowe mięso: tylko smoker i zwykły piec, NIE blast furnace")
        void rawMeat_smokerAndFurnaceOnly() {
            Material meat = Material.BEEF;
            
            boolean inFurnace = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                meat, Material.FURNACE);
            boolean inSmoker = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                meat, Material.SMOKER);
            boolean inBlast = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                meat, Material.BLAST_FURNACE);
            
            assertTrue(inFurnace, "Mięso powinno działać w zwykłym piecu");
            assertTrue(inSmoker, "Mięso powinno działać w smokerze");
            assertFalse(inBlast, "Mięso NIE powinno działać w blast furnace");
        }
        
        @Test
        @DisplayName("Surowe żelazo: tylko blast furnace i zwykły piec, NIE smoker")
        void rawIron_blastAndFurnaceOnly() {
            Material ore = Material.RAW_IRON;
            
            boolean inFurnace = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                ore, Material.FURNACE);
            boolean inSmoker = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                ore, Material.SMOKER);
            boolean inBlast = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                ore, Material.BLAST_FURNACE);
            
            assertTrue(inFurnace, "Ruda powinna działać w zwykłym piecu");
            assertFalse(inSmoker, "Ruda NIE powinna działać w smokerze");
            assertTrue(inBlast, "Ruda powinna działać w blast furnace");
        }
        
        @Test
        @DisplayName("Cobblestone: tylko zwykły piec, NIE smoker ani blast furnace")
        void cobblestone_furnaceOnly() {
            Material block = Material.COBBLESTONE;
            
            boolean inFurnace = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                block, Material.FURNACE);
            boolean inSmoker = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                block, Material.SMOKER);
            boolean inBlast = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                block, Material.BLAST_FURNACE);
            
            assertTrue(inFurnace, "Cobblestone powinien działać w zwykłym piecu");
            assertFalse(inSmoker, "Cobblestone NIE powinien działać w smokerze");
            assertFalse(inBlast, "Cobblestone NIE powinien działać w blast furnace");
        }
        
        @Test
        @DisplayName("Piasek na szkło: tylko zwykły piec")
        void sand_furnaceOnly() {
            Material sand = Material.SAND;
            
            boolean inFurnace = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                sand, Material.FURNACE);
            boolean inSmoker = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                sand, Material.SMOKER);
            boolean inBlast = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                sand, Material.BLAST_FURNACE);
            
            assertTrue(inFurnace, "Piasek powinien działać w zwykłym piecu (na szkło)");
            assertFalse(inSmoker, "Piasek NIE powinien działać w smokerze");
            assertFalse(inBlast, "Piasek NIE powinien działać w blast furnace");
        }
    }
    
    // ========================================================================
    // TESTY NEGATYWNE - PRZEDMIOTY KTÓRE NIE MAJĄ PRZEPISU
    // ========================================================================
    
    @Nested
    @DisplayName("Przedmioty bez przepisu - żaden piec nie przepali")
    class NoRecipeTests {
        
        @Test
        @DisplayName("Diament nie ma przepisu przepalania w żadnym piecu")
        void diamond_noRecipeAnywhere() {
            Material diamond = Material.DIAMOND;
            
            boolean inFurnace = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                diamond, Material.FURNACE);
            boolean inSmoker = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                diamond, Material.SMOKER);
            boolean inBlast = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                diamond, Material.BLAST_FURNACE);
            
            assertFalse(inFurnace, "Diament nie ma przepisu w zwykłym piecu");
            assertFalse(inSmoker, "Diament nie ma przepisu w smokerze");
            assertFalse(inBlast, "Diament nie ma przepisu w blast furnace");
        }
        
        @Test
        @DisplayName("Dirt nie ma przepisu przepalania w żadnym piecu")
        void dirt_noRecipeAnywhere() {
            Material dirt = Material.DIRT;
            
            boolean inFurnace = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                dirt, Material.FURNACE);
            boolean inSmoker = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                dirt, Material.SMOKER);
            boolean inBlast = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                dirt, Material.BLAST_FURNACE);
            
            assertFalse(inFurnace, "Dirt nie ma przepisu w zwykłym piecu");
            assertFalse(inSmoker, "Dirt nie ma przepisu w smokerze");
            assertFalse(inBlast, "Dirt nie ma przepisu w blast furnace");
        }
        
        @Test
        @DisplayName("Już ugotowane jedzenie nie ma przepisu (COOKED_BEEF)")
        void cookedMeat_noRecipeAnywhere() {
            Material cooked = Material.COOKED_BEEF;
            
            boolean inFurnace = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                cooked, Material.FURNACE);
            boolean inSmoker = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                cooked, Material.SMOKER);
            boolean inBlast = EnhancedFurnaceLogic.canBeSmelted_FallbackForTest(
                cooked, Material.BLAST_FURNACE);
            
            assertFalse(inFurnace, "Ugotowane mięso nie ma przepisu w zwykłym piecu");
            assertFalse(inSmoker, "Ugotowane mięso nie ma przepisu w smokerze");
            assertFalse(inBlast, "Ugotowane mięso nie ma przepisu w blast furnace");
        }
    }
}

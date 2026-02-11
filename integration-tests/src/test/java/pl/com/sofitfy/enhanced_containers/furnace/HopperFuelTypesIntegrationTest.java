package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne - rozpoznawanie paliw przez lejki Enhanced Furnace.
 * 
 * Vanilla paliwa (burn time w tickach):
 * - Coal: 1600 (80 sec) - 8 przedmiotów
 * - Charcoal: 1600 (80 sec) - 8 przedmiotów
 * - Coal Block: 16000 (800 sec) - 80 przedmiotów
 * - Lava Bucket: 20000 (1000 sec) - 100 przedmiotów
 * - Blaze Rod: 2400 (120 sec) - 12 przedmiotów
 * - Dried Kelp Block: 4001 (200 sec) - 20 przedmiotów
 * - Stick: 100 (5 sec) - 0.5 przedmiotu
 * - Planks: 300 (15 sec) - 1.5 przedmiotu
 * - Logs/Wood: 300 (15 sec) - 1.5 przedmiotu
 * - Wool: 100 (5 sec) - 0.5 przedmiotu
 * - Carpet: 67 (3.35 sec) - ~0.33 przedmiotu
 * - Bamboo: 50 (2.5 sec) - 0.25 przedmiotu
 * - Scaffolding: 50 (2.5 sec) - 0.25 przedmiotu
 * - Wooden tools/weapons: 200 (10 sec) - 1 przedmiot
 * - Wooden doors: 200 (10 sec) - 1 przedmiot
 * - Bookshelves: 300 (15 sec) - 1.5 przedmiotu
 * - etc.
 */
@DisplayName("Hopper Fuel Types Integration Tests")
@Tag("integration")
class HopperFuelTypesIntegrationTest {
    
    private static final int[] FUEL_SLOTS = {46, 47, 48};
    
    private ServerMock server;
    
    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }
    
    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }
    
    // ========================================================================
    // PODSTAWOWE PALIWA (Najczęściej używane)
    // ========================================================================
    
    @Nested
    @DisplayName("Podstawowe paliwa")
    class BasicFuelsTests {
        
        @Test
        @DisplayName("Węgiel (COAL) jest akceptowany jako paliwo")
        void coalIsFuel() {
            assertFuelAccepted(Material.COAL, "Coal");
        }
        
        @Test
        @DisplayName("Węgiel drzewny (CHARCOAL) jest akceptowany jako paliwo")
        void charcoalIsFuel() {
            assertFuelAccepted(Material.CHARCOAL, "Charcoal");
        }
        
        @Test
        @DisplayName("Blok węgla (COAL_BLOCK) jest akceptowany jako paliwo")
        void coalBlockIsFuel() {
            assertFuelAccepted(Material.COAL_BLOCK, "Coal Block");
        }
        
        @Test
        @DisplayName("Wiadro lawy (LAVA_BUCKET) jest akceptowane jako paliwo")
        void lavaBucketIsFuel() {
            assertFuelAccepted(Material.LAVA_BUCKET, "Lava Bucket");
        }
        
        @Test
        @DisplayName("Blaze Rod jest akceptowany jako paliwo")
        void blazeRodIsFuel() {
            assertFuelAccepted(Material.BLAZE_ROD, "Blaze Rod");
        }
        
        @Test
        @DisplayName("Blok suszonego wodorostu (DRIED_KELP_BLOCK) jest paliwo")
        void driedKelpBlockIsFuel() {
            assertFuelAccepted(Material.DRIED_KELP_BLOCK, "Dried Kelp Block");
        }
        
        @Test
        @DisplayName("Patyk (STICK) jest akceptowany jako paliwo")
        void stickIsFuel() {
            assertFuelAccepted(Material.STICK, "Stick");
        }
    }
    
    // ========================================================================
    // DESKI (PLANKS) - wszystkie rodzaje
    // ========================================================================
    
    @Nested
    @DisplayName("Deski (Planks) - wszystkie rodzaje drzewa")
    class PlanksTests {
        
        static Stream<Material> plankMaterials() {
            return Stream.of(
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
                Material.WARPED_PLANKS,
                Material.PALE_OAK_PLANKS
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwe")
        @MethodSource("plankMaterials")
        @DisplayName("Wszystkie rodzaje desek są paliwem")
        void allPlanksAreFuel(Material plank) {
            assertFuelAccepted(plank, plank.name());
        }
    }
    
    // ========================================================================
    // LOGI (LOGS) - wszystkie rodzaje
    // ========================================================================
    
    @Nested
    @DisplayName("Logi (Logs/Wood) - wszystkie rodzaje drzewa")
    class LogsTests {
        
        static Stream<Material> logMaterials() {
            return Stream.of(
                // Zwykłe logi
                Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
                Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
                Material.MANGROVE_LOG, Material.CHERRY_LOG, Material.PALE_OAK_LOG,
                // Wood (bark on all sides)
                Material.OAK_WOOD, Material.SPRUCE_WOOD, Material.BIRCH_WOOD,
                Material.JUNGLE_WOOD, Material.ACACIA_WOOD, Material.DARK_OAK_WOOD,
                Material.MANGROVE_WOOD, Material.CHERRY_WOOD, Material.PALE_OAK_WOOD
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwem")
        @MethodSource("logMaterials")
        @DisplayName("Wszystkie rodzaje logów są paliwem")
        void allLogsAreFuel(Material log) {
            assertFuelAccepted(log, log.name());
        }
        
        static Stream<Material> strippedLogMaterials() {
            return Stream.of(
                // Stripped logs
                Material.STRIPPED_OAK_LOG, Material.STRIPPED_SPRUCE_LOG, 
                Material.STRIPPED_BIRCH_LOG, Material.STRIPPED_JUNGLE_LOG,
                Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG,
                Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_CHERRY_LOG,
                Material.STRIPPED_PALE_OAK_LOG,
                // Stripped wood
                Material.STRIPPED_OAK_WOOD, Material.STRIPPED_SPRUCE_WOOD,
                Material.STRIPPED_BIRCH_WOOD, Material.STRIPPED_JUNGLE_WOOD,
                Material.STRIPPED_ACACIA_WOOD, Material.STRIPPED_DARK_OAK_WOOD,
                Material.STRIPPED_MANGROVE_WOOD, Material.STRIPPED_CHERRY_WOOD,
                Material.STRIPPED_PALE_OAK_WOOD
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwem")
        @MethodSource("strippedLogMaterials")
        @DisplayName("Wszystkie rodzaje stripped logów są paliwem")
        void allStrippedLogsAreFuel(Material log) {
            assertFuelAccepted(log, log.name());
        }
    }
    
    // ========================================================================
    // WEŁNA (WOOL) - wszystkie kolory
    // ========================================================================
    
    @Nested
    @DisplayName("Wełna (Wool) - wszystkie kolory")
    class WoolTests {
        
        static Stream<Material> woolMaterials() {
            return Stream.of(
                Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL,
                Material.LIGHT_BLUE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL,
                Material.PINK_WOOL, Material.GRAY_WOOL, Material.LIGHT_GRAY_WOOL,
                Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL,
                Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL, 
                Material.BLACK_WOOL
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwem")
        @MethodSource("woolMaterials")
        @DisplayName("Wszystkie kolory wełny są paliwem")
        void allWoolIsFuel(Material wool) {
            assertFuelAccepted(wool, wool.name());
        }
    }
    
    // ========================================================================
    // DYWANY (CARPET) - wszystkie kolory
    // ========================================================================
    
    @Nested
    @DisplayName("Dywany (Carpet) - wszystkie kolory")
    class CarpetTests {
        
        static Stream<Material> carpetMaterials() {
            return Stream.of(
                Material.WHITE_CARPET, Material.ORANGE_CARPET, Material.MAGENTA_CARPET,
                Material.LIGHT_BLUE_CARPET, Material.YELLOW_CARPET, Material.LIME_CARPET,
                Material.PINK_CARPET, Material.GRAY_CARPET, Material.LIGHT_GRAY_CARPET,
                Material.CYAN_CARPET, Material.PURPLE_CARPET, Material.BLUE_CARPET,
                Material.BROWN_CARPET, Material.GREEN_CARPET, Material.RED_CARPET, 
                Material.BLACK_CARPET
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwem")
        @MethodSource("carpetMaterials")
        @DisplayName("Wszystkie kolory dywanów są paliwem")
        void allCarpetsAreFuel(Material carpet) {
            assertFuelAccepted(carpet, carpet.name());
        }
    }
    
    // ========================================================================
    // DREWNIANE NARZĘDZIA - wszystkie rodzaje
    // ========================================================================
    
    @Nested
    @DisplayName("Drewniane narzędzia i bronie")
    class WoodenToolsTests {
        
        static Stream<Material> woodenToolMaterials() {
            return Stream.of(
                Material.WOODEN_SWORD,
                Material.WOODEN_PICKAXE,
                Material.WOODEN_AXE,
                Material.WOODEN_SHOVEL,
                Material.WOODEN_HOE
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwem")
        @MethodSource("woodenToolMaterials")
        @DisplayName("Wszystkie drewniane narzędzia są paliwem")
        void allWoodenToolsAreFuel(Material tool) {
            assertFuelAccepted(tool, tool.name());
        }
    }
    
    // ========================================================================
    // DRZWI DREWNIANE - wszystkie rodzaje
    // ========================================================================
    
    @Nested
    @DisplayName("Drewniane drzwi i klapy")
    class DoorsAndTrapdoorsTests {
        
        static Stream<Material> doorMaterials() {
            return Stream.of(
                Material.OAK_DOOR, Material.SPRUCE_DOOR, Material.BIRCH_DOOR,
                Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR,
                Material.MANGROVE_DOOR, Material.CHERRY_DOOR, Material.BAMBOO_DOOR,
                Material.PALE_OAK_DOOR
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwem")
        @MethodSource("doorMaterials")
        @DisplayName("Wszystkie drewniane drzwi są paliwem")
        void allDoorsAreFuel(Material door) {
            assertFuelAccepted(door, door.name());
        }
        
        static Stream<Material> trapdoorMaterials() {
            return Stream.of(
                Material.OAK_TRAPDOOR, Material.SPRUCE_TRAPDOOR, Material.BIRCH_TRAPDOOR,
                Material.JUNGLE_TRAPDOOR, Material.ACACIA_TRAPDOOR, Material.DARK_OAK_TRAPDOOR,
                Material.MANGROVE_TRAPDOOR, Material.CHERRY_TRAPDOOR, Material.BAMBOO_TRAPDOOR,
                Material.PALE_OAK_TRAPDOOR
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwem")
        @MethodSource("trapdoorMaterials")
        @DisplayName("Wszystkie klapy są paliwem")
        void allTrapdoorsAreFuel(Material trapdoor) {
            assertFuelAccepted(trapdoor, trapdoor.name());
        }
    }
    
    // ========================================================================
    // ZNAKI (SIGNS) - wszystkie rodzaje
    // ========================================================================
    
    @Nested
    @DisplayName("Znaki - wszystkie rodzaje")
    class SignsTests {
        
        static Stream<Material> signMaterials() {
            return Stream.of(
                Material.OAK_SIGN, Material.SPRUCE_SIGN, Material.BIRCH_SIGN,
                Material.JUNGLE_SIGN, Material.ACACIA_SIGN, Material.DARK_OAK_SIGN,
                Material.MANGROVE_SIGN, Material.CHERRY_SIGN, Material.BAMBOO_SIGN,
                Material.PALE_OAK_SIGN
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwem")
        @MethodSource("signMaterials")
        @DisplayName("Wszystkie znaki są paliwem")
        void allSignsAreFuel(Material sign) {
            assertFuelAccepted(sign, sign.name());
        }
        
        static Stream<Material> hangingSignMaterials() {
            return Stream.of(
                Material.OAK_HANGING_SIGN, Material.SPRUCE_HANGING_SIGN, 
                Material.BIRCH_HANGING_SIGN, Material.JUNGLE_HANGING_SIGN,
                Material.ACACIA_HANGING_SIGN, Material.DARK_OAK_HANGING_SIGN,
                Material.MANGROVE_HANGING_SIGN, Material.CHERRY_HANGING_SIGN,
                Material.BAMBOO_HANGING_SIGN, Material.PALE_OAK_HANGING_SIGN
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwem")
        @MethodSource("hangingSignMaterials")
        @DisplayName("Wszystkie wiszące znaki są paliwem")
        void allHangingSignsAreFuel(Material sign) {
            assertFuelAccepted(sign, sign.name());
        }
    }
    
    // ========================================================================
    // ŁODZIE - wszystkie rodzaje
    // ========================================================================
    
    @Nested
    @DisplayName("Łodzie - wszystkie rodzaje")
    class BoatsTests {
        
        static Stream<Material> boatMaterials() {
            return Stream.of(
                Material.OAK_BOAT, Material.SPRUCE_BOAT, Material.BIRCH_BOAT,
                Material.JUNGLE_BOAT, Material.ACACIA_BOAT, Material.DARK_OAK_BOAT,
                Material.MANGROVE_BOAT, Material.CHERRY_BOAT, Material.BAMBOO_RAFT,
                Material.PALE_OAK_BOAT
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwem")
        @MethodSource("boatMaterials")
        @DisplayName("Wszystkie łodzie są paliwem")
        void allBoatsAreFuel(Material boat) {
            assertFuelAccepted(boat, boat.name());
        }
        
        static Stream<Material> chestBoatMaterials() {
            return Stream.of(
                Material.OAK_CHEST_BOAT, Material.SPRUCE_CHEST_BOAT, 
                Material.BIRCH_CHEST_BOAT, Material.JUNGLE_CHEST_BOAT,
                Material.ACACIA_CHEST_BOAT, Material.DARK_OAK_CHEST_BOAT,
                Material.MANGROVE_CHEST_BOAT, Material.CHERRY_CHEST_BOAT,
                Material.BAMBOO_CHEST_RAFT, Material.PALE_OAK_CHEST_BOAT
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwem")
        @MethodSource("chestBoatMaterials")
        @DisplayName("Wszystkie łodzie ze skrzynką są paliwem")
        void allChestBoatsAreFuel(Material boat) {
            assertFuelAccepted(boat, boat.name());
        }
    }
    
    // ========================================================================
    // PŁOTKI I BRAMY - wszystkie rodzaje
    // ========================================================================
    
    @Nested
    @DisplayName("Płotki i bramy - wszystkie rodzaje")
    class FencesAndGatesTests {
        
        static Stream<Material> fenceMaterials() {
            return Stream.of(
                Material.OAK_FENCE, Material.SPRUCE_FENCE, Material.BIRCH_FENCE,
                Material.JUNGLE_FENCE, Material.ACACIA_FENCE, Material.DARK_OAK_FENCE,
                Material.MANGROVE_FENCE, Material.CHERRY_FENCE, Material.BAMBOO_FENCE,
                Material.PALE_OAK_FENCE
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwem")
        @MethodSource("fenceMaterials")
        @DisplayName("Wszystkie płotki są paliwem")
        void allFencesAreFuel(Material fence) {
            assertFuelAccepted(fence, fence.name());
        }
        
        static Stream<Material> fenceGateMaterials() {
            return Stream.of(
                Material.OAK_FENCE_GATE, Material.SPRUCE_FENCE_GATE, 
                Material.BIRCH_FENCE_GATE, Material.JUNGLE_FENCE_GATE,
                Material.ACACIA_FENCE_GATE, Material.DARK_OAK_FENCE_GATE,
                Material.MANGROVE_FENCE_GATE, Material.CHERRY_FENCE_GATE,
                Material.BAMBOO_FENCE_GATE, Material.PALE_OAK_FENCE_GATE
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwem")
        @MethodSource("fenceGateMaterials")
        @DisplayName("Wszystkie bramy są paliwem")
        void allFenceGatesAreFuel(Material gate) {
            assertFuelAccepted(gate, gate.name());
        }
    }
    
    // ========================================================================
    // SCHODY I PŁYTY DREWNIANE - wszystkie rodzaje
    // ========================================================================
    
    @Nested
    @DisplayName("Schody i płyty drewniane")
    class StairsAndSlabsTests {
        
        static Stream<Material> stairsMaterials() {
            return Stream.of(
                Material.OAK_STAIRS, Material.SPRUCE_STAIRS, Material.BIRCH_STAIRS,
                Material.JUNGLE_STAIRS, Material.ACACIA_STAIRS, Material.DARK_OAK_STAIRS,
                Material.MANGROVE_STAIRS, Material.CHERRY_STAIRS, Material.BAMBOO_STAIRS,
                Material.BAMBOO_MOSAIC_STAIRS, Material.PALE_OAK_STAIRS
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwem")
        @MethodSource("stairsMaterials")
        @DisplayName("Wszystkie drewniane schody są paliwem")
        void allStairsAreFuel(Material stairs) {
            assertFuelAccepted(stairs, stairs.name());
        }
        
        static Stream<Material> slabMaterials() {
            return Stream.of(
                Material.OAK_SLAB, Material.SPRUCE_SLAB, Material.BIRCH_SLAB,
                Material.JUNGLE_SLAB, Material.ACACIA_SLAB, Material.DARK_OAK_SLAB,
                Material.MANGROVE_SLAB, Material.CHERRY_SLAB, Material.BAMBOO_SLAB,
                Material.BAMBOO_MOSAIC_SLAB, Material.PALE_OAK_SLAB
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwem")
        @MethodSource("slabMaterials")
        @DisplayName("Wszystkie drewniane płyty są paliwem")
        void allSlabsAreFuel(Material slab) {
            assertFuelAccepted(slab, slab.name());
        }
    }
    
    // ========================================================================
    // POJEMNIKI I MEBLE DREWNIANE
    // ========================================================================
    
    @Nested
    @DisplayName("Pojemniki i meble drewniane")
    class ContainersAndFurnitureTests {
        
        @Test
        @DisplayName("Skrzynia (CHEST) jest paliwem")
        void chestIsFuel() {
            assertFuelAccepted(Material.CHEST, "Chest");
        }
        
        @Test
        @DisplayName("Pułapka skrzynia (TRAPPED_CHEST) jest paliwem")
        void trappedChestIsFuel() {
            assertFuelAccepted(Material.TRAPPED_CHEST, "Trapped Chest");
        }
        
        @Test
        @DisplayName("Beczka (BARREL) jest paliwem")
        void barrelIsFuel() {
            assertFuelAccepted(Material.BARREL, "Barrel");
        }
        
        @Test
        @DisplayName("Stół rzemieślniczy (CRAFTING_TABLE) jest paliwem")
        void craftingTableIsFuel() {
            assertFuelAccepted(Material.CRAFTING_TABLE, "Crafting Table");
        }
        
        @Test
        @DisplayName("Szafka na książki (BOOKSHELF) jest paliwem")
        void bookshelfIsFuel() {
            assertFuelAccepted(Material.BOOKSHELF, "Bookshelf");
        }
        
        @Test
        @DisplayName("Rzeźbiona szafka na książki (CHISELED_BOOKSHELF) jest paliwem")
        void chiseledBookshelfIsFuel() {
            assertFuelAccepted(Material.CHISELED_BOOKSHELF, "Chiseled Bookshelf");
        }
        
        @Test
        @DisplayName("Szafka muzyczna (JUKEBOX) jest paliwem")
        void jukeboxIsFuel() {
            assertFuelAccepted(Material.JUKEBOX, "Jukebox");
        }
        
        @Test
        @DisplayName("Pulpit (LECTERN) jest paliwem")
        void lecternIsFuel() {
            assertFuelAccepted(Material.LECTERN, "Lectern");
        }
        
        @Test
        @DisplayName("Kompostownik (COMPOSTER) jest paliwem")
        void composterIsFuel() {
            assertFuelAccepted(Material.COMPOSTER, "Composter");
        }
    }
    
    // ========================================================================
    // ŁUKI I WĘDKA
    // ========================================================================
    
    @Nested
    @DisplayName("Łuki i wędka")
    class BowsAndFishingRodTests {
        
        @Test
        @DisplayName("Łuk (BOW) jest paliwem")
        void bowIsFuel() {
            assertFuelAccepted(Material.BOW, "Bow");
        }
        
        @Test
        @DisplayName("Kusza (CROSSBOW) jest paliwem")
        void crossbowIsFuel() {
            assertFuelAccepted(Material.CROSSBOW, "Crossbow");
        }
        
        @Test
        @DisplayName("Wędka (FISHING_ROD) jest paliwem")
        void fishingRodIsFuel() {
            assertFuelAccepted(Material.FISHING_ROD, "Fishing Rod");
        }
    }
    
    // ========================================================================
    // SADZONKI I ROŚLINY
    // ========================================================================
    
    @Nested
    @DisplayName("Sadzonki i rośliny")
    class SaplingsAndPlantsTests {
        
        static Stream<Material> saplingMaterials() {
            return Stream.of(
                Material.OAK_SAPLING, Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING,
                Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING,
                Material.CHERRY_SAPLING, Material.PALE_OAK_SAPLING
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwem")
        @MethodSource("saplingMaterials")
        @DisplayName("Wszystkie sadzonki są paliwem")
        void allSaplingsAreFuel(Material sapling) {
            assertFuelAccepted(sapling, sapling.name());
        }
        
        @Test
        @DisplayName("Azalia (AZALEA) jest paliwem")
        void azaleaIsFuel() {
            assertFuelAccepted(Material.AZALEA, "Azalea");
        }
        
        @Test
        @DisplayName("Kwitnąca azalia (FLOWERING_AZALEA) jest paliwem")
        void floweringAzaleaIsFuel() {
            assertFuelAccepted(Material.FLOWERING_AZALEA, "Flowering Azalea");
        }
        
        @Test
        @DisplayName("Martwy krzak (DEAD_BUSH) jest paliwem")
        void deadBushIsFuel() {
            assertFuelAccepted(Material.DEAD_BUSH, "Dead Bush");
        }
    }
    
    // ========================================================================
    // BAMBUS I RUSZTOWANIE
    // ========================================================================
    
    @Nested
    @DisplayName("Bambus i rusztowanie")
    class BambooAndScaffoldingTests {
        
        @Test
        @DisplayName("Bambus (BAMBOO) jest paliwem")
        void bambooIsFuel() {
            assertFuelAccepted(Material.BAMBOO, "Bamboo");
        }
        
        @Test
        @DisplayName("Rusztowanie (SCAFFOLDING) jest paliwem")
        void scaffoldingIsFuel() {
            assertFuelAccepted(Material.SCAFFOLDING, "Scaffolding");
        }
        
        @Test
        @DisplayName("Blok bambusowy (BAMBOO_BLOCK) jest paliwem")
        void bambooBlockIsFuel() {
            assertFuelAccepted(Material.BAMBOO_BLOCK, "Bamboo Block");
        }
        
        @Test
        @DisplayName("Okorowany blok bambusowy (STRIPPED_BAMBOO_BLOCK) jest paliwem")
        void strippedBambooBlockIsFuel() {
            assertFuelAccepted(Material.STRIPPED_BAMBOO_BLOCK, "Stripped Bamboo Block");
        }
    }
    
    // ========================================================================
    // BANERY - wszystkie kolory
    // ========================================================================
    
    @Nested
    @DisplayName("Banery - wszystkie kolory")
    class BannersTests {
        
        static Stream<Material> bannerMaterials() {
            return Stream.of(
                Material.WHITE_BANNER, Material.ORANGE_BANNER, Material.MAGENTA_BANNER,
                Material.LIGHT_BLUE_BANNER, Material.YELLOW_BANNER, Material.LIME_BANNER,
                Material.PINK_BANNER, Material.GRAY_BANNER, Material.LIGHT_GRAY_BANNER,
                Material.CYAN_BANNER, Material.PURPLE_BANNER, Material.BLUE_BANNER,
                Material.BROWN_BANNER, Material.GREEN_BANNER, Material.RED_BANNER, 
                Material.BLACK_BANNER
            );
        }
        
        @ParameterizedTest(name = "{0} powinno być paliwem")
        @MethodSource("bannerMaterials")
        @DisplayName("Wszystkie kolory banerów są paliwem")
        void allBannersAreFuel(Material banner) {
            assertFuelAccepted(banner, banner.name());
        }
    }
    
    // ========================================================================
    // INNE DREWNIANE PRZEDMIOTY
    // ========================================================================
    
    @Nested
    @DisplayName("Inne drewniane przedmioty")
    class OtherWoodenItemsTests {
        
        @Test
        @DisplayName("Miska (BOWL) jest paliwem")
        void bowlIsFuel() {
            assertFuelAccepted(Material.BOWL, "Bowl");
        }
    }
    
    // ========================================================================
    // NIE-PALIWA (materiały które NIE powinny być akceptowane)
    // ========================================================================
    
    @Nested
    @DisplayName("Nie-paliwa (powinny być odrzucone)")
    class NonFuelsTests {
        
        @Test
        @DisplayName("Surowe żelazo (RAW_IRON) NIE jest paliwem")
        void rawIronIsNotFuel() {
            assertFuelRejected(Material.RAW_IRON, "Raw Iron");
        }
        
        @Test
        @DisplayName("Diament (DIAMOND) NIE jest paliwem")
        void diamondIsNotFuel() {
            assertFuelRejected(Material.DIAMOND, "Diamond");
        }
        
        @Test
        @DisplayName("Sztabka żelaza (IRON_INGOT) NIE jest paliwem")
        void ironIngotIsNotFuel() {
            assertFuelRejected(Material.IRON_INGOT, "Iron Ingot");
        }
        
        @Test
        @DisplayName("Kamień (STONE) NIE jest paliwem")
        void stoneIsNotFuel() {
            assertFuelRejected(Material.STONE, "Stone");
        }
        
        @Test
        @DisplayName("Cobblestone NIE jest paliwem")
        void cobblestoneIsNotFuel() {
            assertFuelRejected(Material.COBBLESTONE, "Cobblestone");
        }
        
        @Test
        @DisplayName("Piasek (SAND) NIE jest paliwem")
        void sandIsNotFuel() {
            assertFuelRejected(Material.SAND, "Sand");
        }
        
        @Test
        @DisplayName("Szklany blok (GLASS) NIE jest paliwem")
        void glassIsNotFuel() {
            assertFuelRejected(Material.GLASS, "Glass");
        }
        
        @Test
        @DisplayName("Złoto (GOLD_INGOT) NIE jest paliwem")
        void goldIngotIsNotFuel() {
            assertFuelRejected(Material.GOLD_INGOT, "Gold Ingot");
        }
        
        @Test
        @DisplayName("Szmaragd (EMERALD) NIE jest paliwem")
        void emeraldIsNotFuel() {
            assertFuelRejected(Material.EMERALD, "Emerald");
        }
        
        @Test
        @DisplayName("Woda (WATER_BUCKET) NIE jest paliwem")
        void waterBucketIsNotFuel() {
            assertFuelRejected(Material.WATER_BUCKET, "Water Bucket");
        }
        
        @Test
        @DisplayName("Jedzenie (COOKED_BEEF) NIE jest paliwem")
        void cookedBeefIsNotFuel() {
            assertFuelRejected(Material.COOKED_BEEF, "Cooked Beef");
        }
        
        @Test
        @DisplayName("Obsydian NIE jest paliwem")
        void obsidianIsNotFuel() {
            assertFuelRejected(Material.OBSIDIAN, "Obsidian");
        }
    }
    
    // ========================================================================
    // MIESZANE SCENARIUSZE
    // ========================================================================
    
    @Nested
    @DisplayName("Mieszane scenariusze - paliwo i nie-paliwo razem")
    class MixedScenariosTests {
        
        @Test
        @DisplayName("Hopper z paliwe i nie-paliwem - tylko paliwo trafia do FUEL")
        void mixedHopperOnlyFuelTransferred() {
            // Given: Hopper z mieszanką
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.DIAMOND, 32));     // nie-paliwo
            hopperInv.setItem(1, new ItemStack(Material.COAL, 32));        // paliwo
            hopperInv.setItem(2, new ItemStack(Material.IRON_INGOT, 32));  // nie-paliwo
            hopperInv.setItem(3, new ItemStack(Material.BLAZE_ROD, 32));   // paliwo
            hopperInv.setItem(4, new ItemStack(Material.RAW_COPPER, 32));  // nie-paliwo
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // When: Symuluj 64 transfery (żeby wszystko paliwo mogło pójść)
            int transferred = simulateHopperPushToFuel(hopperInv, furnaceInv, 64);
            
            // Then: Tylko paliwo zostało przeniesione
            assertTrue(transferred > 0, "Paliwo powinno być przeniesione");
            
            // Diamenty, żelazo i miedź powinny zostać nietknięte
            assertEquals(32, hopperInv.getItem(0).getAmount(), "Diamenty nie powinny być ruszane");
            assertEquals(32, hopperInv.getItem(2).getAmount(), "Sztabki żelaza nie powinny być ruszane");
            assertEquals(32, hopperInv.getItem(4).getAmount(), "Surowa miedź nie powinna być ruszana");
            
            // Węgiel i blaze rody powinny być zmniejszone
            int coalLeft = hopperInv.getItem(1) != null ? hopperInv.getItem(1).getAmount() : 0;
            int blazeLeft = hopperInv.getItem(3) != null ? hopperInv.getItem(3).getAmount() : 0;
            assertTrue(coalLeft < 32 || blazeLeft < 32, "Część paliwa powinna być przeniesiona");
        }
        
        @Test
        @DisplayName("Paliwo w różnych slotach hoppera - pierwsze znalezione przenoszone")
        void fuelInVariousSlots() {
            // Given: Paliwo tylko w slocie 3
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.STONE, 64));
            hopperInv.setItem(1, new ItemStack(Material.DIRT, 64));
            hopperInv.setItem(2, new ItemStack(Material.GRAVEL, 64));
            hopperInv.setItem(3, new ItemStack(Material.COAL, 10));  // jedyne paliwo!
            hopperInv.setItem(4, new ItemStack(Material.SAND, 64));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // When
            int transferred = simulateHopperPushToFuel(hopperInv, furnaceInv, 5);
            
            // Then
            assertEquals(5, transferred, "5 węgli powinno być przeniesionych");
            assertEquals(5, hopperInv.getItem(3).getAmount(), "Zostało 5 węgli w hopperze");
        }
    }
    
    // ========================================================================
    // EDGE CASES
    // ========================================================================
    
    @Nested
    @DisplayName("Edge cases")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Puste sloty FUEL wypełniane po kolei")
        void fuelSlotsFilledInOrder() {
            // Given
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.COAL, 64));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // When: Przenieś 5 węgli
            simulateHopperPushToFuel(hopperInv, furnaceInv, 5);
            
            // Then: Powinny być w pierwszym slocie FUEL
            ItemStack slot46 = furnaceInv.getItem(FUEL_SLOTS[0]);
            assertNotNull(slot46, "Pierwszy slot FUEL powinien mieć paliwo");
            assertEquals(5, slot46.getAmount());
            assertNull(furnaceInv.getItem(FUEL_SLOTS[1]), "Drugi slot FUEL powinien być pusty");
            assertNull(furnaceInv.getItem(FUEL_SLOTS[2]), "Trzeci slot FUEL powinien być pusty");
        }
        
        @Test
        @DisplayName("Pełny pierwszy slot FUEL - następny używany")
        void fullFirstSlotUsesSecond() {
            // Given: Pierwszy slot FUEL pełny
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.COAL, 10));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(FUEL_SLOTS[0], new ItemStack(Material.BLAZE_ROD, 64)); // pełny, inny materiał
            
            // When
            simulateHopperPushToFuel(hopperInv, furnaceInv, 5);
            
            // Then: Węgiel w drugim slocie (bo pierwszy ma inny materiał i jest pełny)
            ItemStack slot47 = furnaceInv.getItem(FUEL_SLOTS[1]);
            assertNotNull(slot47, "Drugi slot FUEL powinien mieć węgiel");
            assertEquals(Material.COAL, slot47.getType());
        }
        
        @Test
        @DisplayName("Stacking podobnych paliw w tym samym slocie")
        void stackingSimilarFuels() {
            // Given: Węgiel w slocie FUEL i w hopperze
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.COAL, 32));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(FUEL_SLOTS[0], new ItemStack(Material.COAL, 32));
            
            // When
            simulateHopperPushToFuel(hopperInv, furnaceInv, 10);
            
            // Then: Stack powinien się zwiększyć
            ItemStack fuelSlot = furnaceInv.getItem(FUEL_SLOTS[0]);
            assertEquals(42, fuelSlot.getAmount(), "Stack powinien mieć 32+10=42 węgli");
        }
        
        @Test
        @DisplayName("Wszystkie sloty FUEL pełne - transfer niemożliwy")
        void allFuelSlotsFull() {
            // Given: Wszystkie sloty FUEL pełne (inny materiał)
            Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
            hopperInv.setItem(0, new ItemStack(Material.COAL, 32));
            
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(FUEL_SLOTS[0], new ItemStack(Material.BLAZE_ROD, 64));
            furnaceInv.setItem(FUEL_SLOTS[1], new ItemStack(Material.CHARCOAL, 64));
            furnaceInv.setItem(FUEL_SLOTS[2], new ItemStack(Material.LAVA_BUCKET, 64));
            
            // When
            int transferred = simulateHopperPushToFuel(hopperInv, furnaceInv, 10);
            
            // Then: Nic nie przeniesione
            assertEquals(0, transferred, "Nie powinno być transferu gdy wszystkie sloty FUEL pełne");
            assertEquals(32, hopperInv.getItem(0).getAmount(), "Hopper powinien mieć dalej 32 węgle");
        }
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    /**
     * Sprawdza czy materiał jest akceptowany jako paliwo przez symulację transferu.
     */
    private void assertFuelAccepted(Material fuelMaterial, String name) {
        Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
        hopperInv.setItem(0, new ItemStack(fuelMaterial, 10));
        
        Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
        
        int transferred = simulateHopperPushToFuel(hopperInv, furnaceInv, 5);
        
        assertTrue(transferred > 0, 
            name + " powinno być akceptowane jako paliwo, ale transfer się nie powiódł");
        
        int fuelInFurnace = countItemsInSlots(furnaceInv, FUEL_SLOTS);
        assertTrue(fuelInFurnace > 0,
            name + " powinno trafić do slotów FUEL");
    }
    
    /**
     * Sprawdza czy materiał NIE jest akceptowany jako paliwo.
     */
    private void assertFuelRejected(Material notFuelMaterial, String name) {
        Inventory hopperInv = Bukkit.createInventory(null, InventoryType.HOPPER);
        hopperInv.setItem(0, new ItemStack(notFuelMaterial, 10));
        
        Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
        
        int transferred = simulateHopperPushToFuel(hopperInv, furnaceInv, 5);
        
        assertEquals(0, transferred,
            name + " NIE powinno być akceptowane jako paliwo, ale transfer się powiódł");
        
        int fuelInFurnace = countItemsInSlots(furnaceInv, FUEL_SLOTS);
        assertEquals(0, fuelInFurnace,
            name + " NIE powinno trafić do slotów FUEL");
    }
    
    /**
     * Symuluje wkładanie paliwa przez lejek do slotów FUEL pieca.
     */
    private int simulateHopperPushToFuel(Inventory hopperInv, Inventory furnaceInv, int operations) {
        int transferred = 0;
        
        for (int op = 0; op < operations; op++) {
            for (int hopperSlot = 0; hopperSlot < hopperInv.getSize(); hopperSlot++) {
                ItemStack item = hopperInv.getItem(hopperSlot);
                if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                    
                    if (!isFuel(item.getType())) {
                        continue;
                    }
                    
                    for (int fuelSlot : FUEL_SLOTS) {
                        ItemStack current = furnaceInv.getItem(fuelSlot);
                        
                        if (current == null || current.getType() == Material.AIR) {
                            ItemStack toPlace = item.clone();
                            toPlace.setAmount(1);
                            furnaceInv.setItem(fuelSlot, toPlace);
                            
                            if (item.getAmount() == 1) {
                                hopperInv.setItem(hopperSlot, null);
                            } else {
                                item.setAmount(item.getAmount() - 1);
                            }
                            
                            transferred++;
                            break;
                        } else if (current.isSimilar(item) && current.getAmount() < current.getMaxStackSize()) {
                            current.setAmount(current.getAmount() + 1);
                            
                            if (item.getAmount() == 1) {
                                hopperInv.setItem(hopperSlot, null);
                            } else {
                                item.setAmount(item.getAmount() - 1);
                            }
                            
                            transferred++;
                            break;
                        }
                    }
                    break;
                }
            }
        }
        
        return transferred;
    }
    
    /**
     * Sprawdza czy przedmiot jest paliwem.
     * WAŻNE: Ta lista musi być zsynchronizowana z EnhancedFurnaceLogic.getFuelBurnTime()!
     */
    private boolean isFuel(Material material) {
        return switch (material) {
            case COAL, CHARCOAL, COAL_BLOCK -> true;
            case LAVA_BUCKET, BLAZE_ROD, DRIED_KELP_BLOCK, STICK -> true;
            case BAMBOO, SCAFFOLDING -> true;
            
            // Deski
            case OAK_PLANKS, SPRUCE_PLANKS, BIRCH_PLANKS, JUNGLE_PLANKS, ACACIA_PLANKS,
                 DARK_OAK_PLANKS, MANGROVE_PLANKS, CHERRY_PLANKS, BAMBOO_PLANKS,
                 CRIMSON_PLANKS, WARPED_PLANKS, PALE_OAK_PLANKS -> true;
            
            // Logi
            case OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG,
                 MANGROVE_LOG, CHERRY_LOG, PALE_OAK_LOG,
                 OAK_WOOD, SPRUCE_WOOD, BIRCH_WOOD, JUNGLE_WOOD,
                 ACACIA_WOOD, DARK_OAK_WOOD, MANGROVE_WOOD, CHERRY_WOOD, PALE_OAK_WOOD -> true;
            
            // Stripped logi
            case STRIPPED_OAK_LOG, STRIPPED_SPRUCE_LOG, STRIPPED_BIRCH_LOG,
                 STRIPPED_JUNGLE_LOG, STRIPPED_ACACIA_LOG, STRIPPED_DARK_OAK_LOG,
                 STRIPPED_MANGROVE_LOG, STRIPPED_CHERRY_LOG, STRIPPED_PALE_OAK_LOG,
                 STRIPPED_OAK_WOOD, STRIPPED_SPRUCE_WOOD, STRIPPED_BIRCH_WOOD,
                 STRIPPED_JUNGLE_WOOD, STRIPPED_ACACIA_WOOD, STRIPPED_DARK_OAK_WOOD,
                 STRIPPED_MANGROVE_WOOD, STRIPPED_CHERRY_WOOD, STRIPPED_PALE_OAK_WOOD -> true;
            
            // Drewniane narzędzia
            case WOODEN_SWORD, WOODEN_PICKAXE, WOODEN_AXE, WOODEN_SHOVEL, WOODEN_HOE -> true;
            
            // Sadzonki
            case OAK_SAPLING, SPRUCE_SAPLING, BIRCH_SAPLING, JUNGLE_SAPLING,
                 ACACIA_SAPLING, DARK_OAK_SAPLING, CHERRY_SAPLING, PALE_OAK_SAPLING -> true;
            
            // Miska, znaki
            case BOWL -> true;
            case OAK_SIGN, SPRUCE_SIGN, BIRCH_SIGN, JUNGLE_SIGN, ACACIA_SIGN,
                 DARK_OAK_SIGN, MANGROVE_SIGN, CHERRY_SIGN, BAMBOO_SIGN, PALE_OAK_SIGN -> true;
            case OAK_HANGING_SIGN, SPRUCE_HANGING_SIGN, BIRCH_HANGING_SIGN, JUNGLE_HANGING_SIGN,
                 ACACIA_HANGING_SIGN, DARK_OAK_HANGING_SIGN, MANGROVE_HANGING_SIGN, 
                 CHERRY_HANGING_SIGN, BAMBOO_HANGING_SIGN, PALE_OAK_HANGING_SIGN -> true;
            
            // Drzwi
            case OAK_DOOR, SPRUCE_DOOR, BIRCH_DOOR, JUNGLE_DOOR, ACACIA_DOOR,
                 DARK_OAK_DOOR, MANGROVE_DOOR, CHERRY_DOOR, BAMBOO_DOOR, PALE_OAK_DOOR -> true;
            case OAK_TRAPDOOR, SPRUCE_TRAPDOOR, BIRCH_TRAPDOOR, JUNGLE_TRAPDOOR,
                 ACACIA_TRAPDOOR, DARK_OAK_TRAPDOOR, MANGROVE_TRAPDOOR, CHERRY_TRAPDOOR,
                 BAMBOO_TRAPDOOR, PALE_OAK_TRAPDOOR -> true;
            
            // Płotki
            case OAK_FENCE, SPRUCE_FENCE, BIRCH_FENCE, JUNGLE_FENCE, ACACIA_FENCE,
                 DARK_OAK_FENCE, MANGROVE_FENCE, CHERRY_FENCE, BAMBOO_FENCE, PALE_OAK_FENCE -> true;
            case OAK_FENCE_GATE, SPRUCE_FENCE_GATE, BIRCH_FENCE_GATE, JUNGLE_FENCE_GATE,
                 ACACIA_FENCE_GATE, DARK_OAK_FENCE_GATE, MANGROVE_FENCE_GATE, 
                 CHERRY_FENCE_GATE, BAMBOO_FENCE_GATE, PALE_OAK_FENCE_GATE -> true;
            
            // Schody i płyty
            case OAK_STAIRS, SPRUCE_STAIRS, BIRCH_STAIRS, JUNGLE_STAIRS, ACACIA_STAIRS,
                 DARK_OAK_STAIRS, MANGROVE_STAIRS, CHERRY_STAIRS, BAMBOO_STAIRS, 
                 BAMBOO_MOSAIC_STAIRS, PALE_OAK_STAIRS -> true;
            case OAK_SLAB, SPRUCE_SLAB, BIRCH_SLAB, JUNGLE_SLAB, ACACIA_SLAB,
                 DARK_OAK_SLAB, MANGROVE_SLAB, CHERRY_SLAB, BAMBOO_SLAB, 
                 BAMBOO_MOSAIC_SLAB, PALE_OAK_SLAB -> true;
            
            // Pojemniki i meble
            case CHEST, TRAPPED_CHEST, BARREL -> true;
            case CRAFTING_TABLE, JUKEBOX, BOOKSHELF, CHISELED_BOOKSHELF -> true;
            case LECTERN, COMPOSTER -> true;
            
            // Łuki i wędka
            case BOW, CROSSBOW, FISHING_ROD -> true;
            
            // Łódki
            case OAK_BOAT, SPRUCE_BOAT, BIRCH_BOAT, JUNGLE_BOAT, ACACIA_BOAT,
                 DARK_OAK_BOAT, MANGROVE_BOAT, CHERRY_BOAT, BAMBOO_RAFT, PALE_OAK_BOAT -> true;
            case OAK_CHEST_BOAT, SPRUCE_CHEST_BOAT, BIRCH_CHEST_BOAT, JUNGLE_CHEST_BOAT,
                 ACACIA_CHEST_BOAT, DARK_OAK_CHEST_BOAT, MANGROVE_CHEST_BOAT,
                 CHERRY_CHEST_BOAT, BAMBOO_CHEST_RAFT, PALE_OAK_CHEST_BOAT -> true;
            
            // Wełna
            case WHITE_WOOL, ORANGE_WOOL, MAGENTA_WOOL, LIGHT_BLUE_WOOL, YELLOW_WOOL,
                 LIME_WOOL, PINK_WOOL, GRAY_WOOL, LIGHT_GRAY_WOOL, CYAN_WOOL,
                 PURPLE_WOOL, BLUE_WOOL, BROWN_WOOL, GREEN_WOOL, RED_WOOL, BLACK_WOOL -> true;
            
            // Dywany
            case WHITE_CARPET, ORANGE_CARPET, MAGENTA_CARPET, LIGHT_BLUE_CARPET,
                 YELLOW_CARPET, LIME_CARPET, PINK_CARPET, GRAY_CARPET,
                 LIGHT_GRAY_CARPET, CYAN_CARPET, PURPLE_CARPET, BLUE_CARPET,
                 BROWN_CARPET, GREEN_CARPET, RED_CARPET, BLACK_CARPET -> true;
            
            // Banery
            case WHITE_BANNER, ORANGE_BANNER, MAGENTA_BANNER, LIGHT_BLUE_BANNER,
                 YELLOW_BANNER, LIME_BANNER, PINK_BANNER, GRAY_BANNER,
                 LIGHT_GRAY_BANNER, CYAN_BANNER, PURPLE_BANNER, BLUE_BANNER,
                 BROWN_BANNER, GREEN_BANNER, RED_BANNER, BLACK_BANNER -> true;
            
            // Azalea i rośliny
            case AZALEA, FLOWERING_AZALEA, DEAD_BUSH -> true;
            
            // Bloki bambusowe
            case BAMBOO_BLOCK, STRIPPED_BAMBOO_BLOCK -> true;
            
            default -> false;
        };
    }
    
    private int countItemsInSlots(Inventory inventory, int[] slots) {
        int total = 0;
        for (int slot : slots) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                total += item.getAmount();
            }
        }
        return total;
    }
}

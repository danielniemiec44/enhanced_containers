package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Material;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne dla tablicy paliw (fallback gdy NMS niedostępny)
 * 
 * Fallback tablica jest używana:
 * - W testach jednostkowych (bez prawdziwego serwera)
 * - Gdy NMS jest niedostępny (np. MockBukkit)
 * 
 * Na prawdziwym serwerze paliwa są pobierane z NMS FuelValues.
 */
@DisplayName("Tablica paliw - fallback")
class EnhancedFurnaceFuelTableTest {

    private static ServerMock server;

    @BeforeAll
    static void setUpServer() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void tearDownServer() {
        MockBukkit.unmock();
    }

    @Nested
    @DisplayName("Podstawowe paliwa")
    class BasicFuels {
        
        @Test
        @DisplayName("Węgiel pali się 1600 ticków (80 sekund)")
        void coal_shouldBurn1600Ticks() {
            assertEquals(1600, EnhancedFurnaceLogic.getFuelBurnTime(Material.COAL));
            assertEquals(1600, EnhancedFurnaceLogic.getFuelBurnTime(Material.CHARCOAL));
        }
        
        @Test
        @DisplayName("Blok węgla pali się 16000 ticków (13:20 minut)")
        void coalBlock_shouldBurn16000Ticks() {
            assertEquals(16000, EnhancedFurnaceLogic.getFuelBurnTime(Material.COAL_BLOCK));
        }
        
        @Test
        @DisplayName("Lava bucket pali się 20000 ticków (16:40 minut)")
        void lavaBucket_shouldBurn20000Ticks() {
            assertEquals(20000, EnhancedFurnaceLogic.getFuelBurnTime(Material.LAVA_BUCKET));
        }
        
        @Test
        @DisplayName("Blaze rod pali się 2400 ticków (2 minuty)")
        void blazeRod_shouldBurn2400Ticks() {
            assertEquals(2400, EnhancedFurnaceLogic.getFuelBurnTime(Material.BLAZE_ROD));
        }
        
        @Test
        @DisplayName("Dried kelp block pali się 4001 ticków")
        void driedKelpBlock_shouldBurn4001Ticks() {
            // 4001 ticków - specjalna wartość (topi 20 itemów + 1 tick extra)
            assertEquals(4001, EnhancedFurnaceLogic.getFuelBurnTime(Material.DRIED_KELP_BLOCK));
        }
        
        @Test
        @DisplayName("Stick pali się 100 ticków (5 sekund)")
        void stick_shouldBurn100Ticks() {
            assertEquals(100, EnhancedFurnaceLogic.getFuelBurnTime(Material.STICK));
        }
        
        @Test
        @DisplayName("Bamboo pali się 50 ticków (2.5 sekundy)")
        void bamboo_shouldBurn50Ticks() {
            assertEquals(50, EnhancedFurnaceLogic.getFuelBurnTime(Material.BAMBOO));
            assertEquals(50, EnhancedFurnaceLogic.getFuelBurnTime(Material.SCAFFOLDING));
        }
    }
    
    @Nested
    @DisplayName("Drewniane przedmioty - 300 ticków")
    class WoodenItems300Ticks {
        
        @ParameterizedTest
        @DisplayName("Deski wszystkich typów drewna")
        @EnumSource(value = Material.class, names = {
            "OAK_PLANKS", "SPRUCE_PLANKS", "BIRCH_PLANKS", "JUNGLE_PLANKS",
            "ACACIA_PLANKS", "DARK_OAK_PLANKS", "MANGROVE_PLANKS", "CHERRY_PLANKS",
            "BAMBOO_PLANKS", "CRIMSON_PLANKS", "WARPED_PLANKS", "PALE_OAK_PLANKS"
        })
        void planks_shouldBurn300Ticks(Material planks) {
            assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(planks),
                "Deski " + planks + " powinny palić się 300 ticków");
        }
        
        @ParameterizedTest
        @DisplayName("Płotki wszystkich typów drewna")
        @EnumSource(value = Material.class, names = {
            "OAK_FENCE", "SPRUCE_FENCE", "BIRCH_FENCE", "JUNGLE_FENCE",
            "ACACIA_FENCE", "DARK_OAK_FENCE", "MANGROVE_FENCE", "CHERRY_FENCE",
            "BAMBOO_FENCE", "PALE_OAK_FENCE"
        })
        void fences_shouldBurn300Ticks(Material fence) {
            assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(fence),
                "Płotek " + fence + " powinien palić się 300 ticków");
        }
        
        @ParameterizedTest
        @DisplayName("Bramy płotkowe wszystkich typów drewna")
        @EnumSource(value = Material.class, names = {
            "OAK_FENCE_GATE", "SPRUCE_FENCE_GATE", "BIRCH_FENCE_GATE", 
            "JUNGLE_FENCE_GATE", "ACACIA_FENCE_GATE", "DARK_OAK_FENCE_GATE",
            "MANGROVE_FENCE_GATE", "CHERRY_FENCE_GATE", "BAMBOO_FENCE_GATE",
            "PALE_OAK_FENCE_GATE"
        })
        void fenceGates_shouldBurn300Ticks(Material fenceGate) {
            assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(fenceGate),
                "Brama " + fenceGate + " powinna palić się 300 ticków");
        }
        
        @ParameterizedTest
        @DisplayName("Schody wszystkich typów drewna")
        @EnumSource(value = Material.class, names = {
            "OAK_STAIRS", "SPRUCE_STAIRS", "BIRCH_STAIRS", "JUNGLE_STAIRS",
            "ACACIA_STAIRS", "DARK_OAK_STAIRS", "MANGROVE_STAIRS", "CHERRY_STAIRS",
            "BAMBOO_STAIRS", "BAMBOO_MOSAIC_STAIRS", "PALE_OAK_STAIRS"
        })
        void stairs_shouldBurn300Ticks(Material stairs) {
            assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(stairs),
                "Schody " + stairs + " powinny palić się 300 ticków");
        }
        
        @ParameterizedTest
        @DisplayName("Logi wszystkich typów drewna")
        @EnumSource(value = Material.class, names = {
            "OAK_LOG", "SPRUCE_LOG", "BIRCH_LOG", "JUNGLE_LOG", "ACACIA_LOG",
            "DARK_OAK_LOG", "MANGROVE_LOG", "CHERRY_LOG", "PALE_OAK_LOG"
        })
        void logs_shouldBurn300Ticks(Material log) {
            assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(log),
                "Log " + log + " powinien palić się 300 ticków");
        }
        
        @ParameterizedTest
        @DisplayName("Banery wszystkich kolorów")
        @EnumSource(value = Material.class, names = {
            "WHITE_BANNER", "ORANGE_BANNER", "MAGENTA_BANNER", "LIGHT_BLUE_BANNER",
            "YELLOW_BANNER", "LIME_BANNER", "PINK_BANNER", "GRAY_BANNER",
            "LIGHT_GRAY_BANNER", "CYAN_BANNER", "PURPLE_BANNER", "BLUE_BANNER",
            "BROWN_BANNER", "GREEN_BANNER", "RED_BANNER", "BLACK_BANNER"
        })
        void banners_shouldBurn300Ticks(Material banner) {
            assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(banner),
                "Baner " + banner + " powinien palić się 300 ticków");
        }
    }
    
    @Nested
    @DisplayName("Drewniane przedmioty - 200 ticków")
    class WoodenItems200Ticks {
        
        @ParameterizedTest
        @DisplayName("Drewniane narzędzia")
        @EnumSource(value = Material.class, names = {
            "WOODEN_SWORD", "WOODEN_PICKAXE", "WOODEN_AXE", 
            "WOODEN_SHOVEL", "WOODEN_HOE"
        })
        void woodenTools_shouldBurn200Ticks(Material tool) {
            assertEquals(200, EnhancedFurnaceLogic.getFuelBurnTime(tool),
                "Narzędzie " + tool + " powinno palić się 200 ticków");
        }
        
        @ParameterizedTest
        @DisplayName("Drzwi wszystkich typów drewna")
        @EnumSource(value = Material.class, names = {
            "OAK_DOOR", "SPRUCE_DOOR", "BIRCH_DOOR", "JUNGLE_DOOR",
            "ACACIA_DOOR", "DARK_OAK_DOOR", "MANGROVE_DOOR", "CHERRY_DOOR",
            "BAMBOO_DOOR", "PALE_OAK_DOOR"
        })
        void doors_shouldBurn200Ticks(Material door) {
            assertEquals(200, EnhancedFurnaceLogic.getFuelBurnTime(door),
                "Drzwi " + door + " powinny palić się 200 ticków");
        }
        
        @ParameterizedTest
        @DisplayName("Znaki wszystkich typów drewna")
        @EnumSource(value = Material.class, names = {
            "OAK_SIGN", "SPRUCE_SIGN", "BIRCH_SIGN", "JUNGLE_SIGN",
            "ACACIA_SIGN", "DARK_OAK_SIGN", "MANGROVE_SIGN", "CHERRY_SIGN",
            "BAMBOO_SIGN", "PALE_OAK_SIGN"
        })
        void signs_shouldBurn200Ticks(Material sign) {
            assertEquals(200, EnhancedFurnaceLogic.getFuelBurnTime(sign),
                "Znak " + sign + " powinien palić się 200 ticków");
        }
    }
    
    @Nested
    @DisplayName("Łodzie - 1200 ticków")
    class Boats1200Ticks {
        
        @ParameterizedTest
        @DisplayName("Łodzie wszystkich typów drewna")
        @EnumSource(value = Material.class, names = {
            "OAK_BOAT", "SPRUCE_BOAT", "BIRCH_BOAT", "JUNGLE_BOAT",
            "ACACIA_BOAT", "DARK_OAK_BOAT", "MANGROVE_BOAT", "CHERRY_BOAT",
            "BAMBOO_RAFT", "PALE_OAK_BOAT"
        })
        void boats_shouldBurn1200Ticks(Material boat) {
            assertEquals(1200, EnhancedFurnaceLogic.getFuelBurnTime(boat),
                "Łódź " + boat + " powinna palić się 1200 ticków");
        }
        
        @ParameterizedTest
        @DisplayName("Łodzie ze skrzynią")
        @EnumSource(value = Material.class, names = {
            "OAK_CHEST_BOAT", "SPRUCE_CHEST_BOAT", "BIRCH_CHEST_BOAT",
            "JUNGLE_CHEST_BOAT", "ACACIA_CHEST_BOAT", "DARK_OAK_CHEST_BOAT",
            "MANGROVE_CHEST_BOAT", "CHERRY_CHEST_BOAT", "BAMBOO_CHEST_RAFT",
            "PALE_OAK_CHEST_BOAT"
        })
        void chestBoats_shouldBurn1200Ticks(Material chestBoat) {
            assertEquals(1200, EnhancedFurnaceLogic.getFuelBurnTime(chestBoat),
                "Łódź ze skrzynią " + chestBoat + " powinna palić się 1200 ticków");
        }
    }
    
    @Nested
    @DisplayName("Wełna i dywany")
    class WoolAndCarpets {
        
        @ParameterizedTest
        @DisplayName("Wełna - 100 ticków")
        @EnumSource(value = Material.class, names = {
            "WHITE_WOOL", "ORANGE_WOOL", "MAGENTA_WOOL", "LIGHT_BLUE_WOOL",
            "YELLOW_WOOL", "LIME_WOOL", "PINK_WOOL", "GRAY_WOOL",
            "LIGHT_GRAY_WOOL", "CYAN_WOOL", "PURPLE_WOOL", "BLUE_WOOL",
            "BROWN_WOOL", "GREEN_WOOL", "RED_WOOL", "BLACK_WOOL"
        })
        void wool_shouldBurn100Ticks(Material wool) {
            assertEquals(100, EnhancedFurnaceLogic.getFuelBurnTime(wool),
                "Wełna " + wool + " powinna palić się 100 ticków");
        }
        
        @ParameterizedTest
        @DisplayName("Dywany - 67 ticków")
        @EnumSource(value = Material.class, names = {
            "WHITE_CARPET", "ORANGE_CARPET", "MAGENTA_CARPET", "LIGHT_BLUE_CARPET",
            "YELLOW_CARPET", "LIME_CARPET", "PINK_CARPET", "GRAY_CARPET",
            "LIGHT_GRAY_CARPET", "CYAN_CARPET", "PURPLE_CARPET", "BLUE_CARPET",
            "BROWN_CARPET", "GREEN_CARPET", "RED_CARPET", "BLACK_CARPET"
        })
        void carpets_shouldBurn67Ticks(Material carpet) {
            assertEquals(67, EnhancedFurnaceLogic.getFuelBurnTime(carpet),
                "Dywan " + carpet + " powinien palić się 67 ticków");
        }
    }
    
    @Nested
    @DisplayName("Przedmioty które NIE są paliwem")
    class NonFuelItems {
        
        @ParameterizedTest
        @DisplayName("Kamienne przedmioty nie są paliwem")
        @EnumSource(value = Material.class, names = {
            "STONE", "COBBLESTONE", "STONE_BRICKS", "STONE_SWORD",
            "STONE_PICKAXE", "STONE_AXE", "STONE_SHOVEL", "STONE_HOE"
        })
        void stoneItems_shouldNotBeFuel(Material stone) {
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(stone),
                stone + " nie powinno być paliwem");
        }
        
        @ParameterizedTest
        @DisplayName("Metalowe przedmioty nie są paliwem")
        @EnumSource(value = Material.class, names = {
            "IRON_INGOT", "GOLD_INGOT", "COPPER_INGOT", "NETHERITE_INGOT",
            "IRON_SWORD", "IRON_PICKAXE", "DIAMOND_SWORD", "DIAMOND_PICKAXE"
        })
        void metalItems_shouldNotBeFuel(Material metal) {
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(metal),
                metal + " nie powinno być paliwem");
        }
        
        @Test
        @DisplayName("AIR nie jest paliwem")
        void air_shouldNotBeFuel() {
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(Material.AIR));
        }
        
        @Test
        @DisplayName("null nie jest paliwem")
        void null_shouldNotBeFuel() {
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(null));
        }
    }
    
    @Nested
    @DisplayName("Pojemniki i meble")
    class ContainersAndFurniture {
        
        @Test
        @DisplayName("Skrzynie palą się 300 ticków")
        void chests_shouldBurn300Ticks() {
            assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(Material.CHEST));
            assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(Material.TRAPPED_CHEST));
            assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(Material.BARREL));
        }
        
        @Test
        @DisplayName("Stół rzemieślniczy pali się 300 ticków")
        void craftingTable_shouldBurn300Ticks() {
            assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(Material.CRAFTING_TABLE));
        }
        
        @Test
        @DisplayName("Regały i szafki palą się 300 ticków")
        void bookshelves_shouldBurn300Ticks() {
            assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(Material.BOOKSHELF));
            assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(Material.CHISELED_BOOKSHELF));
            assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(Material.LECTERN));
        }
    }
}

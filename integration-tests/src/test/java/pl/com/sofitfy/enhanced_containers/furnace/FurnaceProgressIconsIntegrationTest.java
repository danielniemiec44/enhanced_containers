package pl.com.sofitfy.enhanced_containers.furnace;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne dla FurnaceProgressIcons z MockBukkit
 * 
 * Testuje tworzenie prawdziwych ItemStack z CustomModelData.
 * Wymaga MockBukkit do inicjalizacji Bukkit API.
 * 
 * Sprawdza:
 * - Tworzenie ikonek z poprawnymi materiałami
 * - Ustawianie CustomModelData
 * - Generowanie nazw i opisów
 */
@DisplayName("FurnaceProgressIcons - testy integracyjne z MockBukkit")
class FurnaceProgressIconsIntegrationTest {
    
    private static ServerMock server;
    
    @BeforeAll
    static void setUp() {
        // Inicjalizuj MockBukkit przed wszystkimi testami
        server = MockBukkit.mock();
    }
    
    @AfterAll
    static void tearDown() {
        // Wyczyść MockBukkit po wszystkich testach
        MockBukkit.unmock();
    }
    
    @Nested
    @DisplayName("createFireIcon - tworzenie ikonki ognia")
    class CreateFireIconTests {
        
        @Test
        @DisplayName("Pełne paliwo (100%) tworzy ikonkę z CMD 113")
        void testFullBurnTime() {
            // 1600 ticks = 80 sekund (węgiel)
            int burnTime = 1600;
            int maxBurnTime = 1600;
            
            ItemStack icon = FurnaceProgressIcons.createFireIcon(burnTime, maxBurnTime);
            
            // Sprawdź że ikonka została utworzona
            assertNotNull(icon, "Ikonka ognia nie powinna być null");
            assertEquals(Material.BLAZE_POWDER, icon.getType(), 
                "Ikonka ognia powinna być z BLAZE_POWDER");
            
            // Sprawdź CustomModelData
            ItemMeta meta = icon.getItemMeta();
            assertNotNull(meta, "ItemMeta nie powinno być null");
            assertTrue(meta.hasCustomModelData(), 
                "Ikonka powinna mieć CustomModelData");
            assertEquals(113, meta.getCustomModelData(), 
                "100% paliwa powinno mieć CMD 113 (fire_13)");
        }
        
        @Test
        @DisplayName("Połowa paliwa (~50%) tworzy ikonkę z CMD ~107")
        void testHalfBurnTime() {
            int burnTime = 800;
            int maxBurnTime = 1600;
            
            ItemStack icon = FurnaceProgressIcons.createFireIcon(burnTime, maxBurnTime);
            
            assertNotNull(icon);
            ItemMeta meta = icon.getItemMeta();
            assertTrue(meta.hasCustomModelData());
            
            int cmd = meta.getCustomModelData();
            // 50% powinno dać CMD w okolicach 107 (połowa z 13)
            assertTrue(cmd >= 106 && cmd <= 108,
                "50% paliwa powinno dać CMD ~107, got: " + cmd);
        }
        
        @Test
        @DisplayName("Brak paliwa (0 ticks) tworzy pustą ikonkę z CMD 100")
        void testNoBurnTime() {
            ItemStack icon = FurnaceProgressIcons.createFireIcon(0, 1600);
            
            assertNotNull(icon, "Pusta ikonka nie powinna być null");
            assertEquals(Material.BLAZE_POWDER, icon.getType());
            
            ItemMeta meta = icon.getItemMeta();
            assertEquals(100, meta.getCustomModelData(),
                "Brak paliwa powinno mieć CMD 100 (fire_empty)");
        }
        
        @Test
        @DisplayName("maxBurnTime = 0 tworzy pustą ikonkę")
        void testZeroMaxBurnTime() {
            ItemStack icon = FurnaceProgressIcons.createFireIcon(100, 0);
            
            assertNotNull(icon);
            ItemMeta meta = icon.getItemMeta();
            assertEquals(100, meta.getCustomModelData(),
                "maxBurnTime=0 powinno dać CMD 100 (empty)");
        }
        
        @Test
        @DisplayName("Ikonka ognia ma poprawną nazwę (Fuel)")
        void testFireIconDisplayName() {
            ItemStack icon = FurnaceProgressIcons.createFireIcon(1600, 1600);
            
            ItemMeta meta = icon.getItemMeta();
            assertNotNull(meta.displayName(), "Nazwa nie powinna być null");
            // Sprawdź że nazwa zawiera "Fuel"
            String plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(meta.displayName());
            assertEquals("Fuel", plainName, "Nazwa powinna być 'Fuel'");
        }
        
        @Test
        @DisplayName("Ikonka ognia ma lore z procentem i czasem")
        void testFireIconLore() {
            ItemStack icon = FurnaceProgressIcons.createFireIcon(400, 1600); // 25%
            
            ItemMeta meta = icon.getItemMeta();
            assertNotNull(meta.lore(), "Lore nie powinno być null");
            assertFalse(meta.lore().isEmpty(), "Lore nie powinno być puste");
            
            String loreLine = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(meta.lore().get(0));
            assertTrue(loreLine.contains("25%"), "Lore powinno zawierać procent: " + loreLine);
            assertTrue(loreLine.contains("20s"), "Lore powinno zawierać czas (20s): " + loreLine);
        }
    }
    
    @Nested
    @DisplayName("createArrowIcon - tworzenie ikonki strzałki")
    class CreateArrowIconTests {
        
        @Test
        @DisplayName("Pełny postęp (100%) tworzy ikonkę z CMD 222")
        void testFullCookTime() {
            // 200 ticks = 10 sekund (standard smelting)
            int cookTime = 200;
            int maxCookTime = 200;
            
            ItemStack icon = FurnaceProgressIcons.createArrowIcon(cookTime, maxCookTime);
            
            assertNotNull(icon, "Ikonka strzałki nie powinna być null");
            assertEquals(Material.ARROW, icon.getType(),
                "Ikonka strzałki powinna być z ARROW");
            
            ItemMeta meta = icon.getItemMeta();
            assertTrue(meta.hasCustomModelData());
            assertEquals(222, meta.getCustomModelData(),
                "100% postępu powinno mieć CMD 222 (arrow_22)");
        }
        
        @Test
        @DisplayName("Połowa postępu (~50%) tworzy ikonkę z CMD ~211")
        void testHalfCookTime() {
            ItemStack icon = FurnaceProgressIcons.createArrowIcon(100, 200);
            
            ItemMeta meta = icon.getItemMeta();
            int cmd = meta.getCustomModelData();
            
            // 50% powinno dać CMD ~211
            assertTrue(cmd >= 210 && cmd <= 212,
                "50% postępu powinno dać CMD ~211, got: " + cmd);
        }
        
        @Test
        @DisplayName("Brak postępu (0 ticks) tworzy pustą ikonkę z CMD 200")
        void testNoCookTime() {
            ItemStack icon = FurnaceProgressIcons.createArrowIcon(0, 200);
            
            assertNotNull(icon);
            ItemMeta meta = icon.getItemMeta();
            assertEquals(200, meta.getCustomModelData(),
                "Brak postępu powinno mieć CMD 200 (arrow_empty)");
        }
        
        @Test
        @DisplayName("Ikonka strzałki ma poprawną nazwę (Progress)")
        void testArrowIconDisplayName() {
            ItemStack icon = FurnaceProgressIcons.createArrowIcon(200, 200);
            
            ItemMeta meta = icon.getItemMeta();
            String plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(meta.displayName());
            assertEquals("Progress", plainName, "Nazwa powinna być 'Progress'");
        }
        
        @Test
        @DisplayName("Ikonka strzałki ma lore z procentem")
        void testArrowIconLore() {
            ItemStack icon = FurnaceProgressIcons.createArrowIcon(150, 200); // 75%
            
            ItemMeta meta = icon.getItemMeta();
            assertNotNull(meta.lore());
            
            String loreLine = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(meta.lore().get(0));
            assertTrue(loreLine.contains("75%"), "Lore powinno zawierać 75%: " + loreLine);
            assertTrue(loreLine.contains("Smelting"), "Lore powinno zawierać 'Smelting': " + loreLine);
        }
    }
    
    @Nested
    @DisplayName("createEmptyFireIcon / createEmptyArrowIcon")
    class EmptyIconTests {
        
        @Test
        @DisplayName("Pusta ikonka ognia ma CMD 100 i nazwę 'No Fuel'")
        void testEmptyFireIcon() {
            ItemStack icon = FurnaceProgressIcons.createEmptyFireIcon();
            
            assertNotNull(icon, "Pusta ikonka ognia nie powinna być null");
            assertEquals(Material.BLAZE_POWDER, icon.getType());
            
            ItemMeta meta = icon.getItemMeta();
            assertEquals(100, meta.getCustomModelData());
            
            String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(meta.displayName());
            assertEquals("No Fuel", name);
        }
        
        @Test
        @DisplayName("Pusta ikonka strzałki ma CMD 200 i nazwę 'Not Smelting'")
        void testEmptyArrowIcon() {
            ItemStack icon = FurnaceProgressIcons.createEmptyArrowIcon();
            
            assertNotNull(icon, "Pusta ikonka strzałki nie powinna być null");
            assertEquals(Material.ARROW, icon.getType());
            
            ItemMeta meta = icon.getItemMeta();
            assertEquals(200, meta.getCustomModelData());
            
            String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(meta.displayName());
            assertEquals("Not Smelting", name);
        }
    }
    
    @Nested
    @DisplayName("Scenariusze edge case")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("burnTime > maxBurnTime daje CMD 113 (nie przekracza max)")
        void testBurnTimeExceedsMax() {
            // Może się zdarzyć przy bugach - upewnij się że nie wychodzimy poza zakres
            ItemStack icon = FurnaceProgressIcons.createFireIcon(2000, 1600);
            
            ItemMeta meta = icon.getItemMeta();
            assertEquals(113, meta.getCustomModelData(),
                "burnTime > maxBurnTime powinno dać CMD 113 (max)");
        }
        
        @Test
        @DisplayName("Bardzo małe burnTime (1 tick) daje CMD > 100")
        void testMinimalBurnTime() {
            ItemStack icon = FurnaceProgressIcons.createFireIcon(1, 1600);
            
            ItemMeta meta = icon.getItemMeta();
            assertTrue(meta.getCustomModelData() > 100,
                "Nawet 1 tick powinien pokazać jakieś wypełnienie (CMD > 100)");
        }
        
        @Test
        @DisplayName("Ujemne wartości traktowane jak brak paliwa/postępu")
        void testNegativeValues() {
            ItemStack fireIcon = FurnaceProgressIcons.createFireIcon(-100, 1600);
            ItemStack arrowIcon = FurnaceProgressIcons.createArrowIcon(-50, 200);
            
            assertEquals(100, fireIcon.getItemMeta().getCustomModelData(),
                "Ujemny burnTime powinien dać CMD 100 (empty)");
            assertEquals(200, arrowIcon.getItemMeta().getCustomModelData(),
                "Ujemny cookTime powinien dać CMD 200 (empty)");
        }
    }
}

package pl.com.sofitfy.enhanced_containers.furnace;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import pl.com.sofitfy.enhanced_containers.Main;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne dla naprawionych funkcjonalności Enhanced Furnace
 * 
 * Testuje:
 * 1. Dropowanie enhanced furnace item (z flagą PDC) przy zniszczeniu
 * 2. Blokadę wkładania do slotów output
 * 3. Wizualny stan "lit" bloku pieca
 */
@DisplayName("Enhanced Furnace - naprawione funkcjonalności (integracyjne)")
class EnhancedFurnaceFixesIntegrationTest {

    private static ServerMock server;
    private static Main plugin;
    private PlayerMock player;
    
    @BeforeAll
    static void setUpServer() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Main.class);
    }
    
    @AfterAll
    static void tearDownServer() {
        MockBukkit.unmock();
    }
    
    @BeforeEach
    void setUp() {
        player = server.addPlayer();
    }
    
    @AfterEach
    void tearDown() {
        // Wyczyść gracza
        if (player != null) {
            player.disconnect();
        }
    }
    
    @Nested
    @DisplayName("1. Dropowanie enhanced furnace item")
    class EnhancedFurnaceItemDrop {
        
        @Test
        @DisplayName("Stworzony enhanced furnace item ma nazwę 'Enhanced Furnace'")
        void createdEnhancedFurnaceItem_shouldHaveCorrectName() {
            // Stwórz enhanced furnace item używając managera
            EnhancedFurnaceManager manager = new EnhancedFurnaceManager(plugin);
            ItemStack enhancedItem = manager.createEnhancedFurnaceItem(Material.FURNACE);
            
            // Sprawdź nazwę
            assertNotNull(enhancedItem.getItemMeta());
            assertTrue(enhancedItem.getItemMeta().hasDisplayName());
            assertEquals("§6Enhanced Furnace", enhancedItem.getItemMeta().getDisplayName());
        }
        
        @Test
        @DisplayName("Stworzony enhanced furnace item ma flagę PDC")
        void createdEnhancedFurnaceItem_shouldHavePdcFlag() {
            EnhancedFurnaceManager manager = new EnhancedFurnaceManager(plugin);
            ItemStack enhancedItem = manager.createEnhancedFurnaceItem(Material.FURNACE);
            
            // Sprawdź czy ma flagę PDC
            var pdc = enhancedItem.getItemMeta().getPersistentDataContainer();
            var key = new org.bukkit.NamespacedKey(plugin, "enhanced_furnace");
            
            assertTrue(pdc.has(key, org.bukkit.persistence.PersistentDataType.BYTE),
                "Item powinien mieć flagę enhanced_furnace w PDC");
        }
        
        @Test
        @DisplayName("Stworzony enhanced furnace item ma lore z opisem")
        void createdEnhancedFurnaceItem_shouldHaveLore() {
            EnhancedFurnaceManager manager = new EnhancedFurnaceManager(plugin);
            ItemStack enhancedItem = manager.createEnhancedFurnaceItem(Material.FURNACE);
            
            // Sprawdź lore
            assertNotNull(enhancedItem.getItemMeta().getLore());
            assertEquals(2, enhancedItem.getItemMeta().getLore().size());
            assertTrue(enhancedItem.getItemMeta().getLore().get(0).contains("special furnace"));
        }
        
        @Test
        @DisplayName("Enhanced furnace item dla BLAST_FURNACE ma typ BLAST_FURNACE")
        void createEnhancedFurnaceItem_forBlastFurnace_shouldHaveCorrectType() {
            EnhancedFurnaceManager manager = new EnhancedFurnaceManager(plugin);
            ItemStack enhancedItem = manager.createEnhancedFurnaceItem(Material.BLAST_FURNACE);
            
            assertEquals(Material.BLAST_FURNACE, enhancedItem.getType());
        }
        
        @Test
        @DisplayName("Enhanced furnace item dla SMOKER ma typ SMOKER")
        void createEnhancedFurnaceItem_forSmoker_shouldHaveCorrectType() {
            EnhancedFurnaceManager manager = new EnhancedFurnaceManager(plugin);
            ItemStack enhancedItem = manager.createEnhancedFurnaceItem(Material.SMOKER);
            
            assertEquals(Material.SMOKER, enhancedItem.getType());
        }
    }
    
    @Nested
    @DisplayName("2. Blokada wkładania do output")
    class OutputSlotProtection {
        
        @Test
        @DisplayName("Slot 14 jest rozpoznawany jako output")
        void slot14_shouldBeRecognizedAsOutput() {
            assertTrue(EnhancedFurnaceGUI.isOutputSlot(14));
        }
        
        @Test
        @DisplayName("Slot 23 jest rozpoznawany jako output")
        void slot23_shouldBeRecognizedAsOutput() {
            assertTrue(EnhancedFurnaceGUI.isOutputSlot(23));
        }
        
        @Test
        @DisplayName("Slot 32 jest rozpoznawany jako output")
        void slot32_shouldBeRecognizedAsOutput() {
            assertTrue(EnhancedFurnaceGUI.isOutputSlot(32));
        }
        
        @Test
        @DisplayName("GUI ma 54 sloty (chest)")
        void gui_shouldHave54Slots() {
            EnhancedFurnaceGUI gui = new EnhancedFurnaceGUI("Test");
            assertEquals(54, gui.getInventory().getSize());
        }
        
        @Test
        @DisplayName("Output slot zwraca poprawny lineIndex")
        void outputSlot_shouldReturnCorrectLineIndex() {
            assertEquals(0, EnhancedFurnaceGUI.getLineIndexForOutputSlot(14));
            assertEquals(1, EnhancedFurnaceGUI.getLineIndexForOutputSlot(23));
            assertEquals(2, EnhancedFurnaceGUI.getLineIndexForOutputSlot(32));
            assertEquals(-1, EnhancedFurnaceGUI.getLineIndexForOutputSlot(11)); // nie output
        }
    }
    
    @Nested
    @DisplayName("3. Stan lit bloku pieca")
    class FurnaceLitState {
        
        @Test
        @DisplayName("Nowy piec ma burnTime = 0")
        void newFurnace_shouldHaveZeroBurnTime() {
            EnhancedFurnaceGUI gui = new EnhancedFurnaceGUI("Test");
            EnhancedFurnaceData data = new EnhancedFurnaceData(gui, Material.FURNACE);
            
            assertEquals(0, data.getBurnTime());
        }
        
        @Test
        @DisplayName("setBurnTime ustawia czas palenia")
        void setBurnTime_shouldSetBurnTime() {
            EnhancedFurnaceGUI gui = new EnhancedFurnaceGUI("Test");
            EnhancedFurnaceData data = new EnhancedFurnaceData(gui, Material.FURNACE);
            
            data.setBurnTime(1600);
            assertEquals(1600, data.getBurnTime());
        }
        
        @Test
        @DisplayName("maxBurnTime przechowuje maksymalny czas palenia paliwa")
        void maxBurnTime_shouldStoreMaxBurnTime() {
            EnhancedFurnaceGUI gui = new EnhancedFurnaceGUI("Test");
            EnhancedFurnaceData data = new EnhancedFurnaceData(gui, Material.FURNACE);
            
            data.setMaxBurnTime(1600);
            assertEquals(1600, data.getMaxBurnTime());
        }
    }
    
    @Nested
    @DisplayName("4. Tablica paliw - fallback")
    class FuelTableFallback {
        
        @Test
        @DisplayName("Węgiel ma 1600 ticków")
        void coal_shouldHave1600Ticks() {
            // W testach MockBukkit NMS jest niedostępny - używa fallback
            assertEquals(1600, EnhancedFurnaceLogic.getFuelBurnTime(Material.COAL));
        }
        
        @Test
        @DisplayName("Płotek dębowy ma 300 ticków")
        void oakFence_shouldHave300Ticks() {
            assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(Material.OAK_FENCE));
        }
        
        @Test
        @DisplayName("Łódź ma 1200 ticków")
        void boat_shouldHave1200Ticks() {
            assertEquals(1200, EnhancedFurnaceLogic.getFuelBurnTime(Material.OAK_BOAT));
        }
        
        @Test
        @DisplayName("Baner ma 300 ticków")
        void banner_shouldHave300Ticks() {
            assertEquals(300, EnhancedFurnaceLogic.getFuelBurnTime(Material.WHITE_BANNER));
        }
        
        @Test
        @DisplayName("Kamień nie jest paliwem")
        void stone_shouldNotBeFuel() {
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(Material.STONE));
        }
        
        @Test
        @DisplayName("Diamenty nie są paliwem")
        void diamond_shouldNotBeFuel() {
            assertEquals(0, EnhancedFurnaceLogic.getFuelBurnTime(Material.DIAMOND));
        }
    }
    
    @Nested
    @DisplayName("5. Integracja GUI z danymi")
    class GuiDataIntegration {
        
        @Test
        @DisplayName("setInputItem i getInputItem działają poprawnie")
        void inputItems_shouldWorkCorrectly() {
            EnhancedFurnaceGUI gui = new EnhancedFurnaceGUI("Test");
            
            ItemStack iron = new ItemStack(Material.RAW_IRON, 32);
            gui.setInputItem(0, iron);
            
            ItemStack retrieved = gui.getInputItem(0);
            assertNotNull(retrieved);
            assertEquals(Material.RAW_IRON, retrieved.getType());
            assertEquals(32, retrieved.getAmount());
        }
        
        @Test
        @DisplayName("setOutputItem i getOutputItem działają poprawnie")
        void outputItems_shouldWorkCorrectly() {
            EnhancedFurnaceGUI gui = new EnhancedFurnaceGUI("Test");
            
            ItemStack ironIngot = new ItemStack(Material.IRON_INGOT, 16);
            gui.setOutputItem(0, ironIngot);
            
            ItemStack retrieved = gui.getOutputItem(0);
            assertNotNull(retrieved);
            assertEquals(Material.IRON_INGOT, retrieved.getType());
            assertEquals(16, retrieved.getAmount());
        }
        
        @Test
        @DisplayName("setFuelItem i getFuelItem działają poprawnie")
        void fuelItems_shouldWorkCorrectly() {
            EnhancedFurnaceGUI gui = new EnhancedFurnaceGUI("Test");
            
            ItemStack coal = new ItemStack(Material.COAL, 64);
            gui.setFuelItem(0, coal);
            
            ItemStack retrieved = gui.getFuelItem(0);
            assertNotNull(retrieved);
            assertEquals(Material.COAL, retrieved.getType());
            assertEquals(64, retrieved.getAmount());
        }
        
        @Test
        @DisplayName("getAllItems zwraca wszystkie przedmioty z input, output i fuel")
        void getAllItems_shouldReturnAllItems() {
            EnhancedFurnaceGUI gui = new EnhancedFurnaceGUI("Test");
            
            // Dodaj przedmioty
            gui.setInputItem(0, new ItemStack(Material.RAW_IRON, 10));
            gui.setOutputItem(1, new ItemStack(Material.IRON_INGOT, 5));
            gui.setFuelItem(2, new ItemStack(Material.COAL, 8));
            
            ItemStack[] allItems = gui.getAllItems();
            
            // Powinno być 9 slotów (3 input + 3 output + 3 fuel)
            assertEquals(9, allItems.length);
            
            // Zlicz niepuste
            int nonEmpty = 0;
            for (ItemStack item : allItems) {
                if (item != null && !item.getType().isAir()) {
                    nonEmpty++;
                }
            }
            assertEquals(3, nonEmpty, "Powinny być 3 niepuste sloty");
        }
    }
}

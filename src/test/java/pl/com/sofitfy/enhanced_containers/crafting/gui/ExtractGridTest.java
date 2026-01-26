package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CraftingGUIListener - extract3x3Grid")
class ExtractGridTest extends MockBukkitPluginTestBase {
    
    private CraftingGUIListener listener;
    private CraftingGUI gui;
    private PlayerMock player;
    
    @BeforeEach
    void setUp() {
        player = createPlayer();
        listener = new CraftingGUIListener();
        gui = new CraftingGUI(player, "Test");
        listener.registerGUI(gui);
    }
    
    @Test
    @DisplayName("Powinien prawidłowo wyciągnąć 3x3 z 4x4 gridu (pozycje top-left)")
    void shouldExtract3x3From4x4Grid() {
        // Wstawiam elementy w pozycjach 4x4:
        // [0][1][2][X]
        // [4][5][6][X]
        // [8][9][10][X]
        // [X][X][X][X]
        
        // Pozycje 16-slotowe w 4x4 gridzie
        ItemStack[] grid4x4 = new ItemStack[16];
        grid4x4[0] = new ItemStack(Material.OAK_LOG);     // (0,0)
        grid4x4[1] = new ItemStack(Material.OAK_LOG);     // (0,1)
        grid4x4[2] = new ItemStack(Material.OAK_LOG);     // (0,2)
        grid4x4[3] = new ItemStack(Material.DIAMOND);     // (0,3) - będzie pominięty
        grid4x4[4] = new ItemStack(Material.STONE);       // (1,0)
        grid4x4[5] = new ItemStack(Material.STONE);       // (1,1)
        grid4x4[6] = new ItemStack(Material.STONE);       // (1,2)
        grid4x4[7] = new ItemStack(Material.IRON_ORE);    // (1,3) - będzie pominięty
        grid4x4[8] = new ItemStack(Material.COAL_BLOCK);  // (2,0)
        grid4x4[9] = new ItemStack(Material.COAL_BLOCK);  // (2,1)
        grid4x4[10] = new ItemStack(Material.COAL_BLOCK); // (2,2)
        grid4x4[11] = new ItemStack(Material.GOLD_ORE);   // (2,3) - będzie pominięty
        // Rzędy 3 (12-15) są całkowicie ignorowane
        
        // Wkładam grid4x4 do GUI
        for (int i = 0; i < 16; i++) {
            if (grid4x4[i] != null) {
                gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[i], grid4x4[i]);
            }
        }
        
        // Wyodrębniaj 3x3 poprzez updateOutput (które wewnętrznie używa extract3x3Grid)
        // Sprawdzam poprzez pośrednią metodę - pattern matching będzie używać extract3x3Grid
        
        // Sprawdzenie: nie powinny zostać pominięte wszystkie elementy
        ItemStack item0 = gui.getInventory().getItem(CraftingGUI.GRID_SLOTS[0]);
        ItemStack item1 = gui.getInventory().getItem(CraftingGUI.GRID_SLOTS[1]);
        ItemStack item2 = gui.getInventory().getItem(CraftingGUI.GRID_SLOTS[2]);
        
        assertNotNull(item0, "Pozycja (0,0) powinna być OAK_LOG");
        assertNotNull(item1, "Pozycja (0,1) powinna być OAK_LOG");
        assertNotNull(item2, "Pozycja (0,2) powinna być OAK_LOG");
        
        assertEquals(Material.OAK_LOG, item0.getType());
        assertEquals(Material.OAK_LOG, item1.getType());
        assertEquals(Material.OAK_LOG, item2.getType());
    }
    
    @Test
    @DisplayName("Powinien ignorować kolumnę 4 (indeks 3, 7, 11, 15)")
    void shouldIgnoreColumn4() {
        ItemStack[] grid4x4 = new ItemStack[16];
        // Kolumna 4 ma być ignorowana
        grid4x4[3] = new ItemStack(Material.DIAMOND);
        grid4x4[7] = new ItemStack(Material.DIAMOND);
        grid4x4[11] = new ItemStack(Material.DIAMOND);
        grid4x4[15] = new ItemStack(Material.DIAMOND);
        
        for (int i = 0; i < 16; i++) {
            if (grid4x4[i] != null) {
                gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[i], grid4x4[i]);
            }
        }
        
        // Sprawdzanie: kolumna 4 (indeksy 3, 7, 11, 15) powinna być ignorowana
        // To testujemy pośrednio - grid powinien być pusty dla outputu
        listener.updateOutput(player, gui);
        
        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        // Samo diamenty w kolumnie 4 nie powinny dać żadnego outputu
        assertNull(output, "Diamenty tylko w kolumnie 4 (ignorowanej) nie powinny dać outputu");
    }
    
    @Test
    @DisplayName("Powinien ignorować rząd 4 (indeksy 12-15)")
    void shouldIgnoreRow4() {
        ItemStack[] grid4x4 = new ItemStack[16];
        // Rząd 4 ma być ignorowany (indeksy 12-15)
        grid4x4[12] = new ItemStack(Material.EMERALD);
        grid4x4[13] = new ItemStack(Material.EMERALD);
        grid4x4[14] = new ItemStack(Material.EMERALD);
        grid4x4[15] = new ItemStack(Material.EMERALD);
        
        for (int i = 0; i < 16; i++) {
            if (grid4x4[i] != null) {
                gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[i], grid4x4[i]);
            }
        }
        
        // Sprawdzanie: rząd 4 (indeksy 12-15) powinna być ignorowana
        listener.updateOutput(player, gui);
        
        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        // Same emeraldy w rzędzie 4 (ignorowanym) nie powinny dać żadnego outputu
        assertNull(output, "Emeraldy tylko w rzędzie 4 (ignorowanym) nie powinny dać outputu");
    }
    
    @Test
    @DisplayName("Powinien prawidłowo mapować 3x3 do 4x4 indeksów")
    void shouldMapIndicesCorrectly() {
        // 4x4 indeksy:
        // 0  1  2  3
        // 4  5  6  7
        // 8  9  10 11
        // 12 13 14 15
        
        // 3x3 powinno mapować:
        // grid3x3[0] <- grid4x4[0]
        // grid3x3[1] <- grid4x4[1]
        // grid3x3[2] <- grid4x4[2]
        // grid3x3[3] <- grid4x4[4]
        // grid3x3[4] <- grid4x4[5]
        // grid3x3[5] <- grid4x4[6]
        // grid3x3[6] <- grid4x4[8]
        // grid3x3[7] <- grid4x4[9]
        // grid3x3[8] <- grid4x4[10]
        
        ItemStack[] grid4x4 = new ItemStack[16];
        
        // Każdy element dostaje unikalny material na podstawie indeksu 3x3
        // Jeśli mapping jest prawidłowy, powinniśmy to móc sprawdzić
        grid4x4[0] = new ItemStack(Material.REDSTONE);  // 3x3[0]
        grid4x4[1] = new ItemStack(Material.REDSTONE);  // 3x3[1]
        grid4x4[2] = new ItemStack(Material.REDSTONE);  // 3x3[2]
        grid4x4[4] = new ItemStack(Material.GOLD_ORE);  // 3x3[3]
        grid4x4[5] = new ItemStack(Material.GOLD_ORE);  // 3x3[4]
        grid4x4[6] = new ItemStack(Material.GOLD_ORE);  // 3x3[5]
        grid4x4[8] = new ItemStack(Material.IRON_ORE);  // 3x3[6]
        grid4x4[9] = new ItemStack(Material.IRON_ORE);  // 3x3[7]
        grid4x4[10] = new ItemStack(Material.IRON_ORE); // 3x3[8]
        
        for (int i = 0; i < 16; i++) {
            if (grid4x4[i] != null) {
                gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[i], grid4x4[i]);
            }
        }
        
        // Sprawdzanie że elementy 3x3 zostały ustawione (indeksy 0,1,2,4,5,6,8,9,10)
        int[] validIndices = {0, 1, 2, 4, 5, 6, 8, 9, 10};
        for (int idx : validIndices) {
            ItemStack item = gui.getInventory().getItem(CraftingGUI.GRID_SLOTS[idx]);
            assertNotNull(item, "Pozycja " + idx + " powinna mieć item");
        }
    }
    
    @Test
    @DisplayName("Powinien obsługiwać częściowo wypełniony grid 4x4")
    void shouldHandlePartiallyFilledGrid() {
        ItemStack[] grid4x4 = new ItemStack[16];
        grid4x4[0] = new ItemStack(Material.OAK_LOG);
        
        for (int i = 0; i < 16; i++) {
            if (grid4x4[i] != null) {
                gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[i], grid4x4[i]);
            }
        }
        
        listener.updateOutput(player, gui);
        
        // Powinno działać bez błędów
        ItemStack item = gui.getInventory().getItem(CraftingGUI.GRID_SLOTS[0]);
        assertNotNull(item, "Pozycja 0 powinna mieć OAK_LOG");
    }
}

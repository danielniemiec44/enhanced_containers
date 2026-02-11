package pl.com.sofitfy.enhanced_containers.crafting.vanilla;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import pl.com.sofitfy.enhanced_containers.EnhancedCraftingMain;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne dla VanillaRecipeMatcher z MockBukkit.
 * 
 * Każdy test:
 * 1. Inicjalizuje serwer MockBukkit
 * 2. Tworzy instancję matchera
 * 3. Przygotowuje matrycę ItemStack[16] (4x4 GUI)
 * 4. Włożem itemy w konkretne sloty
 * 5. Wywołuje matcher.match() i sprawdza wynik
 */
@DisplayName("Testy integracyjne VanillaRecipeMatcher z MockBukkit")
class VanillaMatcherIntegrationTest {
    
    private ServerMock server;
    private EnhancedCraftingMain plugin;
    private World world;
    private Player player;
    private VanillaRecipeMatcher matcher;
    
    /**
     * Inicjalizacja przed każdym testem.
     * - Uruchamia MockBukkit
     * - Ładuje plugin EnhancedCraftingMain
     * - Dodaje świat i gracza
     * - Tworzy instancję matchera
     */
    @BeforeEach
    void setUp() {
        // Inicjalizuj serwer MockBukkit
        this.server = MockBukkit.mock();
        
        // Załaduj plugin EnhancedCraftingMain (aby receptury były dostępne)
        this.plugin = MockBukkit.load(EnhancedCraftingMain.class);
        
        // Dodaj świat do serwera
        this.world = server.addSimpleWorld("test_world");
        
        // Dodaj gracza
        this.player = server.addPlayer();
        
        // Utwórz instancję matchera
        this.matcher = new VanillaRecipeMatcher();
    }
    
    /**
     * Czyszczenie po każdym teście.
     */
    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }
    
    /**
     * TEST 1: Sprawdzenie algorytmu Shrink Wrap
     * 
     * Scenariusz:
     * - Włóż 1x OAK_LOG do slotu 15 (prawy dolny róg macierzy 4x4)
     * - Algorytm powinien przesunąć go do pozycji 0,0 w shrunken grid
     * - Paper API powinno znaleźć recepturę 1x1: drewno -> deski
     * 
     * Oczekiwany wynik: ItemStack z Material.OAK_PLANKS i ilością 4
     */
    @Test
    @DisplayName("Shrink Wrap: drewno w rogu -> deski")
    void testShrinkWrapLogic() {
        // Przygotuj matrycę 4x4 (16 slotów)
        ItemStack[] grid4x4 = new ItemStack[16];
        initializeEmpty(grid4x4);
        
        // Włóż 1x OAK_LOG do slotu 15 (prawy dolny róg)
        grid4x4[15] = new ItemStack(Material.OAK_LOG, 1);
        
        // Wywołaj matcher
        ItemStack result = matcher.match(grid4x4, player);
        
        // Asercje
        assertNotNull(result, "Wynik powinien być nie null");
        assertEquals(Material.OAK_PLANKS, result.getType(), 
            "Drewno powinno dać deski");
        assertEquals(4, result.getAmount(), 
            "1x drewno daje 4x deski");
    }
    
    /**
     * TEST 2: Sprawdzenie szukania Multiple-craft (shapeless) receptur
     * 
     * Scenariusz:
     * - Vanilla receptura Chlebka (BREAD) z 3x WHEAT (shapeless)
     * - Sprawdzamy czy matcher znajduje receptury bez konkretnego schematu
     * 
     * Receptura Chlebka (Shapeless):
     * 3x WHEAT (w dowolnych slotach) -> 1x BREAD
     * 
     * Oczekiwany wynik: BREAD
     */
    @Test
    @DisplayName("Receptura bezszablonowa: 3x ziarno -> chleb")
    void testTagsSupport() {
        // Przygotuj matrycę 4x4
        ItemStack[] grid4x4 = new ItemStack[16];
        initializeEmpty(grid4x4);
        
        // Ułóż recepturę Chlebka (3x WHEAT w dowolnych slotach):
        grid4x4[0] = new ItemStack(Material.WHEAT, 1);
        grid4x4[1] = new ItemStack(Material.WHEAT, 1);
        grid4x4[2] = new ItemStack(Material.WHEAT, 1);
        // Reszta slotów to AIR
        
        // Wywołaj matcher
        ItemStack result = matcher.match(grid4x4, player);
        
        // Asercje
        assertNotNull(result, "Chleb powinien być znaleziony");
        assertEquals(Material.BREAD, result.getType(), 
            "3x ziarno daje chleb");
    }
    
    /**
     * TEST 3: Sprawdzenie receptur bardziej złożonych
     * 
     * Scenariusz:
     * - Receptura na Papier (PAPER) z 3x SUGARCANE (shapeless)
     * - Sprawdzamy czy matcher obsługuje różne warianty receptur
     * 
     * Oczekiwany wynik: PAPER
     */
    @Test
    @DisplayName("Receptura bardziej złożona: 3x cukier trzcinowy -> papier")
    void testComplexRecipe() {
        // Przygotuj matrycę 4x4
        ItemStack[] grid4x4 = new ItemStack[16];
        initializeEmpty(grid4x4);
        
        // Ułóż recepturę Papieru (3x SUGARCANE w dowolnych slotach):
        grid4x4[0] = new ItemStack(Material.SUGAR_CANE, 1);
        grid4x4[1] = new ItemStack(Material.SUGAR_CANE, 1);
        grid4x4[2] = new ItemStack(Material.SUGAR_CANE, 1);
        // Reszta slotów to AIR
        
        // Wywołaj matcher
        ItemStack result = matcher.match(grid4x4, player);
        
        // Asercje
        assertNotNull(result, "Papier powinien być znaleziony");
        assertEquals(Material.PAPER, result.getType(), 
            "3x cukier trzcinowy daje papier");
        assertEquals(3, result.getAmount(), 
            "3x cukier trzcinowy daje 3x papier");
    }
    
    /**
     * TEST 4: Sprawdzenie mechaniki naprawy
     * 
     * Scenariusz:
     * - Utwórz 2x STONE_SWORD ze 100 linią wytrzymałości (max durability)
     * - Ustaw im damage(50) - są zużyte w połowie
     * - Włóż je do macierzy
     * - Matcher powinien:
     *   1. Nie znaleźć vanilla receptury (nie ma receptury na naprawę miecza)
     *   2. Aktywować fallback naprawy
     *   3. Zwrócić 1x STONE_SWORD z mniejszym damage (naprawiony)
     * 
     * Formuła naprawy:
     * durability = (max - damage1) + (max - damage2) + (0.05 * max)
     * durability = (100 - 50) + (100 - 50) + (0.05 * 100)
     * durability = 50 + 50 + 5 = 105
     * damage = max - durability = 100 - 105 = -5 (capped to 0)
     * 
     * Oczekiwany wynik:
     * - Material: STONE_SWORD
     * - Amount: 1
     * - Damage: znacznie mniejszy niż 50 (naprawiony)
     */
    @Test
    @DisplayName("Mechanika naprawy: 2x uszkodzony miecz -> naprawiony")
    void testRepairMechanic() {
        // Przygotuj matrycę 4x4
        ItemStack[] grid4x4 = new ItemStack[16];
        initializeEmpty(grid4x4);
        
        // Utwórz pierwszy miecz (zużyty)
        ItemStack sword1 = new ItemStack(Material.STONE_SWORD, 1);
        Damageable damageable1 = (Damageable) sword1.getItemMeta();
        if (damageable1 != null) {
            damageable1.setDamage(50);
            sword1.setItemMeta(damageable1);
        }
        grid4x4[0] = sword1;
        
        // Utwórz drugi miecz (również zużyty)
        ItemStack sword2 = new ItemStack(Material.STONE_SWORD, 1);
        Damageable damageable2 = (Damageable) sword2.getItemMeta();
        if (damageable2 != null) {
            damageable2.setDamage(50);
            sword2.setItemMeta(damageable2);
        }
        grid4x4[1] = sword2;
        
        // Wywołaj matcher
        ItemStack result = matcher.match(grid4x4, player);
        
        // Asercje
        assertNotNull(result, "Wynik naprawy powinien być nie null");
        assertEquals(Material.STONE_SWORD, result.getType(), 
            "Wynik powinien być mieczem");
        assertEquals(1, result.getAmount(), 
            "Napraw daje 1x miecz");
        
        // Sprawdź czy miecz został naprawiony (damage < 50)
        ItemMeta resultMeta = result.getItemMeta();
        assertNotNull(resultMeta, "ItemMeta wyniku powinna istnieć");
        assertTrue(resultMeta instanceof Damageable, 
            "Meta wyniku powinna być Damageable");
        
        Damageable repairMeta = (Damageable) resultMeta;
        int resultDamage = repairMeta.getDamage();
        
        assertTrue(resultDamage < 50, 
            "Damage naprawionego miecza powinien być mniejszy niż 50");
        assertTrue(resultDamage >= 0, 
            "Damage nie może być ujemny");
    }
    
    // ==================== METODY POMOCNICZE ====================
    
    /**
     * Inicjalizuj matrycę 4x4 - wszystkie sloty to AIR.
     */
    private void initializeEmpty(ItemStack[] grid4x4) {
        for (int i = 0; i < 16; i++) {
            grid4x4[i] = new ItemStack(Material.AIR);
        }
    }
}

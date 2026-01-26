package pl.com.sofitfy.enhanced_containers.crafting.vanilla;

import org.bukkit.Material;
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
import pl.com.sofitfy.enhanced_containers.Main;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne dla mechaniki naprawy - z MockBukkit.
 * 
 * Te testy uruchamiają prawdziwy serwer MockBukkit, ładują plugin,
 * i testują mechanikę naprawy w pełnym environment.
 * 
 * Komentarze po polsku, prosty język.
 */
@DisplayName("Testy integracyjne - Mechanika naprawy z MockBukkit")
class VanillaRepairMechanicsIT {
    
    private ServerMock server;
    private Main plugin;
    private Player player;
    private VanillaRecipeMatcher matcher;
    
    @BeforeEach
    void setUp() {
        // Inicjalizuj serwer MockBukkit
        this.server = MockBukkit.mock();
        
        // Załaduj plugin Main
        this.plugin = MockBukkit.load(Main.class);
        
        // Dodaj gracza
        this.player = server.addPlayer();
        
        // Stwórz matcher
        this.matcher = new VanillaRecipeMatcher();
    }
    
    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }
    
    /**
     * Helperowa metoda - tworzy ItemStack z określonym damage.
     */
    private ItemStack createDamagedItem(Material type, int damage) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        
        if (meta instanceof Damageable) {
            ((Damageable) meta).setDamage(damage);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Helperowa metoda - odczytuje damage z ItemStack'a.
     */
    private int getDamage(ItemStack item) {
        if (item == null) {
            return -1;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable)) {
            return -1;
        }
        
        return ((Damageable) meta).getDamage();
    }
    
    /**
     * TEST 1: Naprawianie dwóch lekko zużytych motyk.
     * 
     * KLUCZOWY TEST z user requesta!
     * 
     * Scenariusz:
     * - Tworzymy 2 DIAMOND_PICKAXE po 10% worn (damage = 156 każdy, max = 1561)
     * - Włożymy je do macierzy 4x4 (sloty 0 i 1)
     * - Matcher powinien zwrócić naprawioną motykę z damage = 0 (idealnie nową)
     * 
     * Oczekiwany wynik:
     * - Material: DIAMOND_PICKAXE
     * - Amount: 1
     * - Damage: 0 (idealnie nowa, bez paska zużycia)
     */
    @Test
    @DisplayName("Naprawianie: dwie motyki 10% worn -> idealnie nowa (damage=0)")
    void repairTwoPickaxes10PercentWorn() {
        // Przygotuj matrycę 4x4
        ItemStack[] grid4x4 = new ItemStack[16];
        for (int i = 0; i < 16; i++) {
            grid4x4[i] = new ItemStack(Material.AIR);
        }
        
        // Utwórz dwie motyki 10% worn (lekko zużyte) - ALE Z RÓŻNYM DAMAGE!
        // To realistyczne warunki z gry: masz 2 motyki, ale każda ma inny stopień zużycia
        int maxDur = Material.DIAMOND_PICKAXE.getMaxDurability();
        int tenPercentDamage = (int) (maxDur * 0.1);
        int fifteenPercentDamage = (int) (maxDur * 0.15);
        
        ItemStack pickaxe1 = createDamagedItem(Material.DIAMOND_PICKAXE, tenPercentDamage);
        ItemStack pickaxe2 = createDamagedItem(Material.DIAMOND_PICKAXE, fifteenPercentDamage);
        
        // Weryfikuj że są zużyte - i że mają RÓŻNY damage (realistycznie!)
        assertEquals(tenPercentDamage, getDamage(pickaxe1),
            "Pickaxe1 musi mieć damage = 10%");
        assertEquals(fifteenPercentDamage, getDamage(pickaxe2),
            "Pickaxe2 musi mieć damage = 15% (INNY niż pickaxe1!)");
        assertTrue(getDamage(pickaxe1) != getDamage(pickaxe2),
            "KRYTYCZNE: Przedmioty muszą mieć RÓŻNY damage (bo w grze tak jest)");
        
        // Włóż do macierzy
        grid4x4[0] = pickaxe1;
        grid4x4[1] = pickaxe2;
        
        // Wywołaj matcher (powinien znaleźć naprawę jako fallback)
        ItemStack result = matcher.match(grid4x4, player);
        
        // Asercje
        assertNotNull(result, "Wynik powinien być nie null");
        assertEquals(Material.DIAMOND_PICKAXE, result.getType(),
            "Wynik powinien być motyka");
        assertEquals(1, result.getAmount(),
            "Wynik powinien być 1 sztuka");
        
        // KLUCZOWA ASERCJA: damage musi być 0!
        int resultDamage = getDamage(result);
        assertEquals(0, resultDamage,
            "Naprawiona motyka musi mieć damage=0 (idealnie nowa, bez paska)");
        
        assertTrue(resultDamage < tenPercentDamage,
            "Naprawiona motyka musi być lepsza niż wejście");
    }
    
    /**
     * TEST 2: Naprawianie dwóch średnio zużytych mieczy.
     * 
     * Scenariusz:
     * - Tworzymy 2 IRON_SWORD po 50% worn
     * - Matcher powinien zwrócić naprawiony miecz
     * 
     * Oczekiwany wynik:
     * - damage < 50% (powinien być prawie 0 lub ~25%)
     */
    @Test
    @DisplayName("Naprawianie: dwie miecze 50% worn -> prawie nowe")
    void repairTwoSwords50PercentWorn() {
        // Przygotuj matrycę 4x4
        ItemStack[] grid4x4 = new ItemStack[16];
        for (int i = 0; i < 16; i++) {
            grid4x4[i] = new ItemStack(Material.AIR);
        }
        
        // Utwórz dwie miecze 50% worn - ALE Z RÓŻNYM DAMAGE!
        int maxDur = Material.IRON_SWORD.getMaxDurability();
        int fiftyPercentDamage = maxDur / 2;
        int fiftyTwoPercentDamage = (int) (maxDur * 0.52);
        
        ItemStack sword1 = createDamagedItem(Material.IRON_SWORD, fiftyPercentDamage);
        ItemStack sword2 = createDamagedItem(Material.IRON_SWORD, fiftyTwoPercentDamage);
        
        // Włóż do macierzy
        grid4x4[0] = sword1;
        grid4x4[1] = sword2;
        
        // Wywołaj matcher
        ItemStack result = matcher.match(grid4x4, player);
        
        // Asercje
        assertNotNull(result, "Wynik powinien być nie null");
        assertEquals(Material.IRON_SWORD, result.getType(),
            "Wynik powinien być miecz");
        
        int resultDamage = getDamage(result);
        assertTrue(resultDamage <= fiftyPercentDamage,
            "Naprawiony miecz musi mieć mniejszy lub równy damage");
        
        // Po naprawie powinien być prawie nowy (wzór: 0.05*maxDur dodatkowy life)
        // life1 = 50%, life2 = 50%, bonus = 5% -> newLife = 105% clamped = 100%
        // Więc resultDamage powinien być 0
        assertEquals(0, resultDamage,
            "Dwie miecze 50% worn powinny dać prawie nowy (damage=0)");
    }
    
    /**
     * TEST 3: Naprawianie dwóch bardzo zużytych przedmiotów.
     * 
     * Scenariusz:
     * - Tworzymy 2 IRON_PICKAXE po 95% worn (prawie zniszczone)
     * - Matcher powinien zwrócić naprawiony, ale wciąż średnio zużyty
     * 
     * Oczekiwany wynik:
     * - damage ~ 85% (lepszy niż 95%)
     */
    @Test
    @DisplayName("Naprawianie: dwie motyki 95% worn -> damage~85% (lepsze)")
    void repairTwoPickaxes95PercentWorn() {
        // Przygotuj matrycę 4x4
        ItemStack[] grid4x4 = new ItemStack[16];
        for (int i = 0; i < 16; i++) {
            grid4x4[i] = new ItemStack(Material.AIR);
        }
        
        // Utwórz dwie motyki 95% worn (prawie zniszczone) - ALE Z RÓŻNYM DAMAGE!
        int maxDur = Material.IRON_PICKAXE.getMaxDurability();
        int ninetyFivePercentDamage = (int) (maxDur * 0.95);
        int ninetySevenPercentDamage = (int) (maxDur * 0.97);
        
        ItemStack pickaxe1 = createDamagedItem(Material.IRON_PICKAXE, ninetyFivePercentDamage);
        ItemStack pickaxe2 = createDamagedItem(Material.IRON_PICKAXE, ninetySevenPercentDamage);
        
        // Włóż do macierzy
        grid4x4[0] = pickaxe1;
        grid4x4[1] = pickaxe2;
        
        // Wywołaj matcher
        ItemStack result = matcher.match(grid4x4, player);
        
        // Asercje
        assertNotNull(result, "Wynik powinien być nie null");
        assertEquals(Material.IRON_PICKAXE, result.getType(),
            "Wynik powinien być motyka");
        
        int resultDamage = getDamage(result);
        assertTrue(resultDamage < ninetyFivePercentDamage,
            "Naprawiona motyka musi być lepsza niż wejście (95% worn)");
        
        // Oczekiwany damage ~= 85% maxDur
        int expectedDamage = (int) (maxDur * 0.85);
        assertTrue(Math.abs(resultDamage - expectedDamage) < 50,
            "Naprawiona motyka powinna mieć damage ~ 85% (tolerancja 50)");
    }
    
    /**
     * TEST 4: KRYTYCZNY - Sprawdzenie że damage nigdy nie jest ujemny.
     * 
     * Scenariusz:
     * - Tworzymy 2 bardzo nowe miecze (5% worn)
     * - Bez CLAMPU bylibyśmy z ujemnym damage!
     * - Z CLAMPEM powinien być 0
     * 
     * To weryfikuje że nasz fix działa idealnie.
     */
    @Test
    @DisplayName("KRYTYCZNY: dwie miecze 5% worn -> damage=0 (bez ujemnych!)")
    void repairTwoSwords5PercentWornNEGATIVE_DAMAGE_CLAMP() {
        // Przygotuj matrycę 4x4
        ItemStack[] grid4x4 = new ItemStack[16];
        for (int i = 0; i < 16; i++) {
            grid4x4[i] = new ItemStack(Material.AIR);
        }
        
        // Utwórz dwie miecze 5% worn (prawie nowe)
        int maxDur = Material.DIAMOND_SWORD.getMaxDurability();
        int fivePercentDamage = (int) (maxDur * 0.05);
        
        ItemStack sword1 = createDamagedItem(Material.DIAMOND_SWORD, fivePercentDamage);
        ItemStack sword2 = createDamagedItem(Material.DIAMOND_SWORD, fivePercentDamage);
        
        // Włóż do macierzy
        grid4x4[0] = sword1;
        grid4x4[1] = sword2;
        
        // Wywołaj matcher
        ItemStack result = matcher.match(grid4x4, player);
        
        // Asercje
        assertNotNull(result, "Wynik powinien być nie null");
        assertEquals(Material.DIAMOND_SWORD, result.getType(),
            "Wynik powinien być miecz");
        
        int resultDamage = getDamage(result);
        
        // KLUCZOWE ASERCJE
        assertTrue(resultDamage >= 0,
            "Damage NIGDY nie może być ujemny! To by oznaczało błąd w logice.");
        assertEquals(0, resultDamage,
            "Naprawiony miecz z 5% worn powinien mieć damage=0");
    }
    
    /**
     * TEST 5: Brak naprawy dla różnych typów przedmiotów.
     * 
     * Scenariusz:
     * - Włóż DIAMOND_PICKAXE + IRON_PICKAXE (różne typy)
     * - Matcher nie powinien znaleźć naprawy
     * 
     * Oczekiwany wynik: null (brak naprawy)
     */
    @Test
    @DisplayName("Brak naprawy: różne typy przedmiotów")
    void noRepairDifferentTypes() {
        // Przygotuj matrycę 4x4
        ItemStack[] grid4x4 = new ItemStack[16];
        for (int i = 0; i < 16; i++) {
            grid4x4[i] = new ItemStack(Material.AIR);
        }
        
        // Utwórz przedmioty różnych typów
        int maxDur = Material.DIAMOND_PICKAXE.getMaxDurability();
        int damage = (int) (maxDur * 0.5);
        
        ItemStack diamondPick = createDamagedItem(Material.DIAMOND_PICKAXE, damage);
        ItemStack ironPick = createDamagedItem(Material.IRON_PICKAXE, damage);
        
        // Włóż do macierzy
        grid4x4[0] = diamondPick;
        grid4x4[1] = ironPick;
        
        // Wywołaj matcher
        ItemStack result = matcher.match(grid4x4, player);
        
        // Asercje - nie powinna być naprawy
        assertNull(result,
            "Nie powinna być naprawy dla różnych typów przedmiotów");
    }
    
    /**
     * TEST 6: "PANCERNY TEST" - Ultra skrajny przypadek
     * 
     * Scenariusz:
     * - Dwa prawie nowe przedmioty (zużyte tylko w 1 punkcie damage)
     * - life1 = maxDur - 1, life2 = maxDur - 1
     * - Razem: life1 + life2 + bonus = (maxDur-1) + (maxDur-1) + 0.05*maxDur
     * - Może być >= maxDur, co bez CLAMPu dałoby ujemny damage!
     * 
     * Oczekiwany wynik:
     * - Przedmiot idealnie nowy (damage=0)
     * - BEZ ujemnego damage
     * - BEZ przeznaczenia ponad maxDur
     * 
     * To jest "test pancerny" - jeśli przejdzie, matematyka jest kuloodporna.
     */
    @Test
    @DisplayName("PANCERNY TEST: dwie hoe 1 damage każdy -> damage=0 (bez ujemnych!)")
    void testOverRepairUltraWrednyCaseTwoPerfectItems() {
        // Przygotuj matrycę 4x4
        ItemStack[] grid4x4 = new ItemStack[16];
        for (int i = 0; i < 16; i++) {
            grid4x4[i] = new ItemStack(Material.AIR);
        }
        
        // Utwórz dwie HOE prawie nowe (1 punkt zużycia każdy)
        // To symuluje sytuację gdzie naprawianie daje "za dużo" durability
        ItemStack hoe1 = new ItemStack(Material.DIAMOND_HOE);
        ItemMeta meta1 = hoe1.getItemMeta();
        ((Damageable) meta1).setDamage(1); // Prawie idealnie nowa
        hoe1.setItemMeta(meta1);
        
        ItemStack hoe2 = hoe1.clone(); // Druga taka sama
        
        // Weryfikuj że są zużyte
        assertEquals(1, getDamage(hoe1),
            "Hoe1 musi mieć damage = 1");
        assertEquals(1, getDamage(hoe2),
            "Hoe2 musi mieć damage = 1");
        
        // Włóż do macierzy
        grid4x4[0] = hoe1;
        grid4x4[1] = hoe2;
        
        // Wywołaj matcher
        ItemStack result = matcher.match(grid4x4, player);
        
        // Asercje
        assertNotNull(result, "Wynik powinien być nie null");
        assertEquals(Material.DIAMOND_HOE, result.getType(),
            "Wynik powinien być hoe");
        assertEquals(1, result.getAmount(),
            "Wynik powinien być 1 sztuka");
        
        // KLUCZOWE ASERCJE - to jest "pancerny test"
        int resultDamage = getDamage(result);
        
        // 1. Damage nigdy nie może być ujemny
        assertTrue(resultDamage >= 0,
            "PANCERNY TEST: Damage NIGDY nie może być ujemny! Wskazuje błąd w logice.");
        
        // 2. Powinien być 0 (całkowicie naprawiony)
        assertEquals(0, resultDamage,
            "PANCERNY TEST: Dwie hoe z 1 damage każdy powinny dać idealnie nową (damage=0)");
        
        // 3. Potwierdź że to lepsze niż wejście
        assertTrue(resultDamage < 1,
            "Naprawiona hoe musi być lepsza niż wejście (1 damage)");
    }
}

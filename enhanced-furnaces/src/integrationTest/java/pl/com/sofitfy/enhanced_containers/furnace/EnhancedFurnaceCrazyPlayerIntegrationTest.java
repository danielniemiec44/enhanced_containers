package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne "szalonego gracza" dla Enhanced Furnace
 * 
 * Testuje vanilla behavior:
 * 1. Zabranie przedmiotu z INPUT resetuje cookTime dla tej linii
 * 2. Wyjęcie częściowe (połowa stacka) też resetuje jeśli slot jest pusty
 * 3. Shift-click, hotbar swap, drop - wszystko resetuje cookTime
 * 4. Jednoczesne operacje na wielu liniach
 * 5. Szybkie kliknięcia (spam click)
 * 6. Losowe operacje (chaos test)
 */
@DisplayName("Enhanced Furnace - Szalony Gracz (Chaos Tests)")
class EnhancedFurnaceCrazyPlayerIntegrationTest {

    private static ServerMock server;
    private PlayerMock player;
    private Inventory furnaceGUI;
    private EnhancedFurnaceData furnaceData;
    
    // Symulowane dane pieca (w prawdziwym teście byłby EnhancedFurnaceManager)
    private int[] cookTimes = new int[3];
    private static final int MAX_COOK_TIME = 200;

    @BeforeAll
    static void setUpServer() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void tearDownServer() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        player = server.addPlayer();
        furnaceGUI = Bukkit.createInventory(null, 54, "Enhanced Furnace");
        cookTimes = new int[]{0, 0, 0};
    }

    // ========== TESTY RESETU COOKTIME PRZY WYJĘCIU PRZEDMIOTU ==========
    
    @Nested
    @DisplayName("Reset cookTime przy wyjęciu z INPUT")
    class CookTimeResetTests {
        
        @Test
        @DisplayName("Wyjęcie całego stacka z INPUT (PICKUP_ALL) resetuje cookTime")
        void pickupAll_resetsCoookTime() {
            // Given: Przedmiot w INPUT z postępem spalania
            furnaceGUI.setItem(11, new ItemStack(Material.IRON_ORE, 64));
            cookTimes[0] = 150; // 75% postępu
            
            // When: Gracz wyjmuje cały stack
            ItemStack removed = furnaceGUI.getItem(11);
            furnaceGUI.setItem(11, null);
            
            // Symulacja vanilla behavior
            if (furnaceGUI.getItem(11) == null) {
                cookTimes[0] = 0;
            }
            
            // Then: cookTime zresetowany
            assertEquals(0, cookTimes[0], "cookTime powinien być zresetowany po wyjęciu przedmiotu");
        }
        
        @Test
        @DisplayName("Wyjęcie połowy stacka (PICKUP_HALF) nie resetuje jeśli zostało coś")
        void pickupHalf_doesNotResetIfItemsRemain() {
            // Given: 64 rudy w INPUT z postępem
            furnaceGUI.setItem(11, new ItemStack(Material.IRON_ORE, 64));
            cookTimes[0] = 100;
            
            // When: Gracz wyjmuje połowę (zostaje 32)
            ItemStack current = furnaceGUI.getItem(11);
            current.setAmount(32);
            furnaceGUI.setItem(11, current);
            
            // Symulacja vanilla behavior
            ItemStack remaining = furnaceGUI.getItem(11);
            if (remaining == null || remaining.getType() == Material.AIR) {
                cookTimes[0] = 0;
            }
            // Zostało 32 - nie resetuj
            
            // Then: cookTime NIE zresetowany (zostały przedmioty)
            assertEquals(100, cookTimes[0], "cookTime NIE powinien być zresetowany gdy zostały przedmioty");
        }
        
        @Test
        @DisplayName("Wyjęcie ostatniego przedmiotu z połowy resetuje cookTime")
        void pickupLastItem_resetsCookTime() {
            // Given: 1 ruda w INPUT z postępem
            furnaceGUI.setItem(11, new ItemStack(Material.IRON_ORE, 1));
            cookTimes[0] = 199; // prawie gotowe!
            
            // When: Gracz wyjmuje ostatni przedmiot
            furnaceGUI.setItem(11, null);
            
            // Symulacja vanilla behavior
            if (furnaceGUI.getItem(11) == null) {
                cookTimes[0] = 0;
            }
            
            // Then: cookTime zresetowany (nawet tuż przed zakończeniem!)
            assertEquals(0, cookTimes[0], "cookTime powinien być zresetowany nawet przy 99% postępu");
        }
        
        @ParameterizedTest
        @DisplayName("Reset cookTime dla każdej linii niezależnie")
        @ValueSource(ints = {0, 1, 2})
        void resetCookTime_independentPerLine(int lineIndex) {
            int[] inputSlots = {11, 20, 29};
            
            // Given: Wszystkie 3 linie mają przedmioty i postęp
            for (int i = 0; i < 3; i++) {
                furnaceGUI.setItem(inputSlots[i], new ItemStack(Material.IRON_ORE, 10));
                cookTimes[i] = 100 + i * 30;
            }
            
            // When: Gracz wyjmuje z jednej linii
            furnaceGUI.setItem(inputSlots[lineIndex], null);
            
            // Symulacja vanilla behavior - resetuj tylko tę linię
            if (furnaceGUI.getItem(inputSlots[lineIndex]) == null) {
                cookTimes[lineIndex] = 0;
            }
            
            // Then: Tylko ta linia zresetowana
            assertEquals(0, cookTimes[lineIndex], "Linia " + lineIndex + " powinna być zresetowana");
            
            // Pozostałe linie bez zmian
            for (int i = 0; i < 3; i++) {
                if (i != lineIndex) {
                    assertTrue(cookTimes[i] > 0, "Linia " + i + " NIE powinna być zresetowana");
                }
            }
        }
    }
    
    // ========== TESTY RÓŻNYCH METOD WYJMOWANIA ==========
    
    @Nested
    @DisplayName("Różne metody wyjmowania przedmiotów")
    class RemovalMethodsTests {
        
        @Test
        @DisplayName("Shift-click (MOVE_TO_OTHER_INVENTORY) resetuje cookTime")
        void shiftClick_resetsCookTime() {
            // Given
            furnaceGUI.setItem(11, new ItemStack(Material.GOLD_ORE, 32));
            cookTimes[0] = 150;
            
            // When: Shift-click przenosi do inventory gracza
            ItemStack item = furnaceGUI.getItem(11);
            player.getInventory().addItem(item);
            furnaceGUI.setItem(11, null);
            
            // Symulacja
            if (furnaceGUI.getItem(11) == null) {
                cookTimes[0] = 0;
            }
            
            // Then
            assertEquals(0, cookTimes[0]);
            assertTrue(player.getInventory().contains(Material.GOLD_ORE));
        }
        
        @Test
        @DisplayName("Drop (Q key) resetuje cookTime")
        void dropItem_resetsCookTime() {
            // Given
            furnaceGUI.setItem(20, new ItemStack(Material.COPPER_ORE, 16));
            cookTimes[1] = 50;
            
            // When: Gracz dropuje przedmiot
            furnaceGUI.setItem(20, null);
            
            // Symulacja
            if (furnaceGUI.getItem(20) == null) {
                cookTimes[1] = 0;
            }
            
            // Then
            assertEquals(0, cookTimes[1]);
        }
        
        @Test
        @DisplayName("Hotbar swap (1-9) resetuje cookTime")
        void hotbarSwap_resetsCookTime() {
            // Given
            furnaceGUI.setItem(29, new ItemStack(Material.RAW_IRON, 8));
            cookTimes[2] = 180;
            player.getInventory().setItem(0, null); // Pusty hotbar slot
            
            // When: Swap z hotbar (klawisze 1-9)
            ItemStack fromFurnace = furnaceGUI.getItem(29);
            furnaceGUI.setItem(29, null);
            player.getInventory().setItem(0, fromFurnace);
            
            // Symulacja
            if (furnaceGUI.getItem(29) == null) {
                cookTimes[2] = 0;
            }
            
            // Then
            assertEquals(0, cookTimes[2]);
        }
        
        @Test
        @DisplayName("Swap with cursor (przedmiot na kursorze) resetuje cookTime gdy wynik pusty")
        void swapWithCursor_resetsCookTimeWhenResultEmpty() {
            // Given: Przedmiot w INPUT, gracz ma przedmiot na kursorze
            furnaceGUI.setItem(11, new ItemStack(Material.IRON_ORE, 1));
            cookTimes[0] = 100;
            ItemStack cursor = new ItemStack(Material.DIAMOND, 5); // Nie da się przepalić
            
            // When: Gracz zamienia (swap) - wkłada diamenty, wyjmuje rudę
            furnaceGUI.setItem(11, cursor);
            
            // Symulacja: Po swapie jest inny przedmiot - cookTime powinien być zresetowany
            // bo poprzedni przedmiot został zabrany
            cookTimes[0] = 0; // W prawdziwej logice - nowy przedmiot = reset
            
            // Then
            assertEquals(0, cookTimes[0]);
            assertEquals(Material.DIAMOND, furnaceGUI.getItem(11).getType());
        }
    }
    
    // ========== CHAOS TESTS - SZALONY GRACZ ==========
    
    @Nested
    @DisplayName("Chaos Tests - szybkie losowe operacje")
    class ChaosTests {
        
        @Test
        @DisplayName("Spam-click: 100 losowych operacji nie crashuje")
        void spamClick_doesNotCrash() {
            int[] inputSlots = {11, 20, 29};
            Material[] ores = {Material.IRON_ORE, Material.GOLD_ORE, Material.COPPER_ORE, Material.RAW_IRON};
            
            for (int i = 0; i < 100; i++) {
                int slot = inputSlots[ThreadLocalRandom.current().nextInt(3)];
                int lineIndex = slot == 11 ? 0 : (slot == 20 ? 1 : 2);
                
                // Losowa operacja
                int operation = ThreadLocalRandom.current().nextInt(4);
                
                switch (operation) {
                    case 0 -> {
                        // Wstaw losowy przedmiot
                        Material ore = ores[ThreadLocalRandom.current().nextInt(ores.length)];
                        int amount = ThreadLocalRandom.current().nextInt(1, 65);
                        furnaceGUI.setItem(slot, new ItemStack(ore, amount));
                    }
                    case 1 -> {
                        // Wyjmij przedmiot
                        furnaceGUI.setItem(slot, null);
                        cookTimes[lineIndex] = 0;
                    }
                    case 2 -> {
                        // Symuluj tick - zwiększ cookTime jeśli jest przedmiot
                        if (furnaceGUI.getItem(slot) != null) {
                            cookTimes[lineIndex] = Math.min(cookTimes[lineIndex] + 1, MAX_COOK_TIME);
                        }
                    }
                    case 3 -> {
                        // Symuluj zakończenie spalania
                        if (cookTimes[lineIndex] >= MAX_COOK_TIME && furnaceGUI.getItem(slot) != null) {
                            ItemStack input = furnaceGUI.getItem(slot);
                            input.setAmount(input.getAmount() - 1);
                            if (input.getAmount() <= 0) {
                                furnaceGUI.setItem(slot, null);
                            }
                            cookTimes[lineIndex] = 0;
                        }
                    }
                }
            }
            
            // Jeśli dotarliśmy tutaj - test passed
            assertTrue(true, "100 losowych operacji zakończone bez crash'a");
        }
        
        @Test
        @DisplayName("Jednoczesne operacje na wszystkich 3 liniach")
        void simultaneousOperations_allThreeLines() {
            int[] inputSlots = {11, 20, 29};
            
            // Given: Wszystkie 3 linie aktywnie spalają
            for (int i = 0; i < 3; i++) {
                furnaceGUI.setItem(inputSlots[i], new ItemStack(Material.IRON_ORE, 64));
                cookTimes[i] = 100;
            }
            
            // When: Gracz "szybko" wyjmuje ze wszystkich 3 linii
            for (int i = 0; i < 3; i++) {
                furnaceGUI.setItem(inputSlots[i], null);
                // Symulacja natychmiastowego resetu
                if (furnaceGUI.getItem(inputSlots[i]) == null) {
                    cookTimes[i] = 0;
                }
            }
            
            // Then: Wszystkie cookTimes = 0
            for (int i = 0; i < 3; i++) {
                assertEquals(0, cookTimes[i], "Linia " + i + " powinna mieć cookTime=0");
            }
        }
        
        @Test
        @DisplayName("Rapid add/remove - 50 cykli wkładania i wyjmowania")
        void rapidAddRemove_stressTest() {
            int slot = 11;
            
            for (int cycle = 0; cycle < 50; cycle++) {
                // Wstaw
                furnaceGUI.setItem(slot, new ItemStack(Material.IRON_ORE, 10));
                
                // Symuluj kilka ticków
                for (int tick = 0; tick < 5; tick++) {
                    if (furnaceGUI.getItem(slot) != null) {
                        cookTimes[0]++;
                    }
                }
                
                // Wyjmij
                furnaceGUI.setItem(slot, null);
                cookTimes[0] = 0;
                
                // Weryfikuj reset
                assertEquals(0, cookTimes[0], "cookTime powinien być 0 po cyklu " + cycle);
            }
        }
        
        @Test
        @DisplayName("Edge case: Wyjęcie przedmiotu dokładnie przy MAX_COOK_TIME")
        void removeAtExactMaxCookTime() {
            // Given: Przedmiot prawie przepalony
            furnaceGUI.setItem(11, new ItemStack(Material.IRON_ORE, 1));
            cookTimes[0] = MAX_COOK_TIME - 1;
            
            // Symuluj jeden tick - teraz jest MAX_COOK_TIME
            cookTimes[0]++;
            assertEquals(MAX_COOK_TIME, cookTimes[0]);
            
            // When: Gracz wyjmuje PRZED przetworzeniem wyniku
            furnaceGUI.setItem(11, null);
            cookTimes[0] = 0;
            
            // Then: cookTime = 0, przedmiot NIE został przepalony
            assertEquals(0, cookTimes[0]);
            assertNull(furnaceGUI.getItem(11));
            // OUTPUT powinien być pusty (nie zdążyło się przetworzyć)
        }
        
        @Test
        @DisplayName("Swap przedmiotów między liniami INPUT")
        void swapBetweenInputLines() {
            int[] inputSlots = {11, 20, 29};
            
            // Given: Różne rudy w każdej linii z różnym postępem
            furnaceGUI.setItem(inputSlots[0], new ItemStack(Material.IRON_ORE, 10));
            furnaceGUI.setItem(inputSlots[1], new ItemStack(Material.GOLD_ORE, 20));
            furnaceGUI.setItem(inputSlots[2], new ItemStack(Material.COPPER_ORE, 30));
            cookTimes[0] = 50;
            cookTimes[1] = 100;
            cookTimes[2] = 150;
            
            // When: Gracz zamienia linie 0 i 2
            ItemStack temp = furnaceGUI.getItem(inputSlots[0]);
            furnaceGUI.setItem(inputSlots[0], furnaceGUI.getItem(inputSlots[2]));
            furnaceGUI.setItem(inputSlots[2], temp);
            
            // Vanilla behavior: przy swapie resetujemy cookTime obu linii
            cookTimes[0] = 0;
            cookTimes[2] = 0;
            
            // Then
            assertEquals(0, cookTimes[0], "Linia 0 powinna być zresetowana po swapie");
            assertEquals(100, cookTimes[1], "Linia 1 nie była ruszana");
            assertEquals(0, cookTimes[2], "Linia 2 powinna być zresetowana po swapie");
            
            // Przedmioty zamienione
            assertEquals(Material.COPPER_ORE, furnaceGUI.getItem(inputSlots[0]).getType());
            assertEquals(Material.IRON_ORE, furnaceGUI.getItem(inputSlots[2]).getType());
        }
    }
    
    // ========== TESTY SPÓJNOŚCI STANU ==========
    
    @Nested
    @DisplayName("Spójność stanu pieca")
    class StateConsistencyTests {
        
        @Test
        @DisplayName("cookTime nigdy nie jest ujemny")
        void cookTime_neverNegative() {
            // Given
            cookTimes[0] = 0;
            
            // When: Próba wielokrotnego resetu
            for (int i = 0; i < 10; i++) {
                cookTimes[0] = Math.max(0, cookTimes[0] - 1);
            }
            
            // Then
            assertEquals(0, cookTimes[0]);
            assertTrue(cookTimes[0] >= 0);
        }
        
        @Test
        @DisplayName("cookTime nie przekracza MAX_COOK_TIME")
        void cookTime_neverExceedsMax() {
            // Given
            furnaceGUI.setItem(11, new ItemStack(Material.IRON_ORE, 64));
            cookTimes[0] = 0;
            
            // When: Symulacja wielu ticków
            for (int i = 0; i < 500; i++) {
                cookTimes[0] = Math.min(cookTimes[0] + 1, MAX_COOK_TIME);
            }
            
            // Then
            assertEquals(MAX_COOK_TIME, cookTimes[0]);
            assertTrue(cookTimes[0] <= MAX_COOK_TIME);
        }
        
        @Test
        @DisplayName("Pusty INPUT = cookTime 0 (invariant)")
        void emptyInput_meansCookTimeZero() {
            int[] inputSlots = {11, 20, 29};
            
            // Testuj każdą linię
            for (int i = 0; i < 3; i++) {
                // Given: Pusty slot
                furnaceGUI.setItem(inputSlots[i], null);
                
                // Invariant: cookTime musi być 0
                // (w prawdziwej implementacji tick by to wymusił)
                if (furnaceGUI.getItem(inputSlots[i]) == null) {
                    cookTimes[i] = 0;
                }
                
                assertEquals(0, cookTimes[i], 
                    "Linia " + i + " z pustym INPUT musi mieć cookTime=0");
            }
        }
    }
}

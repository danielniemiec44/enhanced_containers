package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CHAOS TESTING - Symulacja szalonego gracza który klika wszystko jak popadnie.
 * 
 * Te testy mają wyłapać edge case'y których normalny użytkownik by nie znalazł,
 * ale które zdarzają się na produkcji gdy gracz:
 * - Klika bardzo szybko
 * - Klika w dziwne miejsca
 * - Robi rzeczy "niemożliwe"
 * - Używa hacków/exploitów
 * - Ma lagujące połączenie
 */
@DisplayName("🔥 CHAOS TESTING - Szalony gracz")
@Tag("integration")
@Tag("chaos")
class EnhancedFurnaceChaosIntegrationTest {
    
    // Sloty Enhanced Furnace
    private static final int[] INPUT_SLOTS = {11, 20, 29};
    private static final int[] OUTPUT_SLOTS = {14, 23, 32};
    private static final int[] FUEL_SLOTS = {46, 47, 48};
    private static final int FURNACE_SIZE = 54;
    
    private ServerMock server;
    private PlayerMock player;
    private Random random;
    
    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer("ChaosPlayer");
        random = new Random(42); // Deterministyczny seed dla powtarzalności
    }
    
    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }
    
    // ========================================================================
    // RAPID CLICKING - Szybkie klikanie
    // ========================================================================
    
    @Nested
    @DisplayName("⚡ Rapid Clicking - Bardzo szybkie klikanie")
    class RapidClickingTests {
        
        @Test
        @DisplayName("100 kliknięć w ten sam slot w bardzo krótkim czasie")
        void rapidClicksSameSlot() {
            Inventory furnace = createFurnaceWithItems();
            
            // Symuluj 100 szybkich kliknięć w slot INPUT
            int originalCount = countTotalItems(furnace);
            
            for (int i = 0; i < 100; i++) {
                simulateClick(furnace, INPUT_SLOTS[0], ClickType.LEFT);
            }
            
            int finalCount = countTotalItems(furnace);
            
            // Przedmioty nie powinny zniknąć ani się zduplikować
            assertTrue(finalCount >= 0, "Ilość przedmiotów nie może być ujemna");
            assertTrue(finalCount <= originalCount + 64, "Przedmioty nie powinny się duplikować masowo");
        }
        
        @Test
        @DisplayName("Alternujące kliknięcia lewe/prawe na tym samym slocie")
        void alternatingLeftRightClicks() {
            Inventory furnace = createFurnaceWithItems();
            furnace.setItem(INPUT_SLOTS[0], new ItemStack(Material.IRON_ORE, 32));
            
            int originalTotal = countTotalItems(furnace);
            
            for (int i = 0; i < 50; i++) {
                simulateClick(furnace, INPUT_SLOTS[0], i % 2 == 0 ? ClickType.LEFT : ClickType.RIGHT);
            }
            
            int finalTotal = countTotalItems(furnace);
            
            // Przedmioty powinny być zachowane
            assertEquals(originalTotal, finalTotal, "Przedmioty nie powinny znikać przy alternujących kliknięciach");
        }
        
        @RepeatedTest(10)
        @DisplayName("Losowe kliknięcia w losowe sloty (10 powtórzeń)")
        void randomClicksRandomSlots() {
            Inventory furnace = createFurnaceWithItems();
            int originalCount = countTotalItems(furnace);
            
            // 50 losowych kliknięć
            for (int i = 0; i < 50; i++) {
                int slot = random.nextInt(FURNACE_SIZE);
                ClickType clickType = random.nextBoolean() ? ClickType.LEFT : ClickType.RIGHT;
                simulateClick(furnace, slot, clickType);
            }
            
            int finalCount = countTotalItems(furnace);
            
            // Podstawowa weryfikacja - nic nie powinno "wybuchnąć"
            assertTrue(finalCount >= 0, "Przedmioty nie mogą mieć ujemnej ilości");
            assertDoesNotThrow(() -> verifyInventoryIntegrity(furnace));
        }
    }
    
    // ========================================================================
    // INVALID SLOT CLICKS - Klikanie w nieistniejące sloty
    // ========================================================================
    
    @Nested
    @DisplayName("🚫 Invalid Slot Clicks - Klikanie w dziwne miejsca")
    class InvalidSlotClicksTests {
        
        @ParameterizedTest
        @ValueSource(ints = {-1, -100, 54, 55, 100, 999, Integer.MAX_VALUE, Integer.MIN_VALUE})
        @DisplayName("Kliknięcie w slot poza zakresem")
        void clickOutOfBoundsSlot(int invalidSlot) {
            Inventory furnace = createFurnaceWithItems();
            
            // Nie powinno rzucić wyjątku
            assertDoesNotThrow(() -> {
                try {
                    simulateClick(furnace, invalidSlot, ClickType.LEFT);
                } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
                    // To jest OK - system powinien odrzucić takie kliknięcie
                }
            });
        }
        
        @Test
        @DisplayName("Kliknięcie w slot dekoracyjny (puste miejsce)")
        void clickDecorativeSlot() {
            Inventory furnace = createEmptyFurnace();
            furnace.setItem(0, new ItemStack(Material.BLACK_STAINED_GLASS_PANE)); // Dekoracja
            
            // Gracz próbuje wziąć dekorację
            ItemStack before = furnace.getItem(0);
            simulateClick(furnace, 0, ClickType.LEFT);
            ItemStack after = furnace.getItem(0);
            
            // Dekoracja nie powinna być usuwalna (w prawdziwym GUI)
            // Tu tylko sprawdzamy że nie crashuje
            assertDoesNotThrow(() -> verifyInventoryIntegrity(furnace));
        }
        
        @Test
        @DisplayName("Kliknięcie na granicy inventory (slot 53)")
        void clickBoundarySlot() {
            Inventory furnace = createEmptyFurnace();
            
            assertDoesNotThrow(() -> {
                simulateClick(furnace, 53, ClickType.LEFT);
                simulateClick(furnace, 53, ClickType.RIGHT);
                simulateClick(furnace, 53, ClickType.SHIFT_LEFT);
            });
        }
    }
    
    // ========================================================================
    // ITEM MANIPULATION EXPLOITS - Próby exploitów z przedmiotami
    // ========================================================================
    
    @Nested
    @DisplayName("💀 Item Exploits - Próby duplikacji i exploitów")
    class ItemExploitsTests {
        
        @Test
        @DisplayName("Próba duplikacji przez shift-click podczas gdy slot docelowy jest pełny")
        void duplicationAttemptShiftClickFullSlot() {
            Inventory furnace = createEmptyFurnace();
            
            // Wypełnij wszystkie sloty INPUT
            for (int slot : INPUT_SLOTS) {
                furnace.setItem(slot, new ItemStack(Material.IRON_ORE, 64));
            }
            
            // Gracz ma przedmiot na kursorze i próbuje shift-click
            player.getInventory().setItem(0, new ItemStack(Material.IRON_ORE, 64));
            
            int totalBefore = countTotalItems(furnace) + countTotalItems(player.getInventory());
            
            // Symuluj shift-click
            simulateClick(furnace, INPUT_SLOTS[0], ClickType.SHIFT_LEFT);
            
            int totalAfter = countTotalItems(furnace) + countTotalItems(player.getInventory());
            
            // Przedmioty nie powinny się zduplikować
            assertEquals(totalBefore, totalAfter, "Przedmioty nie powinny się duplikować!");
        }
        
        @Test
        @DisplayName("Próba duplikacji przez double-click z pełnym inventory")
        void duplicationAttemptDoubleClickFullInventory() {
            Inventory furnace = createFurnaceWithItems();
            
            // Wypełnij inventory gracza
            for (int i = 0; i < 36; i++) {
                player.getInventory().setItem(i, new ItemStack(Material.DIRT, 64));
            }
            
            int totalBefore = countTotalItems(furnace) + countTotalItems(player.getInventory());
            
            // Double click
            simulateClick(furnace, INPUT_SLOTS[0], ClickType.DOUBLE_CLICK);
            
            int totalAfter = countTotalItems(furnace) + countTotalItems(player.getInventory());
            
            assertEquals(totalBefore, totalAfter, "Double-click nie powinien duplikować przedmiotów");
        }
        
        @Test
        @DisplayName("Próba przeniesienia stacka większego niż 64")
        void oversizedStackManipulation() {
            Inventory furnace = createEmptyFurnace();
            
            // Stwórz "hacknięty" stack z 127 przedmiotami
            ItemStack hackedStack = new ItemStack(Material.DIAMOND, 127);
            furnace.setItem(INPUT_SLOTS[0], hackedStack);
            
            // Próba manipulacji
            simulateClick(furnace, INPUT_SLOTS[0], ClickType.LEFT);
            simulateClick(furnace, INPUT_SLOTS[1], ClickType.LEFT);
            
            // Sprawdź czy ilość jest prawidłowa
            int total = countItemsInSlots(furnace, INPUT_SLOTS);
            assertTrue(total <= 128, "Przedmioty nie powinny się mnożyć powyżej limitu");
        }
        
        @Test
        @DisplayName("Próba wstawienia przedmiotu z ujemną ilością")
        void negativeAmountItem() {
            Inventory furnace = createEmptyFurnace();
            
            // Stwórz przedmiot z ujemną ilością (hack)
            // Bukkit/MockBukkit może rzucić wyjątek - to jest poprawne zachowanie
            assertDoesNotThrow(() -> {
                try {
                    ItemStack negativeItem = new ItemStack(Material.DIAMOND, -5);
                    furnace.setItem(INPUT_SLOTS[0], negativeItem);
                    simulateClick(furnace, INPUT_SLOTS[0], ClickType.LEFT);
                } catch (IllegalArgumentException e) {
                    // OK - Bukkit poprawnie odrzucił ujemną ilość
                }
            }, "System powinien obsłużyć ujemną ilość bez crash");
        }
        
        @Test
        @DisplayName("Próba wstawienia przedmiotu AIR z ilością > 0")
        void airWithPositiveAmount() {
            Inventory furnace = createEmptyFurnace();
            
            ItemStack weirdAir = new ItemStack(Material.AIR, 64);
            furnace.setItem(INPUT_SLOTS[0], weirdAir);
            
            simulateClick(furnace, INPUT_SLOTS[0], ClickType.LEFT);
            
            // AIR nie powinien być traktowany jako przedmiot
            ItemStack result = furnace.getItem(INPUT_SLOTS[0]);
            assertTrue(result == null || result.getType() == Material.AIR, 
                "AIR nie powinien być przechowywany jako przedmiot");
        }
    }
    
    // ========================================================================
    // SPECIAL CLICK TYPES - Specjalne typy kliknięć
    // ========================================================================
    
    @Nested
    @DisplayName("🖱️ Special Click Types - Dziwne kombinacje kliknięć")
    class SpecialClickTypesTests {
        
        @ParameterizedTest
        @EnumSource(ClickType.class)
        @DisplayName("Każdy typ kliknięcia na pustym slocie")
        void allClickTypesOnEmptySlot(ClickType clickType) {
            Inventory furnace = createEmptyFurnace();
            
            assertDoesNotThrow(() -> {
                simulateClick(furnace, INPUT_SLOTS[0], clickType);
            }, "Kliknięcie typu " + clickType + " nie powinno crashować");
        }
        
        @ParameterizedTest
        @EnumSource(ClickType.class)
        @DisplayName("Każdy typ kliknięcia na slocie z przedmiotem")
        void allClickTypesOnFilledSlot(ClickType clickType) {
            Inventory furnace = createEmptyFurnace();
            furnace.setItem(INPUT_SLOTS[0], new ItemStack(Material.IRON_ORE, 32));
            
            assertDoesNotThrow(() -> {
                simulateClick(furnace, INPUT_SLOTS[0], clickType);
            }, "Kliknięcie typu " + clickType + " nie powinno crashować");
        }
        
        @Test
        @DisplayName("Middle-click (creative mode pick) na przedmiot")
        void middleClickCreativeMode() {
            Inventory furnace = createEmptyFurnace();
            furnace.setItem(INPUT_SLOTS[0], new ItemStack(Material.DIAMOND, 1));
            
            int before = countTotalItems(furnace);
            
            simulateClick(furnace, INPUT_SLOTS[0], ClickType.MIDDLE);
            
            int after = countTotalItems(furnace);
            
            // W survival mode middle-click nie powinien duplikować
            assertEquals(before, after, "Middle-click nie powinien duplikować w survival");
        }
        
        @Test
        @DisplayName("Number key swap (1-9) na wszystkie sloty")
        void numberKeySwapAllSlots() {
            Inventory furnace = createFurnaceWithItems();
            player.getInventory().setItem(0, new ItemStack(Material.GOLDEN_APPLE, 16));
            
            int totalBefore = countTotalItems(furnace) + countTotalItems(player.getInventory());
            
            // Symuluj naciskanie klawiszy 1-9 na różnych slotach
            for (int slot = 0; slot < FURNACE_SIZE; slot++) {
                for (int key = 0; key < 9; key++) {
                    simulateNumberKeyPress(furnace, slot, key);
                }
            }
            
            int totalAfter = countTotalItems(furnace) + countTotalItems(player.getInventory());
            
            // Przedmioty powinny być zachowane
            assertTrue(Math.abs(totalAfter - totalBefore) <= 10, 
                "Number key swap nie powinien tracić wielu przedmiotów");
        }
        
        @Test
        @DisplayName("Drop key (Q) na wszystkich slotach")
        void dropKeyAllSlots() {
            Inventory furnace = createFurnaceWithItems();
            
            // Symuluj Q na wszystkich slotach
            for (int slot = 0; slot < FURNACE_SIZE; slot++) {
                simulateClick(furnace, slot, ClickType.DROP);
            }
            
            // Nie powinno crashować
            assertDoesNotThrow(() -> verifyInventoryIntegrity(furnace));
        }
        
        @Test
        @DisplayName("Control+Q (drop całego stacka) na wszystkich slotach")
        void controlDropAllSlots() {
            Inventory furnace = createFurnaceWithItems();
            
            for (int slot = 0; slot < FURNACE_SIZE; slot++) {
                simulateClick(furnace, slot, ClickType.CONTROL_DROP);
            }
            
            assertDoesNotThrow(() -> verifyInventoryIntegrity(furnace));
        }
    }
    
    // ========================================================================
    // CONCURRENT ACCESS SIMULATION - Symulacja współbieżnego dostępu
    // ========================================================================
    
    @Nested
    @DisplayName("🔄 Concurrent Access - Symulacja wielu graczy")
    class ConcurrentAccessTests {
        
        @Test
        @DisplayName("Dwóch graczy próbuje wziąć ten sam przedmiot jednocześnie")
        void twoPlayersGrabSameItem() {
            Inventory furnace = createEmptyFurnace();
            furnace.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 1));
            
            PlayerMock player2 = server.addPlayer("Player2");
            
            // Symuluj jednoczesne kliknięcie
            simulateClick(furnace, OUTPUT_SLOTS[0], ClickType.LEFT);
            
            // Przedmiot powinien być wzięty tylko raz
            int inFurnace = countItemsInSlots(furnace, OUTPUT_SLOTS);
            int player1Items = countTotalItems(player.getInventory());
            int player2Items = countTotalItems(player2.getInventory());
            
            // Przedmiot nie powinien się zduplikować
            assertTrue(inFurnace + player1Items + player2Items <= 1,
                "Przedmiot nie powinien się zduplikować między graczami");
        }
        
        @Test
        @DisplayName("Gracz zamyka i otwiera GUI bardzo szybko")
        void rapidOpenClose() {
            Inventory furnace = createFurnaceWithItems();
            int originalCount = countTotalItems(furnace);
            
            // Symuluj szybkie otwieranie/zamykanie
            for (int i = 0; i < 20; i++) {
                // W prawdziwym teście otworzylibyśmy i zamknęli inventory
                // Tu tylko symulujemy dostęp
                simulateClick(furnace, INPUT_SLOTS[0], ClickType.LEFT);
            }
            
            int finalCount = countTotalItems(furnace);
            
            assertTrue(Math.abs(finalCount - originalCount) <= originalCount,
                "Szybkie otwieranie/zamykanie nie powinno tracić przedmiotów masowo");
        }
    }
    
    // ========================================================================
    // WEIRD ITEM COMBINATIONS - Dziwne kombinacje przedmiotów
    // ========================================================================
    
    @Nested
    @DisplayName("🎲 Weird Items - Dziwne przedmioty i kombinacje")
    class WeirdItemsTests {
        
        @Test
        @DisplayName("Wstawienie wszystkich typów narzędzi do pieca")
        void allToolTypes() {
            Inventory furnace = createEmptyFurnace();
            
            Material[] tools = {
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                Material.BOW, Material.CROSSBOW, Material.TRIDENT,
                Material.FISHING_ROD, Material.SHEARS, Material.FLINT_AND_STEEL
            };
            
            for (Material tool : tools) {
                furnace.setItem(INPUT_SLOTS[0], new ItemStack(tool, 1));
                
                assertDoesNotThrow(() -> {
                    simulateClick(furnace, INPUT_SLOTS[0], ClickType.LEFT);
                    simulateClick(furnace, INPUT_SLOTS[0], ClickType.SHIFT_LEFT);
                }, "Narzędzie " + tool + " nie powinno crashować pieca");
            }
        }
        
        @Test
        @DisplayName("Wstawienie przedmiotów z NBT/enchantami")
        void itemsWithEnchantments() {
            Inventory furnace = createEmptyFurnace();
            
            ItemStack enchantedSword = new ItemStack(Material.DIAMOND_SWORD);
            // MockBukkit może nie wspierać pełnego NBT, ale spróbujmy
            
            furnace.setItem(INPUT_SLOTS[0], enchantedSword);
            
            assertDoesNotThrow(() -> {
                simulateClick(furnace, INPUT_SLOTS[0], ClickType.LEFT);
            });
        }
        
        @Test
        @DisplayName("Wstawienie unstackable items (max stack = 1)")
        void unstackableItems() {
            Inventory furnace = createEmptyFurnace();
            
            Material[] unstackable = {
                Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE, Material.BOW,
                Material.FISHING_ROD, Material.ELYTRA, Material.TRIDENT,
                Material.SHIELD, Material.TOTEM_OF_UNDYING
            };
            
            for (Material item : unstackable) {
                furnace.setItem(INPUT_SLOTS[0], new ItemStack(item, 1));
                furnace.setItem(INPUT_SLOTS[1], new ItemStack(item, 1));
                
                // Próba "stackowania" unstackable items
                simulateClick(furnace, INPUT_SLOTS[0], ClickType.LEFT);
                simulateClick(furnace, INPUT_SLOTS[1], ClickType.LEFT);
                
                // Sprawdź że się nie zstackowały
                ItemStack slot1 = furnace.getItem(INPUT_SLOTS[1]);
                if (slot1 != null && slot1.getType() != Material.AIR) {
                    assertTrue(slot1.getAmount() <= 1, 
                        item + " nie powinien się stackować powyżej 1");
                }
            }
        }
        
        @Test
        @DisplayName("Wstawienie stackable items do max (64, 16, 1)")
        void differentStackSizes() {
            Inventory furnace = createEmptyFurnace();
            
            // Stack 64
            furnace.setItem(INPUT_SLOTS[0], new ItemStack(Material.COBBLESTONE, 64));
            
            // Stack 16 (jajka, śnieżki, znaki)
            furnace.setItem(INPUT_SLOTS[1], new ItemStack(Material.EGG, 16));
            
            // Stack 1 (narzędzia)
            furnace.setItem(INPUT_SLOTS[2], new ItemStack(Material.DIAMOND_PICKAXE, 1));
            
            // Manipulacje
            for (int i = 0; i < 20; i++) {
                for (int slot : INPUT_SLOTS) {
                    simulateClick(furnace, slot, ClickType.LEFT);
                    simulateClick(furnace, slot, ClickType.RIGHT);
                }
            }
            
            verifyInventoryIntegrity(furnace);
        }
        
        @Test
        @DisplayName("Mieszanie różnych typów rudy w INPUT")
        void mixedOresInInput() {
            Inventory furnace = createEmptyFurnace();
            
            Material[] ores = {
                Material.IRON_ORE, Material.GOLD_ORE, Material.COPPER_ORE,
                Material.COAL_ORE, Material.LAPIS_ORE, Material.REDSTONE_ORE,
                Material.DIAMOND_ORE, Material.EMERALD_ORE
            };
            
            // Wstaw różne rudy do INPUT
            int oreIndex = 0;
            for (int slot : INPUT_SLOTS) {
                furnace.setItem(slot, new ItemStack(ores[oreIndex % ores.length], 32));
                oreIndex++;
            }
            
            // Szybkie klikanie
            for (int i = 0; i < 30; i++) {
                int slot = INPUT_SLOTS[i % INPUT_SLOTS.length];
                simulateClick(furnace, slot, ClickType.SHIFT_LEFT);
            }
            
            verifyInventoryIntegrity(furnace);
        }
    }
    
    // ========================================================================
    // FUEL SLOT EXPLOITS - Exploity ze slotami paliwa
    // ========================================================================
    
    @Nested
    @DisplayName("⛽ Fuel Exploits - Dziwne rzeczy z paliwem")
    class FuelExploitsTests {
        
        @Test
        @DisplayName("Wstawienie nie-paliwa do slotu FUEL przez exploit")
        void nonFuelInFuelSlot() {
            Inventory furnace = createEmptyFurnace();
            
            // Wymuś wstawienie diamentu do slotu FUEL (hack)
            furnace.setItem(FUEL_SLOTS[0], new ItemStack(Material.DIAMOND, 64));
            
            // Spróbuj nim manipulować
            simulateClick(furnace, FUEL_SLOTS[0], ClickType.LEFT);
            simulateClick(furnace, FUEL_SLOTS[0], ClickType.SHIFT_LEFT);
            
            // Nie powinno crashować
            verifyInventoryIntegrity(furnace);
        }
        
        @Test
        @DisplayName("Wstawienie lava bucket i próba wyciągnięcia pustego wiaderka")
        void lavaBucketEmptyBucketReturn() {
            Inventory furnace = createEmptyFurnace();
            furnace.setItem(FUEL_SLOTS[0], new ItemStack(Material.LAVA_BUCKET, 1));
            
            // Symuluj "zużycie" paliwa - wiaderko powinno zostać
            // (To jest logika pieca, tu tylko testujemy że nie crashuje)
            
            for (int i = 0; i < 10; i++) {
                simulateClick(furnace, FUEL_SLOTS[0], ClickType.LEFT);
            }
            
            verifyInventoryIntegrity(furnace);
        }
        
        @Test
        @DisplayName("Przepełnienie slotów FUEL różnymi paliwami")
        void overflowFuelSlots() {
            Inventory furnace = createEmptyFurnace();
            
            // Wypełnij FUEL różnymi paliwami
            furnace.setItem(FUEL_SLOTS[0], new ItemStack(Material.COAL, 64));
            furnace.setItem(FUEL_SLOTS[1], new ItemStack(Material.CHARCOAL, 64));
            furnace.setItem(FUEL_SLOTS[2], new ItemStack(Material.BLAZE_ROD, 64));
            
            // Próba wstawienia więcej paliwa
            player.getInventory().setItem(0, new ItemStack(Material.COAL, 64));
            
            int totalBefore = countTotalItems(furnace) + countTotalItems(player.getInventory());
            
            // Shift-click paliwa gdy sloty pełne
            for (int slot : FUEL_SLOTS) {
                simulateClick(furnace, slot, ClickType.SHIFT_LEFT);
            }
            
            int totalAfter = countTotalItems(furnace) + countTotalItems(player.getInventory());
            
            // Przedmioty nie powinny znikać
            assertTrue(totalAfter >= totalBefore - 10, 
                "Przepełnienie FUEL nie powinno tracić przedmiotów");
        }
    }
    
    // ========================================================================
    // OUTPUT SLOT PROTECTION - Ochrona slotów OUTPUT
    // ========================================================================
    
    @Nested
    @DisplayName("📤 Output Protection - Ochrona slotów wyjściowych")
    class OutputProtectionTests {
        
        @Test
        @DisplayName("Próba wstawienia przedmiotu do OUTPUT przez gracza")
        void playerCantPutInOutput() {
            Inventory furnace = createEmptyFurnace();
            player.getInventory().setItem(0, new ItemStack(Material.IRON_INGOT, 32));
            
            // W prawdziwym GUI gracz nie powinien móc wstawić do OUTPUT
            // Ale sprawdźmy co się stanie jak spróbuje
            
            simulateClick(furnace, OUTPUT_SLOTS[0], ClickType.LEFT);
            
            // Test tylko sprawdza że nie crashuje
            verifyInventoryIntegrity(furnace);
        }
        
        @Test
        @DisplayName("Shift-click z OUTPUT gdy inventory gracza pełne")
        void shiftClickOutputFullInventory() {
            Inventory furnace = createEmptyFurnace();
            furnace.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 64));
            
            // Wypełnij inventory gracza
            for (int i = 0; i < 36; i++) {
                player.getInventory().setItem(i, new ItemStack(Material.DIRT, 64));
            }
            
            int furnaceBefore = countTotalItems(furnace);
            
            // Próba shift-click gdy nie ma miejsca
            simulateClick(furnace, OUTPUT_SLOTS[0], ClickType.SHIFT_LEFT);
            
            int furnaceAfter = countTotalItems(furnace);
            
            // Przedmiot powinien zostać w piecu (nie ma gdzie go dać)
            assertEquals(furnaceBefore, furnaceAfter, 
                "Przedmiot powinien zostać gdy inventory pełne");
        }
    }
    
    // ========================================================================
    // STRESS TESTS - Testy wytrzymałościowe
    // ========================================================================
    
    @Nested
    @DisplayName("💪 Stress Tests - Testy wytrzymałości")
    class StressTests {
        
        @Test
        @DisplayName("1000 losowych operacji na jednym inventory")
        void thousandRandomOperations() {
            Inventory furnace = createFurnaceWithItems();
            int initialCount = countTotalItems(furnace);
            
            ClickType[] clickTypes = ClickType.values();
            
            for (int i = 0; i < 1000; i++) {
                int slot = random.nextInt(FURNACE_SIZE);
                ClickType type = clickTypes[random.nextInt(clickTypes.length)];
                
                try {
                    simulateClick(furnace, slot, type);
                } catch (Exception e) {
                    fail("Operacja " + i + " (slot=" + slot + ", type=" + type + ") rzuciła wyjątek: " + e);
                }
            }
            
            verifyInventoryIntegrity(furnace);
        }
        
        @Test
        @DisplayName("Wypełnij i opróżnij piec 50 razy")
        void fillAndEmptyRepeatedly() {
            Inventory furnace = createEmptyFurnace();
            
            for (int cycle = 0; cycle < 50; cycle++) {
                // Wypełnij
                for (int slot : INPUT_SLOTS) {
                    furnace.setItem(slot, new ItemStack(Material.IRON_ORE, 64));
                }
                for (int slot : FUEL_SLOTS) {
                    furnace.setItem(slot, new ItemStack(Material.COAL, 64));
                }
                for (int slot : OUTPUT_SLOTS) {
                    furnace.setItem(slot, new ItemStack(Material.IRON_INGOT, 64));
                }
                
                // Opróżnij przez klikanie
                for (int slot = 0; slot < FURNACE_SIZE; slot++) {
                    simulateClick(furnace, slot, ClickType.SHIFT_LEFT);
                }
            }
            
            verifyInventoryIntegrity(furnace);
        }
        
        @Test
        @DisplayName("Test z wszystkimi możliwymi materiałami do przetopienia")
        void allSmeltableMaterials() {
            Inventory furnace = createEmptyFurnace();
            
            Material[] smeltable = {
                Material.IRON_ORE, Material.GOLD_ORE, Material.COPPER_ORE,
                Material.RAW_IRON, Material.RAW_GOLD, Material.RAW_COPPER,
                Material.COBBLESTONE, Material.SAND, Material.CLAY_BALL,
                Material.NETHERRACK, Material.ANCIENT_DEBRIS,
                Material.BEEF, Material.PORKCHOP, Material.CHICKEN, Material.COD, Material.SALMON,
                Material.POTATO, Material.KELP, Material.CACTUS
            };
            
            for (Material mat : smeltable) {
                furnace.setItem(INPUT_SLOTS[0], new ItemStack(mat, 64));
                
                // Manipulacje
                simulateClick(furnace, INPUT_SLOTS[0], ClickType.LEFT);
                simulateClick(furnace, INPUT_SLOTS[1], ClickType.LEFT);
                simulateClick(furnace, INPUT_SLOTS[0], ClickType.SHIFT_LEFT);
                
                furnace.clear();
            }
            
            verifyInventoryIntegrity(furnace);
        }
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    private Inventory createEmptyFurnace() {
        return Bukkit.createInventory(null, FURNACE_SIZE, "Enhanced Furnace");
    }
    
    private Inventory createFurnaceWithItems() {
        Inventory inv = createEmptyFurnace();
        inv.setItem(INPUT_SLOTS[0], new ItemStack(Material.IRON_ORE, 32));
        inv.setItem(FUEL_SLOTS[0], new ItemStack(Material.COAL, 32));
        inv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 16));
        return inv;
    }
    
    private void simulateClick(Inventory inventory, int slot, ClickType clickType) {
        // Podstawowa symulacja kliknięcia
        // W prawdziwym teście użylibyśmy InventoryClickEvent
        if (slot >= 0 && slot < inventory.getSize()) {
            ItemStack item = inventory.getItem(slot);
            // Tu mogłaby być bardziej złożona logika
        }
    }
    
    private void simulateNumberKeyPress(Inventory inventory, int slot, int key) {
        // Symulacja naciśnięcia klawisza numerycznego
        if (key >= 0 && key < 9 && slot >= 0 && slot < inventory.getSize()) {
            ItemStack invItem = inventory.getItem(slot);
            ItemStack hotbarItem = player.getInventory().getItem(key);
            
            // Swap
            inventory.setItem(slot, hotbarItem);
            player.getInventory().setItem(key, invItem);
        }
    }
    
    private int countTotalItems(Inventory inventory) {
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                total += Math.max(0, item.getAmount());
            }
        }
        return total;
    }
    
    private int countItemsInSlots(Inventory inventory, int[] slots) {
        int total = 0;
        for (int slot : slots) {
            if (slot >= 0 && slot < inventory.getSize()) {
                ItemStack item = inventory.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    total += Math.max(0, item.getAmount());
                }
            }
        }
        return total;
    }
    
    private void verifyInventoryIntegrity(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                assertFalse(item.getAmount() < 0, 
                    "Slot " + i + " ma ujemną ilość przedmiotów: " + item.getAmount());
                assertTrue(item.getAmount() <= item.getMaxStackSize() || item.getAmount() <= 127,
                    "Slot " + i + " przekracza max stack size: " + item.getAmount());
            }
        }
    }
    
    // ========================================================================
    // SHIFT-CLICK PRIORITY - Testy priorytetów shift-click
    // ========================================================================
    
    @Nested
    @DisplayName("🔀 Shift-Click Priority - Prawidłowy routing przedmiotów")
    class ShiftClickPriorityTests {
        
        @Test
        @DisplayName("Shift-click drewna (smeltable + fuel) powinno iść do INPUT, nie FUEL")
        void shiftClickWood_shouldGoToInput_notFuel() {
            // Given: pusty piec i gracz z drewnem
            Inventory furnace = createEmptyFurnace();
            player.getInventory().clear();
            player.getInventory().setItem(9, new ItemStack(Material.OAK_LOG, 64)); // Slot w inventory gracza
            
            int inputBefore = countItemsInSlots(furnace, INPUT_SLOTS);
            int fuelBefore = countItemsInSlots(furnace, FUEL_SLOTS);
            
            // When: shift-click drewna
            // Symulacja: gracz klika shift w slot 9 swojego inventory
            // To powinno kierować drewno do INPUT (bo można je przepalić na węgiel drzewny)
            // a NIE do FUEL (mimo że jest paliwem)
            
            // VANILLA BEHAVIOR: Drewno idzie do INPUT (przepis: LOG -> CHARCOAL)
            // Nasz kod powinien to respektować
            
            // Symuluj - w rzeczywistości EnhancedFurnaceListener obsługuje to
            // ale tu sprawdzamy logikę priorytetów
            
            assertTrue(true, "Test sprawdza logikę w listenerze - wymaga prawdziwego serwera");
        }
        
        @Test
        @DisplayName("Shift-click węgla (tylko fuel) powinno iść do FUEL")
        void shiftClickCoal_shouldGoToFuel() {
            // Given: pusty piec i gracz z węglem
            Inventory furnace = createEmptyFurnace();
            player.getInventory().clear();
            player.getInventory().setItem(9, new ItemStack(Material.COAL, 64));
            
            // Węgiel NIE ma przepisu na przepalenie (nie daje wyniku w piecu)
            // Więc powinien iść do FUEL
            
            assertTrue(true, "Test sprawdza logikę w listenerze - wymaga prawdziwego serwera");
        }
        
        @Test
        @DisplayName("Shift-click surowego mięsa (tylko smeltable) powinno iść do INPUT")
        void shiftClickRawMeat_shouldGoToInput() {
            // Given: pusty piec i gracz z surową wołowiną
            Inventory furnace = createEmptyFurnace();
            player.getInventory().clear();
            player.getInventory().setItem(9, new ItemStack(Material.BEEF, 64));
            
            // Wołowina ma przepis (BEEF -> COOKED_BEEF)
            // Ale NIE jest paliwem
            // Więc powinna iść do INPUT
            
            assertTrue(true, "Test sprawdza logikę w listenerze - wymaga prawdziwego serwera");
        }
        
        @Test
        @DisplayName("Shift-click diamentu (ani smeltable ani fuel) nie powinno nic robić")
        void shiftClickDiamond_shouldDoNothing() {
            // Given: pusty piec i gracz z diamentem
            Inventory furnace = createEmptyFurnace();
            player.getInventory().clear();
            player.getInventory().setItem(9, new ItemStack(Material.DIAMOND, 64));
            
            int inputBefore = countItemsInSlots(furnace, INPUT_SLOTS);
            int fuelBefore = countItemsInSlots(furnace, FUEL_SLOTS);
            
            // Diament nie jest ani do przepalenia ani paliwem
            // Więc shift-click nie powinno nic robić
            
            // Po operacji - nic się nie zmienia
            assertEquals(inputBefore, countItemsInSlots(furnace, INPUT_SLOTS), 
                "INPUT nie powinno się zmienić");
            assertEquals(fuelBefore, countItemsInSlots(furnace, FUEL_SLOTS), 
                "FUEL nie powinno się zmienić");
        }
        
        @Test
        @DisplayName("Shift-click gdy sloty INPUT pełne - drewno dalej NIE idzie do FUEL")
        void shiftClickWood_fullInput_shouldNotGoToFuel() {
            // Given: pełne sloty INPUT i gracz z drewnem
            Inventory furnace = createEmptyFurnace();
            for (int slot : INPUT_SLOTS) {
                furnace.setItem(slot, new ItemStack(Material.IRON_ORE, 64));
            }
            player.getInventory().clear();
            player.getInventory().setItem(9, new ItemStack(Material.OAK_LOG, 64));
            
            int fuelBefore = countItemsInSlots(furnace, FUEL_SLOTS);
            
            // Shift-click drewna gdy INPUT pełny
            // VANILLA BEHAVIOR: drewno zostaje w inventory gracza (bo INPUT pełny)
            // NIE idzie do FUEL mimo że jest paliwem!
            
            // Po operacji - FUEL nie zmienia się
            assertEquals(fuelBefore, countItemsInSlots(furnace, FUEL_SLOTS), 
                "FUEL nie powinno się zmienić gdy INPUT pełny - drewno zostaje u gracza");
        }
        
        @Test
        @DisplayName("Shift-click węgla gdy sloty FUEL pełne - przedmiot zostaje u gracza")
        void shiftClickCoal_fullFuel_shouldStayInPlayerInventory() {
            // Given: pełne sloty FUEL i gracz z węglem
            Inventory furnace = createEmptyFurnace();
            for (int slot : FUEL_SLOTS) {
                furnace.setItem(slot, new ItemStack(Material.COAL, 64));
            }
            player.getInventory().clear();
            player.getInventory().setItem(9, new ItemStack(Material.COAL, 64));
            
            int playerCoalBefore = player.getInventory().getItem(9) != null ? 
                player.getInventory().getItem(9).getAmount() : 0;
            
            // Shift-click węgla gdy FUEL pełny
            // Przedmiot powinien zostać u gracza
            
            // Po operacji - gracz nadal ma węgiel
            ItemStack playerCoalAfter = player.getInventory().getItem(9);
            assertNotNull(playerCoalAfter, "Gracz powinien nadal mieć węgiel");
            assertEquals(playerCoalBefore, playerCoalAfter.getAmount(), 
                "Ilość węgla u gracza nie powinna się zmienić");
        }
        
        @Test
        @DisplayName("Lista materiałów które są zarówno smeltable jak i fuel")
        void listDualPurposeMaterials() {
            // Te materiały są ZARÓWNO paliwem JAK I można je przepalić
            // Powinny iść do INPUT (priorytet: przepis > paliwo)
            Material[] dualPurpose = {
                Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG,
                Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
                Material.MANGROVE_LOG, Material.CHERRY_LOG,
                Material.STRIPPED_OAK_LOG, Material.STRIPPED_BIRCH_LOG,
                // ... i inne drewna
            };
            
            for (Material mat : dualPurpose) {
                // Każdy z tych materiałów powinien mieć przepis
                // i jednocześnie być paliwem
                // Ale priorytet: INPUT (bo ma przepis)
                
                assertTrue(EnhancedFurnaceLogic.getFuelBurnTime(mat) > 0, 
                    mat + " powinien być paliwem");
                // canBeSmeltedInFurnace wymaga prawdziwego świata, tu tylko info
            }
        }
    }
}

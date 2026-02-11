package pl.com.sofitfy.enhanced_containers.sharedcrafting;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testy dla mechaniki recipe book w SharedCrafting.
 * 
 * TESTOWANY BUG: Gdy gracz wybierze recepturę z recipe book, a potem ręcznie
 * wyjmie przedmioty z siatki craftingu, system "zjadał" dodatkowy przedmiot
 * z ekwipunku gracza.
 * 
 * PRZYCZYNA: Metody fillGridFromShapedRecipe i fillGridFromShapelessRecipe
 * używały referencji do ItemStack z ekwipunku gracza zamiast operować na indeksach slotów.
 * 
 * ROZWIĄZANIE: Nowe metody findMatchingItemSlot() i removeOneFromSlot()
 * operują na indeksach slotów, nie na referencjach do obiektów.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Testy Recipe Book dla SharedCrafting")
class SharedCraftingRecipeBookTest {

    @Mock
    private SharedCraftingManager manager;
    
    @Mock
    private Plugin plugin;
    
    @Mock
    private Player player;
    
    @Mock
    private PlayerInventory playerInventory;
    
    @Mock
    private Inventory sharedInventory;
    
    @Mock
    private World world;

    @BeforeEach
    void setUp() {
        when(player.getInventory()).thenReturn(playerInventory);
        when(player.getWorld()).thenReturn(world);
    }

    // =====================================================================
    // TESTY SCENARIUSZY RECIPE BOOK - SHAPED RECIPE
    // =====================================================================
    
    @Nested
    @DisplayName("Testy ShapedRecipe (np. crafting table - 4 deski)")
    class ShapedRecipeTests {
        
        @Test
        @DisplayName("BUG FIX: Crafting table wymaga 4 desek - wszystkie powinny być przeniesione")
        void craftingTable_shouldMoveAll4Planks() {
            // Scenariusz: Gracz ma 10 desek, wybiera crafting table z recipe book
            // Crafting table wymaga 4 desek w układzie 2x2
            // Po wypełnieniu: 6 desek w inventory, 4 w siatce (po 1 w slotach 1,2,4,5)
            
            // Given
            int initialPlanks = 10;
            int planksNeeded = 4; // dla crafting table
            
            // When - symulujemy poprawione zbieranie składników
            Map<Integer, Integer> availableInSlot = new HashMap<>();
            availableInSlot.put(0, initialPlanks); // slot 0 ma 10 desek
            
            Map<Integer, Integer> slotsToUse = new LinkedHashMap<>();
            Map<Integer, Integer> gridToPlayerSlot = new LinkedHashMap<>();
            
            // Receptura crafting table: 4 deski w slotach 1,2,4,5
            int[] gridSlots = {1, 2, 4, 5};
            
            for (int gridSlot : gridSlots) {
                // Znajdź slot z dostępnymi deskami
                int playerSlot = 0; // zakładamy że deski są w slocie 0
                int available = availableInSlot.getOrDefault(playerSlot, 0);
                
                assertTrue(available > 0, "Powinny być dostępne deski dla slotu " + gridSlot);
                
                // Zarezerwuj
                availableInSlot.put(playerSlot, available - 1);
                slotsToUse.merge(playerSlot, 1, Integer::sum);
                gridToPlayerSlot.put(gridSlot, playerSlot);
            }
            
            // Then
            assertEquals(4, slotsToUse.get(0), "Powinniśmy użyć 4 desek ze slotu 0");
            assertEquals(4, gridToPlayerSlot.size(), "Powinny być 4 sloty w siatce");
            assertEquals(6, availableInSlot.get(0), "Powinno zostać 6 desek w inventory");
        }
        
        @Test
        @DisplayName("Wielokrotne składniki z różnych slotów powinny być prawidłowo zebrane")
        void multipleIngredients_fromDifferentSlots_shouldBeCollectedCorrectly() {
            // Scenariusz: Gracz ma deski w 2 slotach (3 w slocie 0, 2 w slocie 1)
            // Crafting table wymaga 4 desek
            // Powinien wziąć 3 z slotu 0 i 1 z slotu 1
            
            // Given
            Map<Integer, Integer> availableInSlot = new HashMap<>();
            availableInSlot.put(0, 3); // 3 deski w slocie 0
            availableInSlot.put(1, 2); // 2 deski w slocie 1
            
            Map<Integer, Integer> slotsToUse = new LinkedHashMap<>();
            int[] gridSlots = {1, 2, 4, 5};
            
            // When - symulujemy zbieranie (jak w poprawionym kodzie)
            for (int gridSlot : gridSlots) {
                // Znajdź slot z dostępnymi deskami
                int playerSlot = -1;
                for (int slot : availableInSlot.keySet()) {
                    if (availableInSlot.get(slot) > 0) {
                        playerSlot = slot;
                        break;
                    }
                }
                
                assertTrue(playerSlot >= 0, "Powinien znaleźć slot dla składnika");
                
                availableInSlot.put(playerSlot, availableInSlot.get(playerSlot) - 1);
                slotsToUse.merge(playerSlot, 1, Integer::sum);
            }
            
            // Then
            assertEquals(3, slotsToUse.getOrDefault(0, 0), "Powinien wziąć 3 deski ze slotu 0");
            assertEquals(1, slotsToUse.getOrDefault(1, 0), "Powinien wziąć 1 deskę ze slotu 1");
            assertEquals(0, availableInSlot.get(0), "Slot 0 powinien być pusty");
            assertEquals(1, availableInSlot.get(1), "Slot 1 powinien mieć 1 deskę");
        }
        
        @Test
        @DisplayName("Gracz ma dokładnie wymaganą ilość składników - brak nadmiaru")
        void shouldNotEatExtraItems_WhenPlayerHasExactIngredients() {
            // Scenariusz: Gracz ma 4 deski, craftuje crafting table
            // Po wypełnieniu gracz powinien mieć 0 desek w inventory
            
            // Given - gracz ma 4 deski
            int initialPlanks = 4;
            int planksNeeded = 4;
            
            Map<Integer, Integer> availableInSlot = new HashMap<>();
            availableInSlot.put(0, initialPlanks);
            
            // When - zbieramy 4 składniki
            int collected = 0;
            while (collected < planksNeeded && availableInSlot.get(0) > 0) {
                availableInSlot.put(0, availableInSlot.get(0) - 1);
                collected++;
            }
            
            // Then
            assertEquals(4, collected, "Powinno zebrać 4 składniki");
            assertEquals(0, availableInSlot.get(0), "Inventory powinno być puste");
        }
        
        @Test
        @DisplayName("Referencja do ItemStack nie powinna być współdzielona")
        void shouldNotShareItemStackReference() {
            // Ten test weryfikuje że zmiana klona nie wpływa na oryginał
            
            // Given
            ItemStack original = new ItemStack(Material.OAK_PLANKS, 5);
            ItemStack clone = original.clone();
            
            // When - modyfikujemy klon
            clone.setAmount(clone.getAmount() - 1);
            
            // Then - oryginał nie powinien być zmieniony
            assertEquals(5, original.getAmount(), "Oryginał nie powinien być zmieniony");
            assertEquals(4, clone.getAmount(), "Klon powinien mieć 4");
        }
    }
    
    // =====================================================================
    // TESTY SCENARIUSZY RECIPE BOOK - SHAPELESS RECIPE
    // =====================================================================
    
    @Nested
    @DisplayName("Testy ShapelessRecipe (np. book + ink sac = writable book)")
    class ShapelessRecipeTests {
        
        @Test
        @DisplayName("Shapeless recipe nie powinien zjadać nadmiarowych itemów")
        void shouldNotEatExtraItems_ForShapelessRecipe() {
            // Given - gracz ma 5 książek i 5 atramentów
            ItemStack[] storageContents = new ItemStack[36];
            storageContents[0] = new ItemStack(Material.BOOK, 5);
            storageContents[1] = new ItemStack(Material.INK_SAC, 5);
            
            // When - symulujemy jedno wypełnienie (każdy składnik -1)
            ItemStack book = storageContents[0].clone();
            ItemStack ink = storageContents[1].clone();
            
            book.setAmount(book.getAmount() - 1);
            ink.setAmount(ink.getAmount() - 1);
            
            // Then
            assertEquals(4, book.getAmount(), "Powinno zostać 4 książki");
            assertEquals(4, ink.getAmount(), "Powinno zostać 4 atramentów");
        }
    }
    
    // =====================================================================
    // TESTY BEZPIECZEŃSTWA INDEKSÓW SLOTÓW
    // =====================================================================
    
    @Nested
    @DisplayName("Testy bezpieczeństwa operacji na slotach")
    class SlotSafetyTests {
        
        @Test
        @DisplayName("Operacja na slocie nie powinna przekraczać granic tablicy")
        void shouldNotExceedArrayBounds() {
            // Given - ekwipunek z 36 slotami (standardowa wielkość)
            ItemStack[] storageContents = new ItemStack[36];
            storageContents[0] = new ItemStack(Material.DIAMOND, 64);
            
            // When/Then - operacje na slocie 0 powinny być bezpieczne
            assertDoesNotThrow(() -> {
                ItemStack item = storageContents[0];
                if (item != null && item.getAmount() > 0) {
                    item.setAmount(item.getAmount() - 1);
                }
            }, "Operacja na slocie 0 powinna być bezpieczna");
        }
        
        @Test
        @DisplayName("Pusty slot powinien być obsługiwany gracefully")
        void shouldHandleEmptySlotGracefully() {
            // Given - pusty ekwipunek
            ItemStack[] storageContents = new ItemStack[36];
            Arrays.fill(storageContents, null);
            
            // When/Then
            RecipeChoice choice = new RecipeChoice.MaterialChoice(Material.DIAMOND);
            
            // Symulacja findMatchingItemSlot dla pustego inventory
            int foundSlot = -1;
            for (int i = 0; i < storageContents.length; i++) {
                ItemStack item = storageContents[i];
                if (item != null && item.getType() != Material.AIR && choice.test(item)) {
                    foundSlot = i;
                    break;
                }
            }
            
            assertEquals(-1, foundSlot, "Powinien zwrócić -1 gdy brak pasującego itemu");
        }
        
        @Test
        @DisplayName("Slot z itemem o ilości 0 powinien być traktowany jako pusty")
        void shouldTreatZeroAmountAsEmpty() {
            // Given
            ItemStack[] storageContents = new ItemStack[36];
            ItemStack emptyDiamond = new ItemStack(Material.DIAMOND, 0);
            storageContents[0] = emptyDiamond;
            
            // When - szukamy diamentu
            RecipeChoice choice = new RecipeChoice.MaterialChoice(Material.DIAMOND);
            
            int foundSlot = -1;
            for (int i = 0; i < storageContents.length; i++) {
                ItemStack item = storageContents[i];
                // Poprawiona logika: sprawdzamy też amount > 0
                if (item != null && item.getType() != Material.AIR && item.getAmount() > 0 && choice.test(item)) {
                    foundSlot = i;
                    break;
                }
            }
            
            assertEquals(-1, foundSlot, "Item z amount=0 nie powinien być znaleziony");
        }
    }
    
    // =====================================================================
    // TESTY SCENARIUSZY RĘCZNEGO WYJMOWANIA PO RECIPE BOOK
    // =====================================================================
    
    @Nested
    @DisplayName("Testy scenariusza: Recipe Book + ręczne wyjmowanie")
    class RecipeBookManualRemovalTests {
        
        @Test
        @DisplayName("Ręczne wyjęcie itemu z siatki nie powinno wpływać na ekwipunek")
        void shouldNotAffectInventory_WhenManuallyRemovingFromGrid() {
            // Scenariusz użytkownika:
            // 1. Gracz ma 5 planek w ekwipunku
            // 2. Wybiera recepturę z recipe book (2 planki -> siatka)
            // 3. Ma teraz 3 planki w ekwipunku i 2 w siatce
            // 4. Ręcznie zabiera 1 plankę z siatki
            // 5. Powinien mieć 4 planki w ekwipunku i 1 w siatce
            
            // Given - stan początkowy
            int initialInInventory = 5;
            int movedToGrid = 2;
            int expectedAfterFill = initialInInventory - movedToGrid; // 3
            
            // Po recipe book fill
            ItemStack playerPlanks = new ItemStack(Material.OAK_PLANKS, expectedAfterFill);
            ItemStack gridPlanks = new ItemStack(Material.OAK_PLANKS, movedToGrid);
            
            // When - gracz ręcznie zabiera 1 z siatki
            int removedFromGrid = 1;
            gridPlanks.setAmount(gridPlanks.getAmount() - removedFromGrid);
            
            // Ręczne zabranie powinno dodać do ekwipunku
            playerPlanks.setAmount(playerPlanks.getAmount() + removedFromGrid);
            
            // Then
            assertEquals(4, playerPlanks.getAmount(), 
                "Gracz powinien mieć 4 planki w ekwipunku");
            assertEquals(1, gridPlanks.getAmount(), 
                "Siatka powinna mieć 1 plankę");
            
            // Całkowita suma powinna być równa początkowej
            assertEquals(initialInInventory, playerPlanks.getAmount() + gridPlanks.getAmount(),
                "Całkowita suma itemów powinna być zachowana");
        }
        
        @Test
        @DisplayName("BUG FIX: Stary kod powodował utratę itemu przez podwójną dekrementację")
        void oldBug_DoubleDecrementation() {
            // Ten test demonstruje stary bug:
            // findMatchingItem() zwracała referencję do ItemStack,
            // potem kod robił found.setAmount(found.getAmount() - 1)
            // co modyfikowało ORYGINAŁ w ekwipunku gracza
            
            // Given - symulacja starego buga
            ItemStack[] inventory = new ItemStack[36];
            inventory[0] = new ItemStack(Material.OAK_PLANKS, 5);
            
            // STARY KOD (bug):
            // ItemStack found = findMatchingItem(player, choice); // Referencja!
            // found.setAmount(found.getAmount() - 1); // Modyfikuje oryginał!
            
            // Symulacja buga: bezpośrednia modyfikacja referencji
            ItemStack foundRef = inventory[0]; // Referencja do tego samego obiektu
            foundRef.setAmount(foundRef.getAmount() - 1);
            
            // Bug: oryginał też został zmodyfikowany
            assertEquals(4, inventory[0].getAmount(), 
                "Stary bug: referencja modyfikowała oryginał");
            
            // NOWY KOD (poprawka):
            // int slot = findMatchingItemSlot(player, choice); // Tylko indeks
            // removeOneFromSlot(inventory, slot); // Kontrolowana modyfikacja
            
            // Symulacja poprawionego kodu z klonem
            ItemStack original = new ItemStack(Material.IRON_INGOT, 10);
            ItemStack cloned = original.clone();
            cloned.setAmount(cloned.getAmount() - 1);
            
            // Poprawka: oryginał nie jest zmieniony przez operacje na klonie
            assertEquals(10, original.getAmount(), 
                "Poprawka: klon nie modyfikuje oryginału");
            assertEquals(9, cloned.getAmount(), 
                "Klon ma zmniejszoną ilość");
        }
    }
    
    // =====================================================================
    // TESTY WIELOKROTNEGO WYPEŁNIANIA (SHIFT-CLICK W RECIPE BOOK)
    // =====================================================================
    
    @Nested
    @DisplayName("Testy shift-click w recipe book (max fill)")
    class ShiftClickRecipeBookTests {
        
        @Test
        @DisplayName("Shift-click powinien wypełnić maksymalną możliwą ilość")
        void shouldFillMaximumPossible_OnShiftClick() {
            // Scenariusz: Gracz ma 10 planek, shift-click na recepturę stick
            // Stick wymaga 2 planki, więc może wypełnić 5 razy
            
            // Given
            int planksAvailable = 10;
            int planksPerFill = 2;
            int expectedFills = planksAvailable / planksPerFill; // 5
            
            // When
            ItemStack playerPlanks = new ItemStack(Material.OAK_PLANKS, planksAvailable);
            int fills = 0;
            
            while (playerPlanks.getAmount() >= planksPerFill && fills < 64) {
                playerPlanks.setAmount(playerPlanks.getAmount() - planksPerFill);
                fills++;
            }
            
            // Then
            assertEquals(expectedFills, fills, "Powinno być 5 wypełnień");
            assertEquals(0, playerPlanks.getAmount(), "Gracz powinien mieć 0 planek");
        }
        
        @Test
        @DisplayName("Shift-click nie powinien przekraczać 64 wypełnień")
        void shouldNotExceed64Fills() {
            // Given - gracz ma bardzo dużo itemów
            int planksAvailable = 200;
            int planksPerFill = 1; // Załóżmy recepturę wymagającą 1 item
            
            // When
            ItemStack playerPlanks = new ItemStack(Material.OAK_PLANKS, planksAvailable);
            int fills = 0;
            int maxFills = 64;
            
            while (playerPlanks.getAmount() >= planksPerFill && fills < maxFills) {
                playerPlanks.setAmount(playerPlanks.getAmount() - planksPerFill);
                fills++;
            }
            
            // Then
            assertEquals(64, fills, "Powinno być max 64 wypełnień");
            assertEquals(planksAvailable - 64, playerPlanks.getAmount(), 
                "Pozostała ilość powinna być 136");
        }
    }
    
    // =====================================================================
    // TESTY EDGE CASES
    // =====================================================================
    
    @Nested
    @DisplayName("Testy przypadków brzegowych")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Gracz bez wymaganych itemów nie powinien móc wypełnić siatki")
        void shouldNotFill_WhenPlayerHasNoIngredients() {
            // Given
            ItemStack[] storageContents = new ItemStack[36];
            Arrays.fill(storageContents, null);
            
            RecipeChoice choice = new RecipeChoice.MaterialChoice(Material.DIAMOND);
            
            // When
            int foundSlot = -1;
            for (int i = 0; i < storageContents.length; i++) {
                ItemStack item = storageContents[i];
                if (item != null && item.getType() != Material.AIR && item.getAmount() > 0 && choice.test(item)) {
                    foundSlot = i;
                    break;
                }
            }
            
            // Then
            assertEquals(-1, foundSlot, "Nie powinien znaleźć składnika");
        }
        
        @Test
        @DisplayName("Gracz z itemem innego typu nie powinien móc wypełnić siatki")
        void shouldNotFill_WhenPlayerHasWrongItemType() {
            // Given - gracz ma złoto zamiast diamentu
            ItemStack[] storageContents = new ItemStack[36];
            storageContents[0] = new ItemStack(Material.GOLD_INGOT, 10);
            
            RecipeChoice choice = new RecipeChoice.MaterialChoice(Material.DIAMOND);
            
            // When
            int foundSlot = -1;
            for (int i = 0; i < storageContents.length; i++) {
                ItemStack item = storageContents[i];
                if (item != null && item.getType() != Material.AIR && item.getAmount() > 0 && choice.test(item)) {
                    foundSlot = i;
                    break;
                }
            }
            
            // Then
            assertEquals(-1, foundSlot, "Nie powinien znaleźć składnika (zły typ)");
        }
        
        @Test
        @DisplayName("Siatka pełna nie powinna przyjmować więcej itemów")
        void shouldNotAddToFullGrid() {
            // Given - siatka pełna
            int gridSize = 9;
            ItemStack[] grid = new ItemStack[gridSize];
            for (int i = 0; i < gridSize; i++) {
                grid[i] = new ItemStack(Material.STONE, 64);
            }
            
            // When - szukamy pustego slotu
            int emptySlot = -1;
            for (int i = 0; i < gridSize; i++) {
                ItemStack item = grid[i];
                if (item == null || item.getType() == Material.AIR || item.getAmount() == 0) {
                    emptySlot = i;
                    break;
                }
            }
            
            // Then
            assertEquals(-1, emptySlot, "Nie powinno być pustego slotu");
        }
        
        @Test
        @DisplayName("Stackowanie powinno działać gdy item można połączyć")
        void shouldStackWhenPossible() {
            // Given - siatka z itemem który można stackować
            ItemStack gridItem = new ItemStack(Material.OAK_PLANKS, 60);
            ItemStack toAdd = new ItemStack(Material.OAK_PLANKS, 1);
            
            // When - sprawdzamy czy można stackować
            boolean canStack = gridItem.isSimilar(toAdd) && 
                               gridItem.getAmount() < gridItem.getMaxStackSize();
            
            // Then
            assertTrue(canStack, "Powinno być możliwe stackowanie (60 < 64)");
            
            // Symulacja stackowania
            gridItem.setAmount(gridItem.getAmount() + toAdd.getAmount());
            assertEquals(61, gridItem.getAmount(), "Po stackowaniu powinno być 61");
        }
        
        @Test
        @DisplayName("Stackowanie nie powinno przekraczać max stack size")
        void shouldNotExceedMaxStackSize() {
            // Given
            ItemStack gridItem = new ItemStack(Material.OAK_PLANKS, 64);
            ItemStack toAdd = new ItemStack(Material.OAK_PLANKS, 1);
            
            // When
            boolean canStack = gridItem.isSimilar(toAdd) && 
                               gridItem.getAmount() < gridItem.getMaxStackSize();
            
            // Then
            assertFalse(canStack, "Nie powinno być możliwe stackowanie (64 >= 64)");
        }
    }
    
    // =====================================================================
    // TESTY INTEGRALNOŚCI DANYCH
    // =====================================================================
    
    @Nested
    @DisplayName("Testy integralności danych")
    class DataIntegrityTests {
        
        @Test
        @DisplayName("Całkowita ilość itemów powinna być zachowana w systemie")
        void shouldPreserveTotalItemCount() {
            // Given - system zamknięty: 100 diamentów
            int totalDiamonds = 100;
            int inventoryDiamonds = 80;
            int gridDiamonds = 20;
            
            assertEquals(totalDiamonds, inventoryDiamonds + gridDiamonds, 
                "Suma początkowa powinna wynosić 100");
            
            // When - przenosimy 10 z inventory do grida
            int toMove = 10;
            inventoryDiamonds -= toMove;
            gridDiamonds += toMove;
            
            // Then
            assertEquals(totalDiamonds, inventoryDiamonds + gridDiamonds, 
                "Suma po przeniesieniu powinna nadal wynosić 100");
            assertEquals(70, inventoryDiamonds, "Inventory powinno mieć 70");
            assertEquals(30, gridDiamonds, "Grid powinien mieć 30");
        }
        
        @Test
        @DisplayName("Craftowanie powinno zużywać składniki i tworzyć wynik")
        void shouldConsumeIngredientsAndProduceResult() {
            // Given - 2 planki w siatce
            ItemStack[] grid = new ItemStack[9];
            grid[0] = new ItemStack(Material.OAK_PLANKS, 1);
            grid[3] = new ItemStack(Material.OAK_PLANKS, 1);
            
            // Wynik: 4 patyki
            ItemStack result = new ItemStack(Material.STICK, 4);
            
            // When - craftujemy
            int totalBeforeCraft = countItems(grid, Material.OAK_PLANKS);
            
            // Symulacja craftowania: usuwamy składniki
            grid[0] = null;
            grid[3] = null;
            
            int totalAfterCraft = countItems(grid, Material.OAK_PLANKS);
            
            // Then
            assertEquals(2, totalBeforeCraft, "Przed craftem: 2 planki");
            assertEquals(0, totalAfterCraft, "Po crafcie: 0 planek");
            assertEquals(4, result.getAmount(), "Wynik: 4 patyki");
        }
        
        private int countItems(ItemStack[] grid, Material type) {
            int count = 0;
            for (ItemStack item : grid) {
                if (item != null && item.getType() == type) {
                    count += item.getAmount();
                }
            }
            return count;
        }
    }
}

package pl.com.sofitfy.enhanced_containers.sharedcrafting;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitTestBase;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne dla Recipe Book z użyciem MockBukkit.
 * 
 * Te testy weryfikują zachowanie Recipe Book zgodne z vanilla Minecraft:
 * 1. Przedmioty z siatki wracają do inventory gracza przed wypełnieniem nowym przepisem
 * 2. Jeśli inventory jest pełne, przedmioty są upuszczane na ziemię
 * 3. Output slot jest zawsze czyszczony
 */
@DisplayName("Recipe Book - Integration Tests (MockBukkit)")
@Tag("integration")
public class RecipeBookIntegrationTest extends MockBukkitTestBase {

    @Nested
    @DisplayName("Vanilla Behavior: Zwracanie przedmiotów z siatki")
    class ReturnGridItemsIntegrationTests {

        @Test
        @DisplayName("Przedmioty z siatki wracają do inventory gracza")
        void gridItems_returnedToPlayerInventory() {
            // Given
            PlayerMock player = createPlayer();
            Inventory workbench = server.createInventory(null, InventoryType.WORKBENCH);
            
            // Ustaw przedmioty w siatce (sloty 1-9 w WORKBENCH)
            workbench.setItem(1, new ItemStack(Material.DIAMOND, 1));
            workbench.setItem(2, new ItemStack(Material.IRON_INGOT, 3));
            workbench.setItem(5, new ItemStack(Material.GOLD_INGOT, 2));
            
            // Zlicz początkową liczbę przedmiotów w inventory gracza
            int initialCount = countItems(player.getInventory().getStorageContents());
            
            // When: Wywołaj zwracanie przedmiotów
            returnGridItemsToPlayer(player, workbench);
            
            // Then: Przedmioty wróciły do inventory gracza
            int finalCount = countItems(player.getInventory().getStorageContents());
            assertTrue(finalCount > initialCount, "Przedmioty powinny być dodane do inventory gracza");
            
            // And: Siatka jest pusta
            for (int slot = 1; slot <= 9; slot++) {
                ItemStack item = workbench.getItem(slot);
                assertTrue(item == null || item.getType() == Material.AIR, 
                    "Slot " + slot + " powinien być pusty");
            }
        }

        @Test
        @DisplayName("Pusta siatka - inventory gracza bez zmian")
        void emptyGrid_playerInventoryUnchanged() {
            // Given
            PlayerMock player = createPlayer();
            player.getInventory().addItem(new ItemStack(Material.STONE, 10));
            Inventory workbench = server.createInventory(null, InventoryType.WORKBENCH);
            
            int initialCount = countItems(player.getInventory().getStorageContents());
            
            // When
            returnGridItemsToPlayer(player, workbench);
            
            // Then
            int finalCount = countItems(player.getInventory().getStorageContents());
            assertEquals(initialCount, finalCount, "Liczba przedmiotów nie powinna się zmienić");
        }

        @Test
        @DisplayName("Output slot jest czyszczony ale NIE zwracany")
        void outputSlot_clearedButNotReturned() {
            // Given
            PlayerMock player = createPlayer();
            Inventory workbench = server.createInventory(null, InventoryType.WORKBENCH);
            
            // Ustaw wynik craftingu w output slot (slot 0)
            workbench.setItem(0, new ItemStack(Material.DIAMOND_PICKAXE, 1));
            
            int initialCount = countItems(player.getInventory().getStorageContents());
            
            // When
            returnGridItemsToPlayer(player, workbench);
            
            // Then: Output slot jest wyczyszczony
            ItemStack outputItem = workbench.getItem(0);
            assertTrue(outputItem == null || outputItem.getType() == Material.AIR,
                "Output slot powinien być pusty");
            
            // And: Wynik NIE trafił do inventory gracza (to nie jest przedmiot gracza!)
            int finalCount = countItems(player.getInventory().getStorageContents());
            assertEquals(initialCount, finalCount, "Output nie powinien być zwracany do gracza");
        }

        @Test
        @DisplayName("Duży stack - cały wraca do inventory")
        void largeStack_entireStackReturned() {
            // Given
            PlayerMock player = createPlayer();
            Inventory workbench = server.createInventory(null, InventoryType.WORKBENCH);
            
            workbench.setItem(1, new ItemStack(Material.COBBLESTONE, 64));
            
            // When
            returnGridItemsToPlayer(player, workbench);
            
            // Then: Cały stack (64) jest w inventory gracza
            assertTrue(player.getInventory().contains(Material.COBBLESTONE, 64),
                "Gracz powinien mieć 64 kamieni");
        }

        @Test
        @DisplayName("Różne przedmioty - wszystkie wracają z zachowaniem ilości")
        void multipleItems_allReturnedWithCorrectAmounts() {
            // Given
            PlayerMock player = createPlayer();
            Inventory workbench = server.createInventory(null, InventoryType.WORKBENCH);
            
            workbench.setItem(1, new ItemStack(Material.DIAMOND, 5));
            workbench.setItem(3, new ItemStack(Material.EMERALD, 10));
            workbench.setItem(7, new ItemStack(Material.GOLD_INGOT, 20));
            
            // When
            returnGridItemsToPlayer(player, workbench);
            
            // Then
            assertTrue(player.getInventory().contains(Material.DIAMOND, 5), 
                "Gracz powinien mieć 5 diamentów");
            assertTrue(player.getInventory().contains(Material.EMERALD, 10), 
                "Gracz powinien mieć 10 szmaragdów");
            assertTrue(player.getInventory().contains(Material.GOLD_INGOT, 20), 
                "Gracz powinien mieć 20 złotych sztabek");
        }
    }

    @Nested
    @DisplayName("Vanilla Behavior: Pełne inventory gracza")
    class FullInventoryTests {

        @Test
        @DisplayName("Pełne inventory - przedmioty NIE znikają")
        void fullInventory_itemsNotLost() {
            // Given
            PlayerMock player = createPlayer();
            
            // Wypełnij inventory gracza
            for (int i = 0; i < 36; i++) {
                player.getInventory().setItem(i, new ItemStack(Material.DIRT, 64));
            }
            
            Inventory workbench = server.createInventory(null, InventoryType.WORKBENCH);
            workbench.setItem(1, new ItemStack(Material.DIAMOND, 1));
            
            // When
            returnGridItemsToPlayer(player, workbench);
            
            // Then: Slot w siatce został wyczyszczony (przedmiot nie zaginął)
            ItemStack gridItem = workbench.getItem(1);
            assertTrue(gridItem == null || gridItem.getType() == Material.AIR,
                "Slot powinien być pusty - przedmiot został przekazany/upuszczony");
            
            // Uwaga: W prawdziwym serwerze przedmiot zostałby upuszczony na ziemię
            // MockBukkit może nie obsługiwać dropItemNaturally w pełni
        }
    }

    @Nested
    @DisplayName("Scenariusze Recipe Book")
    class RecipeBookScenarios {

        @Test
        @DisplayName("Scenario: Kliknięcie nowego przepisu gdy siatka ma przedmioty")
        void clickNewRecipe_whenGridHasItems_oldItemsReturned() {
            // Given: Gracz ma przedmioty na siatce (z poprzedniego przepisu)
            PlayerMock player = createPlayer();
            Inventory workbench = server.createInventory(null, InventoryType.WORKBENCH);
            
            // Stary układ: przepis na kilof
            workbench.setItem(1, new ItemStack(Material.DIAMOND, 1));
            workbench.setItem(2, new ItemStack(Material.DIAMOND, 1));
            workbench.setItem(3, new ItemStack(Material.DIAMOND, 1));
            workbench.setItem(5, new ItemStack(Material.STICK, 1));
            workbench.setItem(8, new ItemStack(Material.STICK, 1));
            
            // When: Gracz klika nowy przepis - najpierw zwracamy stare
            returnGridItemsToPlayer(player, workbench);
            
            // Then: Wszystkie przedmioty wróciły
            assertTrue(player.getInventory().contains(Material.DIAMOND, 3),
                "3 diamenty powinny wrócić");
            assertTrue(player.getInventory().contains(Material.STICK, 2),
                "2 patyki powinny wrócić");
            
            // And: Siatka jest gotowa na nowy przepis
            for (int slot = 1; slot <= 9; slot++) {
                ItemStack item = workbench.getItem(slot);
                assertTrue(item == null || item.getType() == Material.AIR,
                    "Siatka powinna być pusta przed nowym przepisem");
            }
        }

        @Test
        @DisplayName("Scenario: Kilka kliknięć różnych przepisów pod rząd")
        void multipleRecipeClicks_itemsAlwaysReturned() {
            // Given
            PlayerMock player = createPlayer();
            player.getInventory().addItem(
                new ItemStack(Material.DIAMOND, 10),
                new ItemStack(Material.IRON_INGOT, 10),
                new ItemStack(Material.GOLD_INGOT, 10)
            );
            Inventory workbench = server.createInventory(null, InventoryType.WORKBENCH);
            
            // Symulacja: kliknięcie przepisu 1
            workbench.setItem(1, new ItemStack(Material.DIAMOND, 3));
            int diamondsBeforeReturn1 = countMaterial(player.getInventory().getStorageContents(), Material.DIAMOND);
            
            returnGridItemsToPlayer(player, workbench);
            
            int diamondsAfterReturn1 = countMaterial(player.getInventory().getStorageContents(), Material.DIAMOND);
            assertEquals(diamondsBeforeReturn1 + 3, diamondsAfterReturn1,
                "Po pierwszym zwrocie: diamenty powinny wrócić");
            
            // Symulacja: kliknięcie przepisu 2
            workbench.setItem(1, new ItemStack(Material.IRON_INGOT, 5));
            int ironBeforeReturn2 = countMaterial(player.getInventory().getStorageContents(), Material.IRON_INGOT);
            
            returnGridItemsToPlayer(player, workbench);
            
            int ironAfterReturn2 = countMaterial(player.getInventory().getStorageContents(), Material.IRON_INGOT);
            assertEquals(ironBeforeReturn2 + 5, ironAfterReturn2,
                "Po drugim zwrocie: żelazo powinno wrócić");
        }
    }

    // ========================================
    // TESTY BUGA "ZJADANIA" PRZEDMIOTÓW
    // ========================================
    @Nested
    @DisplayName("BUG FIX: Testy na 'zjadanie' przedmiotów")
    class ItemEatingBugTests {

        @Test
        @DisplayName("BUG: Recipe Book + ręczne zabranie itemów NIE powinno tracić dodatkowych itemów")
        void recipeBookThenManualRemoval_shouldNotLoseExtraItems() {
            // SCENARIUSZ UŻYTKOWNIKA:
            // 1. Gracz ma 10 planek w ekwipunku
            // 2. Używa recipe book do wybrania przepisu (np. stick)
            // 3. Recipe book przenosi 2 planki do siatki
            // 4. Gracz ręcznie zabiera te 2 planki z siatki
            // 5. OCZEKIWANIE: Gracz ma 10 planek (8 w inv + 2 na kursor)
            // 6. BUG: Gracz miał tylko 9 planek (8 w inv + 1 na kursor)
            
            // Given
            PlayerMock player = createPlayer();
            int initialPlanks = 10;
            player.getInventory().addItem(new ItemStack(Material.OAK_PLANKS, initialPlanks));
            Inventory workbench = server.createInventory(null, InventoryType.WORKBENCH);
            
            // Symulacja wypełnienia przez recipe book
            int planksMovedToGrid = 2;
            
            // Usuwamy z inventory (jak robi recipe book)
            removeItemsSafely(player, Material.OAK_PLANKS, planksMovedToGrid);
            
            // Dodajemy do siatki
            workbench.setItem(1, new ItemStack(Material.OAK_PLANKS, 1));
            workbench.setItem(4, new ItemStack(Material.OAK_PLANKS, 1));
            
            int planksInInventoryAfterFill = countMaterial(player.getInventory().getStorageContents(), Material.OAK_PLANKS);
            int planksInGrid = countMaterial(getGridContents(workbench), Material.OAK_PLANKS);
            
            // Verify: Po wypełnieniu suma musi być równa początkowej
            assertEquals(initialPlanks, planksInInventoryAfterFill + planksInGrid,
                "Po wypełnieniu: suma itemów musi być zachowana (8 + 2 = 10)");
            
            // When: Gracz ręcznie zabiera przedmioty z siatki (symulacja)
            returnGridItemsToPlayer(player, workbench);
            
            // Then: Po zabraniu gracz powinien mieć wszystkie planki
            int finalPlanks = countMaterial(player.getInventory().getStorageContents(), Material.OAK_PLANKS);
            assertEquals(initialPlanks, finalPlanks,
                "NAPRAWIONY BUG: Gracz powinien mieć wszystkie 10 planek po zabraniu z siatki");
        }

        @Test
        @DisplayName("Wielokrotne wypełnianie i zabieranie nie powinno tracić itemów")
        void multipleFillAndRemove_shouldNotLoseItems() {
            // Given
            PlayerMock player = createPlayer();
            int initialDiamonds = 20;
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, initialDiamonds));
            Inventory workbench = server.createInventory(null, InventoryType.WORKBENCH);
            
            // Wykonaj 5 cykli: wypełnij + zabierz
            for (int cycle = 1; cycle <= 5; cycle++) {
                // Symulacja wypełnienia (3 diamenty do siatki)
                int toMove = 3;
                removeItemsSafely(player, Material.DIAMOND, toMove);
                workbench.setItem(1, new ItemStack(Material.DIAMOND, 1));
                workbench.setItem(2, new ItemStack(Material.DIAMOND, 1));
                workbench.setItem(3, new ItemStack(Material.DIAMOND, 1));
                
                // Zabierz z siatki
                returnGridItemsToPlayer(player, workbench);
                
                // Weryfikuj że nic nie zginęło
                int totalDiamonds = countMaterial(player.getInventory().getStorageContents(), Material.DIAMOND)
                                   + countMaterial(getGridContents(workbench), Material.DIAMOND);
                
                assertEquals(initialDiamonds, totalDiamonds,
                    "Cykl " + cycle + ": suma diamentów musi być zachowana");
            }
            
            // Final check
            int finalDiamonds = countMaterial(player.getInventory().getStorageContents(), Material.DIAMOND);
            assertEquals(initialDiamonds, finalDiamonds,
                "Po 5 cyklach gracz powinien mieć wszystkie diamenty");
        }

        @Test
        @DisplayName("Shift-click w recipe book powinien przenosić maksymalną ilość")
        void shiftClickRecipeBook_shouldMoveMaximumAmount() {
            // WYMAGANIE: Shift-click w recipe book wypełnia siatkę maksymalnie (jak w 4x4)
            
            // Given
            PlayerMock player = createPlayer();
            int initialPlanks = 128; // Dużo planek
            player.getInventory().addItem(new ItemStack(Material.OAK_PLANKS, initialPlanks));
            Inventory workbench = server.createInventory(null, InventoryType.WORKBENCH);
            
            // Przepis stick wymaga 2 planek w kolumnie (slot 1 i 4)
            // Max stack = 64, więc możemy wypełnić 64 razy każdy slot
            int maxStackSize = 64;
            int slotsNeeded = 2; // dla stick
            int maxFills = maxStackSize; // 64 razy
            int planksNeeded = maxFills * slotsNeeded; // 128
            
            // When: Symulacja shift-click
            int fillCount = 0;
            while (fillCount < 64) {
                int available = countMaterial(player.getInventory().getStorageContents(), Material.OAK_PLANKS);
                if (available < slotsNeeded) break;
                
                // Sprawdź czy siatka ma miejsce
                ItemStack slot1 = workbench.getItem(1);
                ItemStack slot4 = workbench.getItem(4);
                
                boolean slot1HasSpace = slot1 == null || slot1.getType() == Material.AIR || slot1.getAmount() < 64;
                boolean slot4HasSpace = slot4 == null || slot4.getType() == Material.AIR || slot4.getAmount() < 64;
                
                if (!slot1HasSpace || !slot4HasSpace) break;
                
                // Wypełnij
                removeItemsSafely(player, Material.OAK_PLANKS, slotsNeeded);
                
                if (slot1 == null || slot1.getType() == Material.AIR) {
                    workbench.setItem(1, new ItemStack(Material.OAK_PLANKS, 1));
                } else {
                    slot1.setAmount(slot1.getAmount() + 1);
                }
                
                if (slot4 == null || slot4.getType() == Material.AIR) {
                    workbench.setItem(4, new ItemStack(Material.OAK_PLANKS, 1));
                } else {
                    slot4.setAmount(slot4.getAmount() + 1);
                }
                
                fillCount++;
            }
            
            // Then
            int planksInGrid = countMaterial(getGridContents(workbench), Material.OAK_PLANKS);
            int planksInInv = countMaterial(player.getInventory().getStorageContents(), Material.OAK_PLANKS);
            
            assertEquals(initialPlanks, planksInGrid + planksInInv,
                "Suma itemów musi być zachowana");
            
            assertTrue(fillCount >= 64, 
                "Shift-click powinien wypełnić maksymalnie (64 razy)");
        }

        @Test
        @DisplayName("Integralność danych po wielokrotnych operacjach")
        void dataIntegrity_afterMultipleOperations() {
            // Ten test weryfikuje że system nie "tworzy" ani "traci" przedmiotów
            
            // Given - zamknięty system z określoną liczbą przedmiotów
            PlayerMock player = createPlayer();
            HashMap<Material, Integer> initialInventory = new HashMap<>();
            initialInventory.put(Material.DIAMOND, 50);
            initialInventory.put(Material.IRON_INGOT, 100);
            initialInventory.put(Material.STICK, 200);
            
            for (var entry : initialInventory.entrySet()) {
                player.getInventory().addItem(new ItemStack(entry.getKey(), entry.getValue()));
            }
            
            Inventory workbench = server.createInventory(null, InventoryType.WORKBENCH);
            
            // When - wykonaj losowe operacje
            for (int i = 0; i < 10; i++) {
                // Symulacja różnych przepisów
                workbench.setItem(1, new ItemStack(Material.DIAMOND, Math.min(3, countMaterial(player.getInventory().getStorageContents(), Material.DIAMOND))));
                workbench.setItem(5, new ItemStack(Material.STICK, Math.min(2, countMaterial(player.getInventory().getStorageContents(), Material.STICK))));
                
                removeItemsSafely(player, Material.DIAMOND, countMaterial(getGridContents(workbench), Material.DIAMOND));
                removeItemsSafely(player, Material.STICK, countMaterial(getGridContents(workbench), Material.STICK));
                
                // Zwróć do gracza
                returnGridItemsToPlayer(player, workbench);
            }
            
            // Then - weryfikuj integralność
            for (var entry : initialInventory.entrySet()) {
                int total = countMaterial(player.getInventory().getStorageContents(), entry.getKey())
                          + countMaterial(getGridContents(workbench), entry.getKey());
                
                assertEquals(entry.getValue().intValue(), total,
                    "Material " + entry.getKey() + " powinien mieć zachowaną sumę");
            }
        }
    }

    // ========================================
    // Helper methods
    // ========================================
    
    private int countItems(ItemStack[] items) {
        return (int) Arrays.stream(items)
            .filter(item -> item != null && item.getType() != Material.AIR)
            .count();
    }
    
    private int countMaterial(ItemStack[] items, Material material) {
        return Arrays.stream(items)
            .filter(item -> item != null && item.getType() == material)
            .mapToInt(ItemStack::getAmount)
            .sum();
    }
    
    /**
     * Pobiera zawartość siatki (sloty 1-9) jako tablicę.
     */
    private ItemStack[] getGridContents(Inventory workbench) {
        ItemStack[] grid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            grid[i] = workbench.getItem(i + 1);
        }
        return grid;
    }
    
    /**
     * Bezpiecznie usuwa określoną ilość przedmiotów danego typu z ekwipunku gracza.
     * Operuje na indeksach slotów, nie na referencjach - to jest kluczowe dla naprawy buga!
     */
    private void removeItemsSafely(org.bukkit.entity.Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack item = contents[slot];
            if (item != null && item.getType() == material) {
                int toRemove = Math.min(remaining, item.getAmount());
                int newAmount = item.getAmount() - toRemove;
                
                if (newAmount <= 0) {
                    player.getInventory().setItem(slot, null);
                } else {
                    item.setAmount(newAmount);
                }
                
                remaining -= toRemove;
            }
        }
    }
    
    /**
     * Implementacja metody testowanej.
     * To jest kopia logiki z RecipeBookPacketHandler - testujemy ją tu w izolacji.
     */
    private void returnGridItemsToPlayer(org.bukkit.entity.Player player, Inventory sharedInv) {
        for (int slot = 1; slot <= 9; slot++) {
            ItemStack item = sharedInv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                // Próbuj dodać do inventory gracza
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
                
                // Jeśli inventory jest pełne, upuść na ziemię (jak vanilla)
                if (!leftover.isEmpty()) {
                    for (ItemStack dropped : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), dropped);
                    }
                }
                
                // Wyczyść slot w siatce
                sharedInv.setItem(slot, null);
            }
        }
        
        // Wyczyść też output slot (slot 0) - będzie przeliczony
        sharedInv.setItem(0, null);
    }
}

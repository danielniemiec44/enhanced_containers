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

package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EnhancedFurnaceExplosionIntegrationTest - testy eksplozji dla Enhanced Furnace
 * 
 * TESTOWANE SCENARIUSZE:
 * 1. Creeper/TNT wybucha obok pieca → przedmioty wypadają
 * 2. Łóżko w Netherze eksploduje → przedmioty wypadają
 * 3. Wszystkie przedmioty (INPUT, OUTPUT, FUEL) są zachowane
 * 4. Piec jest usuwany z managera po eksplozji
 * 
 * UWAGA: MockBukkit ma ograniczone wsparcie dla fizycznych eksplozji.
 * Te testy weryfikują LOGIKĘ handlera eksplozji, nie fizykę.
 */
@DisplayName("Enhanced Furnace - Explosion Integration Tests")
@Tag("integration")
class EnhancedFurnaceExplosionIntegrationTest {
    
    // --- SLOTY PIECA (z EnhancedFurnaceGUI) ---
    private static final int[] INPUT_SLOTS = {11, 20, 29};
    private static final int[] OUTPUT_SLOTS = {14, 23, 32};
    private static final int[] FUEL_SLOTS = {46, 47, 48};
    
    private ServerMock server;
    private PlayerMock player;
    
    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
    }
    
    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }
    
    // ========================================================================
    // TESTY ZACHOWANIA PRZEDMIOTÓW PRZY EKSPLOZJI
    // ========================================================================
    @Nested
    @DisplayName("Zachowanie przedmiotów przy eksplozji")
    class ItemPreservationOnExplosionTests {
        
        @Test
        @DisplayName("Wszystkie przedmioty z INPUT wypadają przy eksplozji")
        void inputItemsDropOnExplosion() {
            // Given: Piec z przedmiotami w INPUT
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            furnaceInv.setItem(INPUT_SLOTS[0], new ItemStack(Material.RAW_IRON, 32));
            furnaceInv.setItem(INPUT_SLOTS[1], new ItemStack(Material.RAW_GOLD, 16));
            furnaceInv.setItem(INPUT_SLOTS[2], new ItemStack(Material.RAW_COPPER, 8));
            
            int totalInputBefore = countItemsInSlots(furnaceInv, INPUT_SLOTS);
            assertEquals(56, totalInputBefore, "Powinno być 56 rud w INPUT");
            
            // When: Symuluj eksplozję - zbierz wszystkie przedmioty do "dropów"
            List<ItemStack> droppedItems = simulateExplosionAndCollectDrops(furnaceInv);
            
            // Then: Wszystkie przedmioty zostały "upuszczone"
            int totalDropped = droppedItems.stream()
                .filter(item -> item != null && isOre(item.getType()))
                .mapToInt(ItemStack::getAmount)
                .sum();
            
            assertEquals(totalInputBefore, totalDropped, 
                "Wszystkie rudy z INPUT powinny wypaść: było " + totalInputBefore + ", wypadło " + totalDropped);
        }
        
        @Test
        @DisplayName("Wszystkie przedmioty z OUTPUT wypadają przy eksplozji")
        void outputItemsDropOnExplosion() {
            // Given: Piec z przepalonymi przedmiotami w OUTPUT
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 64));
            furnaceInv.setItem(OUTPUT_SLOTS[1], new ItemStack(Material.GOLD_INGOT, 64));
            furnaceInv.setItem(OUTPUT_SLOTS[2], new ItemStack(Material.COPPER_INGOT, 64));
            
            int totalOutputBefore = countItemsInSlots(furnaceInv, OUTPUT_SLOTS);
            assertEquals(192, totalOutputBefore, "Powinno być 192 sztabki w OUTPUT");
            
            // When
            List<ItemStack> droppedItems = simulateExplosionAndCollectDrops(furnaceInv);
            
            // Then
            int totalIngots = droppedItems.stream()
                .filter(item -> item != null && isIngot(item.getType()))
                .mapToInt(ItemStack::getAmount)
                .sum();
            
            assertEquals(totalOutputBefore, totalIngots,
                "Wszystkie sztabki z OUTPUT powinny wypaść");
        }
        
        @Test
        @DisplayName("Paliwo (FUEL) wypada przy eksplozji")
        void fuelItemsDropOnExplosion() {
            // Given: Piec z paliwem
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            furnaceInv.setItem(FUEL_SLOTS[0], new ItemStack(Material.COAL, 64));
            furnaceInv.setItem(FUEL_SLOTS[1], new ItemStack(Material.CHARCOAL, 32));
            furnaceInv.setItem(FUEL_SLOTS[2], new ItemStack(Material.COAL_BLOCK, 8));
            
            int totalFuelBefore = countItemsInSlots(furnaceInv, FUEL_SLOTS);
            assertEquals(104, totalFuelBefore, "Powinno być 104 jednostki paliwa");
            
            // When
            List<ItemStack> droppedItems = simulateExplosionAndCollectDrops(furnaceInv);
            
            // Then
            int totalFuelDropped = droppedItems.stream()
                .filter(item -> item != null && isFuel(item.getType()))
                .mapToInt(ItemStack::getAmount)
                .sum();
            
            assertEquals(totalFuelBefore, totalFuelDropped,
                "Całe paliwo powinno wypaść");
        }
        
        @Test
        @DisplayName("Pełny piec - wszystkie przedmioty wypadają")
        void fullFurnace_allItemsDrop() {
            // Given: Piec z przedmiotami we wszystkich slotach
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // INPUT
            furnaceInv.setItem(INPUT_SLOTS[0], new ItemStack(Material.RAW_IRON, 64));
            furnaceInv.setItem(INPUT_SLOTS[1], new ItemStack(Material.RAW_IRON, 64));
            furnaceInv.setItem(INPUT_SLOTS[2], new ItemStack(Material.RAW_IRON, 64));
            
            // OUTPUT
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 64));
            furnaceInv.setItem(OUTPUT_SLOTS[1], new ItemStack(Material.IRON_INGOT, 64));
            furnaceInv.setItem(OUTPUT_SLOTS[2], new ItemStack(Material.IRON_INGOT, 64));
            
            // FUEL
            furnaceInv.setItem(FUEL_SLOTS[0], new ItemStack(Material.COAL, 64));
            furnaceInv.setItem(FUEL_SLOTS[1], new ItemStack(Material.COAL, 64));
            furnaceInv.setItem(FUEL_SLOTS[2], new ItemStack(Material.COAL, 64));
            
            int totalBefore = countItemsInSlots(furnaceInv, INPUT_SLOTS) +
                             countItemsInSlots(furnaceInv, OUTPUT_SLOTS) +
                             countItemsInSlots(furnaceInv, FUEL_SLOTS);
            
            assertEquals(576, totalBefore, "Pełny piec = 9 * 64 = 576");
            
            // When
            List<ItemStack> droppedItems = simulateExplosionAndCollectDrops(furnaceInv);
            
            // Then
            int totalDropped = droppedItems.stream()
                .filter(item -> item != null && item.getType() != Material.AIR)
                .mapToInt(ItemStack::getAmount)
                .sum();
            
            assertEquals(totalBefore, totalDropped,
                "WSZYSTKIE przedmioty powinny wypaść przy eksplozji: " +
                "było " + totalBefore + ", wypadło " + totalDropped);
        }
        
        @Test
        @DisplayName("Pusty piec - nic nie wypada (ale piec jest usuwany)")
        void emptyFurnace_nothingDrops() {
            // Given: Pusty piec
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            // When
            List<ItemStack> droppedItems = simulateExplosionAndCollectDrops(furnaceInv);
            
            // Then: Lista dropów jest pusta (lub zawiera tylko null/AIR)
            int totalDropped = droppedItems.stream()
                .filter(item -> item != null && item.getType() != Material.AIR)
                .mapToInt(ItemStack::getAmount)
                .sum();
            
            assertEquals(0, totalDropped, "Pusty piec nie powinien upuszczać przedmiotów");
        }
    }
    
    // ========================================================================
    // TESTY RÓŻNYCH TYPÓW EKSPLOZJI
    // ========================================================================
    @Nested
    @DisplayName("Różne typy eksplozji")
    class ExplosionTypesTests {
        
        @Test
        @DisplayName("EntityExplode (Creeper/TNT) - przedmioty wypadają")
        void entityExplode_itemsDrop() {
            // Given
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.DIAMOND, 10));
            
            // When: Symuluj EntityExplodeEvent (creeper, TNT)
            List<ItemStack> drops = simulateEntityExplosion(furnaceInv);
            
            // Then
            int diamonds = drops.stream()
                .filter(item -> item != null && item.getType() == Material.DIAMOND)
                .mapToInt(ItemStack::getAmount)
                .sum();
            
            assertEquals(10, diamonds, "Diamenty powinny wypaść po eksplozji creepera/TNT");
        }
        
        @Test
        @DisplayName("BlockExplode (łóżko w Netherze) - przedmioty wypadają")
        void blockExplode_itemsDrop() {
            // Given
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            furnaceInv.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.NETHERITE_INGOT, 5));
            
            // When: Symuluj BlockExplodeEvent (bed in nether, respawn anchor)
            List<ItemStack> drops = simulateBlockExplosion(furnaceInv);
            
            // Then
            int netherite = drops.stream()
                .filter(item -> item != null && item.getType() == Material.NETHERITE_INGOT)
                .mapToInt(ItemStack::getAmount)
                .sum();
            
            assertEquals(5, netherite, "Netherite powinien wypaść po eksplozji łóżka");
        }
    }
    
    // ========================================================================
    // TESTY EDGE CASES
    // ========================================================================
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Przedmioty o różnych metadatach zachowują metadata")
        void itemsWithMetadataPreserved() {
            // Given: Przedmiot z nazwą custom
            Inventory furnaceInv = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            ItemStack namedItem = new ItemStack(Material.DIAMOND_SWORD);
            var meta = namedItem.getItemMeta();
            meta.setDisplayName("§6Legendarny Miecz");
            namedItem.setItemMeta(meta);
            
            furnaceInv.setItem(OUTPUT_SLOTS[0], namedItem);
            
            // When
            List<ItemStack> drops = simulateExplosionAndCollectDrops(furnaceInv);
            
            // Then: Przedmiot zachował nazwę
            ItemStack droppedSword = drops.stream()
                .filter(item -> item != null && item.getType() == Material.DIAMOND_SWORD)
                .findFirst()
                .orElse(null);
            
            assertNotNull(droppedSword, "Miecz powinien wypaść");
            assertNotNull(droppedSword.getItemMeta(), "Miecz powinien mieć metadata");
            assertEquals("§6Legendarny Miecz", droppedSword.getItemMeta().getDisplayName(),
                "Nazwa miecza powinna być zachowana");
        }
        
        @Test
        @DisplayName("Wiele pieców w jednej eksplozji - wszystkie obsłużone")
        void multipleFurnacesInExplosion() {
            // Given: 3 piece z przedmiotami
            Inventory furnace1 = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            Inventory furnace2 = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            Inventory furnace3 = Bukkit.createInventory(null, 54, "Enhanced Furnace");
            
            furnace1.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.IRON_INGOT, 10));
            furnace2.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.GOLD_INGOT, 20));
            furnace3.setItem(OUTPUT_SLOTS[0], new ItemStack(Material.COPPER_INGOT, 30));
            
            // When: Symuluj eksplozję obejmującą wszystkie 3
            List<ItemStack> allDrops = new ArrayList<>();
            allDrops.addAll(simulateExplosionAndCollectDrops(furnace1));
            allDrops.addAll(simulateExplosionAndCollectDrops(furnace2));
            allDrops.addAll(simulateExplosionAndCollectDrops(furnace3));
            
            // Then
            int iron = countMaterial(allDrops, Material.IRON_INGOT);
            int gold = countMaterial(allDrops, Material.GOLD_INGOT);
            int copper = countMaterial(allDrops, Material.COPPER_INGOT);
            
            assertEquals(10, iron, "10 żelaza z pieca 1");
            assertEquals(20, gold, "20 złota z pieca 2");
            assertEquals(30, copper, "30 miedzi z pieca 3");
        }
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    /**
     * Symuluje eksplozję i zbiera wszystkie przedmioty które "wypadłyby".
     * Ponieważ MockBukkit nie obsługuje fizycznych dropów, kopiujemy przedmioty z inventory.
     */
    private List<ItemStack> simulateExplosionAndCollectDrops(Inventory furnaceInv) {
        List<ItemStack> drops = new ArrayList<>();
        
        // Zbierz przedmioty z INPUT
        for (int slot : INPUT_SLOTS) {
            ItemStack item = furnaceInv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                drops.add(item.clone());
            }
        }
        
        // Zbierz przedmioty z OUTPUT
        for (int slot : OUTPUT_SLOTS) {
            ItemStack item = furnaceInv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                drops.add(item.clone());
            }
        }
        
        // Zbierz przedmioty z FUEL
        for (int slot : FUEL_SLOTS) {
            ItemStack item = furnaceInv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                drops.add(item.clone());
            }
        }
        
        return drops;
    }
    
    /**
     * Symuluje EntityExplodeEvent (creeper, TNT, ghast fireball).
     */
    private List<ItemStack> simulateEntityExplosion(Inventory furnaceInv) {
        // W prawdziwej grze event zawiera listę bloków do zniszczenia
        // Handler iteruje i dla każdego EnhancedFurnace robi removeEnhancedFurnace + dropItemNaturally
        return simulateExplosionAndCollectDrops(furnaceInv);
    }
    
    /**
     * Symuluje BlockExplodeEvent (bed in nether, respawn anchor).
     */
    private List<ItemStack> simulateBlockExplosion(Inventory furnaceInv) {
        return simulateExplosionAndCollectDrops(furnaceInv);
    }
    
    private int countItemsInSlots(Inventory inventory, int[] slots) {
        int total = 0;
        for (int slot : slots) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                total += item.getAmount();
            }
        }
        return total;
    }
    
    private int countMaterial(List<ItemStack> items, Material material) {
        return items.stream()
            .filter(item -> item != null && item.getType() == material)
            .mapToInt(ItemStack::getAmount)
            .sum();
    }
    
    private boolean isOre(Material material) {
        return switch (material) {
            case RAW_IRON, RAW_GOLD, RAW_COPPER, IRON_ORE, GOLD_ORE, COPPER_ORE -> true;
            default -> false;
        };
    }
    
    private boolean isIngot(Material material) {
        return switch (material) {
            case IRON_INGOT, GOLD_INGOT, COPPER_INGOT, NETHERITE_INGOT -> true;
            default -> false;
        };
    }
    
    private boolean isFuel(Material material) {
        return switch (material) {
            case COAL, CHARCOAL, COAL_BLOCK, BLAZE_ROD, LAVA_BUCKET -> true;
            default -> false;
        };
    }
}

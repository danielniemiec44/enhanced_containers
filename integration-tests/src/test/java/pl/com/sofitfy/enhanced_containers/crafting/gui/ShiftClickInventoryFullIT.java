package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne dla blokady shift+click gdy ekwipunek pełny.
 * 
 * Sprawdza tylko metodę canAddToInventory - logikę sprawdzania miejsca w EQ.
 */
@DisplayName("Shift+Click z pełnym ekwipunkiem - canAddToInventory (Integration Test)")
class ShiftClickInventoryFullIT {

    private static ServerMock server;
    private PlayerMock player;

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
        player.getInventory().clear();
    }

    @Test
    @DisplayName("canAddToInventory - pełny EQ zwraca false")
    void testFullInventory_ReturnsFalse() {
        // Given: Pełny ekwipunek (sloty 0-35)
        for (int i = 0; i < 36; i++) {
            player.getInventory().setItem(i, new ItemStack(Material.DIRT, 64));
        }
        
        // When/Then: Próba dodania nowego itemu
        assertFalse(CraftingGUIListener.canAddToInventory(player, new ItemStack(Material.DIAMOND, 1)),
            "Pełny EQ powinien zwrócić false");
    }

    @Test
    @DisplayName("canAddToInventory - pusty slot zwraca true")
    void testEmptySlot_ReturnsTrue() {
        // Given: 35/36 slotów zajętych
        for (int i = 0; i < 35; i++) {
            player.getInventory().setItem(i, new ItemStack(Material.DIRT, 64));
        }
        // slot 35 pusty
        
        // When/Then: Próba dodania itemu
        assertTrue(CraftingGUIListener.canAddToInventory(player, new ItemStack(Material.DIAMOND, 1)),
            "Jeden pusty slot powinien zwrócić true");
    }

    @Test
    @DisplayName("canAddToInventory - stackowalne itemy zwracają true")
    void testStackableItems_ReturnsTrue() {
        // Given: Slot 0 ma 60/64 diamenty
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 60));
        for (int i = 1; i < 36; i++) {
            player.getInventory().setItem(i, new ItemStack(Material.DIRT, 64));
        }
        
        // When/Then: Próba dodania 4 lub mniej diamenty
        assertTrue(CraftingGUIListener.canAddToInventory(player, new ItemStack(Material.DIAMOND, 4)),
            "Powinien zmieścić 4 diamenty do istniejącego stacka 60/64");
        assertTrue(CraftingGUIListener.canAddToInventory(player, new ItemStack(Material.DIAMOND, 1)),
            "Powinien zmieścić 1 diament do istniejącego stacka");
    }

    @Test
    @DisplayName("canAddToInventory - za dużo stackowalnych zwraca false")
    void testTooManyStackables_ReturnsFalse() {
        // Given: Slot 0 ma 60/64 diamenty, reszta pełna
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 60));
        for (int i = 1; i < 36; i++) {
            player.getInventory().setItem(i, new ItemStack(Material.DIRT, 64));
        }
        
        // When/Then: Próba dodania 5 diamenty (zmieści się tylko 4)
        assertFalse(CraftingGUIListener.canAddToInventory(player, new ItemStack(Material.DIAMOND, 5)),
            "Nie powinien zmieścić 5 diamenty gdy stack ma tylko 4 wolne miejsca");
    }

    @Test
    @DisplayName("canAddToInventory - całkowicie pusty EQ zwraca true")
    void testEmptyInventory_ReturnsTrue() {
        // Given: Całkowicie pusty EQ
        player.getInventory().clear();
        
        // When/Then:
        assertTrue(CraftingGUIListener.canAddToInventory(player, new ItemStack(Material.DIAMOND, 64)),
            "Pusty EQ powinien zwrócić true");
    }

    @Test
    @DisplayName("canAddToInventory - null/AIR zwraca true (defensive)")
    void testNullOrAir_ReturnsTrue() {
        // Given: Pełny EQ
        for (int i = 0; i < 36; i++) {
            player.getInventory().setItem(i, new ItemStack(Material.DIRT, 64));
        }
        
        // When/Then: null/AIR zawsze true (defensive)
        assertTrue(CraftingGUIListener.canAddToInventory(player, null),
            "null item powinien zwrócić true (defensive)");
        assertTrue(CraftingGUIListener.canAddToInventory(player, new ItemStack(Material.AIR)),
            "AIR item powinien zwrócić true (defensive)");
    }
}

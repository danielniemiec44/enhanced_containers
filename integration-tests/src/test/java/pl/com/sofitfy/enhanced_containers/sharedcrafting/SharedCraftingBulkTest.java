package pl.com.sofitfy.enhanced_containers.sharedcrafting;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;

import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SharedCraftingBulkTest {

    private ServerMock server;
    private SharedCraftingManager manager;
    private SharedCraftingListener listener;
    private Location tableLocation;
    private PlayerMock player;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        
        manager = new SharedCraftingManager(MockBukkit.createMockPlugin());
        listener = new SharedCraftingListener(manager);
        
        server.getPluginManager().registerEvents(listener, MockBukkit.createMockPlugin());
        
        tableLocation = new Location(server.getWorld("world"), 100, 64, 100);
        tableLocation.getBlock().setType(Material.CRAFTING_TABLE);

        player = server.addPlayer("BulkCrafter");
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testShiftClick_BulkCraft_LowIngredients() {
        // Open the shared inventory
        openSharedInventory(player);
        Inventory sharedInv = player.getOpenInventory().getTopInventory();

        // 10 "Logs" in slot 1. 
        // 4 "Planks" result in slot 0.
        ItemStack input = new ItemStack(Material.OAK_LOG, 10);
        ItemStack result = new ItemStack(Material.OAK_PLANKS, 4);

        sharedInv.setItem(1, input); 
        sharedInv.setItem(0, result); 

        assertEquals(10, sharedInv.getItem(1).getAmount());
        
        // Simulating the InventoryClickEvent manually because player.simulateClick might trigger vanilla handlers or not hit our event listener correctly in this mock environment without full inventory view setup
        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            org.bukkit.event.inventory.InventoryType.SlotType.RESULT,
            0,
            ClickType.SHIFT_LEFT,
            InventoryAction.MOVE_TO_OTHER_INVENTORY
        );
        
        // Dispatch event
        listener.onInventoryClick(event); // Direct call to ensure logic execution on our listener

        // Verification
        // Expected: 10 crafts performed.
        // Total Output: 40 Planks.
        
        int totalPlanks = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.OAK_PLANKS) {
                totalPlanks += item.getAmount();
            }
        }
        assertEquals(40, totalPlanks, "Should have crafted 40 planks (10 crafts * 4 output)");

        // Ensure ingredients are consumed
        ItemStack slot1 = sharedInv.getItem(1);
        int remain = (slot1 == null || slot1.getType() == Material.AIR) ? 0 : slot1.getAmount();
        assertEquals(0, remain, "All ingredients should be consumed");
    }

    @Test
    public void testShiftClick_BulkCraft_LimitedByInventorySpace() {
        // Open
        openSharedInventory(player);
        Inventory sharedInv = player.getOpenInventory().getTopInventory();

        // Setup: 64 oak planks -> 64 sticks (valid recipe: 2 planks in vertical stack = 4 sticks)
        // Place 64 planks in slot 1 (top-left)
        // Place 64 planks in slot 4 (middle-left)
        // This creates 4 sticks output, can craft 32 times (using 64 planks), output = 128 sticks
        
        sharedInv.setItem(1, new ItemStack(Material.OAK_PLANKS, 32)); // Top-left
        sharedInv.setItem(4, new ItemStack(Material.OAK_PLANKS, 32)); // Middle-left
        sharedInv.setItem(0, new ItemStack(Material.STICK, 4)); // Result: 4 sticks per craft
        
        // Fill player inventory completely with dirt
        for (int i = 0; i < 36; i++) {
             player.getInventory().setItem(i, new ItemStack(Material.DIRT, 64));
        }
        
        // Clear last slot - space for 64 sticks (1 stack)
        player.getInventory().setItem(35, null);
        
        // Execute bulk crafting
        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            org.bukkit.event.inventory.InventoryType.SlotType.RESULT,
            0,
            ClickType.SHIFT_LEFT,
            InventoryAction.MOVE_TO_OTHER_INVENTORY
        );
        listener.onInventoryClick(event);

        // Expectation: Can craft 16 times (consumes 32 planks) = 64 sticks (fills 1 slot)
        // Then stops because inventory full (no more space for sticks)
        int totalSticks = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.STICK) {
                totalSticks += item.getAmount();
            }
        }
        assertEquals(64, totalSticks, "Should craft 64 sticks filling the last slot");
        
        // Scenario 2: Limit by partial slot space (32 sticks already in last slot)
        // Reset player inventory
        player.getInventory().clear();
        
        // Fill 35 slots with Dirt
        for (int i = 0; i < 35; i++) {
             player.getInventory().setItem(i, new ItemStack(Material.DIRT, 64));
        }
        
        // Last slot has 32 sticks already - space for 32 more
        player.getInventory().setItem(35, new ItemStack(Material.STICK, 32));
        
        // Reset crafting table with MORE ingredients: 50 planks in each slot
        sharedInv.clear();
        sharedInv.setItem(1, new ItemStack(Material.OAK_PLANKS, 50)); // Top-left
        sharedInv.setItem(4, new ItemStack(Material.OAK_PLANKS, 50)); // Middle-left
        sharedInv.setItem(0, new ItemStack(Material.STICK, 4)); // Result: 4 sticks
        
        // Can craft 50 times (50 planks in each slot) = 200 sticks output
        // But player space is only 32 (to fill last slot to 64)
        // Should craft 8 times (8 * 4 = 32 sticks), consuming 16 planks from each slot
        
        InventoryClickEvent event2 = new InventoryClickEvent(
            player.getOpenInventory(),
            org.bukkit.event.inventory.InventoryType.SlotType.RESULT,
            0,
            ClickType.SHIFT_LEFT,
            InventoryAction.MOVE_TO_OTHER_INVENTORY
        );
        listener.onInventoryClick(event2);
        
        totalSticks = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.STICK) {
                totalSticks += item.getAmount();
            }
        }
        // Original 32 + Crafted 32 = 64
        assertEquals(64, totalSticks, "Should craft 32 more sticks to fill the slot");
        
        // Check remaining ingredients: 50 - 16 = 34 in each slot
        ItemStack slot1 = sharedInv.getItem(1);
        ItemStack slot4 = sharedInv.getItem(4);
        assertEquals(42, slot1.getAmount(), "Should have 42 planks left in slot 1 (50 - 8)");
        assertEquals(42, slot4.getAmount(), "Should have 42 planks left in slot 4 (50 - 8)");
    }

    private void openSharedInventory(Player player) {
        PlayerInteractEvent event = new PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            new ItemStack(Material.AIR),
            tableLocation.getBlock(),
            org.bukkit.block.BlockFace.UP,
            EquipmentSlot.HAND
        );
        listener.onInteract(event); // Call listener to open inv
    }
}

package pl.com.sofitfy.enhanced_containers.sharedcrafting;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SharedCraftingListenerTest {

    @Mock private SharedCraftingManager manager;
    @Mock private PlayerInteractEvent interactEvent;
    @Mock private BlockBreakEvent breakEvent;
    @Mock private EntityExplodeEvent entityExplodeEvent;
    @Mock private BlockExplodeEvent blockExplodeEvent;
    @Mock private BlockPistonExtendEvent pistonExtendEvent;
    @Mock private BlockPistonRetractEvent pistonRetractEvent;
    @Mock private InventoryClickEvent clickEvent;
    @Mock private Block block;
    @Mock private Player player;
    @Mock private World world;
    @Mock private Inventory inventory;
    @Mock private Location location;  // Mock zamiast instancji

    private SharedCraftingListener listener;

    @BeforeEach
    void setUp() {
        listener = new SharedCraftingListener(manager);
    }

    @Test
    void testOnInteract_WithCraftingTable() {
        // Przygotowanie
        when(interactEvent.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(interactEvent.getClickedBlock()).thenReturn(block);
        when(interactEvent.getPlayer()).thenReturn(player);
        when(block.getType()).thenReturn(Material.CRAFTING_TABLE);
        when(block.getLocation()).thenReturn(location);
        
        when(manager.getWorkbenchInventory(location)).thenReturn(inventory);

        // Wykonanie
        listener.onInteract(interactEvent);

        // Weryfikacja
        // 1. Sprawdzamy czy anulowano domyślne otwarcie
        verify(interactEvent).setCancelled(true);
        // 2. Sprawdzamy czy pobrano ekwipunek
        verify(manager).getWorkbenchInventory(location);
        // 3. Sprawdzamy czy otwarto ekwipunek graczowi
        verify(player).openInventory(inventory);
    }

    @Test
    void testOnInteract_NotCraftingTable() {
        // Przygotowanie
        when(interactEvent.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(interactEvent.getClickedBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.STONE); // Inny blok

        // Wykonanie
        listener.onInteract(interactEvent);

        // Weryfikacja
        verify(interactEvent, never()).setCancelled(anyBoolean());
        verify(manager, never()).getWorkbenchInventory(any());
        verify(player, never()).openInventory(any(Inventory.class));
    }

    @Test
    void testOnInteract_LeftClick_Ignored() {
        // Przygotowanie
        when(interactEvent.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);

        // Wykonanie
        listener.onInteract(interactEvent);

        // Weryfikacja
        verify(interactEvent, never()).setCancelled(anyBoolean());
    }
    
    @Test
    void testOnInteract_SneakingPlayer_AllowsBlockPlacement() {
        // Przygotowanie: Gracz sneakuje (shift) i klika prawym na crafting table
        when(interactEvent.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(interactEvent.getClickedBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.CRAFTING_TABLE);
        when(interactEvent.getPlayer()).thenReturn(player);
        when(player.isSneaking()).thenReturn(true); // Gracz trzyma shift

        // Wykonanie
        listener.onInteract(interactEvent);

        // Weryfikacja: Event NIE powinien być anulowany (pozwalamy na kładzenie bloków)
        verify(interactEvent, never()).setCancelled(anyBoolean());
        verify(manager, never()).getWorkbenchInventory(any()); // Nie otwieramy GUI
        verify(player, never()).openInventory(any(Inventory.class));
    }
    
    @Test
    void testOnInventoryClick_DropOneSlot_FromSharedInventory() {
        // Przygotowanie: Gracz naciska Q (drop one) na slocie w shared inventory
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(inventory);

        // Mockito-only: nie tworzymy prawdziwego ItemStack (wymaga serwera Bukkit)
        ItemStack itemInSlot = mock(ItemStack.class);
        ItemStack droppedClone = mock(ItemStack.class);
        when(itemInSlot.getType()).thenReturn(Material.DIAMOND);
        when(itemInSlot.getAmount()).thenReturn(5);
        when(itemInSlot.clone()).thenReturn(droppedClone);
        
        when(clickEvent.getInventory()).thenReturn(inventory);
        when(inventory.getType()).thenReturn(InventoryType.WORKBENCH);
        when(manager.hasInventory(inventory)).thenReturn(true);
        when(clickEvent.getView()).thenReturn(view);
        when(clickEvent.getWhoClicked()).thenReturn(player);
        when(clickEvent.getAction()).thenReturn(InventoryAction.DROP_ONE_SLOT);
        when(clickEvent.getRawSlot()).thenReturn(2); // Slot w górnym inventory (grid)
        when(inventory.getSize()).thenReturn(10);
        when(inventory.getItem(2)).thenReturn(itemInSlot);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);
        
        // Wykonanie
        listener.onInventoryClick(clickEvent);
        
        // Weryfikacja: Event anulowany (ręczna obsługa)
        verify(clickEvent).setCancelled(true);
        
        // 1 item droppowany
        verify(world).dropItemNaturally(eq(location), any(ItemStack.class));

        // Stack zmniejszony (5 -> 4)
        verify(itemInSlot).setAmount(4);
        verify(inventory).setItem(2, itemInSlot);
    }

    @Test
    void testOnBlockBreak_WithStoredInventory() {
        // Przygotowanie
        when(breakEvent.getBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.CRAFTING_TABLE);
        when(block.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.DIAMOND);
        
        when(manager.hasInventory(location)).thenReturn(true);
        when(manager.getWorkbenchInventory(location)).thenReturn(inventory);
        when(inventory.getSize()).thenReturn(10); // 9 + 1 wynik
        when(inventory.getItem(1)).thenReturn(item); // Slot 1 ma diament
        when(inventory.getViewers()).thenReturn(Collections.emptyList());

        // Wykonanie
        listener.onBlockBreak(breakEvent);

        // Weryfikacja
        // 1. Sprawdzamy czy item został wyrzucony (dropItemNaturally)
        verify(world).dropItemNaturally(eq(location), eq(item));
        // 2. Sprawdzamy czy usunięto z managera
        verify(manager).removeInventory(location);
    }
    
    @Test
    void testOnBlockBreak_NoStoredInventory() {
        // Przygotowanie
        when(breakEvent.getBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.CRAFTING_TABLE);
        when(block.getLocation()).thenReturn(location);
        when(manager.hasInventory(location)).thenReturn(false);

        // Wykonanie
        listener.onBlockBreak(breakEvent);

        // Weryfikacja - nic się nie dzieje, bo nie ma zapisanego inventory
        verify(manager, never()).removeInventory(location);
        verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
    }
    
    @Test
    void testOnEntityExplode_WithCraftingTables() {
        // Przygotowanie - 2 bloki: crafting table z itemami i stone
        Block craftingTable = mock(Block.class);
        Block stone = mock(Block.class);
        Location craftingLoc = mock(Location.class);
        World craftingWorld = mock(World.class);
        
        when(craftingTable.getType()).thenReturn(Material.CRAFTING_TABLE);
        when(craftingTable.getLocation()).thenReturn(craftingLoc);
        when(craftingLoc.getWorld()).thenReturn(craftingWorld);
        
        when(stone.getType()).thenReturn(Material.STONE);
        
        ItemStack diamond = mock(ItemStack.class);
        when(diamond.getType()).thenReturn(Material.DIAMOND);
        
        when(manager.hasInventory(craftingLoc)).thenReturn(true);
        when(manager.getWorkbenchInventory(craftingLoc)).thenReturn(inventory);
        when(inventory.getSize()).thenReturn(10);
        when(inventory.getItem(1)).thenReturn(diamond);
        when(inventory.getViewers()).thenReturn(Collections.emptyList());
        
        List<Block> blocks = Arrays.asList(craftingTable, stone);
        when(entityExplodeEvent.blockList()).thenReturn(blocks);

        // Wykonanie
        listener.onEntityExplode(entityExplodeEvent);

        // Weryfikacja - tylko crafting table powinien zostać obsłużony
        verify(craftingWorld).dropItemNaturally(eq(craftingLoc), eq(diamond));
        verify(manager).removeInventory(craftingLoc);
    }
    
    @Test
    void testOnBlockExplode_WithCraftingTables() {
        // Przygotowanie
        Block craftingTable = mock(Block.class);
        Location craftingLoc = mock(Location.class);
        World craftingWorld = mock(World.class);
        
        when(craftingTable.getType()).thenReturn(Material.CRAFTING_TABLE);
        when(craftingTable.getLocation()).thenReturn(craftingLoc);
        when(craftingLoc.getWorld()).thenReturn(craftingWorld);
        
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.IRON_INGOT);
        
        when(manager.hasInventory(craftingLoc)).thenReturn(true);
        when(manager.getWorkbenchInventory(craftingLoc)).thenReturn(inventory);
        when(inventory.getSize()).thenReturn(10);
        
        // Stubujemy wszystkie sloty 1-9 (pętla w handleCraftingTableDestruction sprawdza wszystkie)
        when(inventory.getItem(1)).thenReturn(null);
        when(inventory.getItem(2)).thenReturn(item);
        when(inventory.getItem(3)).thenReturn(null);
        when(inventory.getItem(4)).thenReturn(null);
        when(inventory.getItem(5)).thenReturn(null);
        when(inventory.getItem(6)).thenReturn(null);
        when(inventory.getItem(7)).thenReturn(null);
        when(inventory.getItem(8)).thenReturn(null);
        when(inventory.getItem(9)).thenReturn(null);
        
        when(inventory.getViewers()).thenReturn(Collections.emptyList());
        
        when(blockExplodeEvent.blockList()).thenReturn(Collections.singletonList(craftingTable));

        // Wykonanie
        listener.onBlockExplode(blockExplodeEvent);

        // Weryfikacja
        verify(craftingWorld).dropItemNaturally(eq(craftingLoc), eq(item));
        verify(manager).removeInventory(craftingLoc);
    }
    
    @Test
    void testOnPistonExtend_WithCraftingTables() {
        // Przygotowanie
        Block craftingTable = mock(Block.class);
        Location craftingLoc = mock(Location.class);
        World craftingWorld = mock(World.class);
        
        when(craftingTable.getType()).thenReturn(Material.CRAFTING_TABLE);
        when(craftingTable.getLocation()).thenReturn(craftingLoc);
        when(craftingLoc.getWorld()).thenReturn(craftingWorld);
        
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.GOLD_INGOT);
        
        when(manager.hasInventory(craftingLoc)).thenReturn(true);
        when(manager.getWorkbenchInventory(craftingLoc)).thenReturn(inventory);
        when(inventory.getSize()).thenReturn(10);
        
        // Stubujemy wszystkie sloty 1-9
        when(inventory.getItem(1)).thenReturn(null);
        when(inventory.getItem(2)).thenReturn(null);
        when(inventory.getItem(3)).thenReturn(null);
        when(inventory.getItem(4)).thenReturn(null);
        when(inventory.getItem(5)).thenReturn(item);
        when(inventory.getItem(6)).thenReturn(null);
        when(inventory.getItem(7)).thenReturn(null);
        when(inventory.getItem(8)).thenReturn(null);
        when(inventory.getItem(9)).thenReturn(null);
        
        when(inventory.getViewers()).thenReturn(Collections.emptyList());
        
        when(pistonExtendEvent.getBlocks()).thenReturn(Collections.singletonList(craftingTable));

        // Wykonanie
        listener.onPistonExtend(pistonExtendEvent);

        // Weryfikacja
        verify(craftingWorld).dropItemNaturally(eq(craftingLoc), eq(item));
        verify(manager).removeInventory(craftingLoc);
    }
    
    @Test
    void testOnPistonRetract_WithCraftingTables() {
        // Przygotowanie
        Block craftingTable = mock(Block.class);
        Location craftingLoc = mock(Location.class);
        World craftingWorld = mock(World.class);
        
        when(craftingTable.getType()).thenReturn(Material.CRAFTING_TABLE);
        when(craftingTable.getLocation()).thenReturn(craftingLoc);
        when(craftingLoc.getWorld()).thenReturn(craftingWorld);
        
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.EMERALD);
        
        when(manager.hasInventory(craftingLoc)).thenReturn(true);
        when(manager.getWorkbenchInventory(craftingLoc)).thenReturn(inventory);
        when(inventory.getSize()).thenReturn(10);
        
        // Stubujemy wszystkie sloty 1-9
        when(inventory.getItem(1)).thenReturn(null);
        when(inventory.getItem(2)).thenReturn(null);
        when(inventory.getItem(3)).thenReturn(item);
        when(inventory.getItem(4)).thenReturn(null);
        when(inventory.getItem(5)).thenReturn(null);
        when(inventory.getItem(6)).thenReturn(null);
        when(inventory.getItem(7)).thenReturn(null);
        when(inventory.getItem(8)).thenReturn(null);
        when(inventory.getItem(9)).thenReturn(null);
        
        when(inventory.getViewers()).thenReturn(Collections.emptyList());
        
        when(pistonRetractEvent.getBlocks()).thenReturn(Collections.singletonList(craftingTable));

        // Wykonanie
        listener.onPistonRetract(pistonRetractEvent);

        // Weryfikacja
        verify(craftingWorld).dropItemNaturally(eq(craftingLoc), eq(item));
        verify(manager).removeInventory(craftingLoc);
    }
}

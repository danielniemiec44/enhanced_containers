package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Testy jednostkowe dla CraftingGUIListener.
 *
 * Cel:
 * - Weryfikujemy reguły blokowania slotów (niewidoczne / output)
 * - Bez MockBukkit (tu nie testujemy schedulera)
 */
@DisplayName("Event handlery CraftingGUI - Unit Test")
class CraftingGUIListenerEventTest {

    private CraftingGUIListener listener;
    private Player player;
    private Inventory guiInventory;
    private CraftingGUI gui;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        listener = new CraftingGUIListener();
        player = mock(Player.class);
        PlayerInventory playerInventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(playerInventory);
        when(playerInventory.getStorageContents()).thenReturn(new ItemStack[0]);
        guiInventory = mock(Inventory.class);
        gui = mock(CraftingGUI.class);

        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(gui.getPlayerId()).thenReturn(playerId);
        when(gui.getInventory()).thenReturn(guiInventory);
        when(guiInventory.getSize()).thenReturn(54);

        listener.registerGUI(gui);
    }

    @Test
    @DisplayName("Klik w niezarejestrowane GUI jest ignorowany")
    void clickForUnknownGUIIsIgnored() {
        CraftingGUIListener otherListener = new CraftingGUIListener();

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(guiInventory);
        when(event.getClickedInventory()).thenReturn(guiInventory);
        when(event.getRawSlot()).thenReturn(0);

        otherListener.onInventoryClick(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    @DisplayName("Klik w niewidoczny slot jest blokowany")
    void clickOnInvisibleSlotIsCancelled() {
        int rawSlot = 5;

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(guiInventory);
        when(event.getClickedInventory()).thenReturn(guiInventory);
        when(event.getRawSlot()).thenReturn(rawSlot);

        when(gui.isVisibleSlot(rawSlot)).thenReturn(false);

        listener.onInventoryClick(event);

        verify(event).setCancelled(true);
    }

    @Test
    @DisplayName("Klik w output bez itemu jest blokowany")
    void clickOnEmptyOutputIsCancelled() {
        int rawSlot = CraftingGUI.OUTPUT_SLOT;

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(guiInventory);
        when(event.getClickedInventory()).thenReturn(guiInventory);
        when(event.getRawSlot()).thenReturn(rawSlot);

        when(gui.isVisibleSlot(rawSlot)).thenReturn(true);
        when(gui.isOutputSlot(rawSlot)).thenReturn(true);

        when(guiInventory.getItem(CraftingGUI.OUTPUT_SLOT)).thenReturn(null);

        listener.onInventoryClick(event);

        verify(event).setCancelled(true);
    }

    @Test
    @DisplayName("Klik w output z null clickedInventory jest blokowany")
    void clickOnOutputWithNullClickedInventoryIsCancelled() {
        int rawSlot = CraftingGUI.OUTPUT_SLOT;

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(guiInventory);
        when(event.getClickedInventory()).thenReturn(null);
        when(event.getRawSlot()).thenReturn(rawSlot);

        when(gui.isVisibleSlot(rawSlot)).thenReturn(true);
        when(gui.isOutputSlot(rawSlot)).thenReturn(true);

        when(guiInventory.getItem(CraftingGUI.OUTPUT_SLOT)).thenReturn(null);

        listener.onInventoryClick(event);

        verify(event).setCancelled(true);
    }

    @Test
    @DisplayName("Klik w output z itemem na kursorze jest blokowany")
    void clickOnOutputWithCursorItemIsCancelled() {
        int rawSlot = CraftingGUI.OUTPUT_SLOT;

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(guiInventory);
        when(event.getClickedInventory()).thenReturn(guiInventory);
        when(event.getRawSlot()).thenReturn(rawSlot);

        when(gui.isVisibleSlot(rawSlot)).thenReturn(true);
        when(gui.isOutputSlot(rawSlot)).thenReturn(true);

        // Output istnieje (mockujemy ItemStack, bez tworzenia prawdziwego)
        org.bukkit.inventory.ItemStack outputItem = mock(org.bukkit.inventory.ItemStack.class);
        when(outputItem.getType()).thenReturn(org.bukkit.Material.DIAMOND_BLOCK);
        when(guiInventory.getItem(CraftingGUI.OUTPUT_SLOT)).thenReturn(outputItem);

        // Kursor ma item (mockujemy ItemStack)
        org.bukkit.inventory.ItemStack cursorItem = mock(org.bukkit.inventory.ItemStack.class);
        when(cursorItem.getType()).thenReturn(org.bukkit.Material.COBBLESTONE);
        when(event.getCursor()).thenReturn(cursorItem);
        when(event.getClick()).thenReturn(org.bukkit.event.inventory.ClickType.LEFT);

        listener.onInventoryClick(event);

        verify(event).setCancelled(true);
    }

    @Test
    @DisplayName("Drag na niewidoczny slot jest blokowany")
    void dragOnInvisibleSlotIsCancelled() {
        int rawSlot = 7;

        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(guiInventory);
        when(event.getRawSlots()).thenReturn(Set.of(rawSlot));

        when(gui.isVisibleSlot(rawSlot)).thenReturn(false);

        listener.onInventoryDrag(event);

        verify(event).setCancelled(true);
    }

    @Test
    @DisplayName("Drag na output slot jest blokowany")
    void dragOnOutputSlotIsCancelled() {
        int rawSlot = CraftingGUI.OUTPUT_SLOT;

        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(guiInventory);
        when(event.getRawSlots()).thenReturn(Set.of(rawSlot));

        when(gui.isVisibleSlot(rawSlot)).thenReturn(true);

        listener.onInventoryDrag(event);

        verify(event).setCancelled(true);
    }

    @Test
    @DisplayName("Drag na widoczne input sloty nie jest blokowany")
    void dragOnVisibleInputSlotsIsNotCancelled() {
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(guiInventory);
        when(event.getRawSlots()).thenReturn(Set.of(CraftingGUI.GRID_SLOTS[0], CraftingGUI.GRID_SLOTS[1]));

        when(gui.isVisibleSlot(anyInt())).thenReturn(true);

        listener.onInventoryDrag(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    @DisplayName("Creative: klik w output jest blokowany")
    void creativeClickOnOutputIsCancelled() {
        int rawSlot = CraftingGUI.OUTPUT_SLOT;

        InventoryCreativeEvent event = mock(InventoryCreativeEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(guiInventory);
        when(event.getRawSlot()).thenReturn(rawSlot);

        // Creative event też ma InventoryView
        var view = mock(org.bukkit.inventory.InventoryView.class);
        when(view.getTopInventory()).thenReturn(guiInventory);
        when(event.getView()).thenReturn(view);

        when(gui.isVisibleSlot(rawSlot)).thenReturn(true);
        when(gui.isOutputSlot(rawSlot)).thenReturn(true);

        listener.onInventoryCreative(event);

        verify(event).setCancelled(true);
    }

    @Test
    @DisplayName("Creative: klik w niewidoczny slot jest blokowany")
    void creativeClickOnInvisibleSlotIsCancelled() {
        int rawSlot = 5; // slot spoza gridu i output (czyli niewidoczny)

        InventoryCreativeEvent event = mock(InventoryCreativeEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(guiInventory);
        when(event.getRawSlot()).thenReturn(rawSlot);

        var view = mock(org.bukkit.inventory.InventoryView.class);
        when(view.getTopInventory()).thenReturn(guiInventory);
        when(event.getView()).thenReturn(view);

        when(gui.isVisibleSlot(rawSlot)).thenReturn(false);

        listener.onInventoryCreative(event);

        // W creative też nie wolno dotykać niewidocznych slotów
        verify(event).setCancelled(true);
    }

    @Test
    @DisplayName("Klik w output typu SWAP_OFFHAND jest blokowany")
    void clickOnOutputSwapOffhandIsCancelled() {
        int rawSlot = CraftingGUI.OUTPUT_SLOT;

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(guiInventory);
        when(event.getClickedInventory()).thenReturn(guiInventory);
        when(event.getRawSlot()).thenReturn(rawSlot);

        when(gui.isVisibleSlot(rawSlot)).thenReturn(true);
        when(gui.isOutputSlot(rawSlot)).thenReturn(true);

        // Output istnieje (mockujemy ItemStack, bez tworzenia prawdziwego)
        org.bukkit.inventory.ItemStack outputItem = mock(org.bukkit.inventory.ItemStack.class);
        when(outputItem.getType()).thenReturn(org.bukkit.Material.DIAMOND_BLOCK);
        when(guiInventory.getItem(CraftingGUI.OUTPUT_SLOT)).thenReturn(outputItem);

        // Nietypowy klik - nie jest LEFT/RIGHT/SHIFT
        when(event.getClick()).thenReturn(org.bukkit.event.inventory.ClickType.SWAP_OFFHAND);

        listener.onInventoryClick(event);

        // Output jest read-only: zawsze anulujemy event
        verify(event).setCancelled(true);
        // I nie wolno nic dawać na kursor w takich akcjach
        verify(player, never()).setItemOnCursor(any());
    }
}

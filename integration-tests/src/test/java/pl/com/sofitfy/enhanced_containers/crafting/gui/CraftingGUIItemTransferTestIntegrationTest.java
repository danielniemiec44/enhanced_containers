package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testy jednostkowe dla przenoszenia item'ów w GUI craftingu
 *
 * Testujemy logikę listener'a czy prawidłowo:
 * - Pozwala kliknąć output slot (slot 24) jak vanilla
 * - Pozwala na przenoszenie wewnątrz gridu (input sloty)
 * - Obsługuje drag-drop eventy
 *
 * WAŻNE: Testy jednostkowe nie używają MockBukkit - tylko mocki
 * Prawdziwe obiekty Bukkit testujemy w testach integracyjnych
 *
 * Komentarze po polsku i prostym językiem.
 */
@DisplayName("CraftingGUI - testy jednostkowe logiki listener'a")
@Disabled("Część testów wymaga loadJar - excludowana z normalnych testów. Zachowywana dla referencji.")
class CraftingGUIItemTransferTest {

    private CraftingGUIListener guiListener;
    private Player player;
    private CraftingGUI gui;
    private Inventory inventory;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        // Tworzymy listener
        guiListener = new CraftingGUIListener();

        // Mockujemy gracza i jego ID
        playerId = UUID.randomUUID();
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);

        // Mockujemy inventorium (NIE tworzymy prawdziwego, bo to wymaga MockBukkit)
        inventory = mock(Inventory.class);

        // Mockujemy GUI
        gui = mock(CraftingGUI.class);
        when(gui.getPlayerId()).thenReturn(playerId);
        when(gui.getInventory()).thenReturn(inventory);

        // Rejestrujemy mock GUI w listenera
        guiListener.registerGUI(gui);
    }

    @Tag("integration")
    @Test
    @DisplayName("Event kliknięcia na input slot NIE powinien być zablokowany")
    void clickOnInputSlotShouldNotBeCancelled() {
        // Przygotowanie: tworzymy event kliknięcia na input slot (slot 0)
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getClickedInventory()).thenReturn(inventory); // WAŻNE!
        when(event.getRawSlot()).thenReturn(0); // Slot 0 to input slot
        when(event.isShiftClick()).thenReturn(false);

        // Mockujemy metodę isInputSlot żeby zwracała true dla slotu 0
        when(gui.isInputSlot(0)).thenReturn(true);
        when(gui.isOutputSlot(0)).thenReturn(false);
        when(gui.isVisibleSlot(0)).thenReturn(true); // Slot 0 to widoczny slot (input)

        // Działanie: obsługujemy event
        guiListener.onInventoryClick(event);

        // Sprawdzenie: event NIE powinien być zablokowany
        // Jeśli by był zablokowany (setCancelled(true)), gracz nie mógłby przenośić
        verify(event, never()).setCancelled(true);
    }

    @Tag("integration")
    @Test
    @DisplayName("Event kliknięcia na output slot NIE powinien być zablokowany")
    void clickOnOutputSlotShouldNotBeCancelled() {
        // Przygotowanie: tworzymy event kliknięcia na output slot (slot 24)
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getClickedInventory()).thenReturn(inventory); // WAŻNE - clicked inventory to to samo GUI!
        when(event.getRawSlot()).thenReturn(CraftingGUI.OUTPUT_SLOT); // Slot 24 - output
        when(event.isShiftClick()).thenReturn(false);

        // Mockujemy metody aby wiadomo było że to output slot
        when(gui.isOutputSlot(CraftingGUI.OUTPUT_SLOT)).thenReturn(true);
        when(gui.isInputSlot(CraftingGUI.OUTPUT_SLOT)).thenReturn(false);
        when(gui.isVisibleSlot(CraftingGUI.OUTPUT_SLOT)).thenReturn(true);

        // Działanie: obsługujemy event
        guiListener.onInventoryClick(event);

        // Sprawdzenie: event NIE powinien być zablokowany
        // Gracz ma móc normalnie wyciągnąć item z output slotu
        verify(event, never()).setCancelled(true);
    }

    @Tag("integration")
    @Test
    @DisplayName("Drag-drop na input slot NIE powinien być zablokowany")
    void dragDropOnInputSlotShouldNotBeCancelled() {
        // Przygotowanie: tworzymy drag-drop event na input slot
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(inventory);

        // Drag na slot 1 (input slot)
        Set<Integer> slots = new HashSet<>();
        slots.add(1);
        when(event.getRawSlots()).thenReturn(slots);

        // Mockujemy isVisibleSlot dla slotu 1 (input slot to widoczny)
        when(gui.isVisibleSlot(1)).thenReturn(true);
        when(gui.isOutputSlot(1)).thenReturn(false);

        // Działanie: obsługujemy drag event
        guiListener.onInventoryDrag(event);

        // Sprawdzenie: event NIE powinien być zablokowany
        // Gracz powinien móc drag'ować item'y wewnątrz gridu
        verify(event, never()).setCancelled(true);
    }

    @Tag("integration")
    @Test
    @DisplayName("Drag-drop na output slot POWINIEN być zablokowany")
    void dragDropOnOutputSlotShouldBeCancelled() {
        // Przygotowanie: tworzymy drag-drop event na output slot (slot 24)
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(inventory);

        // Drag na slot 24 (output slot)
        Set<Integer> slots = new HashSet<>();
        slots.add(CraftingGUI.OUTPUT_SLOT);
        when(event.getRawSlots()).thenReturn(slots);

        // Działanie: obsługujemy drag event
        guiListener.onInventoryDrag(event);

        // Sprawdzenie: event POWINIEN być zablokowany
        // Output slot to przycisk do craftowania, nie można go zmodyfikować
        verify(event, times(1)).setCancelled(true);
    }

    @Tag("integration")
    @Test
    @DisplayName("Drag-drop na wiele slotów łącznie z output powinien być zablokowany")
    void dragDropOnMultipleSlotsIncludingOutputShouldBeCancelled() {
        // Przygotowanie: drag-drop na wiele slotów, w tym output
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(inventory);

        // Drag na sloty 0, 1, i 24 (output)
        Set<Integer> slots = new HashSet<>();
        slots.add(0);
        slots.add(1);
        slots.add(CraftingGUI.OUTPUT_SLOT);
        when(event.getRawSlots()).thenReturn(slots);

        // Działanie: obsługujemy drag event
        guiListener.onInventoryDrag(event);

        // Sprawdzenie: event POWINIEN być zablokowany
        // Jeśli gracz próbuje drag'ować na output slot (nawet razem z innymi), blokujemy
        verify(event, times(1)).setCancelled(true);
    }

    @Tag("integration")
    @Test
    @DisplayName("GUI dla innego gracza nie wpływa na event'y")
    void differentPlayerGUIShouldNotAffectEvents() {
        // Przygotowanie: tworzymy drugiego gracza i jego GUI
        Player player2 = mock(Player.class);
        UUID player2Id = UUID.randomUUID();
        when(player2.getUniqueId()).thenReturn(player2Id);

        Inventory inventory2 = mock(Inventory.class);
        CraftingGUI gui2 = mock(CraftingGUI.class);
        when(gui2.getPlayerId()).thenReturn(player2Id);
        when(gui2.getInventory()).thenReturn(inventory2);

        // Rejestrujemy drugie GUI
        guiListener.registerGUI(gui2);

        // Tworzymy event dla player'a 1 ale inventory od player'a 2
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(inventory2); // Inventory innego gracza!
        when(event.getRawSlot()).thenReturn(0);
        when(event.isShiftClick()).thenReturn(false);

        // Działanie: obsługujemy event
        guiListener.onInventoryClick(event);

        // Sprawdzenie: event nie powinien być obsługiwany dla innego GUI
        // Listener powinien zignorować event bo GUI do innego gracza
        verify(event, never()).setCancelled(true);
    }

    @Tag("integration")
    @Test
    @DisplayName("Niezarejestrowane GUI są ignorowane")
    void unregisteredGUIShouldBeIgnored() {
        // Przygotowanie: tworzymy event dla gracza bez zarejestrowanego GUI
        Player unknownPlayer = mock(Player.class);
        when(unknownPlayer.getUniqueId()).thenReturn(UUID.randomUUID());

        Inventory unknownInventory = mock(Inventory.class);

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(unknownPlayer);
        when(event.getInventory()).thenReturn(unknownInventory);
        when(event.getRawSlot()).thenReturn(CraftingGUI.OUTPUT_SLOT);

        // Działanie: obsługujemy event
        guiListener.onInventoryClick(event);

        // Sprawdzenie: event nie powinien być obsługiwany
        // Bo gracz nie ma zarejestrowanego GUI
        verify(event, never()).setCancelled(true);
    }

}

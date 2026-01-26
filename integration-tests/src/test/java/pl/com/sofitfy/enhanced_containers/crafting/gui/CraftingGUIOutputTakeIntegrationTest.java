package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test integracyjny: kliknięcie w output powinno zużyć składniki i odświeżyć wynik.
 *
 * Ważne: logika zużycia składników jest opóźniona na następny tick
 * (handleOutputTakenDelayed), więc w teście symulujemy "zabranie" outputu
 * poprzez wyczyszczenie output slotu przed tickiem.
 */
@DisplayName("Integracja - zabranie output zużywa składniki")
class CraftingGUIOutputTakeIntegrationTest extends MockBukkitPluginTestBase {

    @Test
    @DisplayName("Klik w output: zużywa 1 składnik i przelicza output")
    void takingOutputConsumesIngredientsAndRecalculates() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // 2 deski w jednym slocie -> po 1 craft powinno zostać 1.
        // Vanilla: 1x OAK_PLANKS = 1x OAK_BUTTON
        gui.getInventory().setItem(CraftingGUI.GRID_SLOTS[0], new ItemStack(Material.OAK_PLANKS, 2));
        listener.updateOutput(player, gui);

        ItemStack initialOutput = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(initialOutput, "Output powinien się pojawić przed craftem");
        assertFalse(initialOutput.getType().isAir());
        // Sprawdzmy jaki output dostajemy dla 2x OAK_PLANKS w jednym slocie
        // (może być OAK_BUTTON bo 1 deska = 1 button)
        assertEquals(Material.OAK_BUTTON, initialOutput.getType(), 
            "1x OAK_PLANKS w jednym slocie = OAK_BUTTON");

        // Potrzebujemy InventoryView do poprawnego rozpoznania top inventory.
        player.openInventory(gui.getInventory());
        player.setItemOnCursor(null); // Pusty kursor - normalny pickup

        InventoryClickEvent clickOutput = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            CraftingGUI.OUTPUT_SLOT,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        // Act: klik w output
        listener.onInventoryClick(clickOutput);
        
        // Event jest ZAWSZE anulowany dla output slotu (zapobiegamy exploitom)
        // ALE gracz powinien dostać item na kursor (obsługujemy ręcznie!)
        assertTrue(clickOutput.isCancelled(), 
            "Event MUSI być anulowany (wszystkie kliki na output obsługujemy ręcznie)");

        // Najważniejsze: gracz powinien otrzymać output na kursor!
        ItemStack cursor = player.getItemOnCursor();
        assertNotNull(cursor, "Gracz powinien dostać output na kursor!");
        assertEquals(Material.OAK_BUTTON, cursor.getType(), "Na kursorze powinien być OAK_BUTTON!");
        
        // Output slot powinien być pusty (zabraliśmy item)
        ItemStack outputNow = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        
        // Po ręcznej obsłudze składniki zostały pobrane i output przeliczony
        ItemStack remaining = gui.getInventory().getItem(CraftingGUI.GRID_SLOTS[0]);
        assertNotNull(remaining);
        assertEquals(Material.OAK_PLANKS, remaining.getType());
        assertEquals(1, remaining.getAmount(), "Po 1 craft powinno zostać 1 deska");

        // Output powinien być nadal ustawiony (mamy jeszcze 1 deskę)
        ItemStack outputAfter = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        assertNotNull(outputAfter, "Po craft powinien się nadal pokazywać output (mamy jeszcze 1 deskę)");
        assertFalse(outputAfter.getType().isAir());
    }

    @Test
    @DisplayName("Klik w output z itemem daje output na kursor gracza")
    void clickingNonEmptyOutputPutsItemOnCursor() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Symulujemy, że output jest już ustawiony (receptura pasuje).
        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND, 1));

        player.openInventory(gui.getInventory());
        player.setItemOnCursor(null); // Pusty kursor

        InventoryClickEvent clickOutput = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            CraftingGUI.OUTPUT_SLOT,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        listener.onInventoryClick(clickOutput);
        
        // Event jest ZAWSZE anulowany dla output slotu
        assertTrue(clickOutput.isCancelled(), 
            "Event MUSI być anulowany (wszystkie kliki na output obsługujemy ręcznie)");
        
        // Najważniejsze: gracz powinien otrzymać output na kursor!
        ItemStack cursor = player.getItemOnCursor();
        assertNotNull(cursor, "Gracz powinien dostać output na kursor!");
        assertEquals(Material.DIAMOND, cursor.getType(), "Na kursorze powinien być DIAMOND!");
    }
}

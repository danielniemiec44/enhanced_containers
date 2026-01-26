package pl.com.sofitfy.enhanced_containers.crafting.nms;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Przykład użycia NMS craftingu w GUI.
 *
 * Założenia jak w Twoim opisie:
 * - slot 0 = wynik
 * - sloty 1-9 = siatka 3x3
 *
 * Ten kod jest celowo prosty: po zmianie w siatce przeliczamy wynik i ustawiamy go w slocie 0.
 */
public final class NmsCraftingGuiExample {

    private static final int RESULT_SLOT = 0;
    private static final int[] GRID_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};

    private final NmsCraftingEngine nmsCraftingEngine = new NmsCraftingEngine();

    /**
     * Wywołaj po każdej zmianie w slotach 1-9.
     */
    public void updateCraftingMatrix(Inventory guiInventory, World world) {
        if (guiInventory == null || world == null) {
            return;
        }

        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < GRID_SLOTS.length; i++) {
            matrix[i] = guiInventory.getItem(GRID_SLOTS[i]);
        }

        ItemStack result = nmsCraftingEngine.getNMSCraftingResult(matrix, world);
        guiInventory.setItem(RESULT_SLOT, result);
    }

    /**
     * Minimalny przykład pod InventoryClickEvent.
     *
     * Uwaga:
     * - w realnym pluginie musisz dodać sprawdzenie czy to NA PEWNO Twoje GUI.
     * - zwykle warto update robić "next tick", bo click jeszcze nie zdążył zmienić inventory.
     */
    public void onInventoryClickExample(InventoryClickEvent event, Inventory guiInventory) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int raw = event.getRawSlot();
        if (raw < 0 || raw >= guiInventory.getSize()) {
            return;
        }

        boolean isGridSlot = raw >= 1 && raw <= 9;
        if (!isGridSlot) {
            return;
        }

        // W tym miejscu zakładamy, że Inventory zostanie zaktualizowane po evencie.
        // W praktyce: użyj scheduler/runTask(...), żeby odpalić update w następnym ticku.
        updateCraftingMatrix(guiInventory, player.getWorld());
    }
}

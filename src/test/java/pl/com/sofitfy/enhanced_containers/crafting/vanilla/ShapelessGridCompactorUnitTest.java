package pl.com.sofitfy.enhanced_containers.crafting.vanilla;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShapelessGridCompactor - unit")
class ShapelessGridCompactorUnitTest {

    @Test
    @DisplayName("Upakowuje 2 itemy z 4x4 do 3x3 (kolejność stabilna)")
    void packsTwoItemsInStableOrder() {
        // Mamy 4x4 i dwa itemy rozrzucone po rogach.
        ItemStack[] grid4x4 = new ItemStack[16];
        grid4x4[0] = new ItemStack(Material.PAPER);
        grid4x4[15] = new ItemStack(Material.GUNPOWDER);

        Optional<ItemStack[]> packedOpt = ShapelessGridCompactor.packToVanilla3x3IfAtMost9Occupied(grid4x4);
        assertTrue(packedOpt.isPresent(), "Powinno się dać upakować (2 <= 9)");

        ItemStack[] packed = packedOpt.get();
        assertEquals(9, packed.length);

        // Upakowanie idzie od slotu 0 w górę.
        assertNotNull(packed[0]);
        assertEquals(Material.PAPER, packed[0].getType());
        assertNotNull(packed[1]);
        assertEquals(Material.GUNPOWDER, packed[1].getType());

        // Reszta ma być pusta
        for (int i = 2; i < 9; i++) {
            assertNull(packed[i], "Pozostałe sloty powinny być puste");
        }
    }

    @Test
    @DisplayName("Nie robi upakowania gdy jest >9 zajętych slotów")
    void returnsEmptyWhenMoreThanNineOccupied() {
        ItemStack[] grid4x4 = new ItemStack[16];

        // Wypełniamy 10 slotów.
        for (int i = 0; i < 10; i++) {
            grid4x4[i] = new ItemStack(Material.COBBLESTONE);
        }

        Optional<ItemStack[]> packedOpt = ShapelessGridCompactor.packToVanilla3x3IfAtMost9Occupied(grid4x4);
        assertTrue(packedOpt.isEmpty(), "Dla >9 slotów nie robimy tej ścieżki");
    }

    @Test
    @DisplayName("Ignoruje null/AIR przy liczeniu i pakowaniu")
    void ignoresNullAndAir() {
        ItemStack[] grid4x4 = new ItemStack[16];
        grid4x4[0] = new ItemStack(Material.AIR);
        grid4x4[1] = null;
        grid4x4[2] = new ItemStack(Material.STICK);

        Optional<ItemStack[]> packedOpt = ShapelessGridCompactor.packToVanilla3x3IfAtMost9Occupied(grid4x4);
        assertTrue(packedOpt.isPresent());

        ItemStack[] packed = packedOpt.get();
        assertNotNull(packed[0]);
        assertEquals(Material.STICK, packed[0].getType());
    }
}

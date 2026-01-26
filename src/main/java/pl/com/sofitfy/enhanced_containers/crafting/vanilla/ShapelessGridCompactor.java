package pl.com.sofitfy.enhanced_containers.crafting.vanilla;

import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Pomocnicza logika dla vanilla shapeless w naszym gridzie 4x4.
 *
 * Problem:
 * - vanilla crafting ma 3x3,
 * - my mamy 4x4,
 * - a shapeless receptury powinny działać niezależnie od pozycji składników.
 *
 * Rozwiązanie:
 * - bierzemy wszystkie niepuste sloty z 4x4,
 * - jeśli jest ich max 9, to "upakowujemy" je do wirtualnego 3x3 (od lewej do prawej),
 * - jeśli jest ich > 9, to NIE robimy tego sprawdzania (bo vanilla i tak nie ma 4x4).
 */
public final class ShapelessGridCompactor {

    private ShapelessGridCompactor() {
    }

    /**
     * Upakuj 4x4 do 3x3 tak, aby shapeless można było sprawdzić jakby itemy leżały obok siebie.
     *
     * Zwraca Optional.empty gdy:
     * - grid jest null / ma zły rozmiar,
     * - grid jest pusty,
     * - albo ma więcej niż 9 zajętych slotów (wtedy nie robimy tej ścieżki w ogóle).
     */
    public static Optional<ItemStack[]> packToVanilla3x3IfAtMost9Occupied(ItemStack[] grid4x4) {
        if (grid4x4 == null || grid4x4.length != 16) {
            return Optional.empty();
        }

        int occupied = 0;
        for (ItemStack it : grid4x4) {
            if (it != null && it.getType() != null && !it.getType().isAir()) {
                occupied++;
            }
        }

        if (occupied <= 0) {
            return Optional.empty();
        }

        if (occupied > 9) {
            // Wymaganie: wtedy NIE robimy tego sprawdzania, tylko lecimy innymi ścieżkami.
            return Optional.empty();
        }

        ItemStack[] packed = new ItemStack[9];
        int out = 0;

        // Stabilny porządek: przechodzimy po 4x4 od lewej do prawej, od góry do dołu.
        for (int i = 0; i < 16 && out < 9; i++) {
            ItemStack it = grid4x4[i];
            if (it == null || it.getType() == null || it.getType().isAir()) {
                continue;
            }
            packed[out++] = it;
        }

        return Optional.of(packed);
    }
}

package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Normalizuje itemy wejściowe w custom recepturach.
 *
 * Cel:
 * - W inputach (grid) ilość NIE jest częścią definicji receptury.
 * - Dlatego wymuszamy, żeby każdy slot input miał ilość = 1.
 * - Output zostaje bez zmian (tam ilość jest ważna).
 *
 * Uwaga:
 * - Ta klasa nie używa MockBukkit i nie wymaga uruchomionego serwera.
 * - Działa na czystych ItemStackach.
 */
public final class InputItemStackNormalizer {

    private InputItemStackNormalizer() {
    }

    /**
     * Zwraca kopię itemu z ilością = 1.
     * Jeśli item jest pusty / AIR -> zwraca null.
     */
    public static ItemStack toSingleOrNull(ItemStack item) {
        if (item == null) {
            return null;
        }

        Material type = item.getType();
        if (type == null || type.isAir()) {
            return null;
        }

        ItemStack copy;
        try {
            copy = item.clone();
        } catch (Exception ignored) {
            // Fallback: w testach bez serwera clone() czasem potrafi rzucić wyjątek.
            // Tu wolimy zachować typ i ilość, niż wywalić całą operację.
            copy = new ItemStack(type, item.getAmount());
        }

        copy.setAmount(1);
        return copy;
    }

    /**
     * Normalizuje cały wzór inputu (dowolny rozmiar).
     * Każdy nie-pusty slot ma ilość = 1.
     */
    public static ItemStack[][] normalizePatternToSingle(ItemStack[][] pattern) {
        if (pattern == null) {
            return null;
        }

        ItemStack[][] out = new ItemStack[pattern.length][];
        for (int r = 0; r < pattern.length; r++) {
            ItemStack[] row = pattern[r];
            if (row == null) {
                out[r] = null;
                continue;
            }
            out[r] = new ItemStack[row.length];
            for (int c = 0; c < row.length; c++) {
                out[r][c] = toSingleOrNull(row[c]);
            }
        }
        return out;
    }
}

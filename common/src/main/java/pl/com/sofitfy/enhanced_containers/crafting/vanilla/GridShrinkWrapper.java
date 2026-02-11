package pl.com.sofitfy.enhanced_containers.crafting.vanilla;

import org.bukkit.inventory.ItemStack;

/**
 * Algorytm "Shrink Wrap" - dopasowanie siatki.
 * 
 * Zmniejsza macierz 4x4 do najmniejszego możliwego prostokąta,
 * usuwając puste rzędy i kolumny dookoła przedmiotów.
 * 
 * Umożliwia graczowi ułożenie receptury w dowolnym miejscu siatki 4x4,
 * a nie tylko w lewym górnym rogu.
 * 
 * Przykład:
 * - Gracz ułożył recepturę na deskę w pozycji: slot 6
 *   4x4: [0,1,2,3]
 *        [4,5,6,7]    <- slot 6 to deska
 *        [8,9,A,B]
 *        [C,D,E,F]
 * 
 * - Po "Shrink Wrap" -> najmniejszy prostokąt (1x1):
 *   [deska]
 * 
 * - To pozwala na dopasowanie do vanilla receptury 1x1
 */
public class GridShrinkWrapper {
    
    /**
     * Zmniejsza macierz 4x4 do najmniejszego prostokąta zawierającego wszystkie przedmioty.
     * Usuwa puste wiersze/kolumny z otoczenia.
     * 
     * @param grid4x4 Macierz 4x4 (rozmiar 16)
     *                Layout: [0,1,2,3]
     *                        [4,5,6,7]
     *                        [8,9,A,B]
     *                        [C,D,E,F]
     * @return Macierz zawierająca tylko przedmioty (maksymalnie 3x3 dla vanilla craftingu)
     *         null jeśli siatka pusta
     */
    public static ItemStack[] shrinkWrap4x4(ItemStack[] grid4x4) {
        if (grid4x4 == null || grid4x4.length != 16) {
            return null;
        }

        // Konwertuj 1D array na 2D (4x4)
        ItemStack[][] matrix2d = to2D(grid4x4, 4, 4);
        
        // Znajdź bounding box (najmniejszy prostokąt zawierający przedmioty)
        int minRow = Integer.MAX_VALUE;
        int maxRow = -1;
        int minCol = Integer.MAX_VALUE;
        int maxCol = -1;

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (!isEmpty(matrix2d[row][col])) {
                    minRow = Math.min(minRow, row);
                    maxRow = Math.max(maxRow, row);
                    minCol = Math.min(minCol, col);
                    maxCol = Math.max(maxCol, col);
                }
            }
        }

        // Jeśli siatka całkowicie pusta
        if (minRow == Integer.MAX_VALUE) {
            return null;
        }

        // Wytnij bounding box
        int height = maxRow - minRow + 1;
        int width = maxCol - minCol + 1;

        // Vanilla crafting (3x3) może być maksymalnie
        // ale zwracamy bezpośrednio wyciętą macierz
        ItemStack[] result = new ItemStack[height * width];
        int idx = 0;

        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                result[idx++] = matrix2d[row][col];
            }
        }

        return result;
    }

    /**
     * Zwraca wymiary wyciętego prostokąta (height, width).
     * Przydatne do debugowania i mapowania pozycji.
     * 
     * @param grid4x4 Macierz 4x4
     * @return Array [height, width] lub null jeśli siatka pusta
     */
    public static int[] getShrunkDimensions(ItemStack[] grid4x4) {
        if (grid4x4 == null || grid4x4.length != 16) {
            return null;
        }

        ItemStack[][] matrix2d = to2D(grid4x4, 4, 4);
        
        int minRow = Integer.MAX_VALUE;
        int maxRow = -1;
        int minCol = Integer.MAX_VALUE;
        int maxCol = -1;

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (!isEmpty(matrix2d[row][col])) {
                    minRow = Math.min(minRow, row);
                    maxRow = Math.max(maxRow, row);
                    minCol = Math.min(minCol, col);
                    maxCol = Math.max(maxCol, col);
                }
            }
        }

        if (minRow == Integer.MAX_VALUE) {
            return null;
        }

        return new int[]{maxRow - minRow + 1, maxCol - minCol + 1};
    }

    /**
     * Konwertuje 1D array na 2D macierz.
     */
    private static ItemStack[][] to2D(ItemStack[] array1d, int rows, int cols) {
        ItemStack[][] matrix = new ItemStack[rows][cols];
        for (int i = 0; i < array1d.length; i++) {
            int row = i / cols;
            int col = i % cols;
            matrix[row][col] = array1d[i];
        }
        return matrix;
    }

    /**
     * Sprawdza czy ItemStack jest pusty (null lub AIR).
     */
    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}

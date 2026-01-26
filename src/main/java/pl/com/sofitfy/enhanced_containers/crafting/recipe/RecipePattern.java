package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import org.bukkit.inventory.ItemStack;

/**
 * Reprezentuje wzór receptury z dynamicznym rozmiarem (1x1, 2x2, 3x3, etc).
 * Automatycznie oblicza najmniejszy prostokąt zawierający wszystkie przedmioty.
 * 
 * Wzór jest zawsze przechowywany bez pustych wierszy/kolumn z lewej i góry.
 * Na przykład:
 * - Jeśli gracz umieści 2x2, to trzymamy tablica 2x2
 * - Jeśli umieści 3x3, to trzymamy tablica 3x3
 * 
 * Podczas szukania w gridzie 4x4, szukamy tego pattern'u w każdej możliwej pozycji.
 */
public class RecipePattern {
    // Właściwy wzór (najmniejszy prostokąt)
    private final ItemStack[][] pattern;
    
    // Wymiary wzoru
    private final int width;
    private final int height;

    /**
     * Stwórz pattern z tablicy 2D.
     * Automatycznie obliczy najmniejszy prostokąt.
     * 
     * @param fullPattern nieoczyszczona tablica (może zawierać puste miejsca z lewej/góry)
     */
    public RecipePattern(ItemStack[][] fullPattern) {
        if (fullPattern == null || fullPattern.length == 0) {
            this.pattern = new ItemStack[0][0];
            this.width = 0;
            this.height = 0;
            return;
        }

        // Oblicz najmniejszy prostokąt
        int minRow = -1;
        int maxRow = -1;
        int minCol = -1;
        int maxCol = -1;

        // Szukaj pierwszego i ostatniego rządu z przedmiotem
        for (int row = 0; row < fullPattern.length; row++) {
            ItemStack[] patternRow = fullPattern[row];
            if (patternRow == null) continue;

            for (int col = 0; col < patternRow.length; col++) {
                ItemStack item = patternRow[col];
                if (item != null && !item.getType().isAir()) {
                    if (minRow == -1) minRow = row;
                    maxRow = row;
                    if (minCol == -1 || col < minCol) minCol = col;
                    if (col > maxCol) maxCol = col;
                }
            }
        }

        // Jeśli brak przedmiotów, to pusty pattern
        if (minRow == -1) {
            this.pattern = new ItemStack[0][0];
            this.width = 0;
            this.height = 0;
            return;
        }

        // Wytnij najmniejszy prostokąt
        this.height = maxRow - minRow + 1;
        this.width = maxCol - minCol + 1;
        this.pattern = new ItemStack[this.height][this.width];

        for (int row = minRow; row <= maxRow; row++) {
            ItemStack[] sourceRow = fullPattern[row];
            if (sourceRow == null) continue;

            for (int col = minCol; col <= maxCol; col++) {
                ItemStack item = sourceRow[col];
                if (item != null && !item.getType().isAir()) {
                    // Klonuj item aby nie modyfikować oryginału
                    this.pattern[row - minRow][col - minCol] = item.clone();
                } else {
                    this.pattern[row - minRow][col - minCol] = null;
                }
            }
        }
    }

    /**
     * Pobierz faktyczny wzór (już wyczyszczony).
     * 
     * @return tablica 2D pattern'u
     */
    public ItemStack[][] getPattern() {
        return pattern;
    }

    /**
     * Pobierz szerokość (liczbę kolumn) wzoru.
     * 
     * @return liczba kolumn
     */
    public int getWidth() {
        return width;
    }

    /**
     * Pobierz wysokość (liczbę wierszy) wzoru.
     * 
     * @return liczba wierszy
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sprawdź czy wzór jest pusty (brak przedmiotów).
     * 
     * @return true jeśli pattern nie zawiera żadnych przedmiotów
     */
    public boolean isEmpty() {
        return width == 0 || height == 0;
    }

    /**
     * Zwróć string reprezentujący wymiary (np "2x2", "3x4").
     * Przydatne do logowania i debugowania.
     * 
     * @return string "WxH" (width x height)
     */
    @Override
    public String toString() {
        return width + "x" + height;
    }
}

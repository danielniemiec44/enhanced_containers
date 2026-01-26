package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import org.bukkit.inventory.ItemStack;

/**
 * Przechowuje szczegóły znalezionego matcha receptury na gridzie.
 * Zawiera dokładną pozycję, użytą transformację patternu i info czy znaleziono match.
 */
public class PatternMatchResult {
    private final boolean matched;
    private final ItemStack[][] transformedPattern;
    private final int startRow;
    private final int startCol;

    // Konstruktor dla braku matcha
    public PatternMatchResult() {
        this.matched = false;
        this.transformedPattern = null;
        this.startRow = -1;
        this.startCol = -1;
    }

    // Konstruktor dla znalezionego matcha
    public PatternMatchResult(ItemStack[][] transformedPattern, int startRow, int startCol) {
        this.matched = true;
        this.transformedPattern = transformedPattern;
        this.startRow = startRow;
        this.startCol = startCol;
    }

    /**
     * Czy znaleziono match?
     */
    public boolean isMatched() {
        return matched;
    }

    /**
     * Pattern po zastosowaniu transformacji (rotacja/flip) który pasował do gridu.
     * Null jeśli nie znaleziono matcha.
     */
    public ItemStack[][] getTransformedPattern() {
        return transformedPattern;
    }

    /**
     * Wiersz (0-3) gdzie zaczyna się pattern na gridzie.
     * -1 jeśli nie znaleziono matcha.
     */
    public int getStartRow() {
        return startRow;
    }

    /**
     * Kolumna (0-3) gdzie zaczyna się pattern na gridzie.
     * -1 jeśli nie znaleziono matcha.
     */
    public int getStartCol() {
        return startCol;
    }
}

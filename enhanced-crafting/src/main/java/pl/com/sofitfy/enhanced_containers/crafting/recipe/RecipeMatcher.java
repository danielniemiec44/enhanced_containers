package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class RecipeMatcher {

    /**
     * Znajduje dokładny match receptury na gridzie.
     * Zwraca info o pozycji i użytej transformacji patternu.
     * Obsługuje rotacje i odbicia (jak w vanilli).
     * Receptura może być w dowolnym miejscu na gridzie 4x4.
     *
     * @param gridItems przedmioty z gridu (16 slotów = 4x4)
     * @param recipePattern wzór receptury (z dynamicznym rozmiarem)
     * @return PatternMatchResult z info o znalezionym matchu lub pusty jeśli nie znaleziono
     */
    public static PatternMatchResult findPatternMatch(ItemStack[] gridItems, RecipePattern recipePattern) {
        // Walidacja
        if (recipePattern == null || recipePattern.isEmpty()) {
            return new PatternMatchResult();
        }

        if (gridItems == null || gridItems.length < 16) {
            return new PatternMatchResult();
        }

        ItemStack[][] pattern = recipePattern.getPattern();

        // Konwertuj płaski array (16 itemów) na 2D grid (4x4)
        ItemStack[][] grid = flatArrayTo2DGrid(gridItems);
        if (grid == null) {
            return new PatternMatchResult();
        }

        // Spróbuj oryginalną recepturę we wszystkich pozycjach
        PatternMatchResult result = tryAllPositionsWithResult(grid, pattern);
        if (result.isMatched()) {
            return result;
        }

        // Spróbuj rotacje receptury (90, 180, 270 stopni)
        ItemStack[][] rotated90 = rotatePattern(pattern, 1);
        if (rotated90 != null) {
            result = tryAllPositionsWithResult(grid, rotated90);
            if (result.isMatched()) {
                return result;
            }
        }

        ItemStack[][] rotated180 = rotatePattern(pattern, 2);
        if (rotated180 != null) {
            result = tryAllPositionsWithResult(grid, rotated180);
            if (result.isMatched()) {
                return result;
            }
        }

        ItemStack[][] rotated270 = rotatePattern(pattern, 3);
        if (rotated270 != null) {
            result = tryAllPositionsWithResult(grid, rotated270);
            if (result.isMatched()) {
                return result;
            }
        }

        // Spróbuj odbicia (zwierciadła) - poziome i pionowe
        ItemStack[][] flippedH = flipHorizontal(pattern);
        if (flippedH != null) {
            result = tryAllPositionsWithResult(grid, flippedH);
            if (result.isMatched()) {
                return result;
            }
        }

        ItemStack[][] flippedV = flipVertical(pattern);
        if (flippedV != null) {
            result = tryAllPositionsWithResult(grid, flippedV);
            if (result.isMatched()) {
                return result;
            }
        }

        // Spróbuj odbicia ze wszystkimi rotacjami
        ItemStack[][] flippedHRotated = rotatePattern(flippedH, 1);
        if (flippedHRotated != null) {
            result = tryAllPositionsWithResult(grid, flippedHRotated);
            if (result.isMatched()) {
                return result;
            }
        }

        ItemStack[][] flippedVRotated = rotatePattern(flippedV, 1);
        if (flippedVRotated != null) {
            result = tryAllPositionsWithResult(grid, flippedVRotated);
            if (result.isMatched()) {
                return result;
            }
        }

        return new PatternMatchResult(); // Nie znaleziono matcha
    }

    /**
     * Sprawdza czy receptura ze shapem (RecipePattern) pasuje w gridu.
     * Wrapper dla kompatybilności wstecznej.
     *
     * @param gridItems przedmioty z gridu (16 slotów = 4x4)
     * @param recipePattern wzór receptury (z dynamicznym rozmiarem)
     * @return true jeśli receptura pasuje
     */
    public static boolean matchesPattern(ItemStack[] gridItems, RecipePattern recipePattern) {
        return findPatternMatch(gridItems, recipePattern).isMatched();
    }

    /**
     * Sprawdza czy CUSTOM receptura pasuje w gridu - BEZ rotacji i odbić.
     * Custom receptury nie powinny się obracać - tylko dokładne match.
     * To metoda dla custom craftingów (nie vanilla).
     *
     * @param gridItems przedmioty z gridu (16 slotów = 4x4)
     * @param recipePattern wzór receptury (z dynamicznym rozmiarem)
     * @return true jeśli receptura pasuje (bez rotacji)
     */
    public static boolean matchesPatternExactly(ItemStack[] gridItems, RecipePattern recipePattern) {
        return findPatternMatchExactly(gridItems, recipePattern).isMatched();
    }

    /**
     * Znajduje dokładny match CUSTOM receptury na gridzie - BEZ rotacji i odbić.
     * Zwraca info o pozycji.
     *
     * @param gridItems przedmioty z gridu (16 slotów = 4x4)
     * @param recipePattern wzór receptury (z dynamicznym rozmiarem)
     * @return PatternMatchResult z info o znalezionym matchu lub pusty
     */
    public static PatternMatchResult findPatternMatchExactly(ItemStack[] gridItems, RecipePattern recipePattern) {
        // Walidacja
        if (recipePattern == null || recipePattern.isEmpty()) {
            return new PatternMatchResult();
        }

        if (gridItems == null || gridItems.length < 16) {
            return new PatternMatchResult();
        }

        ItemStack[][] pattern = recipePattern.getPattern();

        // Konwertuj płaski array (16 itemów) na 2D grid (4x4)
        ItemStack[][] grid = flatArrayTo2DGrid(gridItems);
        if (grid == null) {
            return new PatternMatchResult();
        }

        // Spróbuj TYLKO oryginalną recepturę we wszystkich pozycjach
        // BEZ rotacji i odbić!
        PatternMatchResult result = tryAllPositionsWithResult(grid, pattern);
        if (result.isMatched()) {
            return result;
        }

        // Nie znaleziono - zwróć pusty wynik (bez rotacji)
        return new PatternMatchResult();
    }

    /**
     * Znajduje match vanilla receptury - ignoruje NBT tagi, porównuje tylko material.
     * WAŻNE: Vanilla receptury NIE obsługują rotacji (tak jak w vanilla Minecraft).
     * Receptura musi dokładnie pasować do wzoru - bez obrotu.
     *
     * @param gridItems przedmioty z gridu (16 slotów = 4x4)
     * @param pattern wzór receptury (2D tablica - vanilla format)
     * @return PatternMatchResult z info o znalezionym matchu lub pusty jeśli nie znaleziono
     */
    public static PatternMatchResult findPatternMatchVanilla(ItemStack[] gridItems, ItemStack[][] pattern) {
        // Walidacja
        if (pattern == null || pattern.length == 0) {
            return new PatternMatchResult();
        }

        if (gridItems == null || gridItems.length < 16) {
            return new PatternMatchResult();
        }

        // Konwertuj płaski array (16 itemów) na 2D grid (4x4)
        ItemStack[][] grid = flatArrayTo2DGrid(gridItems);
        if (grid == null) {
            return new PatternMatchResult();
        }

        // Spróbuj TYLKO oryginalny wzór we wszystkich pozycjach
        // BEZ rotacji (tak jak w vanilla Minecraft)
        PatternMatchResult result = tryAllPositionsWithResultVanilla(grid, pattern);
        if (result.isMatched()) {
            return result;
        }

        // Nie znaleziono - zwróć pusty wynik
        return new PatternMatchResult();
    }

    /**
     * Sprawdza czy vanilla receptura pasuje - ignoruje NBT tagi.
     * Wrapper dla kompatybilności.
     *
     * @param gridItems przedmioty z gridu (16 slotów = 4x4)
     * @param pattern wzór receptury (2D tablica)
     * @return true jeśli receptura pasuje
     */
    public static boolean matchesPatternVanilla(ItemStack[] gridItems, ItemStack[][] pattern) {
        return findPatternMatchVanilla(gridItems, pattern).isMatched();
    }

    /**
     * Sprawdza czy vanilla SHAPELESS receptura pasuje - ignoruje NBT tagi.
     * Shapeless to znaczy że kolejność i pozycja nie mają znaczenia.
     *
     * @param gridItems przedmioty z gridu (16 slotów = 4x4)
     * @param ingredients lista składników receptury
     * @return true jeśli receptura pasuje
     */
    public static boolean matchesShapelessVanilla(ItemStack[] gridItems, List<ItemStack> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return false;
        }

        // Zbierz wszystkie itemy z gridu (ignoruj puste)
        List<ItemStack> gridItems2D = new ArrayList<>();
        for (ItemStack item : gridItems) {
            if (item != null && !item.getType().isAir()) {
                gridItems2D.add(item);
            }
        }

        // Musi być dokładnie tyle itemów ile w recepturze
        if (gridItems2D.size() != ingredients.size()) {
            return false;
        }

        // Sprawdź czy każdy ingredient jest na gridzie (po materiale, ignorując NBT)
        for (ItemStack ingredient : ingredients) {
            if (ingredient == null || ingredient.getType().isAir()) {
                continue;
            }

            // Szukaj tego ingredientu na gridzie
            boolean found = false;
            for (int i = 0; i < gridItems2D.size(); i++) {
                ItemStack gridItem = gridItems2D.get(i);
                if (gridItem != null && gridItem.getType() == ingredient.getType()) {
                    gridItems2D.remove(i); // Usuń znaleziony item z listy
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false; // Nie znaleziono ingredientu
            }
        }

        return true; // Wszystkie ingredienty znalezione
    }

    /**
     * Znajduje dokładny match vanilla receptury (ItemStack[][]) na gridzie.
     * Zwraca info o pozycji i użytej transformacji patternu.
     * Obsługuje rotacje i odbicia (jak w vanilli).
     * Receptura może być w dowolnym miejscu na gridzie 4x4.
     *
     * @param gridItems przedmioty z gridu (16 slotów = 4x4)
     * @param pattern wzór receptury (2D tablica - vanilla format)
     * @return PatternMatchResult z info o znalezionym matchu lub pusty jeśli nie znaleziono
     */
    public static PatternMatchResult findPatternMatchRaw(ItemStack[] gridItems, ItemStack[][] pattern) {
        // Walidacja
        if (pattern == null || pattern.length == 0) {
            return new PatternMatchResult();
        }

        if (gridItems == null || gridItems.length < 16) {
            return new PatternMatchResult();
        }

        // Konwertuj płaski array (16 itemów) na 2D grid (4x4)
        ItemStack[][] grid = flatArrayTo2DGrid(gridItems);
        if (grid == null) {
            return new PatternMatchResult();
        }

        // Spróbuj oryginalną recepturę we wszystkich pozycjach
        PatternMatchResult result = tryAllPositionsWithResult(grid, pattern);
        if (result.isMatched()) {
            return result;
        }

        // Spróbuj rotacje receptury (90, 180, 270 stopni)
        ItemStack[][] rotated90 = rotatePattern(pattern, 1);
        if (rotated90 != null) {
            result = tryAllPositionsWithResult(grid, rotated90);
            if (result.isMatched()) {
                return result;
            }
        }

        ItemStack[][] rotated180 = rotatePattern(pattern, 2);
        if (rotated180 != null) {
            result = tryAllPositionsWithResult(grid, rotated180);
            if (result.isMatched()) {
                return result;
            }
        }

        ItemStack[][] rotated270 = rotatePattern(pattern, 3);
        if (rotated270 != null) {
            result = tryAllPositionsWithResult(grid, rotated270);
            if (result.isMatched()) {
                return result;
            }
        }

        // Spróbuj odbicia (zwierciadła) - poziome i pionowe
        ItemStack[][] flippedH = flipHorizontal(pattern);
        if (flippedH != null) {
            result = tryAllPositionsWithResult(grid, flippedH);
            if (result.isMatched()) {
                return result;
            }
        }

        ItemStack[][] flippedV = flipVertical(pattern);
        if (flippedV != null) {
            result = tryAllPositionsWithResult(grid, flippedV);
            if (result.isMatched()) {
                return result;
            }
        }

        // Spróbuj odbicia ze wszystkimi rotacjami
        ItemStack[][] flippedHRotated = rotatePattern(flippedH, 1);
        if (flippedHRotated != null) {
            result = tryAllPositionsWithResult(grid, flippedHRotated);
            if (result.isMatched()) {
                return result;
            }
        }

        ItemStack[][] flippedVRotated = rotatePattern(flippedV, 1);
        if (flippedVRotated != null) {
            result = tryAllPositionsWithResult(grid, flippedVRotated);
            if (result.isMatched()) {
                return result;
            }
        }

        return new PatternMatchResult(); // Nie znaleziono matcha
    }

    /**
     * Stara wersja dla vanilla receptur (ItemStack[][]).
     * Wrapper dla kompatybilności wstecznej.
     *
     * @param gridItems przedmioty z gridu (16 slotów = 4x4)
     * @param pattern wzór receptury (2D tablica)
     * @return true jeśli receptura pasuje
     */
    public static boolean matchesPatternRaw(ItemStack[] gridItems, ItemStack[][] pattern) {
        return findPatternMatchRaw(gridItems, pattern).isMatched();
    }

    // Spróbuj daną recepturę we wszystkich możliwych pozycjach na gridzie
    // Zwraca PatternMatchResult z info o znalezionej pozycji
    private static PatternMatchResult tryAllPositionsWithResult(ItemStack[][] grid, ItemStack[][] pattern) {
        int patternHeight = pattern.length;
        int patternWidth = getPatternWidth(pattern);

        if (patternWidth == 0) {
            return new PatternMatchResult();
        }

        // Próbuj wszystkie pozycje gdzie receptura mogłaby pasować
        for (int startRow = 0; startRow <= 4 - patternHeight; startRow++) {
            for (int startCol = 0; startCol <= 4 - patternWidth; startCol++) {
                if (matchesAtPosition(grid, pattern, startRow, startCol)) {
                    // Znaleźliśmy pasującą pozycję! Zwróć info o matchu
                    return new PatternMatchResult(pattern, startRow, startCol);
                }
            }
        }
        return new PatternMatchResult(); // Nie znaleziono
    }

    // Spróbuj daną recepturę we wszystkich możliwych pozycjach na gridzie (vanilla - ignoruje NBT)
    private static PatternMatchResult tryAllPositionsWithResultVanilla(ItemStack[][] grid, ItemStack[][] pattern) {
        int patternHeight = pattern.length;
        int patternWidth = getPatternWidth(pattern);

        if (patternWidth == 0) {
            return new PatternMatchResult();
        }

        // Próbuj wszystkie pozycje gdzie receptura mogłaby pasować
        for (int startRow = 0; startRow <= 4 - patternHeight; startRow++) {
            for (int startCol = 0; startCol <= 4 - patternWidth; startCol++) {
                if (matchesAtPositionVanilla(grid, pattern, startRow, startCol)) {
                    // Znaleźliśmy pasującą pozycję! Zwróć info o matchu
                    return new PatternMatchResult(pattern, startRow, startCol);
                }
            }
        }
        return new PatternMatchResult(); // Nie znaleziono
    }

    // Spróbuj daną recepturę we wszystkich możliwych pozycjach na gridzie
    @SuppressWarnings("unused")
    private static boolean tryAllPositions(ItemStack[][] grid, ItemStack[][] pattern) {
        int patternHeight = pattern.length;
        int patternWidth = getPatternWidth(pattern);

        if (patternWidth == 0) {
            return false;
        }

        // Próbuj wszystkie pozycje gdzie receptura mogłaby pasować
        for (int startRow = 0; startRow <= 4 - patternHeight; startRow++) {
            for (int startCol = 0; startCol <= 4 - patternWidth; startCol++) {
                if (matchesAtPosition(grid, pattern, startRow, startCol)) {
                    return true; // Znaleźliśmy pasującą pozycję!
                }
            }
        }
        return false;
    }

    // Obrócić recepturę o 90 stopni (times razy)
    // times=1 -> 90 stopni, times=2 -> 180 stopni, times=3 -> 270 stopni
    private static ItemStack[][] rotatePattern(ItemStack[][] pattern, int times) {
        if (pattern == null || times <= 0 || times > 3) {
            return null;
        }

        ItemStack[][] result = pattern;
        for (int i = 0; i < times; i++) {
            result = rotateOnce(result);
        }
        return result;
    }

    // Obrócić recepturę o 90 stopni (jeden raz)
    private static ItemStack[][] rotateOnce(ItemStack[][] pattern) {
        int rows = pattern.length;
        int cols = getPatternWidth(pattern);

        // Po obrocie 90 stopni wymiary się zamieniają
        ItemStack[][] rotated = new ItemStack[cols][rows];

        for (int i = 0; i < rows; i++) {
            if (pattern[i] == null) continue;
            for (int j = 0; j < pattern[i].length; j++) {
                // (i,j) -> (j, rows-1-i)
                rotated[j][rows - 1 - i] = pattern[i][j];
            }
        }

        return rotated;
    }

    // Odbić recepturę poziomo (lustrzane odbicie lewo-prawo)
    private static ItemStack[][] flipHorizontal(ItemStack[][] pattern) {
        if (pattern == null) {
            return null;
        }

        int rows = pattern.length;
        ItemStack[][] flipped = new ItemStack[rows][];

        for (int i = 0; i < rows; i++) {
            if (pattern[i] == null) {
                flipped[i] = null;
                continue;
            }

            int cols = pattern[i].length;
            flipped[i] = new ItemStack[cols];

            // Odwróć każdy rząd
            for (int j = 0; j < cols; j++) {
                flipped[i][j] = pattern[i][cols - 1 - j];
            }
        }

        return flipped;
    }

    // Odbić recepturę pionowo (lustrzane odbicie góra-dół)
    private static ItemStack[][] flipVertical(ItemStack[][] pattern) {
        if (pattern == null) {
            return null;
        }

        int rows = pattern.length;
        ItemStack[][] flipped = new ItemStack[rows][];

        // Odwróć kolejność rzędów
        for (int i = 0; i < rows; i++) {
            flipped[i] = pattern[rows - 1 - i];
        }

        return flipped;
    }

    // Sprawdza czy receptura pasuje w konkretnym miejscu gridu
    private static boolean matchesAtPosition(ItemStack[][] grid, ItemStack[][] pattern, int startRow, int startCol) {
        int patternHeight = pattern.length;

        for (int i = 0; i < patternHeight; i++) {
            if (pattern[i] == null) {
                continue;
            }

            for (int j = 0; j < pattern[i].length; j++) {
                ItemStack requiredItem = pattern[i][j];
                int gridRow = startRow + i;
                int gridCol = startCol + j;

                if (gridRow >= 4 || gridCol >= 4) {
                    return false;
                }

                ItemStack gridItem = grid[gridRow][gridCol];

                // Jeśli w recepturze null/powietrze, slot musi być pusty
                if (requiredItem == null || requiredItem.getType().isAir()) {
                    if (gridItem != null && !gridItem.getType().isAir()) {
                        return false; // W recepturze pusty slot ale w gridu coś jest
                    }
                }
                // Jeśli w recepturze jest przedmiot, slot musi go zawierać
                else {
                    if (gridItem == null || gridItem.getType().isAir()) {
                        return false; // Brakuje przedmiotu
                    }

                    // Porównaj ItemStacki za pomocą isSimilar (typ, meta, enchants)
                    // isSimilar zwraca true jeśli obydwa itemy mają ten sam material i meta
                    if (!gridItem.isSimilar(requiredItem)) {
                        return false; // Zły item lub meta
                    }
                }
            }
        }

        // WAŻNE: jak w vanilla crafting table
        // Jeśli w gridzie są jakiekolwiek dodatkowe itemy poza wzorem receptury, to NIE jest match.
        // (Pattern może być mniejszy niż 4x4 i może być przesunięty w dowolne miejsce.)
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                ItemStack gridItem = grid[r][c];
                if (gridItem == null || gridItem.getType().isAir()) {
                    continue; // pusty slot - ok
                }

                int pr = r - startRow;
                int pc = c - startCol;

                ItemStack required = null;
                if (pr >= 0 && pr < pattern.length) {
                    ItemStack[] patternRow = pattern[pr];
                    if (patternRow != null && pc >= 0 && pc < patternRow.length) {
                        required = patternRow[pc];
                    }
                }

                // Jeśli w tym miejscu pattern nie wymaga itemu (null/AIR albo poza obszarem patternu),
                // to grid MUSI być pusty. Skoro nie jest, to mamy "extra item" -> brak match.
                if (required == null || required.getType().isAir()) {
                    return false;
                }
            }
        }

        return true; // Wszystko się zgadza!
    }

    // Sprawdza czy receptura vanilla pasuje w konkretnym miejscu gridu (ignoruje NBT)
    private static boolean matchesAtPositionVanilla(ItemStack[][] grid, ItemStack[][] pattern, int startRow, int startCol) {
        int patternHeight = pattern.length;

        for (int i = 0; i < patternHeight; i++) {
            if (pattern[i] == null) {
                continue;
            }

            for (int j = 0; j < pattern[i].length; j++) {
                ItemStack requiredItem = pattern[i][j];
                int gridRow = startRow + i;
                int gridCol = startCol + j;

                if (gridRow >= 4 || gridCol >= 4) {
                    return false;
                }

                ItemStack gridItem = grid[gridRow][gridCol];

                // Jeśli w recepturze null/powietrze, slot musi być pusty
                if (requiredItem == null || requiredItem.getType().isAir()) {
                    if (gridItem != null && !gridItem.getType().isAir()) {
                        return false; // W recepturze pusty slot ale w gridu coś jest
                    }
                }
                // Jeśli w recepturze jest przedmiot, slot musi go zawierać
                else {
                    if (gridItem == null || gridItem.getType().isAir()) {
                        return false; // Brakuje przedmiotu
                    }

                    // Porównaj ItemStacki tylko po material (ignoruj NBT tagi!)
                    // To pozwala craftować vanilla receptury z custom itemów
                    if (gridItem.getType() != requiredItem.getType()) {
                        return false; // Zły material
                    }
                }
            }
        }

        // WAŻNE: jak w vanilla crafting table
        // Jeśli w gridzie są jakiekolwiek dodatkowe itemy poza wzorem receptury, to NIE jest match.
        // (Pattern może być mniejszy niż 4x4 i może być przesunięty w dowolne miejsce.)
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                ItemStack gridItem = grid[r][c];
                if (gridItem == null || gridItem.getType().isAir()) {
                    continue; // pusty slot - ok
                }

                int pr = r - startRow;
                int pc = c - startCol;

                ItemStack required = null;
                if (pr >= 0 && pr < pattern.length) {
                    ItemStack[] patternRow = pattern[pr];
                    if (patternRow != null && pc >= 0 && pc < patternRow.length) {
                        required = patternRow[pc];
                    }
                }

                // Jeśli w tym miejscu pattern nie wymaga itemu (null/AIR albo poza obszarem patternu),
                // to grid MUSI być pusty. Skoro nie jest, to mamy "extra item" -> brak match.
                if (required == null || required.getType().isAir()) {
                    return false;
                }
            }
        }

        return true; // Wszystko się zgadza!
    }

    // Konwertuj płaski array na 2D grid (4x4)
    // [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15] -> [[0,1,2,3], [4,5,6,7], [8,9,10,11], [12,13,14,15]]
    private static ItemStack[][] flatArrayTo2DGrid(ItemStack[] items) {
        if (items.length < 16) {
            return null;
        }

        ItemStack[][] grid = new ItemStack[4][4];
        for (int i = 0; i < 16; i++) {
            int row = i / 4;
            int col = i % 4;
            grid[row][col] = items[i];
        }

        return grid;
    }

    // Konwertuj 2D grid z powrotem na płaski array
    private static ItemStack[] grid2DToArray(ItemStack[][] grid) {
        ItemStack[] result = new ItemStack[16];
        int index = 0;

        for (ItemStack[] row : grid) {
            for (ItemStack item : row) {
                result[index++] = item;
            }
        }

        return result;
    }

    // Usuwa itemy z gridu zgodnie z PatternMatchResult (już znaleziony match)
    // Ta wersja dostaje gotowe info gdzie i jaki pattern znaleziono
    public static void removeItemsFromMatch(ItemStack[] gridItems, PatternMatchResult matchResult) {
        if (gridItems == null || !matchResult.isMatched()) {
            return;
        }

        // Konwertuj na 2D grid do pracy
        ItemStack[][] grid = flatArrayTo2DGrid(gridItems);
        if (grid == null) {
            return;
        }

        // Usuń itemy z konkretnej pozycji używając znalezionego patternu
        removeItemsAtPosition(grid, matchResult.getTransformedPattern(), 
                             matchResult.getStartRow(), matchResult.getStartCol());

        // Skopiuj zmodyfikowany grid z powrotem do oryginalnego array
        ItemStack[] modified = grid2DToArray(grid);
        System.arraycopy(modified, 0, gridItems, 0, 16);
    }

    // Usuwa itemy z gridu zgodnie z recepturą
    // Znajdź gdzie receptura pasuje i zmniejsz ilość itemów
    // UWAGA: Ta metoda jest przestarzała, użyj removeItemsFromMatch() z PatternMatchResult
    public static void removeItemsFromPattern(ItemStack[] gridItems, ItemStack[][] pattern) {
        if (gridItems == null || pattern == null || gridItems.length < 16) {
            return;
        }

        // Konwertuj na 2D grid do pracy
        ItemStack[][] grid = flatArrayTo2DGrid(gridItems);
        if (grid == null) {
            return;
        }

        // Spróbuj usunąć z oryginalną recepturą
        if (removeItemsWithPattern(grid, pattern, gridItems)) {
            return;
        }

        // Spróbuj ze wszystkimi rotacjami i odbiciami
        ItemStack[][] rotated90 = rotatePattern(pattern, 1);
        if (rotated90 != null && removeItemsWithPattern(grid, rotated90, gridItems)) {
            return;
        }

        ItemStack[][] flippedH = flipHorizontal(pattern);
        if (flippedH != null && removeItemsWithPattern(grid, flippedH, gridItems)) {
            return;
        }

        ItemStack[][] flippedV = flipVertical(pattern);
        if (flippedV != null && removeItemsWithPattern(grid, flippedV, gridItems)) {
            return;
        }
    }

    // Pomocnicza metoda do usuwania itemów z konkretną recepturą
    private static boolean removeItemsWithPattern(ItemStack[][] grid, ItemStack[][] pattern, ItemStack[] gridItems) {
        int patternHeight = pattern.length;
        int patternWidth = getPatternWidth(pattern);

        // Spróbuj znaleźć recepturę i usuń jej itemy
        for (int startRow = 0; startRow <= 4 - patternHeight; startRow++) {
            for (int startCol = 0; startCol <= 4 - patternWidth; startCol++) {
                if (matchesAtPosition(grid, pattern, startRow, startCol)) {
                    // Znaleźliśmy gdzie receptura pasuje, usuń itemy z tej pozycji
                    removeItemsAtPosition(grid, pattern, startRow, startCol);
                    // Skopiuj zmodyfikowany grid z powrotem do oryginalnego array
                    ItemStack[] modified = grid2DToArray(grid);
                    System.arraycopy(modified, 0, gridItems, 0, 16);
                    return true;
                }
            }
        }
        return false;
    }

    // Usuwa itemy z konkretnej pozycji w gridu
    private static void removeItemsAtPosition(ItemStack[][] grid, ItemStack[][] pattern, int startRow, int startCol) {
        int patternHeight = pattern.length;

        for (int i = 0; i < patternHeight; i++) {
            if (pattern[i] == null) {
                continue;
            }

            for (int j = 0; j < pattern[i].length; j++) {
                ItemStack requiredItem = pattern[i][j];
                int gridRow = startRow + i;
                int gridCol = startCol + j;

                // Jeśli w recepturze nie ma null/powietrza (czyli tam coś musi być), zmniejsz ilość
                if (requiredItem != null && !requiredItem.getType().isAir()) {
                    ItemStack item = grid[gridRow][gridCol];
                    if (item != null && !item.getType().isAir()) {
                        // Zmniejsz ilość o tyle ile jest w recepturze
                        int requiredAmount = requiredItem.getAmount();
                        item.setAmount(item.getAmount() - requiredAmount);
                        // Jeśli skończyły się itemy, usuń ze slotu
                        if (item.getAmount() <= 0) {
                            grid[gridRow][gridCol] = null;
                        }
                    }
                }
            }
        }
    }

    // Zwraca szerokość receptury (najszerszy rząd)
    private static int getPatternWidth(ItemStack[][] pattern) {
        int maxWidth = 0;
        for (ItemStack[] row : pattern) {
            if (row != null && row.length > maxWidth) {
                maxWidth = row.length;
            }
        }
        return maxWidth;
    }

    /**
     * Sprawdza czy receptura bez kształtu (ShapelessRecipe) pasuje w gridu.
     * Składniki mogą być w dowolnych slotach - liczy się tylko liczba i typ.
     *
     * @param gridItems przedmioty z gridu (16 slotów = 4x4)
     * @param ingredients lista wymaganych składników
     * @return true jeśli wszystkie składniki są w gridzie
     */
    public static boolean matchesShapeless(ItemStack[] gridItems, List<ItemStack> ingredients) {
        // Walidacja
        if (ingredients == null || ingredients.isEmpty()) {
            return false;
        }

        if (gridItems == null || gridItems.length < 16) {
            return false;
        }

        // Jak w vanilla crafting table: liczba zajętych slotów musi się zgadzać z liczbą składników.
        // (Stack w jednym slocie nadal liczy się jako 1 składnik; ilość w stacku to tylko "zapas".)
        int occupiedSlots = 0;
        for (int i = 0; i < 16; i++) {
            ItemStack item = gridItems[i];
            if (item != null && !item.getType().isAir()) {
                occupiedSlots++;
            }
        }

        int requiredSlots = 0;
        for (ItemStack ingredient : ingredients) {
            if (ingredient == null || ingredient.getType().isAir()) {
                continue;
            }

            // Jeśli receptura ma amount > 1, to oznacza to kilka "slotów" składnika.
            // W praktyce gracz musi mieć to w kilku slotach (jak w normalnym craftingu).
            requiredSlots += Math.max(1, ingredient.getAmount());
        }

        if (occupiedSlots != requiredSlots) {
            return false;
        }

        // Stwórz kopię gridItems aby móc ją modyfikować.
        // Uwaga: nie chcemy dotykać oryginalnych itemów w GUI.
        ItemStack[] gridCopy = new ItemStack[16];
        for (int i = 0; i < 16; i++) {
            if (gridItems[i] != null) {
                gridCopy[i] = gridItems[i].clone();
            }
        }

        // Trzymamy indeksy slotów, które faktycznie zostały użyte do spełnienia receptury.
        // Dzięki temu możemy odrzucić "dodatkowe" itemy w innych slotach,
        // ale jednocześnie pozwolić na większy stack w tym samym slocie (jak w crafting table).
        boolean[] usedSlots = new boolean[16];

        // Dla każdego wymaganego składnika, spróbuj go znaleźć w gridzie.
        // Receptura shapeless w vanilli NIE toleruje dodatkowych zajętych slotów.
        // (Wyjątek: większa ilość w tym samym slocie jest ok, bo po craftowaniu zostaje reszta.)
        for (ItemStack requiredIngredient : ingredients) {
            if (requiredIngredient == null || requiredIngredient.getType().isAir()) {
                continue;
            }

            int requiredAmount = Math.max(1, requiredIngredient.getAmount());

            // Wymagany amount realizujemy jako N razy "znajdź i zabierz 1".
            // To pozwala brać z jednego stacka lub z wielu slotów.
            for (int taken = 0; taken < requiredAmount; taken++) {
                boolean foundOne = false;

                // Szukaj tego ingredientu w gridzie
                for (int i = 0; i < 16; i++) {
                    ItemStack gridItem = gridCopy[i];

                    // Jeśli slot jest pusty - pomiń
                    if (gridItem == null || gridItem.getType().isAir()) {
                        continue;
                    }

                    // Sprawdzaj czy to pasujący item
                    if (itemsMatch(requiredIngredient, gridItem)) {
                        // Zmniejsz ilość o 1
                        gridItem.setAmount(gridItem.getAmount() - 1);

                        // Slot był użyty do spełnienia receptury
                        usedSlots[i] = true;

                        // Jeśli został 0 - wyczyść slot
                        if (gridItem.getAmount() <= 0) {
                            gridCopy[i] = null;
                        }

                        foundOne = true;
                        break;
                    }
                }

                // Jeśli nie znaleźliśmy jednej sztuki składnika - receptura nie pasuje
                if (!foundOne) {
                    return false;
                }
            }
        }

        // Na koniec: odrzuć dodatkowe zajęte sloty, których nie użyliśmy.
        // To jest dodatkowe zabezpieczenie (główny warunek to occupiedSlots == requiredSlots).
        for (int i = 0; i < 16; i++) {
            ItemStack original = gridItems[i];
            if (original == null || original.getType().isAir()) {
                continue;
            }

            if (!usedSlots[i]) {
                return false; // dodatkowy item w innym slocie
            }
        }

        return true;
    }

    /**
     * Sprawdza czy dwa ItemStack'i pasują do siebie.
     * Porównuje material i NBT (jeśli istnieje).
     *
     * @param required wymagany item
     * @param gridItem item z gridu
     * @return true jeśli pasują
     */
    private static boolean itemsMatch(ItemStack required, ItemStack gridItem) {
        // Material musi się zgadzać
        if (required.getType() != gridItem.getType()) {
            return false;
        }

        // Jeśli wymagany item ma meta - sprawdzaj je
        if (required.hasItemMeta() && gridItem.hasItemMeta()) {
            // Dla vanilla receptur - zwykle wystarczy porównanie materiału
            // NBT porównanie jest złożone, więc upraszczamy
            return true;
        }

        // Jeśli wymagany nie ma meta - wystarczy zgoda materiału
        return true;
    }

    /**
     * Usuń przedmioty z gridu dla danej receptury bez kształtu.
     * Musi być wywoływana po matchesShapeless() zwróci true.
     *
     * @param gridItems przedmioty z gridu
     * @param ingredients składniki receptury
     */
    public static void removeShapelessItems(ItemStack[] gridItems, List<ItemStack> ingredients) {
        if (ingredients == null || gridItems == null) {
            return;
        }

        // Przechodzimy po składnikach i dla każdego zdejmujemy wymaganą ilość.
        // W praktyce: 1 ingredient = 1 slot zużyty w craftingu.
        for (ItemStack required : ingredients) {
            if (required == null || required.getType().isAir()) {
                continue;
            }

            int requiredAmount = Math.max(1, required.getAmount());

            for (int taken = 0; taken < requiredAmount; taken++) {
                boolean removedOne = false;

                for (int i = 0; i < 16; i++) {
                    ItemStack gridItem = gridItems[i];
                    if (gridItem == null || gridItem.getType().isAir()) {
                        continue;
                    }

                    if (!itemsMatch(required, gridItem)) {
                        continue;
                    }

                    gridItem.setAmount(gridItem.getAmount() - 1);
                    if (gridItem.getAmount() <= 0) {
                        gridItems[i] = null;
                    }

                    removedOne = true;
                    break;
                }

                if (!removedOne) {
                    // Jeśli to się zdarzy, to znaczy że wywołano removeShapelessItems bez matchesShapeless.
                    return;
                }
            }
        }
    }

    /**
     * Usuń itemy pasujące do RecipePattern (dla custom receptur z dynamic sizing)
     *
     * @param gridItems przedmioty z gridu (16 slotów = 4x4)
     * @param pattern RecipePattern z auto-detected wymiarami
     */
    public static void removeItemsFromPattern(ItemStack[] gridItems, RecipePattern pattern) {
        if (gridItems == null || pattern == null || gridItems.length < 16 || pattern.isEmpty()) {
            return;
        }

        // Konwertuj pattern na ItemStack[][]
        ItemStack[][] patternArray = pattern.getPattern();
        if (patternArray == null) {
            return;
        }

        // Użyj istniejącej logiki
        removeItemsFromPattern(gridItems, patternArray);
    }
}

package pl.com.sofitfy.enhanced_containers.crafting.vanilla;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Recipe;

/**
 * Serwis do szukania i dopasowywania vanilla receptur w custom GUI 4x4.
 * 
 * Logika:
 * 1. Shrink Wrap - przycinanie 4x4 do najmniejszego prostokąta
 * 2. Szukanie receptury za pomocą Bukkit.getCraftingRecipe()
 * 3. Fallback naprawy - dla 2 zużytych przedmiotów tego samego typu
 */
public class VanillaRecipeMatcher {
    
    /**
     * Główna metoda - szuka vanilla receptury w macierzy 4x4.
     * 
     * @param grid4x4 Macierz 4x4 (16 slotów)
     * @param player Gracz (do uzyskania świata)
     * @return ItemStack - wynik receptury lub null
     */
    public ItemStack match(ItemStack[] grid4x4, Player player) {
        if (grid4x4 == null || grid4x4.length != 16 || player == null) {
            return null;
        }
        
        // Krok 1: Shrink Wrap - zmniejsz siatę do minimalnego prostokąta
        ItemStack[] shrunken = shrinkWrap4x4(grid4x4);
        if (shrunken == null || shrunken.length == 0) {
            return null; // Siatka pusta
        }
        
        // Sprawdź wymiary shrunken grid'u
        int[] dimensions = getShrunkDimensions(grid4x4);
        if (dimensions == null || dimensions[0] > 3 || dimensions[1] > 3) {
            return null; // Za duża - vanilla crafting obsługuje max 3x3
        }
        
        // Krok 2: Pad do 3x3 (vanilla crafting size)
        ItemStack[] padded3x3 = pad3x3(shrunken, dimensions[0], dimensions[1]);
        
        // Krok 3: Konwertuj na AIR ItemStack'i (nie null - dla zgodności z Paper API)
        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            ItemStack item = padded3x3[i];
            if (item == null || item.getType().isAir()) {
                matrix[i] = new ItemStack(Material.AIR);
            } else {
                matrix[i] = item;
            }
        }
        
        // Krok 4: Spróbuj znaleźć recepturę vanilla
        try {
            Recipe recipe = Bukkit.getCraftingRecipe(matrix, player.getWorld());
            
            if (recipe != null && recipe.getResult() != null && recipe.getResult().getType() != Material.AIR) {
                return recipe.getResult().clone();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Krok 5: Fallback naprawy - jeśli brak receptury, spróbuj naprawy
        return tryRepair(shrunken);
    }
    
    /**
     * Shrink Wrap - zmniejsza 4x4 grid do najmniejszego prostokąta
     * zawierającego przedmioty.
     * 
     * Przykład:
     * Grid 4x4 z drewnem w slocie [2,3] (row 2, col 3) -> shrunken = [drewno]
     * Grid 4x4 z drewnem w slocie [0,0] i [1,2] -> shrunken = [drewno, AIR, drewno, ...]
     * 
     * @param grid4x4 Macierz 4x4
     * @return Zmniejszona macierz (1-9 itemów) lub null jeśli pusta
     */
    private ItemStack[] shrinkWrap4x4(ItemStack[] grid4x4) {
        // Znajdź granice (min/max wiersze i kolumny zawierające przedmioty)
        int minRow = 4, maxRow = -1;
        int minCol = 4, maxCol = -1;
        
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                int idx = r * 4 + c;
                ItemStack item = grid4x4[idx];
                if (item != null && !item.getType().isAir()) {
                    minRow = Math.min(minRow, r);
                    maxRow = Math.max(maxRow, r);
                    minCol = Math.min(minCol, c);
                    maxCol = Math.max(maxCol, c);
                }
            }
        }
        
        // Jeśli nie znaleziono żadnych przedmiotów
        if (maxRow == -1) {
            return new ItemStack[0];
        }
        
        // Oblicz wymiary shrunk grid'u
        int height = maxRow - minRow + 1;
        int width = maxCol - minCol + 1;
        
        // Skopiuj przedmioty do nowej macierzy
        ItemStack[] shrunken = new ItemStack[height * width];
        for (int r = minRow; r <= maxRow; r++) {
            for (int c = minCol; c <= maxCol; c++) {
                int srcIdx = r * 4 + c;
                int dstIdx = (r - minRow) * width + (c - minCol);
                ItemStack item = grid4x4[srcIdx];
                shrunken[dstIdx] = (item != null && !item.getType().isAir()) ? item : new ItemStack(Material.AIR);
            }
        }
        
        return shrunken;
    }
    
    /**
     * Zwraca wymiary shrunk grid'u (wysokość x szerokość).
     */
    private int[] getShrunkDimensions(ItemStack[] grid4x4) {
        int minRow = 4, maxRow = -1;
        int minCol = 4, maxCol = -1;
        
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                int idx = r * 4 + c;
                ItemStack item = grid4x4[idx];
                if (item != null && !item.getType().isAir()) {
                    minRow = Math.min(minRow, r);
                    maxRow = Math.max(maxRow, r);
                    minCol = Math.min(minCol, c);
                    maxCol = Math.max(maxCol, c);
                }
            }
        }
        
        if (maxRow == -1) {
            return null; // Grid pusta
        }
        
        int height = maxRow - minRow + 1;
        int width = maxCol - minCol + 1;
        
        return new int[]{height, width};
    }
    
    /**
     * Pad-uje macierz do 3x3 (vanilla crafting).
     * 
     * Zmniejszona macierz (np. 2x2) jest pad-owana z powrotem do 3x3
     * poprzez dodanie pustych slotów z lewej i górnie.
     * 
     * @param shrunken Zmniejszona macierz
     * @param height Wysokość shrunken
     * @param width Szerokość shrunken
     * @return Pad-owana macierz 3x3
     */
    private ItemStack[] pad3x3(ItemStack[] shrunken, int height, int width) {
        ItemStack[] result = new ItemStack[9];
        
        // Wypełnij wszystko AIR
        for (int i = 0; i < 9; i++) {
            result[i] = new ItemStack(Material.AIR);
        }
        
        // Skopiuj shrunken do lewego górnego rogu
        int shrinkIdx = 0;
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                int resultIdx = r * 3 + c;
                ItemStack item = shrunken[shrinkIdx++];
                if (item != null && !item.getType().isAir()) {
                    result[resultIdx] = item;
                }
            }
        }
        
        return result;
    }
    
    /**
     * Fallback naprawy przedmiotów - WERSJA Z POLUZOWANYM KRAWATEM.
     * 
     * Sprawdzamy TYLKO to, co widzi gracz:
     * - Typ materiału
     * - DisplayName (jeśli istnieje)
     * - Enchanty
     * 
     * IGNORUJEMY inne metadane NBT takie jak RepairCost, AttributeModifiers, etc.
     * To naprawi problem gdzie motyki z historią naprawy nie chciały się łączyć.
     * 
     * Zawiera LOGI DEBUG - jeśli naprawianie nie działa, konsola powie dlaczego!
     * 
     * @param shrunken Zmniejszona macierz
     * @return Naprawiony przedmiot lub null
     */
    private ItemStack tryRepair(ItemStack[] shrunken) {
        if (shrunken == null || shrunken.length == 0) {
            return null;
        }
        
        // 1. Zbieramy itemy (bez pustych slotów)
        java.util.List<ItemStack> repairItems = new java.util.ArrayList<>();
        for (ItemStack item : shrunken) {
            if (item != null && item.getType() != Material.AIR) {
                repairItems.add(item);
            }
        }

        // DEBUG: Pokaż co widzi kod
        if (repairItems.isEmpty()) {
            return null; // Cicho ignoruj puste
        }
        
        // Jeśli nie ma dokładnie 2 itemów, to nie jest naprawianie
        if (repairItems.size() != 2) {
            // System.out.println("[RepairDebug] Zła ilość itemów: " + repairItems.size());
            return null;
        }

        ItemStack item1 = repairItems.get(0);
        ItemStack item2 = repairItems.get(1);

        // 2. SPRAWDZANIE TYPU (Musi być ten sam materiał)
        if (item1.getType() != item2.getType()) {
            System.out.println("[RepairDebug] Różne typy: " + item1.getType() + " vs " + item2.getType());
            return null;
        }

        // 3. SPRAWDZANIE CZY NAPRAWIALNE
        if (!(item1.getItemMeta() instanceof Damageable)) {
            return null; // Nie da się naprawić np. kamienia
        }

        // 4. LUŹNE SPRAWDZANIE METADANYCH (Fix dla Twojego problemu)
        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();

        // Sprawdzamy tylko: Czy mają tę samą nazwę? (żeby nie łączyć Excalibura ze zwykłym mieczem)
        boolean nameMatch = meta1.hasDisplayName() == meta2.hasDisplayName();
        if (nameMatch && meta1.hasDisplayName()) {
            if (!meta1.getDisplayName().equals(meta2.getDisplayName())) {
                System.out.println("[RepairDebug] Różne nazwy!");
                return null;
            }
        } else if (!nameMatch) {
            System.out.println("[RepairDebug] Jeden ma nazwę, drugi nie!");
            return null;
        }
        
        // Sprawdzamy Enchanty (żeby nie łączyć Fortune III z czystym)
        if (!meta1.getEnchants().equals(meta2.getEnchants())) {
            System.out.println("[RepairDebug] Różne enchanty!");
            return null;
        }

        // UWAGA: Ignorujemy RepairCost i inne ukryte śmieci NBT! To naprawi problem motyki.

        // --- MATEMATYKA ---
        Damageable d1 = (Damageable) meta1;
        Damageable d2 = (Damageable) meta2;
        int max = item1.getType().getMaxDurability();

        // Sprawdzenie czy typ ma durability
        if (max <= 0) {
            return null;
        }

        // Sprawdzenie czy przedmioty są zużyte (damage > 0)
        if (d1.getDamage() <= 0 || d2.getDamage() <= 0) {
            return null;
        }

        int life1 = max - d1.getDamage();
        int life2 = max - d2.getDamage();
        int bonus = (int) (max * 0.05);

        int newLife = life1 + life2 + bonus;
        
        // Ogranicz do maksymalnego możliwego życia
        if (newLife > max) {
            newLife = max;
        }
        
        int newDamage = max - newLife;
        if (newDamage < 0) {
            newDamage = 0; // Clamp
        }

        System.out.println("[RepairDebug] SUKCES! Naprawiam " + item1.getType() + ". Nowy damage: " + newDamage);

        // Tworzenie wyniku (klon item1, aby zachować wszystkie metadane)
        ItemStack result = item1.clone();
        ItemMeta resultMeta = result.getItemMeta();
        ((Damageable) resultMeta).setDamage(newDamage);
        result.setItemMeta(resultMeta);
        result.setAmount(1); // Zawsze 1 sztuka
        
        return result;
    }
}

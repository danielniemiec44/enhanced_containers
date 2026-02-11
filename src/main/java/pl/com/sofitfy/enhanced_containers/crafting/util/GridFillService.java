package pl.com.sofitfy.enhanced_containers.crafting.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.*;

/**
 * Wspólny serwis do wypełniania siatki craftingu składnikami z ekwipunku gracza.
 * 
 * Używany przez:
 * - /4x4 (CraftingGUI Fast Crafting)
 * - SharedCrafting (Recipe Book via NMS)
 * - SharedCrafting (Recipe Book via Bukkit fallback)
 * 
 * Logika oparta na CraftingAutofillPlanner - zbiera WSZYSTKIE pasujące itemy
 * z ekwipunku i rozdziela je równomiernie na sloty siatki.
 */
public final class GridFillService {
    
    private GridFillService() {}
    
    /**
     * Wynik operacji wypełniania siatki.
     */
    public record FillResult(
        boolean success,
        Map<Integer, ItemStack> gridItems,  // gridSlot -> ItemStack do wstawienia
        Map<Integer, Integer> consumeOps    // playerSlot -> ilość do usunięcia
    ) {
        public static FillResult failure() {
            return new FillResult(false, Map.of(), Map.of());
        }
        
        public static FillResult success(Map<Integer, ItemStack> gridItems, Map<Integer, Integer> consumeOps) {
            return new FillResult(true, gridItems, consumeOps);
        }
    }
    
    /**
     * Planuje wypełnienie siatki 3x3 (vanilla crafting table) na podstawie ShapedRecipe.
     * 
     * @param playerInventory Snapshot ekwipunku gracza (getStorageContents)
     * @param recipe Przepis do wypełnienia
     * @param useMaxItems Czy wypełnić maksymalną ilością (shift-click) czy pojedynczo
     * @return FillResult z planem lub failure
     */
    public static FillResult planForShapedRecipe(ItemStack[] playerInventory, ShapedRecipe recipe, boolean useMaxItems) {
        String[] shape = recipe.getShape();
        Map<Character, org.bukkit.inventory.RecipeChoice> choiceMap = recipe.getChoiceMap();
        
        // Zbierz wymagane składniki: gridSlot (1-9) -> RecipeChoice
        Map<Integer, org.bukkit.inventory.RecipeChoice> requiredChoices = new LinkedHashMap<>();
        
        for (int row = 0; row < shape.length; row++) {
            String rowPattern = shape[row];
            for (int col = 0; col < rowPattern.length(); col++) {
                char c = rowPattern.charAt(col);
                if (c == ' ') continue;
                
                org.bukkit.inventory.RecipeChoice choice = choiceMap.get(c);
                if (choice == null) continue;
                
                int gridSlot = 1 + row * 3 + col; // Sloty 1-9 w siatce 3x3
                requiredChoices.put(gridSlot, choice);
            }
        }
        
        return planFill(playerInventory, requiredChoices, useMaxItems, (item, choice) -> choice.test(item));
    }
    
    /**
     * Planuje wypełnienie siatki 3x3 (vanilla crafting table) na podstawie ShapelessRecipe.
     * 
     * @param playerInventory Snapshot ekwipunku gracza (getStorageContents)
     * @param recipe Przepis do wypełnienia
     * @param useMaxItems Czy wypełnić maksymalną ilością (shift-click) czy pojedynczo
     * @return FillResult z planem lub failure
     */
    public static FillResult planForShapelessRecipe(ItemStack[] playerInventory, ShapelessRecipe recipe, boolean useMaxItems) {
        List<org.bukkit.inventory.RecipeChoice> choices = recipe.getChoiceList();
        
        // Shapeless: składniki układamy od slotu 1
        Map<Integer, org.bukkit.inventory.RecipeChoice> requiredChoices = new LinkedHashMap<>();
        int gridSlot = 1;
        for (org.bukkit.inventory.RecipeChoice choice : choices) {
            requiredChoices.put(gridSlot, choice);
            gridSlot++;
        }
        
        return planFill(playerInventory, requiredChoices, useMaxItems, (item, choice) -> choice.test(item));
    }
    
    /**
     * Planuje wypełnienie siatki na podstawie Recipe (auto-detect shaped/shapeless).
     */
    public static FillResult planForRecipe(ItemStack[] playerInventory, Recipe recipe, boolean useMaxItems) {
        if (recipe instanceof ShapedRecipe shapedRecipe) {
            return planForShapedRecipe(playerInventory, shapedRecipe, useMaxItems);
        } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
            return planForShapelessRecipe(playerInventory, shapelessRecipe, useMaxItems);
        }
        return FillResult.failure();
    }
    
    /**
     * Główna logika wypełniania - wspólna dla shaped i shapeless.
     * 
     * Algorytm (jak /4x4 Fast Crafting):
     * 1. Grupuj sloty siatki które używają tego samego składnika
     * 2. Dla każdej grupy: zlicz WSZYSTKIE pasujące itemy z całego ekwipunku
     * 3. Oblicz maxFills = suma_dostępnych / liczba_slotów_w_grupie
     * 4. Rozdziel pobieranie z wielu slotów gracza
     * 5. Zwróć plan: co wstawić do siatki + co usunąć z ekwipunku
     * 
     * @param playerInventory Snapshot ekwipunku gracza
     * @param requiredChoices Mapa gridSlot -> RecipeChoice
     * @param useMaxItems Czy wypełnić maksymalnie
     * @param matcher Funkcja sprawdzająca czy item pasuje do choice
     * @return FillResult z planem
     */
    public static FillResult planFill(
            ItemStack[] playerInventory,
            Map<Integer, org.bukkit.inventory.RecipeChoice> requiredChoices,
            boolean useMaxItems,
            java.util.function.BiPredicate<ItemStack, org.bukkit.inventory.RecipeChoice> matcher) {
        
        if (requiredChoices.isEmpty()) {
            return FillResult.failure();
        }
        
        // KROK 1: Dla każdego gridSlot znajdź WSZYSTKIE pasujące sloty w ekwipunku
        Map<Integer, List<int[]>> gridSlotSources = new LinkedHashMap<>(); // gridSlot -> lista [playerSlot, amount]
        
        for (Map.Entry<Integer, org.bukkit.inventory.RecipeChoice> entry : requiredChoices.entrySet()) {
            int gridSlot = entry.getKey();
            org.bukkit.inventory.RecipeChoice choice = entry.getValue();
            
            List<int[]> sources = new ArrayList<>();
            for (int i = 0; i < playerInventory.length; i++) {
                ItemStack item = playerInventory[i];
                if (item != null && item.getType() != Material.AIR && matcher.test(item, choice)) {
                    sources.add(new int[]{i, item.getAmount()});
                }
            }
            
            if (sources.isEmpty()) {
                return FillResult.failure(); // Brak składnika
            }
            
            gridSlotSources.put(gridSlot, sources);
        }
        
        // KROK 2: Grupuj gridSloty które używają tego samego typu składnika
        // (ten sam RecipeChoice)
        Map<org.bukkit.inventory.RecipeChoice, List<Integer>> choiceToGridSlots = new LinkedHashMap<>();
        for (Map.Entry<Integer, org.bukkit.inventory.RecipeChoice> entry : requiredChoices.entrySet()) {
            choiceToGridSlots.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }
        
        // KROK 3: Dla każdej grupy oblicz maxFills
        Map<Integer, Integer> availableInSlot = new HashMap<>();
        for (int i = 0; i < playerInventory.length; i++) {
            ItemStack item = playerInventory[i];
            if (item != null && item.getType() != Material.AIR) {
                availableInSlot.put(i, item.getAmount());
            }
        }
        
        int maxFills = useMaxItems ? 64 : 1;
        
        for (Map.Entry<org.bukkit.inventory.RecipeChoice, List<Integer>> entry : choiceToGridSlots.entrySet()) {
            org.bukkit.inventory.RecipeChoice choice = entry.getKey();
            List<Integer> gridSlots = entry.getValue();
            int slotsNeeded = gridSlots.size();
            
            // Zlicz WSZYSTKIE pasujące itemy
            int totalAvailable = 0;
            int itemMaxStackSize = 64;
            for (int i = 0; i < playerInventory.length; i++) {
                ItemStack item = playerInventory[i];
                if (item != null && item.getType() != Material.AIR && matcher.test(item, choice)) {
                    totalAvailable += availableInSlot.getOrDefault(i, 0);
                    itemMaxStackSize = item.getMaxStackSize();
                }
            }
            
            // Ile wypełnień możemy zrobić z tej grupy?
            int possibleFills = totalAvailable / slotsNeeded;
            possibleFills = Math.min(possibleFills, itemMaxStackSize);
            
            maxFills = Math.min(maxFills, possibleFills);
        }
        
        if (maxFills <= 0) {
            return FillResult.failure();
        }
        
        // KROK 4: Rozdziel pobieranie z wielu slotów gracza
        Map<Integer, ItemStack> gridItems = new LinkedHashMap<>();
        Map<Integer, Integer> consumeOps = new LinkedHashMap<>();
        
        // Resetuj dostępność
        availableInSlot.clear();
        for (int i = 0; i < playerInventory.length; i++) {
            ItemStack item = playerInventory[i];
            if (item != null && item.getType() != Material.AIR) {
                availableInSlot.put(i, item.getAmount());
            }
        }
        
        // Dla każdego gridSlot przydziel maxFills itemów
        for (Map.Entry<Integer, List<int[]>> entry : gridSlotSources.entrySet()) {
            int gridSlot = entry.getKey();
            List<int[]> sources = entry.getValue();
            
            int needed = maxFills;
            ItemStack itemClone = null;
            
            for (int[] source : sources) {
                int playerSlot = source[0];
                int available = availableInSlot.getOrDefault(playerSlot, 0);
                
                if (available <= 0) continue;
                
                int toTake = Math.min(needed, available);
                if (toTake > 0) {
                    if (itemClone == null) {
                        itemClone = playerInventory[playerSlot].clone();
                    }
                    
                    availableInSlot.put(playerSlot, available - toTake);
                    consumeOps.merge(playerSlot, toTake, Integer::sum);
                    needed -= toTake;
                    
                    if (needed <= 0) break;
                }
            }
            
            if (itemClone == null || needed > 0) {
                return FillResult.failure();
            }
            
            ItemStack toPlace = itemClone.clone();
            toPlace.setAmount(maxFills);
            gridItems.put(gridSlot, toPlace);
        }
        
        return FillResult.success(gridItems, consumeOps);
    }
    
    /**
     * Wykonuje plan - usuwa itemy z ekwipunku gracza.
     */
    public static void applyConsumeOps(Player player, Map<Integer, Integer> consumeOps) {
        for (Map.Entry<Integer, Integer> entry : consumeOps.entrySet()) {
            int playerSlot = entry.getKey();
            int amountToRemove = entry.getValue();
            
            ItemStack item = player.getInventory().getItem(playerSlot);
            if (item != null && item.getType() != Material.AIR) {
                int newAmount = item.getAmount() - amountToRemove;
                if (newAmount <= 0) {
                    player.getInventory().setItem(playerSlot, null);
                } else {
                    item.setAmount(newAmount);
                }
            }
        }
    }
    
    /**
     * Wykonuje plan - wstawia itemy do siatki craftingu.
     */
    public static void applyGridItems(Inventory craftingInventory, Map<Integer, ItemStack> gridItems) {
        for (Map.Entry<Integer, ItemStack> entry : gridItems.entrySet()) {
            int gridSlot = entry.getKey();
            ItemStack item = entry.getValue();
            craftingInventory.setItem(gridSlot, item);
        }
    }
    
    /**
     * Wykonuje cały plan - usuwa z ekwipunku i wstawia do siatki.
     */
    public static void applyFillResult(Player player, Inventory craftingInventory, FillResult result) {
        if (!result.success()) return;
        
        applyConsumeOps(player, result.consumeOps());
        applyGridItems(craftingInventory, result.gridItems());
    }
}

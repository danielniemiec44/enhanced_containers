package pl.com.sofitfy.enhanced_containers.crafting.vanilla;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

/**
 * Wirtualny crafting inventory - bez bazy danych, czysty w pamięci.
 * 
 * Używany aby sprawdzać vanilla receptury bez otwierania prawdziwego GUI gracza.
 * Implementuje interfejs CraftingInventory tak aby można go przekazać
 * do Bukkit.getCraftingRecipe() i recipe.matches().
 * 
 * Przechowuje:
 * - Macierz 3x3 (sloty 0-8) - items do craftu
 * - Slot wynikowy (slot 9) - gdzie pojawia się rezultat
 */
public class VirtualCraftingInventory implements CraftingInventory {
    
    // Macierz 3x3 + 1 slot na wynik = 10 slotów razem
    private final ItemStack[] items = new ItemStack[10];
    private final List<HumanEntity> viewers = new ArrayList<>();
    
    /**
     * Tworzy pusty wirtualny inventory.
     */
    public VirtualCraftingInventory() {
        // Wszystkie sloty zaczynają się jako null (puste)
        for (int i = 0; i < 10; i++) {
            items[i] = null;
        }
    }
    
    /**
     * Tworzy wirtualny inventory z daną macierzą 3x3.
     * 
     * @param matrix Macierz 3x3 (rozmiar musi być 9)
     *               Layout: [0,1,2]
     *                       [3,4,5]
     *                       [6,7,8]
     */
    public VirtualCraftingInventory(ItemStack[] matrix) {
        this();
        if (matrix != null && matrix.length == 9) {
            System.arraycopy(matrix, 0, items, 0, 9);
        }
    }
    
    // ========== CraftingInventory Interface ==========
    
    /**
     * Zwraca macierz 3x3 (sloty 0-8).
     */
    @Override
    public ItemStack[] getMatrix() {
        ItemStack[] matrix = new ItemStack[9];
        System.arraycopy(items, 0, matrix, 0, 9);
        return matrix;
    }
    
    /**
     * Ustawia macierz 3x3 (sloty 0-8).
     */
    @Override
    public void setMatrix(ItemStack[] matrix) {
        if (matrix != null && matrix.length == 9) {
            System.arraycopy(matrix, 0, items, 0, 9);
        }
    }
    
    /**
     * Zwraca slot wynikowy (slot 9).
     */
    @Override
    public ItemStack getResult() {
        return items[9];
    }
    
    /**
     * Ustawia slot wynikowy (slot 9).
     */
    @Override
    public void setResult(ItemStack result) {
        items[9] = result;
    }
    
    /**
     * Zwraca recepturę która pasuje do macierzy.
     * Domyślnie null - Bukkit API szuka receptur niezależnie.
     */
    @Override
    public Recipe getRecipe() {
        return null;
    }
    
    // ========== Inventory Interface ==========
    
    @Override
    public int getSize() {
        return 10;
    }
    
    @Override
    public int getMaxStackSize() {
        return 64;
    }
    
    @Override
    public void setMaxStackSize(int size) {
        // Ignoruj - wirtualny inventory nie zmienia rozmiar stacku
    }
    
    @Override
    public ItemStack getItem(int index) {
        if (index >= 0 && index < 10) {
            return items[index];
        }
        return null;
    }
    
    @Override
    public void setItem(int index, ItemStack item) {
        if (index >= 0 && index < 10) {
            items[index] = item;
        }
    }
    
    @Override
    public HashMap<Integer, ItemStack> addItem(ItemStack... items) {
        HashMap<Integer, ItemStack> result = new HashMap<>();
        for (ItemStack item : items) {
            int added = 0;
            // Szukaj wolnego slotu
            for (int i = 0; i < 10; i++) {
                if (this.items[i] == null || this.items[i].getType().isAir()) {
                    this.items[i] = item.clone();
                    added = item.getAmount();
                    break;
                }
            }
            if (added < item.getAmount()) {
                ItemStack remainder = item.clone();
                remainder.setAmount(item.getAmount() - added);
                result.put(0, remainder);
            }
        }
        return result;
    }
    
    @Override
    public HashMap<Integer, ItemStack> removeItem(ItemStack... items) {
        HashMap<Integer, ItemStack> result = new HashMap<>();
        for (ItemStack item : items) {
            int toRemove = item.getAmount();
            for (int i = 0; i < 10 && toRemove > 0; i++) {
                if (this.items[i] != null && 
                    !this.items[i].getType().isAir() && 
                    this.items[i].isSimilar(item)) {
                    
                    int amount = Math.min(this.items[i].getAmount(), toRemove);
                    this.items[i].setAmount(this.items[i].getAmount() - amount);
                    toRemove -= amount;
                    
                    if (this.items[i].getAmount() <= 0) {
                        this.items[i] = null;
                    }
                }
            }
            if (toRemove > 0) {
                ItemStack remainder = item.clone();
                remainder.setAmount(toRemove);
                result.put(0, remainder);
            }
        }
        return result;
    }
    
    @Override
    public ItemStack[] getContents() {
        return items.clone();
    }
    
    @Override
    public void setContents(ItemStack[] items) {
        if (items.length >= 10) {
            System.arraycopy(items, 0, this.items, 0, 10);
        }
    }
    
    @Override
    public boolean contains(Material material) {
        for (ItemStack item : items) {
            if (item != null && item.getType() == material) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean contains(ItemStack item) {
        for (ItemStack invItem : items) {
            if (invItem != null && invItem.isSimilar(item)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean contains(Material material, int amount) {
        int found = 0;
        for (ItemStack item : items) {
            if (item != null && item.getType() == material) {
                found += item.getAmount();
            }
        }
        return found >= amount;
    }
    
    @Override
    public boolean contains(ItemStack item, int amount) {
        int found = 0;
        for (ItemStack invItem : items) {
            if (invItem != null && invItem.isSimilar(item)) {
                found += invItem.getAmount();
            }
        }
        return found >= amount;
    }
    
    @Override
    public boolean containsAtLeast(ItemStack item, int amount) {
        return contains(item, amount);
    }
    
    @Override
    public HashMap<Integer, ItemStack> all(Material material) {
        HashMap<Integer, ItemStack> result = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            if (items[i] != null && items[i].getType() == material) {
                result.put(i, items[i]);
            }
        }
        return result;
    }
    
    @Override
    public HashMap<Integer, ItemStack> all(ItemStack item) {
        HashMap<Integer, ItemStack> result = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            if (items[i] != null && items[i].isSimilar(item)) {
                result.put(i, items[i]);
            }
        }
        return result;
    }
    
    @Override
    public int first(Material material) {
        for (int i = 0; i < 10; i++) {
            if (items[i] != null && items[i].getType() == material) {
                return i;
            }
        }
        return -1;
    }
    
    @Override
    public int first(ItemStack item) {
        for (int i = 0; i < 10; i++) {
            if (items[i] != null && items[i].isSimilar(item)) {
                return i;
            }
        }
        return -1;
    }
    
    @Override
    public int firstEmpty() {
        for (int i = 0; i < 10; i++) {
            if (items[i] == null || items[i].getType().isAir()) {
                return i;
            }
        }
        return -1;
    }
    
    @Override
    public boolean isEmpty() {
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void remove(Material material) {
        for (int i = 0; i < 10; i++) {
            if (items[i] != null && items[i].getType() == material) {
                items[i] = null;
            }
        }
    }
    
    @Override
    public void remove(ItemStack item) {
        for (int i = 0; i < 10; i++) {
            if (items[i] != null && items[i].isSimilar(item)) {
                items[i] = null;
            }
        }
    }
    
    @Override
    public void clear(int index) {
        if (index >= 0 && index < 10) {
            items[index] = null;
        }
    }
    
    @Override
    public void clear() {
        for (int i = 0; i < 10; i++) {
            items[i] = null;
        }
    }
    
    @Override
    public List<HumanEntity> getViewers() {
        return new ArrayList<>(viewers);
    }
    
    @Override
    public InventoryHolder getHolder() {
        return null; // Wirtualny inventory nie ma właściciela
    }
    
    @Override
    public InventoryHolder getHolder(boolean create) {
        return null; // Wirtualny inventory nie ma właściciela
    }
    
    @Override
    public InventoryType getType() {
        return InventoryType.CRAFTING; // Type musi być InventoryType
    }
    
    @Override
    public ListIterator<ItemStack> iterator() {
        return new ArrayList<>(List.of(items)).listIterator();
    }
    
    @Override
    public ListIterator<ItemStack> iterator(int index) {
        return new ArrayList<>(List.of(items)).listIterator(index);
    }
    
    @Override
    public Location getLocation() {
        return null; // Wirtualny inventory nie ma lokalizacji
    }
    
    @Override
    public int close() {
        // Nic nie rób - wirtualny inventory nie ma zasobów do zamknięcia
        return 0;
    }
    
    @Override
    public ItemStack[] getStorageContents() {
        return getContents();
    }
    
    @Override
    public void setStorageContents(ItemStack[] items) {
        setContents(items);
    }
    
    @Override
    public HashMap<Integer, ItemStack> removeItemAnySlot(ItemStack... items) {
        return removeItem(items);
    }
}

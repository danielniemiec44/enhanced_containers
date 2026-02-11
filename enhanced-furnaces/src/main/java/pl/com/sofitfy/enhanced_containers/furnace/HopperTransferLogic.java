package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * HopperTransferLogic - logika transferu przedmiotów między lejkiem a Enhanced Furnace.
 * 
 * Ta klasa NIE zależy od NMS i może być łatwo testowana jednostkowo.
 * 
 * VANILLA HOPPER BEHAVIOR:
 * - Lejek przenosi 1 przedmiot na transfer (co 8 ticków w vanilla)
 * - Lejek OD GÓRY pieca → wkłada do INPUT
 * - Lejek Z BOKU pieca → wkłada do FUEL (tylko paliwo!)
 * - Lejek OD DOŁU pieca → pobiera z OUTPUT
 * 
 * ENHANCED FURNACE:
 * - Ma 3 sloty INPUT (11, 20, 29)
 * - Ma 3 sloty FUEL (46, 47, 48)
 * - Ma 3 sloty OUTPUT (14, 23, 32)
 * - Lejek powinien operować na wszystkich 3 slotach danego typu
 */
public class HopperTransferLogic {
    
    /**
     * Kierunek lejka względem pieca
     */
    public enum HopperDirection {
        /** Lejek NAD piecem - wkłada do INPUT */
        ABOVE,
        /** Lejek POD piecem - pobiera z OUTPUT */
        BELOW,
        /** Lejek Z BOKU - wkłada do FUEL */
        SIDE
    }
    
    /**
     * Wynik operacji transferu
     */
    public static class TransferResult {
        public final boolean success;
        public final int slotIndex; // Który slot INPUT/OUTPUT/FUEL był użyty (0-2)
        public final String message;
        
        public TransferResult(boolean success, int slotIndex, String message) {
            this.success = success;
            this.slotIndex = slotIndex;
            this.message = message;
        }
        
        public static TransferResult success(int slot) {
            return new TransferResult(true, slot, "Transfer successful");
        }
        
        public static TransferResult failure(String reason) {
            return new TransferResult(false, -1, reason);
        }
    }
    
    // ==================== PUSH TO FURNACE (Hopper → Furnace) ====================
    
    /**
     * Próbuje przenieść 1 przedmiot z lejka do slotów INPUT pieca.
     * Szuka pierwszego slotu który może przyjąć przedmiot.
     * 
     * @param gui GUI pieca (zawiera sloty)
     * @param hopperInv inventory lejka
     * @return wynik operacji
     */
    public static TransferResult tryPushToInput(EnhancedFurnaceGUI gui, Inventory hopperInv) {
        // Znajdź pierwszy przedmiot w lejku
        ItemStack itemToTransfer = findFirstItem(hopperInv);
        if (itemToTransfer == null) {
            return TransferResult.failure("Hopper is empty");
        }
        
        // Próbuj każdy slot INPUT
        for (int i = 0; i < EnhancedFurnaceGUI.INPUT_SLOTS.length; i++) {
            ItemStack inputItem = gui.getInputItem(i);
            
            if (inputItem == null || inputItem.getType().isAir()) {
                // Pusty slot - wstaw 1 przedmiot
                ItemStack toPlace = itemToTransfer.clone();
                toPlace.setAmount(1);
                gui.setInputItem(i, toPlace);
                
                // Usuń 1 z lejka
                decrementItemInInventory(hopperInv, itemToTransfer);
                return TransferResult.success(i);
                
            } else if (inputItem.isSimilar(itemToTransfer) && inputItem.getAmount() < inputItem.getMaxStackSize()) {
                // Można dodać do stacka
                inputItem.setAmount(inputItem.getAmount() + 1);
                
                decrementItemInInventory(hopperInv, itemToTransfer);
                return TransferResult.success(i);
            }
        }
        
        return TransferResult.failure("All INPUT slots full or incompatible");
    }
    
    /**
     * Próbuje przenieść 1 przedmiot (paliwo) z lejka do slotów FUEL pieca.
     * 
     * @param gui GUI pieca
     * @param hopperInv inventory lejka
     * @return wynik operacji
     */
    public static TransferResult tryPushToFuel(EnhancedFurnaceGUI gui, Inventory hopperInv) {
        // Znajdź pierwsze paliwo w lejku
        ItemStack fuelItem = findFirstFuel(hopperInv);
        if (fuelItem == null) {
            return TransferResult.failure("No fuel in hopper");
        }
        
        // Próbuj każdy slot FUEL
        for (int i = 0; i < EnhancedFurnaceGUI.FUEL_SLOTS.length; i++) {
            ItemStack currentFuel = gui.getFuelItem(i);
            
            if (currentFuel == null || currentFuel.getType().isAir()) {
                ItemStack toPlace = fuelItem.clone();
                toPlace.setAmount(1);
                gui.setFuelItem(i, toPlace);
                
                decrementItemInInventory(hopperInv, fuelItem);
                return TransferResult.success(i);
                
            } else if (currentFuel.isSimilar(fuelItem) && currentFuel.getAmount() < currentFuel.getMaxStackSize()) {
                currentFuel.setAmount(currentFuel.getAmount() + 1);
                
                decrementItemInInventory(hopperInv, fuelItem);
                return TransferResult.success(i);
            }
        }
        
        return TransferResult.failure("All FUEL slots full or incompatible");
    }
    
    // ==================== PULL FROM FURNACE (Furnace → Hopper) ====================
    
    /**
     * Próbuje przenieść 1 przedmiot ze slotów OUTPUT pieca do lejka.
     * 
     * @param gui GUI pieca
     * @param hopperInv inventory lejka
     * @return wynik operacji
     */
    public static TransferResult tryPullFromOutput(EnhancedFurnaceGUI gui, Inventory hopperInv) {
        // Próbuj każdy slot OUTPUT
        for (int i = 0; i < EnhancedFurnaceGUI.OUTPUT_SLOTS.length; i++) {
            ItemStack outputItem = gui.getOutputItem(i);
            
            if (outputItem != null && !outputItem.getType().isAir() && outputItem.getAmount() > 0) {
                // Jest przedmiot do pobrania - sprawdź czy lejek może przyjąć
                ItemStack toTransfer = outputItem.clone();
                toTransfer.setAmount(1);
                
                var leftover = hopperInv.addItem(toTransfer);
                
                if (leftover.isEmpty()) {
                    // Lejek przyjął - usuń z output
                    if (outputItem.getAmount() > 1) {
                        outputItem.setAmount(outputItem.getAmount() - 1);
                    } else {
                        gui.setOutputItem(i, null);
                    }
                    return TransferResult.success(i);
                }
                // Lejek nie może przyjąć - spróbuj następny slot
            }
        }
        
        return TransferResult.failure("All OUTPUT slots empty or hopper full");
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Znajduje pierwszy niepusty przedmiot w inventory.
     */
    public static ItemStack findFirstItem(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir() && item.getAmount() > 0) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * Znajduje pierwsze paliwo w inventory.
     */
    public static ItemStack findFirstFuel(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir() && item.getAmount() > 0) {
                if (EnhancedFurnaceLogic.getFuelBurnTime(item.getType()) > 0) {
                    return item;
                }
            }
        }
        return null;
    }
    
    /**
     * Zmniejsza ilość przedmiotu w inventory o 1.
     */
    public static void decrementItemInInventory(Inventory inventory, ItemStack itemToDecrement) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.isSimilar(itemToDecrement)) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    inventory.setItem(i, null);
                }
                return;
            }
        }
    }
    
    /**
     * Sprawdza czy przedmiot jest paliwem.
     */
    public static boolean isFuel(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        return EnhancedFurnaceLogic.getFuelBurnTime(item.getType()) > 0;
    }
    
    /**
     * Sprawdza czy inventory może przyjąć dany przedmiot.
     */
    public static boolean canAcceptItem(Inventory inventory, ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot == null || slot.getType().isAir()) {
                return true; // Pusty slot
            }
            if (slot.isSimilar(item) && slot.getAmount() < slot.getMaxStackSize()) {
                return true; // Można dodać do stacka
            }
        }
        return false;
    }
}

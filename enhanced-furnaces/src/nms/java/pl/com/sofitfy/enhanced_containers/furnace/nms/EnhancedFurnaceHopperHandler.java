package pl.com.sofitfy.enhanced_containers.furnace.nms;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import pl.com.sofitfy.enhanced_containers.furnace.EnhancedFurnaceData;
import pl.com.sofitfy.enhanced_containers.furnace.EnhancedFurnaceGUI;
import pl.com.sofitfy.enhanced_containers.furnace.EnhancedFurnaceLogic;
import pl.com.sofitfy.enhanced_containers.furnace.EnhancedFurnaceManager;

/**
 * NMS Handler dla operacji lejków z Enhanced Furnace.
 * 
 * Problem:
 * - Vanilla lejki operują na vanilla inventory pieca (3 sloty: input, fuel, output)
 * - Nasz Enhanced Furnace ma customowe inventory (54 sloty z 3 liniami)
 * - Bukkit InventoryMoveItemEvent dostaje vanilla inventory, nie nasze
 * 
 * Rozwiązanie:
 * - Przechwytujemy event gdy destination/source to blok FURNACE w lokalizacji naszego pieca
 * - Anulujemy vanilla transfer
 * - Sami obsługujemy transfer do/z naszego customowego inventory
 * - Używamy NMS do sprawdzenia kierunku lejka i poprawnego mapowania slotów
 * 
 * Mapowanie slotów (vanilla → enhanced):
 * - Lejek OD GÓRY → INPUT (slot 0 → sloty 11, 20, 29 po kolei)
 * - Lejek Z BOKU → FUEL (slot 1 → sloty 46, 47, 48 po kolei)
 * - Lejek OD DOŁU → OUTPUT (slot 2 → sloty 14, 23, 32 po kolei)
 */
public class EnhancedFurnaceHopperHandler implements Listener {
    
    private final JavaPlugin plugin;
    private final EnhancedFurnaceManager manager;
    
    public EnhancedFurnaceHopperHandler(JavaPlugin plugin, EnhancedFurnaceManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }
    
    /**
     * Główny handler dla transferu przedmiotów przez lejki.
     * 
     * Przechwytuje eventy gdzie lejek próbuje operować na vanilla inventory pieca,
     * ale pod tą lokalizacją jest nasz Enhanced Furnace.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        Inventory source = event.getSource();
        Inventory destination = event.getDestination();
        
        // Przypadek 1: Lejek wkłada DO pieca
        Location destLoc = getInventoryBlockLocation(destination);
        if (destLoc != null && manager.hasFurnaceAt(destLoc)) {
            handleHopperPushToFurnace(event, source, destLoc);
            return;
        }
        
        // Przypadek 2: Lejek pobiera Z pieca
        Location sourceLoc = getInventoryBlockLocation(source);
        if (sourceLoc != null && manager.hasFurnaceAt(sourceLoc)) {
            handleHopperPullFromFurnace(event, sourceLoc, destination);
            return;
        }
    }
    
    /**
     * Obsługuje wkładanie przedmiotów DO Enhanced Furnace przez lejek.
     * 
     * TYLKO BLOKUJE vanilla transfer - faktyczny transfer robi TickHandler.
     * 
     * Kierunek lejka określa do których slotów wkładamy:
     * - OD GÓRY (lejek nad piecem) → INPUT slots (11, 20, 29)
     * - Z BOKU (lejek obok pieca) → FUEL slots (46, 47, 48) - tylko paliwo!
     * - OD DOŁU - nie powinno się zdarzyć (lejek pod piecem pobiera, nie wkłada)
     */
    private void handleHopperPushToFurnace(InventoryMoveItemEvent event, Inventory hopperInv, Location furnaceLoc) {
        // Anuluj vanilla transfer - TickHandler zrobi właściwy transfer co 8 ticków
        event.setCancelled(true);
        
        // NIE robimy transferu tutaj - TickHandler się tym zajmie
        // To zapobiega podwójnym transferom i zachowuje vanilla timing (1 item / 8 ticków)
    }
    
    /**
     * Obsługuje pobieranie przedmiotów Z Enhanced Furnace przez lejek.
     * 
     * TYLKO BLOKUJE vanilla transfer - faktyczny transfer robi TickHandler.
     */
    private void handleHopperPullFromFurnace(InventoryMoveItemEvent event, Location furnaceLoc, Inventory hopperInv) {
        // Anuluj vanilla transfer - TickHandler zrobi właściwy transfer co 8 ticków
        event.setCancelled(true);
        
        // NIE robimy transferu tutaj - TickHandler się tym zajmie
    }
    
    // ==================== TRANSFER METHODS ====================
    
    /**
     * Próbuje wrzucić 1 przedmiot do slotów INPUT (round-robin między 3 liniami).
     * Vanilla lejek przenosi 1 item co 8 ticków.
     */
    private boolean tryPushToInputSlots(EnhancedFurnaceData data, Inventory hopperInv, org.bukkit.inventory.ItemStack item) {
        EnhancedFurnaceGUI gui = data.getGui();
        
        // Próbuj każdy slot INPUT w kolejności (slot 11, 20, 29)
        for (int i = 0; i < EnhancedFurnaceGUI.INPUT_SLOTS.length; i++) {
            org.bukkit.inventory.ItemStack current = gui.getInputItem(i);
            
            if (current == null || current.getType().isAir()) {
                // Pusty slot - wstaw 1 przedmiot
                org.bukkit.inventory.ItemStack toPlace = item.clone();
                toPlace.setAmount(1);
                gui.setInputItem(i, toPlace);
                
                // Usuń 1 z lejka
                removeOneItemFromHopper(hopperInv, item);
                return true;
                
            } else if (current.isSimilar(item) && current.getAmount() < current.getMaxStackSize()) {
                // Można dodać do istniejącego stacka
                current.setAmount(current.getAmount() + 1);
                
                // Usuń 1 z lejka
                removeOneItemFromHopper(hopperInv, item);
                return true;
            }
        }
        
        return false; // Wszystkie sloty INPUT pełne
    }
    
    /**
     * Próbuje wrzucić 1 przedmiot (paliwo) do slotów FUEL.
     */
    private boolean tryPushToFuelSlots(EnhancedFurnaceData data, Inventory hopperInv, org.bukkit.inventory.ItemStack item) {
        EnhancedFurnaceGUI gui = data.getGui();
        
        // Próbuj każdy slot FUEL w kolejności (slot 46, 47, 48)
        for (int i = 0; i < EnhancedFurnaceGUI.FUEL_SLOTS.length; i++) {
            org.bukkit.inventory.ItemStack current = gui.getFuelItem(i);
            
            if (current == null || current.getType().isAir()) {
                // Pusty slot
                org.bukkit.inventory.ItemStack toPlace = item.clone();
                toPlace.setAmount(1);
                gui.setFuelItem(i, toPlace);
                
                removeOneItemFromHopper(hopperInv, item);
                return true;
                
            } else if (current.isSimilar(item) && current.getAmount() < current.getMaxStackSize()) {
                // Można dodać do stacka
                current.setAmount(current.getAmount() + 1);
                
                removeOneItemFromHopper(hopperInv, item);
                return true;
            }
        }
        
        return false; // Wszystkie sloty FUEL pełne
    }
    
    /**
     * Próbuje pobrać 1 przedmiot ze slotów OUTPUT do lejka.
     */
    private boolean tryPullFromOutputSlots(EnhancedFurnaceData data, Inventory hopperInv) {
        EnhancedFurnaceGUI gui = data.getGui();
        
        // Próbuj każdy slot OUTPUT w kolejności (slot 14, 23, 32)
        for (int i = 0; i < EnhancedFurnaceGUI.OUTPUT_SLOTS.length; i++) {
            org.bukkit.inventory.ItemStack current = gui.getOutputItem(i);
            
            if (current != null && !current.getType().isAir() && current.getAmount() > 0) {
                // Jest przedmiot do pobrania
                org.bukkit.inventory.ItemStack toTransfer = current.clone();
                toTransfer.setAmount(1);
                
                // Sprawdź czy lejek może przyjąć
                var leftover = hopperInv.addItem(toTransfer);
                
                if (leftover.isEmpty()) {
                    // Udało się - usuń 1 z output
                    if (current.getAmount() > 1) {
                        current.setAmount(current.getAmount() - 1);
                    } else {
                        gui.setOutputItem(i, null);
                    }
                    return true;
                }
            }
        }
        
        return false; // Wszystkie sloty OUTPUT puste lub lejek pełny
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Usuwa 1 przedmiot z inventory lejka.
     */
    private void removeOneItemFromHopper(Inventory hopperInv, org.bukkit.inventory.ItemStack itemToRemove) {
        for (int i = 0; i < hopperInv.getSize(); i++) {
            org.bukkit.inventory.ItemStack item = hopperInv.getItem(i);
            if (item != null && item.isSimilar(itemToRemove)) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    hopperInv.setItem(i, null);
                }
                return;
            }
        }
    }
    
    /**
     * Pobiera lokalizację bloku dla danego inventory.
     * Działa dla Furnace, Hopper, Chest itp.
     */
    private Location getInventoryBlockLocation(Inventory inventory) {
        if (inventory == null) return null;
        
        var holder = inventory.getHolder();
        if (holder instanceof org.bukkit.block.BlockState blockState) {
            return blockState.getLocation();
        }
        
        return null;
    }
    
    /**
     * Określa kierunek lejka względem pieca (używając NMS Direction).
     * 
     * @return Direction.UP jeśli lejek jest NAD piecem,
     *         Direction.DOWN jeśli POD piecem,
     *         Direction z boku w innych przypadkach
     */
    private Direction getHopperDirectionRelativeToFurnace(Location hopperLoc, Location furnaceLoc) {
        int dx = hopperLoc.getBlockX() - furnaceLoc.getBlockX();
        int dy = hopperLoc.getBlockY() - furnaceLoc.getBlockY();
        int dz = hopperLoc.getBlockZ() - furnaceLoc.getBlockZ();
        
        if (dy > 0) return Direction.UP;      // Lejek NAD piecem
        if (dy < 0) return Direction.DOWN;    // Lejek POD piecem
        if (dx > 0) return Direction.EAST;    // Lejek na wschód od pieca
        if (dx < 0) return Direction.WEST;    // Lejek na zachód od pieca
        if (dz > 0) return Direction.SOUTH;   // Lejek na południe od pieca
        if (dz < 0) return Direction.NORTH;   // Lejek na północ od pieca
        
        return Direction.UP; // Fallback (nie powinno się zdarzyć)
    }
    
    /**
     * Sprawdza czy przedmiot jest paliwem (używając NMS FuelValues).
     */
    private boolean isFuel(org.bukkit.inventory.ItemStack item, World world) {
        if (item == null || item.getType().isAir()) return false;
        
        // Użyj EnhancedFurnaceLogic który ma już mapę paliw
        return EnhancedFurnaceLogic.getFuelBurnTime(item.getType()) > 0;
    }
}

package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * EnhancedFurnaceHopperListener - obsługa lejków dla rozszerzonego pieca
 * 
 * Vanilla behavior dla lejków:
 * - Lejek od góry → wkłada do slotów INPUT (po 1 z każdego)
 * - Lejek od dołu → pobiera ze slotów OUTPUT (po 1 z każdego)
 * - Lejek z boku → wkłada do slotów FUEL (po 1 z każdego)
 * 
 * Lejki działają w kolejności: slot 0, slot 1, slot 2
 * Przenoszą 1 przedmiot na tick (jak vanilla)
 */
public class EnhancedFurnaceHopperListener implements Listener {
    
    private final JavaPlugin plugin;
    private final EnhancedFurnaceManager manager;
    
    public EnhancedFurnaceHopperListener(JavaPlugin plugin, EnhancedFurnaceManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }
    
    /**
     * Obsługuje transfer przedmiotów przez lejki
     * 
     * Ten event jest wywoływany gdy lejek próbuje przenieść przedmiot
     * z jednego inventory do drugiego.
     * 
     * WAŻNE: Lejki operują na vanilla inventory bloku (Furnace), a nie na naszym
     * customowym inventory. Musimy sprawdzić lokalizację bloku, nie inventory!
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        Inventory source = event.getSource();
        Inventory destination = event.getDestination();
        
        // Sprawdź destination - czy to blok pieca w lokalizacji naszego Enhanced Furnace?
        Location destLoc = getInventoryLocation(destination);
        if (destLoc != null && manager.hasFurnaceAt(destLoc)) {
            handleHopperToFurnace(event, source, destLoc);
            return;
        }
        
        // Sprawdź source - czy pobieramy z vanilla pieca który jest naszym Enhanced Furnace?
        Location sourceLoc = getInventoryLocation(source);
        if (sourceLoc != null && manager.hasFurnaceAt(sourceLoc)) {
            handleFurnaceToHopper(event, sourceLoc, destination);
            return;
        }
    }
    
    /**
     * Pobiera lokalizację inventory (bloku).
     * Dla InventoryHolder który jest blokiem (Furnace, Chest, Hopper) zwraca lokalizację.
     */
    private Location getInventoryLocation(Inventory inventory) {
        if (inventory == null) return null;
        
        var holder = inventory.getHolder();
        if (holder instanceof org.bukkit.block.BlockState blockState) {
            return blockState.getLocation();
        }
        
        return null;
    }
    
    /**
     * Obsługuje wkładanie przedmiotów do pieca przez lejek lub hopper minecart.
     * 
     * @param event event transferu
     * @param hopperInv inventory lejka/wagonika (source)
     * @param furnaceLoc lokalizacja Enhanced Furnace
     */
    private void handleHopperToFurnace(InventoryMoveItemEvent event, Inventory hopperInv, Location furnaceLoc) {
        // Anuluj domyślne zachowanie - sami obsłużymy transfer do naszego customowego inventory
        event.setCancelled(true);
        
        EnhancedFurnaceData data = manager.getFurnaceData(furnaceLoc);
        if (data == null) {
            return;
        }
        
        // Pobierz pozycję lejka/wagonika względem pieca
        Block furnaceBlock = furnaceLoc.getBlock();
        BlockFace direction;
        
        var holder = hopperInv.getHolder();
        
        if (holder instanceof Hopper hopper) {
            // Zwykły lejek (blok)
            Block hopperBlock = hopper.getBlock();
            direction = getHopperDirection(hopperBlock, furnaceBlock);
        } else if (holder instanceof HopperMinecart hopperMinecart) {
            // Hopper Minecart (encja) - określ kierunek na podstawie pozycji
            direction = getMinecartHopperDirection(hopperMinecart.getLocation(), furnaceLoc);
        } else {
            return;
        }
        
        // Pobierz przedmiot z lejka
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        
        boolean transferred = false;
        
        if (direction == BlockFace.UP) {
            // Lejek od góry - wkładaj do INPUT slotów
            transferred = tryTransferToInputSlots(data, hopperInv, item);
        } else if (direction == BlockFace.DOWN) {
            // Lejek od dołu nie może wkładać - tylko pobierać
            // Ten przypadek nie powinien wystąpić, ale na wszelki wypadek
            return;
        } else {
            // Lejek z boku - wkładaj do FUEL slotów
            // Tylko jeśli przedmiot jest paliwem
            if (EnhancedFurnaceLogic.getFuelBurnTime(item.getType()) > 0) {
                transferred = tryTransferToFuelSlots(data, hopperInv, item);
            }
        }
        
        // Jeśli nie udało się przenieść - event pozostaje anulowany
        // Lejek spróbuje ponownie w następnym ticku
    }
    
    /**
     * Obsługuje pobieranie przedmiotów z pieca przez lejek lub hopper minecart.
     * 
     * Vanilla behavior dla hopper minecart:
     * - Hopper minecart przejeżdżający POD piecem pobiera z OUTPUT (jak lejek od dołu)
     * - Hopper minecart pobiera 1 przedmiot na tick (8 ticków cooldown)
     * 
     * @param event event transferu
     * @param furnaceLoc lokalizacja Enhanced Furnace
     * @param hopperInv inventory lejka/wagonika (destination)
     */
    private void handleFurnaceToHopper(InventoryMoveItemEvent event, Location furnaceLoc, Inventory hopperInv) {
        // Anuluj domyślne zachowanie - sami obsłużymy transfer z naszego customowego inventory
        event.setCancelled(true);
        
        EnhancedFurnaceData data = manager.getFurnaceData(furnaceLoc);
        if (data == null) {
            return;
        }
        
        // Pobierz pozycję lejka/wagonika względem pieca
        Block furnaceBlock = furnaceLoc.getBlock();
        BlockFace direction;
        
        var holder = hopperInv.getHolder();
        
        if (holder instanceof Hopper hopper) {
            // Zwykły lejek (blok)
            Block hopperBlock = hopper.getBlock();
            direction = getHopperDirection(hopperBlock, furnaceBlock);
        } else if (holder instanceof HopperMinecart hopperMinecart) {
            // Hopper Minecart (encja) - zawsze pobiera z OUTPUT jak lejek od dołu
            // Wagonik przejeżdżający pod piecem (na torach) ma direction DOWN
            direction = getMinecartHopperDirection(hopperMinecart.getLocation(), furnaceLoc);
        } else {
            return;
        }
        
        if (direction == BlockFace.DOWN) {
            // Lejek/wagonik od dołu - pobieraj z OUTPUT slotów
            tryTransferFromOutputSlots(data, hopperInv);
        }
        // Lejki z innych kierunków nie pobierają z pieca
    }
    
    /**
     * Próbuje przenieść przedmiot do slotów INPUT
     * Przenosi po 1 przedmiocie do pierwszego dostępnego slotu
     */
    private boolean tryTransferToInputSlots(EnhancedFurnaceData data, Inventory hopperInv, ItemStack item) {
        EnhancedFurnaceGUI gui = data.getGui();
        
        for (int i = 0; i < EnhancedFurnaceGUI.INPUT_SLOTS.length; i++) {
            ItemStack current = gui.getInputItem(i);
            
            if (current == null || current.getType() == Material.AIR) {
                // Pusty slot - wstaw 1 przedmiot
                ItemStack toPlace = item.clone();
                toPlace.setAmount(1);
                gui.setInputItem(i, toPlace);
                
                // Usuń 1 z lejka
                removeOneFromHopper(hopperInv, item);
                return true;
                
            } else if (current.isSimilar(item) && current.getAmount() < current.getMaxStackSize()) {
                // Można dodać do stacka
                current.setAmount(current.getAmount() + 1);
                
                // Usuń 1 z lejka
                removeOneFromHopper(hopperInv, item);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Próbuje przenieść przedmiot do slotów FUEL
     */
    private boolean tryTransferToFuelSlots(EnhancedFurnaceData data, Inventory hopperInv, ItemStack item) {
        EnhancedFurnaceGUI gui = data.getGui();
        
        for (int i = 0; i < EnhancedFurnaceGUI.FUEL_SLOTS.length; i++) {
            ItemStack current = gui.getFuelItem(i);
            
            if (current == null || current.getType() == Material.AIR) {
                // Pusty slot - wstaw 1 przedmiot
                ItemStack toPlace = item.clone();
                toPlace.setAmount(1);
                gui.setFuelItem(i, toPlace);
                
                // Usuń 1 z lejka
                removeOneFromHopper(hopperInv, item);
                return true;
                
            } else if (current.isSimilar(item) && current.getAmount() < current.getMaxStackSize()) {
                // Można dodać do stacka
                current.setAmount(current.getAmount() + 1);
                
                // Usuń 1 z lejka
                removeOneFromHopper(hopperInv, item);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Próbuje przenieść przedmiot ze slotów OUTPUT do lejka
     */
    private boolean tryTransferFromOutputSlots(EnhancedFurnaceData data, Inventory hopperInv) {
        EnhancedFurnaceGUI gui = data.getGui();
        
        for (int i = 0; i < EnhancedFurnaceGUI.OUTPUT_SLOTS.length; i++) {
            ItemStack current = gui.getOutputItem(i);
            
            if (current != null && current.getType() != Material.AIR) {
                // Jest przedmiot do przeniesienia
                ItemStack toTransfer = current.clone();
                toTransfer.setAmount(1);
                
                // Spróbuj dodać do lejka
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
        
        return false;
    }
    
    /**
     * Usuwa 1 przedmiot z lejka
     */
    private void removeOneFromHopper(Inventory hopperInv, ItemStack itemToRemove) {
        for (int i = 0; i < hopperInv.getSize(); i++) {
            ItemStack item = hopperInv.getItem(i);
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
     * Określa kierunek lejka względem pieca
     * 
     * @return BlockFace.UP jeśli lejek jest nad piecem,
     *         BlockFace.DOWN jeśli pod piecem,
     *         inny BlockFace jeśli z boku
     */
    private BlockFace getHopperDirection(Block hopperBlock, Block furnaceBlock) {
        int dx = hopperBlock.getX() - furnaceBlock.getX();
        int dy = hopperBlock.getY() - furnaceBlock.getY();
        int dz = hopperBlock.getZ() - furnaceBlock.getZ();
        
        if (dy > 0) {
            return BlockFace.UP; // Lejek nad piecem
        } else if (dy < 0) {
            return BlockFace.DOWN; // Lejek pod piecem
        } else if (dx > 0) {
            return BlockFace.EAST;
        } else if (dx < 0) {
            return BlockFace.WEST;
        } else if (dz > 0) {
            return BlockFace.SOUTH;
        } else if (dz < 0) {
            return BlockFace.NORTH;
        }
        
        return BlockFace.SELF; // Nie powinno się zdarzyć
    }
    
    /**
     * Określa kierunek hopper minecart względem pieca.
     * 
     * Hopper minecart jeździ na torach i może być:
     * - POD piecem (tory na y-1) → kierunek DOWN → pobiera z OUTPUT
     * - OBOK pieca (tory na tym samym y) → kierunek boczny → wkłada FUEL
     * - Teoretycznie nad piecem (tory na y+1) → kierunek UP → wkłada INPUT
     * 
     * W praktyce najczęściej używa się hopper minecart POD piecem do zbierania outputu.
     * 
     * @param minecartLoc lokalizacja wagonika
     * @param furnaceLoc lokalizacja pieca
     * @return BlockFace reprezentujący kierunek wagonika względem pieca
     */
    private BlockFace getMinecartHopperDirection(Location minecartLoc, Location furnaceLoc) {
        // Używamy floor() bo pozycja encji może mieć ułamki
        int minecartY = (int) Math.floor(minecartLoc.getY());
        int furnaceY = furnaceLoc.getBlockY();
        
        int dy = minecartY - furnaceY;
        
        if (dy >= 1) {
            return BlockFace.UP;    // Wagonik nad piecem
        } else if (dy <= -1) {
            return BlockFace.DOWN;  // Wagonik pod piecem (najczęstszy przypadek)
        } else {
            // Na tym samym poziomie - sprawdź kierunek boczny
            int dx = minecartLoc.getBlockX() - furnaceLoc.getBlockX();
            int dz = minecartLoc.getBlockZ() - furnaceLoc.getBlockZ();
            
            if (Math.abs(dx) > Math.abs(dz)) {
                return dx > 0 ? BlockFace.EAST : BlockFace.WEST;
            } else {
                return dz > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
            }
        }
    }
}

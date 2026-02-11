package pl.com.sofitfy.enhanced_containers.furnace.nms;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import pl.com.sofitfy.enhanced_containers.furnace.EnhancedFurnaceData;
import pl.com.sofitfy.enhanced_containers.furnace.EnhancedFurnaceGUI;
import pl.com.sofitfy.enhanced_containers.furnace.EnhancedFurnaceLogic;
import pl.com.sofitfy.enhanced_containers.furnace.EnhancedFurnaceManager;
import pl.com.sofitfy.enhanced_containers.furnace.HopperTransferLogic;

/**
 * NMS Hopper Tick Handler dla Enhanced Furnace.
 * 
 * PROBLEM:
 * Vanilla lejki operują na vanilla inventory pieca (3 sloty).
 * Nasze Enhanced Furnace ma customowe inventory (54 sloty z 3 liniami INPUT/OUTPUT/FUEL).
 * Vanilla lejek "nie widzi" naszych slotów jako dostępnych.
 * 
 * ROZWIĄZANIE:
 * Scheduler który co 8 ticków (jak vanilla hopper) sprawdza lejki wokół
 * każdego Enhanced Furnace i sam wykonuje transfer przedmiotów.
 * 
 * To działa jak vanilla:
 * - Lejek OD GÓRY → wkłada do INPUT (sloty 11, 20, 29)
 * - Lejek Z BOKU → wkłada PALIWO do FUEL (sloty 46, 47, 48)
 * - Lejek OD DOŁU → pobiera z OUTPUT (sloty 14, 23, 32)
 * 
 * Transfer: 1 przedmiot na 8 ticków (jak vanilla hopper)
 */
public class EnhancedFurnaceHopperTickHandler extends BukkitRunnable {
    
    private static final int HOPPER_COOLDOWN = 8; // Vanilla: 8 ticków między transferami
    
    private final JavaPlugin plugin;
    private final EnhancedFurnaceManager manager;
    
    public EnhancedFurnaceHopperTickHandler(JavaPlugin plugin, EnhancedFurnaceManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }
    
    /**
     * Uruchamia handler - wywoływany co 8 ticków.
     */
    public void start() {
        this.runTaskTimer(plugin, HOPPER_COOLDOWN, HOPPER_COOLDOWN);
        plugin.getLogger().info("[HopperTickHandler] Started! Running every " + HOPPER_COOLDOWN + " ticks.");
    }
    
    @Override
    public void run() {
        // Dla każdego Enhanced Furnace sprawdź lejki wokół
        for (Location furnaceLoc : manager.getAllFurnaceLocations()) {
            if (furnaceLoc.getWorld() == null) continue;
            
            EnhancedFurnaceData data = manager.getFurnaceData(furnaceLoc);
            if (data == null) continue;
            
            // Sprawdź lejki (bloki) w 6 kierunkach wokół pieca
            checkAndTransferFromHopper(furnaceLoc, data, Direction.UP);    // Lejek nad piecem
            checkAndTransferFromHopper(furnaceLoc, data, Direction.NORTH); // Lejek z boku
            checkAndTransferFromHopper(furnaceLoc, data, Direction.SOUTH);
            checkAndTransferFromHopper(furnaceLoc, data, Direction.EAST);
            checkAndTransferFromHopper(furnaceLoc, data, Direction.WEST);
            
            // Lejek (blok) pod piecem - pobiera z OUTPUT
            checkAndTransferToHopper(furnaceLoc, data);
            
            // ========== HOPPER MINECARTS ==========
            // Sprawdź hopper minecarts w pobliżu pieca
            checkAndTransferToHopperMinecarts(furnaceLoc, data);
        }
    }
    
    /**
     * Sprawdza hopper minecarts w pobliżu pieca i wykonuje transfer Z pieca do wagonika.
     * 
     * Hopper minecart zbiera przedmioty z kontenerów POWYŻEJ siebie,
     * więc wagonik pod piecem (na torach) pobiera z OUTPUT.
     * 
     * Vanilla timing: 1 przedmiot co 8 ticków (HOPPER_COOLDOWN).
     */
    private void checkAndTransferToHopperMinecarts(Location furnaceLoc, EnhancedFurnaceData data) {
        World world = furnaceLoc.getWorld();
        if (world == null) return;
        
        // Szukaj hopper minecartów pod piecem (y-1) i obok (y+/-0)
        // Wagonik ma wysokość ~0.7 bloku, więc szukamy w promieniu 1.5 bloku
        double searchRadius = 1.5;
        
        for (Entity entity : world.getNearbyEntities(furnaceLoc, searchRadius, searchRadius, searchRadius)) {
            if (!(entity instanceof HopperMinecart hopperMinecart)) continue;
            
            // Sprawdź czy wagonik jest PONIŻEJ pieca (może pobierać z OUTPUT)
            Location minecartLoc = hopperMinecart.getLocation();
            int dy = (int) Math.floor(minecartLoc.getY()) - furnaceLoc.getBlockY();
            
            // Wagonik pod piecem (dy <= -1) lub na tym samym poziomie ale niżej (dy == 0 i Y ułamkowe < 0.5)
            if (dy <= -1 || (dy == 0 && minecartLoc.getY() < furnaceLoc.getBlockY() + 0.5)) {
                // Hopper minecart pod piecem - pobiera z OUTPUT
                Inventory minecartInv = hopperMinecart.getInventory();
                
                var result = HopperTransferLogic.tryPullFromOutput(data.getGui(), minecartInv);
                if (result.success) {
                    plugin.getLogger().fine("[HopperTickHandler] Hopper minecart pulled 1 item from OUTPUT");
                }
            }
        }
    }
    
    /**
     * Sprawdza lejek w danym kierunku od pieca i wykonuje transfer DO pieca.
     * 
     * VANILLA HOPPER BEHAVIOR:
     * - Lejek ZAWSZE WRZUCA w dół (do kontenera PONIŻEJ siebie)
     * - Lejek POBIERA z kierunku określonego przez FACING (lub z kontenera POWYŻEJ)
     * 
     * Więc dla transferu DO pieca:
     * - Lejek NAD piecem → wrzuca do pieca (bo piec jest poniżej lejka)
     * - Lejek Z BOKU NIE wrzuca do pieca - chyba że FACING=kierunek do pieca,
     *   ale wtedy lejek POBIERA z pieca, nie wrzuca!
     * 
     * WAIT - to oznacza że lejek z boku NIGDY nie wrzuci do pieca?
     * Vanilla: Lejek z boku wrzuca do FURNACE jeśli jego FACING wskazuje na furnace!
     * 
     * Czyli:
     * - Lejek wrzuca przedmioty do kontenera na który wskazuje jego FACING
     * - Ale vanilla hopper + furnace: z góry → INPUT, z boku → FUEL, z dołu → pobiera OUTPUT
     * 
     * @param furnaceLoc lokalizacja pieca
     * @param data dane pieca
     * @param direction kierunek (gdzie jest lejek względem pieca)
     */
    private void checkAndTransferFromHopper(Location furnaceLoc, EnhancedFurnaceData data, Direction direction) {
        World world = furnaceLoc.getWorld();
        if (world == null) return;
        
        // Oblicz pozycję gdzie powinien być lejek
        Location hopperLoc = getLocationInDirection(furnaceLoc, direction);
        Block hopperBlock = hopperLoc.getBlock();
        
        // Sprawdź czy to lejek
        if (hopperBlock.getType() != Material.HOPPER) return;
        
        // Sprawdź czy lejek nie jest zablokowany (powered)
        if (isHopperPowered(hopperLoc)) return;
        
        // Pobierz inventory lejka
        if (!(hopperBlock.getState() instanceof Hopper hopper)) return;
        Inventory hopperInv = hopper.getInventory();
        
        // Wykonaj transfer w zależności od kierunku
        if (direction == Direction.UP) {
            // Lejek NAD piecem → wrzuca do kontenera PONIŻEJ (piec)
            // Lejek ZAWSZE wrzuca w dół, więc piec zawsze dostanie przedmiot
            tryTransferToInput(data, hopperInv);
        } else {
            // Lejek Z BOKU → sprawdź czy FACING wskazuje na piec
            if (!isHopperFacingFurnace(hopperLoc, furnaceLoc)) return;
            
            // Lejek z boku FACING na piec → wkłada PALIWO do FUEL
            tryTransferToFuel(data, hopperInv, world);
        }
    }
    
    /**
     * Sprawdza lejek POD piecem i wykonuje transfer Z pieca do lejka.
     */
    private void checkAndTransferToHopper(Location furnaceLoc, EnhancedFurnaceData data) {
        World world = furnaceLoc.getWorld();
        if (world == null) return;
        
        // Lejek pod piecem
        Location hopperLoc = furnaceLoc.clone().add(0, -1, 0);
        Block hopperBlock = hopperLoc.getBlock();
        
        if (hopperBlock.getType() != Material.HOPPER) return;
        
        // Sprawdź czy lejek nie jest zablokowany
        if (isHopperPowered(hopperLoc)) return;
        
        // Pobierz inventory lejka
        if (!(hopperBlock.getState() instanceof Hopper hopper)) return;
        Inventory hopperInv = hopper.getInventory();
        
        // Pobierz z OUTPUT do lejka
        var result = HopperTransferLogic.tryPullFromOutput(data.getGui(), hopperInv);
        if (result.success) {
            plugin.getLogger().fine("[HopperTickHandler] Pulled 1 item from OUTPUT to hopper at " + hopperLoc);
        }
    }
    
    // ==================== TRANSFER METHODS (używają HopperTransferLogic) ====================
    
    /**
     * Przenosi 1 przedmiot z lejka do slotów INPUT pieca.
     */
    private boolean tryTransferToInput(EnhancedFurnaceData data, Inventory hopperInv) {
        var result = HopperTransferLogic.tryPushToInput(data.getGui(), hopperInv);
        if (result.success) {
            plugin.getLogger().fine("[HopperTickHandler] Pushed 1 item to INPUT slot " + result.slotIndex);
        }
        return result.success;
    }
    
    /**
     * Przenosi 1 przedmiot (paliwo) z lejka do slotów FUEL pieca.
     */
    private boolean tryTransferToFuel(EnhancedFurnaceData data, Inventory hopperInv, World world) {
        var result = HopperTransferLogic.tryPushToFuel(data.getGui(), hopperInv);
        if (result.success) {
            plugin.getLogger().fine("[HopperTickHandler] Pushed 1 fuel item to FUEL slot " + result.slotIndex);
        }
        return result.success;
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Oblicza lokalizację w danym kierunku od źródła.
     */
    private Location getLocationInDirection(Location source, Direction direction) {
        return switch (direction) {
            case UP -> source.clone().add(0, 1, 0);
            case DOWN -> source.clone().add(0, -1, 0);
            case NORTH -> source.clone().add(0, 0, -1);
            case SOUTH -> source.clone().add(0, 0, 1);
            case EAST -> source.clone().add(1, 0, 0);
            case WEST -> source.clone().add(-1, 0, 0);
        };
    }
    
    /**
     * Sprawdza czy lejek jest skierowany w stronę pieca (używając NMS).
     * 
     * Lejek może być skierowany: DOWN, NORTH, SOUTH, EAST, WEST
     * Musimy sprawdzić czy kierunek lejka wskazuje na piec.
     */
    private boolean isHopperFacingFurnace(Location hopperLoc, Location furnaceLoc) {
        World world = hopperLoc.getWorld();
        if (!(world instanceof CraftWorld craftWorld)) return false;
        
        ServerLevel level = craftWorld.getHandle();
        BlockPos hopperPos = new BlockPos(hopperLoc.getBlockX(), hopperLoc.getBlockY(), hopperLoc.getBlockZ());
        
        BlockState blockState = level.getBlockState(hopperPos);
        if (!blockState.is(Blocks.HOPPER)) return false;
        
        // Pobierz kierunek lejka z BlockState
        Direction hopperFacing = blockState.getValue(HopperBlock.FACING);
        
        // Oblicz gdzie lejek "wrzuca" przedmioty
        BlockPos targetPos = hopperPos.relative(hopperFacing);
        
        // Sprawdź czy target to nasz piec
        BlockPos furnacePos = new BlockPos(furnaceLoc.getBlockX(), furnaceLoc.getBlockY(), furnaceLoc.getBlockZ());
        
        return targetPos.equals(furnacePos);
    }
    
    /**
     * Sprawdza czy lejek jest zasilany (powered) - zablokowany.
     */
    private boolean isHopperPowered(Location hopperLoc) {
        World world = hopperLoc.getWorld();
        if (!(world instanceof CraftWorld craftWorld)) return false;
        
        ServerLevel level = craftWorld.getHandle();
        BlockPos hopperPos = new BlockPos(hopperLoc.getBlockX(), hopperLoc.getBlockY(), hopperLoc.getBlockZ());
        
        BlockState blockState = level.getBlockState(hopperPos);
        if (!blockState.is(Blocks.HOPPER)) return false;
        
        // enabled = false oznacza że lejek jest zablokowany (powered)
        return !blockState.getValue(HopperBlock.ENABLED);
    }
}

package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * EnhancedFurnaceListener - obsługuje eventy związane z rozszerzonym piecem
 * 
 * Obsługuje:
 * - Kliknięcie prawym na piec (otwieranie GUI)
 * - Kliknięcia w inventory (blokowanie nieprawidłowych slotów)
 * - Postawienie bloku z flagą (automatyczne oznaczenie)
 * - Zniszczenie bloku (dropowanie przedmiotów)
 * - Eksplozje (dropowanie przedmiotów)
 */
public class EnhancedFurnaceListener implements Listener {
    
    private final JavaPlugin plugin;
    private final EnhancedFurnaceManager manager;
    private final NamespacedKey enhancedFurnaceKey;
    
    public EnhancedFurnaceListener(JavaPlugin plugin, EnhancedFurnaceManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.enhancedFurnaceKey = new NamespacedKey(plugin, "enhanced_furnace");
    }
    
    /**
     * Obsługuje postawienie bloku pieca z flagą enhanced
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Material type = block.getType();
        
        // Sprawdź czy to piec
        if (type != Material.FURNACE && type != Material.BLAST_FURNACE && type != Material.SMOKER) {
            return;
        }
        
        // Sprawdź czy przedmiot miał flagę enhanced
        ItemStack item = event.getItemInHand();
        if (isEnhancedFurnaceItem(item)) {
            // Oznacz blok jako enhanced furnace
            manager.markAsEnhancedFurnace(block);
            plugin.getLogger().info("[EnhancedFurnace] Player " + event.getPlayer().getName() + 
                " placed enhanced furnace at " + formatLocation(block.getLocation()));
        }
    }
    
    /**
     * Sprawdza czy przedmiot jest enhanced furnace (ma flagę w PDC)
     */
    private boolean isEnhancedFurnaceItem(ItemStack item) {
        if (item == null) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        return meta.getPersistentDataContainer().has(enhancedFurnaceKey, PersistentDataType.BYTE);
    }
    
    private String formatLocation(Location loc) {
        return String.format("%s (%d, %d, %d)", 
            loc.getWorld() != null ? loc.getWorld().getName() : "null",
            loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
    
    /**
     * Obsługuje kliknięcie prawym przyciskiem na piec
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        
        // Sprawdź czy to rozszerzony piec
        if (!manager.isEnhancedFurnace(block)) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Jeśli gracz kuca (shift) - nie otwieraj GUI
        // Pozwala to na stawianie bloków, używanie przedmiotów itp.
        if (player.isSneaking()) {
            return;
        }
        
        // Anuluj domyślne otwieranie vanilla pieca
        event.setCancelled(true);
        
        // Otwórz nasze custom GUI
        manager.openFurnaceGUI(player, block);
    }
    
    /**
     * Obsługuje kliknięcia w inventory pieca
     * 
     * WAŻNE: Slot output jest TYLKO do odczytu - gracz może tylko wyjmować.
     * Wkładać może TYLKO logika przepalania (EnhancedFurnaceLogic).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        
        // Sprawdź czy to nasze GUI
        if (!manager.hasInventory(topInventory)) {
            return;
        }
        
        // ========== WALIDACJA: Czy piec nadal istnieje? ==========
        // Zapobiega duplikacji gdy piec został usunięty przez WorldEdit, komendy, itp.
        Location furnaceLoc = manager.getLocationForInventory(topInventory);
        if (furnaceLoc != null && !isValidEnhancedFurnaceBlock(furnaceLoc)) {
            // Piec został usunięty! Zamknij inventory i usuń dane
            event.setCancelled(true);
            event.getWhoClicked().closeInventory();
            manager.removeFurnaceByLocation(furnaceLoc);
            return;
        }
        
        int slot = event.getRawSlot();
        org.bukkit.event.inventory.InventoryAction action = event.getAction();
        
        // ========== BLOKADA DOUBLE-CLICK COLLECT ==========
        // Double-click zbiera przedmioty z całego inventory - może zebrać z output!
        // Blokujemy to całkowicie dla bezpieczeństwa
        if (action == org.bukkit.event.inventory.InventoryAction.COLLECT_TO_CURSOR) {
            // Sprawdź czy przedmiot na kursorze mógłby zebrać z output slotów
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                for (int outputSlot : EnhancedFurnaceGUI.OUTPUT_SLOTS) {
                    ItemStack outputItem = topInventory.getItem(outputSlot);
                    if (outputItem != null && outputItem.isSimilar(cursor)) {
                        // Zablokuj - double-click mógłby zebrać z output
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
        
        // ========== KLIKNIĘCIA Z DOLNEGO INVENTORY GRACZA ==========
        if (slot >= EnhancedFurnaceGUI.INVENTORY_SIZE) {
            if (event.isShiftClick()) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    event.setCancelled(true);
                    
                    // Pobierz typ pieca
                    Material furnaceType = Material.FURNACE;
                    Location furnaceLocation = manager.getLocationForInventory(topInventory);
                    if (furnaceLocation != null) {
                        EnhancedFurnaceData data = manager.getFurnaceData(furnaceLocation);
                        if (data != null) {
                            furnaceType = data.getFurnaceType();
                        }
                    }
                    
                    // VANILLA BEHAVIOR: Priorytet 1 - przedmioty z przepisem idą do INPUT
                    boolean canSmelt = EnhancedFurnaceLogic.canBeSmeltedInFurnace(
                            clickedItem.getType(), 
                            event.getWhoClicked().getWorld(), 
                            furnaceType);
                    
                    if (canSmelt) {
                        // Przedmiot można przepalić - kieruj do INPUT
                        if (tryAddToSlots(topInventory, clickedItem, EnhancedFurnaceGUI.INPUT_SLOTS)) {
                            event.setCurrentItem(clickedItem.getAmount() <= 0 ? null : clickedItem);
                        }
                        return;
                    }
                    
                    // VANILLA BEHAVIOR: Priorytet 2 - paliwa bez przepisu idą do FUEL
                    if (EnhancedFurnaceLogic.getFuelBurnTime(clickedItem.getType()) > 0) {
                        if (tryAddToSlots(topInventory, clickedItem, EnhancedFurnaceGUI.FUEL_SLOTS)) {
                            event.setCurrentItem(clickedItem.getAmount() <= 0 ? null : clickedItem);
                        }
                        return;
                    }
                    
                    // Przedmiot nie jest ani do przepalenia ani paliwem - nic nie rób
                }
            }
            return;
        }
        
        // ========== BLOKADA NIEINTERAKTYWNYCH SLOTÓW ==========
        if (!EnhancedFurnaceGUI.isInteractiveSlot(slot)) {
            event.setCancelled(true);
            return;
        }
        
        // ========== OBSŁUGA SLOTÓW OUTPUT (TYLKO WYJMOWANIE!) ==========
        if (EnhancedFurnaceGUI.isOutputSlot(slot)) {
            // Dozwolone akcje na output - TYLKO wyjmowanie
            boolean isAllowedOutputAction = switch (action) {
                // Wyjmowanie przedmiotu z output
                case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME -> true;
                // Shift-click wyjmuje do inventory gracza
                case MOVE_TO_OTHER_INVENTORY -> true;
                // Drop wyrzuca na ziemię - też OK
                case DROP_ALL_SLOT, DROP_ONE_SLOT -> true;
                // Clone w creative - OK bo nie zmienia inventory
                case CLONE_STACK -> true;
                // Wszystko inne (wkładanie, swap) - BLOKUJ
                default -> false;
            };
            
            if (!isAllowedOutputAction) {
                event.setCancelled(true);
            }
            return;
        }
        
        // ========== OBSŁUGA SHIFT-CLICK Z GUI ==========
        // Shift+Click z INPUT/FUEL/OUTPUT przenosi do inventory gracza (domyślne zachowanie Bukkit)
        // Nie potrzebujemy specjalnej obsługi - Bukkit zrobi to poprawnie
        
        // ========== OBSŁUGA HOTBAR SWAP (klawisze 1-9) ==========
        if (action == org.bukkit.event.inventory.InventoryAction.HOTBAR_SWAP ||
            action == org.bukkit.event.inventory.InventoryAction.HOTBAR_MOVE_AND_READD) {
            
            // Dla fuel - sprawdź czy przedmiot z hotbar to paliwo
            if (EnhancedFurnaceGUI.isFuelSlot(slot)) {
                int hotbarSlot = event.getHotbarButton();
                if (hotbarSlot >= 0) {
                    ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(hotbarSlot);
                    if (hotbarItem != null && !hotbarItem.getType().isAir()) {
                        if (EnhancedFurnaceLogic.getFuelBurnTime(hotbarItem.getType()) <= 0) {
                            // Nie jest paliwem - blokuj
                            event.setCancelled(true);
                        }
                    }
                }
            }
            // Dla input - pozwól na wszystko
            return;
        }
        
        // ========== OBSŁUGA SLOTÓW FUEL (tylko paliwo) ==========
        if (EnhancedFurnaceGUI.isFuelSlot(slot)) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                // Sprawdź czy to paliwo
                if (EnhancedFurnaceLogic.getFuelBurnTime(cursor.getType()) <= 0) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        
        // ========== OBSŁUGA SLOTÓW INPUT - resetuj cookTime gdy zabrano przedmiot ==========
        if (EnhancedFurnaceGUI.isInputSlot(slot)) {
            // Sprawdź czy to akcja wyciągania/zamiany przedmiotu
            boolean isRemovalAction = switch (action) {
                case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME,
                     MOVE_TO_OTHER_INVENTORY, DROP_ALL_SLOT, DROP_ONE_SLOT,
                     HOTBAR_SWAP, HOTBAR_MOVE_AND_READD, SWAP_WITH_CURSOR -> true;
                default -> false;
            };
            
            if (isRemovalAction) {
                // Zaplanuj sprawdzenie po wykonaniu akcji
                int lineIndex = EnhancedFurnaceGUI.getLineIndexForInputSlot(slot);
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    checkAndResetCookTimeIfEmpty(topInventory, lineIndex);
                });
            }
        }
    }
    
    /**
     * Sprawdza czy slot INPUT jest pusty i resetuje cookTime jeśli tak
     * Vanilla behavior: zabranie przedmiotu z INPUT natychmiast resetuje postęp spalania
     */
    private void checkAndResetCookTimeIfEmpty(Inventory inventory, int lineIndex) {
        if (lineIndex < 0 || lineIndex >= 3) return;
        
        EnhancedFurnaceData data = manager.getDataByInventory(inventory);
        if (data == null) return;
        
        EnhancedFurnaceGUI gui = data.getGui();
        ItemStack input = gui.getInputItem(lineIndex);
        
        // Jeśli input jest pusty, resetuj cookTime (vanilla behavior)
        if (input == null || input.getType() == Material.AIR) {
            data.resetCookTime(lineIndex);
            gui.updateSmeltProgress(lineIndex, 0, data.getMaxCookTime());
        }
    }
    
    /**
     * Obsługuje przeciąganie przedmiotów w inventory
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        
        // Sprawdź czy to nasze GUI
        if (!manager.hasInventory(topInventory)) {
            return;
        }
        
        // Sprawdź czy przeciąganie dotyczy naszego inventory
        for (int slot : event.getRawSlots()) {
            if (slot < EnhancedFurnaceGUI.INVENTORY_SIZE) {
                // Slot w naszym GUI
                if (!EnhancedFurnaceGUI.isInteractiveSlot(slot)) {
                    // Przeciąganie na nieinteraktywny slot - blokuj całe przeciąganie
                    event.setCancelled(true);
                    return;
                }
                
                // Blokuj przeciąganie na output sloty
                if (EnhancedFurnaceGUI.isOutputSlot(slot)) {
                    event.setCancelled(true);
                    return;
                }
                
                // Sprawdź czy przeciągany przedmiot to paliwo dla slotów fuel
                if (EnhancedFurnaceGUI.isFuelSlot(slot)) {
                    ItemStack dragged = event.getOldCursor();
                    if (dragged != null && EnhancedFurnaceLogic.getFuelBurnTime(dragged.getType()) <= 0) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }
    
    /**
     * Obsługuje zniszczenie bloku pieca
     * Używamy HIGH priority i ustawiamy dropItems=false żeby vanilla nie dropowała zwykłego pieca
     * 
     * VANILLA BEHAVIOR:
     * - Creative mode: nic nie wypada
     * - Survival bez kilofa: zawartość wypada, ale sam piec NIE
     * - Survival z kilofem: wszystko wypada (piec + zawartość)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        if (!manager.isEnhancedFurnace(block)) {
            return;
        }
        
        // Zablokuj vanilla drop - sami obsługujemy co wypada
        event.setDropItems(false);
        
        Player player = event.getPlayer();
        Location loc = block.getLocation();
        
        // Creative mode - nic nie wypada, tylko usuń dane pieca
        if (player.getGameMode() == GameMode.CREATIVE) {
            manager.removeEnhancedFurnace(block);
            return;
        }
        
        // Survival/Adventure - sprawdź czy użyto kilofa
        boolean usedPickaxe = isPickaxe(player.getInventory().getItemInMainHand().getType());
        
        // Pobierz i usuń dane pieca
        ItemStack[] items = manager.removeEnhancedFurnace(block);
        
        // Dropuj przedmioty
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            if (item == null || item.getType() == Material.AIR) continue;
            
            // Pierwszy element (index 0) to sam piec - dropuj tylko jeśli użyto kilofa
            if (i == 0) {
                if (usedPickaxe) {
                    block.getWorld().dropItemNaturally(loc, item);
                }
                // Bez kilofa - piec nie wypada (jak w vanilla)
            } else {
                // Zawartość pieca - zawsze wypada w survival
                block.getWorld().dropItemNaturally(loc, item);
            }
        }
    }
    
    /**
     * Sprawdza czy materiał jest kilofem.
     */
    private boolean isPickaxe(Material material) {
        return material == Material.WOODEN_PICKAXE ||
               material == Material.STONE_PICKAXE ||
               material == Material.IRON_PICKAXE ||
               material == Material.GOLDEN_PICKAXE ||
               material == Material.DIAMOND_PICKAXE ||
               material == Material.NETHERITE_PICKAXE;
    }
    
    /**
     * Obsługuje eksplozję (TNT, creeper, itp.)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            if (manager.isEnhancedFurnace(block)) {
                ItemStack[] items = manager.removeEnhancedFurnace(block);
                Location loc = block.getLocation();
                
                for (ItemStack item : items) {
                    if (item != null && item.getType() != Material.AIR) {
                        block.getWorld().dropItemNaturally(loc, item);
                    }
                }
            }
        }
    }
    
    /**
     * Obsługuje eksplozję bloku (np. bed w netherze)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            if (manager.isEnhancedFurnace(block)) {
                ItemStack[] items = manager.removeEnhancedFurnace(block);
                Location loc = block.getLocation();
                
                for (ItemStack item : items) {
                    if (item != null && item.getType() != Material.AIR) {
                        block.getWorld().dropItemNaturally(loc, item);
                    }
                }
            }
        }
    }
    
    /**
     * Próbuje dodać przedmiot do podanych slotów
     * Najpierw próbuje stackować z istniejącymi, potem do pustych
     * 
     * @return true jeśli cokolwiek zostało dodane
     */
    private boolean tryAddToSlots(Inventory inventory, ItemStack item, int[] slots) {
        boolean addedAny = false;
        
        // Najpierw stackuj z istniejącymi przedmiotami tego samego typu
        for (int slot : slots) {
            if (item.getAmount() <= 0) break;
            
            ItemStack existing = inventory.getItem(slot);
            if (existing != null && existing.isSimilar(item)) {
                int maxStack = existing.getMaxStackSize();
                int canAdd = maxStack - existing.getAmount();
                
                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, item.getAmount());
                    existing.setAmount(existing.getAmount() + toAdd);
                    item.setAmount(item.getAmount() - toAdd);
                    addedAny = true;
                }
            }
        }
        
        // Potem do pustych slotów
        for (int slot : slots) {
            if (item.getAmount() <= 0) break;
            
            ItemStack existing = inventory.getItem(slot);
            if (existing == null || existing.getType() == Material.AIR) {
                ItemStack toPlace = item.clone();
                toPlace.setAmount(Math.min(item.getAmount(), item.getMaxStackSize()));
                inventory.setItem(slot, toPlace);
                item.setAmount(item.getAmount() - toPlace.getAmount());
                addedAny = true;
            }
        }
        
        return addedAny;
    }
    
    /**
     * Sprawdza czy na danej lokalizacji nadal jest piec (FURNACE, BLAST_FURNACE, SMOKER)
     * Używane do wykrywania usunięcia pieca przez WorldEdit, komendy, itp.
     */
    private boolean isValidEnhancedFurnaceBlock(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.isChunkLoaded()) return true; // Nie sprawdzaj niezaładowanych chunków
        
        Material type = loc.getBlock().getType();
        return type == Material.FURNACE || type == Material.BLAST_FURNACE || type == Material.SMOKER;
    }
    
    // ==================== INVENTORY CLOSE - ZAPOBIEGA PONOWNEMU OTWARCIU ====================
    
    /**
     * Obsługuje zamknięcie inventory pieca.
     * WAŻNE: Oznacza gracza jako "zamykającego" PRZED zamknięciem,
     * aby uniknąć race condition z aktualizacją tytułu (setTitle).
     * setTitle() podczas zamykania powoduje ponowne otwarcie GUI!
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        Inventory topInventory = event.getView().getTopInventory();
        
        // Sprawdź czy to nasze GUI pieca
        if (!manager.hasInventory(topInventory)) {
            return;
        }
        
        // Oznacz gracza jako zamykającego - tick pieca pominie aktualizację tytułu
        EnhancedFurnaceGUI.markPlayerAsClosing(player);
        
        // Zaplanuj usunięcie flagi po 1 ticku (gdy zamknięcie będzie w pełni przetworzone)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            EnhancedFurnaceGUI.unmarkPlayerAsClosing(player);
        }, 1L);
    }
    
    // ==================== CHUNK LOAD/UNLOAD - OPTYMALIZACJA ====================
    
    /**
     * Gdy chunk jest ładowany - aktywuj piece w tym chunku
     * To jest KLUCZOWE dla wydajności - zamiast sprawdzać isChunkLoaded() dla każdego pieca co tick,
     * utrzymujemy Set aktywnych pieców który jest aktualizowany tylko przy load/unload
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        manager.onChunkLoad(event.getChunk());
    }
    
    /**
     * Gdy chunk jest odładowany - dezaktywuj piece w tym chunku
     * Dane pieców pozostają w RAM, ale nie są tickowane (zero CPU)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        manager.onChunkUnload(event.getChunk());
    }
}

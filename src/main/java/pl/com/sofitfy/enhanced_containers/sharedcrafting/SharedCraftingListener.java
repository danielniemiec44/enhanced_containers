package pl.com.sofitfy.enhanced_containers.sharedcrafting;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRecipeBookClickEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import pl.com.sofitfy.enhanced_containers.crafting.vanilla.VanillaCraftingSimulator;

public class SharedCraftingListener implements Listener {

    private final SharedCraftingManager manager;
    private final VanillaCraftingSimulator simulator = new VanillaCraftingSimulator();

    public SharedCraftingListener(SharedCraftingManager manager) {
        this.manager = manager;
    }

    // 1. OBSŁUGA OTWIERANIA (Wspólny widok i trwałość)
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Reagujemy tylko na kliknięcie Prawym Przyciskiem Myszki na blok
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();

            // Sprawdzamy czy blok istnieje i czy to stół rzemieślniczy
            if (block != null && block.getType() == Material.CRAFTING_TABLE) {
                
                Player player = event.getPlayer();
                Location loc = block.getLocation();
                
                // WALIDACJA: Sprawdź czy mamy inventory dla tej lokalizacji i czy blok nadal jest CRAFTING_TABLE
                // To łapie przypadki gdy plugin zmienił blok bez wywoływania eventów
                if (manager.hasInventory(loc) && block.getType() != Material.CRAFTING_TABLE) {
                    handleCraftingTableDestruction(loc);
                    return;
                }
                
                // Jeśli gracz sneakuje (shift), pozwalamy na kładzenie bloków
                // Standardowe zachowanie Minecraft: shift + rightclick = postaw blok obok/nad/pod
                if (player.isSneaking()) {
                    return; // Nie anulujemy eventu - pozwalamy na vanilla behavior (kładzenie bloków)
                }
                
                // Anulujemy domyślne zachowanie (otwarcie prywatnego GUI)
                event.setCancelled(true); 

                // Pobieramy (lub tworzymy) współdzielony ekwipunek dla tej lokalizacji
                Inventory sharedInv = manager.getWorkbenchInventory(loc);

                // Otwieramy graczowi ten konkretny ekwipunek
                player.openInventory(sharedInv);
            }
        }
    }

    // 2. OBSŁUGA NISZCZENIA (Wyrzucanie przedmiotów)
    
    /**
     * Pomocnicza metoda do obsługi niszczenia stołu crafting.
     * Wyrzuca wszystkie przedmioty na ziemię, zamyka GUI graczom i usuwa inventory z managera.
     * 
     * @param loc Lokalizacja niszczonego stołu
     */
    private void handleCraftingTableDestruction(Location loc) {
        // Sprawdzamy, czy dla tego stołu istnieje zapisany ekwipunek w naszym managerze
        if (!manager.hasInventory(loc)) {
            return; // Nie ma co niszczyć
        }
        
        Inventory inv = manager.getWorkbenchInventory(loc);

        // Wyrzucamy przedmioty na ziemię
        // Zaczynamy od indeksu 1, aby pominąć slot wyniku (0), wyrzucamy tylko składniki (1-9)
        for (int i = 1; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                loc.getWorld().dropItemNaturally(loc, item);
            }
        }
        
        // Zamykamy ekwipunek wszystkim graczom, którzy go teraz przeglądają
        // Tworzymy kopię listy, żeby uniknąć ConcurrentModificationException
        new java.util.ArrayList<>(inv.getViewers()).forEach(humanEntity -> humanEntity.closeInventory());
        
        // Czyścimy ekwipunek w pamięci, aby zwolnić itemy
        inv.clear();

        // Usuwamy wpis z managera
        manager.removeInventory(loc);
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.CRAFTING_TABLE) {
            handleCraftingTableDestruction(event.getBlock().getLocation());
        }
    }
    
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        // TNT, Creeper, itp.
        for (Block block : event.blockList()) {
            if (block.getType() == Material.CRAFTING_TABLE) {
                handleCraftingTableDestruction(block.getLocation());
            }
        }
    }
    
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        // Bed, Respawn Anchor w niewłaściwym wymiarze, itp.
        for (Block block : event.blockList()) {
            if (block.getType() == Material.CRAFTING_TABLE) {
                handleCraftingTableDestruction(block.getLocation());
            }
        }
    }
    
    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        // Piston pcha bloki
        for (Block block : event.getBlocks()) {
            if (block.getType() == Material.CRAFTING_TABLE) {
                handleCraftingTableDestruction(block.getLocation());
            }
        }
    }
    
    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        // Sticky piston cofa bloki
        for (Block block : event.getBlocks()) {
            if (block.getType() == Material.CRAFTING_TABLE) {
                handleCraftingTableDestruction(block.getLocation());
            }
        }
    }
    
    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        // Blok spala się
        if (event.getBlock().getType() == Material.CRAFTING_TABLE) {
            handleCraftingTableDestruction(event.getBlock().getLocation());
        }
    }
    
    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        // Enderman podnosi blok, Silverfish wchodzi w blok, itp.
        if (event.getBlock().getType() == Material.CRAFTING_TABLE) {
            handleCraftingTableDestruction(event.getBlock().getLocation());
        }
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // Gdy gracz postawi blok na miejscu, gdzie był zapisany stół rzemieślniczy
        Location loc = event.getBlock().getLocation();
        
        // Jeśli to nie jest crafting table, a mamy zapisany inventory dla tej lokalizacji
        if (event.getBlock().getType() != Material.CRAFTING_TABLE && manager.hasInventory(loc)) {
            handleCraftingTableDestruction(loc);
        }
    }
    
    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        // Woda/lawa przepływa i niszczy blok
        Location loc = event.getToBlock().getLocation();
        if (manager.hasInventory(loc)) {
            // Sprawdź czy blok docelowy to nasz stół
            if (event.getToBlock().getType() == Material.CRAFTING_TABLE) {
                handleCraftingTableDestruction(loc);
            }
        }
    }
    
    @EventHandler
    public void onStructureGrow(StructureGrowEvent event) {
        // Drzewo rośnie i zastępuje bloki
        for (org.bukkit.block.BlockState state : event.getBlocks()) {
            Location loc = state.getLocation();
            if (manager.hasInventory(loc)) {
                handleCraftingTableDestruction(loc);
            }
        }
    }
    
    // 2.5. OBSŁUGA RECIPE BOOK (Szybkie craftingi)
    
    /**
     * Obsługuje kliknięcie w Recipe Book.
     * PlayerRecipeBookClickEvent jest wywoływany gdy gracz kliknie przepis w książce przepisów.
     * 
     * UWAGA: Ten handler jest WYŁĄCZONY, ponieważ RecipeBookPacketHandler (NMS) obsługuje
     * to lepiej - ma dostęp do flagi useMaxItems z pakietu, która określa czy użyto shift-click.
     * Bukkit event nie ma tej informacji (isShiftClick() nie działa dla recipe book clicks).
     * 
     * Ten handler byłby wywoływany PO NMS handlerze i nadpisywałby prawidłowe wypełnienie
     * (maksymalne) pojedynczymi itemami.
     */
    @SuppressWarnings("deprecation")
    @EventHandler
    public void onRecipeBookClick(PlayerRecipeBookClickEvent event) {
        // WYŁĄCZONE - obsługiwane przez RecipeBookPacketHandler (NMS)
        // NMS handler ma dostęp do packet.useMaxItems() który prawidłowo określa shift-click
        // Ten Bukkit event nie ma tej informacji (event.isShiftClick() nie działa dla recipe book)
        
        // Opcjonalnie: możemy anulować event żeby vanilla nie przetwarzała go
        // event.setCancelled(true);
        
        // Debug log
        if (manager.getPlugin() != null) {
            manager.getPlugin().getLogger().fine("[SharedCrafting] Recipe Book click detected but handled by NMS handler");
        }
    }
    
    /**
     * Obsługuje CraftItemEvent - wywoływany przez vanilla gdy użytkownik próbuje craftować.
     * Musimy przekierować to do naszego shared inventory.
     */
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null || topInv.getType() != InventoryType.WORKBENCH) {
            return;
        }
        
        // Sprawdź czy to nasz shared inventory
        if (!manager.hasInventory(topInv)) {
            return;
        }
        
        // Debug log
        if (manager.getPlugin() != null) {
            manager.getPlugin().getLogger().info("[SharedCrafting] CraftItemEvent detected, recipe: " + 
                (event.getRecipe() != null ? event.getRecipe().getResult().getType() : "null"));
        }
        
        // Ten event jest już obsługiwany przez nasz onInventoryClick
        // Nie musimy nic robić - zostawiamy dla logowania
    }
    
    /**
     * Obsługuje PrepareItemCraftEvent - wywoływany gdy vanilla przygotowuje wynik craftingu.
     * To obejmuje też kliknięcie w Recipe Book.
     */
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        CraftingInventory craftingInv = event.getInventory();
        
        // Sprawdź czy to nasz shared inventory (przez porównanie zawartości)
        // PrepareItemCraftEvent używa CraftingInventory który może być naszym WORKBENCH
        if (craftingInv.getType() != InventoryType.WORKBENCH) {
            return;
        }
        
        // Sprawdź czy mamy inventory dla tej lokalizacji gracza
        if (event.getViewers().isEmpty()) {
            return;
        }
        
        Player player = (Player) event.getViewers().get(0);
        Inventory topInv = player.getOpenInventory().getTopInventory();
        
        if (!manager.hasInventory(topInv)) {
            return;
        }
        
        // Debug log
        if (manager.getPlugin() != null) {
            manager.getPlugin().getLogger().info("[SharedCrafting] PrepareItemCraftEvent detected, recipe: " + 
                (event.getRecipe() != null ? event.getRecipe().getResult().getType() : "null"));
        }
        
        // Synchronizuj zawartość z CraftingInventory do naszego shared inventory
        // To jest potrzebne gdy Recipe Book wypełnia siatkę
        Inventory sharedInv = manager.getWorkbenchInventory(manager.getLocationForInventory(topInv));
        if (sharedInv != null) {
            // Kopiuj zawartość siatki (sloty 1-9)
            for (int i = 1; i < Math.min(craftingInv.getSize(), sharedInv.getSize()); i++) {
                ItemStack item = craftingInv.getItem(i);
                sharedInv.setItem(i, item != null ? item.clone() : null);
            }
            
            // Aktualizuj wynik
            updateWorkbench(sharedInv, player.getWorld());
        }
    }
    
    /**
     * Wypełnia siatkę craftingu składnikami z ekwipunku gracza na podstawie przepisu.
     * @param player Gracz
     * @param sharedInv Shared crafting inventory
     * @param recipe Przepis do wypełnienia
     * @param shiftClick Czy użyto shift-click (wypełnij maksymalnie)
     * @return true jeśli udało się wypełnić chociaż raz
     */
    private boolean fillGridFromRecipe(Player player, Inventory sharedInv, Recipe recipe, boolean shiftClick) {
        if (recipe instanceof ShapedRecipe shapedRecipe) {
            return fillGridFromShapedRecipe(player, sharedInv, shapedRecipe, shiftClick);
        } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
            return fillGridFromShapelessRecipe(player, sharedInv, shapelessRecipe, shiftClick);
        }
        return false;
    }
    
    /**
     * Wypełnia siatkę craftingu składnikami na podstawie ShapedRecipe.
     * Używa wspólnego GridFillService (ta sama logika co /4x4 Fast Crafting).
     */
    private boolean fillGridFromShapedRecipe(Player player, Inventory sharedInv, ShapedRecipe recipe, boolean shiftClick) {
        ItemStack[] playerInventory = player.getInventory().getStorageContents();
        
        var result = pl.com.sofitfy.enhanced_containers.crafting.util.GridFillService
            .planForShapedRecipe(playerInventory, recipe, shiftClick);
        
        if (!result.success()) {
            return false;
        }
        
        pl.com.sofitfy.enhanced_containers.crafting.util.GridFillService
            .applyFillResult(player, sharedInv, result);
        
        return true;
    }
    
    /**
     * Usuwa określoną ilość itemów ze slotu ekwipunku.
     */
    private void removeAmountFromSlot(Inventory inventory, int slot, int amount) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || item.getType() == Material.AIR) return;
        
        int newAmount = item.getAmount() - amount;
        if (newAmount <= 0) {
            inventory.setItem(slot, null);
        } else {
            item.setAmount(newAmount);
        }
    }
    
    private org.bukkit.inventory.RecipeChoice getChoiceForSlot(ShapedRecipe recipe, int gridSlot) {
        String[] shape = recipe.getShape();
        Map<Character, org.bukkit.inventory.RecipeChoice> choiceMap = recipe.getChoiceMap();
        
        int row = (gridSlot - 1) / 3;
        int col = (gridSlot - 1) % 3;
        
        if (row >= shape.length || col >= shape[row].length()) {
            return null;
        }
        
        char c = shape[row].charAt(col);
        return choiceMap.get(c);
    }
    
    /**
     * Wypełnia siatkę craftingu składnikami na podstawie ShapelessRecipe.
     * Używa wspólnego GridFillService (ta sama logika co /4x4 Fast Crafting).
     */
    private boolean fillGridFromShapelessRecipe(Player player, Inventory sharedInv, ShapelessRecipe recipe, boolean shiftClick) {
        ItemStack[] playerInventory = player.getInventory().getStorageContents();
        
        var result = pl.com.sofitfy.enhanced_containers.crafting.util.GridFillService
            .planForShapelessRecipe(playerInventory, recipe, shiftClick);
        
        if (!result.success()) {
            return false;
        }
        
        pl.com.sofitfy.enhanced_containers.crafting.util.GridFillService
            .applyFillResult(player, sharedInv, result);
        
        return true;
    }
    
    private ItemStack findMatchingItem(Player player, org.bukkit.inventory.RecipeChoice choice) {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() != Material.AIR && choice.test(item)) {
                return item;
            }
        }
        return null;
    }

    // 3. OBSŁUGA CLICK EVENTÓW (Interakcje i Crafting)
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getType() != InventoryType.WORKBENCH) return;
        if (!manager.hasInventory(event.getInventory())) return;

        // Jeśli modyfikacja dotyczy gridu (1-9)
        boolean gridChanged = false;
        for (int slot : event.getRawSlots()) {
            if (slot >= 1 && slot <= 9) {
                gridChanged = true;
                break;
            }
        }
        
        if (gridChanged) {
            scheduleUpdate(event.getInventory(), event.getWhoClicked().getWorld());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Sprawdzamy czy to współdzielony crafting table
        if (event.getInventory().getType() != InventoryType.WORKBENCH) {
            return;
        }

        // Sprawdzamy czy to nasz współdzielony inventory
        // (używamy referencji do inv, bo manager jest ownerem)
        if (!manager.hasInventory(event.getInventory())) {
            return;
        }
        
        // WALIDACJA: Przy każdej interakcji sprawdź czy stół nadal istnieje
        // To łapie przypadki gdy plugin zmienił blok bez wywoływania eventów
        Location tableLoc = manager.getLocationForInventory(event.getInventory());
        if (tableLoc != null && tableLoc.getBlock().getType() != Material.CRAFTING_TABLE) {
            event.setCancelled(true);
            ((Player) event.getWhoClicked()).closeInventory();
            handleCraftingTableDestruction(tableLoc);
            return;
        }

        if (event.isCancelled()) return;

        Inventory topInv = event.getView().getTopInventory();
        Player player = (Player) event.getWhoClicked();
        
        // BUGFIX: Obsługa Q (drop) w shared inventory
        // Problem: Vanilla MC dropuje item z dolnego inventory zamiast z klikniętego slotu w górnym
        // Rozwiązanie: Wykrywamy akcję DROP i ręcznie dropujemy z właściwego slotu
        int rawSlot = event.getRawSlot();
        boolean isDropAction = event.getAction() == InventoryAction.DROP_ONE_SLOT || event.getAction() == InventoryAction.DROP_ALL_SLOT;
        boolean isDropClick = event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP;
        if (isDropAction || isDropClick) {
            // Sprawdzamy czy to górny inventory (shared crafting table)
            if (rawSlot < topInv.getSize()) {
                // Slot 0 to wynik craftingu - Q powinno działać jak normalne kliknięcie
                // tj. scraftować przedmiot i dać na kursor (lub pod nogi jeśli kursor zajęty)
                if (rawSlot == 0) {
                    handleResultClickDrop(event, topInv, player);
                    return;
                }

                event.setCancelled(true); // Blokujemy vanilla behavior
                
                ItemStack itemToDrop = topInv.getItem(rawSlot);
                if (itemToDrop != null && itemToDrop.getType() != Material.AIR) {
                    // Ile dropujemy?
                    int amountToDrop;
                    if (event.getAction() == InventoryAction.DROP_ONE_SLOT || event.getClick() == ClickType.DROP) {
                        amountToDrop = 1;
                    } else {
                        amountToDrop = itemToDrop.getAmount();
                    }
                    
                    // Tworzymy item do wyrzucenia
                    ItemStack dropped = itemToDrop.clone();
                    dropped.setAmount(amountToDrop);
                    
                    // Dropujemy na świat
                    player.getWorld().dropItemNaturally(player.getLocation(), dropped);
                    
                    // Zmniejszamy stack w inventory
                    int remaining = itemToDrop.getAmount() - amountToDrop;
                    if (remaining <= 0) {
                        topInv.setItem(rawSlot, null);
                    } else {
                        itemToDrop.setAmount(remaining);
                        topInv.setItem(rawSlot, itemToDrop);
                    }
                    
                    // Jeśli to był slot z gridu (1-9), aktualizujemy wynik
                    if (rawSlot >= 1 && rawSlot <= 9) {
                        scheduleUpdate(topInv, player.getWorld());
                    }
                }
                return; // Zakończ - obsłużyliśmy drop
            }
        }

        // KRYTYCZNE: Zablokuj "collect to cursor" (double-click)
        // Podobnie jak w CraftingGUI 4x4 - całkowicie blokujemy double-click
        // Zapobiega to exploitom polegającym na duplikacji itemów poprzez zebranie itemów z gridu
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR || event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        int slot = event.getRawSlot();
        // Inventory topInv = event.getView().getTopInventory(); // Moved up
        // Player player = (Player) event.getWhoClicked();       // Moved up

        // A. KLIKNIĘCIE W SLOT WYNIKU (SLOT 0)
        if (slot == 0) {
            handleResultClick(event, topInv, player);
            return;
        }

        // B. SHIFT-CLICK Z INNYCH INVENTORY (np. z gracza do stołu)
        if (event.isShiftClick() && event.getClickedInventory() != null && event.getClickedInventory().getType() != InventoryType.WORKBENCH) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            // Manualne dodawanie do gridu
            int initialAmount = clickedItem.getAmount();
            int remaining = addToCraftingGrid(topInv, clickedItem);

            if (remaining != initialAmount) {
                if (remaining == 0) {
                    event.setCurrentItem(null);
                } else {
                    clickedItem.setAmount(remaining);
                    event.setCurrentItem(clickedItem);
                }
                // Natychmiastowa aktualizacja po zmianie
                updateWorkbench(topInv, player.getWorld());
            }
            return;
        }

        // C. ZMIANA W GRIDZIE (SLOTY 1-9)
        // Jeśli kliknięto w sloty 1-9 lub przeniesiono coś do nich
        if (slot >= 1 && slot <= 9) {
            // Obsługa SHIFT-CLICK z gridu do gracza (wyjęcie itemu)
            if (event.isShiftClick()) {
                ItemStack current = event.getCurrentItem();
                if (current != null && current.getType() != Material.AIR) {
                    event.setCancelled(true); // Anulujemy domyślną akcję, żeby obsłużyć ręcznie (bezpieczniej)
                    
                    // Przenieś do eq gracza
                    HashMap<Integer, ItemStack> left = player.getInventory().addItem(current);
                    
                    if (left.isEmpty()) {
                        // Wszystko weszło
                        event.setCurrentItem(null);
                    } else {
                        // Część została
                        ItemStack remaining = left.get(0);
                        current.setAmount(remaining.getAmount());
                        // event.setCurrentItem(current); // To update view
                        // W sumie setCurrentItem na slocie klikniętym:
                        topInv.setItem(slot, current);
                    }
                }
            }
            
            // Planujemy aktualizację po zakończeniu eventu (1 tick)
            scheduleUpdate(topInv, player.getWorld());
        }
        // Jeśli kliknięto z shiftem W gridzie (wyjęcie itemu) -> też aktualizuj (pokryte wyżej przez isShiftClick check wewnątrz slot range, ale zostawmy dla innych przypadków)
        else if (event.isShiftClick() && event.getClickedInventory() == topInv) {
             scheduleUpdate(topInv, player.getWorld());
        }
    }

    /**
     * Obsługuje naciśnięcie Q (drop) na slocie wyniku.
     * Zachowanie jak vanilla: craftuje i dropuje wynik na ziemię.
     */
    private void handleResultClickDrop(InventoryClickEvent event, Inventory inv, Player player) {
        event.setCancelled(true);
        
        ItemStack result = inv.getItem(0);
        if (result == null || result.getType() == Material.AIR) {
            return;
        }

        // Vanilla behavior: Q na result slot zawsze dropuje na ziemię
        player.getWorld().dropItemNaturally(player.getLocation(), result.clone());
        
        // Zużyj składniki
        decrementIngredients(inv);
        
        // Aktualizuj stan stołu
        updateWorkbench(inv, player.getWorld());
    }

    private void handleResultClick(InventoryClickEvent event, Inventory inv, Player player) {
        ItemStack result = inv.getItem(0);
        if (result == null || result.getType() == Material.AIR) {
            event.setCancelled(true); 
            return;
        }

        ItemStack cursor = event.getCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            if (!cursor.isSimilar(result) || cursor.getAmount() + result.getAmount() > cursor.getMaxStackSize()) {
                event.setCancelled(true); 
                return;
            }
        }
        
        event.setCancelled(true); // Zawsze anulujemy event, obsługujemy ręcznie

        if (event.isShiftClick()) {
            // ================================================================
            // SHIFT-CLICK CRAFTING - PODEJŚCIE TRANSAKCYJNE
            // Zapobiega duplikacji i utracie przedmiotów poprzez:
            // 1. Obliczenie limitów PRZED modyfikacją
            // 2. Atomową modyfikację stanu
            // ================================================================
            handleShiftClickCraftingTransactional(inv, player, result);
        } else {
            // Zwykłe wyjęcie na kursor (pojedynczy craft)
            if (cursor == null || cursor.getType() == Material.AIR) {
                event.getView().setCursor(result.clone());
            } else {
                cursor.setAmount(cursor.getAmount() + result.getAmount());
                event.getView().setCursor(cursor);
            }
            // Zjedz 1 zestaw składników
            decrementIngredients(inv);
            
            // Aktualizuj stan stołu po operacji
            updateWorkbench(inv, player.getWorld());
        }
    }

    /**
     * Obsługuje Shift-Click na slot wyniku używając PODEJŚCIA TRANSAKCYJNEGO.
     * 
     * ALGORYTM:
     * 1. ANALIZA (tylko odczyt) - oblicz maksymalną liczbę craftów
     * 2. WALIDACJA - sprawdź czy można wykonać operację  
     * 3. WYKONANIE - atomowa modyfikacja: najpierw daj itemy, potem usuń składniki
     * 
     * Ten algorytm zapobiega:
     * - Duplikacji przedmiotów (dawna pętla while mogła dodać więcej niż zużyć)
     * - Utracie przedmiotów (składniki są usuwane DOPIERO po wydaniu wyniku)
     */
    private void handleShiftClickCraftingTransactional(Inventory inv, Player player, ItemStack result) {
        
        // ========================================
        // KROK 1: ANALIZA STANU (TYLKO ODCZYT)
        // ========================================
        
        // Oblicz maksymalną liczbę craftów na podstawie najmniejszego stosu składników
        int maxCraftsFromIngredients = calculateMaxCraftsFromMatrix(inv);
        
        // Oblicz ile miejsca jest w ekwipunku gracza na wynik
        int maxCraftsFromSpace = calculateInventorySpaceForResult(player, result);
        
        // Określ finalną liczbę craftów (minimum z obu limitów)
        // Limit 64 dla bezpieczeństwa (zapobiega nieskończonym pętlom)
        int finalCraftCount = Math.min(maxCraftsFromIngredients, maxCraftsFromSpace);
        finalCraftCount = Math.min(finalCraftCount, 64);
        
        // ========================================
        // KROK 2: WALIDACJA
        // ========================================
        
        if (finalCraftCount <= 0) {
            // Nie można wykonać żadnego craftu - zakończ bez modyfikacji
            return;
        }
        
        // ========================================
        // KROK 3: WYKONANIE TRANSAKCJI (ATOMOWO)
        // ========================================
        
        // KROK A: Oblicz całkowitą ilość przedmiotów do wydania
        int totalItemsToGive = result.getAmount() * finalCraftCount;
        
        // KROK B: Przygotuj przedmioty do wydania
        ItemStack itemsToGive = result.clone();
        itemsToGive.setAmount(totalItemsToGive);
        
        // KROK C: Dodaj przedmioty do ekwipunku gracza
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(itemsToGive);
        
        // KROK D: Jeśli coś nie zmieściło się - upuść na ziemię (bezpieczeństwo)
        if (!overflow.isEmpty()) {
            for (ItemStack overflowItem : overflow.values()) {
                if (overflowItem != null && overflowItem.getType() != Material.AIR) {
                    player.getWorld().dropItemNaturally(player.getLocation(), overflowItem);
                }
            }
        }
        
        // KROK E: Usuń składniki z matrycy (dokładnie finalCraftCount razy)
        decrementIngredientsMultiple(inv, finalCraftCount);
        
        // KROK F: Zaktualizuj wynik craftingu
        updateWorkbenchSync(inv, player.getWorld());
    }

    /**
     * Oblicza maksymalną liczbę craftów możliwych do wykonania
     * na podstawie NAJMNIEJSZEGO stosu składników w matrycy.
     * 
     * Matryca craftingu: sloty 1-9 w inventory (slot 0 to wynik)
     * 
     * @param inv Inventory stołu rzemieślniczego
     * @return Maksymalna liczba craftów (minimum ze wszystkich stosów składników)
     */
    private int calculateMaxCraftsFromMatrix(Inventory inv) {
        int minStackSize = Integer.MAX_VALUE;
        boolean hasAnyIngredient = false;
        
        // Sloty 1-9 to matryca craftingu
        for (int i = 1; i < 10; i++) {
            ItemStack ingredient = inv.getItem(i);
            
            // Pomiń puste sloty
            if (ingredient == null || ingredient.getType() == Material.AIR) {
                continue;
            }
            
            hasAnyIngredient = true;
            
            // Znajdź najmniejszy stos (to limituje liczbę craftów)
            if (ingredient.getAmount() < minStackSize) {
                minStackSize = ingredient.getAmount();
            }
        }
        
        // Jeśli brak składników, nie można craftować
        if (!hasAnyIngredient) {
            return 0;
        }
        
        return minStackSize;
    }

    /**
     * Oblicza ile razy można scraftować dany wynik zanim ekwipunek gracza się zapełni.
     * Uwzględnia zarówno wolne sloty jak i możliwość dokładania do istniejących stosów.
     * 
     * @param player Gracz
     * @param result Przedmiot wyniku (z jednego craftu)
     * @return Maksymalna liczba craftów które zmieszczą się w ekwipunku
     */
    private int calculateInventorySpaceForResult(Player player, ItemStack result) {
        int availableSpace = 0;
        int maxStackSize = result.getMaxStackSize();
        
        // Przeszukaj storage contents gracza (sloty 0-35, bez zbroi i off-hand)
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                // Pusty slot - zmieści się cały stos
                availableSpace += maxStackSize;
            } else if (slot.isSimilar(result)) {
                // Slot z tym samym przedmiotem - zmieści się różnica do max stacka
                availableSpace += Math.max(0, maxStackSize - slot.getAmount());
            }
        }
        
        // Zwróć liczbę craftów (dzielenie przez ilość wyników z jednego craftu)
        // Użyj dzielenia całkowitego w dół
        return availableSpace / result.getAmount();
    }

    /**
     * Zmniejsza składniki w matrycy craftingu o określoną liczbę craftów.
     * Obsługuje specjalne przypadki jak wiadra (zostawiają puste wiadro).
     * 
     * @param inv Inventory stołu rzemieślniczego
     * @param craftCount Liczba craftów do odjęcia od każdego składnika
     */
    private void decrementIngredientsMultiple(Inventory inv, int craftCount) {
        // Sloty 1-9 to matryca craftingu
        for (int i = 1; i < 10; i++) {
            ItemStack item = inv.getItem(i);
            
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            
            int currentAmount = item.getAmount();
            int newAmount = currentAmount - craftCount;
            
            if (newAmount <= 0) {
                // Składnik został zużyty całkowicie
                // Obsługa specjalnych przypadków (wiadra)
                if (item.getType() == Material.MILK_BUCKET || 
                    item.getType() == Material.WATER_BUCKET || 
                    item.getType() == Material.LAVA_BUCKET) {
                    // Zostaw puste wiadra — tyle ile zużyto
                    inv.setItem(i, new ItemStack(Material.BUCKET, craftCount));
                } else {
                    inv.setItem(i, null);
                }
            } else {
                // Pozostała część składnika
                item.setAmount(newAmount);
                inv.setItem(i, item);
            }
        }
    }

    /**
     * Sprawdza czy gracz ma miejsce w ekwipunku na dany item.
     * Sprawdzane są tylko sloty głównego ekwipunku (bez armora i offhandu).
     */
    private boolean canFit(Player player, ItemStack item) {
        int space = 0;
        // Sprawdzamy tylko storage contents (bez armora/offhandu)
        for (ItemStack i : player.getInventory().getStorageContents()) {
            if (i == null || i.getType() == Material.AIR) {
                space += item.getMaxStackSize();
            } else if (i.isSimilar(item)) {
                space += Math.max(0, i.getMaxStackSize() - i.getAmount());
            }
        }
        return space >= item.getAmount();
    }

    private void decrementIngredients(Inventory inv) {
        for (int i = 1; i < 10; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                int amt = item.getAmount();
                if (amt > 1) {
                    item.setAmount(amt - 1);
                    inv.setItem(i, item);
                } else {
                     if (item.getType() == Material.MILK_BUCKET || item.getType() == Material.WATER_BUCKET || item.getType() == Material.LAVA_BUCKET) {
                        inv.setItem(i, new ItemStack(Material.BUCKET));
                    } else {
                        inv.setItem(i, null);
                    }
                }
            }
        }
    }

    private void scheduleUpdate(Inventory inv, World world) {
        if (manager.getPlugin() == null) return;
        Bukkit.getScheduler().runTask(manager.getPlugin(), () -> {
            updateWorkbench(inv, world);
        });
    }

    private void updateWorkbench(Inventory inv, World world) {
        // Defensive null check - inv może być null jeśli stół został usunięty
        if (inv == null) {
            return;
        }
        
        ItemStack[] grid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            grid[i] = inv.getItem(i + 1);
        }

        Player updateContext = null;
        if (!inv.getViewers().isEmpty() && inv.getViewers().get(0) instanceof Player) {
            updateContext = (Player) inv.getViewers().get(0);
        }
        
        ItemStack result = simulator.tryMatch(updateContext, grid);
        inv.setItem(0, result);
    }

    private int addToCraftingGrid(Inventory inv, ItemStack item) {
        int amount = item.getAmount();
        int maxStack = item.getMaxStackSize();

        // 1. Spróbuj dodać do istniejących stacków (sloty 1-9)
        for (int i = 1; i < 10; i++) {
            if (amount <= 0) break;
            ItemStack slotItem = inv.getItem(i);

            if (slotItem != null && slotItem.isSimilar(item)) {
                int space = maxStack - slotItem.getAmount();
                if (space > 0) {
                    int toAdd = Math.min(amount, space);
                    slotItem.setAmount(slotItem.getAmount() + toAdd);
                    inv.setItem(i, slotItem);
                    amount -= toAdd;
                }
            }
        }

        // 2. Wypełnij puste sloty (sloty 1-9)
        for (int i = 1; i < 10; i++) {
            if (amount <= 0) break;
            ItemStack slotItem = inv.getItem(i);

            if (slotItem == null || slotItem.getType() == Material.AIR) {
                ItemStack newItem = item.clone();
                newItem.setAmount(Math.min(amount, maxStack));
                inv.setItem(i, newItem);
                amount -= newItem.getAmount();
            }
        }

        return amount;
    }
    
    /**
     * Synchronicznie aktualizuje wynik craftingu w stole.
     * Używane podczas bulk craftingu, gdzie musimy od razu wiedzieć jaki jest nowy wynik.
     */
    private void updateWorkbenchSync(Inventory inv, World world) {
        // Defensive null check - inv może być null jeśli stół został usunięty
        if (inv == null) {
            return;
        }
        
        ItemStack[] grid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            grid[i] = inv.getItem(i + 1);
        }

        Player updateContext = null;
        if (!inv.getViewers().isEmpty() && inv.getViewers().get(0) instanceof Player) {
            updateContext = (Player) inv.getViewers().get(0);
        }
        
        ItemStack result = simulator.tryMatch(updateContext, grid);
        inv.setItem(0, result);
    }
}

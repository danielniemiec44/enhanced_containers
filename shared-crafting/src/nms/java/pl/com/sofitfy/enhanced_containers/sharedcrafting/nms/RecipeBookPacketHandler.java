package pl.com.sofitfy.enhanced_containers.sharedcrafting.nms;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import pl.com.sofitfy.enhanced_containers.sharedcrafting.SharedCraftingManager;
import pl.com.sofitfy.enhanced_containers.crafting.vanilla.VanillaCraftingSimulator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * NMS Packet Handler do przechwytywania kliknięć w Recipe Book.
 * 
 * Kiedy gracz kliknie przepis w Recipe Book, klient wysyła pakiet ServerboundPlaceRecipePacket.
 * Vanilla Minecraft obsługuje ten pakiet tylko dla domyślnych CraftingInventory (vanilla workbench).
 * Nasze custom inventory (utworzone przez Bukkit.createInventory) nie jest rozpoznawane przez vanilla.
 * 
 * To rozwiązanie:
 * 1. Przechwytuje pakiet ServerboundPlaceRecipePacket przed dotarciem do serwera
 * 2. Sprawdza czy gracz ma otwarty nasz shared crafting inventory
 * 3. Pobiera RecipeDisplayEntry z kontenerowego menu
 * 4. Ręcznie wypełnia siatkę składnikami i blokuje oryginalny pakiet
 */
public class RecipeBookPacketHandler {

    private static final String HANDLER_NAME = "plethoras_recipe_book_handler";
    
    // Śledzi stan shift dla każdego gracza (UUID -> czy shift wciśnięty)
    private static final Map<UUID, Boolean> playerShiftState = new ConcurrentHashMap<>();
    
    private final JavaPlugin plugin;
    private final SharedCraftingManager sharedCraftingManager;
    private final VanillaCraftingSimulator simulator = new VanillaCraftingSimulator();
    
    public RecipeBookPacketHandler(JavaPlugin plugin, SharedCraftingManager sharedCraftingManager) {
        this.plugin = plugin;
        this.sharedCraftingManager = sharedCraftingManager;
    }
    
    public JavaPlugin getPlugin() {
        return plugin;
    }
    
    public void registerPlayer(Player player) {
        if (!(player instanceof CraftPlayer craftPlayer)) {
            return;
        }
        
        try {
            ServerPlayer serverPlayer = craftPlayer.getHandle();
            Channel channel = serverPlayer.connection.connection.channel;
            ChannelPipeline pipeline = channel.pipeline();
            
            if (pipeline.get(HANDLER_NAME) != null) {
                pipeline.remove(HANDLER_NAME);
            }
            
            pipeline.addBefore("packet_handler", HANDLER_NAME, new RecipeBookChannelHandler(player));
            
            plugin.getLogger().info("[RecipeBook NMS] Registered packet handler for player: " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("[RecipeBook NMS] Failed to register packet handler for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    public void unregisterPlayer(Player player) {
        if (!(player instanceof CraftPlayer craftPlayer)) {
            return;
        }
        
        try {
            ServerPlayer serverPlayer = craftPlayer.getHandle();
            Channel channel = serverPlayer.connection.connection.channel;
            ChannelPipeline pipeline = channel.pipeline();
            
            if (pipeline.get(HANDLER_NAME) != null) {
                pipeline.remove(HANDLER_NAME);
            }
        } catch (Exception e) {
            // Ignoruj
        }
    }
    
    public void registerAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            registerPlayer(player);
        }
    }
    
    public void unregisterAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            unregisterPlayer(player);
        }
    }
    
    private class RecipeBookChannelHandler extends ChannelDuplexHandler {
        
        private final Player player;
        
        public RecipeBookChannelHandler(Player player) {
            this.player = player;
        }
        
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // Śledź stan shift przez pakiety input
            if (msg instanceof ServerboundPlayerInputPacket inputPacket) {
                // input.shift() zwraca true gdy gracz trzyma shift
                boolean isShiftDown = inputPacket.input().shift();
                playerShiftState.put(player.getUniqueId(), isShiftDown);
            }
            
            if (msg instanceof ServerboundPlaceRecipePacket packet) {
                if (handleRecipeBookClick(packet)) {
                    return;
                }
            }
            super.channelRead(ctx, msg);
        }
        
        private boolean handleRecipeBookClick(ServerboundPlaceRecipePacket packet) {
            Inventory topInv = player.getOpenInventory().getTopInventory();
            if (topInv == null || topInv.getType() != InventoryType.WORKBENCH) {
                return false;
            }
            
            if (!sharedCraftingManager.hasInventory(topInv)) {
                return false;
            }
            
            RecipeDisplayId recipeId = packet.recipe();
            boolean useMaxItems = packet.useMaxItems();
            
            // Dla shared crafting ZAWSZE wypełniaj maksymalną ilość
            // Klient nie wysyła poprawnie useMaxItems dla custom inventory
            // Gracz może ręcznie umieścić pojedyncze itemy jeśli chce mniej
            boolean shouldUseMax = true;
            
            plugin.getLogger().info("[RecipeBook NMS] Intercepted recipe click: recipeId=" + recipeId.index() 
                + ", useMaxItems=" + useMaxItems + ", shouldUseMax=" + shouldUseMax + " (forced max for shared crafting)");
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    Inventory currentTopInv = player.getOpenInventory().getTopInventory();
                    if (currentTopInv == null || !sharedCraftingManager.hasInventory(currentTopInv)) {
                        return;
                    }
                    
                    boolean success = fillGridFromRecipeDisplayId(player, currentTopInv, recipeId, shouldUseMax);
                    
                    if (success) {
                        updateWorkbench(currentTopInv, player);
                        player.updateInventory();
                        plugin.getLogger().info("[RecipeBook NMS] Successfully filled grid for recipe: " + recipeId.index());
                    } else {
                        plugin.getLogger().info("[RecipeBook NMS] Could not fill grid for recipe: " + recipeId.index());
                    }
                    
                } catch (Exception e) {
                    plugin.getLogger().warning("[RecipeBook NMS] Error handling recipe: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
            return true;
        }
    }
    
    /**
     * Wypełnia siatkę craftingu na podstawie RecipeDisplayId.
     * Używa ServerRecipeBook gracza do pobrania przepisu.
     */
    private boolean fillGridFromRecipeDisplayId(Player player, Inventory sharedInv, RecipeDisplayId recipeId, boolean useMaxItems) {
        if (!(player instanceof CraftPlayer craftPlayer)) {
            return false;
        }
        
        ServerPlayer serverPlayer = craftPlayer.getHandle();
        
        // Pobierz RecipeDisplayEntry z ServerRecipeBook gracza
        RecipeDisplayEntry displayEntry = getRecipeDisplayEntryFromServer(recipeId, serverPlayer);
        
        if (displayEntry == null) {
            plugin.getLogger().warning("[RecipeBook NMS] Could not get RecipeDisplayEntry for recipeId: " + recipeId.index());
            return false;
        }
        
        var display = displayEntry.display();
        
        plugin.getLogger().info("[RecipeBook NMS] Found recipe display type: " + display.getClass().getSimpleName());
        
        if (display instanceof ShapedCraftingRecipeDisplay shapedDisplay) {
            return fillFromShapedDisplay(player, sharedInv, shapedDisplay, serverPlayer, useMaxItems);
        } else if (display instanceof ShapelessCraftingRecipeDisplay shapelessDisplay) {
            return fillFromShapelessDisplay(player, sharedInv, shapelessDisplay, serverPlayer, useMaxItems);
        } else {
            plugin.getLogger().info("[RecipeBook NMS] Unsupported recipe display type: " + display.getClass().getSimpleName());
            return false;
        }
    }
    
    /**
     * Pobiera RecipeDisplayEntry z serwera używając RecipeDisplayId.
     * W 1.21.11 używamy RecipeManager.getRecipeFromDisplay() który zwraca ServerDisplayInfo.
     */
    private RecipeDisplayEntry getRecipeDisplayEntryFromServer(RecipeDisplayId recipeId, ServerPlayer serverPlayer) {
        try {
            plugin.getLogger().info("[RecipeBook NMS] Looking for recipeId index: " + recipeId.index());
            
            // Użyj RecipeManager.getRecipeFromDisplay(RecipeDisplayId) -> ServerDisplayInfo
            var server = ((org.bukkit.craftbukkit.CraftServer) org.bukkit.Bukkit.getServer()).getServer();
            if (server != null) {
                var recipeManager = server.getRecipeManager();
                
                // Wywołaj getRecipeFromDisplay(RecipeDisplayId) -> ServerDisplayInfo
                var serverDisplayInfo = recipeManager.getRecipeFromDisplay(recipeId);
                
                if (serverDisplayInfo != null) {
                    // ServerDisplayInfo ma metodę display() która zwraca RecipeDisplayEntry
                    RecipeDisplayEntry entry = serverDisplayInfo.display();
                    if (entry != null) {
                        plugin.getLogger().info("[RecipeBook NMS] Found RecipeDisplayEntry for recipeId: " + recipeId.index());
                        return entry;
                    }
                } else {
                    plugin.getLogger().warning("[RecipeBook NMS] getRecipeFromDisplay returned null for recipeId: " + recipeId.index());
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("[RecipeBook NMS] Error getting recipe display entry: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    private boolean fillFromShapedDisplay(Player player, Inventory sharedInv, ShapedCraftingRecipeDisplay display, 
                                          ServerPlayer serverPlayer, boolean useMaxItems) {
        int width = display.width();
        int height = display.height();
        List<SlotDisplay> ingredients = display.ingredients();
        
        // Mapuj składniki na pozycje w siatce 3x3
        Map<Integer, SlotDisplay> slotToIngredient = new HashMap<>();
        
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int ingredientIndex = row * width + col;
                if (ingredientIndex < ingredients.size()) {
                    SlotDisplay ingredient = ingredients.get(ingredientIndex);
                    if (!(ingredient instanceof SlotDisplay.Empty)) {
                        int gridSlot = 1 + row * 3 + col;
                        slotToIngredient.put(gridSlot, ingredient);
                    }
                }
            }
        }
        
        return fillGrid(player, sharedInv, slotToIngredient, serverPlayer, useMaxItems);
    }
    
    private boolean fillFromShapelessDisplay(Player player, Inventory sharedInv, ShapelessCraftingRecipeDisplay display,
                                             ServerPlayer serverPlayer, boolean useMaxItems) {
        List<SlotDisplay> ingredients = display.ingredients();
        
        Map<Integer, SlotDisplay> slotToIngredient = new HashMap<>();
        int slotIndex = 1;
        
        for (SlotDisplay ingredient : ingredients) {
            if (!(ingredient instanceof SlotDisplay.Empty)) {
                slotToIngredient.put(slotIndex, ingredient);
                slotIndex++;
                if (slotIndex > 9) break;
            }
        }
        
        return fillGrid(player, sharedInv, slotToIngredient, serverPlayer, useMaxItems);
    }
    
    /**
     * Wypełnia siatkę craftingu składnikami z ekwipunku gracza.
     * 
     * Przy useMaxItems=true (shift-click) wypełnia MAKSYMALNĄ możliwą ilością,
     * tak jak komenda /4x4 - zbiera WSZYSTKIE pasujące itemy z ekwipunku
     * i rozdziela je równomiernie na sloty siatki.
     */
    private boolean fillGrid(Player player, Inventory sharedInv, 
                            Map<Integer, SlotDisplay> slotToIngredient,
                            ServerPlayer serverPlayer,
                            boolean useMaxItems) {
        
        // VANILLA BEHAVIOR: Najpierw zwróć wszystkie przedmioty z siatki do inventory gracza
        returnGridItemsToPlayer(player, sharedInv);
        
        if (slotToIngredient.isEmpty()) {
            return false;
        }
        
        ItemStack[] playerContents = player.getInventory().getStorageContents();
        
        // ========== NOWA LOGIKA DLA SHIFT-CLICK ==========
        // Zbieramy WSZYSTKIE pasujące itemy z ekwipunku, nie tylko z pierwszego slotu
        
        // Dla każdego gridSlot zapisz listę pasujących slotów gracza z ilościami
        Map<Integer, java.util.List<int[]>> gridSlotSources = new LinkedHashMap<>();
        
        // Znajdź wszystkie źródła dla każdego gridSlot
        for (Map.Entry<Integer, SlotDisplay> entry : slotToIngredient.entrySet()) {
            int gridSlot = entry.getKey();
            SlotDisplay slotDisplay = entry.getValue();
            
            java.util.List<int[]> sources = new java.util.ArrayList<>();
            List<net.minecraft.world.item.ItemStack> possibleItems = resolvePossibleItems(slotDisplay, serverPlayer);
            
            if (!possibleItems.isEmpty()) {
                for (int i = 0; i < playerContents.length; i++) {
                    ItemStack item = playerContents[i];
                    if (item != null && item.getType() != Material.AIR) {
                        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
                        for (net.minecraft.world.item.ItemStack possibleItem : possibleItems) {
                            if (net.minecraft.world.item.ItemStack.isSameItem(nmsItem, possibleItem)) {
                                sources.add(new int[]{i, item.getAmount()});
                                break;
                            }
                        }
                    }
                }
            }
            
            if (sources.isEmpty()) {
                return false; // Brak składnika
            }
            
            gridSlotSources.put(gridSlot, sources);
        }
        
        // Grupuj gridSloty które mogą używać tych samych itemów
        // Zamiast toString(), grupuj po pierwszym pasującym itemie w inventory
        // (wszystkie sloty używające tego samego itemu powinny być w jednej grupie)
        Map<String, java.util.List<Integer>> itemTypeToGridSlots = new LinkedHashMap<>();
        
        for (Map.Entry<Integer, java.util.List<int[]>> entry : gridSlotSources.entrySet()) {
            int gridSlot = entry.getKey();
            java.util.List<int[]> sources = entry.getValue();
            
            // Użyj Material pierwszego pasującego itemu jako klucz grupowania
            if (!sources.isEmpty()) {
                int firstSourceSlot = sources.get(0)[0];
                ItemStack firstItem = playerContents[firstSourceSlot];
                String itemKey = firstItem.getType().name();
                itemTypeToGridSlots.computeIfAbsent(itemKey, k -> new java.util.ArrayList<>()).add(gridSlot);
            }
        }
        
        plugin.getLogger().info("[RecipeBook NMS] Grouped grid slots: " + itemTypeToGridSlots);
        
        // Mapa: playerSlot -> ile dostępne
        Map<Integer, Integer> availableInSlot = new HashMap<>();
        for (int i = 0; i < playerContents.length; i++) {
            ItemStack item = playerContents[i];
            if (item != null && item.getType() != Material.AIR) {
                availableInSlot.put(i, item.getAmount());
            }
        }
        
        // Oblicz maxFills dla każdej grupy składników
        int maxFills = useMaxItems ? 64 : 1;
        
        for (Map.Entry<String, java.util.List<Integer>> entry : itemTypeToGridSlots.entrySet()) {
            String itemKey = entry.getKey();
            java.util.List<Integer> gridSlots = entry.getValue();
            int slotsNeeded = gridSlots.size();
            
            // Zlicz WSZYSTKIE pasujące itemy dla tego typu
            int totalAvailable = 0;
            int itemMaxStackSize = 64;
            Material targetMaterial = Material.valueOf(itemKey);
            
            for (int i = 0; i < playerContents.length; i++) {
                ItemStack item = playerContents[i];
                if (item != null && item.getType() == targetMaterial) {
                    totalAvailable += item.getAmount();
                    itemMaxStackSize = item.getMaxStackSize();
                }
            }
            
            // Ile wypełnień możemy zrobić z tej grupy?
            int possibleFills = totalAvailable / slotsNeeded;
            possibleFills = Math.min(possibleFills, itemMaxStackSize);
            
            plugin.getLogger().info("[RecipeBook NMS] Group " + itemKey + ": slotsNeeded=" + slotsNeeded 
                + ", totalAvailable=" + totalAvailable + ", possibleFills=" + possibleFills);
            
            maxFills = Math.min(maxFills, possibleFills);
        }
        
        if (maxFills <= 0) {
            plugin.getLogger().warning("[RecipeBook NMS] maxFills <= 0, cannot fill grid");
            return false;
        }
        
        plugin.getLogger().info("[RecipeBook NMS] Calculated maxFills=" + maxFills + " (useMaxItems=" + useMaxItems + ")");
        
        // Rozdziel pobieranie z wielu slotów gracza
        Map<Integer, ItemStack> gridToItemClone = new LinkedHashMap<>();
        Map<Integer, Integer> slotsToUse = new LinkedHashMap<>();
        
        // Resetuj dostępność
        availableInSlot.clear();
        for (int i = 0; i < playerContents.length; i++) {
            ItemStack item = playerContents[i];
            if (item != null && item.getType() != Material.AIR) {
                availableInSlot.put(i, item.getAmount());
            }
        }
        
        // Dla każdego gridSlot, przydziel maxFills itemów z dostępnych źródeł
        for (Map.Entry<Integer, java.util.List<int[]>> entry : gridSlotSources.entrySet()) {
            int gridSlot = entry.getKey();
            java.util.List<int[]> sources = entry.getValue();
            
            int needed = maxFills;
            ItemStack itemClone = null;
            
            for (int[] source : sources) {
                int playerSlot = source[0];
                int available = availableInSlot.getOrDefault(playerSlot, 0);
                
                if (available <= 0) continue;
                
                int toTake = Math.min(needed, available);
                if (toTake > 0) {
                    if (itemClone == null) {
                        itemClone = playerContents[playerSlot].clone();
                    }
                    
                    availableInSlot.put(playerSlot, available - toTake);
                    slotsToUse.merge(playerSlot, toTake, Integer::sum);
                    needed -= toTake;
                    
                    if (needed <= 0) break;
                }
            }
            
            if (itemClone == null || needed > 0) {
                return false;
            }
            
            gridToItemClone.put(gridSlot, itemClone);
        }
        
        // Usuń z ekwipunku gracza
        for (Map.Entry<Integer, Integer> entry : slotsToUse.entrySet()) {
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
        
        // Dodaj do siatki - każdy slot dostaje maxFills itemów
        for (Map.Entry<Integer, ItemStack> entry : gridToItemClone.entrySet()) {
            int gridSlot = entry.getKey();
            ItemStack itemClone = entry.getValue();
            
            if (itemClone == null) continue;
            
            ItemStack toPlace = itemClone.clone();
            toPlace.setAmount(maxFills);
            sharedInv.setItem(gridSlot, toPlace);
        }
        
        return true;
    }
    
    /**
     * Znajduje slot gracza z pasującym itemem który ma jeszcze dostępne sztuki.
     * @return indeks slotu lub -1 jeśli nie znaleziono
     */
    private int findMatchingSlotForSlotDisplay(Player player, SlotDisplay slotDisplay, ServerPlayer serverPlayer,
                                               Map<Integer, Integer> availableInSlot, ItemStack currentInGrid) {
        List<net.minecraft.world.item.ItemStack> possibleItems = resolvePossibleItems(slotDisplay, serverPlayer);
        if (possibleItems.isEmpty()) {
            return -1;
        }
        
        ItemStack[] playerContents = player.getInventory().getStorageContents();
        
        for (int slot = 0; slot < playerContents.length; slot++) {
            ItemStack item = playerContents[slot];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            
            int available = availableInSlot.getOrDefault(slot, 0);
            if (available <= 0) {
                continue;
            }
            
            // Sprawdź czy item pasuje do tego co już jest w siatce
            if (currentInGrid != null && currentInGrid.getType() != Material.AIR) {
                if (!currentInGrid.isSimilar(item)) {
                    continue;
                }
            }
            
            // Sprawdź czy item pasuje do wymagań przepisu
            net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
            for (net.minecraft.world.item.ItemStack possibleItem : possibleItems) {
                if (net.minecraft.world.item.ItemStack.isSameItem(nmsItem, possibleItem)) {
                    return slot;
                }
            }
        }
        
        return -1;
    }
    
    /**
     * Zwraca wszystkie przedmioty z siatki craftingu (sloty 1-9) do inventory gracza.
     * VANILLA BEHAVIOR: W Recipe Book przed wypełnieniem nowym przepisem,
     * stare przedmioty zawsze wracają do ekwipunku gracza.
     * 
     * @param player gracz do którego zwracamy przedmioty
     * @param sharedInv shared crafting inventory
     */
    private void returnGridItemsToPlayer(Player player, Inventory sharedInv) {
        for (int slot = 1; slot <= 9; slot++) {
            ItemStack item = sharedInv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                // Próbuj dodać do inventory gracza
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
                
                // Jeśli inventory jest pełne, upuść na ziemię (jak vanilla)
                if (!leftover.isEmpty()) {
                    for (ItemStack dropped : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), dropped);
                    }
                }
                
                // Wyczyść slot w siatce
                sharedInv.setItem(slot, null);
            }
        }
        
        // Wyczyść też output slot (slot 0) - będzie przeliczony
        sharedInv.setItem(0, null);
    }
    
    /**
     * Znajduje pasujący przedmiot w ekwipunku gracza dla danego SlotDisplay.
     * SlotDisplay może zawierać kilka możliwych przedmiotów (np. dla tagów).
     */
    private ItemStack findMatchingItemForSlotDisplay(Player player, SlotDisplay slotDisplay, ServerPlayer serverPlayer) {
        // Rozwiąż SlotDisplay na listę możliwych ItemStack
        List<net.minecraft.world.item.ItemStack> possibleItems = resolvePossibleItems(slotDisplay, serverPlayer);
        
        if (possibleItems.isEmpty()) {
            return null;
        }
        
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            
            net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
            
            for (net.minecraft.world.item.ItemStack possibleItem : possibleItems) {
                if (net.minecraft.world.item.ItemStack.isSameItem(nmsItem, possibleItem)) {
                    return item;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Rozwiązuje SlotDisplay na listę możliwych ItemStack.
     */
    private List<net.minecraft.world.item.ItemStack> resolvePossibleItems(SlotDisplay slotDisplay, ServerPlayer serverPlayer) {
        List<net.minecraft.world.item.ItemStack> result = new ArrayList<>();
        
        // Obsłuż poszczególne typy SlotDisplay
        if (slotDisplay instanceof SlotDisplay.ItemSlotDisplay itemDisplay) {
            result.add(new net.minecraft.world.item.ItemStack(itemDisplay.item()));
        } else if (slotDisplay instanceof SlotDisplay.ItemStackSlotDisplay stackDisplay) {
            result.add(stackDisplay.stack());
        } else if (slotDisplay instanceof SlotDisplay.TagSlotDisplay tagDisplay) {
            // Dla tagów musimy ręcznie rozwiązać
            var tag = tagDisplay.tag();
            try {
                var registry = serverPlayer.level().registryAccess().lookupOrThrow(Registries.ITEM);
                var holders = registry.get(tag);
                if (holders.isPresent()) {
                    for (var holder : holders.get()) {
                        result.add(new net.minecraft.world.item.ItemStack(holder.value()));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[RecipeBook NMS] Error resolving tag: " + e.getMessage());
            }
        } else if (slotDisplay instanceof SlotDisplay.Composite compositeDisplay) {
            for (SlotDisplay nested : compositeDisplay.contents()) {
                result.addAll(resolvePossibleItems(nested, serverPlayer));
            }
        }
        
        return result;
    }
    
    private void updateWorkbench(Inventory inv, Player player) {
        ItemStack[] grid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            grid[i] = inv.getItem(i + 1);
        }
        
        ItemStack result = simulator.tryMatch(player, grid);
        inv.setItem(0, result);
    }
}

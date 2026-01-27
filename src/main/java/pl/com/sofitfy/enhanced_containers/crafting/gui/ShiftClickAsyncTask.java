package pl.com.sofitfy.enhanced_containers.crafting.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.com.sofitfy.enhanced_containers.crafting.nms.NmsCraftingEngine;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.CraftingRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeMatcher;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipePattern;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.VanillaRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.PatternMatchResult;

/**
 * Asynchroniczny task do obsługi shift+click craftingu.
 * 
 * Kopiuje grid na początek, potem async robimy obliczenia craftingu (bez zmian inventory).
 * Na koniec schedulujemy update inventorza na main thread (safe!).
 * 
 * Dlaczego async?
 * - Shift+click 1000 itemów nie zablokuje TPS serwera
 * - Crafting logika (matching + obliczenia) jest pure i thread-safe
 * - Update inventory robimy na main threadzie (safe dla Bukkita)
 */
public class ShiftClickAsyncTask extends BukkitRunnable {
    
    private final Player player;
    private final CraftingGUI gui;
    private final JavaPlugin plugin;
    private final CraftingGUIListener listener;
    private final Object foundRecipe; // CraftingRecipe lub VanillaRecipe
    private final ItemStack craftOutput;
    
    // Wyniki craftingu do apliki na main threadzie
    private ItemStack[] finalGridItems;
    private int craftedCount = 0;
    
    // Remainder items z każdego craftu (do aplikacji na main threadzie)
    private final List<ItemStack[]> remainderItemsPerCraft = new ArrayList<>();
    
    // Flaga - czy pokazać komunikat "Inventory full"
    private boolean inventoryFullMessageNeeded = false;
    
    // Snapshot inventory gracza do symulacji (deep copy)
    private final ItemStack[] playerInventorySnapshot;
    
    /**
     * Tworzy async task do craftingu.
     * 
     * @param player gracz który robi shift+click
     * @param gui okno craftingu
     * @param plugin main plugin instance (JavaPlugin aby móc invokeAsync)
     * @param listener CraftingGUIListener instance do updateOutput
     * @param foundRecipe receptura do craftowania (CraftingRecipe lub VanillaRecipe)
     * @param craftOutput item co się craftuje
     * @param playerInventorySnapshot snapshot inventory gracza (deep copy)
     */
    public ShiftClickAsyncTask(
        @NotNull Player player,
        @NotNull CraftingGUI gui,
        @NotNull JavaPlugin plugin,
        @NotNull CraftingGUIListener listener,
        @Nullable Object foundRecipe,
        @Nullable ItemStack craftOutput,
        @NotNull ItemStack[] playerInventorySnapshot
    ) {
        this.player = player;
        this.gui = gui;
        this.plugin = plugin;
        this.listener = listener;
        this.foundRecipe = foundRecipe;
        this.craftOutput = craftOutput;
        this.playerInventorySnapshot = playerInventorySnapshot;
    }

    /**
     * Główna pętla async craftingu.
     * Robimy crafty dopóki receptura pasuje, max 10000 iteracji (safety limit).
     */
    @Override
    public void run() {
        if (player == null || !player.isOnline() || gui == null) {
            return;
        }

        if (foundRecipe == null || craftOutput == null || craftOutput.getType().isAir()) {
            return;
        }

        // Kopiujemy grid na początek (snapshot)
        ItemStack[] gridItems = CraftingGUIListener.extractGridItems(gui);
        
        // Bezpieczny limit - żeby nie croaować nieskończenie
        final int MAX_CRAFTS = 10000;
        
        // PĘTLA CRAFTINGU - robimy async bez lockowania serwera
        while (craftedCount < MAX_CRAFTS) {
            // Sprawdź czy jest miejsce w EQ PRZED craftingiem (używając snapshotu)
            // Symuluje vanilla workbench - jeśli EQ pełne, przerywa shift+click
            // Metoda simulateAdd modyfikuje snapshot, więc dokładnie śledzi zapełnienie
            if (!CraftingGUIListener.simulateAdd(playerInventorySnapshot, craftOutput)) {
                // Brak miejsca - przerwij pętlę i wyślij komunikat (NIE konsumujemy składników)
                inventoryFullMessageNeeded = true;
                break;
            }
            
            // Sprawdzić czy ta SAMA receptura pasuje
            boolean stillMatches = false;

            // Custom receptura
            if (foundRecipe instanceof CraftingRecipe) {
                CraftingRecipe recipe = (CraftingRecipe) foundRecipe;
                RecipePattern pattern = recipe.getRecipePattern();
                
                if (pattern != null && !pattern.isEmpty()) {
                    PatternMatchResult matchResult = RecipeMatcher.findPatternMatchExactly(gridItems, pattern);
                    if (matchResult.isMatched()) {
                        stillMatches = true;

                        // Dodaj output do listy (aplika sie na main threadzie)
                        ItemStack outputCopy = craftOutput.clone();
                        outputCopy.setAmount(craftOutput.getAmount());
                        
                        // Zapisz grid PRZED usunięciem składników (do remainder items)
                        // WAŻNE: Musi być DEEP COPY bo removeItemsFromMatch modyfikuje ItemStack'i in-place
                        ItemStack[] gridBeforeCrafting = new ItemStack[gridItems.length];
                        for (int i = 0; i < gridItems.length; i++) {
                            gridBeforeCrafting[i] = gridItems[i] != null ? gridItems[i].clone() : null;
                        }
                        
                        // Usuń składniki z kopii gridu
                        RecipeMatcher.removeItemsFromMatch(gridItems, matchResult);
                        
                        // Zbierz remainder items dla custom receptury
                        // Sprawdzamy każdy item indywidualnie (MILK_BUCKET → BUCKET, itp.)
                        remainderItemsPerCraft.add(gridBeforeCrafting);
                        
                        craftedCount++;
                    }
                }
            }
            // Vanilla receptura
            else if (foundRecipe instanceof VanillaRecipe) {
                VanillaRecipe recipe = (VanillaRecipe) foundRecipe;

                if (!recipe.isShapeless()) {
                    // Shaped vanilla
                    ItemStack[][] pattern = recipe.getPattern();
                    if (pattern != null) {
                        PatternMatchResult matchResult = RecipeMatcher.findPatternMatchVanilla(gridItems, pattern);
                        if (matchResult.isMatched()) {
                            stillMatches = true;

                            ItemStack outputCopy = craftOutput.clone();
                            outputCopy.setAmount(craftOutput.getAmount());
                            
                            // Zapisz grid PRZED usunięciem składników (do remainder items)
                            // WAŻNE: Musi być DEEP COPY bo removeItemsFromMatch modyfikuje ItemStack'i in-place
                            ItemStack[] gridBeforeCrafting = new ItemStack[gridItems.length];
                            for (int i = 0; i < gridItems.length; i++) {
                                gridBeforeCrafting[i] = gridItems[i] != null ? gridItems[i].clone() : null;
                            }
                            
                            RecipeMatcher.removeItemsFromMatch(gridItems, matchResult);
                            
                            // Pobierz remainder items z NMS dla vanilla receptury
                            ItemStack[] grid3x3 = CraftingGUIListener.extract3x3Grid(gridBeforeCrafting);
                            if (grid3x3 != null) {
                                NmsCraftingEngine engine = new NmsCraftingEngine();
                                ItemStack[] remainingItems = engine.getRemainingItems(grid3x3, player.getWorld());
                                if (remainingItems != null && remainingItems.length == 9) {
                                    remainderItemsPerCraft.add(remainingItems);
                                } else {
                                    remainderItemsPerCraft.add(null); // Brak remainder items dla tego craftu
                                }
                            } else {
                                remainderItemsPerCraft.add(null);
                            }
                            
                            craftedCount++;
                        }
                    }
                } else {
                    // Shapeless vanilla
                    List<ItemStack> ingredients = recipe.getShapelessIngredients();
                    if (ingredients != null && !ingredients.isEmpty()) {
                        if (RecipeMatcher.matchesShapelessVanilla(gridItems, ingredients)) {
                            stillMatches = true;

                            ItemStack outputCopy = craftOutput.clone();
                            outputCopy.setAmount(craftOutput.getAmount());
                            
                            // Zapisz grid PRZED usunięciem składników (do remainder items)
                            // WAŻNE: Musi być DEEP COPY bo removeShapelessItems modyfikuje ItemStack'i in-place
                            ItemStack[] gridBeforeCrafting = new ItemStack[gridItems.length];
                            for (int i = 0; i < gridItems.length; i++) {
                                gridBeforeCrafting[i] = gridItems[i] != null ? gridItems[i].clone() : null;
                            }
                            
                            RecipeMatcher.removeShapelessItems(gridItems, ingredients);
                            
                            // Pobierz remainder items z NMS dla vanilla receptury
                            ItemStack[] grid3x3 = CraftingGUIListener.extract3x3Grid(gridBeforeCrafting);
                            if (grid3x3 != null) {
                                NmsCraftingEngine engine = new NmsCraftingEngine();
                                ItemStack[] remainingItems = engine.getRemainingItems(grid3x3, player.getWorld());
                                if (remainingItems != null && remainingItems.length == 9) {
                                    remainderItemsPerCraft.add(remainingItems);
                                } else {
                                    remainderItemsPerCraft.add(null); // Brak remainder items dla tego craftu
                                }
                            } else {
                                remainderItemsPerCraft.add(null);
                            }
                            
                            craftedCount++;
                        }
                    }
                }
            }

            // Jeśli receptura się zmienia - stop (nawet w async)
            if (!stillMatches) {
                break;
            }
        }

        // Zapisz finalny grid state
        this.finalGridItems = gridItems;

        // Teraz scheduluj update na main threadzie (safe dla Bukkita)
        scheduleMainThreadUpdate();
    }

    /**
     * Scheduluje update na main threadzie.
     * Aplika rezultaty craftingu do inventory gracza i gridu.
     */
    private void scheduleMainThreadUpdate() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player == null || !player.isOnline() || gui == null) {
                return;
            }

            // Aplika zmieniony grid na gui
            if (finalGridItems != null) {
                CraftingGUIListener.updateGridSlots(gui, finalGridItems);
            }
            
            // Aplikuj remainder items z OSTATNIEGO craftu z powrotem do gridu
            // W shift-click, tylko ostatni craft może zostawić remainder items w gridzie,
            // bo poprzednie crafty "konsumują" remainder items w kolejnych craftach.
            if (craftedCount > 0 && !remainderItemsPerCraft.isEmpty()) {
                ItemStack[] gridBeforeLastCraft = remainderItemsPerCraft.get(remainderItemsPerCraft.size() - 1);
                
                if (gridBeforeLastCraft != null && finalGridItems != null) {
                    // Dla custom receptur: gridBeforeLastCraft to pełny 4x4 grid (16 elementów)
                    // Dla vanilla receptur: gridBeforeLastCraft to 3x3 grid (9 elementów)
                    
                    if (gridBeforeLastCraft.length == 16) {
                        // Custom receptura - użyj nowej metody applyRemainingItemsToGrid
                        // która sprawdza każdy item indywidualnie (MILK_BUCKET → BUCKET, itp.)
                        listener.applyRemainingItemsToGridPublic(gridBeforeLastCraft, finalGridItems, player);
                        CraftingGUIListener.updateGridSlots(gui, finalGridItems);
                        
                    } else if (gridBeforeLastCraft.length == 9) {
                        // Vanilla receptura - stara logika z 3x3 → 4x4
                        // Mapowanie 3x3 → 4x4 (zakładamy że receptura jest w lewym górnym rogu)
                        for (int r = 0; r < 3; r++) {
                            for (int c = 0; c < 3; c++) {
                                int index3x3 = r * 3 + c;
                                ItemStack remainingItem = gridBeforeLastCraft[index3x3];
                                
                                if (remainingItem != null && !remainingItem.getType().isAir()) {
                                    int index4x4 = r * 4 + c;
                                    if (index4x4 < finalGridItems.length) {
                                        // Dodaj remainder item do gridu (jeśli slot pusty) lub do EQ (jeśli zajęty)
                                        if (finalGridItems[index4x4] == null || finalGridItems[index4x4].getType().isAir()) {
                                            finalGridItems[index4x4] = remainingItem.clone();
                                        } else {
                                            // Slot zajęty - dodaj do EQ gracza
                                            CraftingGUIListener.addItemToPlayerOrDrop(player, remainingItem.clone());
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Zaktualizuj grid w GUI po dodaniu remainder items
                        CraftingGUIListener.updateGridSlots(gui, finalGridItems);
                    }
                }
            }
            
            // Jeśli EQ było pełne podczas craftingu - wyślij komunikat
            if (inventoryFullMessageNeeded) {
                player.sendMessage("§cInventory full! Cannot craft more items.");
            }

            // Dodaj wszystkie crafty do inventory gracza
            // Async pętla już sprawdziła że zmieszczą się wszystkie (z craftedCount)
            for (int i = 0; i < craftedCount; i++) {
                ItemStack outputCopy = craftOutput.clone();
                outputCopy.setAmount(craftOutput.getAmount());
                CraftingGUIListener.addItemToPlayerOrDrop(player, outputCopy);
            }

            // Zaktualizuj output slot do kolejnej możliwej receptury
            listener.updateOutput(player, gui);
        });
    }

    /**
     * Zwraca ile itemów się craftowało.
     */
    public int getCraftedCount() {
        return craftedCount;
    }
}

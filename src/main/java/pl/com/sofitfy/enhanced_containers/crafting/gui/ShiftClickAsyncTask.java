package pl.com.sofitfy.enhanced_containers.crafting.gui;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    
    /**
     * Tworzy async task do craftingu.
     * 
     * @param player gracz który robi shift+click
     * @param gui okno craftingu
     * @param plugin main plugin instance (JavaPlugin aby móc invokeAsync)
     * @param listener CraftingGUIListener instance do updateOutput
     * @param foundRecipe receptura do craftowania (CraftingRecipe lub VanillaRecipe)
     * @param craftOutput item co się craftuje
     */
    public ShiftClickAsyncTask(
        @NotNull Player player,
        @NotNull CraftingGUI gui,
        @NotNull JavaPlugin plugin,
        @NotNull CraftingGUIListener listener,
        @Nullable Object foundRecipe,
        @Nullable ItemStack craftOutput
    ) {
        this.player = player;
        this.gui = gui;
        this.plugin = plugin;
        this.listener = listener;
        this.foundRecipe = foundRecipe;
        this.craftOutput = craftOutput;
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
                        
                        // Usuń składniki z kopii gridu
                        RecipeMatcher.removeItemsFromMatch(gridItems, matchResult);
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
                            
                            RecipeMatcher.removeItemsFromMatch(gridItems, matchResult);
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
                            
                            RecipeMatcher.removeShapelessItems(gridItems, ingredients);
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

            // Dodaj wszystkie crafty do inventory gracza
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

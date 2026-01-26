package pl.com.sofitfy.enhanced_containers.crafting.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeDraft;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.InputItemStackNormalizer;

/**
 * RecipeEditorGUI - edytor receptur dla adminów
 *
 * Pozwala adminowi tworzyć i edytować receptury craftingu
 * Layout: 54 sloty (6 rzędów × 9 kolumn)
 *
 * Rząd 0: [Slot 0-3]    = grid 4x4 input (rząd 1)
 * Rząd 1: [Slot 9-12]   = grid 4x4 input (rząd 2)
 * Rząd 2: [Slot 18-21]  = grid 4x4 input (rząd 3)
 * Rząd 3: [Slot 27-30]  = grid 4x4 input (rząd 4)
 *
 * Rząd 2: [Slot 24]     = OUTPUT (obok gridu - zawsze slot 24)
 *
 * Rząd 4-5: Puste sloty
 * Rząd 6: Gracz widzi swój inventory
 *
 * Admin umieszcza:
 * - Input przedmioty w slotach: 0,1,2,3 / 9,10,11,12 / 18,19,20,21 / 27,28,29,30 (grid 4x4)
 * - Output przedmiot w slocie 24 (ZAWSZE!)
 * - Po Escape: validacja i zapis do config.yml
 */
public class RecipeEditorGUI {
    private final Player player;
    private final Inventory inventory;
    private final RecipeDraft draft;

    // Rozmiar inventoria - ZAWSZE 54 sloty (9 kolumn × 6 rzędów)
    // Nie można tego zmieniać!
    public static final int INVENTORY_SIZE = 54;

    // Indeksy slotów dla gridu inputu (4x4 = 16 slotów)
    // Każdy rząd ma 9 slotów w Minecrafcie!
    // Rząd 0: 0,1,2,3
    // Rząd 1: 9,10,11,12
    // Rząd 2: 18,19,20,21
    // Rząd 3: 27,28,29,30
    public static final int[] GRID_SLOTS = {
        0, 1, 2, 3,
        9, 10, 11, 12,
        18, 19, 20, 21,
        27, 28, 29, 30
    };

    // Slot na output - ZAWSZE slot 24!
    public static final int OUTPUT_SLOT = 24;

    /**
     * Konstruktor - stwórz edytor dla nowej receptury
     */
    public RecipeEditorGUI(Player player, RecipeDraft draft) {
        this.player = player;
        this.draft = draft;

        // Stwórz inventory 54 sloty (6 rzędów × 9 kolumn)
        // Tytuł będzie zawierać \uE001 które wyświetli ex_crafting.png z resource packa
        String editorTitle = GUITitleFormatter.getEditorTitle();
        this.inventory = Bukkit.createInventory(
            null,
            54,
            Component.text(editorTitle)
        );

        // Załaduj dane z draftu do GUI
        loadDraftToGUI();
    }

    /**
     * Załaduj dane z RecipeDraft do GUI
     * (jeśli edytujemy istniejącą recepturę)
     */
    private void loadDraftToGUI() {
        // Załaduj input pattern do slotów
        ItemStack[][] inputPattern = draft.getInputPattern();
        if (inputPattern != null) {
            int slotIndex = 0;
            for (ItemStack[] row : inputPattern) {
                if (row != null) {
                    for (ItemStack item : row) {
                        if (slotIndex < GRID_SLOTS.length) {
                            // W inputach ilość nie jest częścią receptury.
                            // Nawet jeśli w configu ktoś miał stare dane z amount>1, to w GUI pokazujemy 1.
                            inventory.setItem(GRID_SLOTS[slotIndex], InputItemStackNormalizer.toSingleOrNull(item));
                            slotIndex++;
                        }
                    }
                }
            }
        }

        // Załaduj output
        ItemStack output = draft.getOutput();
        if (output != null) {
            inventory.setItem(OUTPUT_SLOT, output);
        }
    }

    /**
     * Otwórz edytor dla gracza
     */
    public void open() {
        player.openInventory(inventory);
    }

    /**
     * Pobierz inventory edytora
     */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Pobierz gracza
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Pobierz draft (dane edycji)
     */
    public RecipeDraft getDraft() {
        return draft;
    }

    /**
     * Sprawdzenie czy slot jest częścią gridu inputu
     *
     * @param slot - numer slotu
     * @return true jeśli slot to część gridu
     */
    public static boolean isGridSlot(int slot) {
        for (int gridSlot : GRID_SLOTS) {
            if (slot == gridSlot) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sprawdzenie czy slot jest slotem outputu
     *
     * @param slot - numer slotu
     * @return true jeśli slot to output
     */
    public static boolean isOutputSlot(int slot) {
        return slot == OUTPUT_SLOT;
    }

    /**
     * Zaktualizuj draft z zawartością GUI
     * (przed walidacją)
     */
    public void updateDraftFromGUI() {
        // Pobierz itemy z gridu i ustaw w draftu
        ItemStack[][] inputPattern = new ItemStack[4][4];

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                int slotIndex = row * 4 + col;
                int slot = GRID_SLOTS[slotIndex];
                ItemStack item = inventory.getItem(slot);

                // Jeśli slot jest pusty lub powietrze - ustaw null
                if (item == null || item.getType().isAir()) {
                    inputPattern[row][col] = null;
                } else {
                    // W inputach receptury ilość NIE jest potrzebna.
                    // Wymuszamy amount=1, żeby w configu nie było "kosztów" typu 64 w jednym slocie.
                    inputPattern[row][col] = InputItemStackNormalizer.toSingleOrNull(item);
                }
            }
        }

        draft.setInputPattern(inputPattern);

        // Pobierz output
        ItemStack outputItem = inventory.getItem(OUTPUT_SLOT);
        if (outputItem != null && !outputItem.getType().isAir()) {
            draft.setOutput(outputItem.clone());
        } else {
            draft.setOutput(null);
        }

        // Ustaw nazwę z display name outputu (jeśli istnieje)
        if (draft.getOutput() != null && draft.getOutput().getItemMeta() != null) {
            Component displayName = draft.getOutput().getItemMeta().displayName();
            if (displayName != null) {
                draft.setName(displayName.toString());
            }
        }
    }

    /**
     * Czy GUI jest puste (anulacja)
     * = brak input i brak output
     *
     * @return true jeśli nic nie ma
     */
    public boolean isEmpty() {
        // Sprawdź czy jest coś w gridzie
        for (int slot : GRID_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                return false; // Coś tam jest
            }
        }

        // Sprawdź czy jest output
        ItemStack outputItem = inventory.getItem(OUTPUT_SLOT);
        if (outputItem != null && !outputItem.getType().isAir()) {
            return false; // Output jest
        }

        return true; // Nic tam nie ma
    }

    /**
     * Czy GUI ma błędy (brakuje input lub output)
     * = musi być co najmniej 1 input i 1 output
     *
     * @return true jeśli są błędy
     */
    public boolean hasErrors() {
        // Sprawdzenie czy jest co najmniej jeden item w gridzie
        boolean hasInput = false;
        for (int slot : GRID_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                hasInput = true;
                break;
            }
        }

        // Sprawdzenie czy jest output
        ItemStack outputItem = inventory.getItem(OUTPUT_SLOT);
        boolean hasOutput = outputItem != null && !outputItem.getType().isAir();

        // Błąd jeśli brakuje input LUB output
        return !hasInput || !hasOutput;
    }

    /**
     * Pobierz wiadomość błędu
     *
     * @return tekst błędu
     */
    public String getErrorMessage() {
        // Sprawdzenie czy jest input
        boolean hasInput = false;
        for (int slot : GRID_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                hasInput = true;
                break;
            }
        }

        // Sprawdzenie czy jest output
        ItemStack outputItem = inventory.getItem(OUTPUT_SLOT);
        boolean hasOutput = outputItem != null && !outputItem.getType().isAir();

        // Jeśli jest input i output, to nie ma błędu.
        // Zwracamy pusty tekst, bo komunikat błędu nie jest potrzebny.
        if (hasInput && hasOutput) {
            return "";
        }

        if (!hasInput && !hasOutput) {
            return "§cMust have at least one input item and one output item!";
        } else if (!hasInput) {
            return "§cMust have at least one input item!";
        } else if (!hasOutput) {
            return "§cMust have at least one output item!";
        }

        // Fallback (nie powinno się zdarzyć)
        return "§cUnknown error!";
    }
}


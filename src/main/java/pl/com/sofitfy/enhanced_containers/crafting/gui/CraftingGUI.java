package pl.com.sofitfy.enhanced_containers.crafting.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

/**
 * CraftingGUI - interfejs craftowania dla gracza
 *
 * Gracz umieszcza przedmioty w gridu 4x4 i klika output aby craftować
 * Layout: 54 sloty (6 rzędów × 9 kolumn)
 *
 * Rząd 0: [Slot 0-3]    = grid 4x4 input (rząd 1)
 * Rząd 1: [Slot 9-12]   = grid 4x4 input (rząd 2)
 * Rząd 2: [Slot 18-21]  = grid 4x4 input (rząd 3) + [Slot 24] = OUTPUT
 * Rząd 3: [Slot 27-30]  = grid 4x4 input (rząd 4)
 *
 * Slot 24 to zawsze OUTPUT gdzie gracz klika aby craftować
 */
public class CraftingGUI {
    private final UUID playerId;
    private final String title;
    private final int size;
    private Inventory inventory;

    // Rozmiar inventoria - ZAWSZE 54 sloty (9 kolumn × 6 rzędów)
    // Nie można tego zmieniać!
    public static final int INVENTORY_SIZE = 54;

    // Sloty inputu - grid 4x4
    public static final int[] GRID_SLOTS = {
        0, 1, 2, 3,
        9, 10, 11, 12,
        18, 19, 20, 21,
        27, 28, 29, 30
    };

    // Slot na output - ZAWSZE 24!
    public static final int OUTPUT_SLOT = 24;

    // Sloty na "podpowiedzi craftingu" (ostatni rząd).
    // UWAGA: sloty liczymy 0-53. Slot 49 zostawiamy na ikonę informacji.
    public static final int[] SUGGESTION_SLOTS = {
        45, 46, 47, 48,
        50, 51, 52, 53
    };

    // Ikona w środku ostatniego rzędu (np. w przyszłości pod menu/paginację)
    public static final int SUGGESTIONS_INFO_SLOT = 49;

    public CraftingGUI(Player player, String title) {
        this.playerId = player.getUniqueId();
        this.title = title;
        this.size = INVENTORY_SIZE;  // Zawsze 54, bez zmian!
        createInventory();
    }

    private void createInventory() {
        // Tworzy inventory bez deprecated metody
        inventory = Bukkit.createInventory(null, size, Component.text(title));
        setupLayout();
    }

    /**
     * Ustawia układ GUI - umieszcza output slot, reszta slotów pozostaje pusta
     * Brak szklanych paneli w tle - czysty wygląd bez domyślnego tła
     */
    private void setupLayout() {
        // Wypełnij wszystkie sloty pustkami (null) - bez szklanych paneli tła
        for (int i = 0; i < size; i++) {
            inventory.setItem(i, null);
        }

        // Output slot zostaje pusty.
        // Chcemy odwzorować crafting table: wynik pojawia się tylko gdy receptura pasuje.
        inventory.setItem(OUTPUT_SLOT, null);

        // Ikona "recipe hints" w środku ostatniego rzędu.
        // Teksty w grze po angielsku.
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§6Recipe suggestions"));
            meta.lore(List.of(Component.text("§7Shows craftable outputs")));
            info.setItemMeta(meta);
        }
        inventory.setItem(SUGGESTIONS_INFO_SLOT, info);
    }

    /**
     * Sprawdź czy slot to jeden z slotów podpowiedzi (same ikonki).
     */
    public boolean isSuggestionSlot(int slot) {
        for (int s : SUGGESTION_SLOTS) {
            if (s == slot) {
                return true;
            }
        }
        return false;
    }

    /**
     * Slot w środku ostatniego rzędu - informacja.
     */
    public boolean isSuggestionsInfoSlot(int slot) {
        return slot == SUGGESTIONS_INFO_SLOT;
    }

    /**
     * Wyczyść ikonki podpowiedzi.
     */
    public void clearSuggestions() {
        for (int slot : SUGGESTION_SLOTS) {
            inventory.setItem(slot, null);
        }
    }

    /**
     * Ustaw ikonki podpowiedzi w ostatnim rzędzie.
     * items są tylko do wyświetlania - listener blokuje interakcje z tymi slotami.
     */
    public void setSuggestions(List<ItemStack> items) {
        clearSuggestions();
        if (items == null || items.isEmpty()) {
            return;
        }

        int i = 0;
        for (int slot : SUGGESTION_SLOTS) {
            if (i >= items.size()) {
                break;
            }
            ItemStack item = items.get(i);
            if (item == null || item.getType().isAir()) {
                inventory.setItem(slot, null);
            } else {
                ItemStack icon = item.clone();
                icon.setAmount(1); // tylko podgląd
                inventory.setItem(slot, icon);
            }
            i++;
        }
    }

    /**
     * Ustawia item w input slocie
     */
    public void setInputItem(int slot, ItemStack item) {
        // Sprawdź czy slot należy do gridu
        boolean isValidGridSlot = false;
        for (int gridSlot : GRID_SLOTS) {
            if (gridSlot == slot) {
                isValidGridSlot = true;
                break;
            }
        }

        if (isValidGridSlot) {
            inventory.setItem(slot, item != null ? item.clone() : null);
        }
    }

    /**
     * Pobierz item z input slotu
     */
    public ItemStack getInputItem(int slot) {
        ItemStack item = inventory.getItem(slot);
        return item != null && !item.getType().isAir() ? item.clone() : null;
    }

    /**
     * Ustawia item w output slocie (slot 24)
     */
    public void setOutputItem(ItemStack item) {
        inventory.setItem(OUTPUT_SLOT, item != null ? item.clone() : null);
    }

    /**
     * Pobierz item z output slotu (slot 24)
     */
    public ItemStack getOutputItem() {
        ItemStack item = inventory.getItem(OUTPUT_SLOT);
        return item != null && !item.getType().isAir() ? item.clone() : null;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Wyczyść wszystkie sloty inputu (ustaw na null - puste)
     * Bez szklanych paneli w tle
     */
    public void clearInput() {
        // Wyczyść wszystkie sloty gridu na null (brak szklanych paneli)
        for (int slot : GRID_SLOTS) {
            inventory.setItem(slot, null);
        }
    }

    /**
     * Sprawdź czy slot należy do gridu inputu
     * Grid slots: 0,1,2,3 / 9,10,11,12 / 18,19,20,21 / 27,28,29,30
     */
    public boolean isInputSlot(int slot) {
        for (int gridSlot : GRID_SLOTS) {
            if (gridSlot == slot) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sprawdź czy slot to output (zawsze slot 24)
     */
    public boolean isOutputSlot(int slot) {
        return slot == OUTPUT_SLOT;
    }

    /**
     * Sprawdź czy slot jest widoczny dla gracza
     * Widoczne sloty to: input grid (16) + output (1)
     * Pozostałe sloty (37) są ukryte za customowym fontem i niedostępne
     */
    public boolean isVisibleSlot(int slot) {
        return isInputSlot(slot) || isOutputSlot(slot) || isSuggestionSlot(slot) || isSuggestionsInfoSlot(slot);
    }
}


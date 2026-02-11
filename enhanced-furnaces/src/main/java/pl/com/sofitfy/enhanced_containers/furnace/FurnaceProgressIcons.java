package pl.com.sofitfy.enhanced_containers.furnace;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * FurnaceProgressIcons - zarządza ikonkami postępu pieca z CustomModelData
 * 
 * Używa CustomModelData do zmiany tekstury itemów (jak w vanilla):
 * - Ogień (BLAZE_POWDER): CustomModelData 100-113 (14 stanów: empty + 13 poziomów)
 * - Strzałka (ARROW): CustomModelData 200-222 (23 stanów: empty + 22 poziomów)
 * 
 * Resource pack musi zawierać odpowiednie override'y w:
 * - assets/minecraft/models/item/blaze_powder.json
 * - assets/minecraft/models/item/arrow.json
 * 
 * Tekstury są w: assets/enhanced_containers/textures/gui/furnace/
 */
public class FurnaceProgressIcons {
    
    // Bazowe materiały dla ikonek
    public static final Material FIRE_MATERIAL = Material.BLAZE_POWDER;
    public static final Material ARROW_MATERIAL = Material.ARROW;
    public static final Material ARROW_RIGHT_MATERIAL = Material.FEATHER;
    
    // Maksymalne wartości (klatki animacji jak w vanilla)
    public static final int MAX_FIRE_FRAMES = 13;   // Vanilla ogień ma 13 poziomów
    public static final int MAX_ARROW_FRAMES = 22;  // Vanilla strzałka ma 22 piksele
    
    // CustomModelData bazowe wartości
    // Fire: 100 = empty, 101-113 = poziomy 1-13
    public static final int FIRE_CMD_BASE = 100;
    // Arrow: 200 = empty, 201-222 = poziomy 1-22
    public static final int ARROW_CMD_BASE = 200;
    
    /**
     * Tworzy ikonkę ognia dla wskaźnika paliwa
     * Używa CustomModelData do wyświetlenia odpowiedniego poziomu wypełnienia
     * 
     * @param burnTime pozostały czas palenia (w tickach)
     * @param maxBurnTime maksymalny czas palenia (w tickach)
     * @return ItemStack z ikonką ognia (nigdy null - zawsze pokazuje stan)
     */
    public static ItemStack createFireIcon(int burnTime, int maxBurnTime) {
        // Brak paliwa - zwróć pustą ikonkę (kontur)
        if (burnTime <= 0 || maxBurnTime <= 0) {
            return createEmptyFireIcon();
        }
        
        ItemStack icon = new ItemStack(FIRE_MATERIAL);
        ItemMeta meta = icon.getItemMeta();
        
        // Oblicz poziom wypełnienia (1-13)
        int level = Math.max(1, Math.min(MAX_FIRE_FRAMES, 
            (int) Math.ceil(MAX_FIRE_FRAMES * (float) burnTime / maxBurnTime)));
        
        // Ustaw CustomModelData (101-113 dla poziomów 1-13)
        meta.setCustomModelData(FIRE_CMD_BASE + level);
        
        // Nazwa i opis po angielsku (wyświetlane w grze)
        int percent = Math.round(100f * burnTime / maxBurnTime);
        int seconds = burnTime / 20;
        
        meta.displayName(Component.text("Fuel")
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            Component.text("Remaining: " + percent + "% (" + seconds + "s)")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        icon.setItemMeta(meta);
        return icon;
    }
    
    /**
     * Tworzy ikonkę strzałki dla wskaźnika postępu przepalania
     * Używa CustomModelData do wyświetlenia odpowiedniego poziomu wypełnienia
     * 
     * @param cookTime aktualny czas przepalania (w tickach)
     * @param maxCookTime maksymalny czas przepalania (w tickach)
     * @return ItemStack z ikonką strzałki (nigdy null - zawsze pokazuje stan)
     */
    public static ItemStack createArrowIcon(int cookTime, int maxCookTime) {
        // Zachowane dla kompatybilności: domyślnie renderuj lewą połówkę (ARROW)
        return createArrowLeftIcon(cookTime, maxCookTime);
    }

    /**
     * Lewa połówka strzałki (item: ARROW) - wersja z precyzyjnym postępem (0.0 - 1.0)
     */
    public static ItemStack createArrowLeftIcon(double progress) {
        if (progress <= 0.0) {
            return createEmptyArrowIcon();
        }

        ItemStack icon = new ItemStack(ARROW_MATERIAL);
        ItemMeta meta = icon.getItemMeta();

        // Oblicz poziom wypełnienia (1-22)
        int level = Math.max(1, Math.min(MAX_ARROW_FRAMES, 
            (int) Math.ceil(MAX_ARROW_FRAMES * progress)));

        meta.setCustomModelData(ARROW_CMD_BASE + level);

        int percent = (int)Math.round(progress * 100.0);
        meta.displayName(Component.text("Progress")
            .color(NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false));

        meta.lore(List.of(
            Component.text("Smelting: " + percent + "%")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
        ));

        icon.setItemMeta(meta);
        return icon;
    }

    /**
     * Stara metoda dla kompatybilności
     */
    public static ItemStack createArrowLeftIcon(int cookTime, int maxCookTime) {
        if (maxCookTime == 0) return createEmptyArrowIcon();
        return createArrowLeftIcon((double) cookTime / maxCookTime);
    }

    /**
     * Prawa połówka strzałki (item: FEATHER) - wersja z precyzyjnym postępem (0.0 - 1.0)
     */
    public static ItemStack createArrowRightIcon(double progress) {
        if (progress <= 0.0) {
            return createEmptyArrowRightIcon();
        }

        ItemStack icon = new ItemStack(ARROW_RIGHT_MATERIAL);
        ItemMeta meta = icon.getItemMeta();

        // Oblicz poziom wypełnienia (1-22)
        int level = Math.max(1, Math.min(MAX_ARROW_FRAMES,
            (int) Math.ceil(MAX_ARROW_FRAMES * progress)));

        meta.setCustomModelData(ARROW_CMD_BASE + level);

        int percent = (int)Math.round(progress * 100.0);
        meta.displayName(Component.text("Progress")
            .color(NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false));

        meta.lore(List.of(
            Component.text("Smelting: " + percent + "%")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
        ));

        icon.setItemMeta(meta);
        return icon;
    }

    /**
     * Stara metoda dla kompatybilności
     */
    public static ItemStack createArrowRightIcon(int cookTime, int maxCookTime) {
        if (maxCookTime == 0) return createEmptyArrowRightIcon();
        return createArrowRightIcon((double) cookTime / maxCookTime);
    }
    
    /**
     * Tworzy pustą ikonkę ognia (kontur bez wypełnienia)
     * CustomModelData 100 = fire_empty - pokazuje szary kontur płomienia
     * 
     * @return ItemStack z pustą ikonką ognia
     */
    public static ItemStack createEmptyFireIcon() {
        ItemStack icon = new ItemStack(FIRE_MATERIAL);
        ItemMeta meta = icon.getItemMeta();
        
        // CustomModelData 100 = pusta ikonka (kontur)
        meta.setCustomModelData(FIRE_CMD_BASE);
        
        meta.displayName(Component.text("No Fuel")
            .color(NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, false));
        
        icon.setItemMeta(meta);
        return icon;
    }
    
    /**
     * Tworzy pustą ikonkę strzałki (kontur bez wypełnienia)
     * CustomModelData 200 = arrow_empty - pokazuje szary kontur strzałki
     * 
     * @return ItemStack z pustą ikonką strzałki
     */
    public static ItemStack createEmptyArrowIcon() {
        ItemStack icon = new ItemStack(ARROW_MATERIAL);
        ItemMeta meta = icon.getItemMeta();
        
        // CustomModelData 200 = pusta ikonka (kontur)
        meta.setCustomModelData(ARROW_CMD_BASE);
        
        meta.displayName(Component.text("Not Smelting")
            .color(NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, false));
        
        icon.setItemMeta(meta);
        return icon;
    }

    public static ItemStack createEmptyArrowRightIcon() {
        ItemStack icon = new ItemStack(ARROW_RIGHT_MATERIAL);
        ItemMeta meta = icon.getItemMeta();

        meta.setCustomModelData(ARROW_CMD_BASE);

        meta.displayName(Component.text("Not Smelting")
            .color(NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, false));

        icon.setItemMeta(meta);
        return icon;
    }
    
    /**
     * Oblicza poziom CustomModelData dla ognia na podstawie procentu wypełnienia
     * Przydatne do testowania i debugowania
     * 
     * @param percent procent wypełnienia (0-100)
     * @return wartość CustomModelData (100-113)
     */
    public static int getFireCustomModelData(int percent) {
        if (percent <= 0) {
            return FIRE_CMD_BASE; // empty (100)
        }
        int level = Math.max(1, Math.min(MAX_FIRE_FRAMES, 
            (int) Math.ceil(MAX_FIRE_FRAMES * percent / 100.0)));
        return FIRE_CMD_BASE + level;
    }
    
    /**
     * Oblicza poziom CustomModelData dla strzałki na podstawie procentu wypełnienia
     * Przydatne do testowania i debugowania
     * 
     * @param percent procent wypełnienia (0-100)
     * @return wartość CustomModelData (200-222)
     */
    public static int getArrowCustomModelData(int percent) {
        if (percent <= 0) {
            return ARROW_CMD_BASE; // empty (200)
        }
        int level = Math.max(1, Math.min(MAX_ARROW_FRAMES, 
            (int) Math.ceil(MAX_ARROW_FRAMES * percent / 100.0)));
        return ARROW_CMD_BASE + level;
    }
}

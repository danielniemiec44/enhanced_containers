package pl.com.sofitfy.enhanced_containers.crafting.gui;

/**
 * GUITitleFormatter - formatowanie tytułów okien z Custom Font'ów
 *
 * Pozwala wyświetlać niestandardową grafikę w tytułach okien dzięki Custom Font Texture
 * z Resource Packa. Używa znaku \uE001 który mapuje się na teksturę ex_crafting.png
 */
public class GUITitleFormatter {
    // Znak do cofnięcia kursora o 8 pikseli w lewo
    // Przydatne do wycentrowania tekstury w tytule okna
    private static final String CURSOR_BACK = "\uF808";

    // Znak Custom Font dla tekstury ex_crafting.png
    // Zdefiniowany w default.json jako:
    // {
    //   "type": "bitmap",
    //   "file": "minecraft:gui/plethora/ex_crafting.png",
    //   "ascent": 18,
    //   "height": 256,
    //   "chars": ["\uE001"]
    // }
    private static final String CUSTOM_FONT_CHAR = "\uE001";

    // Kod koloru białego - wymusza jasny kolor zamiast domyślnego szarego
    // Minecraft przyciemnia okna poprzez drop shadow i overlay
    // Kombinacja §f (biały) + §l (pogrubienie) przebija przez renderowanie okna
    // Pogrubienie zmienia render pass tekstu na wyższą warstwę
    private static final String WHITE_COLOR = "§f";
    private static final String BOLD_FORMAT = "§l";

    // Prefiks do wyczyszczenia formatowania (żeby nie było kolorów pod teksturą)
    private static final String RESET_FORMAT = "§r";

    /**
     * Stwórz tytuł okna craftingu z Custom Font teksturą
     *
     * Zwraca: §r§f§l\uF808\uF808\uF808\uF808\uF808\uF808\uF808\uF808\uE001
     * Gdzie:
     * - §r to reset formatowania (wyczyści domyślne kodowanie GUI Minecrafta)
     * - §f to biały kolor (wymusza jasny kolor po resetowaniu)
     * - §l to pogrubienie (przebija przez drop shadow i overlay)
     * - \uF808 to cofnięcie kursora o 8 pikseli (powtórzone 8 razy)
     * - \uE001 to Custom Font Character wyświetlający ex_crafting.png
     *
     * Efekt: tekstura ex_crafting.png jest wyświetlana jasno bez przyciemniania
     * (reset + biały + pogrubienie przebijają drop shadow i overlay GUI)
     *
     * @return tytuł okna z teksturą craftingu (bez przyciemniania)
     */
    public static String getCraftingTitle() {
        // §r - najpierw resetuj formatowanie
        // §f§l - potem wymusz biały + pogrubienie
        return RESET_FORMAT + WHITE_COLOR + BOLD_FORMAT + CURSOR_BACK + CURSOR_BACK + CURSOR_BACK + CURSOR_BACK + CURSOR_BACK + CURSOR_BACK + CURSOR_BACK + CURSOR_BACK + CUSTOM_FONT_CHAR;
    }

    /**
     * Stwórz tytuł okna edytora receptury z Custom Font teksturą
     *
     * Format: §r§f§l\uF808\uF808\uF808\uF808\uF808\uF808\uF808\uF808\uE001
     * (identyczne do craftingu, bo edytor też używa tego samego layoutu)
     *
     * Efekt: tekstura ex_crafting.png jest wyświetlana jasno bez przyciemniania
     * (reset + biały + pogrubienie przebijają drop shadow i overlay GUI)
     *
     * @return tytuł okna edytora z teksturą (bez przyciemniania)
     */
    public static String getEditorTitle() {
        // §r - najpierw resetuj formatowanie
        // §f§l - potem wymusz biały + pogrubienie
        return RESET_FORMAT + WHITE_COLOR + BOLD_FORMAT + CURSOR_BACK + CURSOR_BACK + CURSOR_BACK + CURSOR_BACK + CURSOR_BACK + CURSOR_BACK + CURSOR_BACK + CURSOR_BACK + CUSTOM_FONT_CHAR;
    }

    /**
     * Sprawdź czy tytuł zawiera Custom Font Character
     *
     * Przydatne do testów i debugowania
     *
     * @param title tytuł do sprawdzenia
     * @return true jeśli zawiera \uE001
     */
    public static boolean hasCustomFont(String title) {
        return title != null && title.contains(CUSTOM_FONT_CHAR);
    }

    /**
     * Pobierz surowy Custom Font Character
     *
     * @return char \uE001
     */
    public static String getCustomFontChar() {
        return CUSTOM_FONT_CHAR;
    }

    /**
     * Stwórz normalny tytuł dla okna wyboru receptur (bez custom czfonki)
     *
     * Zwraca: §6§lRecipe Selection
     * Gdzie:
     * - §6 to złoty kolor (ładnie wygląda w menu)
     * - §l to pogrubienie (bardziej czytelne)
     *
     * @return normalny tytuł dla selectora receptur
     */
    public static String getRecipeSelectionTitle() {
        return "§6§lRecipe Selection";
    }
}


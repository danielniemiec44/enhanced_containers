package pl.com.sofitfy.enhanced_containers.crafting.gui;

/**
 * GUITitleFormatter - formatowanie tytułów okien z Custom Font'ów
 *
 * Pozwala wyświetlać niestandardową grafikę w tytułach okien dzięki Custom Font Texture
 * z Resource Packa. Używa znaku \uE001 który mapuje się na teksturę ex_crafting.png
 */
public class GUITitleFormatter {
    // Znak do cofnięcia kursora w lewo (Custom Font)
    // Przydatne do wycentrowania tekstury w tytule okna
    private static final String CURSOR_BACK = "\uF808";

    // Ile razy powtarzamy cofnięcie kursora PRZED teksturą.
    // Uwaga: ta wartość zależy od metryk fontu w resource packu.
    static final int CURSOR_BACK_COUNT = 9;

    // Ile razy cofamy kursor PO teksturze, zanim wypiszemy normalny tytuł.
    // To jest celowo stałe (bez configu), bo ma zależeć wyłącznie od resource packa.
    // Uwaga praktyczna: to jest liczba ZNAKÓW, więc duża wartość = długi tytuł.
    public static final int CURSOR_BACK_AFTER_TEXTURE_COUNT = 169;

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
    * Buduje tytuł okna tak, żeby:
     * 1) najpierw wyrenderować teksturę (custom font),
     * 2) a potem wyświetlić normalny tytuł (np. z configu) w "vanilla" stylu.
     *
     * Dlaczego tak?
     * - W samym tytule inventoria nie mamy prawdziwego "przesuwania w pionie".
     * - Możemy natomiast sterować X poprzez znaki o ujemnym advance (tu: \uF808 z RP).
     * - Kluczowe jest, żeby po części "§f§l + tekstura" zrobić reset formatowania,
     *   bo inaczej tytuł z configu byłby wymuszony na biało/pogrubiony.
     *
     * @param vanillaTitle tytuł jaki ma być widoczny normalnie (np. z configu)
     * @param cursorBackAfterTextureCount ile razy cofnąć kursor po teksturze
     * @return string tytułu: tekstura + cofnięcie + reset + tytuł
     */
    public static String withTextureAndVanillaTitle(String vanillaTitle, int cursorBackAfterTextureCount) {
        String safeTitle = (vanillaTitle == null) ? "" : vanillaTitle;
        int safeBack = Math.max(0, cursorBackAfterTextureCount);
        return RESET_FORMAT
            + WHITE_COLOR
            + BOLD_FORMAT
            + CURSOR_BACK.repeat(CURSOR_BACK_COUNT)
            + CUSTOM_FONT_CHAR
            + CURSOR_BACK.repeat(safeBack)
            + RESET_FORMAT
            + safeTitle;
    }

    /**
     * Wersja wygodna: używa domyślnej liczby cofnięć po teksturze.
     */
    public static String withTextureAndVanillaTitle(String vanillaTitle) {
        return withTextureAndVanillaTitle(vanillaTitle, CURSOR_BACK_AFTER_TEXTURE_COUNT);
    }

    /**
     * Stwórz tytuł okna craftingu z Custom Font teksturą
     *
        * Zwraca: §r§f§l\uF808 x9 + \uE001
     * Gdzie:
     * - §r to reset formatowania (wyczyści domyślne kodowanie GUI Minecrafta)
     * - §f to biały kolor (wymusza jasny kolor po resetowaniu)
     * - §l to pogrubienie (przebija przez drop shadow i overlay)
        * - \uF808 to cofnięcie kursora w lewo (u Ciebie advance = -2 px, powtórzone 9 razy)
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
        return RESET_FORMAT + WHITE_COLOR + BOLD_FORMAT + CURSOR_BACK.repeat(CURSOR_BACK_COUNT) + CUSTOM_FONT_CHAR;
    }

    /**
     * Stwórz tytuł okna edytora receptury z Custom Font teksturą
     *
        * Format: §r§f§l\uF808 x9 + \uE001
     *
     * Efekt: tekstura ex_crafting.png jest wyświetlana jasno bez przyciemniania
     * (reset + biały + pogrubienie przebijają drop shadow i overlay GUI)
     *
     * @return tytuł okna edytora z teksturą (bez przyciemniania)
     */
    public static String getEditorTitle() {
        // §r - najpierw resetuj formatowanie
        // §f§l - potem wymusz biały + pogrubienie
        return RESET_FORMAT + WHITE_COLOR + BOLD_FORMAT + CURSOR_BACK.repeat(CURSOR_BACK_COUNT) + CUSTOM_FONT_CHAR;
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


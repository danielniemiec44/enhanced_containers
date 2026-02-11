package pl.com.sofitfy.enhanced_containers.furnace;

/**
 * FurnaceGUITitleFormatter - formatowanie tytułów okien pieca z Custom Font
 * * Implementacja "Softify Zero-Width":
 * Renderowanie elementów niezależnie od siebie, używając cofania kursora
 * do punktów kotwiczenia (Anchor Points).
 */
public class FurnaceGUITitleFormatter {
    
    // Znak do cofnięcia kursora w lewo (Custom Font z Resource Packa)
    private static final String CURSOR_BACK = "\uF808"; // -2px
    
    // Negative space characters (przesuwanie kursora W LEWO)
    private static final String NEG_1 = "\uF801";   // -1px
    private static final String NEG_2 = "\uF802";   // -2px
    private static final String NEG_4 = "\uF804";   // -4px
    private static final String NEG_8 = "\uF809";   // -8px
    private static final String NEG_16 = "\uF80A";  // -16px
    private static final String NEG_32 = "\uF80B";  // -32px
    private static final String NEG_64 = "\uF80C";  // -64px
    private static final String NEG_128 = "\uF80D"; // -128px
    
    // Positive space characters (przesuwanie kursora W PRAWO)
    private static final String POS_1 = "\uF811";   // +1px
    private static final String POS_2 = "\uF812";   // +2px
    private static final String POS_4 = "\uF814";   // +4px
    private static final String POS_8 = "\uF818";   // +8px
    private static final String POS_16 = "\uF819";  // +16px
    private static final String POS_32 = "\uF81A";  // +32px
    private static final String POS_64 = "\uF81B";  // +64px
    private static final String POS_128 = "\uF81C"; // +128px
    
    // Konfiguracja Offsetów
    private static final int CURSOR_BACK_COUNT = 9; // Cofnięcie przed teksturą tła
    
    // Znak Custom Font dla tekstury tła (ex_furnace.png)
    public static final String FURNACE_FONT_CHAR = "\uE002";
    
    // === POZYCJONOWANIE ===
    // Szerokość GUI tła: ~176px
    // Pozycja X slotu strzałek (kolumna 3): ~62px od lewej
    // Pozycja X startu tytułu: ~8px od lewej
    
    // Ile trzeba cofnąć od KOŃCA tła (176px), żeby trafić w początek strzałek (62px)
    // 176 - 62 = 114px
    private static final int OFFSET_BACKGROUND_TO_ARROW = 114;
    
    // Ile trzeba cofnąć od strzałek (62px), żeby trafić w tytuł (8px)
    // 62 - 8 = 54px
    private static final int OFFSET_ARROW_TO_TITLE = 54;

    // Alternatywnie: Cofnięcie od tła bezpośrednio do tytułu (dla metod bez strzałek)
    // 176 - 8 = 168px (blisko Twojego oryginalnego 169)
    public static final int CURSOR_BACK_AFTER_TEXTURE_COUNT = 169; // Oryginalna wartość dla bezpiecznego fallbacku
    
    // =============== ARROW FONT CHARS ===============
    public static final char ARROW_LINE0_START = 0xE030;
    public static final char ARROW_LINE1_START = 0xE050;
    public static final char ARROW_LINE2_START = 0xE070;
    
    public static final int[] ARROW_LINE_STARTS = {
        ARROW_LINE0_START, ARROW_LINE1_START, ARROW_LINE2_START
    };
    
    public static final int MAX_ARROW_FRAMES = 22;
    
    // =============== FIRE FONT CHARS ===============
    // Ogień: E090 (empty) - E09D (13 = full) = 14 stanów
    public static final int FIRE_CHAR_START = 0xE090;
    public static final int MAX_FIRE_FRAMES = 13;
    
    // Pozycja X ognia: slot 38 = kolumna 2 (0-indexed)
    // 7 (GUI padding) + 2*18 (2 kolumny) + 1 (slot padding) = 44px
    // Od końca tła (176px) do ognia (44px) = 132px
    private static final int OFFSET_BACKGROUND_TO_FIRE = 132;
    
    // Szerokość tekstury ognia (14px) + 1px shadow = 15px
    private static final int FIRE_ADVANCE = 15;
    
    // Kolory i formatowanie
    private static final String WHITE_COLOR = "§f";
    private static final String BOLD_FORMAT = "§l";
    private static final String RESET_FORMAT = "§r";
    
    /**
     * DOKŁADNA mapa szerokości (advance) dla każdego poziomu strzałki.
     * Zmierzone skryptem + 1px poprawki na Shadow Minecrafta.
     */
    private static final int[] ARROW_ADVANCES = {
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
    };

    /**
     * Główna metoda naprawiająca problem "skakania".
     * Renderuje elementy w kolejności: TŁO -> (cofnij) -> STRZAŁKI -> (cofnij) -> TYTUŁ.
     * Używa logiki Zero-Width dla strzałek.
     */
    public static String createTitleWithArrows(String vanillaTitle, double[] arrowProgress) {
        StringBuilder sb = new StringBuilder();
        
        // 1. Reset formatowania
        sb.append(RESET_FORMAT).append(WHITE_COLOR).append(BOLD_FORMAT);
        
        // 2. Ustawienie na start i renderowanie TŁA
        sb.append(CURSOR_BACK.repeat(CURSOR_BACK_COUNT)); 
        sb.append(FURNACE_FONT_CHAR);
        
        // KURSOR JEST TERAZ NA KOŃCU TŁA (~176px)
        
        // 3. Cofnięcie do pozycji STRZAŁEK (X = 62)
        sb.append(negativeSpace(OFFSET_BACKGROUND_TO_ARROW));
        
        // 4. Renderowanie Strzałek (Metoda Zero-Width)
        // Kursor stoi na X=62. Rysujemy strzałkę, a potem natychmiast cofamy o jej szerokość.
        // Dzięki temu dla każdej linii zaczynamy z tego samego X=62.
        
        double[] safeProgress = arrowProgress != null ? arrowProgress : new double[]{0, 0, 0};
        
        // Reset koloru dla pewności (zazwyczaj biały dla fontów)
        sb.append(RESET_FORMAT).append(WHITE_COLOR);

        for (int line = 0; line < 3; line++) {
            double progress = line < safeProgress.length ? safeProgress[line] : 0.0;
            
            // Oblicz level (0-22)
            int level = calculateLevel(progress);
            
            // Pobierz znak
            char arrowChar = getArrowChar(line, level);
            
            // Pobierz szerokość (advance) + shadow fix
            int advance = getArrowAdvance(level);
            
            // Przesunięcie X: LINE1 i LINE2 mają +1px w prawo (cofamy o 1px mniej)
            int xShift = (line == 0) ? 0 : 1;
            
            // Rysuj: [Znak] + [Cofnij]
            sb.append(arrowChar);
            
            // Cofamy advance + xShift (jeśli xShift ujemny, cofamy mniej)
            sb.append(negativeSpace(advance + xShift));
        }
        
        // KURSOR JEST NADAL NA POZYCJI STRZAŁEK (X=62) - logicznie
        
        // 5. Cofnięcie do pozycji TYTUŁU (X = ~8)
        sb.append(negativeSpace(OFFSET_ARROW_TO_TITLE));
        
        // 6. Renderowanie Tytułu
        sb.append(RESET_FORMAT); // Reset, żeby tytuł był szary/domyślny, chyba że vanillaTitle ma kolory
        String safeTitle = (vanillaTitle != null) ? vanillaTitle : "";
        sb.append(safeTitle);
        
        return sb.toString();
    }
    
    // ================= HELPERY =================
    
    private static int calculateLevel(double progress) {
        if (progress <= 0.0) return 0;
        if (progress >= 1.0) return MAX_ARROW_FRAMES;
        return (int) Math.ceil(progress * MAX_ARROW_FRAMES);
    }
    
    private static int getArrowAdvance(int level) {
        int rawAdvance;
        if (level < 0) rawAdvance = ARROW_ADVANCES[0];
        else if (level >= ARROW_ADVANCES.length) rawAdvance = ARROW_ADVANCES[ARROW_ADVANCES.length - 1];
        else rawAdvance = ARROW_ADVANCES[level];
        
        // SOFTIFY FIX: Dodajemy 1px, bo Minecraft renderuje cień (shadow), 
        // który przesuwa kursor o dodatkowy piksel, nawet jeśli go nie widzimy.
        return rawAdvance + 1; 
    }

    public static char getArrowChar(int lineIndex, int level) {
        int safeLineIndex = Math.max(0, Math.min(2, lineIndex));
        int safeLevel = Math.max(0, Math.min(MAX_ARROW_FRAMES, level));
        return (char) (ARROW_LINE_STARTS[safeLineIndex] + safeLevel);
    }
    
    /**
     * Pobiera znak ognia dla danego poziomu wypełnienia (0-13)
     * 
     * @param level poziom wypełnienia 0 (pusty) - 13 (pełny)
     * @return znak Unicode ognia
     */
    public static char getFireChar(int level) {
        int safeLevel = Math.max(0, Math.min(MAX_FIRE_FRAMES, level));
        return (char) (FIRE_CHAR_START + safeLevel);
    }
    
    /**
     * Oblicza poziom ognia na podstawie postępu (0.0-1.0)
     */
    private static int calculateFireLevel(double progress) {
        if (progress <= 0.0) return 0;
        if (progress >= 1.0) return MAX_FIRE_FRAMES;
        return (int) Math.ceil(progress * MAX_FIRE_FRAMES);
    }
    
    /**
     * Główna metoda z obsługą strzałek I ognia.
     * Renderuje: TŁO -> STRZAŁKI -> OGIEŃ -> TYTUŁ
     * 
     * @param vanillaTitle tytuł widoczny dla gracza
     * @param arrowProgress tablica 3 wartości postępu strzałek (0.0-1.0)
     * @param fireProgress postęp palenia paliwa (0.0-1.0)
     * @return sformatowany tytuł
     */
    public static String createTitleWithArrowsAndFire(String vanillaTitle, double[] arrowProgress, double fireProgress) {
        StringBuilder sb = new StringBuilder();
        
        // 1. Reset formatowania
        sb.append(RESET_FORMAT).append(WHITE_COLOR).append(BOLD_FORMAT);
        
        // 2. Ustawienie na start i renderowanie TŁA
        sb.append(CURSOR_BACK.repeat(CURSOR_BACK_COUNT)); 
        sb.append(FURNACE_FONT_CHAR);
        
        // KURSOR JEST TERAZ NA KOŃCU TŁA (~176px)
        
        // 3. Cofnięcie do pozycji STRZAŁEK (X = 62)
        sb.append(negativeSpace(OFFSET_BACKGROUND_TO_ARROW));
        
        // 4. Renderowanie Strzałek (Metoda Zero-Width) - każda indywidualnie
        double[] safeProgress = arrowProgress != null ? arrowProgress : new double[]{0, 0, 0};
        sb.append(RESET_FORMAT).append(WHITE_COLOR);
        
        // Indywidualne przesunięcia dla każdej linii (w pikselach w prawo)
        int[] lineShifts = {0, 1, 2}; // Linia 0: 0px, Linia 1: +1px, Linia 2: +2px

        for (int line = 0; line < 3; line++) {
            double progress = line < safeProgress.length ? safeProgress[line] : 0.0;
            int level = calculateLevel(progress);
            char arrowChar = getArrowChar(line, level);
            int advance = getArrowAdvance(level);
            
            // Przed renderowaniem: przesuń w prawo o lineShift dla tej linii
            if (lineShifts[line] > 0) {
                sb.append(positiveSpace(lineShifts[line]));
            }
            
            sb.append(arrowChar);
            
            // Cofnij o advance + lineShift (żeby wrócić do pozycji bazowej)
            sb.append(negativeSpace(advance + lineShifts[line]));
        }
        
        // KURSOR JEST NA POZYCJI STRZAŁEK (X=62) - niezależnie od szerokości strzałek!
        
        // 5. Przesunięcie do pozycji OGNIA (X = 40)
        // Od strzałek (62px) do ognia (40px) = cofnij 22px
        sb.append(negativeSpace(22));
        
        // 6. Renderowanie Ognia (Zero-Width) - TYLKO gdy jest aktywny
        int fireLevel = calculateFireLevel(Math.max(0.0, Math.min(1.0, fireProgress)));
        
        if (fireLevel > 0) {
            // Ogień aktywny - renderuj znak i cofnij o jego szerokość
            char fireChar = getFireChar(fireLevel);
            sb.append(fireChar);
            sb.append(negativeSpace(FIRE_ADVANCE));
        }
        // Gdy fireLevel = 0, nie renderujemy nic - pusty ogień nie ma sensu wizualnego
        // i fire_empty.png może mieć nieprawidłowy advance (0 gdy przezroczysty)
        
        // KURSOR JEST NA POZYCJI OGNIA (X=40) niezależnie czy ogień był renderowany
        
        // 7. Tytuł: pozycjonuj od KOŃCA TŁA (stała pozycja!) nie od strzałek
        // Cofnij do początku: 40px (pozycja ognia) -> 8px (tytuł) = 32px
        sb.append(negativeSpace(32));
        
        // 8. Renderowanie Tytułu
        sb.append(RESET_FORMAT);
        String safeTitle = (vanillaTitle != null) ? vanillaTitle : "";
        sb.append(safeTitle);
        
        return sb.toString();
    }

    // Metody legacy (dla kompatybilności z resztą kodu)
    public static String withTextureAndVanillaTitle(String vanillaTitle) {
        return withTextureAndVanillaTitle(vanillaTitle, CURSOR_BACK_AFTER_TEXTURE_COUNT);
    }

    public static String withTextureAndVanillaTitle(String vanillaTitle, int cursorBack) {
        String safeTitle = (vanillaTitle == null) ? "" : vanillaTitle;
        int safeBack = Math.max(0, cursorBack);
        return RESET_FORMAT + WHITE_COLOR + BOLD_FORMAT
            + CURSOR_BACK.repeat(CURSOR_BACK_COUNT)
            + FURNACE_FONT_CHAR
            + CURSOR_BACK.repeat(safeBack)
            + RESET_FORMAT
            + safeTitle;
    }
    
    public static String createFurnaceTitle() {
        return withTextureAndVanillaTitle("Furnace");
    }
    
    public static String createFurnaceTitle(String title) {
        return withTextureAndVanillaTitle(title);
    }

    public static String negativeSpace(int pixels) {
        if (pixels <= 0) return "";
        StringBuilder sb = new StringBuilder();
        int remaining = pixels;
        // Greedy algorithm dla najmniejszej liczby znaków
        while (remaining >= 128) { sb.append(NEG_128); remaining -= 128; }
        if (remaining >= 64) { sb.append(NEG_64); remaining -= 64; }
        if (remaining >= 32) { sb.append(NEG_32); remaining -= 32; }
        if (remaining >= 16) { sb.append(NEG_16); remaining -= 16; }
        if (remaining >= 8)  { sb.append(NEG_8);  remaining -= 8; }
        if (remaining >= 4)  { sb.append(NEG_4);  remaining -= 4; }
        if (remaining >= 2)  { sb.append(NEG_2);  remaining -= 2; }
        if (remaining >= 1)  { sb.append(NEG_1);  remaining -= 1; }
        return sb.toString();
    }
    
    public static String positiveSpace(int pixels) {
        if (pixels <= 0) return "";
        StringBuilder sb = new StringBuilder();
        int remaining = pixels;
        while (remaining >= 128) { sb.append(POS_128); remaining -= 128; }
        if (remaining >= 64) { sb.append(POS_64); remaining -= 64; }
        if (remaining >= 32) { sb.append(POS_32); remaining -= 32; }
        if (remaining >= 16) { sb.append(POS_16); remaining -= 16; }
        if (remaining >= 8)  { sb.append(POS_8);  remaining -= 8; }
        if (remaining >= 4)  { sb.append(POS_4);  remaining -= 4; }
        if (remaining >= 2)  { sb.append(POS_2);  remaining -= 2; }
        if (remaining >= 1)  { sb.append(POS_1);  remaining -= 1; }
        return sb.toString();
    }
}

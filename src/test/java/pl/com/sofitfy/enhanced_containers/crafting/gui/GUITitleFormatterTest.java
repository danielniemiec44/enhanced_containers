package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GUITitleFormatterTest - testy dla formatowania tytułów okien
 *
 * Sprawdzamy czy Custom Font Character jest prawidłowo wstawiany do tytułów
 */
@DisplayName("Formatowanie tytułów okien z Custom Font'ami")
public class GUITitleFormatterTest {

    private static int countChar(String text, char searched) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == searched) {
                count++;
            }
        }
        return count;
    }

    private static String expectedTitleWithCursorBackCount(int cursorBackCount) {
        return "§r§f§l" + "\uF808".repeat(cursorBackCount) + "\uE001";
    }

    @Nested
    @DisplayName("Generowanie tytułu craftingu")
    class CraftingTitleTests {

        @Test
        @DisplayName("Tytuł craftingu powinien zawierać Custom Font Character")
        void testCraftingTitleHasCustomFont() {
            // Dado: pobieram tytuł craftingu
            String title = GUITitleFormatter.getCraftingTitle();

            // Kiedy: sprawdzam czy zawiera Custom Font Character
            // To: powinien zawierać \uE001
            assertTrue(GUITitleFormatter.hasCustomFont(title),
                "Tytuł craftingu powinien zawierać Custom Font Character");
        }

        @Test
        @DisplayName("Tytuł craftingu powinien zawierać znaki cofnięcia kursora")
        void testCraftingTitleHasCursorBack() {
            // Dado: pobieram tytuł craftingu
            String title = GUITitleFormatter.getCraftingTitle();

            // Kiedy: sprawdzam czy zawiera znaki cofnięcia kursora
            // To: powinien zawierać \uF808 odpowiednią liczbę razy dla lepszego wycentrowania
            int cursorBackCount = countChar(title, '\uF808');
            assertEquals(GUITitleFormatter.CURSOR_BACK_COUNT, cursorBackCount,
                "Tytuł powinien zawierać " + GUITitleFormatter.CURSOR_BACK_COUNT + " znaków \\uF808 do cofnięcia kursora");
        }

        @Test
        @DisplayName("Tytuł craftingu powinien zawierać reset formatowania")
        void testCraftingTitleHasResetFormat() {
            // Dado: pobieram tytuł craftingu
            String title = GUITitleFormatter.getCraftingTitle();

            // Kiedy: sprawdzam czy zaczyna się od §r§f§l
            // To: reset najpierw wyczyści formatowanie, potem biały + pogrubienie
            assertTrue(title.startsWith("§r§f§l"),
                "Tytuł powinien zaczynać się od §r§f§l (reset + biały + pogrubienie)");
        }

        @Test
        @DisplayName("Tytuł craftingu powinien zawierać reset przed białym kolorem")
        void testCraftingTitleHasBoldForNoDimming() {
            // Dado: pobieram tytuł craftingu
            String title = GUITitleFormatter.getCraftingTitle();

            // Kiedy: sprawdzam czy reset jest PRZED §f§l
            // To: porządek jest ważny - najpierw reset, potem biały + pogrubienie
            assertTrue(title.startsWith("§r") && title.contains("§f") && title.contains("§l"),
                "Tytuł musi mieć §r na początku, potem §f i §l aby przebić drop shadow");
            int resetPos = title.indexOf("§r");
            int boldPos = title.indexOf("§f");
            assertTrue(resetPos < boldPos,
                "Reset (§r) musi być PRZED białym kolorem (§f)");
        }

        @Test
        @DisplayName("Struktura tytułu craftingu powinna być: §r§f§l + N x\\uF808 + \\uE001")
        void testCraftingTitleStructure() {
            // Dado: pobieram tytuł craftingu
            String title = GUITitleFormatter.getCraftingTitle();

            // Kiedy: sprawdzam strukturę tytułu
            // To: powinien mieć format: §r§f§l + 17 znaków cofnięcia i Custom Font
            String expectedFormat = expectedTitleWithCursorBackCount(GUITitleFormatter.CURSOR_BACK_COUNT);
            assertEquals(expectedFormat, title,
                "Tytuł craftingu powinien mieć format: §r (reset) + §f§l (biały+pogrubienie) + " + GUITitleFormatter.CURSOR_BACK_COUNT + "x\\uF808 + \\uE001");
        }
    }

    @Nested
    @DisplayName("Generowanie tytułu edytora")
    class EditorTitleTests {

        @Test
        @DisplayName("Tytuł edytora powinien zawierać Custom Font Character")
        void testEditorTitleHasCustomFont() {
            // Dado: pobieram tytuł edytora
            String title = GUITitleFormatter.getEditorTitle();

            // Kiedy: sprawdzam czy zawiera Custom Font Character
            // To: powinien zawierać \uE001
            assertTrue(GUITitleFormatter.hasCustomFont(title),
                "Tytuł edytora powinien zawierać Custom Font Character");
        }

        @Test
        @DisplayName("Tytuł edytora powinien zawierać znaki cofnięcia kursora")
        void testEditorTitleHasCursorBack() {
            // Dado: pobieram tytuł edytora
            String title = GUITitleFormatter.getEditorTitle();

            // Kiedy: zliczam znaki cofnięcia kursora
            // To: powinien zawierać \uF808 odpowiednią liczbę razy dla lepszego wycentrowania
            int cursorBackCount = countChar(title, '\uF808');
            assertEquals(GUITitleFormatter.CURSOR_BACK_COUNT, cursorBackCount,
                "Tytuł edytora powinien zawierać " + GUITitleFormatter.CURSOR_BACK_COUNT + " znaków \\uF808 do cofnięcia kursora");
        }

        @Test
        @DisplayName("Tytuł edytora powinien nie być pusty")
        void testEditorTitleNotEmpty() {
            // Dado: pobieram tytuł edytora
            String title = GUITitleFormatter.getEditorTitle();

            // Kiedy: sprawdzam czy tytuł nie jest pusty
            // To: powinien mieć zawartość
            assertNotNull(title, "Tytuł nie powinien być null");
            assertFalse(title.isBlank(), "Tytuł nie powinien być pusty");
        }

        @Test
        @DisplayName("Tytuł edytora powinien zawierać reset przed białym kolorem")
        void testEditorTitleHasBoldForNoDimming() {
            // Dado: pobieram tytuł edytora
            String title = GUITitleFormatter.getEditorTitle();

            // Kiedy: sprawdzam czy reset jest PRZED §f§l
            // To: porządek jest ważny - najpierw reset, potem biały + pogrubienie
            assertTrue(title.startsWith("§r") && title.contains("§f") && title.contains("§l"),
                "Tytuł edytora musi mieć §r na początku, potem §f i §l aby przebić drop shadow");
            int resetPos = title.indexOf("§r");
            int boldPos = title.indexOf("§f");
            assertTrue(resetPos < boldPos,
                "Reset (§r) musi być PRZED białym kolorem (§f)");
        }

        @Test
        @DisplayName("Struktura tytułu edytora powinna być: §r§f§l + N x\\uF808 + \\uE001")
        void testEditorTitleStructure() {
            // Dado: pobieram tytuł edytora
            String title = GUITitleFormatter.getEditorTitle();

            // Kiedy: sprawdzam strukturę tytułu
            // To: powinien mieć format: §r§f§l + 17 znaków cofnięcia i Custom Font
            String expectedFormat = expectedTitleWithCursorBackCount(GUITitleFormatter.CURSOR_BACK_COUNT);
            assertEquals(expectedFormat, title,
                "Tytuł edytora powinien mieć format: §r (reset) + §f§l (biały+pogrubienie) + " + GUITitleFormatter.CURSOR_BACK_COUNT + "x\\uF808 + \\uE001");
        }
    }

    @Nested
    @DisplayName("Sprawdzanie Custom Font Character")
    class CustomFontCharTests {

        @Test
        @DisplayName("Custom Font Character powinien być dostępny")
        void testCustomFontCharAvailable() {
            // Dado: pobieram Character
            String customChar = GUITitleFormatter.getCustomFontChar();

            // Kiedy: sprawdzam czy jest dostępny
            // To: powinien być non-null i non-empty
            assertNotNull(customChar, "Custom Font Character nie powinien być null");
            assertFalse(customChar.isEmpty(), "Custom Font Character nie powinien być pusty");
        }

        @Test
        @DisplayName("Metoda hasCustomFont powinna zwrócić true dla poprawnego tytułu")
        void testHasCustomFontWithValidTitle() {
            // Dado: mam tytuł ze Custom Font Character
            String title = GUITitleFormatter.getCraftingTitle();

            // Kiedy: sprawdzam czy zawiera Custom Font
            // To: powinno zwrócić true
            assertTrue(GUITitleFormatter.hasCustomFont(title),
                "hasCustomFont powinno zwrócić true dla tytułu z Custom Font Character");
        }

        @Test
        @DisplayName("Metoda hasCustomFont powinna zwrócić false dla zwykłego tytułu")
        void testHasCustomFontWithRegularTitle() {
            // Dado: mam zwykły tytuł bez Custom Font
            String regularTitle = "§6Normalne okno";

            // Kiedy: sprawdzam czy zawiera Custom Font
            // To: powinno zwrócić false
            assertFalse(GUITitleFormatter.hasCustomFont(regularTitle),
                "hasCustomFont powinno zwrócić false dla zwykłego tytułu");
        }

        @Test
        @DisplayName("Metoda hasCustomFont powinna obsługiwać null")
        void testHasCustomFontWithNull() {
            // Dado: mam null
            // Kiedy: sprawdzam czy zawiera Custom Font
            // To: powinno zwrócić false (bez throwowania błędu)
            assertFalse(GUITitleFormatter.hasCustomFont(null),
                "hasCustomFont powinno zwrócić false dla null");
        }
    }

    @Nested
    @DisplayName("Scenariusze praktyczne")
    public class PracticalTests {

        @Test
        @DisplayName("Scenariusz: gracz otwiera okno craftingu")
        void testPlayerOpensCraftingWindow() {
            // Dado: system musi stworzyć tytuł do okna craftingu
            String title = GUITitleFormatter.getCraftingTitle();

            // Kiedy: tytuł jest używany do stworzenia inventory
            // To: powinien mieć prawidłowy format z Custom Font Character
            assertNotNull(title, "Tytuł musi być dostępny");
            assertTrue(GUITitleFormatter.hasCustomFont(title),
                "Tytuł musi zawierać Custom Font Character");
            assertTrue(title.startsWith("§r§f§l"),
                "Tytuł musi zaczynać się od §r§f§l (reset + biały + pogrubienie przebija drop shadow)");
        }

        @Test
        @DisplayName("Scenariusz: admin otwiera edytor receptury")
        void testAdminOpensRecipeEditor() {
            // Dado: system musi stworzyć tytuł do edytora
            String title = GUITitleFormatter.getEditorTitle();

            // Kiedy: tytuł jest używany do stworzenia inventory
            // To: powinien mieć prawidłowy format z Custom Font Character
            assertNotNull(title, "Tytuł musi być dostępny");
            assertTrue(GUITitleFormatter.hasCustomFont(title),
                "Tytuł musi zawierać Custom Font Character");
            assertTrue(title.startsWith("§r§f§l"),
                "Tytuł musi zaczynać się od §r§f§l (reset + biały + pogrubienie przebija drop shadow)");
        }

        @Test
        @DisplayName("Scenariusz: tytuły mają strukturę reset+białe+pogrubienie + cofnięcia + Custom Font")
        void testTitleHasProperCentering() {
            // Dado: pobieram tytuły
            String craftingTitle = GUITitleFormatter.getCraftingTitle();
            String editorTitle = GUITitleFormatter.getEditorTitle();

            // Kiedy: sprawdzam strukturę
            // To: powinny zaczynać się od reset + biały + pogrubienie, potem cofnięcia i Custom Font
            String expectedCraftingFormat = expectedTitleWithCursorBackCount(GUITitleFormatter.CURSOR_BACK_COUNT);
            String expectedEditorFormat = expectedTitleWithCursorBackCount(GUITitleFormatter.CURSOR_BACK_COUNT);

            assertEquals(expectedCraftingFormat, craftingTitle,
                "Tytuł craftingu powinien mieć format: reset + biały + pogrubienie + " + GUITitleFormatter.CURSOR_BACK_COUNT + " cofnięć");
            assertEquals(expectedEditorFormat, editorTitle,
                "Tytuł edytora powinien mieć format: reset + biały + pogrubienie + " + GUITitleFormatter.CURSOR_BACK_COUNT + " cofnięć");
        }
    }

    @Nested
    @DisplayName("Generowanie tytułu wyboru receptur (bez Custom Font)")
    class RecipeSelectionTitleTests {

        @Test
        @DisplayName("Tytuł wyboru receptur NIE powinien zawierać Custom Font Character")
        void testRecipeSelectionTitleNoCustomFont() {
            // Dado: pobieram tytuł wyboru receptur
            String title = GUITitleFormatter.getRecipeSelectionTitle();

            // Kiedy: sprawdzam czy zawiera Custom Font Character
            // To: NIE powinien zawierać \uE001 (bo to nie jest miejsce do symulacji crafcingu)
            assertFalse(GUITitleFormatter.hasCustomFont(title),
                "Tytuł wyboru receptur NIE powinien zawierać Custom Font Character");
        }

        @Test
        @DisplayName("Tytuł wyboru receptur powinien być złoty i pogrubiony")
        void testRecipeSelectionTitleFormatting() {
            // Dado: pobieram tytuł wyboru receptur
            String title = GUITitleFormatter.getRecipeSelectionTitle();

            // Kiedy: sprawdzam formatowanie
            // To: powinien zaczynać się od §6§l (złoty + pogrubienie)
            assertTrue(title.startsWith("§6§l"),
                "Tytuł powinien być złoty i pogrubiony (§6§l)");
        }

        @Test
        @DisplayName("Tytuł wyboru receptur zawiera tekst 'Recipe Selection'")
        void testRecipeSelectionTitleText() {
            // Dado: pobieram tytuł wyboru receptur
            String title = GUITitleFormatter.getRecipeSelectionTitle();

            // Kiedy: sprawdzam czy zawiera tekst
            // To: powinien zawierać "Recipe Selection"
            assertTrue(title.contains("Recipe Selection"),
                "Tytuł powinien zawierać tekst 'Recipe Selection'");
        }

        @Test
        @DisplayName("Tytuł wyboru receptur powinien być inny od tytułów craftingu/edytora")
        void testRecipeSelectionTitleDifferentFromCraftingTitles() {
            // Dado: pobieramy wszystkie tytuły
            String selectionTitle = GUITitleFormatter.getRecipeSelectionTitle();
            String craftingTitle = GUITitleFormatter.getCraftingTitle();
            String editorTitle = GUITitleFormatter.getEditorTitle();

            // Kiedy: porównujemy
            // To: tytuł wyboru powinien być zupełnie inny od craftingu/edytora
            assertNotEquals(selectionTitle, craftingTitle,
                "Tytuł wyboru powinien być inny od craftingu");
            assertNotEquals(selectionTitle, editorTitle,
                "Tytuł wyboru powinien być inny od edytora");
        }
    }
}


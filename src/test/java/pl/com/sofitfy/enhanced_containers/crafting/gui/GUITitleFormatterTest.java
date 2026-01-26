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
            // To: powinien zawierać \uF808 (osiem razy) dla lepszego wycentrowania
            int cursorBackCount = 0;
            for (char c : title.toCharArray()) {
                if (c == '\uF808') {
                    cursorBackCount++;
                }
            }

            assertEquals(8, cursorBackCount,
                "Tytuł powinien zawierać 8 znaków \\uF808 do cofnięcia kursora");
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
        @DisplayName("Struktura tytułu craftingu powinna być: §r§f§l + 8x\\uF808 + \\uE001")
        void testCraftingTitleStructure() {
            // Dado: pobieram tytuł craftingu
            String title = GUITitleFormatter.getCraftingTitle();

            // Kiedy: sprawdzam strukturę tytułu
            // To: powinien mieć format: §r§f§l\uF808\uF808\uF808\uF808\uF808\uF808\uF808\uF808\uE001 (reset + biały + pogrubienie + 8 cofnięć)
            String expectedFormat = "§r§f§l\uF808\uF808\uF808\uF808\uF808\uF808\uF808\uF808\uE001";
            assertEquals(expectedFormat, title,
                "Tytuł craftingu powinien mieć format: §r (reset) + §f§l (biały+pogrubienie) + 8x\\uF808 + \\uE001");
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
        @DisplayName("Tytuł edytora powinien być taki sam jak craftingu")
        void testEditorTitleSameAsCrafting() {
            // Dado: pobieram oba tytuły
            String craftingTitle = GUITitleFormatter.getCraftingTitle();
            String editorTitle = GUITitleFormatter.getEditorTitle();

            // Kiedy: porównuję tytuły
            // To: powinny być identyczne (oba używają tej samej tekstury)
            assertEquals(craftingTitle, editorTitle,
                "Tytuł edytora powinien być taki sam jak craftingu (taka sama tekstura)");
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
        @DisplayName("Scenariusz: oba okna (crafting i edytor) mają tę samą teksturę")
        void testBothWindowsHaveSameTexture() {
            // Dado: tworzę tytuły dla obu okien
            String craftingTitle = GUITitleFormatter.getCraftingTitle();
            String editorTitle = GUITitleFormatter.getEditorTitle();

            // Kiedy: porównuję tytuły
            // To: powinny być identyczne (bo obie tekstury są takie same)
            assertEquals(craftingTitle, editorTitle,
                "Oba okna powinny mieć tę samą teksturę");
        }

        @Test
        @DisplayName("Scenariusz: tytuł ma strukturę §r§f§l + 8x\\uF808 + \\uE001 dla lepszego wycentrowania i bez przyciemniania")
        void testTitleHasProperCentering() {
            // Dado: pobieram tytuły
            String craftingTitle = GUITitleFormatter.getCraftingTitle();
            String editorTitle = GUITitleFormatter.getEditorTitle();

            // Kiedy: sprawdzam strukturę
            // To: powinny zaczynać się od reset + biały + pogrubienie, potem 8 znaków cofnięcia i Custom Font
            String expectedFormat = "§r§f§l\uF808\uF808\uF808\uF808\uF808\uF808\uF808\uF808\uE001";

            assertEquals(expectedFormat, craftingTitle,
                "Tytuł craftingu powinien mieć format: reset + biały + pogrubienie + 8 cofnięć");
            assertEquals(expectedFormat, editorTitle,
                "Tytuł edytora powinien mieć format: reset + biały + pogrubienie + 8 cofnięć");
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
            
            // Dodatkowo: tytuły craftingu i edytora powinny być identyczne
            assertEquals(craftingTitle, editorTitle,
                "Tytuły craftingu i edytora powinny być identyczne (oba z Custom Font)");
        }
    }
}


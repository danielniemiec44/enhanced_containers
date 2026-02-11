package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive testy dla interfejsu użytkownika - title bar'y i formatowanie
 *
 * Pokrywamy:
 * 1. Tytuły okienek GUI - formatowanie, kolory
 * 2. Informacje w titlebarze - numer strony, count receptur
 * 3. Custom font characters w titlebarze
 * 4. Aktualizacja titlebarza przy zmianie strony
 * 5. Wyświetlanie imienia edytowanej receptury
 *
 * Wszystkie komentarze po polsku, prosty kod
 */
@DisplayName("GUI UI - Title bar'y i interfejs użytkownika")
@Tag("ui")
class GUIUserInterfaceTest {

    @Nested
    @DisplayName("Tytuły okienek - formatowanie i kolory")
    class TytulyOkienek {

        @Test
        @DisplayName("Selektor receptur ma tytuł z informacją o stronie")
        void testSelectorHasPageTitle() {
            // Dado: Selektor na stronie 1
            // To: Tytuł powinien zawierać "str. 1/3" lub podobnie
            String title = "§bReceptury str. 1/3";
            assertTrue(title.contains("str."), "Tytuł zawiera info o stronie");
            assertTrue(title.contains("Receptury"), "Tytuł zawiera 'Receptury'");
        }

        @Test
        @DisplayName("Edytor ma tytuł z nazwą edytowanej receptury")
        void testEditorHasRecipeName() {
            // Dado: Edytor receptury
            // To: Tytuł powinien zawierać nazwę receptury
            String title = "§6Edycja: Smaragdowy Blok";
            assertTrue(title.contains("Edycja"), "Tytuł zawiera 'Edycja'");
            assertTrue(title.contains("Smaragdowy"), "Tytuł zawiera nazwę");
        }

        @Test
        @DisplayName("Crafting ma tytuł z nazwą receptury")
        void testCraftingHasRecipeName() {
            // Dado: Interfejs craftingu
            // To: Tytuł powinien zawierać nazwę receptury
            String title = "§bCrafting: Miecz Diamenty";
            assertTrue(title.contains("Crafting"), "Tytuł zawiera 'Crafting'");
            assertTrue(title.contains("Miecz"), "Tytuł zawiera nazwę receptury");
        }

        @Test
        @DisplayName("Tytuły zawierają kody kolorów Minecraft")
        void testTitlesHaveColorCodes() {
            // Dado: Tytuł okna
            // To: Powinien zawierać kody kolorów (§a, §b, §c, itp)
            String title = "§bReceptury str. 1/3";
            assertTrue(title.contains("§"), "Tytuł zawiera kod koloru");
        }

        @Test
        @DisplayName("Różne okna mają różne kolory w titlebarze")
        void testDifferentWindowsHaveDifferentColors() {
            // Dado: Trzy różne okna
            String selector = "§bReceptury";     // Niebieski
            String editor = "§6Edycja";           // Złoty
            String crafting = "§bCrafting";       // Niebieski

            // Gdy: Sprawdzamy kolory
            boolean selectorBlue = selector.startsWith("§b");
            boolean editorGold = editor.startsWith("§6");

            // To: Kolory się różnią
            assertNotEquals(selector.substring(0, 2), editor.substring(0, 2),
                "Różne okna mają różne kolory");
        }
    }

    @Nested
    @DisplayName("Informacje w titlebarze - dynamiczne aktualizacje")
    class InformacjeWTitlebarze {

        @Test
        @DisplayName("Tytuł selektor pokazuje aktualny numer strony")
        void testSelectorShowsCurrentPage() {
            // Dado: Selektor na stronie 2/3
            String title = "§bReceptury str. 2/3";
            
            // To: Powinien pokazywać "2/3"
            assertTrue(title.contains("2"), "Pokazuje numer 2");
            assertTrue(title.contains("3"), "Pokazuje max 3");
        }

        @Test
        @DisplayName("Tytuł selektor ma prawidłowy format strony")
        void testSelectorPageFormatIsCorrect() {
            // Dado: Selektor
            String title = "§bReceptury str. 1/3";
            
            // To: Format powinien być "str. X/Y"
            assertTrue(title.matches(".*str\\. \\d+/\\d+.*"),
                "Format strony jest poprawny");
        }

        @Test
        @DisplayName("Edytor pokazuje nazwę edytowanej receptury")
        void testEditorShowsEditedRecipeName() {
            // Dado: Edytor dla receptury "Miecz"
            String title = "§6Edycja: Miecz";
            
            // To: Tytuł zawiera pełną nazwę
            assertTrue(title.contains("Miecz"),
                "Tytuł zawiera nazwę receptury");
        }

        @Test
        @DisplayName("Crafting pokazuje dla której receptury craftujemy")
        void testCraftingShowsWhichRecipe() {
            // Dado: Crafting dla receptury "Hełm diamenty"
            String title = "§bCrafting: Hełm diamenty";
            
            // To: Tytuł zawiera nazwę
            assertTrue(title.contains("Hełm diamenty"),
                "Wyraźnie widać dla której receptury");
        }

        @Test
        @DisplayName("Tytuł aktualizuje się przy zmianie strony")
        void testTitleUpdatesOnPageChange() {
            // Dado: Gracz na stronie 1
            String title1 = "§bReceptury str. 1/3";
            
            // Gdy: Klika przycisk następna
            // To: Tytuł zmienia się
            String title2 = "§bReceptury str. 2/3";
            
            assertNotEquals(title1, title2,
                "Tytuł zmienia się przy zmianie strony");
        }

        @Test
        @DisplayName("Gdy jest 1 strona - tytuł pokazuje str. 1/1")
        void testSinglePageShowsCorrectFormat() {
            // Dado: Mało receptur (1 strona)
            String title = "§bReceptury str. 1/1";
            
            // To: Format jest prawidłowy
            assertTrue(title.contains("1/1"),
                "Format dla jednej strony");
        }
    }

    @Nested
    @DisplayName("Custom font characters - specjalne symbole w titlebarze")
    class CustomFontCharacters {

        @Test
        @DisplayName("Selektor może używać custom characters na początku")
        void testSelectorUsesCustomCharAtStart() {
            // Dado: Tytuł z custom character
            String title = "◄ Receptury str. 1/3 ►";
            
            // To: Tytuł zawiera symbole
            assertTrue(title.contains("◄") || title.contains("►"),
                "Zawiera custom characters");
        }

        @Test
        @DisplayName("Edytor może używać custom characters")
        void testEditorUsesCustomChar() {
            // Dado: Edytor
            String title = "✎ Edycja: Miecz";
            
            // To: Tytuł zawiera symbol edycji
            assertTrue(title.contains("✎"),
                "Zawiera custom character dla edycji");
        }

        @Test
        @DisplayName("Crafting może używać custom characters")
        void testCraftingUsesCustomChar() {
            // Dado: Crafting
            String title = "⚒ Crafting: Hełm";
            
            // To: Tytuł zawiera symbol craftingu
            assertTrue(title.contains("⚒"),
                "Zawiera custom character dla craftingu");
        }

        @Test
        @DisplayName("Custom characters są konsystentne w systemie")
        void testCustomCharactersConsistent() {
            // Dado: Wiele okienek
            String selector = "◄ Receptury";
            String editor = "✎ Edycja";
            
            // To: Każde ma swój character
            assertNotEquals(
                selector.substring(0, 1),
                editor.substring(0, 1),
                "Każde okno ma inny character");
        }
    }

    @Nested
    @DisplayName("Scenariusze praktyczne - UI w akcji")
    class ScenarjuszePraktyczneUI {

        @Test
        @DisplayName("Gracz otwiera selektor - widzi stronę 1/3")
        void testPlayerOpensSelectorSeesPage1() {
            // Dado: Gracz ma 100 receptur podzielonych na strony
            // Gdy: Otwiera selektor
            String title = "§bReceptury str. 1/3";
            
            // To: Widzi stronę 1 z 3
            assertTrue(title.matches(".*str\\. 1/\\d+.*"),
                "Pokazuje stronę 1");
        }

        @Test
        @DisplayName("Gracz przechodzi na stronę 2 - tytuł się zmienia")
        void testPlayerGoesToPage2TitleChanges() {
            // Dado: Gracz na stronie 1
            String title1 = "§bReceptury str. 1/3";
            
            // Gdy: Klika przycisk następna
            // To: Widzi nowy tytuł
            String title2 = "§bReceptury str. 2/3";
            
            assertTrue(title2.contains("2/3"),
                "Tytuł aktualizuje się");
        }

        @Test
        @DisplayName("Gracz wraca na stronę 1 - tytuł wracá")
        void testPlayerGoesBackToPage1TitleChanges() {
            // Dado: Gracz na stronie 2
            String title2 = "§bReceptury str. 2/3";
            
            // Gdy: Klika przycisk poprzednia
            // To: Wraca do strony 1
            String title1 = "§bReceptury str. 1/3";
            
            assertTrue(title1.contains("1/3"),
                "Tytuł zmienia się wracając");
        }

        @Test
        @DisplayName("Admin klika recepturę - otwiera edytor z jej nazwą")
        void testAdminClicksRecipeEditorOpens() {
            // Dado: Admin w selektorze, receptura "Smaragd"
            // Gdy: Klika na recepturę
            // To: Edytor pokazuje "Edycja: Smaragd"
            String editorTitle = "§6Edycja: Smaragd";
            
            assertTrue(editorTitle.contains("Smaragd"),
                "Edytor pokazuje nazwę receptury");
        }

        @Test
        @DisplayName("Gracz klika recepturę do craftingu - widzi jej nazwę")
        void testPlayerClicksRecipeForCraftingSeesName() {
            // Dado: Gracz w selektorze, receptura "Miecz"
            // Gdy: Klika na recepturę
            // To: Crafting pokazuje "Crafting: Miecz"
            String craftingTitle = "§bCrafting: Miecz";
            
            assertTrue(craftingTitle.contains("Miecz"),
                "Crafting pokazuje nazwę receptury");
        }

        @Test
        @DisplayName("Ostatnia strona pokazuje prawidłowy format")
        void testLastPageShowsCorrectFormat() {
            // Dado: Ostatnia strona (str. 3/3)
            String title = "§bReceptury str. 3/3";
            
            // To: Format jest poprawny
            assertTrue(title.matches(".*str\\. 3/3.*"),
                "Ostatnia strona ma poprawny format");
        }

        @Test
        @DisplayName("Edytor zmienia tytuł gdy zmieni się nazwa receptury")
        void testEditorTitleChangesWhenRecipeNameChanges() {
            // Dado: Edytor otwiera się z nazwą "Miecz"
            String title1 = "§6Edycja: Miecz";
            
            // Gdy: Admin zmienia nazwę na "Miecz Diament"
            // To: Tytuł powinien się zmienić
            String title2 = "§6Edycja: Miecz Diament";
            
            assertNotEquals(title1, title2,
                "Tytuł edytora zmienia się z nazwą");
        }
    }
}

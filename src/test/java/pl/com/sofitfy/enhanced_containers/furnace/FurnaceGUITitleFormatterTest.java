package pl.com.sofitfy.enhanced_containers.furnace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy dla FurnaceGUITitleFormatter
 * Weryfikuje formatowanie tytułu GUI pieca z custom font
 */
@DisplayName("FurnaceGUITitleFormatter Tests")
class FurnaceGUITitleFormatterTest {
    
    @Nested
    @DisplayName("Title Creation")
    class TitleCreation {
        
        @Test
        @DisplayName("createFurnaceTitle should return non-empty string")
        void createFurnaceTitleShouldReturnNonEmptyString() {
            String title = FurnaceGUITitleFormatter.createFurnaceTitle();
            assertNotNull(title);
            assertFalse(title.isEmpty(), "Title should not be empty");
        }
        
        @Test
        @DisplayName("createFurnaceTitle should contain furnace font character")
        void createFurnaceTitleShouldContainFurnaceFontChar() {
            String title = FurnaceGUITitleFormatter.createFurnaceTitle();
            assertTrue(title.contains(FurnaceGUITitleFormatter.FURNACE_FONT_CHAR),
                "Title should contain furnace font character \\uE002");
        }
        
        @Test
        @DisplayName("createFurnaceTitle should return consistent results")
        void createFurnaceTitleShouldReturnConsistentResults() {
            String title1 = FurnaceGUITitleFormatter.createFurnaceTitle();
            String title2 = FurnaceGUITitleFormatter.createFurnaceTitle();
            assertEquals(title1, title2, "Multiple calls should return the same title");
        }
    }
    
    @Nested
    @DisplayName("Font Character")
    class FontCharacter {
        
        @Test
        @DisplayName("FURNACE_FONT_CHAR should be \\uE002")
        void furnaceFontCharShouldBeE002() {
            assertEquals("\uE002", FurnaceGUITitleFormatter.FURNACE_FONT_CHAR);
        }
        
        @Test
        @DisplayName("FURNACE_FONT_CHAR should be different from crafting font char")
        void furnaceFontCharShouldBeDifferentFromCraftingFontChar() {
            // Crafting używa \uE001, furnace powinien używać \uE002
            assertNotEquals("\uE001", FurnaceGUITitleFormatter.FURNACE_FONT_CHAR,
                "Furnace font char should be different from crafting font char");
        }
    }
    
    @Nested
    @DisplayName("Title Structure")
    class TitleStructure {
        
        @Test
        @DisplayName("Title should contain negative space for cursor positioning")
        void titleShouldContainNegativeSpaceForCursorPositioning() {
            String title = FurnaceGUITitleFormatter.createFurnaceTitle();
            
            // Tytuł powinien zawierać kombinację znaków do pozycjonowania kursora
            // Sprawdzamy czy nie jest to po prostu sam znak fontu
            assertTrue(title.length() > FurnaceGUITitleFormatter.FURNACE_FONT_CHAR.length(),
                "Title should contain more than just the font character");
        }
    }
}

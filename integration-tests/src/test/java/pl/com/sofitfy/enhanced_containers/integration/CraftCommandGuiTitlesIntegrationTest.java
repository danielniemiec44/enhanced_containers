package pl.com.sofitfy.enhanced_containers.integration;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import pl.com.sofitfy.enhanced_containers.EnhancedCraftingMain;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.CraftingRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne (MockBukkit): tytuły GUI z custom font teksturą.
 *
 * Wymaganie od użytkownika:
 * - Po wyrenderowaniu tekstury (\uE001) cofamy kursor (169 powtórzeń \uF808).
 * - Tekst tytułu nadal ma pochodzić z configu (dla craftingu) lub z domyślnych tytułów (dla edytora/selektora).
 *
 * Komentarze po polsku i prostym językiem.
 * Teksty widoczne w grze są po angielsku.
 */
@Tag("integration")
@DisplayName("Integration - tytuły GUI /4x4 new i /4x4 edit")
class CraftCommandGuiTitlesIntegrationTest {

    private ServerMock server;
    private EnhancedCraftingMain plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EnhancedCraftingMain.class);
    }

    @AfterEach
    void tearDown() {
        try {
            RecipeManager.getInstance().clearRecipes();
        } catch (Exception ignored) {
            // Ignorujemy - to tylko sprzątanie testów.
        }
        MockBukkit.unmock();
    }

    private static int countChar(String text, char searched) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == searched) {
                count++;
            }
        }
        return count;
    }

    @Test
    @DisplayName("/4x4 new otwiera edytor z teksturą i dużym cofnięciem po teksturze")
    void fourByFourNewOpensEditorWithTextureAndLargeBackshift() {
        Player player = server.addPlayer();
        player.addAttachment(plugin, "plethoras.tools.use", true);
        player.addAttachment(plugin, "plethoras.tools.admin", true);

        boolean executed = server.dispatchCommand(player, "4x4 new");
        assertTrue(executed, "Komenda /4x4 new powinna się wykonać");

        assertNotNull(player.getOpenInventory(), "GUI powinno być otwarte");

        String title = player.getOpenInventory().getTitle();
        assertNotNull(title, "Tytuł nie powinien być null");

        assertTrue(title.contains("\uE001"), "Tytuł edytora powinien zawierać znak custom font (tekstura)");
        assertTrue(title.contains("Recipe Editor"), "Tytuł edytora powinien zawierać tekst po angielsku");

        // Ważne: po teksturze cofamy kursor bardzo dużo.
        // Nie sprawdzamy dokładnie pozycji w pikselach, bo to zależy od resource packa.
        // Sprawdzamy, że ilość cofnięć jest duża, więc tekst nie jest "przyklejony" do obrazka.
        int cursorBackCount = countChar(title, '\uF808');
        assertTrue(cursorBackCount >= 169, "Tytuł edytora powinien mieć co najmniej 169 znaków cofnięcia");
    }

    @Test
    @DisplayName("/4x4 edit otwiera selektor z normalnym tytułem (bez custom font)")
    void fourByFourEditOpensSelectorWithNormalTitle() {
        // Przygotowanie: musi istnieć chociaż jedna custom receptura, inaczej komenda /4x4 edit kończy się komunikatem.
        CraftingRecipe recipe = new CraftingRecipe(
            "test_recipe",
            "Test",
            new ItemStack[4][4],
            new ItemStack(Material.STONE),
            "plethoras.tools.use"
        );
        RecipeManager.getInstance().registerRecipe(recipe);

        Player player = server.addPlayer();
        player.addAttachment(plugin, "plethoras.tools.use", true);
        player.addAttachment(plugin, "plethoras.tools.admin", true);

        boolean executed = server.dispatchCommand(player, "4x4 edit");
        assertTrue(executed, "Komenda /4x4 edit powinna się wykonać");

        assertNotNull(player.getOpenInventory(), "GUI powinno być otwarte");

        String title = player.getOpenInventory().getTitle();
        assertNotNull(title, "Tytuł nie powinien być null");

        // RecipeSelectionGUI celowo NIE używa custom font (tekstury) - tylko CraftingGUI i RecipeEditorGUI używają.
        // Selektor receptur ma prosty, czytelny tytuł.
        assertTrue(title.contains("Recipe Selection"), "Tytuł selektora powinien zawierać 'Recipe Selection'");
        
        // Tytuł powinien mieć formatowanie kolorów (§6§l)
        assertTrue(title.contains("§6") || title.contains("§l"), 
            "Tytuł selektora powinien mieć formatowanie kolorów");
    }
}

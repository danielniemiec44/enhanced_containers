package pl.com.sofitfy.enhanced_containers.integration;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import pl.com.sofitfy.enhanced_containers.Main;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne: tytuł GUI craftingu.
 *
 * Cel:
 * - Po otwarciu /4x4 tytuł ma zawierać custom font teksturę (\uE001)
 * - I ma zawierać normalny tekst z configu (np. "Crafting table")
 *
 * Komentarze po polsku i prostym językiem.
 */
@DisplayName("Integration - tytuł crafting GUI")
class CraftingGUITitleIntegrationTest {

    private ServerMock server;
    private Main plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Main.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Tag("integration")
    @Test
    @DisplayName("/4x4 otwiera GUI z tytułem zawierającym teksturę i tekst z configu")
    void craftingGuiTitleContainsTextureAndConfigText() {
        Player player = server.addPlayer();
        player.addAttachment(plugin, "plethoras.tools.use", true);

        boolean executed = server.dispatchCommand(player, "4x4");
        assertTrue(executed, "Komenda /4x4 powinna się wykonać");

        assertNotNull(player.getOpenInventory(), "GUI powinno być otwarte");

        // W MockBukkit tytuł jest dostępny jako String.
        // Nie sprawdzamy tutaj kolorów (to zależy od klienta), tylko obecność treści.
        String title = player.getOpenInventory().getTitle();
        assertNotNull(title, "Tytuł nie powinien być null");

        assertTrue(title.contains("\uE001"), "Tytuł powinien zawierać znak custom font (tekstura)");
        assertTrue(title.contains("Crafting table"), "Tytuł powinien zawierać tekst z configu");

        // Dodatkowo: po teksturze cofamy kursor (169 powtórzeń \uF808).
        // To ma zapobiec sytuacji, gdzie tekst jest "przyklejony" do obrazka.
        int cursorBackCount = 0;
        for (int i = 0; i < title.length(); i++) {
            if (title.charAt(i) == '\uF808') {
                cursorBackCount++;
            }
        }
        assertTrue(cursorBackCount >= 169, "Tytuł powinien mieć co najmniej 169 znaków cofnięcia");
    }
}

package pl.com.sofitfy.enhanced_containers.testutils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager;

/**
 * Wspólna baza dla testów z MockBukkit.
 *
 * Dlaczego to jest potrzebne:
 * - MockBukkit musi być uruchomiony przed testem i wyłączony po teście.
 * - W testach integracyjnych często potrzebujemy też załadować plugin.
 *
 * Uwaga:
 * - Komentarze są po polsku (prosty język), bo to jest kod testowy.
 */
public abstract class MockBukkitTestBase {

    protected ServerMock server;

    @BeforeEach
    protected void baseSetUp() {
        // Startujemy fake serwer tylko jeśli jeszcze nie istnieje
        if (MockBukkit.isMocked()) {
            server = MockBukkit.getMock();
        } else {
            server = MockBukkit.mock();
        }
    }

    @AfterEach
    protected void baseTearDown() {
        // Czyścimy custom receptury dodane podczas testu - izolacja testów
        try {
            RecipeManager.getInstance().clearRecipes();
        } catch (Exception ignored) {
            // Ignoruj - może nie być zainicjalizowany
        }
        
        // Sprzątamy po teście (ważne, bo MockBukkit ma stan globalny).
        MockBukkit.unmock();
    }

    protected PlayerMock createPlayer() {
        return server.addPlayer();
    }

}

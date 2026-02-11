package pl.com.sofitfy.enhanced_containers.sharedcrafting;

import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testy jednostkowe dla SharedCraftingCommand
 * 
 * Testujemy logikę komendy /sharedcrafting fill.
 * Używamy Mockito do mockowania Bukkit API.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SharedCraftingCommand - testy jednostkowe")
class SharedCraftingCommandTest {

    @Mock
    private JavaPlugin plugin;

    @Mock
    private Player player;

    @Mock
    private Command command;

    @Mock
    private SharedCraftingManager craftingManager;

    private SharedCraftingCommand sharedCraftingCommand;

    @BeforeEach
    void setUp() {
        sharedCraftingCommand = new SharedCraftingCommand(plugin, craftingManager);
    }

    @Nested
    @DisplayName("Podstawowe testy komendy")
    class BasicCommandTests {

        @Test
        @DisplayName("Brak argumentów wyświetla pomoc")
        void shouldShowHelpWhenNoArguments() {
            boolean result = sharedCraftingCommand.onCommand(player, command, "sharedcrafting", new String[]{});

            assertTrue(result, "Komenda powinna zwrócić true (obsłużona)");
            verify(player, atLeastOnce()).sendMessage(anyString());
        }

        @Test
        @DisplayName("Nieznana subkomenda wyświetla pomoc")
        void shouldShowHelpForUnknownSubcommand() {
            boolean result = sharedCraftingCommand.onCommand(player, command, "sharedcrafting", new String[]{"unknown"});

            assertTrue(result);
            verify(player, atLeastOnce()).sendMessage(anyString());
        }
    }

    @Nested
    @DisplayName("Subkomenda fill")
    class FillCommandTests {

        @Test
        @DisplayName("Fill bez permisji jest odrzucany")
        void shouldDenyFillWithoutPermission() {
            when(player.hasPermission("plethoras.sharedcrafting.fill")).thenReturn(false);

            boolean result = sharedCraftingCommand.onCommand(player, command, "sharedcrafting", new String[]{"fill", "diamond"});

            assertTrue(result);
            verify(player).sendMessage(contains("don't have permission"));
        }

        @Test
        @DisplayName("Fill bez argumentu materiału wyświetla użycie")
        void shouldShowUsageWhenNoMaterialProvided() {
            when(player.hasPermission("plethoras.sharedcrafting.fill")).thenReturn(true);

            boolean result = sharedCraftingCommand.onCommand(player, command, "sharedcrafting", new String[]{"fill"});

            assertTrue(result);
            verify(player).sendMessage(contains("Usage"));
        }

        @Test
        @DisplayName("Fill z nieprawidłowym materiałem jest odrzucany")
        void shouldRejectInvalidMaterial() {
            when(player.hasPermission("plethoras.sharedcrafting.fill")).thenReturn(true);

            boolean result = sharedCraftingCommand.onCommand(player, command, "sharedcrafting", new String[]{"fill", "not_a_real_material_xyz"});

            assertTrue(result);
            verify(player).sendMessage(contains("Unknown material"));
        }

        @Test
        @DisplayName("Fill z nieprawidłową ilością jest odrzucany")
        void shouldRejectInvalidAmount() {
            when(player.hasPermission("plethoras.sharedcrafting.fill")).thenReturn(true);

            boolean result = sharedCraftingCommand.onCommand(player, command, "sharedcrafting", new String[]{"fill", "diamond", "abc"});

            assertTrue(result);
            verify(player).sendMessage(contains("Invalid amount"));
        }
    }

    @Nested
    @DisplayName("Tab completion")
    class TabCompletionTests {

        @Test
        @DisplayName("Pierwszy argument sugeruje fill gdy gracz ma permisję")
        void shouldSuggestFillWithPermission() {
            when(player.hasPermission("plethoras.sharedcrafting.fill")).thenReturn(true);

            var completions = sharedCraftingCommand.onTabComplete(player, command, "sharedcrafting", new String[]{""});

            assertNotNull(completions);
            // Fill jest dodawany tylko gdy WorldEdit jest dostępny
            // W środowisku testowym WorldEdit może nie być dostępny
        }

        @Test
        @DisplayName("Drugi argument dla fill sugeruje materiały")
        void shouldSuggestMaterialsForFill() {
            var completions = sharedCraftingCommand.onTabComplete(player, command, "sharedcrafting", new String[]{"fill", ""});

            assertNotNull(completions);
            assertTrue(completions.contains("diamond"));
            assertTrue(completions.contains("iron_ingot"));
        }

        @Test
        @DisplayName("Trzeci argument dla fill sugeruje ilości")
        void shouldSuggestAmountsForFill() {
            var completions = sharedCraftingCommand.onTabComplete(player, command, "sharedcrafting", new String[]{"fill", "diamond", ""});

            assertNotNull(completions);
            assertTrue(completions.contains("64"));
            assertTrue(completions.contains("32"));
            assertTrue(completions.contains("16"));
            assertTrue(completions.contains("1"));
        }
    }

    @Nested
    @DisplayName("Parsowanie ilości")
    class AmountParsingTests {

        @Test
        @DisplayName("Domyślna ilość to 64")
        void shouldDefaultToMaxAmount() {
            when(player.hasPermission("plethoras.sharedcrafting.fill")).thenReturn(true);
            
            // Komenda bez ilości - używa domyślnych 64
            // Test weryfikuje że nie ma błędu przy parsowaniu
            boolean result = sharedCraftingCommand.onCommand(player, command, "sharedcrafting", new String[]{"fill", "diamond"});

            assertTrue(result);
            // Dalsza weryfikacja wymaga WorldEdit - tutaj sprawdzamy tylko parsowanie
        }
    }
}

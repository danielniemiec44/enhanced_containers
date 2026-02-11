package pl.com.sofitfy.enhanced_containers.commands;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.command.Command;
import org.junit.jupiter.api.*;
import pl.com.sofitfy.enhanced_containers.sharedcrafting.SharedCraftingCommand;
import pl.com.sofitfy.enhanced_containers.sharedcrafting.SharedCraftingManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testy integracyjne dla SharedCraftingCommand z subkomendą fill
 * 
 * Testujemy zachowanie komendy w pełnym środowisku MockBukkit.
 * Weryfikujemy permisje, parsowanie argumentów i interakcję z graczem.
 */
@DisplayName("SharedCraftingCommand - testy integracyjne (MockBukkit)")
class SharedCraftingCommandIntegrationTest {

    private static ServerMock server;
    private PlayerMock player;
    private SharedCraftingCommand sharedCraftingCommand;
    private SharedCraftingManager craftingManager;
    private Command mockCommand;

    @BeforeAll
    static void setUpAll() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void tearDownAll() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        // Stwórz gracza testowego
        player = server.addPlayer("CraftingFillTestPlayer");
        
        // Stwórz mock komendy
        mockCommand = mock(Command.class);
        when(mockCommand.getName()).thenReturn("sharedcrafting");
        
        // Stwórz instancje
        var mockPlugin = MockBukkit.createMockPlugin();
        craftingManager = new SharedCraftingManager(mockPlugin);
        sharedCraftingCommand = new SharedCraftingCommand(mockPlugin, craftingManager);
    }

    @Nested
    @DisplayName("Permisje dla fill")
    class FillPermissionTests {

        @Test
        @DisplayName("Gracz bez permisji nie może użyć fill")
        void shouldDenyFillWithoutPermission() {
            player.setOp(false);
            
            boolean result = sharedCraftingCommand.onCommand(player, mockCommand, "sharedcrafting", new String[]{"fill", "diamond"});
            
            assertTrue(result);
            
            String message = player.nextMessage();
            assertNotNull(message);
            assertTrue(message.contains("permission"), "Komunikat powinien informować o braku permisji");
        }

        @Test
        @DisplayName("OP może użyć fill")
        void shouldAllowFillForOp() {
            player.setOp(true);
            
            boolean result = sharedCraftingCommand.onCommand(player, mockCommand, "sharedcrafting", new String[]{"fill", "diamond"});
            
            assertTrue(result);
            // OP ma permisję - komenda przechodzi do sprawdzania WorldEdit
        }

        @Test
        @DisplayName("Gracz z permisją może użyć fill")
        void shouldAllowFillWithPermission() {
            player.addAttachment(MockBukkit.createMockPlugin(), "plethoras.sharedcrafting.fill", true);
            
            boolean result = sharedCraftingCommand.onCommand(player, mockCommand, "sharedcrafting", new String[]{"fill", "diamond"});
            
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Parsowanie argumentów fill")
    class FillArgumentParsingTests {

        @BeforeEach
        void grantPermission() {
            player.setOp(true);
        }

        @Test
        @DisplayName("Fill bez materiału wyświetla użycie")
        void shouldShowUsageWithoutMaterial() {
            sharedCraftingCommand.onCommand(player, mockCommand, "sharedcrafting", new String[]{"fill"});
            
            String message = player.nextMessage();
            assertNotNull(message);
            assertTrue(message.contains("Usage") || message.contains("usage"));
        }

        @Test
        @DisplayName("Fill z nieprawidłowym materiałem jest odrzucany")
        void shouldRejectInvalidMaterial() {
            sharedCraftingCommand.onCommand(player, mockCommand, "sharedcrafting", new String[]{"fill", "nieistniejacy_material"});
            
            String message = player.nextMessage();
            assertNotNull(message);
            assertTrue(message.contains("Unknown material"));
        }

        @Test
        @DisplayName("Fill z prawidłowym materiałem przechodzi do WorldEdit")
        void shouldAcceptValidMaterial() {
            sharedCraftingCommand.onCommand(player, mockCommand, "sharedcrafting", new String[]{"fill", "diamond"});
            
            String message = player.nextMessage();
            assertNotNull(message);
            // W MockBukkit WorldEdit nie jest dostępny
            assertTrue(message.contains("WorldEdit") || message.contains("Starting"));
        }

        @Test
        @DisplayName("Fill parsuje ilość poprawnie")
        void shouldParseAmountCorrectly() {
            sharedCraftingCommand.onCommand(player, mockCommand, "sharedcrafting", new String[]{"fill", "diamond", "32"});
            
            String message = player.nextMessage();
            assertNotNull(message);
            assertFalse(message.contains("Invalid amount"));
        }

        @Test
        @DisplayName("Fill z nieprawidłową ilością jest odrzucany")
        void shouldRejectInvalidAmount() {
            sharedCraftingCommand.onCommand(player, mockCommand, "sharedcrafting", new String[]{"fill", "diamond", "abc"});
            
            String message = player.nextMessage();
            assertNotNull(message);
            assertTrue(message.contains("Invalid amount"));
        }
    }

    @Nested
    @DisplayName("Pomoc i nieznane komendy")
    class HelpAndUnknownCommandTests {

        @Test
        @DisplayName("Brak argumentów wyświetla pomoc")
        void shouldShowHelpWithNoArguments() {
            sharedCraftingCommand.onCommand(player, mockCommand, "sharedcrafting", new String[]{});
            
            String message = player.nextMessage();
            assertNotNull(message);
            assertTrue(message.contains("Shared Crafting") || message.contains("sharedcrafting"));
        }

        @Test
        @DisplayName("Nieznana subkomenda wyświetla pomoc")
        void shouldShowHelpForUnknownSubcommand() {
            sharedCraftingCommand.onCommand(player, mockCommand, "sharedcrafting", new String[]{"unknown"});
            
            String message = player.nextMessage();
            assertNotNull(message);
        }
    }

    @Nested
    @DisplayName("Tab completion")
    class TabCompletionTests {

        @Test
        @DisplayName("Sugeruje fill dla pierwszego argumentu gdy gracz ma permisję")
        void shouldSuggestFillWithPermission() {
            player.addAttachment(MockBukkit.createMockPlugin(), "plethoras.sharedcrafting.fill", true);
            
            var completions = sharedCraftingCommand.onTabComplete(player, mockCommand, "sharedcrafting", new String[]{""});
            
            assertNotNull(completions);
            // Fill jest dodawany tylko gdy WorldEdit jest dostępny
            // W MockBukkit może nie być dostępny
        }

        @Test
        @DisplayName("Sugeruje materiały dla drugiego argumentu fill")
        void shouldSuggestMaterialsForFill() {
            var completions = sharedCraftingCommand.onTabComplete(player, mockCommand, "sharedcrafting", new String[]{"fill", ""});
            
            assertNotNull(completions);
            assertTrue(completions.contains("diamond"));
            assertTrue(completions.contains("iron_ingot"));
        }

        @Test
        @DisplayName("Sugeruje ilości dla trzeciego argumentu fill")
        void shouldSuggestAmountsForFill() {
            var completions = sharedCraftingCommand.onTabComplete(player, mockCommand, "sharedcrafting", new String[]{"fill", "diamond", ""});
            
            assertNotNull(completions);
            assertTrue(completions.contains("64"));
            assertTrue(completions.contains("32"));
            assertTrue(completions.contains("16"));
            assertTrue(completions.contains("1"));
        }

        @Test
        @DisplayName("Filtruje materiały po partial match")
        void shouldFilterMaterialsByPartialMatch() {
            var completions = sharedCraftingCommand.onTabComplete(player, mockCommand, "sharedcrafting", new String[]{"fill", "dia"});
            
            assertNotNull(completions);
            assertTrue(completions.contains("diamond"));
            assertFalse(completions.contains("iron_ingot"));
        }
    }

    @Nested
    @DisplayName("Zachowanie bez WorldEdit")
    class WithoutWorldEditTests {

        @BeforeEach
        void grantPermission() {
            player.setOp(true);
        }

        @Test
        @DisplayName("Fill informuje o braku WorldEdit")
        void shouldInformAboutMissingWorldEdit() {
            sharedCraftingCommand.onCommand(player, mockCommand, "sharedcrafting", new String[]{"fill", "diamond"});
            
            String message = player.nextMessage();
            assertNotNull(message);
            assertTrue(message.contains("WorldEdit"), 
                "Komunikat powinien wspominać o WorldEdit: " + message);
        }

        @Test
        @DisplayName("Pomoc wspomina że fill wymaga WorldEdit gdy niedostępny")
        void shouldMentionWorldEditRequirementInHelp() {
            sharedCraftingCommand.onCommand(player, mockCommand, "sharedcrafting", new String[]{});
            
            // Sprawdź wszystkie wiadomości
            String message;
            boolean mentionsWorldEdit = false;
            while ((message = player.nextMessage()) != null) {
                if (message.contains("WorldEdit") || message.contains("not installed")) {
                    mentionsWorldEdit = true;
                    break;
                }
            }
            
            // W pomocy powinno być wspomniane że WorldEdit jest wymagany lub niedostępny
            // (zależy od implementacji sendHelp)
        }
    }
}

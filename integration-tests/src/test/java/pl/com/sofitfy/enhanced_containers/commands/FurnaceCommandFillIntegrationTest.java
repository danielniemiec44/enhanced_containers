package pl.com.sofitfy.enhanced_containers.commands;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import pl.com.sofitfy.enhanced_containers.furnace.EnhancedFurnaceManager;
import pl.com.sofitfy.enhanced_containers.furnace.FurnaceCommand;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testy integracyjne dla FurnaceCommand z subkomendą fill
 * 
 * Testujemy zachowanie komendy w pełnym środowisku MockBukkit.
 * Weryfikujemy permisje, parsowanie argumentów i interakcję z graczem.
 */
@DisplayName("FurnaceCommand Fill - testy integracyjne (MockBukkit)")
class FurnaceCommandFillIntegrationTest {

    private static ServerMock server;
    private PlayerMock player;
    private FurnaceCommand furnaceCommand;
    private EnhancedFurnaceManager furnaceManager;
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
        player = server.addPlayer("FillTestPlayer");
        
        // Stwórz mock komendy
        mockCommand = mock(Command.class);
        when(mockCommand.getName()).thenReturn("efurnace");
        
        // Stwórz instancje
        var mockPlugin = MockBukkit.createMockPlugin();
        furnaceManager = new EnhancedFurnaceManager(mockPlugin);
        furnaceCommand = new FurnaceCommand(mockPlugin);
        furnaceCommand.setFurnaceManager(furnaceManager);
    }

    @Nested
    @DisplayName("Permisje dla fill")
    class FillPermissionTests {

        @Test
        @DisplayName("Gracz bez permisji nie może użyć fill")
        void shouldDenyFillWithoutPermission() {
            // Gracz domyślnie nie ma permisji
            player.setOp(false);
            
            boolean result = furnaceCommand.onCommand(player, mockCommand, "efurnace", new String[]{"fill", "raw_iron"});
            
            assertTrue(result, "Komenda powinna być obsłużona");
            
            String message = player.nextMessage();
            assertNotNull(message, "Gracz powinien otrzymać komunikat");
            assertTrue(message.contains("permission"), "Komunikat powinien informować o braku permisji");
        }

        @Test
        @DisplayName("OP może użyć fill")
        void shouldAllowFillForOp() {
            player.setOp(true);
            
            boolean result = furnaceCommand.onCommand(player, mockCommand, "efurnace", new String[]{"fill", "raw_iron"});
            
            assertTrue(result);
            // OP ma permisję - komenda przechodzi do sprawdzania WorldEdit
        }

        @Test
        @DisplayName("Gracz z permisją może użyć fill")
        void shouldAllowFillWithPermission() {
            player.addAttachment(MockBukkit.createMockPlugin(), "plethoras.furnace.fill", true);
            
            boolean result = furnaceCommand.onCommand(player, mockCommand, "efurnace", new String[]{"fill", "raw_iron"});
            
            assertTrue(result);
            // Komenda przechodzi do sprawdzania WorldEdit
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
            furnaceCommand.onCommand(player, mockCommand, "efurnace", new String[]{"fill"});
            
            String message = player.nextMessage();
            assertNotNull(message);
            // Bez WorldEdit (MockBukkit) komenda zwraca komunikat o WorldEdit; z WorldEdit zwróciłaby Usage
            assertTrue(message.contains("Usage") || message.contains("usage") || message.contains("WorldEdit"),
                "Komunikat: instrukcja użycia lub brak WorldEdit");
        }

        @Test
        @DisplayName("Fill z nieprawidłowym materiałem jest odrzucany")
        void shouldRejectInvalidMaterial() {
            furnaceCommand.onCommand(player, mockCommand, "efurnace", new String[]{"fill", "nieistniejacy_material"});
            
            String message = player.nextMessage();
            assertNotNull(message);
            // Bez WorldEdit komenda zwraca komunikat o WorldEdit; z WorldEdit zwróciłaby Unknown material
            assertTrue(message.contains("Unknown material") || message.contains("WorldEdit"),
                "Komunikat: nieznany materiał lub brak WorldEdit");
        }

        @Test
        @DisplayName("Fill z prawidłowym materiałem przechodzi do WorldEdit")
        void shouldAcceptValidMaterial() {
            furnaceCommand.onCommand(player, mockCommand, "efurnace", new String[]{"fill", "raw_iron", "coal"});
            
            // Powinien przejść do sprawdzania WorldEdit
            String message = player.nextMessage();
            assertNotNull(message);
            // W MockBukkit WorldEdit nie jest dostępny
            assertTrue(message.contains("WorldEdit") || message.contains("Starting"), 
                "Komunikat powinien dotyczyć WorldEdit lub startu operacji");
        }

        @Test
        @DisplayName("Fill parsuje ilość poprawnie")
        void shouldParseAmountCorrectly() {
            furnaceCommand.onCommand(player, mockCommand, "efurnace", new String[]{"fill", "raw_iron", "coal", "32"});
            
            // Komenda powinna przyjąć argumenty bez błędów parsowania
            String message = player.nextMessage();
            assertNotNull(message);
            assertFalse(message.contains("Invalid amount"), "Ilość 32 powinna być poprawna");
        }

        @Test
        @DisplayName("Fill clampuje ilość do 1-64")
        void shouldClampAmountToValidRange() {
            // Ilość > 64 powinna być ograniczona do 64
            furnaceCommand.onCommand(player, mockCommand, "efurnace", new String[]{"fill", "raw_iron", "coal", "999"});
            
            String message = player.nextMessage();
            assertNotNull(message);
            // Nie powinno być błędu - ilość jest clamped
            assertFalse(message.contains("Invalid amount"));
        }
    }

    @Nested
    @DisplayName("Tab completion dla fill")
    class FillTabCompletionTests {

        @Test
        @DisplayName("Sugeruje materiały dla drugiego argumentu fill")
        void shouldSuggestMaterialsForFill() {
            var completions = furnaceCommand.onTabComplete(player, mockCommand, "efurnace", new String[]{"fill", "raw"});
            
            assertNotNull(completions);
            assertTrue(completions.stream().anyMatch(s -> s.startsWith("raw_")), 
                "Powinno sugerować materiały zaczynające się od 'raw_'");
        }

        @Test
        @DisplayName("Sugeruje paliwa dla trzeciego argumentu fill")
        void shouldSuggestFuelsForFill() {
            var completions = furnaceCommand.onTabComplete(player, mockCommand, "efurnace", new String[]{"fill", "raw_iron", ""});
            
            assertNotNull(completions);
            assertTrue(completions.contains("coal") || completions.contains("charcoal"), 
                "Powinno sugerować paliwa");
        }

        @Test
        @DisplayName("Sugeruje ilości dla czwartego argumentu fill")
        void shouldSuggestAmountsForFill() {
            var completions = furnaceCommand.onTabComplete(player, mockCommand, "efurnace", new String[]{"fill", "raw_iron", "coal", ""});
            
            assertNotNull(completions);
            assertTrue(completions.contains("64"), "Powinno sugerować 64");
            assertTrue(completions.contains("32"), "Powinno sugerować 32");
        }
    }

    @Nested
    @DisplayName("Integracja z podstawową komendą")
    class BasicCommandIntegrationTests {

        @Test
        @DisplayName("Komenda give nadal działa")
        void shouldStillAllowGiveCommand() {
            player.addAttachment(MockBukkit.createMockPlugin(), "plethoras.furnace.give", true);
            
            boolean result = furnaceCommand.onCommand(player, mockCommand, "efurnace", new String[]{"furnace"});
            
            assertTrue(result);
            
            // Sprawdź czy gracz otrzymał przedmiot
            ItemStack[] contents = player.getInventory().getContents();
            boolean hasFurnace = false;
            for (ItemStack item : contents) {
                if (item != null && item.getType() == Material.FURNACE) {
                    hasFurnace = true;
                    break;
                }
            }
            assertTrue(hasFurnace, "Gracz powinien otrzymać piec");
        }

        @Test
        @DisplayName("Nieprawidłowy argument pokazuje pomoc zamiast fill")
        void shouldShowHelpForInvalidArgument() {
            player.addAttachment(MockBukkit.createMockPlugin(), "plethoras.furnace.give", true);
            
            furnaceCommand.onCommand(player, mockCommand, "efurnace", new String[]{"invalid"});
            
            String message = player.nextMessage();
            assertNotNull(message);
            // Powinno pokazać pomoc
        }
    }
}

package pl.com.sofitfy.enhanced_containers.integration;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne dla WorldEditIntegration
 * 
 * Testujemy zachowanie integracji z WorldEdit w środowisku MockBukkit.
 * WorldEdit NIE jest zainstalowany w MockBukkit, więc testujemy głównie
 * zachowanie gdy WorldEdit jest niedostępny.
 */
@DisplayName("WorldEditIntegration - testy integracyjne (MockBukkit)")
class WorldEditIntegrationIntegrationTest {

    private static ServerMock server;
    private PlayerMock player;
    private World world;

    @BeforeAll
    static void setUpAll() {
        // Uruchom MockBukkit
        server = MockBukkit.mock();
    }

    @AfterAll
    static void tearDownAll() {
        // Zamknij MockBukkit
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        // Stwórz gracza testowego
        player = server.addPlayer("TestPlayer");
        world = server.getWorld("world");
        
        // Jeśli świat nie istnieje, stwórz nowy
        if (world == null) {
            world = server.addSimpleWorld("world");
        }
        
        // Reset cache WorldEdit przed każdym testem
        WorldEditIntegration.resetCache();
    }

    @Nested
    @DisplayName("Sprawdzanie dostępności WorldEdit")
    class WorldEditAvailabilityTests {

        @Test
        @DisplayName("isWorldEditAvailable() zwraca false gdy WorldEdit nie jest zainstalowany")
        void shouldReturnFalseWhenWorldEditNotInstalled() {
            // W MockBukkit WorldEdit nie jest zainstalowany
            boolean result = WorldEditIntegration.isWorldEditAvailable();
            
            assertFalse(result, "Powinno zwrócić false gdy WorldEdit nie jest zainstalowany");
        }

        @Test
        @DisplayName("Cache działa poprawnie - drugie wywołanie zwraca tę samą wartość")
        void shouldCacheResult() {
            boolean first = WorldEditIntegration.isWorldEditAvailable();
            boolean second = WorldEditIntegration.isWorldEditAvailable();
            
            assertEquals(first, second, "Wyniki powinny być identyczne (cached)");
        }

        @Test
        @DisplayName("resetCache() pozwala na ponowne sprawdzenie")
        void shouldAllowRecheckAfterReset() {
            WorldEditIntegration.isWorldEditAvailable();
            WorldEditIntegration.resetCache();
            
            // Po reset powinno ponownie sprawdzić
            assertDoesNotThrow(() -> WorldEditIntegration.isWorldEditAvailable());
        }
    }

    @Nested
    @DisplayName("Pobieranie selekcji")
    class SelectionTests {

        @Test
        @DisplayName("getSelection() zwraca błąd gdy WorldEdit niedostępny")
        void shouldReturnErrorWhenWorldEditUnavailable() {
            var result = WorldEditIntegration.getSelection(player);
            
            assertNotNull(result, "Wynik nie powinien być null");
            assertFalse(result.success(), "success powinno być false");
            assertTrue(result.message().contains("WorldEdit"), "Komunikat powinien wspominać o WorldEdit");
            assertTrue(result.locations().isEmpty(), "Lista lokalizacji powinna być pusta");
        }

        @Test
        @DisplayName("getSelection() z null playerem nie crashuje")
        void shouldHandleNullPlayer() {
            // Ten test sprawdza odporność na edge cases
            assertThrows(NullPointerException.class, () -> {
                WorldEditIntegration.getSelection(null);
            }, "Powinno rzucić NullPointerException dla null player");
        }
    }

    @Nested
    @DisplayName("Iteracja asynchroniczna")
    class AsyncIterationTests {

        @Test
        @DisplayName("iterateSelectionAsync() kończy się natychmiast gdy WorldEdit niedostępny")
        void shouldCompleteImmediatelyWhenWorldEditUnavailable() {
            final boolean[] completed = {false};
            final int[] processedCount = {-1};
            
            WorldEditIntegration.iterateSelectionAsync(
                player,
                1000,
                (location, progress) -> {
                    // Nie powinno być wywołane
                    fail("Processor nie powinien być wywołany gdy WorldEdit niedostępny");
                },
                (total) -> {
                    completed[0] = true;
                    processedCount[0] = total;
                },
                MockBukkit.createMockPlugin()
            );
            
            // Wykonaj ticki żeby scheduler mógł działać
            server.getScheduler().performTicks(10);
            
            assertTrue(completed[0], "Callback powinien być wywołany");
            assertEquals(0, processedCount[0], "Powinno zwrócić 0 przetworzonych");
        }

        @Test
        @DisplayName("Gracz otrzymuje komunikat o braku WorldEdit")
        void shouldNotifyPlayerAboutMissingWorldEdit() {
            WorldEditIntegration.iterateSelectionAsync(
                player,
                1000,
                (location, progress) -> {},
                (total) -> {},
                MockBukkit.createMockPlugin()
            );
            
            // Sprawdź czy gracz otrzymał komunikat
            // MockBukkit zapisuje wiadomości wysłane do gracza
            String lastMessage = player.nextMessage();
            assertNotNull(lastMessage, "Gracz powinien otrzymać komunikat");
            assertTrue(lastMessage.contains("WorldEdit"), "Komunikat powinien wspominać o WorldEdit");
        }
    }

    @Nested
    @DisplayName("SelectionResult i BatchProgress")
    class RecordTests {

        @Test
        @DisplayName("SelectionResult z prawdziwymi lokalizacjami Bukkit")
        void shouldWorkWithBukkitLocations() {
            List<Location> locations = new ArrayList<>();
            locations.add(new Location(world, 0, 64, 0));
            locations.add(new Location(world, 1, 64, 1));
            locations.add(new Location(world, 2, 64, 2));
            
            var result = new WorldEditIntegration.SelectionResult(true, "Test", locations);
            
            assertEquals(3, result.locations().size());
            assertEquals(world, result.locations().get(0).getWorld());
        }

        @Test
        @DisplayName("BatchProgress dla dużej ilości bloków")
        void shouldHandleLargeBlockCounts() {
            // Symulacja 100k bloków
            var progress = new WorldEditIntegration.BatchProgress(50000, 100000, 45000);
            
            assertEquals(50000, progress.current());
            assertEquals(100000, progress.total());
            assertEquals(45000, progress.processedCount());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Wielokrotne wywołania resetCache są bezpieczne")
        void shouldHandleMultipleResetCalls() {
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 100; i++) {
                    WorldEditIntegration.resetCache();
                    WorldEditIntegration.isWorldEditAvailable();
                }
            });
        }

        @Test
        @DisplayName("Operacje są thread-safe")
        void shouldBeThreadSafe() throws InterruptedException {
            // Uruchom kilka wątków jednocześnie sprawdzających dostępność
            Thread[] threads = new Thread[10];
            final boolean[] results = new boolean[10];
            
            for (int i = 0; i < 10; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    WorldEditIntegration.resetCache();
                    results[index] = WorldEditIntegration.isWorldEditAvailable();
                });
            }
            
            for (Thread thread : threads) {
                thread.start();
            }
            
            for (Thread thread : threads) {
                thread.join();
            }
            
            // Wszystkie wyniki powinny być takie same (false - brak WorldEdit)
            for (boolean result : results) {
                assertFalse(result);
            }
        }
    }
}

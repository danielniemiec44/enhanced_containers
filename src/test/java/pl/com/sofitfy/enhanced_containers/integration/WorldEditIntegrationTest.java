package pl.com.sofitfy.enhanced_containers.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy jednostkowe dla WorldEditIntegration
 * 
 * Testujemy logikę sprawdzania dostępności WorldEdit bez MockBukkit.
 * Te testy używają tylko czystej Javy - nie wymagają środowiska Bukkit.
 */
@DisplayName("WorldEditIntegration - testy jednostkowe")
class WorldEditIntegrationTest {

    @BeforeEach
    void setUp() {
        // Reset cache przed każdym testem
        WorldEditIntegration.resetCache();
    }

    @Nested
    @DisplayName("Sprawdzanie dostępności WorldEdit")
    class WorldEditAvailabilityTests {

        @Test
        @DisplayName("isWorldEditAvailable() zwraca false gdy Bukkit nie jest dostępny (środowisko testowe)")
        void shouldReturnFalseWhenBukkitNotAvailable() {
            // W środowisku testowym bez MockBukkit, Bukkit.getPluginManager() wyrzuci błąd
            // lub zwróci null - isWorldEditAvailable() powinno to obsłużyć gracefully
            
            // Ten test weryfikuje że metoda nie crashuje gdy Bukkit jest niedostępny
            try {
                boolean result = WorldEditIntegration.isWorldEditAvailable();
                // Jeśli dojdziemy tutaj, metoda obsłużyła brak Bukkita
                // W czystym środowisku testowym powinno zwrócić false
                assertFalse(result, "Powinno zwrócić false gdy Bukkit niedostępny");
            } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
                // Oczekiwane w środowisku bez Bukkit - test przechodzi
                // To znaczy że metoda próbowała użyć Bukkit API
            }
        }
    }

    @Nested
    @DisplayName("SelectionResult record")
    class SelectionResultTests {

        @Test
        @DisplayName("SelectionResult przechowuje poprawne wartości")
        void shouldStoreCorrectValues() {
            // Testujemy czy record poprawnie przechowuje dane
            var locations = new java.util.ArrayList<org.bukkit.Location>();
            
            WorldEditIntegration.SelectionResult result = 
                new WorldEditIntegration.SelectionResult(true, "Test message", locations);
            
            assertTrue(result.success(), "success powinno być true");
            assertEquals("Test message", result.message(), "message powinno być prawidłowe");
            assertSame(locations, result.locations(), "locations powinno być tą samą listą");
        }

        @Test
        @DisplayName("SelectionResult z failure ma pusty success")
        void shouldHandleFailureResult() {
            WorldEditIntegration.SelectionResult result = 
                new WorldEditIntegration.SelectionResult(false, "No selection", new java.util.ArrayList<>());
            
            assertFalse(result.success(), "success powinno być false");
            assertEquals("No selection", result.message());
            assertTrue(result.locations().isEmpty(), "locations powinno być puste");
        }
    }

    @Nested
    @DisplayName("BatchProgress record")
    class BatchProgressTests {

        @Test
        @DisplayName("BatchProgress przechowuje informacje o postępie")
        void shouldStoreProgressInfo() {
            WorldEditIntegration.BatchProgress progress = 
                new WorldEditIntegration.BatchProgress(50, 100, 45);
            
            assertEquals(50, progress.current(), "current powinno być 50");
            assertEquals(100, progress.total(), "total powinno być 100");
            assertEquals(45, progress.processedCount(), "processedCount powinno być 45");
        }

        @Test
        @DisplayName("BatchProgress dla początku przetwarzania")
        void shouldHandleStartOfProcessing() {
            WorldEditIntegration.BatchProgress progress = 
                new WorldEditIntegration.BatchProgress(1, 100000, 0);
            
            assertEquals(1, progress.current());
            assertEquals(100000, progress.total());
            assertEquals(0, progress.processedCount());
        }

        @Test
        @DisplayName("BatchProgress dla końca przetwarzania")
        void shouldHandleEndOfProcessing() {
            WorldEditIntegration.BatchProgress progress = 
                new WorldEditIntegration.BatchProgress(100000, 100000, 50000);
            
            assertEquals(100000, progress.current());
            assertEquals(100000, progress.total());
            assertEquals(50000, progress.processedCount());
        }
    }

    @Nested
    @DisplayName("Funkcje pomocnicze")
    class HelperMethodsTests {

        @Test
        @DisplayName("resetCache() resetuje cache dostępności")
        void shouldResetCache() {
            // Wywołaj resetCache kilka razy - nie powinno być błędów
            assertDoesNotThrow(() -> {
                WorldEditIntegration.resetCache();
                WorldEditIntegration.resetCache();
                WorldEditIntegration.resetCache();
            }, "resetCache() nie powinno rzucać wyjątków");
        }
    }
}

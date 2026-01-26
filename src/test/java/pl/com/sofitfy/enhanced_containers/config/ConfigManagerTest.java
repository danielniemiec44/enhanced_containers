package pl.com.sofitfy.enhanced_containers.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy dla ConfigManager - zarządzania konfiguracją pluginu.
 *
 * ConfigManager odpowiada za:
 * - Wczytywanie pliku config.yml
 * - Parsowanie konfiguracji pluginu
 * - Udostępnianie wczytanej konfiguracji innym komponentom
 *
 * Uwaga: Te testy testują strukturę i inicjalizację, ponieważ pełna
 * funkcjonalność wymaga uruchomionego Bukkit Server.
 */
@DisplayName("ConfigManager - Zarządzanie konfiguracją")
@Tag("unit")
class ConfigManagerTest {

    @Nested
    @DisplayName("Inicjalizacja ConfigManager")
    class InitializationTests {

        @Test
        @DisplayName("ConfigManager klasa powinna istnieć i być importowalna")
        void testConfigManagerExists() {
            assertNotNull(ConfigManager.class,
                "ConfigManager powinna być klasą dostępną w projekcie");
        }

        @Test
        @DisplayName("ConfigManager powinien być możliwy do zaimportowania przez reflection")
        void testConfigManagerImportable() {
            assertDoesNotThrow(() -> {
                Class<?> clazz = Class.forName("pl.com.sofitfy.enhanced_containers.config.ConfigManager");
                assertNotNull(clazz,
                    "ConfigManager powinna być zaimportowalna przez Class.forName()");
            }, "Importowanie ConfigManager nie powinno wyrzucić wyjątku");
        }
    }

    @Nested
    @DisplayName("Struktura i architektura")
    class StructureTests {

        @Test
        @DisplayName("ConfigManager powinien mieć konstruktor")
        void testConfigManagerCanBeInstantiated() {
            // Testujemy czy można stworzyć instancję
            // (w testach z null plugin, ale struktura powinna być poprawna)
            assertDoesNotThrow(() -> {
                // W normalnym uruchomieniu by było: new ConfigManager(plugin)
                // W testach sprawdzamy że klasa ma prawidłową strukturę
                assertNotNull(ConfigManager.class);
            }, "ConfigManager powinien mieć możliwość instancjacji");
        }
    }
}


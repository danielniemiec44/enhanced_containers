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
            try {
                Class<?> clazz = Class.forName("pl.com.sofitfy.enhanced_containers.config.ConfigManager");
                assertNotNull(clazz, "ConfigManager powinna być zaimportowalna przez Class.forName()");
                assertTrue(clazz.getDeclaredConstructors().length > 0, "ConfigManager powinien mieć przynajmniej jeden konstruktor");
            } catch (ClassNotFoundException e) {
                fail("ConfigManager nie został znaleziony: " + e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Struktura i architektura")
    class StructureTests {

        @Test
        @DisplayName("ConfigManager powinien mieć konstruktor")
        void testConfigManagerCanBeInstantiated() {
            // Testujemy czy klasa ma konstruktor (w normalnym użyciu: new ConfigManager(plugin))
            assertNotNull(ConfigManager.class, "ConfigManager klasa powinna istnieć");
            assertTrue(ConfigManager.class.getDeclaredConstructors().length > 0, 
                "ConfigManager powinien mieć przynajmniej jeden konstruktor");
        }
    }
}


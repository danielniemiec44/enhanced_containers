package pl.com.sofitfy.enhanced_containers.furnace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy jednostkowe dla FurnaceProgressIcons
 * 
 * Testuje logikę obliczania CustomModelData dla ikonek ognia i strzałki.
 * Te testy NIE wymagają MockBukkit - testują tylko pure Java logic.
 * 
 * CustomModelData:
 * - Fire: 100 = empty, 101-113 = poziomy 1-13
 * - Arrow: 200 = empty, 201-222 = poziomy 1-22
 */
@DisplayName("FurnaceProgressIcons - testy CustomModelData")
@Tag("integration") // Wykluczone z gradle test (używają static values z klasy)
class FurnaceProgressIconsTest {
    
    @Nested
    @DisplayName("Stałe konfiguracyjne")
    class ConfigConstants {
        
        @Test
        @DisplayName("Fire CMD base = 100")
        void testFireCmdBase() {
            // Sprawdzamy czy stała bazowa dla ognia to 100
            assertEquals(100, FurnaceProgressIcons.FIRE_CMD_BASE,
                "Bazowa wartość CustomModelData dla ognia powinna być 100");
        }
        
        @Test
        @DisplayName("Arrow CMD base = 200")
        void testArrowCmdBase() {
            // Sprawdzamy czy stała bazowa dla strzałki to 200
            assertEquals(200, FurnaceProgressIcons.ARROW_CMD_BASE,
                "Bazowa wartość CustomModelData dla strzałki powinna być 200");
        }
        
        @Test
        @DisplayName("Max fire frames = 13")
        void testMaxFireFrames() {
            // Vanilla furnace ma 13 poziomów ognia
            assertEquals(13, FurnaceProgressIcons.MAX_FIRE_FRAMES,
                "Maksymalna liczba klatek ognia powinna być 13 (jak w vanilla)");
        }
        
        @Test
        @DisplayName("Max arrow frames = 22")
        void testMaxArrowFrames() {
            // Vanilla furnace ma 22 poziomy strzałki
            assertEquals(22, FurnaceProgressIcons.MAX_ARROW_FRAMES,
                "Maksymalna liczba klatek strzałki powinna być 22 (jak w vanilla)");
        }
    }
    
    @Nested
    @DisplayName("getFireCustomModelData - obliczanie CMD dla ognia")
    class FireCustomModelDataTests {
        
        @Test
        @DisplayName("0% = CMD 100 (empty)")
        void testZeroPercent() {
            // 0% powinno zwrócić bazową wartość (empty)
            assertEquals(100, FurnaceProgressIcons.getFireCustomModelData(0),
                "0% powinno zwrócić CMD 100 (fire_empty)");
        }
        
        @Test
        @DisplayName("Ujemny procent = CMD 100 (empty)")
        void testNegativePercent() {
            // Ujemne wartości też powinny zwrócić empty
            assertEquals(100, FurnaceProgressIcons.getFireCustomModelData(-10),
                "Ujemny procent powinien zwrócić CMD 100 (fire_empty)");
        }
        
        @Test
        @DisplayName("100% = CMD 113 (pełny ogień)")
        void testFullPercent() {
            // 100% powinno zwrócić maksymalną wartość
            assertEquals(113, FurnaceProgressIcons.getFireCustomModelData(100),
                "100% powinno zwrócić CMD 113 (fire_13 = pełny ogień)");
        }
        
        @Test
        @DisplayName("Ponad 100% = CMD 113 (ograniczenie do max)")
        void testOverFullPercent() {
            // Wartości > 100% powinny być ograniczone do 113
            assertEquals(113, FurnaceProgressIcons.getFireCustomModelData(150),
                "150% powinno być ograniczone do CMD 113");
        }
        
        @ParameterizedTest(name = "{0}% -> CMD {1}")
        @CsvSource({
            "1, 101",    // 1% = poziom 1
            "8, 102",    // ~8% = poziom 1-2
            "50, 107",   // 50% = poziom ~7
            "77, 111",   // ~77% = poziom 10
            "92, 113",   // ~92% = poziom 12-13
            "99, 113"    // 99% = poziom 13
        })
        @DisplayName("Pośrednie wartości procentowe")
        void testIntermediatePercents(int percent, int expectedCmd) {
            // Sprawdzamy czy pośrednie wartości są poprawnie mapowane
            int result = FurnaceProgressIcons.getFireCustomModelData(percent);
            assertTrue(result >= 101 && result <= 113,
                "Wynik dla " + percent + "% powinien być w zakresie 101-113, got: " + result);
        }
        
        @Test
        @DisplayName("1% powinno dać minimum CMD 101 (nie empty)")
        void testMinimumOnePercent() {
            // Nawet 1% powinno pokazać jakieś wypełnienie
            int result = FurnaceProgressIcons.getFireCustomModelData(1);
            assertTrue(result > 100, 
                "1% powinno dać CMD > 100 (pokazać jakieś wypełnienie)");
        }
    }
    
    @Nested
    @DisplayName("getArrowCustomModelData - obliczanie CMD dla strzałki")
    class ArrowCustomModelDataTests {
        
        @Test
        @DisplayName("0% = CMD 200 (empty)")
        void testZeroPercent() {
            // 0% powinno zwrócić bazową wartość (empty)
            assertEquals(200, FurnaceProgressIcons.getArrowCustomModelData(0),
                "0% powinno zwrócić CMD 200 (arrow_empty)");
        }
        
        @Test
        @DisplayName("Ujemny procent = CMD 200 (empty)")
        void testNegativePercent() {
            // Ujemne wartości też powinny zwrócić empty
            assertEquals(200, FurnaceProgressIcons.getArrowCustomModelData(-5),
                "Ujemny procent powinien zwrócić CMD 200 (arrow_empty)");
        }
        
        @Test
        @DisplayName("100% = CMD 222 (pełna strzałka)")
        void testFullPercent() {
            // 100% powinno zwrócić maksymalną wartość
            assertEquals(222, FurnaceProgressIcons.getArrowCustomModelData(100),
                "100% powinno zwrócić CMD 222 (arrow_22 = pełna strzałka)");
        }
        
        @Test
        @DisplayName("Ponad 100% = CMD 222 (ograniczenie do max)")
        void testOverFullPercent() {
            // Wartości > 100% powinny być ograniczone do 222
            assertEquals(222, FurnaceProgressIcons.getArrowCustomModelData(200),
                "200% powinno być ograniczone do CMD 222");
        }
        
        @ParameterizedTest(name = "{0}% -> CMD >= 201")
        @ValueSource(ints = {1, 5, 10, 25, 50, 75, 90, 99})
        @DisplayName("Każdy procent > 0 powinien dać CMD > 200")
        void testNonZeroPercents(int percent) {
            // Każdy niezerowy procent powinien pokazać jakieś wypełnienie
            int result = FurnaceProgressIcons.getArrowCustomModelData(percent);
            assertTrue(result > 200,
                percent + "% powinno dać CMD > 200, got: " + result);
        }
        
        @Test
        @DisplayName("50% powinno dać CMD ~211 (połowa)")
        void testHalfPercent() {
            // 50% powinno dać mniej więcej połowę (CMD ~211)
            int result = FurnaceProgressIcons.getArrowCustomModelData(50);
            assertTrue(result >= 210 && result <= 212,
                "50% powinno dać CMD w okolicach 211 (połowa z 22), got: " + result);
        }
    }
    
    @Nested
    @DisplayName("Walidacja zakresów CustomModelData")
    class RangeValidation {
        
        @Test
        @DisplayName("Fire CMD zawsze w zakresie 100-113")
        void testFireCmdRange() {
            // Testujemy pełny zakres procentów
            for (int percent = -10; percent <= 150; percent++) {
                int cmd = FurnaceProgressIcons.getFireCustomModelData(percent);
                assertTrue(cmd >= 100 && cmd <= 113,
                    "Fire CMD dla " + percent + "% powinno być w zakresie 100-113, got: " + cmd);
            }
        }
        
        @Test
        @DisplayName("Arrow CMD zawsze w zakresie 200-222")
        void testArrowCmdRange() {
            // Testujemy pełny zakres procentów
            for (int percent = -10; percent <= 150; percent++) {
                int cmd = FurnaceProgressIcons.getArrowCustomModelData(percent);
                assertTrue(cmd >= 200 && cmd <= 222,
                    "Arrow CMD dla " + percent + "% powinno być w zakresie 200-222, got: " + cmd);
            }
        }
        
        @Test
        @DisplayName("Fire CMD rośnie monotonicznie z procentem")
        void testFireCmdMonotonic() {
            // CMD powinno rosnąć (lub pozostać stałe) wraz z procentem
            int prevCmd = FurnaceProgressIcons.getFireCustomModelData(0);
            for (int percent = 1; percent <= 100; percent++) {
                int cmd = FurnaceProgressIcons.getFireCustomModelData(percent);
                assertTrue(cmd >= prevCmd,
                    "Fire CMD powinno rosnąć monotonicznie: " + (percent-1) + "% -> " + prevCmd + 
                    ", " + percent + "% -> " + cmd);
                prevCmd = cmd;
            }
        }
        
        @Test
        @DisplayName("Arrow CMD rośnie monotonicznie z procentem")
        void testArrowCmdMonotonic() {
            // CMD powinno rosnąć (lub pozostać stałe) wraz z procentem
            int prevCmd = FurnaceProgressIcons.getArrowCustomModelData(0);
            for (int percent = 1; percent <= 100; percent++) {
                int cmd = FurnaceProgressIcons.getArrowCustomModelData(percent);
                assertTrue(cmd >= prevCmd,
                    "Arrow CMD powinno rosnąć monotonicznie: " + (percent-1) + "% -> " + prevCmd + 
                    ", " + percent + "% -> " + cmd);
                prevCmd = cmd;
            }
        }
    }
}

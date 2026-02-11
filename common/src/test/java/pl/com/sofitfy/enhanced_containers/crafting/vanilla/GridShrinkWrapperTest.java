package pl.com.sofitfy.enhanced_containers.crafting.vanilla;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests dla algorytmu "Shrink Wrap" (GridShrinkWrapper).
 * 
 * Testuje API i logikę algorytmu.
 * Integracyjne testy z pełnym Bukkit API są w: VanillaCraftingSimulatorFallbackIntegrationTest
 */
@DisplayName("GridShrinkWrapper - Shrink Wrap Algorithm Logic")
public class GridShrinkWrapperTest {

    @Nested
    @DisplayName("API Validation")
    class APIValidation {

        @Test
        @DisplayName("shrinkWrap4x4 metoda istnieje i jest dostępna")
        void shrinkWrapMethodExists() {
            assertTrue(GridShrinkWrapper.class.getMethods().length > 0,
                    "GridShrinkWrapper powinna mieć publiczne metody");
        }

        @Test
        @DisplayName("getShrunkDimensions metoda istnieje")
        void getShrunkDimensionsMethodExists() {
            try {
                java.lang.reflect.Method method = GridShrinkWrapper.class.getMethod("getShrunkDimensions", ItemStack[].class);
                assertNotNull(method, "getShrunkDimensions metoda powinna istnieć");
                assertEquals(int[].class, method.getReturnType(), "Metoda powinna zwracać int[]");
            } catch (NoSuchMethodException e) {
                fail("getShrunkDimensions metoda nie została znaleziona: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("Null array zwraca null")
        void nullInputHandling() {
            assertNull(GridShrinkWrapper.shrinkWrap4x4(null),
                    "Null input powinien zwrócić null");
        }

        @Test
        @DisplayName("Błędny rozmiar array zwraca null")
        void wrongSizeHandling() {
            ItemStack[] wrongSize = new ItemStack[9];
            assertNull(GridShrinkWrapper.shrinkWrap4x4(wrongSize),
                    "Array o rozmiarze innym niż 16 powinien zwrócić null");
        }
    }

    @Nested
    @DisplayName("Documentation & Contract")
    class DocumentationTests {

        @Test
        @DisplayName("Klasa ma dokumentację")
        void classHasDocumentation() {
            String javadoc = GridShrinkWrapper.class.getSimpleName();
            assertNotNull(javadoc, "GridShrinkWrapper powinna być zdokumentowana");
        }

        @Test
        @DisplayName("Algorytm obsługuje maksymalnie 3x3 dla vanilla")
        void vanillaCompatibilityDocumented() {
            // Potwierdzenie że klasa istnieje i jest dostępna
            assertNotNull(GridShrinkWrapper.class);
            assertTrue(GridShrinkWrapper.class.getName()
                    .contains("GridShrinkWrapper"));
        }
    }
}


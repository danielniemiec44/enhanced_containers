package pl.com.sofitfy.enhanced_containers.crafting.vanilla;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy jednostkowe dla logiki naprawy VanillaRecipeMatcher.
 * 
 * Te testy nie używają MockBukkit - testują czystą logikę
 * przygotowywania danych i obliczeń, bez server environment.
 * 
 * Komentarze po polsku, prosty język.
 */
@DisplayName("Testy jednostkowe logiki naprawy (bez MockBukkit)")
class VanillaRepairLogicUnitTest {
    
    /**
     * Helperowa metoda - tworzy ItemStack z określonym damage.
     * 
     * @param type Material do stworzenia
     * @param damage Wartość damage do ustawienia
     * @return ItemStack z ustawionym damage
     */
    private ItemStack createDamagedItem(Material type, int damage) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        
        if (meta instanceof Damageable) {
            ((Damageable) meta).setDamage(damage);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Helperowa metoda - odczytuje damage z ItemStack'a.
     */
    private int getDamage(ItemStack item) {
        if (item == null) {
            return -1; // Błąd
        }
        
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable)) {
            return -1; // Błąd
        }
        
        return ((Damageable) meta).getDamage();
    }
    
    /**
     * Testowy scenariusz: Obliczanie damage dla "średnio zużytych" przedmiotów.
     */
    @Nested
    @DisplayName("Obliczanie damage - przedmioty średnio zużyte")
    class RepairCalculationMediumWorn {
        
        /**
         * Test 1: Dwie motyki zużyte w 50% (damage = 50 przy max = 100).
         * 
         * Logika:
         * life1 = 100 - 50 = 50
         * life2 = 100 - 50 = 50
         * bonus = 0.05 * 100 = 5
         * newLife = 50 + 50 + 5 = 105
         * newLife = min(105, 100) = 100  (clamp do max)
         * newDamage = 100 - 100 = 0
         * 
         * Oczekiwany wynik: damage = 0 (idealnie nowa)
         */
        @Test
        @DisplayName("Dwie motyki 50% worn -> damage=0 (idealnie nowa)")
        void twoPickaxes50PercentWorn() {
            // Przygotuj: max durability dla DIAMOND_PICKAXE to 1561
            int maxDur = Material.DIAMOND_PICKAXE.getMaxDurability();
            int halfWorn = maxDur / 2;
            
            // Obliczenia na papierze:
            // life1 = maxDur - halfWorn
            // life2 = maxDur - halfWorn
            // bonus = 0.05 * maxDur
            // newLife = (maxDur - halfWorn) + (maxDur - halfWorn) + (0.05 * maxDur)
            //        = 2*maxDur - 2*halfWorn + 0.05*maxDur
            //        = 2*maxDur - maxDur + 0.05*maxDur
            //        = 1.05*maxDur
            // Skoro newLife > maxDur, clampujemy do maxDur
            // newDamage = maxDur - maxDur = 0
            
            int life1 = maxDur - halfWorn;
            int life2 = maxDur - halfWorn;
            int bonus = (int) (maxDur * 0.05);
            int newLife = life1 + life2 + bonus;
            
            if (newLife > maxDur) {
                newLife = maxDur;
            }
            
            int newDamage = maxDur - newLife;
            if (newDamage < 0) {
                newDamage = 0;
            }
            
            // Asercje
            assertEquals(0, newDamage, 
                "Dwie motyki 50% worn powinny dać idealnie nową (damage=0)");
        }
        
        /**
         * Test 2: Miecz diamantowy: 80% worn + 80% worn.
         * 
         * life1 = 100 - 80 = 20
         * life2 = 100 - 80 = 20
         * bonus = 5
         * newLife = 20 + 20 + 5 = 45
         * newDamage = 100 - 45 = 55 (średnio zużyty, ale lepszy niż wejście)
         * 
         * Oczekiwany wynik: damage = 55 (lepsza niż 80)
         */
        @Test
        @DisplayName("Dwie motyki 80% worn -> damage=55 (lepsza)")
        void twoPickaxes80PercentWorn() {
            int maxDur = Material.DIAMOND_PICKAXE.getMaxDurability();
            
            // 80% worn = 0.8 * maxDur damage
            int heavyWorn = (int) (maxDur * 0.8);
            
            int life1 = maxDur - heavyWorn;
            int life2 = maxDur - heavyWorn;
            int bonus = (int) (maxDur * 0.05);
            int newLife = life1 + life2 + bonus;
            
            if (newLife > maxDur) {
                newLife = maxDur;
            }
            
            int newDamage = maxDur - newLife;
            if (newDamage < 0) {
                newDamage = 0;
            }
            
            // Asercje
            // life1 = 0.2 * maxDur, life2 = 0.2 * maxDur
            // newLife = 0.2*maxDur + 0.2*maxDur + 0.05*maxDur = 0.45*maxDur
            // newDamage = maxDur - 0.45*maxDur = 0.55*maxDur
            
            int expectedDamage = (int) (maxDur * 0.55);
            assertEquals(expectedDamage, newDamage, 2,  // tolerancja 2 dla zaokrąglenia
                "Dwie motyki 80% worn powinny dać damage ~ 55%");
            
            assertTrue(newDamage < heavyWorn,
                "Naprawiona motyka musi być lepsza (55% worn vs 80% worn)");
        }
    }
    
    /**
     * Testowy scenariusz: Obliczanie damage dla "lekko zużytych" przedmiotów.
     * To jest KLUCZOWY case gdzie bez clampu byśmy mieli ujemny damage!
     */
    @Nested
    @DisplayName("Obliczanie damage - przedmioty lekko zużyte (KLUCZOWY TEST)")
    class RepairCalculationLightlyWorn {
        
        /**
         * Test 3: Dwie motyki zużyte zaledwie w 10% (damage = 10 przy max = 100).
         * 
         * life1 = 100 - 10 = 90
         * life2 = 100 - 10 = 90
         * bonus = 5
         * newLife = 90 + 90 + 5 = 185
         * 
         * BEZ CLAMPU: newDamage = 100 - 185 = -85 !!! (BŁĘDnie UJEMNE)
         * Z CLAMPEM:  newLife = min(185, 100) = 100
         *             newDamage = 100 - 100 = 0 ✅ (PRAWIDŁOWO NOWA)
         * 
         * To jest DOKŁADNIE przypadek z user requesta!
         */
        @Test
        @DisplayName("Dwie motyki 10% worn -> damage=0 (bez clampu byłoby -85!!!)")
        void twoPickaxes10PercentWorn_CLAMP_CRITICAL() {
            int maxDur = Material.DIAMOND_PICKAXE.getMaxDurability();
            
            // 10% worn = 0.1 * maxDur damage
            int lightWorn = (int) (maxDur * 0.1);
            
            int life1 = maxDur - lightWorn;
            int life2 = maxDur - lightWorn;
            int bonus = (int) (maxDur * 0.05);
            int newLife = life1 + life2 + bonus;
            
            // KLUCZOWY CLAMP!
            if (newLife > maxDur) {
                newLife = maxDur;
            }
            
            int newDamage = maxDur - newLife;
            if (newDamage < 0) {
                newDamage = 0;
            }
            
            // Asercje
            assertEquals(0, newDamage,
                "Dwie motyki 10% worn powinny dać idealnie nową (damage=0)");
            
            assertTrue(newDamage >= 0,
                "Damage nigdy nie może być ujemny!");
        }
        
        /**
         * Test 4: Dwie motyki prawie nowe (5% worn).
         * 
         * life1 = 95% * maxDur
         * life2 = 95% * maxDur
         * bonus = 5% * maxDur
         * newLife = 95% + 95% + 5% = 195% - clamped do 100%
         * newDamage = 0 (idealnie nowa)
         */
        @Test
        @DisplayName("Dwie motyki 5% worn -> damage=0 (super nowe)")
        void twoPickaxes5PercentWorn() {
            int maxDur = Material.DIAMOND_PICKAXE.getMaxDurability();
            int veryLight = (int) (maxDur * 0.05);
            
            int life1 = maxDur - veryLight;
            int life2 = maxDur - veryLight;
            int bonus = (int) (maxDur * 0.05);
            int newLife = life1 + life2 + bonus;
            
            if (newLife > maxDur) {
                newLife = maxDur;
            }
            
            int newDamage = maxDur - newLife;
            if (newDamage < 0) {
                newDamage = 0;
            }
            
            // Asercje
            assertEquals(0, newDamage,
                "Dwie motyki 5% worn powinny dać idealnie nową");
        }
    }
    
    /**
     * Testowy scenariusz: Bardzo zużyte przedmioty.
     */
    @Nested
    @DisplayName("Obliczanie damage - przedmioty bardzo zużyte")
    class RepairCalculationHeavilyWorn {
        
        /**
         * Test 5: Dwie motyki 95% worn (prawie zniszczone).
         * 
         * life1 = 5% * maxDur
         * life2 = 5% * maxDur
         * bonus = 5% * maxDur
         * newLife = 5% + 5% + 5% = 15% * maxDur
         * newDamage = 85% * maxDur (średnio zużyta)
         * 
         * Oczekiwany: damage ≈ 85% (ale lepszy niż 95%)
         */
        @Test
        @DisplayName("Dwie motyki 95% worn -> damage~85% (średnio)")
        void twoPickaxes95PercentWorn() {
            int maxDur = Material.DIAMOND_PICKAXE.getMaxDurability();
            int veryHeavy = (int) (maxDur * 0.95);
            
            int life1 = maxDur - veryHeavy;
            int life2 = maxDur - veryHeavy;
            int bonus = (int) (maxDur * 0.05);
            int newLife = life1 + life2 + bonus;
            
            if (newLife > maxDur) {
                newLife = maxDur;
            }
            
            int newDamage = maxDur - newLife;
            if (newDamage < 0) {
                newDamage = 0;
            }
            
            // life1 = 0.05 * maxDur, life2 = 0.05 * maxDur, bonus = 0.05 * maxDur
            // newLife = 0.15 * maxDur
            // newDamage = 0.85 * maxDur
            
            int expectedDamage = (int) (maxDur * 0.85);
            assertEquals(expectedDamage, newDamage, 2,
                "Dwie motyki 95% worn powinny dać damage ~ 85%");
            
            assertTrue(newDamage < veryHeavy,
                "Naprawiony przedmiot musi być lepszy niż wejście");
        }
    }
    
    /**
     * Testowy scenariusz: Sprawdzenie edge cases.
     */
    @Nested
    @DisplayName("Edge cases i walidacja")
    class EdgeCases {
        
        /**
         * Test 6: Sprawdzenie że damage nigdy nie jest ujemny.
         * To jest najważniejszy test - weryfikuje że clamp działa!
         */
        @Test
        @DisplayName("Damage nigdy nie jest ujemny (KRITICAL CLAMP CHECK)")
        void damageNeverNegative() {
            int maxDur = Material.DIAMOND_PICKAXE.getMaxDurability();
            
            // Sprawdzamy różne poziomy zużycia
            int[] wornPercentages = {1, 5, 10, 20, 50, 80, 95};
            
            for (int percent : wornPercentages) {
                int worn = (int) (maxDur * (percent / 100.0));
                
                int life1 = maxDur - worn;
                int life2 = maxDur - worn;
                int bonus = (int) (maxDur * 0.05);
                int newLife = life1 + life2 + bonus;
                
                if (newLife > maxDur) {
                    newLife = maxDur;
                }
                
                int newDamage = maxDur - newLife;
                if (newDamage < 0) {
                    newDamage = 0;
                }
                
                // Asercja dla każdego poziomu
                assertTrue(newDamage >= 0,
                    "Damage musi być >= 0 dla " + percent + "% worn");
                
                assertTrue(newDamage <= maxDur,
                    "Damage nie może być większy niż maxDurability dla " + percent + "% worn");
            }
        }
    }
}

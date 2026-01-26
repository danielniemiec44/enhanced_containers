package pl.com.sofitfy.enhanced_containers.crafting.nms;

import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * Prosty mostek do NMS craftingu.
 *
 * Dlaczego to jest w tej formie:
 * - Ten projekt jest Mavenowy i normalnie nie ma NMS/CraftBukkit na classpath.
 * - Wersja "prawdziwa" (Mojang mappings, Paper 1.21.11) siedzi w klasie
 *   {@code NmsCraftingEngineDirect} i jest kompilowana przez Gradle + paperweight-userdev.
 * - Tutaj robimy tylko wywołanie refleksją, żeby Mavenowe testy dalej działały.
 */
public class NmsCraftingEngine {

    /**
     * Zwraca wynik craftingu policzony przez NMS (z prawidłowym NBT) albo {@code null}, jeśli:
     * - brak pasującej receptury,
     * - środowisko nie ma NMS (np. testy jednostkowe / MockBukkit),
     * - albo wystąpił błąd.
     */
    public ItemStack getNMSCraftingResult(ItemStack[] matrix, World bukkitWorld) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix must not be null");
        }
        if (matrix.length != 9) {
            throw new IllegalArgumentException("matrix must be length 9");
        }
        if (bukkitWorld == null) {
            throw new IllegalArgumentException("world must not be null");
        }

        try {
            Class<?> directClass = Class.forName(
                "pl.com.sofitfy.enhanced_containers.crafting.nms.NmsCraftingEngineDirect"
            );

            Object direct = directClass.getDeclaredConstructor().newInstance();
            Method method = directClass.getMethod("getNMSCraftingResult", ItemStack[].class, World.class);
            return (ItemStack) method.invoke(direct, matrix, bukkitWorld);
        } catch (ClassNotFoundException e) {
            // Brak klasy direct = budujemy przez Maven bez paperweight.
            return null;
        } catch (Throwable t) {
            // W testach/MockBukkit często nie ma CraftWorld i to jest OK.
            return null;
        }
    }

    /**
     * Jak getNMSCraftingResult, ale zwraca wynik TYLKO jeśli matchowana receptura jest shapeless.
     *
     * To jest potrzebne dla naszego fallbacku 4x4 -> 3x3:
     * - w shapeless chcemy dopuścić "dowolne sloty" i traktować je jakby były obok siebie,
     * - ale nie chcemy w ten sposób przypadkiem matchować receptur shaped.
     */
    public ItemStack getNMSShapelessCraftingResult(ItemStack[] matrix, World bukkitWorld) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix must not be null");
        }
        if (matrix.length != 9) {
            throw new IllegalArgumentException("matrix must be length 9");
        }
        if (bukkitWorld == null) {
            throw new IllegalArgumentException("world must not be null");
        }

        try {
            Class<?> directClass = Class.forName(
                "pl.com.sofitfy.enhanced_containers.crafting.nms.NmsCraftingEngineDirect"
            );

            Object direct = directClass.getDeclaredConstructor().newInstance();
            Method method = directClass.getMethod("getNMSShapelessCraftingResult", ItemStack[].class, World.class);
            return (ItemStack) method.invoke(direct, matrix, bukkitWorld);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Throwable t) {
            return null;
        }
    }
}

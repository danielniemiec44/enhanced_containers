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

    /**
     * Zwraca remainder items (container items) dla vanilla receptur.
     * 
     * Co to są remainder items?
     * - Po scraftowaniu niektóre składniki "zostawiają coś" w gridzie
     * - Przykład: MILK_BUCKET → BUCKET (po zrobieniu ciasta)
     * - Przykład: HONEY_BOTTLE → GLASS_BOTTLE (po zrobieniu honey block)
     * 
     * Jak to działa:
     * - NMS sprawdza która receptura pasuje do gridu
     * - Wywołuje recipe.getRemainingItems() która zwraca co zostaje
     * - Zwracamy tablicę 9 elementów (null = nic nie zostaje w tym slocie)
     * 
     * Kiedy używać:
     * - Po każdym vanilla craftingu (nie custom!)
     * - Wstawiamy remainder items z powrotem do gridu
     * - Gracze automatycznie dostają puste wiaderka/butelki
     * 
     * @param matrix Siatka 3x3 PRZED craftingiem (oryginalne składniki)
     * @param bukkitWorld Świat Bukkit
     * @return Tablica remainder items (null w slocie = nic nie zostaje), pusta tablica jeśli NMS niedostępny
     */
    public ItemStack[] getRemainingItems(ItemStack[] matrix, World bukkitWorld) {
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
            Method method = directClass.getMethod("getRemainingItems", ItemStack[].class, World.class);
            return (ItemStack[]) method.invoke(direct, matrix, bukkitWorld);
        } catch (ClassNotFoundException e) {
            // Brak klasy direct = budujemy przez Maven bez paperweight
            return new ItemStack[9];
        } catch (Throwable t) {
            // Testy jednostkowe / MockBukkit: zwracamy pustą tablicę
            return new ItemStack[9];
        }
    }
    
    /**
     * Pobiera remainder item (container item) dla pojedynczego ItemStack.
     * Używa NMS Item.getCraftingRemainingItem() do sprawdzenia czy item ma remainder.
     * 
     * Przykłady:
     * - MILK_BUCKET → BUCKET
     * - WATER_BUCKET → BUCKET
     * - LAVA_BUCKET → BUCKET
     * - HONEY_BOTTLE → GLASS_BOTTLE
     * 
     * @param itemStack ItemStack do sprawdzenia
     * @return remainder item lub null jeśli brak
     */
    public ItemStack getCraftingRemainder(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return null;
        }

        try {
            Class<?> directClass = Class.forName(
                "pl.com.sofitfy.enhanced_containers.crafting.nms.NmsCraftingEngineDirect"
            );

            Object direct = directClass.getDeclaredConstructor().newInstance();
            Method method = directClass.getMethod("getCraftingRemainder", ItemStack.class);
            return (ItemStack) method.invoke(direct, itemStack);
        } catch (ClassNotFoundException e) {
            // Brak klasy direct = budujemy przez Maven bez paperweight
            return null;
        } catch (Throwable t) {
            // Testy jednostkowe / MockBukkit: zwracamy null
            return null;
        }
    }}
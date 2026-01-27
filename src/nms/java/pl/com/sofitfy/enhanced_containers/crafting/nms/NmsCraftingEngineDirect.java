package pl.com.sofitfy.enhanced_containers.crafting.nms;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Prawdziwy NMS crafting engine dla Paper 1.21.11 (Mojang mappings).
 *
 * Cel:
 * - ominąć ograniczenia Bukkit API dla "complex" receptur (repair, dyeing, fireworks, banners),
 * - dostać wynik dokładnie taki jak vanilla, razem z NBT,
 * - oraz użyć krytycznego wywołania: recipe.value().assemble(...).
 *
 * WAŻNE:
 * - Ta klasa wymaga paperweight-userdev (dev bundle). Dlatego jest w src/nms/java.
 * - Jeśli uruchamiasz testy na MockBukkit, World nie będzie CraftWorld → zwracamy null.
 */
public class NmsCraftingEngineDirect {

    public ItemStack getNMSCraftingResult(ItemStack[] matrix, World bukkitWorld) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix must not be null");
        }
        if (matrix.length != 9) {
            throw new IllegalArgumentException("matrix must be length 9");
        }
        if (!(bukkitWorld instanceof CraftWorld craftWorld)) {
            // MockBukkit / testy: brak dostępu do NMS level.
            return null;
        }

        ServerLevel level = craftWorld.getHandle();

        CraftingContainer container = create3x3Container();
        for (int i = 0; i < 9; i++) {
            ItemStack bukkit = matrix[i];
            net.minecraft.world.item.ItemStack nms = (bukkit == null || bukkit.getType().isAir())
                ? net.minecraft.world.item.ItemStack.EMPTY
                : CraftItemStack.asNMSCopy(bukkit);
            container.setItem(i, nms);
        }

        // 1) Szukamy receptury craftingu na podstawie aktualnej siatki
        // W 1.21.11 RecipeManager jest na serwerze (level.getServer().getRecipeManager()).
        // Wymaganie "container" realizujemy przez container.asCraftInput(), bo tak działa API NMS.
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer().getRecipeManager().getRecipeFor(
            RecipeType.CRAFTING,
            container.asCraftInput(),
            level
        );

        // 2) Krytycznie ważne: assemble(...) daje dynamiczny wynik (np. z NBT)
        net.minecraft.world.item.ItemStack result = recipe
            .map(r -> r.value().assemble(container.asCraftInput(), level.registryAccess()))
            .orElse(net.minecraft.world.item.ItemStack.EMPTY);

        // 3) Konwersja z NMS do Bukkit
        ItemStack bukkitResult = CraftItemStack.asBukkitCopy(result);
        if (bukkitResult == null || bukkitResult.getType().isAir() || bukkitResult.getAmount() <= 0) {
            return null;
        }

        return bukkitResult;
    }

    /**
        * Wariant: zwraca wynik tylko jeśli pasująca receptura NIE jest shaped.
     *
     * Dzięki temu możemy robić "upakowanie" składników z 4x4 do 3x3
     * bez ryzyka, że przypadkiem zmatchujemy recepturę shaped.
     */
    public ItemStack getNMSShapelessCraftingResult(ItemStack[] matrix, World bukkitWorld) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix must not be null");
        }
        if (matrix.length != 9) {
            throw new IllegalArgumentException("matrix must be length 9");
        }
        if (!(bukkitWorld instanceof CraftWorld craftWorld)) {
            return null;
        }

        ServerLevel level = craftWorld.getHandle();

        CraftingContainer container = create3x3Container();
        for (int i = 0; i < 9; i++) {
            ItemStack bukkit = matrix[i];
            net.minecraft.world.item.ItemStack nms = (bukkit == null || bukkit.getType().isAir())
                ? net.minecraft.world.item.ItemStack.EMPTY
                : CraftItemStack.asNMSCopy(bukkit);
            container.setItem(i, nms);
        }

        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer().getRecipeManager().getRecipeFor(
            RecipeType.CRAFTING,
            container.asCraftInput(),
            level
        );

        if (recipe.isEmpty()) {
            return null;
        }

        CraftingRecipe value = recipe.get().value();
        // W vanilli wiele "shapeless" craftów jest realizowanych przez klasy specjalne
        // (repair, dyeing, fireworks, itp.) i NIE są instancją ShapelessRecipe.
        // Kluczowy warunek dla naszego use-case: odfiltrować TYLKO receptury shaped.
        if (value instanceof net.minecraft.world.item.crafting.ShapedRecipe) {
            return null;
        }

        net.minecraft.world.item.ItemStack result = value.assemble(container.asCraftInput(), level.registryAccess());
        ItemStack bukkitResult = CraftItemStack.asBukkitCopy(result);
        if (bukkitResult == null || bukkitResult.getType().isAir() || bukkitResult.getAmount() <= 0) {
            return null;
        }

        return bukkitResult;
    }

    /**
     * Zwraca remainder items (container items) dla danego gridu po scraftowaniu.
     * 
     * Przykłady vanilla remainder items:
     * - MILK_BUCKET → BUCKET (po scraftowaniu ciasta)
     * - HONEY_BOTTLE → GLASS_BOTTLE (po scraftowaniu honey block)
     * - WATER_BUCKET → BUCKET (po scraftowaniu betonu)
     * - LAVA_BUCKET → BUCKET
     * - POWDER_SNOW_BUCKET → BUCKET
     * 
     * W NMS każdy Item ma metodę getCraftingRemainingItem() która zwraca co zostaje.
     * Wstawiamy remainder items z powrotem do gridu w te same sloty gdzie były oryginalne składniki.
     * 
     * @param matrix Siatka 3x3 PRZED scraftowaniem (oryginalne składniki)
     * @param bukkitWorld Świat Bukkit (potrzebny do konwersji NMS)
     * @return Tablica 9 elementów z remainder items (null = nic nie zostaje w tym slocie)
     */
    public ItemStack[] getRemainingItems(ItemStack[] matrix, World bukkitWorld) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix must not be null");
        }
        if (matrix.length != 9) {
            throw new IllegalArgumentException("matrix must be length 9");
        }
        if (!(bukkitWorld instanceof CraftWorld craftWorld)) {
            // MockBukkit / testy jednostkowe: zwracamy pustą tablicę
            return new ItemStack[9];
        }

        ServerLevel level = craftWorld.getHandle();
        CraftingContainer container = create3x3Container();
        
        // Wypełnij container NMS itemami
        for (int i = 0; i < 9; i++) {
            ItemStack bukkit = matrix[i];
            net.minecraft.world.item.ItemStack nms = (bukkit == null || bukkit.getType().isAir())
                ? net.minecraft.world.item.ItemStack.EMPTY
                : CraftItemStack.asNMSCopy(bukkit);
            container.setItem(i, nms);
        }

        // Szukamy receptury która pasuje
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer().getRecipeManager().getRecipeFor(
            RecipeType.CRAFTING,
            container.asCraftInput(),
            level
        );

        if (recipe.isEmpty()) {
            // Brak receptury = brak remainder items
            System.out.println("[NMS DEBUG] Brak receptury dla gridu: " + java.util.Arrays.toString(matrix));
            return new ItemStack[9];
        }

        System.out.println("[NMS DEBUG] Znaleziono recepturę: " + recipe.get().id());
        
        // getRemainingItems() zwraca co zostaje w gridzie po scraftowaniu
        // To jest kluczowa metoda NMS do obsługi container items
        net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> remainingNms = 
            recipe.get().value().getRemainingItems(container.asCraftInput());

        System.out.println("[NMS DEBUG] getRemainingItems() zwróciło: " + remainingNms.size() + " elementów");
        for (int i = 0; i < remainingNms.size(); i++) {
            net.minecraft.world.item.ItemStack nmsItem = remainingNms.get(i);
            System.out.println("[NMS DEBUG]   Slot " + i + ": " + (nmsItem == null ? "null" : nmsItem.isEmpty() ? "EMPTY" : nmsItem.toString()));
        }

        // Konwertuj z NMS do Bukkit
        ItemStack[] remainingBukkit = new ItemStack[9];
        for (int i = 0; i < 9 && i < remainingNms.size(); i++) {
            net.minecraft.world.item.ItemStack nmsItem = remainingNms.get(i);
            if (nmsItem == null || nmsItem.isEmpty()) {
                remainingBukkit[i] = null;
            } else {
                remainingBukkit[i] = CraftItemStack.asBukkitCopy(nmsItem);
            }
        }

        System.out.println("[NMS DEBUG] Zwracam remainder items (Bukkit): " + java.util.Arrays.toString(remainingBukkit));
        return remainingBukkit;
    }

    private CraftingContainer create3x3Container() {
        AbstractContainerMenu dummy = new AbstractContainerMenu(null, -1) {
            @Override
            public InventoryView getBukkitView() {
                return null;
            }

            @Override
            public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                return false;
            }

            @Override
            public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slot) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
        };

        return new TransientCraftingContainer(dummy, 3, 3);
    }
    
    /**
     * Pobiera remainder item (container item) dla pojedynczego ItemStack.
     * 
     * W Minecraft NMS remainder items są zdefiniowane statycznie dla każdego Item typu:
     * - Items.MILK_BUCKET ma craftingRemainingItem = Items.BUCKET
     * - Items.WATER_BUCKET ma craftingRemainingItem = Items.BUCKET
     * - Items.LAVA_BUCKET ma craftingRemainingItem = Items.BUCKET
     * - Items.HONEY_BOTTLE ma craftingRemainingItem = Items.GLASS_BOTTLE
     * 
     * To jest prywatne pole w klasie Item, ale możemy je odczytać przez refleksję
     * lub sprawdzić używając CraftingRecipe.getRemainingItems().
     * 
     * Tutaj używamy prostszego podejścia: tworzymy 1x1 grid i wywołujemy vanilla logic.
     * 
     * @param itemStack ItemStack do sprawdzenia
     * @return remainder item lub null jeśli brak
     */
    public ItemStack getCraftingRemainder(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return null;
        }
        
        // Konwertuj Bukkit → NMS
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        if (nmsItem == null || nmsItem.isEmpty()) {
            return null;
        }
        
        try {
            // Pobierz Item z NMS ItemStack
            net.minecraft.world.item.Item item = nmsItem.getItem();
            if (item == null) {
                return null;
            }
            
            // Użyj refleksji aby odczytać prywatne pole 'craftingRemainingItem'
            // W Mojang mappings: private @Nullable Item craftingRemainingItem
            java.lang.reflect.Field field = net.minecraft.world.item.Item.class.getDeclaredField("craftingRemainingItem");
            field.setAccessible(true);
            net.minecraft.world.item.Item remainderItem = (net.minecraft.world.item.Item) field.get(item);
            
            if (remainderItem == null) {
                return null;
            }
            
            // Utwórz ItemStack z remainder item (domyślna ilość: 1)
            net.minecraft.world.item.ItemStack remainingNms = new net.minecraft.world.item.ItemStack(remainderItem);
            if (remainingNms.isEmpty()) {
                return null;
            }
            
            // Konwertuj NMS → Bukkit
            ItemStack bukkitRemaining = CraftItemStack.asBukkitCopy(remainingNms);
            if (bukkitRemaining == null || bukkitRemaining.getType().isAir()) {
                return null;
            }
            
            return bukkitRemaining;
        } catch (Exception e) {
            // Refleksja nie zadziałała (może zmieniona nazwa pola w innej wersji?)
            return null;
        }
    }
}

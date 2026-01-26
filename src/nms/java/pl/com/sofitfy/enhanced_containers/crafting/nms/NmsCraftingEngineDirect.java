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
}

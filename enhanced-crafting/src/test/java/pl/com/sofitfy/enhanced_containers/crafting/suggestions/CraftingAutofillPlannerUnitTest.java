package pl.com.sofitfy.enhanced_containers.crafting.suggestions;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CraftingAutofillPlannerUnitTest {

    @Test
    void plan_succeedsAndUsesFirstMatching() {
        // Dado: ekwipunek ma 2 sloty z DIAMOND
        ItemStack a = mockStack(Material.DIAMOND, 1);
        ItemStack b = mockStack(Material.DIAMOND, 5);

        ItemStack[] inv = new ItemStack[] {a, b};

        // Plan gridu: potrzebujemy 2x DIAMOND (w dwóch slotach)
        ItemStack[] grid = new ItemStack[16];
        grid[0] = mockStack(Material.DIAMOND, 1);
        grid[1] = mockStack(Material.DIAMOND, 1);

        BiPredicate<ItemStack, ItemStack> matches = (have, need) -> have.getType() == need.getType();

        Optional<CraftingAutofillPlanner.AutofillPlan> plan = CraftingAutofillPlanner.plan(inv, grid, matches);
        assertTrue(plan.isPresent(), "Plan powinien się udać");

        List<CraftingAutofillPlanner.ConsumeOp> ops = plan.get().consumeOps();
        assertEquals(2, ops.size(), "Powinny być 2 zużycia");

        // Pierwszy DIAMOND powinien zejść z slotu 0
        assertEquals(0, ops.get(0).inventorySlot());
        assertEquals(1, ops.get(0).amount());
    }

    @Test
    void plan_failsWhenMissingItems() {
        ItemStack a = mockStack(Material.IRON_INGOT, 1);
        ItemStack[] inv = new ItemStack[] {a};

        ItemStack[] grid = new ItemStack[16];
        grid[0] = mockStack(Material.DIAMOND, 1);

        Optional<CraftingAutofillPlanner.AutofillPlan> plan = CraftingAutofillPlanner.plan(
            inv,
            grid,
            (have, need) -> have.getType() == need.getType()
        );

        assertTrue(plan.isEmpty(), "Plan powinien się nie udać gdy brakuje itemów");
    }

    @Test
    void maximizePlan16_distributesEvenlyForSameIngredient() {
        // Mamy recepturę 2x2 (4 sloty) tego samego składnika.
        ItemStack[] base = new ItemStack[16];
        base[0] = new ItemStack(Material.COAL, 1);
        base[1] = new ItemStack(Material.COAL, 1);
        base[4] = new ItemStack(Material.COAL, 1);
        base[5] = new ItemStack(Material.COAL, 1);

        // W ekwipunku 65 szt. -> najlepiej rozłożyć 17/16/16/16 (żeby zrobić 16 craftów i mieć 1 nadmiar).
        ItemStack[] inv = new ItemStack[] { new ItemStack(Material.COAL, 65) };

        Optional<ItemStack[]> out = CraftingAutofillPlanner.maximizePlan16(inv, base, ItemStack::isSimilar);
        assertTrue(out.isPresent(), "Powinno się dać zbudować max plan");

        ItemStack[] plan = out.get();
        assertEquals(17, plan[0].getAmount());
        assertEquals(16, plan[1].getAmount());
        assertEquals(16, plan[4].getAmount());
        assertEquals(16, plan[5].getAmount());
    }

    @Test
    void maximizePlan16_capsAtMaxStackSize() {
        // 4 sloty tego samego składnika, ale w ekwipunku bardzo dużo.
        ItemStack[] base = new ItemStack[16];
        base[0] = new ItemStack(Material.COAL, 1);
        base[1] = new ItemStack(Material.COAL, 1);
        base[4] = new ItemStack(Material.COAL, 1);
        base[5] = new ItemStack(Material.COAL, 1);

        // 300 coal: docelowo każdy slot max 64, reszta zostaje w ekwipunku.
        ItemStack[] inv = new ItemStack[] { new ItemStack(Material.COAL, 300) };

        Optional<ItemStack[]> out = CraftingAutofillPlanner.maximizePlan16(inv, base, ItemStack::isSimilar);
        assertTrue(out.isPresent());

        ItemStack[] plan = out.get();
        assertEquals(64, plan[0].getAmount());
        assertEquals(64, plan[1].getAmount());
        assertEquals(64, plan[4].getAmount());
        assertEquals(64, plan[5].getAmount());
    }

    private static ItemStack mockStack(Material type, int amount) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(type);
        when(stack.getAmount()).thenReturn(amount);
        when(stack.getMaxStackSize()).thenReturn(64);
        when(stack.clone()).thenReturn(stack);
        when(stack.hasItemMeta()).thenReturn(false);
        when(stack.getItemMeta()).thenReturn(null);
        return stack;
    }
}

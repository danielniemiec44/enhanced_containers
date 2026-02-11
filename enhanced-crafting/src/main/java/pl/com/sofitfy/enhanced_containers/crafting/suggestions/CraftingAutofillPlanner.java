package pl.com.sofitfy.enhanced_containers.crafting.suggestions;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Planer auto-układania receptury.
 *
 * Dlaczego planer:
 * - najpierw liczymy czy się da (bez modyfikowania ekwipunku),
 * - potem dopiero listener wykonuje plan (zużywa itemy i wstawia do gridu).
 */
public final class CraftingAutofillPlanner {

    private CraftingAutofillPlanner() {
    }

    public static Optional<AutofillPlan> plan(
        ItemStack[] inventorySnapshot,
        ItemStack[] gridPlan16,
        BiPredicate<ItemStack, ItemStack> matches
    ) {
        if (inventorySnapshot == null || gridPlan16 == null || gridPlan16.length != 16) {
            return Optional.empty();
        }
        if (matches == null) {
            matches = ItemStack::isSimilar;
        }

        int[] remaining = new int[inventorySnapshot.length];
        for (int i = 0; i < inventorySnapshot.length; i++) {
            ItemStack s = inventorySnapshot[i];
            remaining[i] = (s == null || s.getType().isAir()) ? 0 : Math.max(0, s.getAmount());
        }

        List<ConsumeOp> ops = new ArrayList<>();

        for (ItemStack needed : gridPlan16) {
            if (needed == null || needed.getType().isAir()) {
                continue;
            }

            int toTake = Math.max(1, needed.getAmount());
            int left = toTake;

            for (int slot = 0; slot < inventorySnapshot.length && left > 0; slot++) {
                ItemStack have = inventorySnapshot[slot];
                if (have == null || have.getType().isAir() || remaining[slot] <= 0) {
                    continue;
                }

                boolean ok;
                try {
                    ok = matches.test(have, needed);
                } catch (Exception e) {
                    ok = false;
                }

                if (!ok) {
                    continue;
                }

                int take = Math.min(remaining[slot], left);
                if (take <= 0) {
                    continue;
                }

                remaining[slot] -= take;
                left -= take;
                ops.add(new ConsumeOp(slot, take));
            }

            if (left > 0) {
                return Optional.empty();
            }
        }

        return Optional.of(new AutofillPlan(List.copyOf(ops)));
    }

    /**
     * Buduje "maksymalny" plan do wypełnienia gridu.
     *
     * Cel (UX): po kliknięciu podpowiedzi chcemy wypełnić sloty nie tylko 1 sztuką,
     * ale na tyle, żeby gracz mógł od razu robić wiele craftów (np. shift-click na wynik).
     *
     * Zasada:
     * - każdy wymagany slot musi dostać co najmniej 1 sztukę,
     * - jeśli ten sam składnik występuje w wielu slotach, rozdzielamy ilość możliwie równo
     *   (bo vanilla przy craftowaniu zabiera 1 szt. z KAŻDEGO takiego slotu).
     * - nie przekraczamy max stack size.
     */
    public static Optional<ItemStack[]> maximizePlan16(
        ItemStack[] inventorySnapshot,
        ItemStack[] basePlan16,
        BiPredicate<ItemStack, ItemStack> matches
    ) {
        if (inventorySnapshot == null || basePlan16 == null || basePlan16.length != 16) {
            return Optional.empty();
        }
        if (matches == null) {
            matches = ItemStack::isSimilar;
        }

        // Grupy składników (sloty w planie, które chcą "to samo").
        List<Group> groups = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            ItemStack need = basePlan16[i];
            if (need == null || need.getType().isAir()) {
                continue;
            }

            Group g = null;
            for (Group existing : groups) {
                boolean ok;
                try {
                    ok = matches.test(existing.representative, need) && matches.test(need, existing.representative);
                } catch (Exception e) {
                    ok = false;
                }
                if (ok) {
                    g = existing;
                    break;
                }
            }
            if (g == null) {
                g = new Group(need);
                groups.add(g);
            }
            g.planSlots.add(i);
        }

        ItemStack[] out = new ItemStack[16];

        // Dla każdej grupy liczymy ile mamy w ekwipunku i jak to rozłożyć.
        for (Group g : groups) {
            int occurrences = g.planSlots.size();
            if (occurrences <= 0) {
                continue;
            }

            int total = 0;
            for (ItemStack have : inventorySnapshot) {
                if (have == null || have.getType().isAir() || have.getAmount() <= 0) {
                    continue;
                }
                boolean ok;
                try {
                    ok = matches.test(have, g.representative);
                } catch (Exception e) {
                    ok = false;
                }
                if (!ok) {
                    continue;
                }
                total += have.getAmount();
            }

            // Jeśli nie mamy nawet po 1 na każdy slot - nie ma sensu.
            if (total < occurrences) {
                return Optional.empty();
            }

            int maxStack = Math.max(1, g.representative.getMaxStackSize());
            int base = total / occurrences;
            if (base <= 0) {
                return Optional.empty();
            }

            // Jeśli base jest większy niż max stack, to i tak nie wsadzimy więcej niż max.
            if (base >= maxStack) {
                for (int slot : g.planSlots) {
                    out[slot] = copyStackWithAmount(g.representative, maxStack);
                }
                continue;
            }

            int remainder = total % occurrences;

            // Rozkładamy równo: base + 1 dla pierwszych "remainder" slotów.
            for (int idx = 0; idx < g.planSlots.size(); idx++) {
                int slot = g.planSlots.get(idx);
                int amount = base + (idx < remainder ? 1 : 0);
                amount = Math.min(amount, maxStack);
                if (amount <= 0) {
                    return Optional.empty();
                }
                out[slot] = copyStackWithAmount(g.representative, amount);
            }
        }

        // Puste sloty zostają null.
        return Optional.of(out);
    }

    private static ItemStack copyStackWithAmount(ItemStack src, int amount) {
        int safeAmount = Math.max(1, amount);
        ItemStack out = new ItemStack(src.getType(), safeAmount);

        // Unit testy Mavenowe nie mają uruchomionego serwera Bukkit, więc kopiowanie meta
        // musi być ostrożne (np. ItemStackMock potrafi NPE bez Bukkit.server).
        try {
            if (Bukkit.getServer() != null && src.hasItemMeta()) {
                out.setItemMeta(src.getItemMeta());
            }
        } catch (Throwable ignored) {
            // no-op
        }

        return out;
    }

    private static final class Group {
        private final ItemStack representative;
        private final List<Integer> planSlots = new ArrayList<>();

        private Group(ItemStack representative) {
            this.representative = representative;
        }
    }

    public record ConsumeOp(int inventorySlot, int amount) {
    }

    public record AutofillPlan(List<ConsumeOp> consumeOps) {
    }
}

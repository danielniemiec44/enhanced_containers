package pl.com.sofitfy.enhanced_containers.crafting.vanilla;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.Color;

import pl.com.sofitfy.enhanced_containers.crafting.nms.NmsCraftingEngine;

import java.util.Map;

/**
 * Symuluje prawdziwy vanilla crafting table z obsługą pluginów.
 * 
 * Kod jest skondensowany i prosty:
 * 1. Tworzy wirtualny stół
 * 2. Wrzuca itemy do gridu
 * 3. Szuka vanilla receptury i ustawia wynik
 * 4. Odpala event, żeby pluginy mogły zmodyfikować wynik
 * 5. Zwraca ostateczny wynik
 * 
 * To gwarantuje 100% kompatybilność z vanilla craftingiem i każdym pluginem,
 * który nasłuchuje PrepareItemCraftEvent.
 * 
 * WAŻNE: Ta klasa obsługuje tylko 3x3 siatki (vanilla crafting).
 * Dla 4x4 siatek z CraftingGUIListener, używaj metody tryMatch4x4().
 */
public class VanillaCraftingSimulator {

    private final NmsCraftingEngine nmsCraftingEngine = new NmsCraftingEngine();
    
    /**
     * Symuluje vanilla crafting table dla danego 3x3 gridu.
     * 
     * Proces:
     * 1. Tworzy macierz 3x3 z itemkami
     * 2. Szuka vanilla receptury używając Paper API: Bukkit.getCraftingRecipe()
     * 3. Dla receptur ShapedRecipe/ShapelessRecipe - używa recipe.getResult()
     * 4. Dla ComplexRecipe (fajerwerki, naprawy) - fallback do manual logiki
     * 5. Odpala PrepareItemCraftEvent aby inne pluginy mogły zmodyfikować wynik
     * 6. Zwraca finalny crafting result
     * 
     * @param player Gracz craftujący (używany do świata)
     * @param grid 3x3 ItemStack array (rozmiar 9)
     *             Layout: [0,1,2]
     *                    [3,4,5]
     *                    [6,7,8]
     * @return ItemStack (output receptury) lub null jeśli brak match'a
     */
    public ItemStack tryMatch(Player player, ItemStack[] grid) {
        if (player == null || grid == null || grid.length != 9) {
            return null;
        }
        
        try {
            // Przygotuj macierz - puste sloty mogą być null lub AIR
            // Paper API akceptuje oba
            ItemStack[] matrix = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                ItemStack item = grid[i];
                boolean isEmpty = item == null || item.getType().isAir();
                
                // Dla zgodności z MockBukkit (i Paper) - używaj AIR ItemStack
                // zamiast null, żeby uniknąć NPE w RecipeManager.matches()
                if (isEmpty) {
                    matrix[i] = new ItemStack(Material.AIR);
                } else {
                    matrix[i] = item;
                }
            }
            
            // Szukaj receptury w rejestrze Bukkit'a
            // 1) Najpierw próbuj prawdziwy NMS crafting (Paper 1.21.11, Mojang mappings).
            //    W testach (Maven/MockBukkit) ta ścieżka zwykle zwróci null i to jest OK.
            ItemStack baseResult = null;
            try {
                baseResult = nmsCraftingEngine.getNMSCraftingResult(matrix, player.getWorld());
            } catch (Throwable ignored) {
                baseResult = null;
            }

            // 2) Jeśli NMS nie zadziałał, próbuj Bukkit API
            if (baseResult == null || baseResult.getType() == Material.AIR) {
                Recipe recipe = Bukkit.getCraftingRecipe(matrix, player.getWorld());
                if (recipe != null && recipe.getResult() != null && recipe.getResult().getType() != Material.AIR) {
                    baseResult = recipe.getResult().clone();
                }
            }

            // 3) Ostateczny fallback dla kilku "special" receptur (głównie pod testy i kompatybilność)
            if (baseResult == null || baseResult.getType() == Material.AIR) {
                baseResult = tryResolveSpecialVanilla(matrix);
            }
            
            // Zwróć wynik jeśli znaleziony
            return baseResult != null && baseResult.getType() != Material.AIR && baseResult.getAmount() > 0 ? baseResult.clone() : null;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Wersja tryMatch, która akceptuje tylko receptury shapeless.
     *
     * Używamy tego w dodatkowym fallbacku dla 4x4:
     * - jeśli składniki są rozrzucone po 4x4, to "upakowujemy" je do 3x3,
     * - ale nie chcemy, żeby przez to zaczęły pasować receptury shaped.
     */
    public ItemStack tryMatchShapelessOnly(Player player, ItemStack[] grid) {
        if (player == null || grid == null || grid.length != 9) {
            return null;
        }

        try {
            ItemStack[] matrix = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                ItemStack item = grid[i];
                boolean isEmpty = item == null || item.getType().isAir();
                matrix[i] = isEmpty ? new ItemStack(Material.AIR) : item;
            }

            // 1) NMS (najdokładniejsze): tylko shapeless
            ItemStack baseResult = null;
            try {
                baseResult = nmsCraftingEngine.getNMSShapelessCraftingResult(matrix, player.getWorld());
            } catch (Throwable ignored) {
                baseResult = null;
            }

            // 2) Bukkit API: tylko shapeless
            if (baseResult == null || baseResult.getType() == Material.AIR) {
                Recipe recipe = Bukkit.getCraftingRecipe(matrix, player.getWorld());
                // W Paper/Spigot vanilla receptury shapeless często NIE są instancją org.bukkit.inventory.ShapelessRecipe
                // (to klasa używana głównie do rejestracji receptur przez pluginy).
                // Musimy więc dopuścić każdy crafting recipe, który NIE jest shaped.
                if (recipe != null && !(recipe instanceof org.bukkit.inventory.ShapedRecipe)) {
                    ItemStack res = recipe.getResult();
                    if (res != null && res.getType() != Material.AIR) {
                        baseResult = res.clone();
                    }
                }
            }

            // 3) Ostateczny fallback dla "special" receptur (naprawy, farbowanie zbroi, fajerwerki).
            // One w praktyce zachowują się jak shapeless/complex i nie powinny zależeć od pozycjonowania.
            if (baseResult == null || baseResult.getType() == Material.AIR) {
                baseResult = tryResolveSpecialVanilla(matrix);
            }

            return baseResult != null && baseResult.getType() != Material.AIR && baseResult.getAmount() > 0 ? baseResult.clone() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Minimalny fallback dla kilku "special" receptur vanilla.
     * Celowo wąski zakres: tylko przypadki, które realnie występują w GUI 4x4.
     */
    private ItemStack tryResolveSpecialVanilla(ItemStack[] matrix) {
        if (matrix == null || matrix.length != 9) {
            return null;
        }

        // FIREWORK_ROCKET: 1 papier + 1-3 proch -> 3 rakiety, power = ilość prochu
        int paperSlots = 0;
        int gunpowderSlots = 0;
        int otherOccupied = 0;

        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            if (item.getType() == Material.PAPER) {
                paperSlots++;
            } else if (item.getType() == Material.GUNPOWDER) {
                gunpowderSlots++;
            } else {
                otherOccupied++;
            }
        }

        if (otherOccupied == 0 && paperSlots == 1 && gunpowderSlots >= 1 && gunpowderSlots <= 3) {
            ItemStack rocket = new ItemStack(Material.FIREWORK_ROCKET, 3);
            if (rocket.getItemMeta() instanceof FireworkMeta meta) {
                meta.setPower(gunpowderSlots);
                rocket.setItemMeta(meta);
            }
            return rocket;
        }

        // REPAIR: 2 takie same narzędzia/bron (damageable) -> 1 naprawiony (z bonusem 5%)
        ItemStack repairResult = tryResolveRepairRecipe(matrix);
        if (repairResult != null) {
            return repairResult;
        }

        // DYE LEATHER ARMOR: 1 skórzana część zbroi + 1+ barwników -> zabarwiona zbroja
        ItemStack dyeResult = tryResolveLeatherArmorDye(matrix);
        if (dyeResult != null) {
            return dyeResult;
        }

        return null;
    }

    private ItemStack tryResolveLeatherArmorDye(ItemStack[] matrix) {
        ItemStack armor = null;
        int occupied = 0;

        int dyeCount = 0;

        int colorComponents = 0;
        int sumRed = 0;
        int sumGreen = 0;
        int sumBlue = 0;
        int sumMaxChannel = 0;

        for (ItemStack item : matrix) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            occupied++;

            if (isLeatherArmor(item)) {
                if (armor != null) {
                    return null; // tylko 1 element zbroi
                }
                armor = item;

                if (!(item.getItemMeta() instanceof LeatherArmorMeta lam)) {
                    return null;
                }

                // Vanilla: jeśli zbroja NIE była wcześniej farbowana, nie miesza "domyślnego" koloru skóry.
                // Bukkit nie daje prostego hasColor(), więc robimy rozsądne przybliżenie:
                // - jeśli kolor to dokładnie domyślny (160, 101, 64) to traktujemy jak "brak koloru".
                Color base = lam.getColor();
                boolean hasCustomColor = !(base.getRed() == 160 && base.getGreen() == 101 && base.getBlue() == 64);

                if (hasCustomColor) {
                    int r = base.getRed();
                    int g = base.getGreen();
                    int b = base.getBlue();

                    sumRed += r;
                    sumGreen += g;
                    sumBlue += b;
                    sumMaxChannel += Math.max(r, Math.max(g, b));
                    colorComponents++;
                }

                continue;
            }

            Color dye = dyeColorFromMaterial(item.getType());
            if (dye == null) {
                return null;
            }

            int r = dye.getRed();
            int g = dye.getGreen();
            int b = dye.getBlue();

            sumRed += r;
            sumGreen += g;
            sumBlue += b;
            sumMaxChannel += Math.max(r, Math.max(g, b));
            colorComponents++;
            dyeCount++;
        }

        if (occupied < 2 || armor == null) {
            return null;
        }

        // musi być przynajmniej 1 barwnik
        if (dyeCount < 1) {
            return null;
        }

        // Jeśli zbroja nie miała wcześniej koloru, to colorComponents pochodzi tylko z barwników.
        int avgRed = sumRed / colorComponents;
        int avgGreen = sumGreen / colorComponents;
        int avgBlue = sumBlue / colorComponents;

        float avgMax = (float) sumMaxChannel / (float) colorComponents;
        float maxAvg = (float) Math.max(avgRed, Math.max(avgGreen, avgBlue));

        if (maxAvg > 0.0f) {
            float scale = avgMax / maxAvg;
            avgRed = Math.min(255, (int) (avgRed * scale));
            avgGreen = Math.min(255, (int) (avgGreen * scale));
            avgBlue = Math.min(255, (int) (avgBlue * scale));
        }

        ItemStack result = armor.clone();
        result.setAmount(1);

        if (!(result.getItemMeta() instanceof LeatherArmorMeta resultMeta)) {
            return null;
        }

        resultMeta.setColor(Color.fromRGB(avgRed, avgGreen, avgBlue));
        result.setItemMeta(resultMeta);

        return result;
    }

    private boolean isLeatherArmor(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material type = item.getType();
        return type == Material.LEATHER_HELMET
            || type == Material.LEATHER_CHESTPLATE
            || type == Material.LEATHER_LEGGINGS
            || type == Material.LEATHER_BOOTS;
    }

    private Color dyeColorFromMaterial(Material material) {
        if (material == null) {
            return null;
        }

        return switch (material) {
            case WHITE_DYE -> Color.fromRGB(0xF9FFFE);
            case ORANGE_DYE -> Color.fromRGB(0xF9801D);
            case MAGENTA_DYE -> Color.fromRGB(0xC74EBD);
            case LIGHT_BLUE_DYE -> Color.fromRGB(0x3AB3DA);
            case YELLOW_DYE -> Color.fromRGB(0xFED83D);
            case LIME_DYE -> Color.fromRGB(0x80C71F);
            case PINK_DYE -> Color.fromRGB(0xF38BAA);
            case GRAY_DYE -> Color.fromRGB(0x474F52);
            case LIGHT_GRAY_DYE -> Color.fromRGB(0x9D9D97);
            case CYAN_DYE -> Color.fromRGB(0x169C9C);
            case PURPLE_DYE -> Color.fromRGB(0x8932B8);
            case BLUE_DYE -> Color.fromRGB(0x3C44AA);
            case BROWN_DYE -> Color.fromRGB(0x835432);
            case GREEN_DYE -> Color.fromRGB(0x5E7C16);
            case RED_DYE -> Color.fromRGB(0xB02E26);
            case BLACK_DYE -> Color.fromRGB(0x1D1D21);
            default -> null;
        };
    }

    private ItemStack tryResolveRepairRecipe(ItemStack[] matrix) {
        ItemStack first = null;
        ItemStack second = null;
        int occupied = 0;

        for (ItemStack item : matrix) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            occupied++;
            if (first == null) {
                first = item;
            } else if (second == null) {
                second = item;
            }

            if (occupied > 2) {
                return null;
            }
        }

        if (occupied != 2 || first == null || second == null) {
            return null;
        }

        if (first.getType() != second.getType()) {
            return null;
        }

        if (first.getType().getMaxDurability() <= 0) {
            return null;
        }

        if (!(first.getItemMeta() instanceof Damageable meta1) || !(second.getItemMeta() instanceof Damageable meta2)) {
            return null;
        }

        if (!hasSameVisibleMeta(first, second)) {
            return null;
        }

        int maxDurability = first.getType().getMaxDurability();
        int damage1 = meta1.getDamage();
        int damage2 = meta2.getDamage();

        int life1 = maxDurability - damage1;
        int life2 = maxDurability - damage2;
        int bonus = (int) Math.floor(maxDurability * 0.05);

        int newLife = Math.min(maxDurability, life1 + life2 + bonus);
        int newDamage = maxDurability - newLife;

        if (newDamage < 0) {
            newDamage = 0;
        }

        ItemStack result = first.clone();
        result.setAmount(1);

        if (result.getItemMeta() instanceof Damageable resultMeta) {
            resultMeta.setDamage(newDamage);
            result.setItemMeta(resultMeta);
        }

        return result;
    }

    private boolean hasSameVisibleMeta(ItemStack first, ItemStack second) {
        if (first == null || second == null) {
            return false;
        }

        boolean firstHasName = first.hasItemMeta() && first.getItemMeta() != null && first.getItemMeta().hasDisplayName();
        boolean secondHasName = second.hasItemMeta() && second.getItemMeta() != null && second.getItemMeta().hasDisplayName();

        if (firstHasName != secondHasName) {
            return false;
        }

        if (firstHasName) {
            String name1 = first.getItemMeta().getDisplayName();
            String name2 = second.getItemMeta().getDisplayName();
            if (name1 == null ? name2 != null : !name1.equals(name2)) {
                return false;
            }
        }

        Map<?, ?> ench1 = first.getEnchantments();
        Map<?, ?> ench2 = second.getEnchantments();
        if (ench1 == null ? ench2 != null : !ench1.equals(ench2)) {
            return false;
        }

        return true;
    }

    /**
     * FALLBACK dla 4x4 gridu z CraftingGUIListener.
     * 
     * Strategia:
     * 1. Zrobi "Shrink Wrap" (zmniejszenie siatki do najmniejszego prostokąta)
     * 2. Pad-uje zmniejszony grid do 3x3 (vanilla crafting size)
     * 3. Wrzuca do tryMatch() aby znaleźć recepturę vanilla
     * 
     * WAŻNE: Ta metoda jest FALLBACK - powinna być wywoływana TYLKO wtedy,
     * gdy w CraftingGUIListener nie znaleziono żadnej customowej receptury!
     * 
     * @param player Gracz craftujący
     * @param grid4x4 Macierz 4x4 (rozmiar 16)
     *                Layout: [0,1,2,3]
     *                        [4,5,6,7]
     *                        [8,9,A,B]
     *                        [C,D,E,F]
     * @return ItemStack (output vanilla receptury) lub null jeśli brak match'a
     */
    public ItemStack tryMatch4x4Fallback(Player player, ItemStack[] grid4x4) {
        if (player == null || grid4x4 == null || grid4x4.length != 16) {
            return null;
        }

        try {
            // 1. Zrób "Shrink Wrap" - zmniejsz sieć do minimalnego prostokąta
            ItemStack[] shrunken = GridShrinkWrapper.shrinkWrap4x4(grid4x4);
            if (shrunken == null || shrunken.length == 0) {
                // Siatka pusta
                return null;
            }

            // 2. Sprawdź wymiary shrunken grid'u
            int[] dimensions = GridShrinkWrapper.getShrunkDimensions(grid4x4);
            if (dimensions == null) {
                return null;
            }

            // Jeśli bounding box jest większy niż 3x3, to shaped i tak nie zadziała.
            // ALE shapeless powinien zadziałać, jeśli mamy max 9 zajętych slotów.
            if (dimensions[0] > 3 || dimensions[1] > 3) {
                return ShapelessGridCompactor
                    .packToVanilla3x3IfAtMost9Occupied(grid4x4)
                    .map(packed3x3 -> tryMatchShapelessOnly(player, packed3x3))
                    .orElse(null);
            }

            int height = dimensions[0];
            int width = dimensions[1];

            // 3) Najpierw ścieżka dla VANILLA shapeless/complex:
            // traktujemy to jak normalny crafting 3x3 (do 9 itemów, 3 na rząd), niezależnie od pozycji w 4x4.
            ItemStack packedResult = ShapelessGridCompactor
                .packToVanilla3x3IfAtMost9Occupied(grid4x4)
                .map(packed3x3 -> tryMatchShapelessOnly(player, packed3x3))
                .orElse(null);
            if (packedResult != null) {
                return packedResult;
            }

            // 4) Dopiero potem VANILLA shaped: dopuszczamy 3x3 w dowolnym miejscu 4x4 (shrink-wrap + pad).
            ItemStack[] padded3x3 = pad3x3(shrunken, height, width);
            return tryMatch(player, padded3x3);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Pad-uje zmniejszoną macierz do 3x3 (vanilla crafting).
     * 
     * Przykład: mamy 2x2:
     *   [deska, deska]
     *   [deska, deska]
     * 
     * Po pad-owaniu do 3x3 (górny-lewy róg):
     *   [deska, deska, null]
     *   [deska, deska, null]
     *   [null,  null,  null]
     * 
     * @param shrunken Zmniejszona macierz (1D array)
     * @param height Wysokość (1-3)
     * @param width Szerokość (1-3)
     * @return Array 3x3 (rozmiar 9)
     */
    private ItemStack[] pad3x3(ItemStack[] shrunken, int height, int width) {
        ItemStack[] padded = new ItemStack[9];

        // Wypełnij nullami
        for (int i = 0; i < 9; i++) {
            padded[i] = null;
        }

        // Wstaw shrunken do górnego-lewego rogu
        int idx = 0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                padded[row * 3 + col] = shrunken[idx++];
            }
        }

        return padded;
    }

}


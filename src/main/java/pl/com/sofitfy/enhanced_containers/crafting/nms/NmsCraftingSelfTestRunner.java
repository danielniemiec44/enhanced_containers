package pl.com.sofitfy.enhanced_containers.crafting.nms;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.DyeColor;
import org.bukkit.block.Banner;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.map.MapView;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.FireworkEffect;
import org.bukkit.plugin.java.JavaPlugin;
import pl.com.sofitfy.enhanced_containers.crafting.suggestions.NmsVanillaSuggestion;
import pl.com.sofitfy.enhanced_containers.crafting.suggestions.NmsVanillaSuggestionEngine;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Self-test uruchamiany TYLKO w testach integracyjnych z prawdziwym Paper serwerem.
 *
 * Cel:
 * - faktycznie wywołać NMS recipe.assemble(...)
 * - sprawdzić matematykę craftingu (repair + dye)
 * - zapisać wynik do pliku, żeby JUnit mógł to łatwo zweryfikować bez wchodzenia do gry.
 *
 * Włączanie:
 * - ustaw JVM property: -Dplethoras.nmsSelfTest=true
 */
public final class NmsCraftingSelfTestRunner {

    private static final String FLAG = "plethoras.nmsSelfTest";
    private static final String OUTPUT_FILE = "nms-selftest.properties";

    private final JavaPlugin plugin;

    public NmsCraftingSelfTestRunner(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void maybeRun() {
        if (!Boolean.getBoolean(FLAG)) {
            return;
        }

        // Odpalamy tick później, żeby serwer zdążył wstać.
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                runAndWriteResults();
            } finally {
                // Po self-teście kończymy serwer, żeby test JUnit nie wisiał.
                Bukkit.shutdown();
            }
        });
    }

    private void runAndWriteResults() {
        File out = new File(plugin.getDataFolder(), OUTPUT_FILE);
        plugin.getDataFolder().mkdirs();

        boolean ok = true;
        StringBuilder sb = new StringBuilder();

        try {
            World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (world == null) {
                ok = false;
                sb.append("ok=false\n");
                sb.append("error=No world loaded\n");
                write(out, sb);
                return;
            }

            NmsCraftingEngine engine = new NmsCraftingEngine();

            // =====================
            // 1) REPAIR TEST
            // =====================
            ItemStack a = new ItemStack(Material.IRON_PICKAXE);
            ItemStack b = new ItemStack(Material.IRON_PICKAXE);

            int max = a.getType().getMaxDurability();

            setDamage(a, max - 50);   // zostało 50 życia
            setDamage(b, max - 100);  // zostało 100 życia

            ItemStack[] repairMatrix = new ItemStack[9];
            repairMatrix[0] = a;
            repairMatrix[1] = b;

            ItemStack repairResult = engine.getNMSCraftingResult(repairMatrix, world);
            Integer repairActualDamage = getDamageOrNull(repairResult);

            int life1 = 50;
            int life2 = 100;
            int bonus = (int) Math.floor(max * 0.05);
            int expectedLife = Math.min(max, life1 + life2 + bonus);
            int expectedDamage = max - expectedLife;
            if (expectedDamage < 0) expectedDamage = 0;

            boolean repairOk = repairResult != null
                && repairResult.getType() == Material.IRON_PICKAXE
                && repairActualDamage != null
                && repairActualDamage == expectedDamage;

            sb.append("repair.ok=").append(repairOk).append("\n");
            sb.append("repair.expectedDamage=").append(expectedDamage).append("\n");
            sb.append("repair.actualDamage=").append(repairActualDamage).append("\n");

            // =====================
            // 2) NMS SUGGESTIONS TEST (VANILLA)
            // =====================
            // Dajemy "ekwipunek" jako tablicę ItemStacków.
            // Chcemy sprawdzić, że NMS potrafi znaleźć craftowalny przepis,
            // oraz że matryca z podpowiedzi daje poprawny wynik przez assemble().

            NmsVanillaSuggestionEngine suggestionEngine = new NmsVanillaSuggestionEngine();
            ItemStack[] fakeInventory = new ItemStack[] { new ItemStack(Material.OAK_LOG, 1) };
            List<NmsVanillaSuggestion> suggestions = suggestionEngine.computeSuggestions(fakeInventory, world, 5);

            boolean suggestionsOk = suggestions != null && !suggestions.isEmpty();
            boolean planksOk = false;
            if (suggestionsOk) {
                for (NmsVanillaSuggestion s : suggestions) {
                    if (s == null || s.getOutput() == null) {
                        continue;
                    }
                    if (s.getOutput().getType() != Material.OAK_PLANKS) {
                        continue;
                    }

                    ItemStack[] matrix = s.getMatrix3x3();
                    if (matrix == null || matrix.length != 9) {
                        continue;
                    }

                    ItemStack crafted = engine.getNMSCraftingResult(matrix, world);
                    planksOk = crafted != null
                        && crafted.getType() == Material.OAK_PLANKS
                        && crafted.getAmount() == 4;
                    break;
                }
            }

            sb.append("suggestions.ok=").append(suggestionsOk).append("\n");
            sb.append("suggestions.planks.ok=").append(planksOk).append("\n");

            if (!repairOk) ok = false;

            // =====================
            // 1b) REPAIR CLAMP (over-repair)
            // =====================
            ItemStack c = new ItemStack(Material.IRON_PICKAXE);
            ItemStack d = new ItemStack(Material.IRON_PICKAXE);
            setDamage(c, 1);
            setDamage(d, 1);

            ItemStack[] repairClampMatrix = new ItemStack[9];
            repairClampMatrix[3] = c;
            repairClampMatrix[4] = d;

            ItemStack repairClampResult = engine.getNMSCraftingResult(repairClampMatrix, world);
            Integer clampDamage = getDamageOrNull(repairClampResult);

            // W vanilla wynik nie może mieć ujemnego damage → clamp do 0.
            boolean repairClampOk = repairClampResult != null
                && repairClampResult.getType() == Material.IRON_PICKAXE
                && clampDamage != null
                && clampDamage == 0;

            sb.append("repairClamp.ok=").append(repairClampOk).append("\n");
            sb.append("repairClamp.actualDamage=").append(clampDamage).append("\n");
            if (!repairClampOk) ok = false;

            // =====================
            // 2) DYE TEST
            // =====================
            ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
            ItemStack redDye = new ItemStack(Material.RED_DYE);

            ItemStack[] dyeMatrix = new ItemStack[9];
            dyeMatrix[0] = helmet;
            dyeMatrix[1] = redDye;

            ItemStack dyeResult = engine.getNMSCraftingResult(dyeMatrix, world);
            Color dyeActual = getLeatherColorOrNull(dyeResult);

            // WAŻNE: vanilla nie miesza "domyślnego" koloru skóry, jeśli zbroja nie była wcześniej farbowana.
            // Dla 1 barwnika oczekujemy dokładnie koloru barwnika.
            Color expected = Color.fromRGB(0xB02E26); // RED_DYE

            boolean dyeOk = dyeResult != null
                && dyeResult.getType() == Material.LEATHER_HELMET
                && dyeActual != null
                && dyeActual.asRGB() == expected.asRGB();

            sb.append("dye.ok=").append(dyeOk).append("\n");
            sb.append("dye.expectedRgb=").append(expected.getRed()).append(",").append(expected.getGreen()).append(",").append(expected.getBlue()).append("\n");
            sb.append("dye.actualRgb=");
            if (dyeActual == null) {
                sb.append("null\n");
            } else {
                sb.append(dyeActual.getRed()).append(",").append(dyeActual.getGreen()).append(",").append(dyeActual.getBlue()).append("\n");
            }

            if (!dyeOk) ok = false;

            // =====================
            // 2b) DYE MULTI (2 barwniki, zbroja NIE farbowana)
            // =====================
            ItemStack helmet2 = new ItemStack(Material.LEATHER_HELMET);
            ItemStack blue = new ItemStack(Material.BLUE_DYE);
            ItemStack white = new ItemStack(Material.WHITE_DYE);

            ItemStack[] dyeMultiMatrix = new ItemStack[9];
            dyeMultiMatrix[0] = helmet2;
            dyeMultiMatrix[1] = blue;
            dyeMultiMatrix[2] = white;

            ItemStack dyeMultiResult = engine.getNMSCraftingResult(dyeMultiMatrix, world);
            Color dyeMultiActual = getLeatherColorOrNull(dyeMultiResult);
            Color dyeMultiExpected = mixDyeColorsVanillaLike(
                null,
                new Color[]{Color.fromRGB(0x3C44AA), Color.fromRGB(0xF9FFFE)}
            );

            boolean dyeMultiOk = dyeMultiResult != null
                && dyeMultiResult.getType() == Material.LEATHER_HELMET
                && dyeMultiActual != null
                && dyeMultiActual.asRGB() == dyeMultiExpected.asRGB();

            sb.append("dyeMulti.ok=").append(dyeMultiOk).append("\n");
            sb.append("dyeMulti.expectedRgb=").append(rgb(dyeMultiExpected)).append("\n");
            sb.append("dyeMulti.actualRgb=").append(rgb(dyeMultiActual)).append("\n");
            if (!dyeMultiOk) ok = false;

            // =====================
            // 2c) DYE MULTI (2 barwniki, zbroja JUŻ farbowana)
            // =====================
            ItemStack helmet3 = new ItemStack(Material.LEATHER_HELMET);
            setLeatherColor(helmet3, Color.fromRGB(10, 20, 30));

            ItemStack[] dyeMultiDyedMatrix = new ItemStack[9];
            dyeMultiDyedMatrix[0] = helmet3;
            dyeMultiDyedMatrix[1] = blue;
            dyeMultiDyedMatrix[2] = white;

            ItemStack dyeMultiDyedResult = engine.getNMSCraftingResult(dyeMultiDyedMatrix, world);
            Color dyeMultiDyedActual = getLeatherColorOrNull(dyeMultiDyedResult);
            Color dyeMultiDyedExpected = mixDyeColorsVanillaLike(
                Color.fromRGB(10, 20, 30),
                new Color[]{Color.fromRGB(0x3C44AA), Color.fromRGB(0xF9FFFE)}
            );

            boolean dyeMultiDyedOk = dyeMultiDyedResult != null
                && dyeMultiDyedResult.getType() == Material.LEATHER_HELMET
                && dyeMultiDyedActual != null
                && dyeMultiDyedActual.asRGB() == dyeMultiDyedExpected.asRGB();

            sb.append("dyeMultiDyed.ok=").append(dyeMultiDyedOk).append("\n");
            sb.append("dyeMultiDyed.expectedRgb=").append(rgb(dyeMultiDyedExpected)).append("\n");
            sb.append("dyeMultiDyed.actualRgb=").append(rgb(dyeMultiDyedActual)).append("\n");
            if (!dyeMultiDyedOk) ok = false;

            // =====================
            // 3) FIREWORK ROCKET (NBT/power przez assemble)
            // =====================
            ItemStack paper = new ItemStack(Material.PAPER);
            ItemStack gunpowder1 = new ItemStack(Material.GUNPOWDER);
            ItemStack gunpowder2 = new ItemStack(Material.GUNPOWDER);

            ItemStack[] fireworkMatrix = new ItemStack[9];
            fireworkMatrix[0] = paper;
            fireworkMatrix[1] = gunpowder1;
            fireworkMatrix[2] = gunpowder2;

            ItemStack fireworkResult = engine.getNMSCraftingResult(fireworkMatrix, world);
            Integer power = getFireworkPowerOrNull(fireworkResult);
            boolean fireworkOk = fireworkResult != null
                && fireworkResult.getType() == Material.FIREWORK_ROCKET
                && fireworkResult.getAmount() == 3
                && power != null
                && power == 2;

            sb.append("firework.ok=").append(fireworkOk).append("\n");
            sb.append("firework.actualPower=").append(power).append("\n");
            sb.append("firework.actualAmount=").append(fireworkResult == null ? "null" : fireworkResult.getAmount()).append("\n");
            if (!fireworkOk) ok = false;

            // =====================
            // 4) MAP CLONING (NBT: map id)
            // =====================
            MapView view = Bukkit.createMap(world);
            ItemStack filledMap = new ItemStack(Material.FILLED_MAP);
            MapMeta mapMeta = (MapMeta) filledMap.getItemMeta();
            mapMeta.setMapView(view);
            filledMap.setItemMeta(mapMeta);

            ItemStack emptyMap = new ItemStack(Material.MAP);

            ItemStack[] mapCloneMatrix = new ItemStack[9];
            mapCloneMatrix[0] = filledMap;
            mapCloneMatrix[1] = emptyMap;

            ItemStack mapCloneResult = engine.getNMSCraftingResult(mapCloneMatrix, world);
            Integer mapCloneId = getMapIdOrNull(mapCloneResult);

            boolean mapCloneOk = mapCloneResult != null
                && mapCloneResult.getType() == Material.FILLED_MAP
                && mapCloneResult.getAmount() == 2
                && mapCloneId != null
                && mapCloneId == view.getId();

            sb.append("mapClone.ok=").append(mapCloneOk).append("\n");
            sb.append("mapClone.expectedId=").append(view.getId()).append("\n");
            sb.append("mapClone.actualId=").append(mapCloneId).append("\n");
            sb.append("mapClone.actualAmount=").append(mapCloneResult == null ? "null" : mapCloneResult.getAmount()).append("\n");
            if (!mapCloneOk) ok = false;

            // =====================
            // 5) SHIELD + BANNER (NBT: banner patterns)
            // =====================
            ItemStack banner = new ItemStack(Material.WHITE_BANNER);
            BannerMeta bannerMeta = (BannerMeta) banner.getItemMeta();
            Pattern expectedPattern = new Pattern(DyeColor.BLACK, PatternType.STRIPE_CENTER);
            bannerMeta.setPatterns(java.util.List.of(expectedPattern));
            banner.setItemMeta(bannerMeta);

            ItemStack shield = new ItemStack(Material.SHIELD);

            ItemStack[] shieldBannerMatrix = new ItemStack[9];
            shieldBannerMatrix[0] = banner;
            shieldBannerMatrix[1] = shield;

            ItemStack shieldBannerResult = engine.getNMSCraftingResult(shieldBannerMatrix, world);
            Pattern actualPattern = getShieldFirstPatternOrNull(shieldBannerResult);
            boolean shieldBannerOk = shieldBannerResult != null
                && shieldBannerResult.getType() == Material.SHIELD
                && actualPattern != null
                && actualPattern.getColor() == expectedPattern.getColor()
                && actualPattern.getPattern() == expectedPattern.getPattern();

            sb.append("shieldBanner.ok=").append(shieldBannerOk).append("\n");
            sb.append("shieldBanner.expected=").append(expectedPattern.getColor()).append(":").append(expectedPattern.getPattern()).append("\n");
            sb.append("shieldBanner.actual=").append(actualPattern == null ? "null" : (actualPattern.getColor() + ":" + actualPattern.getPattern())).append("\n");
            if (!shieldBannerOk) ok = false;

            // =====================
            // 6) SUSPICIOUS STEW (NBT: custom effect)
            // =====================
            ItemStack bowl = new ItemStack(Material.BOWL);
            ItemStack redMushroom = new ItemStack(Material.RED_MUSHROOM);
            ItemStack brownMushroom = new ItemStack(Material.BROWN_MUSHROOM);
            ItemStack witherRose = new ItemStack(Material.WITHER_ROSE);

            ItemStack[] stewMatrix = new ItemStack[9];
            stewMatrix[0] = bowl;
            stewMatrix[1] = redMushroom;
            stewMatrix[2] = brownMushroom;
            stewMatrix[3] = witherRose;

            ItemStack stewResult = engine.getNMSCraftingResult(stewMatrix, world);
            boolean stewHasWither = hasStewEffect(stewResult, PotionEffectType.WITHER);
            boolean stewOk = stewResult != null
                && stewResult.getType() == Material.SUSPICIOUS_STEW
                && stewHasWither;

            sb.append("stew.ok=").append(stewOk).append("\n");
            sb.append("stew.hasWither=").append(stewHasWither).append("\n");
            if (!stewOk) ok = false;

            // =====================
            // 7) BANNER DUPLICATION (NBT: patterns copied)
            // =====================
            ItemStack patternedBanner = new ItemStack(Material.WHITE_BANNER);
            BannerMeta patternedMeta = (BannerMeta) patternedBanner.getItemMeta();
            Pattern bannerPattern = new Pattern(DyeColor.RED, PatternType.STRIPE_CENTER);
            patternedMeta.setPatterns(java.util.List.of(bannerPattern));
            patternedBanner.setItemMeta(patternedMeta);

            ItemStack blankBanner = new ItemStack(Material.WHITE_BANNER);

            ItemStack[] bannerDupMatrix = new ItemStack[9];
            bannerDupMatrix[0] = patternedBanner;
            bannerDupMatrix[1] = blankBanner;

            ItemStack bannerDupResult = engine.getNMSCraftingResult(bannerDupMatrix, world);
            boolean bannerDupOk = bannerDupResult != null
                && bannerDupResult.getType() == Material.WHITE_BANNER
                && bannerDupResult.getAmount() >= 1
                && bannerHasPattern(bannerDupResult, bannerPattern);

            sb.append("bannerDup.ok=").append(bannerDupOk).append("\n");
            sb.append("bannerDup.actualAmount=").append(bannerDupResult == null ? "null" : bannerDupResult.getAmount()).append("\n");
            if (!bannerDupOk) ok = false;

            // =====================
            // 8) WRITTEN BOOK CLONE (NBT: title/author/pages)
            // =====================
            ItemStack written = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta writtenMeta = (BookMeta) written.getItemMeta();
            writtenMeta.setTitle("Test Title");
            writtenMeta.setAuthor("Test Author");
            writtenMeta.setPages(java.util.List.of("page-1", "page-2"));
            written.setItemMeta(writtenMeta);

            ItemStack writable = new ItemStack(Material.WRITABLE_BOOK);

            ItemStack[] bookCloneMatrix = new ItemStack[9];
            bookCloneMatrix[0] = written;
            bookCloneMatrix[1] = writable;

            ItemStack bookCloneResult = engine.getNMSCraftingResult(bookCloneMatrix, world);
            boolean bookCloneOk = bookCloneResult != null
                && bookCloneResult.getType() == Material.WRITTEN_BOOK
                && bookCloneResult.getAmount() >= 1
                && bookMatches(bookCloneResult, "Test Title", "Test Author", 2);

            sb.append("bookClone.ok=").append(bookCloneOk).append("\n");
            sb.append("bookClone.actualAmount=").append(bookCloneResult == null ? "null" : bookCloneResult.getAmount()).append("\n");
            if (!bookCloneOk) ok = false;

            // =====================
            // 9) SHULKER RECOLOR (type only, still via assemble)
            // =====================
            ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
            ItemStack shulkerDye = new ItemStack(Material.BLUE_DYE);

            ItemStack[] shulkerMatrix = new ItemStack[9];
            shulkerMatrix[0] = shulker;
            shulkerMatrix[1] = shulkerDye;

            ItemStack shulkerResult = engine.getNMSCraftingResult(shulkerMatrix, world);
            boolean shulkerOk = shulkerResult != null
                && shulkerResult.getType() == Material.BLUE_SHULKER_BOX
                && shulkerResult.getAmount() == 1;

            sb.append("shulker.ok=").append(shulkerOk).append("\n");
            if (!shulkerOk) ok = false;

            // =====================
            // 10) FIREWORK STAR (NBT: effect colors)
            // =====================
            ItemStack starGunpowder = new ItemStack(Material.GUNPOWDER);
            ItemStack starDye = new ItemStack(Material.RED_DYE);

            ItemStack[] starMatrix = new ItemStack[9];
            starMatrix[0] = starGunpowder;
            starMatrix[1] = starDye;

            ItemStack starResult = engine.getNMSCraftingResult(starMatrix, world);
            boolean starHasRed = fireworkStarHasColor(starResult, DyeColor.RED.getFireworkColor());
            boolean starOk = starResult != null
                && starResult.getType() == Material.FIREWORK_STAR
                && starHasRed;

            sb.append("fireworkStar.ok=").append(starOk).append("\n");
            sb.append("fireworkStar.hasRed=").append(starHasRed).append("\n");
            if (!starOk) ok = false;

            sb.insert(0, "ok=" + ok + "\n");
            write(out, sb);
        } catch (Throwable t) {
            sb.setLength(0);
            sb.append("ok=false\n");
            sb.append("error=").append(t.getClass().getName()).append(": ").append(safe(t.getMessage())).append("\n");
            write(out, sb);
        }
    }

    private static void setDamage(ItemStack item, int damage) {
        if (item == null || item.getType().isAir()) return;
        if (!(item.getItemMeta() instanceof Damageable d)) return;
        d.setDamage(Math.max(0, damage));
        item.setItemMeta(d);
    }

    private static Integer getDamageOrNull(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        if (!(item.getItemMeta() instanceof Damageable d)) return null;
        return d.getDamage();
    }

    private static Color getLeatherColorOrNull(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        if (!(item.getItemMeta() instanceof LeatherArmorMeta m)) return null;
        return m.getColor();
    }

    private static void setLeatherColor(ItemStack item, Color color) {
        if (item == null || item.getType().isAir() || color == null) return;
        if (!(item.getItemMeta() instanceof LeatherArmorMeta m)) return;
        m.setColor(color);
        item.setItemMeta(m);
    }

    private static Integer getFireworkPowerOrNull(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        if (!(item.getItemMeta() instanceof FireworkMeta m)) return null;
        return m.getPower();
    }

    private static Integer getMapIdOrNull(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        if (!(item.getItemMeta() instanceof MapMeta m)) return null;
        MapView v = m.getMapView();
        return v == null ? null : v.getId();
    }

    private static Pattern getShieldFirstPatternOrNull(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        if (!(item.getItemMeta() instanceof BlockStateMeta m)) return null;
        if (!(m.getBlockState() instanceof Banner b)) return null;
        java.util.List<Pattern> patterns = b.getPatterns();
        if (patterns == null || patterns.isEmpty()) return null;
        return patterns.get(0);
    }

    private static boolean hasStewEffect(ItemStack item, PotionEffectType type) {
        if (item == null || item.getType().isAir() || type == null) return false;
        if (!(item.getItemMeta() instanceof SuspiciousStewMeta m)) return false;
        // Paper/Bukkit: custom effects są mapowane z NBT.
        for (PotionEffect e : m.getCustomEffects()) {
            if (e != null && type.equals(e.getType())) {
                return true;
            }
        }
        return false;
    }

    private static boolean bannerHasPattern(ItemStack item, Pattern expected) {
        if (item == null || item.getType().isAir() || expected == null) return false;
        if (!(item.getItemMeta() instanceof BannerMeta m)) return false;
        java.util.List<Pattern> patterns = m.getPatterns();
        if (patterns == null) return false;
        for (Pattern p : patterns) {
            if (p == null) continue;
            if (p.getColor() == expected.getColor() && p.getPattern() == expected.getPattern()) {
                return true;
            }
        }
        return false;
    }

    private static boolean bookMatches(ItemStack item, String title, String author, int pages) {
        if (item == null || item.getType().isAir()) return false;
        if (!(item.getItemMeta() instanceof BookMeta m)) return false;
        if (title != null && !title.equals(m.getTitle())) return false;
        if (author != null && !author.equals(m.getAuthor())) return false;
        java.util.List<String> ps = m.getPages();
        return ps != null && ps.size() == pages;
    }

    private static boolean fireworkStarHasColor(ItemStack item, Color expected) {
        if (item == null || item.getType().isAir() || expected == null) return false;
        if (!(item.getItemMeta() instanceof FireworkEffectMeta m)) return false;
        FireworkEffect e = m.getEffect();
        if (e == null) return false;
        for (Color c : e.getColors()) {
            if (c != null && c.asRGB() == expected.asRGB()) {
                return true;
            }
        }
        return false;
    }

    private static String rgb(Color c) {
        if (c == null) return "null";
        return c.getRed() + "," + c.getGreen() + "," + c.getBlue();
    }

    /**
     * Vanilla-like mieszanie kolorów dla leather armor.
     *
     * Zasada:
     * - jeżeli zbroja była wcześniej farbowana, bierzemy jej kolor jako komponent,
     * - jeżeli nie była, bazę pomijamy,
     * - dodajemy wszystkie barwniki,
     * - liczymy średnią i normalizujemy jasność (tak jak w vanilla).
     */
    private static Color mixDyeColorsVanillaLike(Color baseIfAlreadyDyed, Color[] dyes) {
        int components = 0;
        int sumR = 0;
        int sumG = 0;
        int sumB = 0;
        int sumMax = 0;

        if (baseIfAlreadyDyed != null) {
            int r = baseIfAlreadyDyed.getRed();
            int g = baseIfAlreadyDyed.getGreen();
            int b = baseIfAlreadyDyed.getBlue();
            sumR += r;
            sumG += g;
            sumB += b;
            sumMax += Math.max(r, Math.max(g, b));
            components++;
        }

        if (dyes != null) {
            for (Color dye : dyes) {
                if (dye == null) continue;
                int r = dye.getRed();
                int g = dye.getGreen();
                int b = dye.getBlue();
                sumR += r;
                sumG += g;
                sumB += b;
                sumMax += Math.max(r, Math.max(g, b));
                components++;
            }
        }

        if (components <= 0) {
            return null;
        }

        int avgR = sumR / components;
        int avgG = sumG / components;
        int avgB = sumB / components;

        float avgMax = (float) sumMax / (float) components;
        float maxAvg = (float) Math.max(avgR, Math.max(avgG, avgB));

        if (maxAvg > 0.0f) {
            float scale = avgMax / maxAvg;
            avgR = Math.min(255, (int) (avgR * scale));
            avgG = Math.min(255, (int) (avgG * scale));
            avgB = Math.min(255, (int) (avgB * scale));
        }

        return Color.fromRGB(avgR, avgG, avgB);
    }

    private static void write(File out, StringBuilder content) {
        try {
            try (FileWriter fw = new FileWriter(out, StandardCharsets.UTF_8)) {
                fw.write(content.toString());
            }
        } catch (Exception ignored) {
            // Jeśli nie da się zapisać, to i tak test zobaczy brak pliku.
        }
    }

    private static String safe(String msg) {
        if (msg == null) return "";
        return msg.replace("\n", " ").replace("\r", " ");
    }
}

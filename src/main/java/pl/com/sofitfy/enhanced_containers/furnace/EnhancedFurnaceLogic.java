package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import pl.com.sofitfy.enhanced_containers.furnace.nms.NmsFurnaceEngineDirect;

/**
 * EnhancedFurnaceLogic - logika przepalania w rozszerzonym piecu
 * 
 * Tickuje co 1 tick (20 razy na sekundę) i:
 * - Zużywa paliwo proporcjonalnie do ilości aktywnych linii
 * - Przepala przedmioty w każdej aktywnej linii
 * - Używa NMS do pobierania przepisów i czasów palenia (vanilla logika)
 */
public class EnhancedFurnaceLogic {
    
    private final JavaPlugin plugin;
    private final EnhancedFurnaceManager manager;
    private NmsFurnaceEngineDirect nmsEngine; // Lazy initialization - unika problemów w MockBukkit
    
    // Fallback czasy przepalania gdy NMS niedostępny
    public static final int COOK_TIME_NORMAL = 200; // Zwykły piec - 10 sekund
    public static final int COOK_TIME_BLAST = 100;  // Blast furnace - 5 sekund
    public static final int COOK_TIME_SMOKER = 100; // Smoker - 5 sekund
    
    public EnhancedFurnaceLogic(JavaPlugin plugin, EnhancedFurnaceManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        // NMS engine jest tworzony lazy - dopiero gdy jest potrzebny
        // To pozwala MockBukkit testom działać bez inicjalizacji NMS
        this.nmsEngine = null;
    }
    
    /**
     * Lazy initialization NMS engine - tworzy instancję tylko gdy jest potrzebna
     * W środowisku testowym (MockBukkit) zwraca null i używa fallback logic
     */
    private NmsFurnaceEngineDirect getNmsEngine() {
        if (this.nmsEngine == null) {
            try {
                this.nmsEngine = new NmsFurnaceEngineDirect();
            } catch (Exception | NoClassDefFoundError e) {
                // NMS niedostępny (np. MockBukkit) - używamy fallback
                plugin.getLogger().warning("[EnhancedFurnace] NMS niedostępny, używam fallback logiki");
                return null;
            }
        }
        return this.nmsEngine;
    }
    
    /**
     * Startuje tickowanie pieców
     * Wywołuje tick() co 1 tick serwera
     */
    public void startTicking() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickAllFurnaces, 1L, 1L);
    }
    
    // Licznik ticków do okresowej walidacji pieców
    private int validationTickCounter = 0;
    
    /**
     * Tickuje wszystkie AKTYWNE piece (w załadowanych chunkach)
     * 
     * OPTYMALIZACJE:
     * 1. Tylko piece w załadowanych chunkach (Set aktywnych)
     * 2. Pomijaj puste piece (brak inputu i nie płonie)
     * 3. NMS cache przepisów - unikamy RecipeManager co tick
     * 4. Okresowa walidacja - co 20 ticków sprawdź czy bloki nadal istnieją
     */
    public void tickAllFurnaces() {
        validationTickCounter++;
        boolean shouldValidate = (validationTickCounter >= 20); // Co sekundę
        if (shouldValidate) validationTickCounter = 0;
        
        // Używamy kopii seta bo możemy usuwać podczas iteracji
        java.util.Set<Location> toRemove = null;
        
        for (Location loc : manager.getActiveFurnaceLocations()) {
            // Okresowa walidacja - czy blok nadal istnieje?
            if (shouldValidate && !isValidFurnaceBlock(loc)) {
                if (toRemove == null) toRemove = new java.util.HashSet<>();
                toRemove.add(loc);
                continue;
            }
            
            EnhancedFurnaceData data = manager.getFurnaceData(loc);
            if (data != null) {
                // OPTYMALIZACJA: Pomiń puste piece które nie płoną
                // Jeśli brak paliwa i brak inputu - nie ma co robić
                if (!data.isBurning() && !data.hasAnyInput()) {
                    continue;
                }
                tickFurnace(data, loc);
            }
        }
        
        // Usuń piece których bloki zostały usunięte (WorldEdit, komendy, itp.)
        if (toRemove != null) {
            for (Location loc : toRemove) {
                plugin.getLogger().warning("[EnhancedFurnace] Block removed externally at " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
                manager.removeFurnaceByLocation(loc);
            }
        }
    }
    
    /**
     * Sprawdza czy na lokalizacji jest blok pieca
     */
    private boolean isValidFurnaceBlock(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        Material type = loc.getBlock().getType();
        return type == Material.FURNACE || type == Material.BLAST_FURNACE || type == Material.SMOKER;
    }
    
    /**
     * Tickuje pojedynczy piec - ZOPTYMALIZOWANA WERSJA
     * 
     * Optymalizacje:
     * 1. NMS wywoływane tylko raz na linię (cache wyników)
     * 2. GUI aktualizowane tylko co 4 ticki (lub gdy zmiana stanu)
     * 3. Minimalna ilość operacji na tick
     * 
     * @param data dane pieca
     * @param location lokalizacja pieca (do ustawiania BlockData lit)
     */
    private void tickFurnace(EnhancedFurnaceData data, Location location) {
        World world = location.getWorld();
        EnhancedFurnaceGUI gui = data.getGui();
        
        // Cache wyników NMS dla każdej linii - unikamy podwójnego wywołania NMS
        ItemStack[] cachedResults = new ItemStack[3];
        int activeLines = 0;
        
        NmsFurnaceEngineDirect engine = getNmsEngine();
        
        // Jednokrotne sprawdzenie wszystkich linii + cache wyników
        for (int i = 0; i < 3; i++) {
            ItemStack input = gui.getInputItem(i);
            
            // Sprawdź czy item się zmienił - jeśli tak, resetuj cookTime
            // (np. gracz podmienił surowe mięso na rudę)
            data.checkAndResetIfInputChanged(i, input);
            
            if (input != null && input.getType() != Material.AIR) {
                ItemStack result;
                if (engine != null) {
                    result = engine.getSmeltingResultForFurnaceType(input, world, data.getFurnaceType());
                } else {
                    // Fallback uwzględnia typ pieca (smoker=jedzenie, blast=rudy)
                    result = getSimpleFallbackSmeltingResult(input, data.getFurnaceType());
                }
                
                if (result != null) {
                    ItemStack output = gui.getOutputItem(i);
                    if (canStackWith(output, result)) {
                        cachedResults[i] = result;
                        activeLines++;
                    }
                }
            }
        }
        
        // Poprzedni stan palenia (do wykrywania zmian)
        boolean wasBurning = data.getBurnTime() > 0;
        
        if (activeLines == 0) {
            // Brak przedmiotów do przepalenia
            // VANILLA BEHAVIOR: paliwo nadal się zużywa nawet bez przedmiotów!
            boolean justExtinguished = false;
            if (data.getBurnTime() > 0) {
                data.setBurnTime(data.getBurnTime() - 1);
                if (data.getBurnTime() == 0) {
                    justExtinguished = true;
                }
            }
            
            // GUI aktualizuj co 4 ticki LUB natychmiast gdy ogień właśnie zgasł
            if (data.getTickCounter() % 4 == 0 || justExtinguished) {
                gui.updateFireIndicator(data.getBurnTime(), data.getMaxBurnTime());
                for (int i = 0; i < 3; i++) {
                    gui.updateSmeltProgress(i, data.getCookTime(i), data.getMaxCookTime());
                }
            }
            data.incrementTickCounter();
            
            // Aktualizuj stan lit bloku tylko jeśli się zmienił
            if (wasBurning && data.getBurnTime() <= 0) {
                updateBlockLitState(location, false);
            }
            return;
        }
        
        // Jeśli nie ma aktywnego paliwa, spróbuj zużyć nowe
        if (data.getBurnTime() <= 0) {
            if (!tryConsumeFuel(data, world)) {
                // Brak paliwa - resetuj postęp wszystkich linii
                for (int i = 0; i < 3; i++) {
                    data.resetCookTime(i);
                }
                // GUI aktualizuj tylko co 4 ticki
                if (data.getTickCounter() % 4 == 0) {
                    for (int i = 0; i < 3; i++) {
                        gui.updateSmeltProgress(i, 0, data.getMaxCookTime());
                    }
                    gui.updateFireIndicator(0, 0);
                }
                data.incrementTickCounter();
                
                // Zgaś piec
                if (wasBurning) {
                    updateBlockLitState(location, false);
                }
                return;
            } else {
                // Właśnie zużyliśmy paliwo - zapal piec
                updateBlockLitState(location, true);
            }
        }
        
        // Zużyj paliwo proporcjonalnie do ilości aktywnych linii
        // UWAGA: Smoker i Blast Furnace przepalają 2x szybciej, więc zużywają 2x więcej paliwa!
        // To zapewnia spójną proporcjonalność: 1 patyk = ~0.5 itema niezależnie od typu pieca
        int fuelConsumptionRate = activeLines;
        if (data.getFurnaceType() == Material.SMOKER || data.getFurnaceType() == Material.BLAST_FURNACE) {
            fuelConsumptionRate = activeLines * 2; // 2x szybsze zużycie = ta sama proporcja co zwykły piec
        }
        data.setBurnTime(data.getBurnTime() - fuelConsumptionRate);
        
        // Przepalaj każdą linię używając cached results (bez ponownego NMS)
        for (int line = 0; line < 3; line++) {
            if (cachedResults[line] != null) {
                tickLineOptimized(data, line, cachedResults[line]);
            } else {
                // Linia nieaktywna - resetuj postęp jeśli był
                if (data.getCookTime(line) > 0) {
                    data.resetCookTime(line);
                }
            }
        }
        
        // GUI aktualizuj tylko co 10 ticków LUB gdy paliwo właśnie się skończyło
        // ANTI-FLICKER: Mniejsza częstotliwość = mniej pakietów = mniej migania
        boolean fuelJustRanOut = data.getBurnTime() <= 0 && wasBurning;
        boolean shouldUpdateGui = (data.getTickCounter() % 10 == 0) || fuelJustRanOut;
        if (shouldUpdateGui) {
            gui.updateFireIndicator(data.getBurnTime(), data.getMaxBurnTime());
            for (int i = 0; i < 3; i++) {
                gui.updateSmeltProgress(i, data.getCookTime(i), data.getMaxCookTime());
            }
        }
        data.incrementTickCounter();
        
        // Sprawdź czy paliwo się skończyło
        if (fuelJustRanOut) {
            updateBlockLitState(location, false);
        }
    }
    
    /**
     * Tickuje linię z już obliczonym wynikiem (bez NMS)
     */
    private void tickLineOptimized(EnhancedFurnaceData data, int lineIndex, ItemStack cachedResult) {
        // Zwiększ postęp przepalania
        data.incrementCookTime(lineIndex);
        
        // Sprawdź czy przepalanie zakończone
        if (data.getCookTime(lineIndex) >= data.getMaxCookTime()) {
            completeSmelt(data, lineIndex, cachedResult);
        }
    }
    
    /**
     * Aktualizuje stan "lit" bloku pieca w świecie
     * Piec zapalony (lit=true) pokazuje animację płomieni i świeci
     * 
     * @param location lokalizacja pieca
     * @param lit czy piec ma być zapalony
     */
    private void updateBlockLitState(Location location, boolean lit) {
        if (location == null || location.getWorld() == null) return;
        
        try {
            org.bukkit.block.Block block = location.getBlock();
            org.bukkit.block.data.BlockData blockData = block.getBlockData();
            
            // Sprawdź czy blok ma właściwość "lit" (piece ją mają)
            if (blockData instanceof org.bukkit.block.data.Lightable lightable) {
                // Zmień tylko jeśli stan jest inny
                if (lightable.isLit() != lit) {
                    lightable.setLit(lit);
                    block.setBlockData(blockData, false); // false = bez aktualizacji fizyki
                }
            }
        } catch (Exception e) {
            // Ignoruj błędy - np. chunk nie załadowany
        }
    }
    
    /**
     * Kończy przepalanie - dodaje wynik i zmniejsza input
     */
    private void completeSmelt(EnhancedFurnaceData data, int lineIndex, ItemStack result) {
        EnhancedFurnaceGUI gui = data.getGui();
        
        // Zmniejsz input o 1
        ItemStack input = gui.getInputItem(lineIndex);
        if (input.getAmount() > 1) {
            input.setAmount(input.getAmount() - 1);
        } else {
            gui.setInputItem(lineIndex, null);
        }
        
        // Dodaj wynik do output
        ItemStack currentOutput = gui.getOutputItem(lineIndex);
        if (currentOutput == null || currentOutput.getType() == Material.AIR) {
            gui.setOutputItem(lineIndex, result.clone());
        } else {
            currentOutput.setAmount(currentOutput.getAmount() + result.getAmount());
        }
        
        // Resetuj postęp przepalania
        data.resetCookTime(lineIndex);
    }
    
    /**
     * Próbuje zużyć paliwo z jednego ze slotów paliwa
     * Używa NMS do pobierania czasów palenia
     * Zwraca true jeśli udało się zużyć paliwo
     */
    private boolean tryConsumeFuel(EnhancedFurnaceData data, World world) {
        EnhancedFurnaceGUI gui = data.getGui();
        
        // Sprawdź każdy slot paliwa
        for (int i = 0; i < EnhancedFurnaceGUI.FUEL_SLOTS.length; i++) {
            ItemStack fuel = gui.getFuelItem(i);
            if (fuel != null && fuel.getType() != Material.AIR) {
                // Pobierz czas palenia z NMS lub fallback
                NmsFurnaceEngineDirect engine = getNmsEngine();
                int burnTime = engine != null ? engine.getFuelBurnTime(fuel, world) : getFuelBurnTime(fuel.getType());
                if (burnTime > 0) {
                    // Zużyj 1 paliwo
                    if (fuel.getAmount() > 1) {
                        fuel.setAmount(fuel.getAmount() - 1);
                    } else {
                        // Specjalny przypadek: lava bucket -> zwraca empty bucket
                        if (fuel.getType() == Material.LAVA_BUCKET) {
                            gui.setFuelItem(i, new ItemStack(Material.BUCKET));
                        } else {
                            gui.setFuelItem(i, null);
                        }
                    }
                    
                    // Ustaw czas palenia
                    data.setBurnTime(burnTime);
                    data.setMaxBurnTime(burnTime);
                    
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Sprawdza czy przedmiot może być dodany do stacka (lub stack jest pusty)
     */
    private boolean canStackWith(ItemStack existing, ItemStack toAdd) {
        if (existing == null || existing.getType() == Material.AIR) {
            return true;
        }
        
        if (!existing.isSimilar(toAdd)) {
            return false;
        }
        
        return existing.getAmount() + toAdd.getAmount() <= existing.getMaxStackSize();
    }
    
    /**
     * Prosty fallback dla przepalania gdy NMS niedostępny (np. w MockBukkit)
     * Mapuje kilka podstawowych przepisów RAW -> COOKED
     * 
     * VANILLA BEHAVIOR:
     * - Smoker: TYLKO jedzenie (mięso, ryby, ziemniaki, kelp)
     * - Blast Furnace: TYLKO rudy i metale
     * - Zwykły piec: wszystko
     * 
     * @param input przedmiot do przepalenia
     * @param furnaceType typ pieca (FURNACE, BLAST_FURNACE, SMOKER)
     * @return wynik przepalania lub null
     */
    private ItemStack getSimpleFallbackSmeltingResult(ItemStack input, Material furnaceType) {
        Material inputMat = input.getType();
        Material result = null;
        
        if (furnaceType == Material.SMOKER) {
            // Smoker: TYLKO jedzenie - bez fallbacku do innych przepisów!
            result = switch (inputMat) {
                case BEEF -> Material.COOKED_BEEF;
                case PORKCHOP -> Material.COOKED_PORKCHOP;
                case CHICKEN -> Material.COOKED_CHICKEN;
                case MUTTON -> Material.COOKED_MUTTON;
                case RABBIT -> Material.COOKED_RABBIT;
                case COD -> Material.COOKED_COD;
                case SALMON -> Material.COOKED_SALMON;
                case POTATO -> Material.BAKED_POTATO;
                case KELP -> Material.DRIED_KELP;
                default -> null;
            };
        } else if (furnaceType == Material.BLAST_FURNACE) {
            // Blast Furnace: TYLKO rudy i metale - bez fallbacku do innych przepisów!
            result = switch (inputMat) {
                case RAW_IRON -> Material.IRON_INGOT;
                case RAW_GOLD -> Material.GOLD_INGOT;
                case RAW_COPPER -> Material.COPPER_INGOT;
                case IRON_ORE, DEEPSLATE_IRON_ORE -> Material.IRON_INGOT;
                case GOLD_ORE, DEEPSLATE_GOLD_ORE -> Material.GOLD_INGOT;
                case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.COPPER_INGOT;
                case ANCIENT_DEBRIS -> Material.NETHERITE_SCRAP;
                default -> null;
            };
        } else {
            // Zwykły piec: wszystkie przepisy (uniwersalny)
            result = switch (inputMat) {
                // Mięso i ryby
                case BEEF -> Material.COOKED_BEEF;
                case PORKCHOP -> Material.COOKED_PORKCHOP;
                case CHICKEN -> Material.COOKED_CHICKEN;
                case MUTTON -> Material.COOKED_MUTTON;
                case RABBIT -> Material.COOKED_RABBIT;
                case COD -> Material.COOKED_COD;
                case SALMON -> Material.COOKED_SALMON;
                case POTATO -> Material.BAKED_POTATO;
                case KELP -> Material.DRIED_KELP;
                
                // Rudy
                case RAW_IRON -> Material.IRON_INGOT;
                case RAW_GOLD -> Material.GOLD_INGOT;
                case RAW_COPPER -> Material.COPPER_INGOT;
                case IRON_ORE, DEEPSLATE_IRON_ORE -> Material.IRON_INGOT;
                case GOLD_ORE, DEEPSLATE_GOLD_ORE -> Material.GOLD_INGOT;
                case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.COPPER_INGOT;
                case ANCIENT_DEBRIS -> Material.NETHERITE_SCRAP;
                
                // Pozostałe przepisy tylko dla zwykłego pieca
                case SAND, RED_SAND -> Material.GLASS;
                case COBBLESTONE -> Material.STONE;
                case CLAY_BALL -> Material.BRICK;
                case NETHERRACK -> Material.NETHER_BRICK;
                case CACTUS -> Material.GREEN_DYE;
                case WET_SPONGE -> Material.SPONGE;
                
                default -> null;
            };
        }
        
        return result != null ? new ItemStack(result, 1) : null;
    }
    
    /**
     * Sprawdza czy przedmiot może być przepalony w piecu (ma przepis)
     * Używa NMS jeśli dostępny, fallback dla podstawowych materiałów.
     * 
     * VANILLA BEHAVIOR: shift-click w piecu kieruje przedmioty z przepisem do INPUT,
     * a paliwa bez przepisu do FUEL.
     * 
     * @param material materiał do sprawdzenia
     * @param world świat (do sprawdzenia przepisów przez NMS)
     * @param furnaceType typ pieca (Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER)
     * @return true jeśli przedmiot ma przepis na przepalenie
     */
    public static boolean canBeSmeltedInFurnace(Material material, org.bukkit.World world, Material furnaceType) {
        if (material == null || material.isAir()) return false;
        
        // Spróbuj użyć NMS dla dokładnego sprawdzenia przepisu
        try {
            NmsFurnaceEngineDirect engine = new NmsFurnaceEngineDirect();
            ItemStack testItem = new ItemStack(material, 1);
            ItemStack result = engine.getSmeltingResultForFurnaceType(testItem, world, furnaceType);
            if (result != null) {
                return true;
            }
        } catch (Exception | NoClassDefFoundError e) {
            // NMS niedostępny - użyj fallback
        }
        
        // Fallback - podstawowe materiały które można przepalić (uwzględnia typ pieca)
        return canBeSmelted_Fallback(material, furnaceType);
    }
    
    /**
     * Fallback sprawdzania czy przedmiot można przepalić (gdy NMS niedostępny)
     * 
     * VANILLA BEHAVIOR:
     * - Smoker: TYLKO jedzenie
     * - Blast Furnace: TYLKO rudy i metale
     * - Zwykły piec: wszystko
     * 
     * @param material materiał do sprawdzenia
     * @param furnaceType typ pieca
     * @return true jeśli można przepalić w tym typie pieca
     */
    private static boolean canBeSmelted_Fallback(Material material, Material furnaceType) {
        if (furnaceType == Material.SMOKER) {
            // Smoker: TYLKO jedzenie
            return switch (material) {
                case BEEF, PORKCHOP, CHICKEN, MUTTON, RABBIT, COD, SALMON,
                     POTATO, KELP -> true;
                default -> false;
            };
        } else if (furnaceType == Material.BLAST_FURNACE) {
            // Blast Furnace: TYLKO rudy i metale
            return switch (material) {
                case RAW_IRON, RAW_GOLD, RAW_COPPER -> true;
                case IRON_ORE, DEEPSLATE_IRON_ORE -> true;
                case GOLD_ORE, DEEPSLATE_GOLD_ORE -> true;
                case COPPER_ORE, DEEPSLATE_COPPER_ORE -> true;
                case ANCIENT_DEBRIS -> true;
                default -> false;
            };
        } else {
            // Zwykły piec: wszystko
            return switch (material) {
                // Mięso i ryby
                case BEEF, PORKCHOP, CHICKEN, MUTTON, RABBIT, COD, SALMON -> true;
                case POTATO, KELP -> true;
                // Rudy
                case RAW_IRON, RAW_GOLD, RAW_COPPER -> true;
                case IRON_ORE, DEEPSLATE_IRON_ORE -> true;
                case GOLD_ORE, DEEPSLATE_GOLD_ORE -> true;
                case COPPER_ORE, DEEPSLATE_COPPER_ORE -> true;
                case ANCIENT_DEBRIS -> true;
                // Inne materiały
                case SAND, RED_SAND -> true;
                case COBBLESTONE -> true;
                case CLAY_BALL, CLAY -> true;
                case NETHERRACK -> true;
                case CACTUS -> true;
                case WET_SPONGE -> true;
                // Drewno (można przepalić na węgiel drzewny)
                case OAK_LOG, BIRCH_LOG, SPRUCE_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG,
                     MANGROVE_LOG, CHERRY_LOG, CRIMSON_STEM, WARPED_STEM,
                     OAK_WOOD, BIRCH_WOOD, SPRUCE_WOOD, JUNGLE_WOOD, ACACIA_WOOD, DARK_OAK_WOOD,
                     MANGROVE_WOOD, CHERRY_WOOD, CRIMSON_HYPHAE, WARPED_HYPHAE,
                     STRIPPED_OAK_LOG, STRIPPED_BIRCH_LOG, STRIPPED_SPRUCE_LOG, STRIPPED_JUNGLE_LOG,
                     STRIPPED_ACACIA_LOG, STRIPPED_DARK_OAK_LOG, STRIPPED_MANGROVE_LOG, STRIPPED_CHERRY_LOG,
                     STRIPPED_CRIMSON_STEM, STRIPPED_WARPED_STEM,
                     STRIPPED_OAK_WOOD, STRIPPED_BIRCH_WOOD, STRIPPED_SPRUCE_WOOD, STRIPPED_JUNGLE_WOOD,
                     STRIPPED_ACACIA_WOOD, STRIPPED_DARK_OAK_WOOD, STRIPPED_MANGROVE_WOOD, STRIPPED_CHERRY_WOOD,
                     STRIPPED_CRIMSON_HYPHAE, STRIPPED_WARPED_HYPHAE -> true;
                default -> false;
            };
        }
    }
    
    /**
     * Publiczna wersja canBeSmelted_Fallback do użytku w testach jednostkowych.
     * 
     * @param material materiał do sprawdzenia
     * @param furnaceType typ pieca
     * @return true jeśli można przepalić w tym typie pieca (wg logiki fallback)
     */
    public static boolean canBeSmelted_FallbackForTest(Material material, Material furnaceType) {
        return canBeSmelted_Fallback(material, furnaceType);
    }
    
    /**
     * Pobiera czas palenia paliwa w tickach
     * Próbuje użyć NMS (FuelValues) dla dokładnych wartości vanilla.
     * Jeśli NMS niedostępny (np. MockBukkit) - używa fallback tablicy.
     * 
     * @param material materiał do sprawdzenia
     * @return czas palenia w tickach lub 0 jeśli nie jest paliwem
     */
    public static int getFuelBurnTime(Material material) {
        if (material == null || material.isAir()) return 0;
        
        // Spróbuj użyć NMS dla dokładnych wartości
        int nmsResult = getNmsFuelBurnTime(material);
        if (nmsResult >= 0) {
            return nmsResult;
        }
        
        // Fallback - gdy NMS niedostępny (np. testy bez prawdziwego serwera)
        return getFallbackFuelBurnTime(material);
    }
    
    /**
     * Próbuje pobrać czas palenia z NMS (FuelValues)
     * 
     * @return czas palenia lub -1 jeśli NMS niedostępny
     */
    private static int getNmsFuelBurnTime(Material material) {
        try {
            // Pobierz pierwszy dostępny świat z serwera
            var worlds = Bukkit.getWorlds();
            if (worlds == null || worlds.isEmpty()) {
                return -1; // Brak światów - fallback
            }
            
            World world = worlds.get(0);
            if (!(world instanceof org.bukkit.craftbukkit.CraftWorld craftWorld)) {
                return -1; // Nie CraftWorld - fallback
            }
            
            // Użyj NMS FuelValues
            var level = craftWorld.getHandle();
            var nmsItem = org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(
                new ItemStack(material));
            var fuelValues = level.fuelValues();
            
            return fuelValues.burnDuration(nmsItem);
            
        } catch (Exception | NoClassDefFoundError e) {
            // NMS niedostępny - użyj fallback
            return -1;
        }
    }
    
    /**
     * Fallback tablica paliw gdy NMS niedostępny
     * Wartości zgodne z vanilla Minecraft 1.21
     * Package-private dla testów
     */
    static int getFallbackFuelBurnTime(Material material) {
        return switch (material) {
            // Węgiel
            case COAL, CHARCOAL -> 1600;
            case COAL_BLOCK -> 16000;
            
            // Specjalne paliwa
            case LAVA_BUCKET -> 20000;
            case BLAZE_ROD -> 2400;
            case DRIED_KELP_BLOCK -> 4001;
            case STICK -> 100;
            case BAMBOO, SCAFFOLDING -> 50;
            
            // Deski - 300 ticków
            case OAK_PLANKS, SPRUCE_PLANKS, BIRCH_PLANKS, JUNGLE_PLANKS, ACACIA_PLANKS, 
                 DARK_OAK_PLANKS, MANGROVE_PLANKS, CHERRY_PLANKS, BAMBOO_PLANKS,
                 CRIMSON_PLANKS, WARPED_PLANKS, PALE_OAK_PLANKS -> 300;
            
            // Logi - 300 ticków
            case OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG,
                 MANGROVE_LOG, CHERRY_LOG, PALE_OAK_LOG,
                 OAK_WOOD, SPRUCE_WOOD, BIRCH_WOOD, JUNGLE_WOOD,
                 ACACIA_WOOD, DARK_OAK_WOOD, MANGROVE_WOOD, CHERRY_WOOD, PALE_OAK_WOOD -> 300;
            
            // Stripped logi - 300 ticków
            case STRIPPED_OAK_LOG, STRIPPED_SPRUCE_LOG, STRIPPED_BIRCH_LOG, 
                 STRIPPED_JUNGLE_LOG, STRIPPED_ACACIA_LOG, STRIPPED_DARK_OAK_LOG,
                 STRIPPED_MANGROVE_LOG, STRIPPED_CHERRY_LOG, STRIPPED_PALE_OAK_LOG,
                 STRIPPED_OAK_WOOD, STRIPPED_SPRUCE_WOOD, STRIPPED_BIRCH_WOOD,
                 STRIPPED_JUNGLE_WOOD, STRIPPED_ACACIA_WOOD, STRIPPED_DARK_OAK_WOOD,
                 STRIPPED_MANGROVE_WOOD, STRIPPED_CHERRY_WOOD, STRIPPED_PALE_OAK_WOOD -> 300;
            
            // Drewniane narzędzia i przedmioty - 200 ticków
            case WOODEN_SWORD, WOODEN_PICKAXE, WOODEN_AXE, WOODEN_SHOVEL, WOODEN_HOE -> 200;
            case OAK_SAPLING, SPRUCE_SAPLING, BIRCH_SAPLING, JUNGLE_SAPLING, 
                 ACACIA_SAPLING, DARK_OAK_SAPLING, CHERRY_SAPLING, PALE_OAK_SAPLING -> 100;
            case BOWL, OAK_SIGN, SPRUCE_SIGN, BIRCH_SIGN, JUNGLE_SIGN, ACACIA_SIGN,
                 DARK_OAK_SIGN, MANGROVE_SIGN, CHERRY_SIGN, BAMBOO_SIGN, PALE_OAK_SIGN -> 200;
            case OAK_HANGING_SIGN, SPRUCE_HANGING_SIGN, BIRCH_HANGING_SIGN, JUNGLE_HANGING_SIGN,
                 ACACIA_HANGING_SIGN, DARK_OAK_HANGING_SIGN, MANGROVE_HANGING_SIGN, 
                 CHERRY_HANGING_SIGN, BAMBOO_HANGING_SIGN, PALE_OAK_HANGING_SIGN -> 200;
                 
            // Drewniane drzwi - 200 ticków  
            case OAK_DOOR, SPRUCE_DOOR, BIRCH_DOOR, JUNGLE_DOOR, ACACIA_DOOR,
                 DARK_OAK_DOOR, MANGROVE_DOOR, CHERRY_DOOR, BAMBOO_DOOR, PALE_OAK_DOOR -> 200;
            case OAK_TRAPDOOR, SPRUCE_TRAPDOOR, BIRCH_TRAPDOOR, JUNGLE_TRAPDOOR,
                 ACACIA_TRAPDOOR, DARK_OAK_TRAPDOOR, MANGROVE_TRAPDOOR, CHERRY_TRAPDOOR,
                 BAMBOO_TRAPDOOR, PALE_OAK_TRAPDOOR -> 300;
            
            // Płotki i bramy - 300 ticków
            case OAK_FENCE, SPRUCE_FENCE, BIRCH_FENCE, JUNGLE_FENCE, ACACIA_FENCE,
                 DARK_OAK_FENCE, MANGROVE_FENCE, CHERRY_FENCE, BAMBOO_FENCE, PALE_OAK_FENCE -> 300;
            case OAK_FENCE_GATE, SPRUCE_FENCE_GATE, BIRCH_FENCE_GATE, JUNGLE_FENCE_GATE,
                 ACACIA_FENCE_GATE, DARK_OAK_FENCE_GATE, MANGROVE_FENCE_GATE, 
                 CHERRY_FENCE_GATE, BAMBOO_FENCE_GATE, PALE_OAK_FENCE_GATE -> 300;
            
            // Schody i płyty - 300 (schody), 150 (płyty)
            case OAK_STAIRS, SPRUCE_STAIRS, BIRCH_STAIRS, JUNGLE_STAIRS, ACACIA_STAIRS,
                 DARK_OAK_STAIRS, MANGROVE_STAIRS, CHERRY_STAIRS, BAMBOO_STAIRS, 
                 BAMBOO_MOSAIC_STAIRS, PALE_OAK_STAIRS -> 300;
            case OAK_SLAB, SPRUCE_SLAB, BIRCH_SLAB, JUNGLE_SLAB, ACACIA_SLAB,
                 DARK_OAK_SLAB, MANGROVE_SLAB, CHERRY_SLAB, BAMBOO_SLAB, 
                 BAMBOO_MOSAIC_SLAB, PALE_OAK_SLAB -> 150;
            
            // Skrzynie i inne pojemniki - 300 ticków
            case CHEST, TRAPPED_CHEST, BARREL -> 300;
            case CRAFTING_TABLE -> 300;
            case JUKEBOX, BOOKSHELF, CHISELED_BOOKSHELF -> 300;
            case LECTERN, COMPOSTER -> 300;
            
            // Łuki - 300 ticków
            case BOW, CROSSBOW -> 300;
            case FISHING_ROD -> 300;
            
            // Łodzie - 1200 ticków (60 sekund)
            case OAK_BOAT, SPRUCE_BOAT, BIRCH_BOAT, JUNGLE_BOAT, ACACIA_BOAT,
                 DARK_OAK_BOAT, MANGROVE_BOAT, CHERRY_BOAT, BAMBOO_RAFT, PALE_OAK_BOAT -> 1200;
            case OAK_CHEST_BOAT, SPRUCE_CHEST_BOAT, BIRCH_CHEST_BOAT, JUNGLE_CHEST_BOAT,
                 ACACIA_CHEST_BOAT, DARK_OAK_CHEST_BOAT, MANGROVE_CHEST_BOAT,
                 CHERRY_CHEST_BOAT, BAMBOO_CHEST_RAFT, PALE_OAK_CHEST_BOAT -> 1200;
            
            // Wełna - 100 ticków
            case WHITE_WOOL, ORANGE_WOOL, MAGENTA_WOOL, LIGHT_BLUE_WOOL, YELLOW_WOOL,
                 LIME_WOOL, PINK_WOOL, GRAY_WOOL, LIGHT_GRAY_WOOL, CYAN_WOOL,
                 PURPLE_WOOL, BLUE_WOOL, BROWN_WOOL, GREEN_WOOL, RED_WOOL, BLACK_WOOL -> 100;
            
            // Dywany - 67 ticków
            case WHITE_CARPET, ORANGE_CARPET, MAGENTA_CARPET, LIGHT_BLUE_CARPET, 
                 YELLOW_CARPET, LIME_CARPET, PINK_CARPET, GRAY_CARPET, 
                 LIGHT_GRAY_CARPET, CYAN_CARPET, PURPLE_CARPET, BLUE_CARPET,
                 BROWN_CARPET, GREEN_CARPET, RED_CARPET, BLACK_CARPET -> 67;
            
            // Banery - 300 ticków
            case WHITE_BANNER, ORANGE_BANNER, MAGENTA_BANNER, LIGHT_BLUE_BANNER,
                 YELLOW_BANNER, LIME_BANNER, PINK_BANNER, GRAY_BANNER,
                 LIGHT_GRAY_BANNER, CYAN_BANNER, PURPLE_BANNER, BLUE_BANNER,
                 BROWN_BANNER, GREEN_BANNER, RED_BANNER, BLACK_BANNER -> 300;
            
            // Azalea i inne rośliny
            case AZALEA, FLOWERING_AZALEA -> 100;
            case DEAD_BUSH -> 100;
            
            // Bamboo blocks
            case BAMBOO_BLOCK, STRIPPED_BAMBOO_BLOCK -> 300;
            
            default -> 0;
        };
    }
}

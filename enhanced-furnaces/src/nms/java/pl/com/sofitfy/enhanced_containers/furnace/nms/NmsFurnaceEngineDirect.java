package pl.com.sofitfy.enhanced_containers.furnace.nms;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.block.entity.FuelValues;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NMS Furnace Engine dla Paper 1.21.x (Mojang mappings)
 * 
 * Bezpośredni dostęp do vanilla logiki pieca:
 * - Przepisy smeltingu (FurnaceRecipe, BlastingRecipe, SmokingRecipe)
 * - Czasy palenia paliwa (FuelValues)
 * - Czasy przepalania z przepisów
 * 
 * OPTYMALIZACJA: Cache przepisów - unikamy RecipeManager.getRecipeFor() co tick
 * 
 * Ta klasa wymaga paperweight-userdev. Dlatego jest w src/nms/java.
 */
public class NmsFurnaceEngineDirect {
    
    // Cache przepisów: "MATERIAL:FURNACE_TYPE" -> Optional<wynik>
    // Optional.empty() = brak przepisu (ConcurrentHashMap nie akceptuje null wartości!)
    // Używamy ConcurrentHashMap dla thread safety
    private static final Map<String, Optional<ItemStack>> recipeCache = new ConcurrentHashMap<>();
    
    // Cache paliw: Material -> czas palenia
    private static final Map<Material, Integer> fuelCache = new ConcurrentHashMap<>();
    
    /**
     * Pobiera wynik przepalania dla przedmiotu w zwykłym piecu (FURNACE)
     * 
     * @param input przedmiot do przepalenia
     * @param bukkitWorld świat (do pobierania RecipeManager)
     * @return wynik przepalania lub null jeśli brak przepisu
     */
    public ItemStack getSmeltingResult(ItemStack input, World bukkitWorld) {
        return getCookingResult(input, bukkitWorld, RecipeType.SMELTING);
    }
    
    /**
     * Pobiera wynik przepalania dla blast furnace
     */
    public ItemStack getBlastingResult(ItemStack input, World bukkitWorld) {
        return getCookingResult(input, bukkitWorld, RecipeType.BLASTING);
    }
    
    /**
     * Pobiera wynik przepalania dla smokera
     */
    public ItemStack getSmokingResult(ItemStack input, World bukkitWorld) {
        return getCookingResult(input, bukkitWorld, RecipeType.SMOKING);
    }
    
    /**
     * Pobiera wynik przepalania dla dowolnego typu pieca - WERSJA Z CACHE
     * Automatycznie wybiera przepis na podstawie typu pieca
     * 
     * OPTYMALIZACJA: Cache przepisów - unikamy RecipeManager.getRecipeFor() co tick
     * Cache jest thread-safe i automatycznie wypełniany przy pierwszym użyciu
     * 
     * @param input przedmiot do przepalenia
     * @param bukkitWorld świat
     * @param furnaceType typ pieca (FURNACE, BLAST_FURNACE, SMOKER)
     * @return wynik przepalania lub null
     */
    public ItemStack getSmeltingResultForFurnaceType(ItemStack input, World bukkitWorld, Material furnaceType) {
        if (input == null || input.getType().isAir()) return null;
        
        // Klucz cache: "MATERIAL:FURNACE_TYPE"
        String cacheKey = input.getType().name() + ":" + furnaceType.name();
        
        // Sprawdź cache
        Optional<ItemStack> cached = recipeCache.get(cacheKey);
        if (cached != null) {
            // Cache hit - zwróć kopię lub null (Optional.empty() = brak przepisu)
            return cached.map(ItemStack::clone).orElse(null);
        }
        
        // Brak w cache - szukaj przepisu (wolne, ale tylko raz na materiał)
        ItemStack result = findSmeltingResultUncached(input, bukkitWorld, furnaceType);
        
        // Zapisz do cache: Optional.of(result) lub Optional.empty() dla braku przepisu
        // ConcurrentHashMap nie akceptuje null wartości, więc używamy Optional
        recipeCache.put(cacheKey, result != null ? Optional.of(result.clone()) : Optional.empty());
        
        return result;
    }
    
    /**
     * Szuka przepisu BEZ cache - używane tylko do wypełnienia cache
     * 
     * VANILLA BEHAVIOR:
     * - Blast Furnace: TYLKO przepisy BlastingRecipe (rudy, metale) - BEZ fallbacku!
     * - Smoker: TYLKO przepisy SmokingRecipe (jedzenie) - BEZ fallbacku!
     * - Zwykły piec: SmeltingRecipe + może użyć blasting/smoking (uniwersalny)
     */
    private ItemStack findSmeltingResultUncached(ItemStack input, World bukkitWorld, Material furnaceType) {
        if (furnaceType == Material.BLAST_FURNACE) {
            // Blast Furnace: TYLKO blasting recipes (rudy, metale)
            // Vanilla nie pozwala na przepalanie jedzenia czy drewna w blast furnace!
            return getBlastingResult(input, bukkitWorld);
        } else if (furnaceType == Material.SMOKER) {
            // Smoker: TYLKO smoking recipes (jedzenie)
            // Vanilla nie pozwala na przepalanie rud czy cobblestone w smokerze!
            return getSmokingResult(input, bukkitWorld);
        } else {
            // Zwykły piec: uniwersalny - sprawdza wszystkie typy przepisów
            ItemStack result = getSmeltingResult(input, bukkitWorld);
            if (result != null) return result;
            result = getBlastingResult(input, bukkitWorld);
            if (result != null) return result;
            return getSmokingResult(input, bukkitWorld);
        }
    }
    
    /**
     * Pobiera czas przepalania (w tickach) dla danego przedmiotu
     * 
     * @param input przedmiot do przepalenia
     * @param bukkitWorld świat
     * @param furnaceType typ pieca
     * @return czas przepalania w tickach lub 0 jeśli brak przepisu
     */
    public int getCookingTime(ItemStack input, World bukkitWorld, Material furnaceType) {
        if (input == null || input.getType().isAir()) return 0;
        if (!(bukkitWorld instanceof CraftWorld craftWorld)) return 0;
        
        ServerLevel level = craftWorld.getHandle();
        net.minecraft.world.item.ItemStack nmsInput = CraftItemStack.asNMSCopy(input);
        SingleRecipeInput recipeInput = new SingleRecipeInput(nmsInput);
        
        // Wybierz typ przepisu na podstawie typu pieca
        RecipeType<? extends AbstractCookingRecipe> recipeType = getRecipeTypeForFurnace(furnaceType);
        
        Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> recipe = 
            level.getServer().getRecipeManager().getRecipeFor(recipeType, recipeInput, level);
        
        if (recipe.isPresent()) {
            return recipe.get().value().cookingTime();
        }
        
        // Jeśli nie ma przepisu dla tego typu, sprawdź smelting
        if (furnaceType != Material.FURNACE) {
            Optional<RecipeHolder<SmeltingRecipe>> smeltingRecipe = 
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, level);
            if (smeltingRecipe.isPresent()) {
                int baseTime = smeltingRecipe.get().value().cookingTime();
                // Blast furnace i smoker mają 2x szybsze przepalanie
                return (furnaceType == Material.BLAST_FURNACE || furnaceType == Material.SMOKER) 
                    ? baseTime / 2 : baseTime;
            }
        }
        
        return 0;
    }
    
    /**
     * Pobiera czas palenia paliwa (w tickach) dla danego materiału - WERSJA Z CACHE
     * Używa vanilla FuelValues
     * 
     * @param fuelMaterial materiał paliwa
     * @param bukkitWorld świat (do pobierania FuelValues)
     * @return czas palenia w tickach lub 0 jeśli nie jest paliwem
     */
    public int getFuelBurnTime(Material fuelMaterial, World bukkitWorld) {
        if (fuelMaterial == null || fuelMaterial.isAir()) return 0;
        
        // Sprawdź cache
        Integer cached = fuelCache.get(fuelMaterial);
        if (cached != null) {
            return cached;
        }
        
        if (!(bukkitWorld instanceof CraftWorld craftWorld)) return 0;
        
        ServerLevel level = craftWorld.getHandle();
        
        // Stwórz ItemStack z materiału
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(new ItemStack(fuelMaterial));
        
        // Pobierz FuelValues z serwera
        FuelValues fuelValues = level.fuelValues();
        
        // Pobierz czas palenia
        int burnTime = fuelValues.burnDuration(nmsItem);
        
        // Zapisz do cache
        fuelCache.put(fuelMaterial, burnTime);
        
        return burnTime;
    }
    
    /**
     * Pobiera czas palenia paliwa dla konkretnego ItemStack - WERSJA Z CACHE
     * Dla zwykłych przedmiotów używa cache po Material
     * Przedmioty z NBT (enchanted) nadal używają pełnego lookup
     */
    public int getFuelBurnTime(ItemStack fuel, World bukkitWorld) {
        if (fuel == null || fuel.getType().isAir()) return 0;
        
        // Dla zwykłych przedmiotów (bez NBT) używaj cache
        if (!fuel.hasItemMeta()) {
            return getFuelBurnTime(fuel.getType(), bukkitWorld);
        }
        
        // Dla przedmiotów z NBT - pełny lookup (rzadkie)
        if (!(bukkitWorld instanceof CraftWorld craftWorld)) return 0;
        
        ServerLevel level = craftWorld.getHandle();
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(fuel);
        
        FuelValues fuelValues = level.fuelValues();
        return fuelValues.burnDuration(nmsItem);
    }
    
    /**
     * Sprawdza czy materiał jest paliwem
     */
    public boolean isFuel(Material material, World bukkitWorld) {
        return getFuelBurnTime(material, bukkitWorld) > 0;
    }
    
    /**
     * Sprawdza czy przedmiot można przepalić w danym typie pieca
     */
    public boolean canSmelt(ItemStack input, World bukkitWorld, Material furnaceType) {
        return getSmeltingResultForFurnaceType(input, bukkitWorld, furnaceType) != null;
    }
    
    // ==================== PRIVATE METHODS ====================
    
    @SuppressWarnings("unchecked")
    private <T extends AbstractCookingRecipe> ItemStack getCookingResult(
            ItemStack input, World bukkitWorld, RecipeType<T> recipeType) {
        
        if (input == null || input.getType().isAir()) return null;
        if (!(bukkitWorld instanceof CraftWorld craftWorld)) return null;
        
        ServerLevel level = craftWorld.getHandle();
        RegistryAccess registryAccess = level.registryAccess();
        
        // Konwertuj Bukkit ItemStack na NMS
        net.minecraft.world.item.ItemStack nmsInput = CraftItemStack.asNMSCopy(input);
        
        // Stwórz SingleRecipeInput dla przepisów pieca
        SingleRecipeInput recipeInput = new SingleRecipeInput(nmsInput);
        
        // Szukaj przepisu
        Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> recipe = 
            level.getServer().getRecipeManager().getRecipeFor(recipeType, recipeInput, level);
        
        if (recipe.isEmpty()) return null;
        
        // Pobierz wynik z przepisu - używamy assemble() dla dynamicznych wyników
        net.minecraft.world.item.ItemStack nmsResult = recipe.get().value().assemble(recipeInput, registryAccess);
        
        if (nmsResult.isEmpty()) return null;
        
        // Konwertuj z powrotem na Bukkit
        ItemStack bukkitResult = CraftItemStack.asBukkitCopy(nmsResult);
        if (bukkitResult == null || bukkitResult.getType().isAir()) return null;
        
        return bukkitResult;
    }
    
    private RecipeType<? extends AbstractCookingRecipe> getRecipeTypeForFurnace(Material furnaceType) {
        return switch (furnaceType) {
            case BLAST_FURNACE -> RecipeType.BLASTING;
            case SMOKER -> RecipeType.SMOKING;
            default -> RecipeType.SMELTING;
        };
    }
    
    // ==================== CACHE MANAGEMENT ====================
    
    /**
     * Czyści cache przepisów i paliw
     * Wywołaj po reloadzie przepisów lub przy /reload
     */
    public static void clearCache() {
        recipeCache.clear();
        fuelCache.clear();
    }
    
    /**
     * Zwraca rozmiar cache (do debugowania)
     */
    public static int getCacheSize() {
        return recipeCache.size() + fuelCache.size();
    }
}

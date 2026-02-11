package pl.com.sofitfy.enhanced_containers.furnace;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * EnhancedFurnaceData - przechowuje dane jednego rozszerzonego pieca
 * 
 * Zawiera:
 * - GUI pieca (inventory)
 * - Stan przepalania (burn time, cook time)
 * - Typ pieca (FURNACE, BLAST_FURNACE, SMOKER)
 */
public class EnhancedFurnaceData {
    
    // GUI pieca
    private final EnhancedFurnaceGUI gui;
    
    // Typ pieca
    private final Material furnaceType;
    
    // Pozostały czas palenia paliwa (w tickach)
    // Wspólny dla wszystkich 3 linii
    private int burnTime = 0;
    
    // Maksymalny czas palenia aktualnego paliwa
    private int maxBurnTime = 0;
    
    // Czas przepalania dla każdej linii (0-2)
    // Vanilla: 200 ticków = 10 sekund dla zwykłego pieca
    // BLAST_FURNACE i SMOKER: 100 ticków = 5 sekund
    private final int[] cookTime = new int[3];
    
    // Poprzedni typ materiału w każdym slocie input (do detekcji zmian)
    // Gdy item się zmieni, resetujemy cookTime dla tego slotu
    private final Material[] lastInputTypes = new Material[3];
    
    // Maksymalny czas przepalania (zależny od typu pieca)
    private int maxCookTime;
    
    // Licznik ticków do optymalizacji GUI updates (0-255, overflow ok)
    private byte tickCounter = 0;
    
    // Vanilla cook times
    public static final int FURNACE_COOK_TIME = 200;       // 10 sekund
    public static final int BLAST_FURNACE_COOK_TIME = 100; // 5 sekund
    public static final int SMOKER_COOK_TIME = 100;        // 5 sekund
    
    public EnhancedFurnaceData(EnhancedFurnaceGUI gui, Material furnaceType) {
        this.gui = gui;
        this.furnaceType = furnaceType;
        
        // Ustaw czas przepalania w zależności od typu pieca
        this.maxCookTime = switch (furnaceType) {
            case BLAST_FURNACE -> BLAST_FURNACE_COOK_TIME;
            case SMOKER -> SMOKER_COOK_TIME;
            default -> FURNACE_COOK_TIME;
        };
    }
    
    public EnhancedFurnaceGUI getGui() {
        return gui;
    }
    
    public Material getFurnaceType() {
        return furnaceType;
    }
    
    public int getBurnTime() {
        return burnTime;
    }
    
    public void setBurnTime(int burnTime) {
        this.burnTime = Math.max(0, burnTime);
    }
    
    public int getMaxBurnTime() {
        return maxBurnTime;
    }
    
    public void setMaxBurnTime(int maxBurnTime) {
        this.maxBurnTime = Math.max(0, maxBurnTime);
    }
    
    public int getCookTime(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= 3) return 0;
        return cookTime[lineIndex];
    }
    
    public void setCookTime(int lineIndex, int time) {
        if (lineIndex < 0 || lineIndex >= 3) return;
        cookTime[lineIndex] = Math.max(0, time);
    }
    
    public void incrementCookTime(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= 3) return;
        cookTime[lineIndex]++;
    }
    
    public void resetCookTime(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= 3) return;
        cookTime[lineIndex] = 0;
    }
    
    /**
     * Sprawdza czy item w danym slocie się zmienił i resetuje cookTime jeśli tak.
     * Wywoływane co tick przed przetwarzaniem linii.
     * 
     * @param lineIndex indeks linii (0-2)
     * @param currentInput aktualny item w slocie input
     * @return true jeśli item się zmienił (cookTime zostało zresetowane)
     */
    public boolean checkAndResetIfInputChanged(int lineIndex, ItemStack currentInput) {
        if (lineIndex < 0 || lineIndex >= 3) return false;
        
        Material currentType = (currentInput != null && !currentInput.getType().isAir()) 
            ? currentInput.getType() 
            : null;
        Material lastType = lastInputTypes[lineIndex];
        
        // Sprawdź czy typ się zmienił
        boolean changed = (currentType != lastType);
        
        // Aktualizuj zapamiętany typ
        lastInputTypes[lineIndex] = currentType;
        
        // Jeśli się zmienił - resetuj cookTime
        if (changed && cookTime[lineIndex] > 0) {
            cookTime[lineIndex] = 0;
            return true;
        }
        
        return false;
    }
    
    public int getMaxCookTime() {
        return maxCookTime;
    }
    
    /**
     * Sprawdza czy piec jest aktywnie palący (ma paliwo)
     */
    public boolean isBurning() {
        return burnTime > 0;
    }
    
    /**
     * Sprawdza czy piec ma jakikolwiek input do przepalenia
     * OPTYMALIZACJA: Puste piece (bez inputu i bez paliwa) są pomijane w tickowaniu
     */
    public boolean hasAnyInput() {
        for (int i = 0; i < 3; i++) {
            ItemStack input = gui.getInputItem(i);
            if (input != null && input.getType() != Material.AIR) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Sprawdza czy piec ma jakiekolwiek paliwo
     */
    public boolean hasAnyFuel() {
        for (int i = 0; i < EnhancedFurnaceGUI.FUEL_SLOTS.length; i++) {
            ItemStack fuel = gui.getFuelItem(i);
            if (fuel != null && fuel.getType() != Material.AIR) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Sprawdza czy dana linia aktywnie przepala (ma input i postęp)
     */
    public boolean isLineActive(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= 3) return false;
        return cookTime[lineIndex] > 0;
    }
    
    /**
     * Oblicza postęp przepalania dla danej linii (0.0 - 1.0)
     */
    public float getCookProgress(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= 3) return 0;
        if (maxCookTime <= 0) return 0;
        return (float) cookTime[lineIndex] / maxCookTime;
    }
    
    /**
     * Oblicza postęp palenia paliwa (0.0 - 1.0)
     */
    public float getBurnProgress() {
        if (maxBurnTime <= 0) return 0;
        return (float) burnTime / maxBurnTime;
    }
    
    /**
     * Zlicza ile linii aktualnie przepala
     */
    public int countActiveLines() {
        int count = 0;
        for (int i = 0; i < 3; i++) {
            if (gui.getInputItem(i) != null && gui.getInputItem(i).getType() != Material.AIR) {
                // Linia ma input - może być aktywna
                count++;
            }
        }
        return count;
    }
    
    /**
     * Pobiera tick counter (do optymalizacji GUI updates)
     */
    public int getTickCounter() {
        return tickCounter & 0xFF; // unsigned byte -> int
    }
    
    /**
     * Zwiększa tick counter
     */
    public void incrementTickCounter() {
        tickCounter++;
    }
}

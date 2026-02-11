package pl.com.sofitfy.enhanced_containers.furnace;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EnhancedFurnaceGUI - rozszerzony piec z 3 slotami na input, output i paliwo
 * 
 * Layout GUI (54 sloty, chest):
 * - Sloty INPUT: 3 sloty (lewy górny obszar)
 * - Sloty OUTPUT: 3 sloty (prawy górny obszar)
 * - Sloty FUEL: 3 sloty (dolny obszar, wspólne dla wszystkich linii przepalania)
 * 
 * Piec działa jak 3 równoległe linie przepalania z wspólnym paliwem:
 * - INPUT_0 -> OUTPUT_0 (linia 1)
 * - INPUT_1 -> OUTPUT_1 (linia 2)
 * - INPUT_2 -> OUTPUT_2 (linia 3)
 * 
 * Paliwo zużywa się proporcjonalnie do ilości aktywnych linii:
 * - 3 aktywne linie = 3x szybsze zużycie paliwa
 * - 2 aktywne linie = 2x szybsze zużycie paliwa
 * - 1 aktywna linia = normalne zużycie paliwa
 * 
 * STRZAŁKI POSTĘPU (Title Overlay Method):
 * Zamiast ItemStack w slotach, strzałki są renderowane jako znaki Custom Font
 * w tytule GUI. To omija problem scissor test w MC 1.20+ który obcina itemy.
 * Sloty strzałek (12-13, 21-22, 30-31) pozostają puste.
 */
public class EnhancedFurnaceGUI implements InventoryHolder {
    
    // Rozmiar inventory - zawsze 54 sloty (6 rzędów × 9 kolumn)
    public static final int INVENTORY_SIZE = 54;
    
    // Sloty wejściowe (input) - 3 sloty w lewej kolumnie
    // Dopasowane do tekstury ex_furnace.png
    // Układ: slot 11, 20, 29 (kolumna 2, rzędy 1-3)
    public static final int[] INPUT_SLOTS = {11, 20, 29};
    
    // Sloty wyjściowe (output) - 3 sloty w prawej kolumnie
    // Dopasowane do tekstury ex_furnace.png
    // Układ: slot 14, 23, 32 (kolumna 5, rzędy 1-3)
    public static final int[] OUTPUT_SLOTS = {14, 23, 32};
    
    // Sloty na paliwo - 3 sloty w dolnym rzędzie (wspólne dla wszystkich linii)
    // Dopasowane do tekstury ex_furnace.png
    // Układ: sloty 46, 47, 48 (rząd 5, kolumny 1-3)
    public static final int[] FUEL_SLOTS = {46, 47, 48};
    
    // Slot na wskaźnik postępu palenia (ikonka ognia) - nad paliwem
    // Układ: slot 38 (rząd 4, kolumna 2)
    public static final int[] PROGRESS_FIRE_SLOTS = {38};
    
    // Sloty na wskaźnik postępu przepalania (strzałka) - dla każdej linii
    // UWAGA: Te sloty są teraz PUSTE - strzałki są renderowane w tytule GUI
    // (Title Overlay Method - omija scissor test)
    // Zostawione dla kompatybilności wstecznej i blokowania interakcji
    public static final int[] PROGRESS_SMELT_LEFT_SLOTS = {12, 21, 30};
    public static final int[] PROGRESS_SMELT_RIGHT_SLOTS = {13, 22, 31};
    
    private final Inventory inventory;
    private final String baseTitle;
    
    // Postęp strzałek dla każdej z 3 linii (0.0 - 1.0)
    // Używany do generowania dynamicznego tytułu
    private final double[] arrowProgress = {0.0, 0.0, 0.0};
    
    // ANTI-FLICKER: Cache poprzednich leveli strzałek (0-22)
    // Aktualizujemy tytuł TYLKO gdy level się zmieni (nie przy każdej zmianie progress)
    private final int[] lastArrowLevels = {-1, -1, -1};
    
    // Postęp palenia paliwa (0.0 - 1.0)
    // Używany do renderowania ognia w tytule
    private double fireProgress = 0.0;
    
    // ANTI-FLICKER: Cache poprzedniego levelu ognia (0-13)
    private int lastFireLevel = -1;
    
    // Ostatnio wygenerowany tytuł (cache do unikania zbędnych aktualizacji)
    private String lastGeneratedTitle = null;
    
    // Gracze w procesie zamykania GUI - pomijamy aktualizację tytułu dla nich
    // (setTitle() podczas zamykania może powodować ponowne otwarcie GUI!)
    private static final Set<UUID> closingPlayers = ConcurrentHashMap.newKeySet();
    
    /**
     * Tworzy nowe GUI pieca z podanym tytułem
     * 
     * @param title tytuł okna (z custom fontem)
     */
    public EnhancedFurnaceGUI(String title) {
        this.baseTitle = title;
        Component titleComponent = LegacyComponentSerializer.legacySection().deserialize(title);
        this.inventory = Bukkit.createInventory(this, INVENTORY_SIZE, titleComponent);
        setupLayout();
    }
    
    /**
     * Ustawia początkowy układ GUI
     */
    private void setupLayout() {
        // Wszystkie sloty puste na start
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, null);
        }
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Otwiera GUI dla gracza
     */
    public void open(Player player) {
        player.openInventory(inventory);
    }
    
    /**
     * Sprawdza czy slot jest slotem wejściowym
     */
    public static boolean isInputSlot(int slot) {
        for (int s : INPUT_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }
    
    /**
     * Sprawdza czy slot jest slotem wyjściowym
     */
    public static boolean isOutputSlot(int slot) {
        for (int s : OUTPUT_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }
    
    /**
     * Sprawdza czy slot jest slotem na paliwo
     */
    public static boolean isFuelSlot(int slot) {
        for (int s : FUEL_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }
    
    /**
     * Sprawdza czy slot jest slotem postępu (nie można go klikać)
     */
    public static boolean isProgressSlot(int slot) {
        for (int s : PROGRESS_FIRE_SLOTS) {
            if (s == slot) return true;
        }
        for (int s : PROGRESS_SMELT_LEFT_SLOTS) {
            if (s == slot) return true;
        }
        for (int s : PROGRESS_SMELT_RIGHT_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }
    
    /**
     * Sprawdza czy slot jest interaktywny (input, output lub fuel)
     */
    public static boolean isInteractiveSlot(int slot) {
        return isInputSlot(slot) || isOutputSlot(slot) || isFuelSlot(slot);
    }
    
    /**
     * Pobiera indeks linii dla danego slotu input (0-2)
     * Zwraca -1 jeśli slot nie jest inputem
     */
    public static int getLineIndexForInputSlot(int slot) {
        for (int i = 0; i < INPUT_SLOTS.length; i++) {
            if (INPUT_SLOTS[i] == slot) return i;
        }
        return -1;
    }
    
    /**
     * Pobiera indeks linii dla danego slotu output (0-2)
     * Zwraca -1 jeśli slot nie jest outputem
     */
    public static int getLineIndexForOutputSlot(int slot) {
        for (int i = 0; i < OUTPUT_SLOTS.length; i++) {
            if (OUTPUT_SLOTS[i] == slot) return i;
        }
        return -1;
    }
    
    /**
     * Pobiera indeks dla danego slotu fuel (0-2)
     * Zwraca -1 jeśli slot nie jest fuel
     */
    public static int getFuelIndexForSlot(int slot) {
        for (int i = 0; i < FUEL_SLOTS.length; i++) {
            if (FUEL_SLOTS[i] == slot) return i;
        }
        return -1;
    }
    
    /**
     * Pobiera przedmiot z danego slotu input
     */
    public ItemStack getInputItem(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= INPUT_SLOTS.length) return null;
        return inventory.getItem(INPUT_SLOTS[lineIndex]);
    }
    
    /**
     * Ustawia przedmiot w danym slocie input
     * ANTI-FLICKER: Używa setItemSmart() - wysyła pakiet tylko gdy item się zmienił
     */
    public void setInputItem(int lineIndex, ItemStack item) {
        if (lineIndex < 0 || lineIndex >= INPUT_SLOTS.length) return;
        setItemSmart(INPUT_SLOTS[lineIndex], item);
    }
    
    /**
     * Pobiera przedmiot z danego slotu output
     */
    public ItemStack getOutputItem(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= OUTPUT_SLOTS.length) return null;
        return inventory.getItem(OUTPUT_SLOTS[lineIndex]);
    }
    
    /**
     * Ustawia przedmiot w danym slocie output
     * ANTI-FLICKER: Używa setItemSmart() - wysyła pakiet tylko gdy item się zmienił
     */
    public void setOutputItem(int lineIndex, ItemStack item) {
        if (lineIndex < 0 || lineIndex >= OUTPUT_SLOTS.length) return;
        setItemSmart(OUTPUT_SLOTS[lineIndex], item);
    }
    
    /**
     * Pobiera przedmiot z danego slotu fuel
     */
    public ItemStack getFuelItem(int fuelIndex) {
        if (fuelIndex < 0 || fuelIndex >= FUEL_SLOTS.length) return null;
        return inventory.getItem(FUEL_SLOTS[fuelIndex]);
    }
    
    /**
     * Ustawia przedmiot w danym slocie fuel
     * ANTI-FLICKER: Używa setItemSmart() - wysyła pakiet tylko gdy item się zmienił
     */
    public void setFuelItem(int fuelIndex, ItemStack item) {
        if (fuelIndex < 0 || fuelIndex >= FUEL_SLOTS.length) return;
        setItemSmart(FUEL_SLOTS[fuelIndex], item);
    }
    
    // ==================== ANTI-FLICKER DIRTY CHECK ====================
    
    /**
     * Aktualizuje slot tylko wtedy, gdy przedmiot faktycznie się różni.
     * Eliminuje miganie (flickering) przy słabym internecie.
     * 
     * Zamiast wysyłać 20 pakietów na sekundę, wysyła 1 pakiet tylko gdy item się zmieni.
     * 
     * @param slot numer slotu
     * @param newItem nowy przedmiot (może być null)
     */
    private void setItemSmart(int slot, ItemStack newItem) {
        ItemStack currentItem = inventory.getItem(slot);
        
        // 1. Jeśli oba są null (puste) -> nic nie rób
        if (currentItem == null && newItem == null) return;
        
        // 2. Jeśli oba są AIR -> nic nie rób
        if (currentItem != null && newItem != null 
            && currentItem.getType().isAir() && newItem.getType().isAir()) return;
        
        // 3. Jeśli jeden jest null/air, a drugi nie -> musimy podmienić
        boolean currentEmpty = (currentItem == null || currentItem.getType().isAir());
        boolean newEmpty = (newItem == null || newItem.getType().isAir());
        
        if (currentEmpty != newEmpty) {
            inventory.setItem(slot, newItem);
            return;
        }
        
        // 4. Oba są nie-puste - sprawdzamy czy to ten sam item
        // isSimilar sprawdza typ i NBT/Display Name, ale ignoruje ilość
        boolean sameItem = currentItem.isSimilar(newItem);
        boolean sameAmount = currentItem.getAmount() == newItem.getAmount();
        
        // 5. Jeśli cokolwiek się różni -> wysyłamy pakiet (podmieniamy)
        if (!sameItem || !sameAmount) {
            inventory.setItem(slot, newItem);
        }
        // W przeciwnym razie: NIC NIE RÓB. Klient już ma ten item.
    }
    
    /**
     * Pobiera wszystkie przedmioty z inventory (do dropowania przy zniszczeniu)
     */
    public ItemStack[] getAllItems() {
        ItemStack[] items = new ItemStack[INPUT_SLOTS.length + OUTPUT_SLOTS.length + FUEL_SLOTS.length];
        int index = 0;
        
        for (int slot : INPUT_SLOTS) {
            items[index++] = inventory.getItem(slot);
        }
        for (int slot : OUTPUT_SLOTS) {
            items[index++] = inventory.getItem(slot);
        }
        for (int slot : FUEL_SLOTS) {
            items[index++] = inventory.getItem(slot);
        }
        
        return items;
    }
    
    /**
     * Czyści całe inventory pieca
     */
    public void clear() {
        for (int slot : INPUT_SLOTS) {
            inventory.setItem(slot, null);
        }
        for (int slot : OUTPUT_SLOTS) {
            inventory.setItem(slot, null);
        }
        for (int slot : FUEL_SLOTS) {
            inventory.setItem(slot, null);
        }
        // Wyczyść wskaźnik ognia
        for (int slot : PROGRESS_FIRE_SLOTS) {
            inventory.setItem(slot, null);
        }

        // Wyczyść sloty strzałek (powinny być puste, ale dla pewności)
        for (int slot : PROGRESS_SMELT_LEFT_SLOTS) {
            inventory.setItem(slot, null);
        }
        for (int slot : PROGRESS_SMELT_RIGHT_SLOTS) {
            inventory.setItem(slot, null);
        }
        
        // Resetuj postępy strzałek i ognia
        for (int i = 0; i < 3; i++) {
            arrowProgress[i] = 0.0;
        }
        fireProgress = 0.0;
        lastGeneratedTitle = null;
    }
    
    /**
     * Aktualizuje wskaźnik ognia (fire progress indicator)
     * Ogień jest renderowany jako znak Custom Font w tytule GUI (Title Overlay Method)
     * 
     * ANTI-FLICKER: Aktualizuje tytuł TYLKO gdy level ognia się zmieni (0-13)
     * 
     * @param burnTime pozostały czas palenia (ticki)
     * @param maxBurnTime maksymalny czas palenia aktualnego paliwa
     */
    public void updateFireIndicator(int burnTime, int maxBurnTime) {
        // Oblicz postęp jako 0.0-1.0
        if (burnTime <= 0 || maxBurnTime <= 0) {
            fireProgress = 0.0;
        } else {
            fireProgress = Math.max(0.0, Math.min(1.0, (double) burnTime / maxBurnTime));
        }
        
        // ANTI-FLICKER: Oblicz level (0-13) i sprawdź czy się zmienił
        int fireLevel = (int) Math.ceil(fireProgress * 13);
        boolean levelChanged = (fireLevel != lastFireLevel);
        lastFireLevel = fireLevel;
        
        // OPTYMALIZACJA: Sloty i tytuł aktualizuj tylko gdy ktoś patrzy I level się zmienił
        if (!inventory.getViewers().isEmpty() && levelChanged) {
            // Sloty ognia muszą być puste - ogień jest renderowany w tytule
            // ANTI-FLICKER: Używamy setItemSmart żeby nie wysyłać pakietów gdy już puste
            for (int slot : PROGRESS_FIRE_SLOTS) {
                setItemSmart(slot, null);
            }
            
            // Aktualizuj tytuł GUI dla wszystkich viewerów
            updateTitleForAllViewers();
        }
    }
    
    /**
     * Aktualizuje wskaźnik postępu przepalania dla danej linii (strzałka)
     * 
     * TITLE OVERLAY METHOD:
     * Zamiast ItemStack w slotach, strzałki są renderowane jako znaki Custom Font
     * w tytule GUI.
     * 
     * ANTI-FLICKER: Aktualizuje tytuł TYLKO gdy level strzałki się zmieni (0-22)
     * 
     * @param lineIndex indeks linii (0-2)
     * @param cookTime aktualny postęp przepalania (ticki)
     * @param maxCookTime maksymalny czas przepalania
     */
    public void updateSmeltProgress(int lineIndex, int cookTime, int maxCookTime) {
        if (lineIndex < 0 || lineIndex >= 3) return;

        // Oblicz postęp jako procent (0.0 - 1.0)
        double progress;
        if (cookTime <= 0 || maxCookTime <= 0) {
            progress = 0.0;
        } else {
            progress = Math.max(0.0, Math.min(1.0, (double) cookTime / maxCookTime));
        }
        
        // Zapisz postęp
        arrowProgress[lineIndex] = progress;
        
        // ANTI-FLICKER: Oblicz level (0-22) i sprawdź czy się zmienił
        int arrowLevel = (int) Math.ceil(progress * 22);
        boolean levelChanged = (arrowLevel != lastArrowLevels[lineIndex]);
        lastArrowLevels[lineIndex] = arrowLevel;
        
        // OPTYMALIZACJA: Sloty i tytuł aktualizuj tylko gdy ktoś patrzy I level się zmienił
        if (!inventory.getViewers().isEmpty() && levelChanged) {
            // Sloty strzałek muszą być puste (AIR) - strzałka jest w tytule
            // ANTI-FLICKER: Używamy setItemSmart żeby nie wysyłać pakietów gdy już puste
            int leftSlot = PROGRESS_SMELT_LEFT_SLOTS[lineIndex];
            int rightSlot = PROGRESS_SMELT_RIGHT_SLOTS[lineIndex];
            setItemSmart(leftSlot, null);
            setItemSmart(rightSlot, null);
            
            // Aktualizuj tytuł GUI dla wszystkich viewerów
            updateTitleForAllViewers();
        }
    }
    
    /**
     * Aktualizuje tytuł GUI dla wszystkich graczy którzy go widzą
     * Używa Paper API: player.getOpenInventory().setTitle()
     * 
     * OPTYMALIZACJA: Jeśli nikt nie patrzy - nie aktualizuj tytułu
     * WAŻNE: Pomija graczy w procesie zamykania (zapobiega ponownemu otwarciu GUI)
     */
    private void updateTitleForAllViewers() {
        // OPTYMALIZACJA: Jeśli nikt nie patrzy - nie aktualizuj tytułu
        if (inventory.getViewers().isEmpty()) {
            return;
        }
        
        // Generuj nowy tytuł ze strzałkami i ogniem
        String newTitle = FurnaceGUITitleFormatter.createTitleWithArrowsAndFire("Furnace", arrowProgress, fireProgress);
        
        // Sprawdź czy tytuł się zmienił (cache)
        if (newTitle.equals(lastGeneratedTitle)) {
            return; // Bez zmian - nie aktualizuj
        }
        lastGeneratedTitle = newTitle;
        
        // Aktualizuj tytuł dla wszystkich viewerów (POMIJAJ zamykających!)
        for (HumanEntity viewer : inventory.getViewers()) {
            if (viewer instanceof Player player) {
                // WAŻNE: Nie aktualizuj tytułu dla gracza który właśnie zamyka GUI!
                // setTitle() podczas zamykania powoduje ponowne otwarcie.
                if (closingPlayers.contains(player.getUniqueId())) {
                    continue;
                }
                
                try {
                    // Paper API - dynamiczna zmiana tytułu
                    player.getOpenInventory().setTitle(newTitle);
                } catch (Exception e) {
                    // Fallback - jeśli Paper API nie działa, zignoruj
                }
            }
        }
    }
    
    /**
     * Oznacza gracza jako zamykającego GUI.
     * Wywoływane PRZED zamknięciem aby uniknąć race condition z aktualizacją tytułu.
     */
    public static void markPlayerAsClosing(Player player) {
        closingPlayers.add(player.getUniqueId());
    }
    
    /**
     * Usuwa gracza z listy zamykających.
     * Wywoływane PO zamknięciu GUI.
     */
    public static void unmarkPlayerAsClosing(Player player) {
        closingPlayers.remove(player.getUniqueId());
    }
    
    /**
     * Pobiera aktualny postęp strzałki dla danej linii
     * 
     * @param lineIndex indeks linii (0-2)
     * @return postęp 0.0 - 1.0
     */
    public double getArrowProgress(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= 3) return 0.0;
        return arrowProgress[lineIndex];
    }
    
    /**
     * Pobiera tablicę postępów strzałek dla wszystkich linii
     * 
     * @return kopia tablicy postępów [linia0, linia1, linia2]
     */
    public double[] getAllArrowProgress() {
        return arrowProgress.clone();
    }
}

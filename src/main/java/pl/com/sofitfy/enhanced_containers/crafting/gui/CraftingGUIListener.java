package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.CraftingRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.PatternMatchResult;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeMatcher;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipePattern;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.VanillaRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.vanilla.VanillaCraftingSimulator;

import java.util.*;

// Słucha co gracz robi w okienku craftingu - kliknięcia, zamykanie itd
public class CraftingGUIListener implements Listener {
    
    /**
     * Sprawdza czy gracz ma permisję do receptury.
     * Obsługuje wildcard "*" jako "dla wszystkich" (nie wymaga hasPermission).
     * 
     * @param player gracz do sprawdzenia
     * @param permission permisja receptury (może być "*" dla public)
     * @return true jeśli gracz ma dostęp
     */
    private static boolean hasRecipePermission(Player player, String permission) {
        if (permission == null || permission.isEmpty() || "*".equals(permission)) {
            // Wildcard lub brak permisji = publiczna receptura
            return true;
        }
        return player.hasPermission(permission);
    }
    // Przechowuje które GUI są otwarte u których graczy
    private final Map<UUID, CraftingGUI> activeGUIs = new HashMap<>();
    
    // Przechowuje virtual crafting simulatory dla każdego gracza
    private final Map<UUID, VanillaCraftingSimulator> vanillaSimulators = new HashMap<>();

    private final JavaPlugin plugin;

    public CraftingGUIListener() {
        this.plugin = null;
    }

    public CraftingGUIListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private void runNextTickIfPossible(Runnable action) {
        if (action == null) {
            return;
        }

        // Czyste testy jednostkowe mogą nie mieć zainicjalizowanego Bukkit'a.
        // W takim przypadku pomijamy scheduler (testy sprawdzają tylko cancel/flow).
        if (Bukkit.getServer() == null) {
            return;
        }

        // Jeśli mamy wstrzykniętą instancję pluginu, planujemy na następny tick.
        // Jeśli nie - spróbuj użyć pierwszego załadowanego pluginu (dla testów integracyjnych).
        JavaPlugin pluginToUse = this.plugin;
        if (pluginToUse == null) {
            // Fallback: Spróbuj pobrać nasz plugin z załadowanych
            try {
                org.bukkit.plugin.Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
                for (org.bukkit.plugin.Plugin p : plugins) {
                    if (p instanceof JavaPlugin && p.isEnabled()) {
                        pluginToUse = (JavaPlugin) p;
                        break;
                    }
                }
            } catch (Exception ignored) {
                // Ignoruj błędy - wykonamy synchronicznie
            }
        }

        if (pluginToUse == null) {
            // Brak pluginu - wykonaj synchronicznie (stary fallback)
            action.run();
            return;
        }

        try {
            Bukkit.getScheduler().runTask(pluginToUse, action);
        } catch (RuntimeException e) {
            // MockBukkit/Paper mogą mieć różne ograniczenia dot. scheduler/plugin classloader.
            action.run();
        }
    }

    // Zarejestruj że gracz ma otwarte GUI
    public void registerGUI(CraftingGUI gui) {
        activeGUIs.put(gui.getPlayerId(), gui);
    }

    // Wyrejestruj GUI gracza (np gdy go zamyka)
    public void unregisterGUI(UUID playerId) {
        activeGUIs.remove(playerId);
    }

    // Słucha kiedy gracz klika w okienko
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        CraftingGUI gui = activeGUIs.get(playerId);

        // W Bukkit/Paper kliknięcie w dolny ekwipunek nadal jest tym samym "widokiem".
        // Najpewniejszy sposób na sprawdzenie czy to nasze GUI: porównać TOP inventory z widoku.
        Inventory topInventory = null;
        if (event.getView() != null) {
            topInventory = event.getView().getTopInventory();
        }

        // Czyste testy jednostkowe mogą mockować event bez InventoryView.
        // Wtedy robimy fallback na event.getInventory().
        if (topInventory == null) {
            topInventory = event.getInventory();
        }

        // Sprawdź czy to nasze GUI
        if (gui == null || topInventory == null || !topInventory.equals(gui.getInventory())) {
            return;
        }

        int slot = event.getRawSlot();
        int topSize = topInventory.getSize();

        // Jeśli slot jest poza zakresem - ignoruj (np. klik poza oknem)
        if (slot < 0) {
            return;
        }

        // Kliknięcie w górny inventory rozpoznajemy po rawSlot,
        // bo w niektórych trybach (np. creative) clickedInventory może być null.
        boolean isTopSlot = slot < topSize;

        // Jeśli gracz kliknął na dolny inventory (ekwipunek gracza)
        if (!isTopSlot) {
            // Jeśli clickedInventory jest null, to znaczy że klik był poza oknem
            // albo w nietypowym miejscu - ignorujemy.
            if (event.getClickedInventory() == null) {
                return;
            }
            InventoryAction action = event.getAction();

            // Shift+click z ekwipunku - wkładaj przedmioty do gridu
            // Naturalnie, bez duplikacji, niezależnie od gamemode
            if (event.isShiftClick() || action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                ItemStack moving = event.getCurrentItem();
                if (moving == null || moving.getType().isAir()) {
                    return;
                }

                // Blokujemy domyślny transfer - robimy własny, tylko do slotów gridu
                event.setCancelled(true);

                // Pracujemy na KOPII - oryginał zostaje niezmieniony
                ItemStack workingCopy = moving.clone();
                ItemStack remaining = moveIntoGrid(gui.getInventory(), workingCopy);

                // KLUCZOWE: Zmieniamy "current item" w EVECIE przed zakończeniem handlera
                // Bukkit przywraca snapshot tego co było w event.getCurrentItem()
                // Więc jak zmienimy current item na remaining, Bukkit przywróci remaining (nie oryginał)
                if (remaining == null || remaining.getType().isAir() || remaining.getAmount() <= 0) {
                    event.setCurrentItem(null);
                } else {
                    event.setCurrentItem(remaining);
                }

                runNextTickIfPossible(() -> updateOutput(player, gui));
                return;
            }

            // Pozostałe akcje w ekwipunku gracza - pozwalamy normalnie
            return;
        }

        // Jeśli kliknął na niedostępny slot (ukryty za customowym fontem) - blokuj!
        // Traktujemy te sloty jakby ich w ogóle nie było
        if (!gui.isVisibleSlot(slot)) {
            event.setCancelled(true);
            return;
        }

        // ============================================================================
        // OBSŁUGA OUTPUT SLOTU (slot 24) - READ-ONLY WYNIK CRAFTINGU
        // ============================================================================
        // Output slot jest CAŁKOWICIE ZABLOKOWANY dla Bukkita!
        // Wszystkie operacje obsługujemy RĘCZNIE aby zapobiec exploitom.
        // 
        // KRYTYCZNE: Bukkit ZAWSZE chce zrobić swap/pickup na click.
        // Jeśli pozwolimy na domyślne zachowanie, gracz może:
        // - Zamienić output z itemem w kursorze (exploit: darmowy craft!)
        // - Wyciągnąć output bez płacenia składnikami
        //
        // ROZWIĄZANIE: event.setCancelled(true) ZAWSZE, potem ręczna obsługa.
        // ============================================================================
        if (gui.isOutputSlot(slot)) {
            // ⚠️ ZAWSZE ANULUJ EVENT! Bukkit nie może dotknąć output slotu!
            event.setCancelled(true);
            
            // Sprawdzanie: czy jest coś do zabrania?
            ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
            if (output == null || output.getType().isAir()) {
                // Output pusty = nic się nie może stać. Już anulowaliśmy event.
                return;
            }

            // ========== OPERACJA 1: SHIFT+CLICK - CRAFTING WIELOKROTNIE ==========
            if (event.isShiftClick()) {
                // Shift+click craftuje wielokrotnie (jak vanilla)
                handleShiftClickOutput(player, gui);
                return;
            }

            // ========== OPERACJA 2: NORMALNY CLICK - ZABRANIE ITEMU ==========
            ClickType clickType = event.getClick();
            if (clickType == ClickType.LEFT || clickType == ClickType.RIGHT) {
                // Ważne: w vanilla workbench można klikać wynik wielokrotnie.
                // Jeśli na kursorze jest TEN SAM item, to dokładamy wynik do stacka.
                // Jeśli na kursorze jest inny item, to traktujemy to jak próbę swapu i blokujemy.

                ItemStack cursor = event.getCursor();
                if (cursor != null && !cursor.getType().isAir()) {
                    int takeAmount = output.getAmount();
                    if (!canAddToCursor(cursor, output, takeAmount)) {
                        return;
                    }
                }

                // RĘCZNIE dajemy graczowi output na kursor (lub dokładamy do stacka)
                // i usuwamy składniki z gridu
                handleNormalClickOutput(event, player, gui, output, clickType, cursor);
                return;
            }

            // ========== OPERACJA 3: WSZYSTKIE INNE KLIKI ==========
            // Event już anulowany - nic więcej nie robimy
            // (DOUBLE_CLICK, DROP, NUMBER_KEY, MIDDLE - wszystko zablokowane)
            return;
        }

        // Jeśli gracz kliknął na input slot (grid 4x4) - pozwól na normalne operacje
        // Po każdej zmianie w input slotach - zaktualizuj output
        else if (gui.isInputSlot(slot)) {
            // Pozwól na normalne przenoszenie item'ów
            // Nie ustawiamy event.setCancelled(true)
            
            // Zaplanuj aktualizację output slotu na następny tick
            // (po tym jak gracz skończy operację drag/drop)
            runNextTickIfPossible(() -> updateOutput(player, gui));
        }
    }

    // Słucha kiedy gracz robi kreatywne akcje (Creative Inventory)
    // W trybie creative Bukkit używa osobnego eventu, który potrafi ominąć zwykły click.
    @EventHandler
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        CraftingGUI gui = activeGUIs.get(playerId);

        Inventory topInventory = null;
        if (event.getView() != null) {
            topInventory = event.getView().getTopInventory();
        }

        if (topInventory == null) {
            topInventory = event.getInventory();
        }

        // Sprawdź czy to nasze GUI
        if (gui == null || topInventory == null || !topInventory.equals(gui.getInventory())) {
            return;
        }

        int slot = event.getRawSlot();
        int topSize = topInventory.getSize();

        // Klik poza górnym inventory nie dotyczy naszego GUI
        if (slot < 0 || slot >= topSize) {
            return;
        }

        // Blokuj wszystkie próby wstawienia/zmiany na niewidocznych slotach
        if (!gui.isVisibleSlot(slot)) {
            event.setCancelled(true);
            return;
        }

        // Output slot jest TYLKO do odczytu - w creative też blokujemy!
        if (gui.isOutputSlot(slot)) {
            event.setCancelled(true);
            return;
        }
    }

    /**
     * Wyodrębni przedmioty z gridu do tablicy 16-elementowej.
     * Mapuje sloty [0,1,2,3,9,10,11,12,18,19,20,21,27,28,29,30]
     * na [0-15] w kolejności wierszy.
     * 
     * WAŻNE:
     * - Jeśli slot jest pusty: zwraca null (tak jak Bukkit Inventory#getItem)
     * - RecipeMatcher traktuje null jako pusty slot (odwzorowanie crafting table)
     */
    public static ItemStack[] extractGridItems(CraftingGUI gui) {
        if (gui == null) {
            return new ItemStack[16];
        }

        ItemStack[] gridItems = new ItemStack[16];
        Inventory inv = gui.getInventory();

        // Rząd 1 (sloty 0-3)
        for (int i = 0; i < 4; i++) {
            gridItems[i] = inv.getItem(i);
        }

        // Rząd 2 (sloty 9-12)
        for (int i = 0; i < 4; i++) {
            gridItems[4 + i] = inv.getItem(9 + i);
        }

        // Rząd 3 (sloty 18-21)
        for (int i = 0; i < 4; i++) {
            gridItems[8 + i] = inv.getItem(18 + i);
        }

        // Rząd 4 (sloty 27-30)
        for (int i = 0; i < 4; i++) {
            gridItems[12 + i] = inv.getItem(27 + i);
        }

        return gridItems;
    }

    /**
     * Sprawdza czy receptura pasuje i wyświetla wynik w output slocie.
     * Działa jak vanilla crafting table - automatycznie aktualizuje output.
     */
    public void updateOutput(Player player, CraftingGUI gui) {
        if (player == null || gui == null) {
            return;
        }

        RecipeManager manager = RecipeManager.getInstance();
        if (manager == null) {
            return;
        }

        // Wyodrębni przedmioty z gridu (4x4)
        ItemStack[] gridItems = extractGridItems(gui);

        // Spróbuj znaleźć pasującą recepturę
        // Najpierw custom receptury (4x4 pattern)
        for (CraftingRecipe recipe : manager.getAllRecipes()) {
            if (recipe == null) {
                continue;
            }

            // Sprawdzić czy gracz ma permissję do tej receptury ("*" = publiczna)
            if (!hasRecipePermission(player, recipe.getPermission())) {
                continue;
            }

            // Użyj RecipePattern dla custom receptur (auto-detect bounding box)
            // WAŻNE: Custom receptury NIE mogą się obracać - tylko dokładny match!
            RecipePattern pattern = recipe.getRecipePattern();
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }

            if (!RecipeMatcher.matchesPatternExactly(gridItems, pattern)) {
                continue;
            }

            // Znaleźliśmy pasującą custom recepturę - wyświetl output
            ItemStack output = recipe.getOutput();
            if (output != null) {
                gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, output.clone());
            }
            return; // Skończ - custom receptury mają priorytet
        }

        // Jeśli brak custom receptury - spróbuj vanilla (fallback dla 4x4)
        // Fallback będzie używał "Shrink Wrap" + pad do 3x3
        // To umożliwia graczowi ułożenie receptury w dowolnym miejscu siatki 4x4
        ItemStack vanillaOutput = tryVanillaCrafting4x4Fallback(player, gridItems);
        
        if (vanillaOutput != null) {
            gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, vanillaOutput);
            return;
        }

        // Żadna receptura nie pasuje - wyczyść output slot
        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, null);
    }

    /**
     * Obsługuje normalny click (LEFT/RIGHT) na output slot.
     * 
     * ⚠️ KRYTYCZNE: Event już został ANULOWANY zanim ta metoda jest wywoływana!
     * Bukkit NIE MOŻE dotknąć output slotu - my robimy wszystko RĘCZNIE.
     * 
     * Proces:
     * 1. RĘCZNIE daj graczowi output na kursor (player.setItemOnCursor)
     * 2. Wyczyść output slot
     * 3. Usuń składniki z gridu
     * 4. Zaktualizuj output dla następnego craftu
     * 
     * @param player Gracz który kliknął
     * @param gui GUI craftingu
     * @param output Item który jest w output slocie (NIE może być null/air!)
     * @param clickType Typ kliknięcia (LEFT/RIGHT)
     */
    private void handleNormalClickOutput(InventoryClickEvent event, Player player, CraftingGUI gui, ItemStack output, ClickType clickType, ItemStack cursorFromEvent) {
        if (player == null || gui == null || output == null || output.getType().isAir()) {
            return;
        }

        RecipeManager manager = RecipeManager.getInstance();
        if (manager == null) {
            return;
        }

        // Ustal ile itemów chcemy dać na kursor w tym jednym craftcie.
        // Dla spójności: zarówno LEFT jak i RIGHT zabierają cały wynik receptury.
        int takeAmount = output.getAmount();
        if (takeAmount <= 0) {
            return;
        }

        // Najpierw sprawdzamy czy na kursorze da się bezpiecznie dołożyć wynik.
        // To jest kluczowe, żeby nie robić "połowicznych" craftów.
        ItemStack currentCursor = (cursorFromEvent != null) ? cursorFromEvent : player.getItemOnCursor();
        ItemStack newCursor = buildNewCursorAfterTaking(currentCursor, output, takeAmount);
        if (newCursor == null) {
            return;
        }

        // Wyodrębni przedmioty z gridu
        ItemStack[] gridItems = extractGridItems(gui);

        // Znajdź recepturę która pasuje - najpierw custom
        for (CraftingRecipe recipe : manager.getAllRecipes()) {
            if (recipe == null || !hasRecipePermission(player, recipe.getPermission())) {
                continue;
            }

            RecipePattern pattern = recipe.getRecipePattern();
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }

            PatternMatchResult matchResult = RecipeMatcher.findPatternMatchExactly(gridItems, pattern);
            if (!matchResult.isMatched()) {
                continue;
            }

            // Znaleźliśmy recepturę! RĘCZNIE obsługujemy pickup:
            
            // 1. Daj graczowi output na kursor (lub dołóż do istniejącego stacka)
            // Uwaga: w anulowanych eventach bezpieczniej ustawić to przez event (lepsza synchronizacja klienta).
            if (event != null) {
                event.setCursor(newCursor);
            }
            player.setItemOnCursor(newCursor);
            
            // 2. Wyczyść output slot
            gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, null);
            if (event != null) {
                // Bukkit potrafi przywrócić kliknięty slot z wartości event.getCurrentItem().
                // Ustawiamy null, żeby nie było desync (zwłaszcza przy wielokrotnym klikaniu).
                event.setCurrentItem(null);
            }
            
            // 3. Usuń składniki z gridu
            RecipeMatcher.removeItemsFromMatch(gridItems, matchResult);
            updateGridSlots(gui, gridItems);
            
            // 4. Zaktualizuj output dla następnego craftu
            updateOutput(player, gui);

            // 5. Dla pewności: odśwież klienta (w anulowanych eventach potrafi się rozjechać UI)
            runNextTickIfPossible(player::updateInventory);
            
            return;
        }

        // Spróbuj vanilla shaped receptury
        for (VanillaRecipe recipe : manager.getAllVanillaRecipes()) {
            if (recipe == null) {
                continue;
            }

            if (!recipe.isShapeless()) {
                ItemStack[][] pattern = recipe.getPattern();
                if (pattern == null) {
                    continue;
                }

                PatternMatchResult matchResult = RecipeMatcher.findPatternMatchVanilla(gridItems, pattern);
                if (!matchResult.isMatched()) {
                    continue;
                }

                // Znaleźliśmy vanilla shaped recepturę! RĘCZNIE obsługujemy pickup:
                
                // 1. Daj graczowi output na kursor (lub dołóż do istniejącego stacka)
                if (event != null) {
                    event.setCursor(newCursor);
                }
                player.setItemOnCursor(newCursor);
                
                // 2. Wyczyść output slot
                gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, null);
                if (event != null) {
                    event.setCurrentItem(null);
                }
                
                // 3. Usuń składniki z gridu
                RecipeMatcher.removeItemsFromMatch(gridItems, matchResult);
                updateGridSlots(gui, gridItems);
                
                // 4. Zaktualizuj output dla następnego craftu
                updateOutput(player, gui);

                // 5. Sync UI
                runNextTickIfPossible(player::updateInventory);
                
                return;
            }
            // Shapeless receptura
            else {
                List<ItemStack> ingredients = recipe.getShapelessIngredients();
                if (ingredients == null || ingredients.isEmpty()) {
                    continue;
                }

                if (!RecipeMatcher.matchesShapelessVanilla(gridItems, ingredients)) {
                    continue;
                }

                // Znaleźliśmy vanilla shapeless recepturę! RĘCZNIE obsługujemy pickup:
                
                // 1. Daj graczowi output na kursor (lub dołóż do istniejącego stacka)
                if (event != null) {
                    event.setCursor(newCursor);
                }
                player.setItemOnCursor(newCursor);
                
                // 2. Wyczyść output slot
                gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, null);
                if (event != null) {
                    event.setCurrentItem(null);
                }
                
                // 3. Usuń składniki z gridu
                RecipeMatcher.removeShapelessItems(gridItems, ingredients);
                updateGridSlots(gui, gridItems);
                
                // 4. Zaktualizuj output dla następnego craftu
                updateOutput(player, gui);

                // 5. Sync UI
                runNextTickIfPossible(player::updateInventory);
                
                return;
            }
        }

        // Żadna receptura nie pasowała - nie powinno się zdarzyć
        // (output był tam bo receptura pasowała wcześniej)
        // Bezpieczna fallback: daj graczowi output bez usuwania składników
        if (event != null) {
            event.setCursor(newCursor);
        }
        player.setItemOnCursor(newCursor);
        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, null);
        if (event != null) {
            event.setCurrentItem(null);
        }
        updateOutput(player, gui);
        runNextTickIfPossible(player::updateInventory);
    }

    /**
     * Sprawdza czy można dołożyć wynik craftingu do itemu na kursorze.
     *
     * Prosto:
     * - jeśli item na kursorze jest "taki sam" (meta też), to można dołożyć
     * - ale tylko jeśli nie przekroczymy max stack size
     */
    static boolean canAddToCursor(ItemStack cursor, ItemStack output, int addAmount) {
        if (cursor == null || cursor.getType().isAir()) return true;
        if (output == null || output.getType().isAir()) return false;
        if (addAmount <= 0) return false;
        if (!cursor.isSimilar(output)) return false;
        int max = cursor.getMaxStackSize();
        return cursor.getAmount() + addAmount <= max;
    }

    /**
     * Buduje nowy stan kursora po zabraniu wyniku craftingu.
     * Zwraca null jeśli nie da się tego zrobić (np. inny item na kursorze lub brak miejsca).
     */
    static ItemStack buildNewCursorAfterTaking(ItemStack cursor, ItemStack output, int takeAmount) {
        if (output == null || output.getType().isAir() || takeAmount <= 0) {
            return null;
        }

        ItemStack resultPiece = output.clone();
        resultPiece.setAmount(takeAmount);

        if (cursor == null || cursor.getType().isAir()) {
            return resultPiece;
        }

        if (!canAddToCursor(cursor, output, takeAmount)) {
            return null;
        }

        ItemStack merged = cursor.clone();
        merged.setAmount(cursor.getAmount() + takeAmount);
        return merged;
    }

    /**
     * Obsługuje removal itemów gdy gracz wychyła output item
     * Wykonuje się DELAYED (po kliknięciu) żeby pozwolić graczowi wychylnąć item
     */
    private void handleOutputTakenDelayed(Player player, CraftingGUI gui) {
        if (player == null || gui == null) {
            return;
        }

        // Sprawdzę czy output slot teraz pusty (gracz zabrał item)
        ItemStack output = gui.getInventory().getItem(CraftingGUI.OUTPUT_SLOT);
        if (output != null && !output.getType().isAir()) {
            // Output jeszcze nie zabrany - czekaj następny tick
            return;
        }

        // Output został zabrany! Teraz usuń input itemy

        RecipeManager manager = RecipeManager.getInstance();
        if (manager == null) {
            return;
        }

        // Wyodrębni przedmioty z gridu
        ItemStack[] gridItems = extractGridItems(gui);

        // Spróbuj znaleźć recepturę która pasuje i usuń jej składniki
        // Najpierw custom receptury
        for (CraftingRecipe recipe : manager.getAllRecipes()) {
            if (recipe == null) {
                continue;
            }

            // Sprawdzić czy gracz ma permissję do tej receptury ("*" = publiczna)
            if (!hasRecipePermission(player, recipe.getPermission())) {
                continue;
            }

            // Użyj RecipePattern dla custom receptur
            // WAŻNE: Custom receptury NIE mogą się obracać - tylko dokładny match!
            RecipePattern pattern = recipe.getRecipePattern();
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }

            // Znajdź dokładny match tej receptury (BEZ rotacji)
            PatternMatchResult matchResult = RecipeMatcher.findPatternMatchExactly(gridItems, pattern);
            if (!matchResult.isMatched()) {
                continue;
            }

            // Znaleźliśmy recepturę! Usuń jej składniki z input slotów
            RecipeMatcher.removeItemsFromMatch(gridItems, matchResult);

            // Zaktualizuj sloty w okienku
            updateGridSlots(gui, gridItems);

            // Zaktualizuj output do następnego możliwego craftu
            updateOutput(player, gui);

            return; // Skończ - usunęliśmy składniki
        }

        // Spróbuj vanilla receptury
        for (VanillaRecipe recipe : manager.getAllVanillaRecipes()) {
            if (recipe == null) {
                continue;
            }

            // Shaped receptura
            if (!recipe.isShapeless()) {
                ItemStack[][] pattern = recipe.getPattern();
                if (pattern == null) {
                    continue;
                }

                // Znajdź dokładny match tej receptury
                PatternMatchResult matchResult = RecipeMatcher.findPatternMatchVanilla(gridItems, pattern);
                if (!matchResult.isMatched()) {
                    continue;
                }

                // Znaleźliśmy vanilla shaped recepturę! Usuń jej składniki
                RecipeMatcher.removeItemsFromMatch(gridItems, matchResult);

                // Zaktualizuj sloty w okienku
                updateGridSlots(gui, gridItems);

                // Zaktualizuj output do następnego możliwego craftu
                updateOutput(player, gui);

                return; // Skończ - usunęliśmy składniki
            }
            // Shapeless receptura
            else {
                List<ItemStack> ingredients = recipe.getShapelessIngredients();
                if (ingredients == null || ingredients.isEmpty()) {
                    continue;
                }

                // Sprawdzić czy receptura pasuje - vanilla obsługuje NBT tags (custom itemy)
                if (!RecipeMatcher.matchesShapelessVanilla(gridItems, ingredients)) {
                    continue;
                }

                // Znaleźliśmy vanilla shapeless recepturę! Usuń jej składniki
                RecipeMatcher.removeShapelessItems(gridItems, ingredients);

                // Skopiuj zmodyfikowany grid z powrotem do okienka
                updateGridSlots(gui, gridItems);

                // Zaktualizuj output do następnego możliwego craftu
                updateOutput(player, gui);

                return; // Skończ - usunęliśmy składniki
            }
        }

        // Żadna receptura nie pasowała - to nie powinno się stać
        // (bo output był tam wciśnięty)
    }

    /**
     * Obsługuje Shift+Click na output slot.
     * Craftuje wielokrotnie ASYNCHRONICZNIE - TYLKO TĄ SAMĄ RECEPTURĘ - aż się zmieni lub zabraknie składników.
     * 
     * Dlaczego async?
     * - Shift+click 1000 itemów nie zablokuje TPS serwera
     * - Crafting logika robimy async (thread-safe)
     * - Update inventory tylko na main threadzie (safe!)
     */
    private void handleShiftClickOutput(Player player, CraftingGUI gui) {
        if (player == null || gui == null) {
            return;
        }

        RecipeManager manager = RecipeManager.getInstance();
        if (manager == null) {
            return;
        }

        // Najpierw znajdź aktualnie pasującą recepturę (aby wiedzieć którą craftować)
        ItemStack[] gridItems = extractGridItems(gui);
        Object foundRecipe = null; // Może być CraftingRecipe lub VanillaRecipe
        ItemStack craftOutput = null;

        // Szukaj custom receptury
        for (CraftingRecipe recipe : manager.getAllRecipes()) {
            if (recipe == null) {
                continue;
            }

            // Sprawdź permisję receptury ("*" = publiczna dla wszystkich)
            if (!hasRecipePermission(player, recipe.getPermission())) {
                continue;
            }

            RecipePattern pattern = recipe.getRecipePattern();
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }

            PatternMatchResult matchResult = RecipeMatcher.findPatternMatchExactly(gridItems, pattern);
            if (matchResult.isMatched()) {
                foundRecipe = recipe;
                craftOutput = recipe.getOutput();
                break;
            }
        }

        // Jeśli nie znalazła custom - szukaj vanilla
        if (foundRecipe == null) {
            for (VanillaRecipe recipe : manager.getAllVanillaRecipes()) {
                if (recipe == null) {
                    continue;
                }

                if (!recipe.isShapeless()) {
                    ItemStack[][] pattern = recipe.getPattern();
                    if (pattern == null) {
                        continue;
                    }

                    PatternMatchResult matchResult = RecipeMatcher.findPatternMatchVanilla(gridItems, pattern);
                    if (matchResult.isMatched()) {
                        foundRecipe = recipe;
                        craftOutput = recipe.getOutput();
                        break;
                    }
                } else {
                    List<ItemStack> ingredients = recipe.getShapelessIngredients();
                    if (ingredients == null || ingredients.isEmpty()) {
                        continue;
                    }

                    if (RecipeMatcher.matchesShapelessVanilla(gridItems, ingredients)) {
                        foundRecipe = recipe;
                        craftOutput = recipe.getOutput();
                        break;
                    }
                }
            }
        }

        // Jeśli nic nie znalezła - wyjdź
        if (foundRecipe == null || craftOutput == null || craftOutput.getType().isAir()) {
            return;
        }

        // Uruchom async task - craftuje bez blokowania serwera!
        // Jeśli brak pluginu - spróbuj użyć pierwszego załadowanego (dla testów integracyjnych)
        JavaPlugin pluginToUse = this.plugin;
        if (pluginToUse == null && Bukkit.getServer() != null) {
            try {
                for (org.bukkit.plugin.Plugin p : Bukkit.getPluginManager().getPlugins()) {
                    if (p instanceof JavaPlugin && p.isEnabled()) {
                        pluginToUse = (JavaPlugin) p;
                        break;
                    }
                }
            } catch (Exception ignored) {
                // Ignoruj - brak pluginu
            }
        }

        if (pluginToUse == null) {
            return;
        }

        ShiftClickAsyncTask asyncTask = new ShiftClickAsyncTask(player, gui, pluginToUse, this, foundRecipe, craftOutput);
        asyncTask.runTaskAsynchronously(pluginToUse);
    }

    /**
     * Dodaje item do inventorza gracza.
     * Jeśli brak miejsca - dropuje na ziemię (jak w vanilla Minecraft).
     * Static aby mogły z niego używać async taski!
     */
    public static void addItemToPlayerOrDrop(Player player, ItemStack item) {
        if (player == null || item == null || item.getType().isAir()) {
            return;
        }

        // Spróbuj dodać do inventorza
        java.util.HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(item);

        // Jeśli są remaining itemy - dropuj na ziemię
        for (ItemStack drop : remaining.values()) {
            if (drop == null || drop.getType().isAir()) {
                continue;
            }
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    // Główna logika craftingu
    // Słucha kiedy gracz drag'uje item'y (przeciąga z myszą)
    /**
     * Obsługuje drag event - gdy gracz przesunął item myszą.
     * 
     * BUG FIX (24.01.2026):
     * Problem: Gdy gracz drag'ował item, output slot nie się aktualizował.
     * Przyczyna: updateOutput() nie było wywoływane w onInventoryDrag().
     * Rozwiązanie: Dodano runNextTickIfPossible(() -> updateOutput()) na koniec.
     * 
     * Scenariusz testowy:
     * 1. Gracz ma recepturę (4 itemy) -> output wyświetlony
     * 2. Gracz drag'uje 5-ty item do gridu
     * 3. onInventoryDrag() się wypalał
     * 4. Teraz: updateOutput() się planuje
     * 5. updateOutput() czyszcze output (receptura nie pasuje)
     * 
     * Rezultat: Output zawsze aktualny, niezależnie od click czy drag.
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        CraftingGUI gui = activeGUIs.get(playerId);

        // Podobnie jak w onInventoryClick - używamy TOP inventory z widoku
        Inventory topInventory = null;
        if (event.getView() != null) {
            topInventory = event.getView().getTopInventory();
        }
        if (topInventory == null) {
            topInventory = event.getInventory();
        }

        // Sprawdzenie czy to nasze GUI
        if (gui == null || topInventory == null || !topInventory.equals(gui.getInventory())) {
            return;
        }

        // Sprawdź czy gracz próbuje drag'ować na jakiś niedostępny slot
        // Niedostępne sloty są ukryte za customowym fontem - traktujemy je jakby ich nie było
        for (int slot : event.getRawSlots()) {
            if (!gui.isVisibleSlot(slot)) {
                // Blokuj drag - próba dotknięcia niewidocznego slotu
                event.setCancelled(true);
                return;
            }
        }

        // Jeśli którykolwiek slot w drag'u to output (slot 24) - blokuj!
        // Output slot to przycisk do craftowania, nie można go modyfikować
        if (event.getRawSlots().contains(CraftingGUI.OUTPUT_SLOT)) {
            event.setCancelled(true);
            return;
        }

        // Wszystkie sloty w drag'u są prawidłowe (input sloty) - pozwól na drag'owanie
        // Po zakończeniu drag'owania - zaktualizuj output (BUG FIX)
        runNextTickIfPossible(() -> updateOutput(player, gui));
    }

    // Słucha kiedy gracz zamyka okienko
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            CraftingGUI gui = activeGUIs.get(player.getUniqueId());

            // Jeśli to nasze GUI - zwróć itemy i wyrejestruj go
            if (gui != null && event.getInventory().equals(gui.getInventory())) {
                // Zwróć wszystkie itemy z input slotów do ekwipunku gracza
                returnItemsToPlayerInventory(player, gui);
                
                unregisterGUI(player.getUniqueId());
            }
        }
    }

    /**
     * Zaktualizuj sloty gridu w okienku zgodnie z tablicą przedmiotów.
     * Używane po removal itemów - aby GUI pokazała aktualne stany slotów.
     * Static aby mogły z niego używać async taski!
     */
    public static void updateGridSlots(CraftingGUI gui, ItemStack[] gridItems) {
        if (gui == null || gridItems == null || gridItems.length < 16) {
            return;
        }

        // Zaktualizuj sloty w GUI na podstawie tablicy gridItems
        // Rząd 1: sloty 0-3
        for (int i = 0; i < 4; i++) {
            gui.getInventory().setItem(i, gridItems[i]);
        }

        // Rząd 2: sloty 9-12
        for (int i = 0; i < 4; i++) {
            gui.getInventory().setItem(9 + i, gridItems[4 + i]);
        }

        // Rząd 3: sloty 18-21
        for (int i = 0; i < 4; i++) {
            gui.getInventory().setItem(18 + i, gridItems[8 + i]);
        }

        // Rząd 4: sloty 27-30
        for (int i = 0; i < 4; i++) {
            gui.getInventory().setItem(27 + i, gridItems[12 + i]);
        }
    }

    /**
     * Zwraca wszystkie itemy z grid'u do ekwipunku gracza.
     * Jeśli ekwipunek jest pełny - rzuca itemy na ziemię przy graczu.
     */
    private void returnItemsToPlayerInventory(Player player, CraftingGUI gui) {
        Inventory playerInv = player.getInventory();
        Inventory guiInv = gui.getInventory();

        // Zbieramy WSZYSTKIE itemy z GUI poza output slotem.
        // To jest zabezpieczenie na wypadek, gdyby item wpadł w ukryty slot.
        // (Bez tego gracz mógłby stracić itemy.)
        for (int slot = 0; slot < guiInv.getSize(); slot++) {
            if (slot == CraftingGUI.OUTPUT_SLOT) {
                continue;
            }

            ItemStack item = guiInv.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }

            // Czyścimy slot w GUI (żeby nie było duplikacji)
            guiInv.setItem(slot, null);

            // Spróbuj dodać do ekwipunku
            java.util.Map<Integer, ItemStack> leftover = playerInv.addItem(item.clone());

            // Jeśli coś zostało (ekwipunek pełny) - rzuć item na ziemię
            for (ItemStack remaining : leftover.values()) {
                if (remaining != null && !remaining.getType().isAir()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), remaining);
                }
            }
        }
    }

    /**
     * Przerzuca itemy do 16 slotów gridu (4x4).
     * To jest używane głównie do obsługi shift-click z ekwipunku gracza.
     */
    private ItemStack moveIntoGrid(Inventory guiInventory, ItemStack stack) {
        if (guiInventory == null || stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return stack;
        }

        int maxStack = Math.max(1, stack.getMaxStackSize());

        // 1) Najpierw spróbuj dopełnić istniejące stacki w gridzie
        for (int slot : CraftingGUI.GRID_SLOTS) {
            if (stack.getAmount() <= 0) {
                return null;
            }

            ItemStack existing = guiInventory.getItem(slot);
            if (existing == null || existing.getType().isAir()) {
                continue;
            }

            // Łączymy tylko jeśli itemy są takie same (typ + meta)
            if (!existing.isSimilar(stack)) {
                continue;
            }

            int free = maxStack - existing.getAmount();
            if (free <= 0) {
                continue;
            }

            int toMove = Math.min(free, stack.getAmount());
            existing.setAmount(existing.getAmount() + toMove);
            stack.setAmount(stack.getAmount() - toMove);
            guiInventory.setItem(slot, existing);
        }

        // 2) Potem wkładaj w puste sloty gridu
        for (int slot : CraftingGUI.GRID_SLOTS) {
            if (stack.getAmount() <= 0) {
                return null;
            }

            ItemStack existing = guiInventory.getItem(slot);
            if (existing != null && !existing.getType().isAir()) {
                continue;
            }

            int toPut = Math.min(maxStack, stack.getAmount());
            ItemStack placed = stack.clone();
            placed.setAmount(toPut);
            guiInventory.setItem(slot, placed);
            stack.setAmount(stack.getAmount() - toPut);
        }

        return stack.getAmount() > 0 ? stack : null;
    }

    /**
     * Ekstrakcja 3x3 gridu z lewego górnego rogu 4x4 gridu.
     * 
     * 4x4 grid w GUI:
     * [0 ][1 ][2 ][3 ]
     * [4 ][5 ][6 ][7 ]
     * [8 ][9 ][10][11]
     * [12][13][14][15]
     * 
     * 3x3 grid (z lewego górnego rogu):
     * [0 ][1 ][2 ]
     * [4 ][5 ][6 ]
     * [8 ][9 ][10]
     * 
     * @param grid 4x4 grid z craftingu
     * @return 3x3 array (9 elementów)
     */
    private ItemStack[] extract3x3Grid(ItemStack[] grid) {
        ItemStack[] grid3x3 = new ItemStack[9];
        
        // Bierz tylko 3 pierwsze kolumny z 3 pierwszych rzędów
        grid3x3[0] = grid[0];
        grid3x3[1] = grid[1];
        grid3x3[2] = grid[2];
        // Pomijamy grid[3] (4-ta kolumna)
        
        grid3x3[3] = grid[4];
        grid3x3[4] = grid[5];
        grid3x3[5] = grid[6];
        // Pomijamy grid[7] (4-ta kolumna)
        
        grid3x3[6] = grid[8];
        grid3x3[7] = grid[9];
        grid3x3[8] = grid[10];
        // Pomijamy grid[11] (4-ta kolumna)
        // Pomijamy grid[12-15] (4-ty rząd)
        
        return grid3x3;
    }

    /**
     * Tworzy macierz 3x3 do vanilla craftingu na podstawie 4x4.
     * Zasada: bierzemy bounding box wszystkich niepustych slotów w 4x4.
     * Jeśli bounding box ma szerokość/wysokość <= 3, kopiujemy go do 3x3 (od (0,0)).
     * Dzięki temu vanilla działa niezależnie od pozycji w GUI 4x4.
     */
    private ItemStack[] extractVanilla3x3GridFrom4x4(ItemStack[] grid4x4) {
        if (grid4x4 == null || grid4x4.length != 16) {
            return null;
        }

        int minRow = 4;
        int minCol = 4;
        int maxRow = -1;
        int maxCol = -1;

        for (int i = 0; i < 16; i++) {
            ItemStack item = grid4x4[i];
            if (item == null || item.getType().isAir()) {
                continue;
            }
            int row = i / 4;
            int col = i % 4;
            if (row < minRow) minRow = row;
            if (col < minCol) minCol = col;
            if (row > maxRow) maxRow = row;
            if (col > maxCol) maxCol = col;
        }

        // Pusto
        if (maxRow == -1) {
            return new ItemStack[9];
        }

        int height = maxRow - minRow + 1;
        int width = maxCol - minCol + 1;

        // Jeśli nie mieści się w 3x3, to vanilla nie ma prawa zadziałać
        if (height > 3 || width > 3) {
            return null;
        }

        ItemStack[] grid3x3 = new ItemStack[9];
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                int fromIndex = (minRow + r) * 4 + (minCol + c);
                int toIndex = r * 3 + c;
                grid3x3[toIndex] = grid4x4[fromIndex];
            }
        }

        return grid3x3;
    }

    /**
     * Próbuje znaleźć output za pomocą vanilla crafting simulatora.
     * Każdy gracz ma swoją instancję simulatora.
     * 
     * @param player gracz
     * @param grid3x3 3x3 grid do sprawdzenia
     * @return ItemStack (output) lub null jeśli brak match'a
     */
    private ItemStack tryVanillaCrafting(Player player, ItemStack[] grid3x3) {
        if (player == null || grid3x3 == null) {
            return null;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Utwórz simulator dla gracza jeśli go jeszcze nie ma
        VanillaCraftingSimulator simulator = vanillaSimulators.get(playerId);
        if (simulator == null) {
            simulator = new VanillaCraftingSimulator();
            vanillaSimulators.put(playerId, simulator);
        }
        
        // Spróbuj znaleźć vanilla recepturę
        return simulator.tryMatch(player, grid3x3);
    }

    /**
     * Próbuje znaleźć output za pomocą vanilla crafting simulatora dla 4x4.
     * 
     * FALLBACK mechanizm:
     * 1. Jeśli 4x4 grid zawiera recepturę, która mieści się w max 3x3
     * 2. Robi "Shrink Wrap" (zmniejszenie do minimalnego prostokąta)
     * 3. Pad-uje do 3x3 (jeśli mniejsza)
     * 4. Próbuje znaleźć vanilla recepturę (obsługuje Tagi i ComplexRecipes)
     * 
     * To umożliwia:
     * - Gracze mogą ułożyć recepturę w dowolnym miejscu siatki 4x4
     * - Obsługa Vanilla receptur (ze all jej takami i ComplexRecipes)
     * 
     * @param player gracz
     * @param grid4x4 Kompletny 4x4 grid (rozmiar 16)
     * @return ItemStack (output) lub null jeśli brak match'a
     */
    private ItemStack tryVanillaCrafting4x4Fallback(Player player, ItemStack[] grid4x4) {
        if (player == null || grid4x4 == null || grid4x4.length != 16) {
            return null;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Utwórz simulator dla gracza jeśli go jeszcze nie ma
        VanillaCraftingSimulator simulator = vanillaSimulators.get(playerId);
        if (simulator == null) {
            simulator = new VanillaCraftingSimulator();
            vanillaSimulators.put(playerId, simulator);
        }
        
        // Spróbuj znaleźć vanilla recepturę dla 4x4 z fallback mechanizmem
        // Simulator zrobi "Shrink Wrap" + pad do 3x3 wewnętrznie
        return simulator.tryMatch4x4Fallback(player, grid4x4);
    }

    /**
     * Czyści simulatory gdy gracz zamyka GUI lub opuszcza serwer.
     */
    public void cleanupPlayer(UUID playerId) {
        activeGUIs.remove(playerId);
        vanillaSimulators.remove(playerId);
    }
}



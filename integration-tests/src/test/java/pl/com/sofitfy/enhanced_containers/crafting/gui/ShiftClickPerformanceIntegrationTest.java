package pl.com.sofitfy.enhanced_containers.crafting.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.CraftingRecipe;
import pl.com.sofitfy.enhanced_containers.crafting.recipe.RecipeManager;
import pl.com.sofitfy.enhanced_containers.testutils.MockBukkitPluginTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy wydajnościowe shift+click craftingu (integracyjne - z MockBukkit).
 * 
 * Mierzą:
 * - Wydajność pełnego flow shift+click (event -> handler -> inventory updates)
 * - Wydajność przy wielu graczach craftujących jednocześnie
 * - Wydajność aktualizacji UI (updateOutput)
 * - Throughput w pełnym kontekście Bukkit
 */
@DisplayName("Wydajność - Shift+click (integracyjne)")
class ShiftClickPerformanceIntegrationTest extends MockBukkitPluginTestBase {

    /**
     * Test wydajności pełnego shift+click flow.
     * Od triggera eventu do aktualizacji output.
     */
    @Test
    @DisplayName("Pełny flow shift+click - 50 craftu")
    void fullShiftClickFlowPerformance() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Przygotowujemy recepturę
        ItemStack[][] pattern = new ItemStack[1][1];
        pattern[0][0] = new ItemStack(Material.DIAMOND);

        CraftingRecipe recipe = new CraftingRecipe(
            "perf_test_1",
            "Performance Test 1",
            pattern,
            new ItemStack(Material.DIAMOND_BLOCK, 1),
            "*"
        );

        RecipeManager.getInstance().registerRecipe(recipe);

        // Wrzucamy 50 diamenty do gridu
        gui.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 50));
        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND_BLOCK, 1));

        player.openInventory(gui.getInventory());

        // Mierzymy czas pełnego shift+click
        long startTime = System.nanoTime();

        // Tworzymy i obsługujemy event
        org.bukkit.event.inventory.InventoryClickEvent event = 
            new org.bukkit.event.inventory.InventoryClickEvent(
                player.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                CraftingGUI.OUTPUT_SLOT,
                org.bukkit.event.inventory.ClickType.SHIFT_LEFT,
                org.bukkit.event.inventory.InventoryAction.PICKUP_ALL
            );

        listener.onInventoryClick(event);
        
        // Czekamy na wykonanie async craftingu
        try {
            Thread.sleep(200); // Daj czas na async task (50 craftu)
        } catch (InterruptedException e) {
            // ignore
        }
        tick(1); // Wykonaj scheduled callback z async task

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        // Powinno być szybko - max 500ms na 50 craftu
        assertTrue(durationMs < 500,
            String.format("Shift+click pełny flow zbyt wolny: %d ms", durationMs));

        // Sprawdzamy czy gracz ma wszystkie crafted items
        int totalBlocks = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND_BLOCK) {
                totalBlocks += item.getAmount();
            }
        }

        assertEquals(50, totalBlocks, "Gracz powinien mieć 50 bloków diamantu");

        System.out.println(String.format(
            "✓ Pełny shift+click flow: %d ms na 50 craftu",
            durationMs
        ));
    }

    /**
     * Test throughput - ile craftu na sekundę.
     * Mierzy maksymalną prędkość craftowania.
     */
    @Test
    @DisplayName("Throughput - 500 craftu")
    void craftsPerSecond() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Receptura
        ItemStack[][] pattern = new ItemStack[1][1];
        pattern[0][0] = new ItemStack(Material.OAK_PLANKS);

        CraftingRecipe recipe = new CraftingRecipe(
            "perf_test_2",
            "Performance Test 2",
            pattern,
            new ItemStack(Material.STICK, 1),
            "*"
        );

        RecipeManager.getInstance().registerRecipe(recipe);

        // 500 desek do gridu
        gui.getInventory().setItem(0, new ItemStack(Material.OAK_PLANKS, 500));
        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.STICK, 1));

        player.openInventory(gui.getInventory());

        long startTime = System.nanoTime();

        org.bukkit.event.inventory.InventoryClickEvent event = 
            new org.bukkit.event.inventory.InventoryClickEvent(
                player.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                CraftingGUI.OUTPUT_SLOT,
                org.bukkit.event.inventory.ClickType.SHIFT_LEFT,
                org.bukkit.event.inventory.InventoryAction.PICKUP_ALL
            );

        listener.onInventoryClick(event);
        tick(1);

        long endTime = System.nanoTime();
        long durationNs = endTime - startTime;
        double craftsPerSecond = (500.0 / durationNs) * 1_000_000_000;

        assertTrue(craftsPerSecond > 100,
            String.format("Zbyt mało craftu na sekundę: %.0f", craftsPerSecond));

        System.out.println(String.format(
            "✓ Throughput: %.0f craftu na sekundę",
            craftsPerSecond
        ));
    }

    /**
     * Test wielograczowy - czy system obsługuje wielu graczy craftujących jednocześnie.
     */
    @Test
    @DisplayName("Wielograczowy - 10 graczy craftu jednocześnie")
    void multiplayerCraftingPerformance() {
        int playerCount = 10;
        PlayerMock[] players = new PlayerMock[playerCount];
        CraftingGUI[] guis = new CraftingGUI[playerCount];

        // Przygotowujemy 10 graczy
        for (int i = 0; i < playerCount; i++) {
            players[i] = createPlayer();
            players[i].setOp(true);

            guis[i] = new CraftingGUI(players[i], "Crafting " + i);

            // Każdy gracz ma własną recepturę
            ItemStack[][] pattern = new ItemStack[1][1];
            pattern[0][0] = new ItemStack(Material.DIAMOND);

            CraftingRecipe recipe = new CraftingRecipe(
                "perf_player_" + i,
                "Performance Test Player " + i,
                pattern,
                new ItemStack(Material.DIAMOND_BLOCK, 1),
                "*"
            );

            RecipeManager.getInstance().registerRecipe(recipe);

            // 100 diamenty do gridu
            guis[i].getInventory().setItem(0, new ItemStack(Material.DIAMOND, 100));
            guis[i].getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.DIAMOND_BLOCK, 1));

            players[i].openInventory(guis[i].getInventory());
        }

        CraftingGUIListener listener = new CraftingGUIListener();

        // Rejestrujemy wszystkie GUI
        for (int i = 0; i < playerCount; i++) {
            listener.registerGUI(guis[i]);
        }

        // Mierzymy czas dla wszystkich 10 graczy
        long startTime = System.nanoTime();

        for (int i = 0; i < playerCount; i++) {
            org.bukkit.event.inventory.InventoryClickEvent event = 
                new org.bukkit.event.inventory.InventoryClickEvent(
                    players[i].getOpenInventory(),
                    org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                    CraftingGUI.OUTPUT_SLOT,
                    org.bukkit.event.inventory.ClickType.SHIFT_LEFT,
                    org.bukkit.event.inventory.InventoryAction.PICKUP_ALL
                );

            listener.onInventoryClick(event);
        }

        // Czekamy na wykonanie async craftingu dla wszystkich graczy
        try {
            Thread.sleep(500); // Daj czas na wszystkie async taski (10 graczy * 100 craftu)
        } catch (InterruptedException e) {
            // ignore
        }
        tick(1); // Wykonaj scheduled callback z async task

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        // Powinno być szybko - max 2 sekundy na 10 graczy
        assertTrue(durationMs < 2000,
            String.format("Wielograczowy craft zbyt wolny: %d ms", durationMs));

        // Sprawdzamy czy każdy gracz ma wszystkie crafted items
        for (int i = 0; i < playerCount; i++) {
            int totalBlocks = 0;
            for (ItemStack item : players[i].getInventory().getContents()) {
                if (item != null && item.getType() == Material.DIAMOND_BLOCK) {
                    totalBlocks += item.getAmount();
                }
            }

            assertEquals(100, totalBlocks,
                String.format("Gracz %d powinien mieć 100 bloków diamantu", i));
        }

        System.out.println(String.format(
            "✓ Wielograczowy (10 graczy): %d ms na 100 craftu każdy",
            durationMs
        ));
    }

    /**
     * Test wydajności updateOutput.
     * Ta metoda się wywoła po każdym craftingu w pętli.
     * Powinna być szybka bo jest wielokrotnie używana.
     */
    @Test
    @DisplayName("UpdateOutput performance - 100 wywołań")
    void updateOutputPerformance() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Przygotowujemy recepturę
        ItemStack[][] pattern = new ItemStack[1][1];
        pattern[0][0] = new ItemStack(Material.DIAMOND);

        CraftingRecipe recipe = new CraftingRecipe(
            "perf_update_output",
            "Performance Test Update Output",
            pattern,
            new ItemStack(Material.DIAMOND_BLOCK, 1),
            "*"
        );

        RecipeManager.getInstance().registerRecipe(recipe);

        player.openInventory(gui.getInventory());

        // Mierzymy czas 100 wywołań updateOutput
        gui.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 100));

        long startTime = System.nanoTime();

        for (int i = 0; i < 100; i++) {
            listener.updateOutput(player, gui);
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        double msPerUpdate = (double) durationMs / 100.0;

        // updateOutput powinno być szybkie - max 1ms per call
        assertTrue(msPerUpdate < 1.0,
            String.format("updateOutput zbyt wolny: %.3f ms per call", msPerUpdate));

        System.out.println(String.format(
            "✓ updateOutput: %d ms na 100 wywołań (%.3f ms per call)",
            durationMs, msPerUpdate
        ));
    }

    /**
     * Test stress - maksymalna ilość craftu w jednym click.
     * W praktyce max to 64 items na stack, ale testujemy czy system się nie zawali
     * przy nawet większych ilościach.
     */
    @Test
    @DisplayName("Stress test - 1000 craftu w jednym click")
    void stressTest1000Crafts() {
        PlayerMock player = createPlayer();
        player.setOp(true);

        CraftingGUI gui = new CraftingGUI(player, "Crafting");
        CraftingGUIListener listener = new CraftingGUIListener();
        listener.registerGUI(gui);

        // Receptura
        ItemStack[][] pattern = new ItemStack[1][1];
        pattern[0][0] = new ItemStack(Material.OAK_PLANKS);

        CraftingRecipe recipe = new CraftingRecipe(
            "perf_stress",
            "Stress Test",
            pattern,
            new ItemStack(Material.STICK, 1),
            "*"
        );

        RecipeManager.getInstance().registerRecipe(recipe);

        // 1000 desek
        gui.getInventory().setItem(0, new ItemStack(Material.OAK_PLANKS, 1000));
        gui.getInventory().setItem(CraftingGUI.OUTPUT_SLOT, new ItemStack(Material.STICK, 1));

        player.openInventory(gui.getInventory());

        long startTime = System.nanoTime();

        org.bukkit.event.inventory.InventoryClickEvent event = 
            new org.bukkit.event.inventory.InventoryClickEvent(
                player.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                CraftingGUI.OUTPUT_SLOT,
                org.bukkit.event.inventory.ClickType.SHIFT_LEFT,
                org.bukkit.event.inventory.InventoryAction.PICKUP_ALL
            );

        listener.onInventoryClick(event);
        tick(1);

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        // Nawet przy 1000 craftu powinno być mniej niż 10 sekund
        assertTrue(durationMs < 10000,
            String.format("Stress test 1000 craftu zbyt wolny: %d ms", durationMs));

        System.out.println(String.format(
            "✓ Stress test (1000 craftu): %d ms",
            durationMs
        ));
    }
}

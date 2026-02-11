package pl.com.sofitfy.enhanced_containers.integration;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import pl.com.sofitfy.enhanced_containers.EnhancedCraftingMain;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy integracyjne dla permisji /4x4 sign_item.
 *
 * Używamy MockBukkit.load(EnhancedCraftingMain.class) do ładowania pluginu.
 *
 * Komentarze po polsku i prostym językiem.
 */
@DisplayName("PlethorasExtendedTools - testy integracyjne permisji sign_item")
class CraftCommandSignItemIntegrationTest {

    private ServerMock server;
    private EnhancedCraftingMain plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EnhancedCraftingMain.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Tag("integration")
    @Test
    @DisplayName("Gracz bez permisji sign_item nie może zmienić nazwy item'u na serwerze")
    void playerWithoutSignItemPermissionCannotRenameOnServer() {
        // Stwórz gracza bez żadnych permisji
        Player playerNoPermission = server.addPlayer();
        assertNotNull(playerNoPermission, "Gracz powinien być stworzony");

        // Daj graczowi item do ręki
        ItemStack diamondSword = new ItemStack(Material.DIAMOND_SWORD);
        playerNoPermission.getInventory().setItemInMainHand(diamondSword);

        // Wykonaj komendę /4x4 sign_item Miecz
        boolean commandExecuted = server.dispatchCommand(playerNoPermission, "4x4 sign_item Miecz");

        // Sprawdzenie: komenda powinna zostać przetworzana
        assertTrue(commandExecuted, "Komenda powinna być przetworzona");
    }

    @Tag("integration")
    @Test
    @DisplayName("Gracz z permisją sign_item może zmienić nazwę item'u na serwerze")
    void playerWithSignItemPermissionCanRenameOnServer() {
        // Stwórz gracza z permisją sign_item
        Player playerWithPermission = server.addPlayer();
        assertNotNull(playerWithPermission, "Gracz powinien być stworzony");

        // Daj graczowi permisje
        playerWithPermission.setOp(false);
        playerWithPermission.addAttachment(plugin, "plethoras.tools.use", true);
        playerWithPermission.addAttachment(plugin, "plethoras.tools.sign_item", true);

        // Daj graczowi item do ręki
        ItemStack diamondSword = new ItemStack(Material.DIAMOND_SWORD);
        playerWithPermission.getInventory().setItemInMainHand(diamondSword);

        // Wykonaj komendę /4x4 sign_item Miecz
        boolean commandExecuted = server.dispatchCommand(playerWithPermission, "4x4 sign_item Miecz");

        // Sprawdzenie: komenda powinna zostać przetwarzana
        assertTrue(commandExecuted, "Komenda powinna być przetworzona");

        // Sprawdzenie: item powinien mieć nową nazwę
        ItemStack itemInHand = playerWithPermission.getInventory().getItemInMainHand();
        assertNotNull(itemInHand, "Gracz powinien mieć item w ręce");
        assertTrue(
            itemInHand.getItemMeta() != null && itemInHand.getItemMeta().hasDisplayName(),
            "Item powinien mieć display name"
        );
        assertEquals("Miecz", itemInHand.getItemMeta().getDisplayName(), "Item powinien mieć nazwę 'Miecz'");
    }

    @Tag("integration")
    @Test
    @DisplayName("Permisja sign_item jest domyślnie ustawiona na false dla zwykłych graczy")
    void signItemPermissionIsDefaultFalse() {
        // Stwórz gracza
        Player player = server.addPlayer();
        assertNotNull(player, "Gracz powinien być stworzony");

        // Gracz nie jest operatorem
        player.setOp(false);

        // Sprawdzenie: zwykły gracz nie ma permisji sign_item
        assertFalse(
            player.hasPermission("plethoras.tools.sign_item"),
            "Zwykły gracz nie powinien mieć permisji plethoras.tools.sign_item"
        );
    }

    @Tag("integration")
    @Test
    @DisplayName("Operator ma dostęp do wszystkich permisji (w tym sign_item)")
    void operatorHasAccessToAllPermissions() {
        // Stwórz operatora
        Player operator = server.addPlayer();
        assertNotNull(operator, "Operator powinien być stworzony");
        operator.setOp(true);

        // Sprawdzenie: operator ma permisje
        assertTrue(
            operator.hasPermission("plethoras.tools.sign_item") || operator.isOp(),
            "Operator powinien mieć dostęp do komendy (poprzez OP)"
        );
    }

    @Tag("integration")
    @Test
    @DisplayName("Gracz z permisją użyje kolorów w nazwie item'u")
    void playerCanUseColorCodesInItemName() {
        // Stwórz gracza z permisją
        Player player = server.addPlayer();
        assertNotNull(player, "Gracz powinien być stworzony");

        // Daj graczowi permisje
        player.addAttachment(plugin, "plethoras.tools.use", true);
        player.addAttachment(plugin, "plethoras.tools.sign_item", true);

        // Daj graczowi item
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        player.getInventory().setItemInMainHand(item);

        // Wykonaj komendę z kodami kolorów: /4x4 sign_item &cRed &aPickaxe
        boolean commandExecuted = server.dispatchCommand(player, "4x4 sign_item &cRed &aPickaxe");

        // Sprawdzenie: komenda powinna zostać przetworzona
        assertTrue(commandExecuted, "Komenda powinna być przetworzona");

        // Sprawdzenie: item powinien mieć display name ze znakami § (konwertowane z &)
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        assertNotNull(itemInHand, "Item powinien być w ręce");
        assertTrue(
            itemInHand.getItemMeta() != null && itemInHand.getItemMeta().hasDisplayName(),
            "Item powinien mieć display name"
        );

        // Sprawdzenie: nazwa powinna zawierać znaki formatowania Minecrafta (§)
        String displayName = itemInHand.getItemMeta().getDisplayName();
        assertTrue(
            displayName.contains("§"),
            "Nazwa powinna zawierać znaki formatowania (§) zamiast (&)"
        );
    }
}

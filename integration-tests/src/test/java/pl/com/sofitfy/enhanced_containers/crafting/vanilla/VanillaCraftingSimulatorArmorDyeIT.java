package pl.com.sofitfy.enhanced_containers.crafting.vanilla;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VanillaCraftingSimulator - Leather Armor Dye IT")
class VanillaCraftingSimulatorArmorDyeIT {

    private ServerMock server;
    private PlayerMock player;
    private VanillaCraftingSimulator simulator;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        simulator = new VanillaCraftingSimulator();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("3x3: LEATHER_HELMET + RED_DYE -> colored helmet")
    void dyeLeatherHelmet3x3() {
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        ItemStack dye = new ItemStack(Material.RED_DYE);

        Color defaultColor = ((LeatherArmorMeta) helmet.getItemMeta()).getColor();

        ItemStack[] grid3x3 = new ItemStack[9];
        grid3x3[0] = helmet;
        grid3x3[1] = dye;

        ItemStack result = simulator.tryMatch(player, grid3x3);

        assertNotNull(result);
        assertEquals(Material.LEATHER_HELMET, result.getType());
        assertEquals(1, result.getAmount());
        assertTrue(result.getItemMeta() instanceof LeatherArmorMeta);

        LeatherArmorMeta meta = (LeatherArmorMeta) result.getItemMeta();
        Color c = meta.getColor();
        assertNotNull(c);
        assertNotEquals(defaultColor, c, "Kolor po barwieniu powinien się zmienić");
    }

    @Test
    @DisplayName("4x4 fallback: LEATHER_CHESTPLATE + BLUE_DYE -> colored chestplate")
    void dyeLeatherChestplate4x4Fallback() {
        ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack dye = new ItemStack(Material.BLUE_DYE);

        Color defaultColor = ((LeatherArmorMeta) chest.getItemMeta()).getColor();

        ItemStack[] grid4x4 = new ItemStack[16];
        grid4x4[5] = chest;
        grid4x4[10] = dye;

        ItemStack result = simulator.tryMatch4x4Fallback(player, grid4x4);

        assertNotNull(result);
        assertEquals(Material.LEATHER_CHESTPLATE, result.getType());
        assertEquals(1, result.getAmount());
        assertTrue(result.getItemMeta() instanceof LeatherArmorMeta);

        LeatherArmorMeta meta = (LeatherArmorMeta) result.getItemMeta();
        assertNotEquals(defaultColor, meta.getColor(), "Kolor po barwieniu powinien się zmienić");
    }
}

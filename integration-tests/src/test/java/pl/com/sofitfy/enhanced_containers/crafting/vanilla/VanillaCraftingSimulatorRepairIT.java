package pl.com.sofitfy.enhanced_containers.crafting.vanilla;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VanillaCraftingSimulator - Repair Fallback IT")
class VanillaCraftingSimulatorRepairIT {

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
    @DisplayName("3x3: 2x damaged IRON_PICKAXE -> repaired result")
    void repair3x3_works() {
        int max = Material.IRON_PICKAXE.getMaxDurability();

        ItemStack a = new ItemStack(Material.IRON_PICKAXE);
        Damageable metaA = (Damageable) a.getItemMeta();
        metaA.setDamage((int) Math.floor(max * 0.50));
        a.setItemMeta(metaA);

        ItemStack b = new ItemStack(Material.IRON_PICKAXE);
        Damageable metaB = (Damageable) b.getItemMeta();
        metaB.setDamage((int) Math.floor(max * 0.60));
        b.setItemMeta(metaB);

        ItemStack[] grid3x3 = new ItemStack[9];
        grid3x3[0] = a;
        grid3x3[1] = b;

        ItemStack result = simulator.tryMatch(player, grid3x3);

        assertNotNull(result);
        assertEquals(Material.IRON_PICKAXE, result.getType());
        assertEquals(1, result.getAmount());
        assertTrue(result.getItemMeta() instanceof Damageable);

        int life1 = max - metaA.getDamage();
        int life2 = max - metaB.getDamage();
        int bonus = (int) Math.floor(max * 0.05);
        int expectedLife = Math.min(max, life1 + life2 + bonus);
        int expectedDamage = Math.max(0, max - expectedLife);

        int actualDamage = ((Damageable) result.getItemMeta()).getDamage();
        assertEquals(expectedDamage, actualDamage);
    }

    @Test
    @DisplayName("4x4 fallback: 2x damaged IRON_PICKAXE anywhere -> repaired result")
    void repair4x4Fallback_works() {
        ItemStack a = new ItemStack(Material.IRON_PICKAXE);
        Damageable metaA = (Damageable) a.getItemMeta();
        metaA.setDamage(50);
        a.setItemMeta(metaA);

        ItemStack b = new ItemStack(Material.IRON_PICKAXE);
        Damageable metaB = (Damageable) b.getItemMeta();
        metaB.setDamage(80);
        b.setItemMeta(metaB);

        ItemStack[] grid4x4 = new ItemStack[16];
        grid4x4[5] = a;
        grid4x4[10] = b;

        ItemStack result = simulator.tryMatch4x4Fallback(player, grid4x4);

        assertNotNull(result);
        assertEquals(Material.IRON_PICKAXE, result.getType());
        assertEquals(1, result.getAmount());
    }
}

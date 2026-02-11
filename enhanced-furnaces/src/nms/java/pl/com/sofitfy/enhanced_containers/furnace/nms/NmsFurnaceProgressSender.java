package pl.com.sofitfy.enhanced_containers.furnace.nms;

import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

/**
 * NmsFurnaceProgressSender - wysyła window properties do klienta przez NMS
 * 
 * Vanilla furnace używa ContainerData z 4 properties:
 * - DATA_LIT_TIME (0): pozostały czas palenia (burnTime)
 * - DATA_LIT_DURATION (1): maksymalny czas palenia (maxBurnTime)
 * - DATA_COOKING_PROGRESS (2): postęp gotowania (cookTime)
 * - DATA_COOKING_TOTAL_TIME (3): całkowity czas gotowania (maxCookTime)
 * 
 * Problem: Te properties działają tylko dla MenuType.FURNACE.
 * Dla custom chest inventory klient ignoruje te pakiety.
 * 
 * Alternatywne rozwiązanie: Użyj CustomModelData na itemach.
 */
public class NmsFurnaceProgressSender {
    
    // Vanilla furnace data slot IDs
    public static final int DATA_LIT_TIME = 0;
    public static final int DATA_LIT_DURATION = 1;
    public static final int DATA_COOKING_PROGRESS = 2;
    public static final int DATA_COOKING_TOTAL_TIME = 3;
    
    /**
     * Próbuje wysłać window property do gracza.
     * UWAGA: To NIE działa dla chest inventory - klient ignoruje te pakiety
     * dla MenuType innych niż FURNACE.
     * 
     * @param player gracz
     * @param propertyId ID property (0-3)
     * @param value wartość
     * @return true jeśli pakiet został wysłany (ale może nie działać!)
     */
    public static boolean sendWindowProperty(Player player, int propertyId, int value) {
        try {
            ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
            int containerId = serverPlayer.containerMenu.containerId;
            
            // Wyślij pakiet window property
            ClientboundContainerSetDataPacket packet = new ClientboundContainerSetDataPacket(
                containerId,
                propertyId,
                value
            );
            
            serverPlayer.connection.send(packet);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Wysyła wszystkie 4 properties pieca.
     * UWAGA: To NIE działa dla chest inventory!
     */
    public static void sendAllFurnaceData(Player player, int litTime, int litDuration, 
                                          int cookingProgress, int cookingTotalTime) {
        sendWindowProperty(player, DATA_LIT_TIME, litTime);
        sendWindowProperty(player, DATA_LIT_DURATION, litDuration);
        sendWindowProperty(player, DATA_COOKING_PROGRESS, cookingProgress);
        sendWindowProperty(player, DATA_COOKING_TOTAL_TIME, cookingTotalTime);
    }
    
    /**
     * Sprawdza czy gracz ma otwarte custom furnace GUI.
     * 
     * @param player gracz
     * @param expectedTitle oczekiwany tytuł GUI
     * @return true jeśli gracz ma otwarte odpowiednie GUI
     */
    public static boolean hasOpenFurnaceGUI(Player player, String expectedTitle) {
        var view = player.getOpenInventory();
        if (view == null) return false;
        
        String title = view.getTitle();
        return title != null && title.contains(expectedTitle);
    }
}

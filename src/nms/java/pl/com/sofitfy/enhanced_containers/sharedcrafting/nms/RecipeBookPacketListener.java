package pl.com.sofitfy.enhanced_containers.sharedcrafting.nms;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener do rejestracji packet handlerów dla graczy.
 * Rejestruje handler przy dołączeniu gracza i wyrejestrowuje przy wyjściu.
 */
public class RecipeBookPacketListener implements Listener {

    private final RecipeBookPacketHandler packetHandler;
    
    public RecipeBookPacketListener(RecipeBookPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Rejestruj z małym opóźnieniem, żeby dać czas na pełne połączenie
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            packetHandler.getPlugin(), 
            () -> packetHandler.registerPlayer(event.getPlayer()),
            5L // 5 ticków opóźnienia
        );
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        packetHandler.unregisterPlayer(event.getPlayer());
    }
}

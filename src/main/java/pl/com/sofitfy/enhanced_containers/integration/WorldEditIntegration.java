package pl.com.sofitfy.enhanced_containers.integration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * WorldEditIntegration - integracja z WorldEdit/FastAsyncWorldEdit
 * 
 * Obsługuje pobieranie selekcji gracza z WorldEdit.
 * Jeśli WorldEdit nie jest załadowany, metody zwracają odpowiednie informacje.
 * 
 * Używa refleksji żeby nie wymagać WorldEdit jako hard dependency.
 */
public class WorldEditIntegration {
    
    // Cache czy WorldEdit jest dostępny (sprawdzane raz przy pierwszym użyciu)
    private static Boolean worldEditAvailable = null;
    
    /**
     * Sprawdza czy WorldEdit lub FastAsyncWorldEdit jest załadowany
     * 
     * @return true jeśli WE/FAWE jest dostępny
     */
    public static boolean isWorldEditAvailable() {
        if (worldEditAvailable != null) {
            return worldEditAvailable;
        }
        
        // Sprawdź czy plugin WorldEdit lub FastAsyncWorldEdit jest załadowany
        Plugin wePlugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
        Plugin fawePlugin = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
        
        worldEditAvailable = (wePlugin != null && wePlugin.isEnabled()) ||
                            (fawePlugin != null && fawePlugin.isEnabled());
        
        return worldEditAvailable;
    }
    
    /**
     * Pobiera selekcję gracza z WorldEdit
     * 
     * @param player gracz którego selekcję pobieramy
     * @return SelectionResult z informacją o powodzeniu i listą lokalizacji
     */
    public static SelectionResult getSelection(Player player) {
        if (!isWorldEditAvailable()) {
            return new SelectionResult(false, "WorldEdit is not installed!", new ArrayList<>());
        }
        
        try {
            // Używamy refleksji żeby uniknąć hard dependency na WorldEdit
            // WorldEdit API: WorldEdit.getInstance().getSessionManager().get(player).getSelection(world)
            
            Class<?> worldEditClass = Class.forName("com.sk89q.worldedit.WorldEdit");
            Object worldEditInstance = worldEditClass.getMethod("getInstance").invoke(null);
            
            // Pobierz SessionManager
            Object sessionManager = worldEditClass.getMethod("getSessionManager").invoke(worldEditInstance);
            
            // Pobierz BukkitAdapter
            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object wePlayer = bukkitAdapterClass.getMethod("adapt", Player.class).invoke(null, player);
            
            // Pobierz LocalSession dla gracza
            // SessionManager.get() przyjmuje SessionOwner, a BukkitPlayer implementuje SessionOwner
            Class<?> localSessionClass = Class.forName("com.sk89q.worldedit.LocalSession");
            Class<?> sessionOwnerClass = Class.forName("com.sk89q.worldedit.session.SessionOwner");
            Object localSession = sessionManager.getClass().getMethod("get", sessionOwnerClass).invoke(sessionManager, wePlayer);
            
            // Pobierz świat gracza jako WorldEdit World
            Object weWorld = bukkitAdapterClass.getMethod("adapt", World.class).invoke(null, player.getWorld());
            
            // Pobierz selekcję
            Object selection = localSessionClass.getMethod("getSelection", 
                Class.forName("com.sk89q.worldedit.world.World")).invoke(localSession, weWorld);
            
            if (selection == null) {
                return new SelectionResult(false, "No selection! Use //pos1 and //pos2 first.", new ArrayList<>());
            }
            
            // Pobierz min i max punkty selekcji
            Class<?> regionClass = Class.forName("com.sk89q.worldedit.regions.Region");
            Object minPoint = regionClass.getMethod("getMinimumPoint").invoke(selection);
            Object maxPoint = regionClass.getMethod("getMaximumPoint").invoke(selection);
            
            // BlockVector3 -> int coordinates
            // Obsługujemy zarówno nowe API (x(), y(), z()) jak i stare (getX(), getY(), getZ())
            Class<?> blockVectorClass = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            
            int minX, minY, minZ, maxX, maxY, maxZ;
            try {
                // Nowe API (WorldEdit 7.x) - metody x(), y(), z()
                minX = (int) blockVectorClass.getMethod("x").invoke(minPoint);
                minY = (int) blockVectorClass.getMethod("y").invoke(minPoint);
                minZ = (int) blockVectorClass.getMethod("z").invoke(minPoint);
                maxX = (int) blockVectorClass.getMethod("x").invoke(maxPoint);
                maxY = (int) blockVectorClass.getMethod("y").invoke(maxPoint);
                maxZ = (int) blockVectorClass.getMethod("z").invoke(maxPoint);
            } catch (NoSuchMethodException e) {
                // Stare API (WorldEdit 6.x) - metody getX(), getY(), getZ()
                minX = (int) blockVectorClass.getMethod("getX").invoke(minPoint);
                minY = (int) blockVectorClass.getMethod("getY").invoke(minPoint);
                minZ = (int) blockVectorClass.getMethod("getZ").invoke(minPoint);
                maxX = (int) blockVectorClass.getMethod("getX").invoke(maxPoint);
                maxY = (int) blockVectorClass.getMethod("getY").invoke(maxPoint);
                maxZ = (int) blockVectorClass.getMethod("getZ").invoke(maxPoint);
            }
            
            // Stwórz listę wszystkich lokalizacji w selekcji
            List<Location> locations = new ArrayList<>();
            World world = player.getWorld();
            
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        locations.add(new Location(world, x, y, z));
                    }
                }
            }
            
            // Oblicz rozmiar selekcji
            long volume = (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
            
            return new SelectionResult(true, "Selection: " + volume + " blocks", locations);
            
        } catch (ClassNotFoundException e) {
            // WorldEdit klasy nie znalezione - plugin załadowany ale API niedostępne
            return new SelectionResult(false, "WorldEdit API not found!", new ArrayList<>());
        } catch (Exception e) {
            // Inny błąd - loguj szczegóły do konsoli
            e.printStackTrace();
            return new SelectionResult(false, "Error getting selection: " + e.getClass().getSimpleName() + " - " + e.getMessage(), new ArrayList<>());
        }
    }
    
    /**
     * Iteruje przez selekcję WorldEdit asynchronicznie w partiach (batch)
     * Pozwala na przetworzenie 100k+ bloków bez zamrażania serwera
     * 
     * @param player gracz z selekcją
     * @param batchSize ile bloków przetwarzać na tick (np. 1000)
     * @param processor funkcja do przetworzenia każdej lokalizacji
     * @param onComplete callback po zakończeniu (totalProcessed)
     * @param plugin instancja pluginu do schedulera
     */
    public static void iterateSelectionAsync(
            Player player,
            int batchSize,
            BiConsumer<Location, BatchProgress> processor,
            java.util.function.Consumer<Integer> onComplete,
            Plugin plugin) {
        
        SelectionResult selection = getSelection(player);
        
        if (!selection.success()) {
            player.sendMessage("§c" + selection.message());
            onComplete.accept(0);
            return;
        }
        
        List<Location> locations = selection.locations();
        int total = locations.size();
        
        if (total == 0) {
            player.sendMessage("§eSelection is empty!");
            onComplete.accept(0);
            return;
        }
        
        // Progress tracking
        final int[] currentIndex = {0};
        final int[] processed = {0};
        
        // Scheduler task - przetwarza partiami
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            int endIndex = Math.min(currentIndex[0] + batchSize, total);
            
            for (int i = currentIndex[0]; i < endIndex; i++) {
                Location loc = locations.get(i);
                BatchProgress progress = new BatchProgress(i + 1, total, processed[0]);
                processor.accept(loc, progress);
            }
            
            currentIndex[0] = endIndex;
            
            // Progres co 10%
            int percent = (int) ((currentIndex[0] / (double) total) * 100);
            if (percent % 10 == 0 || currentIndex[0] == total) {
                player.sendMessage("§7Progress: §e" + percent + "% §7(" + currentIndex[0] + "/" + total + ")");
            }
            
            // Zakończenie
            if (currentIndex[0] >= total) {
                task.cancel();
                onComplete.accept(processed[0]);
            }
            
        }, 1L, 1L); // Start po 1 ticku, powtarzaj co tick
    }
    
    /**
     * Resetuje cache dostępności WorldEdit (używane w testach)
     */
    public static void resetCache() {
        worldEditAvailable = null;
    }
    
    /**
     * Wynik pobierania selekcji
     */
    public record SelectionResult(boolean success, String message, List<Location> locations) {}
    
    /**
     * Informacje o postępie przetwarzania
     */
    public record BatchProgress(int current, int total, int processedCount) {
        public void incrementProcessed() {
            // Ten record jest immutable, więc trzeba użyć zewnętrznego countera
        }
    }
}

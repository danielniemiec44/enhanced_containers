package pl.com.sofitfy.enhanced_containers.crafting.suggestions;

import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Mostek do NMS podpowiedzi vanilla.
 *
 * Dlaczego refleksja:
 * - Maven nie ma NMS/CraftBukkit na classpath,
 * - prawdziwa implementacja siedzi w src/nms/java i jest kompilowana przez Gradle + paperweight.
 */
public class NmsVanillaSuggestionEngine {

    public List<NmsVanillaSuggestion> computeSuggestions(ItemStack[] inventoryContents, World world, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        if (inventoryContents == null || world == null) {
            return List.of();
        }

        try {
            Class<?> directClass = Class.forName(
                "pl.com.sofitfy.enhanced_containers.crafting.suggestions.NmsVanillaSuggestionEngineDirect"
            );

            Object direct = directClass.getDeclaredConstructor().newInstance();
            Method method = directClass.getMethod("computeSuggestions", ItemStack[].class, World.class, int.class);

            @SuppressWarnings("unchecked")
            List<NmsVanillaSuggestion> result = (List<NmsVanillaSuggestion>) method.invoke(direct, inventoryContents, world, limit);

            return result == null ? Collections.emptyList() : result;
        } catch (ClassNotFoundException e) {
            // Budowane Mavenem bez NMS.
            return List.of();
        } catch (Throwable t) {
            // MockBukkit / brak CraftWorld / inne ograniczenia.
            return List.of();
        }
    }
}

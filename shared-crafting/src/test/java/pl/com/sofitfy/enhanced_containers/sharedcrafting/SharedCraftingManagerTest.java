package pl.com.sofitfy.enhanced_containers.sharedcrafting;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SharedCraftingManagerTest {

    @Mock private JavaPlugin plugin;
    @Mock private Inventory inventory;
    @Mock private Location location;  // Mock zamiast instancji
    
    // Mockujemy statyczne metody Bukkit
    private MockedStatic<Bukkit> bukkitMock;

    private SharedCraftingManager manager;

    @BeforeEach
    void setUp() {
        bukkitMock = mockStatic(Bukkit.class);
        // Konfiguracja mocka dla folderu danych
        when(plugin.getDataFolder()).thenReturn(new File("dummy_folder"));
        
        manager = new SharedCraftingManager(plugin);
    }

    @AfterEach
    void tearDown() {
        // Zamykamy static mock po każdym teście
        bukkitMock.close();
    }

    @Test
    void testGetWorkbenchInventory_CreatesNew() {
        // Przygotowanie
        bukkitMock.when(() -> Bukkit.createInventory(any(), eq(InventoryType.WORKBENCH)))
                  .thenReturn(inventory);

        // Wykonanie - pierwsze pobranie
        Inventory result = manager.getWorkbenchInventory(location);

        // Weryfikacja
        assertNotNull(result);
        assertEquals(inventory, result);
        // Sprawdzamy, czy createInventory zostało wywołane raz
        bukkitMock.verify(() -> Bukkit.createInventory(any(), eq(InventoryType.WORKBENCH)), times(1));
    }

    @Test
    void testGetWorkbenchInventory_ReturnsExisting() {
        // Przygotowanie
        bukkitMock.when(() -> Bukkit.createInventory(any(), eq(InventoryType.WORKBENCH)))
                  .thenReturn(inventory);

        // Pierwsze wywołanie (tworzy)
        manager.getWorkbenchInventory(location);
        
        // Wykonanie - drugie pobranie
        Inventory result = manager.getWorkbenchInventory(location);

        // Weryfikacja
        assertEquals(inventory, result);
        // createInventory nadal powinno być wywołane tylko raz!
        bukkitMock.verify(() -> Bukkit.createInventory(any(), eq(InventoryType.WORKBENCH)), times(1));
    }

    @Test
    void testRemoveInventory() {
        // Przygotowanie
        bukkitMock.when(() -> Bukkit.createInventory(any(), eq(InventoryType.WORKBENCH)))
                  .thenReturn(inventory);
        manager.getWorkbenchInventory(location);
        
        assertTrue(manager.hasInventory(location));

        // Wykonanie
        manager.removeInventory(location);

        // Weryfikacja
        assertFalse(manager.hasInventory(location));
    }
}

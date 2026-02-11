package pl.com.sofitfy.enhanced_containers.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * ItemSerializer - serializacja ItemStack do/z Base64
 * 
 * Zachowuje wszystkie dane przedmiotu:
 * - Typ i ilość
 * - Wszystkie metadane (display name, lore, enchanty)
 * - Custom model data
 * - Pełne dane NBT
 * - PersistentDataContainer
 */
public class ItemSerializer {
    
    /**
     * Serializuje ItemStack do Base64 String
     * 
     * @param item przedmiot do serializacji
     * @return Base64 string lub null jeśli item jest null/AIR
     */
    public static String serialize(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            
            dataOutput.writeObject(item);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Deserializuje ItemStack z Base64 String
     * 
     * @param base64 Base64 string
     * @return ItemStack lub null jeśli deserializacja się nie udała
     */
    public static ItemStack deserialize(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
                 BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
                
                return (ItemStack) dataInput.readObject();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Serializuje tablicę ItemStack do tablicy Base64 Strings
     */
    public static String[] serializeArray(ItemStack[] items) {
        if (items == null) return new String[0];
        
        String[] result = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            result[i] = serialize(items[i]);
        }
        return result;
    }
    
    /**
     * Deserializuje tablicę Base64 Strings do tablicy ItemStack
     */
    public static ItemStack[] deserializeArray(String[] base64Array) {
        if (base64Array == null) return new ItemStack[0];
        
        ItemStack[] result = new ItemStack[base64Array.length];
        for (int i = 0; i < base64Array.length; i++) {
            result[i] = deserialize(base64Array[i]);
        }
        return result;
    }
}

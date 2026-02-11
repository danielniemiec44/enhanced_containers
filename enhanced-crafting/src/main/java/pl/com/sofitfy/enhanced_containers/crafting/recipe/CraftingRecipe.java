package pl.com.sofitfy.enhanced_containers.crafting.recipe;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

// Reprezentuje jedną recepturę craftingu
// Przechowuje wzór input, wynik output i permissję
// Input pattern to teraz ItemStack[][] - pełne serializowane przedmioty, nie tylko stringi materiałów
public class CraftingRecipe {
    private final String id;
    private final String name;
    private final ItemStack[][] inputPattern;
    private final ItemStack output;
    private final String permission;

    public CraftingRecipe(String id, String name, ItemStack[][] inputPattern, ItemStack output, String permission) {
        this.id = id;
        this.name = name;
        this.inputPattern = inputPattern;
        this.output = output;
        this.permission = permission;
    }

    // Zwróć ID receptury (używane do identyfikacji)
    public String getId() {
        return id;
    }

    // Zwróć nazwę receptury (co widzi gracz)
    public String getName() {
        return name;
    }

    // Zwróć wzór receptury (2D array ItemStacków - pełne serializowane przedmioty)
    public ItemStack[][] getInputPattern() {
        return inputPattern;
    }

    // Zwróć item wynikowy - zawsze clone (aby nie modyfikować oryginału)
    public ItemStack getOutput() {
        return output.clone();
    }

    // Zwróć permissję wymaganą do craftingu tej receptury
    public String getPermission() {
        return permission;
    }

    // Zwróć RecipePattern - auto-detect bounding box dla custom receptury
    // To umożliwia wyszukiwanie 2x2 receptury nawet jeśli zapisana jako 4x4 z AIR
    public RecipePattern getRecipePattern() {
        if (inputPattern == null) {
            return new RecipePattern(new ItemStack[0][0]);
        }
        return new RecipePattern(inputPattern);
    }

    // Stwórz recepturę z danych z config.yml
    // Zwraca null jeśli coś jest nie tak (brakuje pola, zły format itd)
    public static CraftingRecipe fromConfig(String id, Object recipeData) {
        // Sprawdzenie czy ID jest prawidłowy
        if (id == null || id.isEmpty()) {
            return null;
        }

        // Sprawdzenie czy to mapa (słownik)
        if (!(recipeData instanceof java.util.Map)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> recipe = (java.util.Map<String, Object>) recipeData;

        // Pobierz nazwę receptury (jeśli nie ma, użyj ID)
        Object nameObj = recipe.get("name");
        String name = nameObj instanceof String ? (String) nameObj : id;

        // Pobierz permissję (jeśli nie ma, użyj podstawowej plethoras.tools.use)
        Object permObj = recipe.get("permission");
        String permission = permObj instanceof String ? (String) permObj : "plethoras.tools.use";

        // Parsuj wzór input - lista rzędów przedmiotów (serializowane ItemStacki)
        Object inputObj = recipe.get("input");
        if (!(inputObj instanceof List)) {
            return null;
        }

        List<?> inputList = (List<?>) inputObj;
        if (inputList == null || inputList.isEmpty()) {
            return null;
        }

        // Konwertuj każdy rząd - deserializuj ItemStacki z YAML (Bukkit format)
        ItemStack[][] inputPattern = new ItemStack[inputList.size()][];
        for (int i = 0; i < inputList.size(); i++) {
            Object row = inputList.get(i);

            if (row instanceof List) {
                // Rząd to lista serializowanych ItemStacków z YAMLa
                @SuppressWarnings("unchecked")
                List<?> rowList = (List<?>) row;
                inputPattern[i] = new ItemStack[rowList.size()];

                for (int j = 0; j < rowList.size(); j++) {
                    Object item = rowList.get(j);

                    if (item == null) {
                        // null reprezentuje pusty slot
                        inputPattern[i][j] = null;
                    } else if (item instanceof ItemStack) {
                        // Bezpośrednio deserializowany ItemStack z YAMLa (Bukkit format)
                        inputPattern[i][j] = (ItemStack) item;
                    } else if (item instanceof java.util.Map) {
                        // Jeśli to mapa, spróbuj deserializować jako ItemStack
                        // To rozpatrywać fallback dla custom formatu
                        @SuppressWarnings("unchecked")
                        ItemStack itemStack = deserializeItemStack((java.util.Map<String, Object>) item);
                        inputPattern[i][j] = itemStack;
                    } else {
                        // Nieznany format - pomijamy
                        inputPattern[i][j] = null;
                    }
                }
            } else {
                // Jeśli nie lista, pusta tablica
                inputPattern[i] = new ItemStack[0];
            }
        }

        // Parsuj output (wynik receptury) - pełny serializowany ItemStack
        Object outputObj = recipe.get("output");
        if (outputObj == null) {
            return null;
        }

        ItemStack output = null;

        // Output może być ItemStack (deserializowany z YAML) lub mapą (custom format)
        if (outputObj instanceof ItemStack) {
            // Bezpośrednio deserializowany ItemStack z YAMLa (Bukkit format)
            output = (ItemStack) outputObj;
        } else if (outputObj instanceof java.util.Map) {
            // Fallback dla custom formatu - wciąż obsługujemy stary format
            // Ale teraz preferujemy pełny ItemStack
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> outputMap = (java.util.Map<String, Object>) outputObj;

            // Sprawdź czy to stary format (material + amount + meta)
            Object materialObj = outputMap.get("material");
            if (materialObj instanceof String && !((String) materialObj).isEmpty()) {
                // Stary format - konwertuj do ItemStack
                String materialStr = (String) materialObj;
                Material material = Material.matchMaterial(materialStr);
                if (material == null) {
                    return null;
                }

                Object amountObj = outputMap.get("amount");
                int amount = amountObj instanceof Integer ? (Integer) amountObj : 1;
                if (amount <= 0) amount = 1;

                output = new ItemStack(material, amount);
                ItemMeta meta = output.getItemMeta();

                if (meta != null) {
                    Object customName = outputMap.get("custom-name");
                    if (customName instanceof String && !((String) customName).isEmpty()) {
                        meta.displayName(Component.text((String) customName));
                    }

                    Object loreObj = outputMap.get("lore");
                    if (loreObj instanceof List) {
                        List<?> loreList = (List<?>) loreObj;
                        if (!loreList.isEmpty()) {
                            java.util.List<Component> lore = new java.util.ArrayList<>();
                            for (Object loreLine : loreList) {
                                if (loreLine instanceof String) {
                                    lore.add(Component.text((String) loreLine));
                                }
                            }
                            if (!lore.isEmpty()) {
                                meta.lore(lore);
                            }
                        }
                    }
                    output.setItemMeta(meta);
                }
            }
        }

        // Output musi być ustawiony
        if (output == null) {
            return null;
        }

        // Zwróć nową recepturę z wczytanymi danymi
        return new CraftingRecipe(id, name, inputPattern, output, permission);
    }

    // Helper do deserializacji ItemStack z mapy (fallback dla custom formatu)
    private static ItemStack deserializeItemStack(java.util.Map<String, Object> itemData) {
        if (itemData == null || itemData.isEmpty()) {
            return null;
        }

        // Pobierz material (wymagany)
        Object materialObj = itemData.get("material");
        if (!(materialObj instanceof String)) {
            return null;
        }

        Material material = Material.matchMaterial((String) materialObj);
        if (material == null) {
            return null;
        }

        // Pobierz ilość (opcjonalnie)
        Object amountObj = itemData.get("amount");
        int amount = amountObj instanceof Integer ? (Integer) amountObj : 1;
        if (amount <= 0) amount = 1;

        // Stwórz ItemStack
        ItemStack itemStack = new ItemStack(material, amount);
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            // Custom name
            Object displayName = itemData.get("display-name");
            if (displayName instanceof String && !((String) displayName).isEmpty()) {
                meta.displayName(Component.text((String) displayName));
            }

            // Lore
            Object loreObj = itemData.get("lore");
            if (loreObj instanceof List) {
                List<?> loreList = (List<?>) loreObj;
                java.util.List<Component> lore = new java.util.ArrayList<>();
                for (Object loreLine : loreList) {
                    if (loreLine instanceof String) {
                        lore.add(Component.text((String) loreLine));
                    }
                }
                if (!lore.isEmpty()) {
                    meta.lore(lore);
                }
            }

            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }
}

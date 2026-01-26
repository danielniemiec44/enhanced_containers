package pl.com.sofitfy.enhanced_containers.config;

import java.util.List;
import java.util.Map;

public record CraftingConfig(
    String title,
    int size,
    List<Map<String, Object>> recipes
) {}

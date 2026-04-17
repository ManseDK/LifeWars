package com.vitaxses.lifesteal;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CraftingRecipes {

    public static final String HEART_ID = "heart";
    public static final String REVIVE_BOOK_ID = "revivebook";
    public static final String CUSTOM_HEART_TOKEN = "CUSTOM_HEART";
    public static final String LEGACY_CUSTOM_HEART_TOKEN = "LIFEWARS_DEFAULTHEART";

    private static final String[] HEART_DEFAULT_GRID = {
            "DIAMOND_BLOCK", "GOLD_BLOCK", "DIAMOND_BLOCK",
            "NETHERITE_INGOT", "TOTEM_OF_UNDYING", "NETHERITE_INGOT",
            "DIAMOND_BLOCK", "GOLD_BLOCK", "DIAMOND_BLOCK"
    };

    private static final String[] REVIVE_DEFAULT_GRID = {
            "DIAMOND_BLOCK", CUSTOM_HEART_TOKEN, "GOLD_BLOCK",
            CUSTOM_HEART_TOKEN, "NETHER_STAR", CUSTOM_HEART_TOKEN,
            "GOLD_BLOCK", CUSTOM_HEART_TOKEN, "DIAMOND_BLOCK"
    };

    private final LifeWars main;
    private final NamespacedKey reviveRecipeKey;
    private final NamespacedKey heartRecipeKey;

    public CraftingRecipes(LifeWars main) {
        this.main = main;
        this.reviveRecipeKey = new NamespacedKey(main, "revive_recipe");
        this.heartRecipeKey = new NamespacedKey(main, "heart_recipe");
    }

    public void registerRecipe() {

        main.getServer().removeRecipe(reviveRecipeKey);
        main.getServer().removeRecipe(heartRecipeKey);

        if (!main.getBoolean("recipes.customRecipesEnabled", "CustomRecipes", true)) {
            return;
        }

        if (main.getBoolean("recipes.reviveBookRecipeEnabled", "ReviveRecipe", true)) {
            String[] reviveGrid = getRecipeGrid(REVIVE_BOOK_ID);
            registerShapedRecipe(reviveRecipeKey, main.createReviveBook(), reviveGrid);
        }
        if (main.getBoolean("recipes.heartRecipeEnabled", "HeartRecipe", true)) {
            String[] heartGrid = getRecipeGrid(HEART_ID);
            registerShapedRecipe(heartRecipeKey, main.createHeartItem(1), heartGrid);
        }
    }

    public String[] getDefaultGrid(String recipeId) {
        return switch (recipeId.toLowerCase()) {
            case HEART_ID -> HEART_DEFAULT_GRID.clone();
            case REVIVE_BOOK_ID -> REVIVE_DEFAULT_GRID.clone();
            default -> new String[9];
        };
    }

    public String[] getRecipeGrid(String recipeId) {
        String path = "recipes.layouts." + recipeId + ".grid";
        List<String> fromConfig = main.getConfig().getStringList(path);
        if (fromConfig.size() != 9) {
            String[] fallback = getDefaultGrid(recipeId);
            saveRecipeGrid(recipeId, fallback);
            main.saveConfig();
            return fallback;
        }

        String[] grid = new String[9];
        for (int i = 0; i < 9; i++) {
            String token = normalizeRecipeToken(fromConfig.get(i));
            grid[i] = token == null ? "AIR" : token;
        }
        return grid;
    }

    public void saveRecipeGrid(String recipeId, String[] grid) {
        List<String> stored = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            String value = i < grid.length ? normalizeRecipeToken(grid[i]) : null;
            stored.add(value == null ? "AIR" : value);
        }
        main.getConfig().set("recipes.layouts." + recipeId + ".grid", stored);
        main.saveConfig();
    }

    private void registerShapedRecipe(NamespacedKey key, ItemStack result, String[] grid) {
        char[] symbols = new char[9];
        Map<String, Character> tokenToChar = new LinkedHashMap<>();
        char next = 'A';

        for (int i = 0; i < 9; i++) {
            String token = i < grid.length ? normalizeRecipeToken(grid[i]) : null;
            if (token == null || token.equals("AIR")) {
                symbols[i] = ' ';
                continue;
            }

            if (!tokenToChar.containsKey(token)) {
                if (next > 'I') {
                    main.getLogger().warning("Recipe has too many different ingredients: " + key.getKey());
                    return;
                }
                tokenToChar.put(token, next++);
            }
            symbols[i] = tokenToChar.get(token);
        }

        if (tokenToChar.isEmpty()) {
            main.getLogger().warning("Skipped empty recipe layout: " + key.getKey());
            return;
        }

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                new String(new char[]{symbols[0], symbols[1], symbols[2]}),
                new String(new char[]{symbols[3], symbols[4], symbols[5]}),
                new String(new char[]{symbols[6], symbols[7], symbols[8]})
        );

        for (Map.Entry<String, Character> entry : tokenToChar.entrySet()) {
            RecipeChoice choice = toChoice(entry.getKey());
            if (choice == null) {
                main.getLogger().warning("Unknown ingredient token '" + entry.getKey() + "' for recipe " + key.getKey());
                return;
            }
            recipe.setIngredient(entry.getValue(), choice);
        }

        if (!main.getServer().addRecipe(recipe)) {
            main.getLogger().warning("Could not register recipe: " + key.getKey());
        }
    }

    private RecipeChoice toChoice(String token) {
        if (isCustomHeartToken(token)) {
            return new RecipeChoice.ExactChoice(main.createHeartItem(1));
        }

        Material material = resolveMaterialToken(token);
        if (material == null || material == Material.AIR) {
            return null;
        }
        return new RecipeChoice.MaterialChoice(material);
    }

    public boolean isCustomHeartToken(String token) {
        String normalized = sanitizeToken(token);
        return CUSTOM_HEART_TOKEN.equals(normalized) || LEGACY_CUSTOM_HEART_TOKEN.equals(normalized);
    }

    public Material resolveMaterialToken(String token) {
        if (token == null) {
            return null;
        }

        String normalized = token.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        String stripped = normalized;
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < normalized.length()) {
            stripped = normalized.substring(namespaceSeparator + 1);
        }

        String upperToken = stripped.toUpperCase(Locale.ROOT);
        for (Material material : Material.values()) {
            if (material == Material.AIR || material.isLegacy()) {
                continue;
            }
            if (material.name().equals(upperToken)) {
                return material;
            }
            if (material.getKey().toString().equalsIgnoreCase(normalized)) {
                return material;
            }
        }
        return null;
    }

    private String sanitizeToken(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;

    }

    private String normalizeRecipeToken(String value) {
        String token = sanitizeToken(value);
        if (token == null) {
            return null;
        }
        if (LEGACY_CUSTOM_HEART_TOKEN.equals(token)) {
            return CUSTOM_HEART_TOKEN;
        }
        return token;
    }
}
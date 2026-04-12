package com.vitaxses.lifesteal;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

public class CraftingRecipes {

    private final LifeWars main;

    public CraftingRecipes(LifeWars main) {
        this.main = main;
    }

    public void registerRecipe() {

        if (main.getBoolean("recipes.customRecipesEnabled", "CustomRecipes", true)) {

            if (main.getBoolean("recipes.reviveBookRecipeEnabled", "ReviveRecipe", true)) {

                NamespacedKey recipeKey2 = new NamespacedKey(main, "revive_recipe");
                ItemStack reviveBook = main.createReviveBook();

                ShapedRecipe recipe2 = new ShapedRecipe(recipeKey2, reviveBook);
                recipe2.shape("DHG", "HNH", "GHD");

                recipe2.setIngredient('H', new RecipeChoice.ExactChoice(main.createHeartItem(1)));
                recipe2.setIngredient('N', Material.NETHER_STAR);
                recipe2.setIngredient('D', Material.DIAMOND_BLOCK);
                recipe2.setIngredient('G', Material.GOLD_BLOCK);

                main.getServer().addRecipe(recipe2);
            }
            if (main.getBoolean("recipes.heartRecipeEnabled", "HeartRecipe", true)) {
                //heart recipe
                NamespacedKey recipeKey4 = new NamespacedKey(main, "Heart_recipe");

                ItemStack heartresult = main.createHeartItem(1);

                ShapedRecipe recipe4 = new ShapedRecipe(recipeKey4, heartresult);
                recipe4.shape("#G#", "NTN", "#G#");
                recipe4.setIngredient('#', Material.DIAMOND_BLOCK);
                recipe4.setIngredient('G', Material.GOLD_BLOCK);
                recipe4.setIngredient('N', Material.NETHERITE_INGOT);
                recipe4.setIngredient('T', Material.TOTEM_OF_UNDYING);

                main.getServer().addRecipe(recipe4);
            }

        }

    }
}
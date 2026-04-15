package com.vitaxses.lifesteal.commands;

import com.vitaxses.lifesteal.LifeWars;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class RLConfig implements CommandExecutor {
    private final LifeWars main;

    public RLConfig(LifeWars main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        main.reloadConfig();
        if (main.getCraftingRecipes() != null) {
            main.getCraftingRecipes().registerRecipe();
        }
        commandSender.sendMessage(main.getPrefixedMessageComponent("reloadMsg"));

        return true;
    }
}

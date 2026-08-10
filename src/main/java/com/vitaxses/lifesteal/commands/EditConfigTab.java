package com.vitaxses.lifesteal.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EditConfigTab implements TabCompleter {

    private static final List<String> CONFIG_KEYS = Arrays.asList(
            "features.reviveBookEnabled",
            "features.heartUseEnabled",
            "features.withdrawCommandEnabled",
            "messages.announceAdminRevive",
            "recipes.customRecipesEnabled",
            "recipes.heartRecipeEnabled",
            "recipes.reviveBookRecipeEnabled"
    );

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                                @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], CONFIG_KEYS, new ArrayList<>());
        }
        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], Arrays.asList("true", "false"), new ArrayList<>());
        }
        return null;
    }
}


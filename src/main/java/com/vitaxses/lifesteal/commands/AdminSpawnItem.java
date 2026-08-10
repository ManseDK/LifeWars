package com.vitaxses.lifesteal.commands;

import com.vitaxses.lifesteal.LifeWars;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminSpawnItem implements CommandExecutor, TabCompleter {

    private static final List<String> ITEMS = Arrays.asList("heart", "revivebook");

    private final LifeWars main;

    public AdminSpawnItem(LifeWars main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(main.getPrefixedMessageComponent("needToBePlayer"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(main.formatPrefixedMessageComponent(
                    "usageError", "%usage%", "/adminspawnitem <heart|revivebook>"));
            return true;
        }

        ItemStack item = switch (args[0].toLowerCase()) {
            case "heart" -> main.createHeartItem(1);
            case "revivebook" -> main.createReviveBook();
            default -> null;
        };

        if (item == null) {
            player.sendMessage(main.formatPrefixedMessageComponent(
                    "usageError", "%usage%", "/adminspawnitem <heart|revivebook>"));
            return true;
        }

        player.getInventory().addItem(item);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], ITEMS, new ArrayList<>());
        }
        return List.of();
    }
}


package com.vitaxses.lifesteal.commands;

import com.vitaxses.lifesteal.LifeWars;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Eliminate implements CommandExecutor {

    private final LifeWars main;

    public Eliminate(LifeWars main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage(main.formatPrefixedMessageComponent("usageError", "%usage%", "/eliminate <player>"));
            return true;
        }

        String playerName = args[0];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sender.sendMessage(main.getPrefixedMessageComponent("playerNotFound"));
            return true;
        }

        main.banAndKickPlayer(target);
        sender.sendMessage(main.formatPrefixedMessageComponent("eliminateSuccess", "%player%", playerName));
        return true;
    }
}

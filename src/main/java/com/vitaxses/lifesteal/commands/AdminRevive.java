package com.vitaxses.lifesteal.commands;

import com.vitaxses.lifesteal.LifeWars;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class AdminRevive implements CommandExecutor {

    private final LifeWars main;

    public AdminRevive(LifeWars main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) {
            sender.sendMessage(main.formatPrefixedMessageComponent("usageError", "%usage%", "/adminrevive <player>"));
            return true;
        }

        String playerName = args[0];

        if (!main.getBannedPlayers(true).contains(playerName.toLowerCase(Locale.ROOT))) {
            sender.sendMessage(main.getPrefixedMessageComponent("onlyReviveElimPlayers"));
            return true;
        }

        main.unbanPlayer(playerName);

        if (main.getBoolean("messages.announceAdminRevive", null, true)) {
            Bukkit.broadcast(main.formatPrefixedMessageComponent("reviveSuccess", "%player%", playerName));
        } else {
            sender.sendMessage(main.formatPrefixedMessageComponent("reviveSuccess", "%player%", playerName));
        }

        return true;
    }
}

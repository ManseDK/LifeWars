package com.vitaxses.lifesteal.commands;

import com.vitaxses.lifesteal.LifeWars;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

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
        Player onlineTarget = Bukkit.getPlayer(playerName);

        if (onlineTarget != null) {
            main.banAndKickPlayer(onlineTarget);
            sender.sendMessage(main.formatPrefixedMessageComponent("eliminateSuccess", "%player%", onlineTarget.getName()));
            return true;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(playerName);
        String resolvedName = offlineTarget.getName();

        if (!offlineTarget.hasPlayedBefore() || resolvedName == null) {
            sender.sendMessage(main.getPrefixedMessageComponent("playerNotFound"));
            return true;
        }

        if (main.getBannedPlayers(true).contains(resolvedName.toLowerCase(Locale.ROOT))) {
            sender.sendMessage(main.formatPrefixedMessageComponent("eliminateSuccess", "%player%", resolvedName));
            return true;
        }

        main.writeToBannedPlayers(resolvedName);
        sender.sendMessage(main.formatPrefixedMessageComponent("eliminateSuccess", "%player%", resolvedName));
        return true;
    }
}

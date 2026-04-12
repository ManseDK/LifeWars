package com.vitaxses.lifesteal.commands;

import com.vitaxses.lifesteal.LifeWars;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class AdminRevive implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        LifeWars main = LifeWars.getInstance();
        if (args.length == 1) {
            String playerName = args[0];
            if (main.getBannedPlayers(true).contains(playerName.toLowerCase(Locale.ROOT))) {
                unbanPlayer(playerName);
                Bukkit.broadcast(main.formatPrefixedMessageComponent("reviveSuccess", "%player%", playerName));
            } else {
                sender.sendMessage(main.getPrefixedMessageComponent("onlyReviveElimPlayers"));
            }
            return true;
        }
        sender.sendMessage(main.formatPrefixedMessageComponent("usageError", "%usage%", "/adminrevive <player>"));
        return true;
    }


    public void unbanPlayer(String playerName) {
        LifeWars.getInstance().RemoveBannedPlayer(playerName, true);
        String command = "pardon " + playerName;
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
    }

}



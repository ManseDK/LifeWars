package com.vitaxses.lifesteal.commands;

import com.vitaxses.lifesteal.LifeWars;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.bukkit.profile.PlayerProfile;

import java.time.Instant;

public class Eliminate implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        LifeWars main = LifeWars.getInstance();

        if (args.length < 1) {
            sender.sendMessage(main.formatPrefixedMessage("usageError", "%usage%", "/eliminate <player>"));
            return true;
        }

        String playerName = args[0];
        Player player = Bukkit.getPlayer(playerName);

        if (player != null) {
            @SuppressWarnings("unchecked")
            BanList<PlayerProfile> profileBanList = (BanList<PlayerProfile>) Bukkit.getBanList(BanList.Type.PROFILE);
            profileBanList.addBan(player.getPlayerProfile(), main.plainMessage("eliminatedJoin"), (Instant) null, "LifeWars");
            player.kick(main.getMessageComponent("eliminatedJoin"));
            main.WriteToBannedPlayers(playerName);
            sender.sendMessage(main.formatPrefixedMessageComponent("eliminateSuccess", "%player%", playerName));
            return true;
        } else {
            sender.sendMessage(main.getPrefixedMessageComponent("playerNotFound"));
            return true;
        }
    }
}

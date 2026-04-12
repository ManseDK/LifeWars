package com.vitaxses.lifesteal.commands;

import com.vitaxses.lifesteal.LifeWars;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetHealth implements CommandExecutor {


    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        LifeWars main = LifeWars.getInstance();

        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage(main.getPrefixedMessageComponent("needToBePlayer"));
            return true;
        }

        if (strings.length != 2) {
            player.sendMessage(main.formatPrefixedMessageComponent("usageError", "%usage%", "/sethealth <player> <value>"));
            return true;
        }

        String targetPlayerName = strings[0];
        double healthValue;

        try {
            healthValue = Double.parseDouble(strings[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(main.formatPrefixedMessageComponent("usageError", "%usage%", "/sethealth <player> <value>"));
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

        if (targetPlayer == null) {
            player.sendMessage(main.getPrefixedMessageComponent("playerNotFound"));
            return true;
        }

        AttributeInstance targetHealthAttribute = targetPlayer.getAttribute(Attribute.MAX_HEALTH);
        if (targetHealthAttribute == null) {
            player.sendMessage(main.getPrefixedMessageComponent("playerNotFound"));
            return true;
        }

        targetHealthAttribute.setBaseValue(healthValue);
        player.sendMessage(main.formatPrefixedMessageComponent("setHeartsConfirmSingle", "%player%", targetPlayer.getName(), "%amount%", String.valueOf(healthValue / 2)));
        return true;
    }
}
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

    private final LifeWars main;

    public SetHealth(LifeWars main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(main.getPrefixedMessageComponent("needToBePlayer"));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(main.formatPrefixedMessageComponent("usageError", "%usage%", "/sethealth <player> <value>"));
            return true;
        }

        double healthValue;
        try {
            healthValue = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(main.formatPrefixedMessageComponent("usageError", "%usage%", "/sethealth <player> <value>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(main.getPrefixedMessageComponent("playerNotFound"));
            return true;
        }

        AttributeInstance healthAttribute = target.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttribute == null) {
            player.sendMessage(main.getPrefixedMessageComponent("playerNotFound"));
            return true;
        }

        healthAttribute.setBaseValue(healthValue);
        player.sendMessage(main.formatPrefixedMessageComponent(
                "setHeartsConfirmSingle",
                "%player%", target.getName(),
                "%amount%", String.valueOf(healthValue / 2)
        ));
        return true;
    }
}
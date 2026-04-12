package com.vitaxses.lifesteal.commands;

import com.vitaxses.lifesteal.LifeWars;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class Withdraw implements CommandExecutor {

    private final LifeWars main;

    public Withdraw(LifeWars main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!main.getBoolean("features.withdrawCommandEnabled", "WithdrawCm", true)) {
            commandSender.sendMessage(main.getPrefixedMessageComponent("disabledInConfigMsg"));
            return true;
        }

        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage(main.getPrefixedMessageComponent("needToBePlayer"));
            return true;
        }

        int number = 1;
        if (strings.length > 0) {
            if (!StringUtils.isNumeric(strings[0])) {
                player.sendMessage(main.formatPrefixedMessageComponent("usageError", "%usage%", "/withdraw <amount>"));
                return true;
            }
            number = Integer.parseInt(strings[0]);
        }

        if (number < 1) {
            player.sendMessage(main.getPrefixedMessageComponent("withdrawMin"));
            return true;
        }

        int amount = number * 2;
        int heartItem = number;

        giveItem(player, amount, heartItem);
        return true;
    }

    private void giveItem(Player player, int amount, int heartItem) {
        AttributeInstance healthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttribute == null) {
            return;
        }
        double originalHealth = healthAttribute.getBaseValue();
        double newHealth = originalHealth - amount;

        if (newHealth > 1) {
            healthAttribute.setBaseValue(newHealth);

            ItemStack heart = main.createHeartItem(heartItem);

            player.getInventory().addItem(heart);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.5f, 1.5f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1.5f, 1.5f);
            player.sendMessage(main.formatPrefixedMessageComponent("successWithdraw", "%amount%", String.valueOf(heartItem), "%item%", "Heart(s)"));
        } else {
            player.sendMessage(main.getPrefixedMessageComponent("noWithdraw"));
        }
    }
}

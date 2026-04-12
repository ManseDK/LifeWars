package com.vitaxses.lifesteal.commands;

import com.vitaxses.lifesteal.LifeWars;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class EditConfigCm implements CommandExecutor {
    private final LifeWars plugin;

    public EditConfigCm(LifeWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length != 2) {
            sender.sendMessage(plugin.formatPrefixedMessageComponent("usageError", "%usage%", "/editconfig <configKey> <true|false>"));
            return true;
        }

            String configKey = args[0];
            boolean value;

            if (args[1].equalsIgnoreCase("true")) {
                value = true;
            } else if (args[1].equalsIgnoreCase("false")) {
                value = false;
            } else {
                sender.sendMessage(plugin.formatPrefixedMessageComponent("usageError", "%usage%", "/editconfig <configKey> <true|false>"));
                return true;
            }

            if (!plugin.getConfig().contains(configKey)) {
                sender.sendMessage(plugin.formatPrefixedMessageComponent("configKeyNotFound", "%key%", configKey));
                return true;
            }

            plugin.getConfig().set(configKey, value);
            plugin.saveConfig();
            plugin.reloadConfig();
            sender.sendMessage(plugin.formatPrefixedMessageComponent("configUpdated", "%key%", configKey, "%value%", String.valueOf(value)));
            return true;
        }
}

package com.vitaxses.lifesteal;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

public class RevivePlayers implements Listener {

    private final LifeWars main;

    public RevivePlayers(LifeWars main) {
        this.main = main;
    }

    @EventHandler
    public void onInteractRevive(PlayerInteractEvent event) {
        if (!main.getBoolean("features.reviveBookEnabled", "Revive", true)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;

        Player player = event.getPlayer();
        ItemStack heldItem = event.getItem();

        if (heldItem == null || heldItem.getType() != Material.ENCHANTED_BOOK || !heldItem.hasItemMeta()) {
            return;
        }

        List<Component> expectedLore = main.createReviveBook().getItemMeta().lore();
        List<Component> actualLore = heldItem.getItemMeta().lore();
        if (actualLore == null || expectedLore == null || !actualLore.equals(expectedLore)) {
            player.sendActionBar(main.getMessageComponent("recipeNotFound"));
            return;
        }

        Component display = heldItem.getItemMeta().displayName();
        if (display == null) return;

        String targetName = main.toPlainText(display).trim();
        if (targetName.isEmpty() || targetName.equalsIgnoreCase(main.getReviveBookTemplateNamePlain())) {
            player.sendMessage(main.formatPrefixedMessageComponent(
                    "usageError", "%usage%", "Rename the Revive Book to the target player's name"));
            return;
        }

        if (!main.getBannedPlayers(true).contains(targetName.toLowerCase(Locale.ROOT))) {
            return;
        }

        revivePlayer(player, targetName);
        consumeOneBook(player);
    }

    private void revivePlayer(Player player, String targetName) {
        main.unbanPlayer(targetName);
        Bukkit.broadcast(main.formatPrefixedMessageComponent("reviveSuccess", "%player%", targetName));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    private void consumeOneBook(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }
    }

}
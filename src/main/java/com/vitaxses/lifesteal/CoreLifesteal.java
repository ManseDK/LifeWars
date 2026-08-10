package com.vitaxses.lifesteal;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public class CoreLifesteal implements Listener {

    private final LifeWars main;

    public CoreLifesteal(LifeWars main) {
        this.main = main;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        String name = event.getPlayer().getName().toLowerCase(Locale.ROOT);
        if (main.getBannedPlayers(true).contains(name)) {
            event.disallow(
                    PlayerLoginEvent.Result.KICK_BANNED,
                    main.getMessageComponent("eliminatedJoin")
            );
        }
    }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null) {
            handleKillerHealthGain(killer);
        }

        AttributeInstance victimHealth = victim.getAttribute(Attribute.MAX_HEALTH);
        if (victimHealth == null) return;

        double newHealth = victimHealth.getBaseValue() - 2.0;

        if (newHealth <= 1) {
            handleElimination(victim);
            return;
        }

        victimHealth.setBaseValue(newHealth);
    }

    private void handleKillerHealthGain(Player killer) {
        AttributeInstance killerHealth = killer.getAttribute(Attribute.MAX_HEALTH);
        if (killerHealth == null) return;

        int maxHealthCap = main.getInt("gameplay.maxHealth", "MaxHealthPoints", 40);
        double newHealth = killerHealth.getBaseValue() + 2.0;

        if (newHealth > maxHealthCap) {
            newHealth = maxHealthCap;
            if (main.getBoolean("gameplay.dropHeartsIfMax", null, true)) {
                dropHeartAt(killer);
            } else {
                giveHeart(killer);
            }
        }

        killerHealth.setBaseValue(newHealth);
    }

    private void handleElimination(Player player) {
        int reviveHealth = main.getInt("gameplay.reviveHealth", "ReviveHealth", 10);
        player.getInventory().clear();

        AttributeInstance health = player.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(reviveHealth);
        }

        main.banAndKickPlayer(player);
    }

    private void giveHeart(Player player) {
        player.getInventory().addItem(main.createHeartItem(1));
    }

    private void dropHeartAt(Player player) {
        player.getWorld().dropItemNaturally(player.getLocation(), main.createHeartItem(1));
    }

    private void setPlayerMaxHealth(Player player, double health) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(health);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onHeartEquip(PlayerInteractEvent event) {
        if (!main.getBoolean("features.heartUseEnabled", "Use/EquipHearts", true)) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (action == Action.RIGHT_CLICK_BLOCK) {
            Block clicked = event.getClickedBlock();
            if (clicked != null && clicked.getType().isInteractable()) {
                return;
            }
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        AttributeInstance healthAttribute = player.getAttribute(Attribute.MAX_HEALTH);

        if (item.getType() != Material.NETHER_STAR || !main.isHeartItem(item) || healthAttribute == null) {
            return;
        }

        int maxHealthCap = main.getInt("gameplay.maxHealth", "MaxHealthPoints", 40);
        double newHealth = healthAttribute.getBaseValue() + 2.0;

        if (newHealth > maxHealthCap) {
            player.sendMessage(main.formatPrefixedMessageComponent(
                    "maxHeartLimitReached", "%limit%", String.valueOf(maxHealthCap / 2)));
            healthAttribute.setBaseValue(maxHealthCap);
            return;
        }

        healthAttribute.setBaseValue(newHealth);
        item.setAmount(item.getAmount() - 1);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.5f, 0.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.5f, 1.5f);
    }
}

package com.vitaxses.lifesteal;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.profile.PlayerProfile;

import java.time.Instant;

public class CoreLifesteal implements Listener {

    private final LifeWars main;

    public CoreLifesteal(LifeWars main) {
        this.main = main;
    }

    boolean shouldMinus = true;

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();

        if (killer != null && killer.getAttribute(Attribute.MAX_HEALTH) != null) {
            double originalHealthKiller = killer.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            double newHealthKiller = originalHealthKiller + 2.0;

            int maxHealthCap = main.getInt("gameplay.maxHealth", "MaxHealthPoints", 40);
            if (newHealthKiller > maxHealthCap) {
                newHealthKiller = maxHealthCap;
                if (main.getBoolean("gameplay.dropHeartsIfMax", null, true)) {
                    dropHeartAtPlayer(killer);
                } else {
                    givePlayerHeart(killer);
                }
            }

            setPlayerMaxHealth(killer, newHealthKiller);
        }

        if (player.getAttribute(Attribute.MAX_HEALTH) == null) {
            return;
        }
        double originalHealth = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        double newHealth = originalHealth - 2.0;

        if (newHealth <= 1) {
            newHealth = 0;
            handlePlayerDeath(player);
        }

        setPlayerMaxHealth(player, newHealth);

    }

    private void givePlayerHeart (Player player){
        ItemStack heart = main.createHeartItem(1);
        player.getInventory().addItem(heart);
    }

    private void dropHeartAtPlayer(Player player) {
        ItemStack heart = main.createHeartItem(1);
        player.getWorld().dropItemNaturally(player.getLocation(), heart);
    }

    private void setPlayerMaxHealth(Player player,double health){
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
        }
    }

    private void handlePlayerDeath(Player player){
        int reviveMaxHealth = main.getInt("gameplay.reviveHealth", "ReviveHealth", 10);
        player.getInventory().clear();
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(reviveMaxHealth);
        }
        @SuppressWarnings("unchecked")
        BanList<PlayerProfile> profileBanList = (BanList<PlayerProfile>) Bukkit.getBanList(BanList.Type.PROFILE);
        profileBanList.addBan(player.getPlayerProfile(), main.plainMessage("eliminatedJoin"), (Instant) null, "LifeWars");
        main.WriteToBannedPlayers(player.getName());
        player.kick(main.getMessageComponent("eliminatedJoin"));
    }



    @EventHandler
    public void HeartEquip(PlayerInteractEvent event) {
        shouldMinus = true;
        if (main.getBoolean("features.heartUseEnabled", "Use/EquipHearts", true)) {
            Player player = event.getPlayer();

            Action action = event.getAction();
            if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
                ItemStack item = player.getInventory().getItemInMainHand();
                AttributeInstance healthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
                if (item.getType() == Material.NETHER_STAR && main.isHeartItem(item) && healthAttribute != null) {
                        double killerMaxHealth = healthAttribute.getBaseValue();
                        double newKillerMaxHealth = killerMaxHealth + 2;

                        int maxHealthCap = main.getInt("gameplay.maxHealth", "MaxHealthPoints", 40);


                        if (newKillerMaxHealth > maxHealthCap) {
                            newKillerMaxHealth = maxHealthCap;
                            int maxHealthCapHalf = maxHealthCap / 2;
                            shouldMinus = false;
                            player.sendMessage(main.formatPrefixedMessageComponent("maxHeartLimitReached", "%limit%", String.valueOf(maxHealthCapHalf)));
                        }



                        healthAttribute.setBaseValue(newKillerMaxHealth);
                        if (shouldMinus) {
                            int minusOne = player.getInventory().getItemInMainHand().getAmount();
                            player.getInventory().getItemInMainHand().setAmount(minusOne - 1);
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.5f, 0.5f);
                            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.5f, 1.5f);

                        }
                }
            }
        }
    }
}

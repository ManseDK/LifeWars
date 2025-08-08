package com.vitaxses.lifesteal.banItems;

import com.vitaxses.lifesteal.LifeWars;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class NetheriteMeleeModule implements Listener {

    public boolean isEnabled() {
        return LifeWars.getInstance().getConfig().getBoolean("NetheriteMeleeModule");
    }

    public NetheriteMeleeModule() {
        Bukkit.getPluginManager().registerEvents(this, LifeWars.getInstance());
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player player && isEnabled()) {
            player.getInventory().getItemInMainHand();
            if (isWeapon(player.getInventory().getItemInMainHand().getType())) {
                e.setCancelled(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('§', LifeWars.getInstance().getConfig().getString("NetheriteMeleeIsBannedMsg")));
            }
        }
    }

    public boolean isWeapon(Material t) {
        return t == Material.NETHERITE_SWORD || t == Material.NETHERITE_AXE;
    }
}

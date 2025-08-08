package com.vitaxses.lifesteal.banItems;

import com.vitaxses.lifesteal.LifeWars;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class NetheriteArmorModule implements Listener {

    public boolean isEnabled() {
        return LifeWars.getInstance().getConfig().getBoolean("NetheriteArmorModule");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isEnabled()) return;
        ItemStack item = event.isShiftClick() ? event.getCurrentItem() : event.getCursor();
        if (!isNetheriteArmor(item)) return;
        if (event.getSlotType() == InventoryType.SlotType.ARMOR && !event.isShiftClick()) {
            event.setCancelled(true);
        }
        if (event.isShiftClick() && isNetheriteArmor(item) && event.getSlot() < 36) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isEnabled()) return;
        removeNetheriteArmor(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClickMinimal(InventoryClickEvent event) {
        if (!isEnabled()) return;
        if (event.getWhoClicked() instanceof Player player) {
            removeNetheriteArmor(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isEnabled()) return;

        ItemStack item = event.getItem();
        if (!isNetheriteArmor(item)) return;

        event.setCancelled(true);
    }

    private void removeNetheriteArmor(Player player) {
        PlayerInventory inventory = player.getInventory();

        ArrayList<ItemStack> armor = new ArrayList<>();
        if (inventory.getBoots() != null && inventory.getBoots().getType() == Material.NETHERITE_BOOTS)
            armor.add(inventory.getBoots());
        if (inventory.getLeggings() != null && inventory.getLeggings().getType() == Material.NETHERITE_LEGGINGS)
            armor.add(inventory.getLeggings());
        if (inventory.getChestplate() != null && inventory.getChestplate().getType() == Material.NETHERITE_CHESTPLATE)
            armor.add(inventory.getChestplate());
        if (inventory.getHelmet() != null && inventory.getHelmet().getType() == Material.NETHERITE_HELMET)
            armor.add(inventory.getHelmet());
        if (armor.isEmpty()) return;
        for (ItemStack item : armor) {
            if (item == null) return;
            Item item1 = player.getWorld().dropItem(player.getLocation(), item);
            item1.setCanMobPickup(false);
            item1.setPickupDelay(0);
            item1.setCanPlayerPickup(true);
            item1.setGlowing(true);
            item1.setInvulnerable(true);
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('§', LifeWars.getInstance().getConfig().getString("NetheriteArmorIsBannedMsg")));
    }

    private boolean isNetheriteArmor(@Nullable ItemStack item) {
        if (item == null) return false;
        return switch (item.getType()) {
            case NETHERITE_HELMET, NETHERITE_CHESTPLATE, NETHERITE_LEGGINGS, NETHERITE_BOOTS -> true;
            default -> false;
        };
    }
}
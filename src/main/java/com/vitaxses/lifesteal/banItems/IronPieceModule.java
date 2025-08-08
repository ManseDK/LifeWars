package com.vitaxses.lifesteal.banItems;

import com.vitaxses.lifesteal.LifeWars;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class IronPieceModule implements Listener {

    private boolean isEnabled() {
        return LifeWars.getInstance().getConfig().getBoolean("IronPieceMinModule", false);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isEnabled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack item = event.isShiftClick() ? event.getCurrentItem() : event.getCursor();
        if (!isDiamondArmor(item)) return;

        PlayerInventory inventory = player.getInventory();
        int equippedDiamond = countEquippedDiamondArmor(inventory);

        if (event.getSlotType() == InventoryType.SlotType.ARMOR && !event.isShiftClick() && equippedDiamond == 3) {
            event.setCancelled(true);
        }

        if (event.isShiftClick() && isDiamondArmor(item) && event.getSlot() < 36) {
            if (equippedDiamond == 3) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isEnabled()) return;
        removeRandomDiamondArmorPiece(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClickMinimal(InventoryClickEvent event) {
        if (!isEnabled()) return;
        if (event.getWhoClicked() instanceof Player player) {
            removeRandomDiamondArmorPiece(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isEnabled()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!isDiamondArmor(item)) return;

        if (countEquippedDiamondArmor(player.getInventory()) == 3) {
            event.setCancelled(true);
        }
    }

    private void removeRandomDiamondArmorPiece(Player player) {
        PlayerInventory inventory = player.getInventory();
        if (countEquippedDiamondArmor(inventory) < 4) return;

        Random random = new Random(System.nanoTime());
        int armorSlot = random.nextInt(4); // 0 = boots, 1 = leggings, 2 = chestplate, 3 = helmet

        ItemStack removed = null;
        switch (armorSlot) {
            case 0 -> { removed = inventory.getBoots(); inventory.setBoots(null); }
            case 1 -> { removed = inventory.getLeggings(); inventory.setLeggings(null); }
            case 2 -> { removed = inventory.getChestplate(); inventory.setChestplate(null); }
            case 3 -> { removed = inventory.getHelmet(); inventory.setHelmet(null); }
        }

        if (removed != null && removed.getType() != Material.AIR) {
            player.getWorld().dropItemNaturally(player.getLocation(), removed);
        }
    }

    private int countEquippedDiamondArmor(PlayerInventory inventory) {
        int count = 0;
        if (isDiamondArmor(inventory.getHelmet())) count++;
        if (isDiamondArmor(inventory.getChestplate())) count++;
        if (isDiamondArmor(inventory.getLeggings())) count++;
        if (isDiamondArmor(inventory.getBoots())) count++;
        return count;
    }

    private boolean isDiamondArmor(@Nullable ItemStack item) {
        if (item == null) return false;
        return switch (item.getType()) {
            case DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS -> true;
            default -> false;
        };
    }
}

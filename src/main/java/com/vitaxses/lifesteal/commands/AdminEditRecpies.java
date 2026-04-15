package com.vitaxses.lifesteal.commands;

import com.vitaxses.lifesteal.CraftingRecipes;
import com.vitaxses.lifesteal.LifeWars;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AdminEditRecpies implements CommandExecutor, TabCompleter, Listener {

    private static final String EDITOR_TITLE_PREFIX = "Recipe Editor: ";
    private static final String PICKER_TITLE_PREFIX = "Item Picker: ";
    private static final int[] WORKBENCH_GRID_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final int PICKER_CONTENT_SIZE = 45;
    private static final List<String> OPTIONS = List.of(CraftingRecipes.HEART_ID, CraftingRecipes.REVIVE_BOOK_ID);
    private static final Component EDITOR_HINT_LORE = Component.text(
            "Shift left click to clear, shift right click to choose an item.",
            TextColor.fromHexString("#ADADAD")
    );

    private static final List<Material> PICKABLE_MATERIALS = Arrays.stream(Material.values())
            .filter(Material::isItem)
            .filter(material -> material != Material.AIR)
            .sorted(Comparator.comparing(Material::name))
            .toList();

    private final LifeWars main;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public AdminEditRecpies(LifeWars main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(main.getPrefixedMessageComponent("needToBePlayer"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(main.formatPrefixedMessageComponent(
                    "usageError", "%usage%", "/admineditrecpies <heart|revivebook>"));
            return true;
        }

        String recipeId = args[0].toLowerCase(Locale.ROOT);
        if (!OPTIONS.contains(recipeId)) {
            player.sendMessage(main.formatPrefixedMessageComponent(
                    "usageError", "%usage%", "/admineditrecpies <heart|revivebook>"));
            return true;
        }

        Session session = new Session(recipeId, main.getCraftingRecipes().getRecipeGrid(recipeId));
        sessions.put(player.getUniqueId(), session);
        openEditor(player, session);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], OPTIONS, new ArrayList<>());
        }
        return List.of();
    }

    @EventHandler
    public void onEditorClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof SessionHolder holder)) {
            return;
        }

        if (!holder.recipeId().equals(session.recipeId)) {
            return;
        }

        if (holder.editor()) {
            handleEditorClick(event, player, session);
        } else {
            handlePickerClick(event, player, session);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof SessionHolder holder)) {
            return;
        }

        if (!holder.recipeId().equals(session.recipeId)) {
            return;
        }

        if (holder.editor()) {
            if (session.transitioningInventory) {
                session.transitioningInventory = false;
                return;
            }

            main.getCraftingRecipes().saveRecipeGrid(session.recipeId, session.gridTokens);
            main.getCraftingRecipes().registerRecipe();
            player.sendActionBar(main.getMessageComponent("recipeSavedActionbar"));
            sessions.remove(player.getUniqueId());
            return;
        }

        if (session.transitioningInventory) {
            session.transitioningInventory = false;
            return;
        }

        session.transitioningInventory = true;
        Bukkit.getScheduler().runTask(main, () -> openEditor(player, session));
    }

    private void handleEditorClick(InventoryClickEvent event, Player player, Session session) {
        event.setCancelled(true);

        if (event.getRawSlot() == 0) {
            return;
        }

        int index = toGridIndex(event.getRawSlot());
        if (index < 0) {
            return;
        }

        if (event.getClick() == ClickType.SHIFT_LEFT) {
            session.gridTokens[index] = "AIR";
            setEditorSlot(event.getView().getTopInventory(), index, "AIR");
            return;
        }

        if (event.getClick() == ClickType.SHIFT_RIGHT) {
            session.selectedGridIndex = index;
            session.pickerPage = 0;
            session.transitioningInventory = true;
            openPicker(player, session);
            return;
        }

        // No action-bar fallback hint: instructions are shown directly in item lore.
    }

    private void handlePickerClick(InventoryClickEvent event, Player player, Session session) {
        event.setCancelled(true);

        if (event.getRawSlot() >= PICKER_CONTENT_SIZE && event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot == 45 && session.pickerPage > 0) {
            session.pickerPage--;
            session.transitioningInventory = true;
            openPicker(player, session);
            return;
        }
        if (rawSlot == 49) {
            session.transitioningInventory = true;
            openEditor(player, session);
            return;
        }
        if (rawSlot == 53 && (session.pickerPage + 1) * PICKER_CONTENT_SIZE < PICKABLE_MATERIALS.size()) {
            session.pickerPage++;
            session.transitioningInventory = true;
            openPicker(player, session);
            return;
        }
        if (rawSlot == 47) {
            applyPickerSelection(player, session, CraftingRecipes.CUSTOM_HEART_TOKEN);
            return;
        }

        if (rawSlot < 0 || rawSlot >= PICKER_CONTENT_SIZE) {
            return;
        }

        int materialIndex = session.pickerPage * PICKER_CONTENT_SIZE + rawSlot;
        if (materialIndex < 0 || materialIndex >= PICKABLE_MATERIALS.size()) {
            return;
        }

        applyPickerSelection(player, session, PICKABLE_MATERIALS.get(materialIndex).name());
    }

    private void applyPickerSelection(Player player, Session session, String token) {
        if (session.selectedGridIndex < 0 || session.selectedGridIndex >= 9) {
            return;
        }

        session.gridTokens[session.selectedGridIndex] = token;
        session.transitioningInventory = true;
        openEditor(player, session);
    }

    private void openEditor(Player player, Session session) {
        Inventory editor = Bukkit.createInventory(new SessionHolder(session.recipeId, true), InventoryType.WORKBENCH,
                Component.text(EDITOR_TITLE_PREFIX + session.recipeId));

        for (int i = 0; i < 9; i++) {
            setEditorSlot(editor, i, session.gridTokens[i]);
        }

        player.openInventory(editor);
    }

    private void openPicker(Player player, Session session) {
        Inventory picker = Bukkit.createInventory(new SessionHolder(session.recipeId, false), 54,
                Component.text(PICKER_TITLE_PREFIX + session.recipeId + " (page " + (session.pickerPage + 1) + ")"));

        int start = session.pickerPage * PICKER_CONTENT_SIZE;
        for (int slot = 0; slot < PICKER_CONTENT_SIZE; slot++) {
            int index = start + slot;
            if (index >= PICKABLE_MATERIALS.size()) {
                break;
            }
            picker.setItem(slot, withEditorHintLore(new ItemStack(PICKABLE_MATERIALS.get(index))));
        }

        picker.setItem(45, withEditorHintLore(namedItem(Material.ARROW, "Previous page")));
        picker.setItem(47, withEditorHintLore(namedItem(Material.NETHER_STAR, "Use Custom Heart Item")));
        picker.setItem(49, withEditorHintLore(namedItem(Material.BARRIER, "Back to recipe")));
        picker.setItem(53, withEditorHintLore(namedItem(Material.ARROW, "Next page")));

        player.openInventory(picker);
    }

    private void setEditorSlot(Inventory editor, int gridIndex, String token) {
        int rawSlot = WORKBENCH_GRID_SLOTS[gridIndex];
        ItemStack icon = tokenToIcon(token);
        editor.setItem(rawSlot, icon);
    }

    private ItemStack tokenToIcon(String token) {
        if (token == null || token.equalsIgnoreCase("AIR")) {
            return withEditorHintLore(namedItem(Material.GRAY_STAINED_GLASS_PANE, "Empty"));
        }

        if (CraftingRecipes.CUSTOM_HEART_TOKEN.equalsIgnoreCase(token)) {
            ItemStack heart = main.createHeartItem(1);
            ItemMeta meta = heart.getItemMeta();
            List<Component> lore = new ArrayList<>();
            List<Component> existingLore = meta.lore();
            if (existingLore != null) {
                lore.addAll(existingLore);
            }
            lore.add(Component.text("Used as custom heart ingredient"));
            meta.lore(lore);
            heart.setItemMeta(meta);
            return withEditorHintLore(heart);
        }

        Material material = Material.matchMaterial(token, true);
        if (material == null || material == Material.AIR || !material.isItem()) {
            return withEditorHintLore(namedItem(Material.GRAY_STAINED_GLASS_PANE, "Empty"));
        }
        return withEditorHintLore(new ItemStack(material));
    }

    private ItemStack withEditorHintLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        List<Component> existingLore = meta.lore();
        if (existingLore != null) {
            lore.addAll(existingLore);
        }
        lore.add(EDITOR_HINT_LORE);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack namedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }

    private int toGridIndex(int rawSlot) {
        for (int i = 0; i < WORKBENCH_GRID_SLOTS.length; i++) {
            if (WORKBENCH_GRID_SLOTS[i] == rawSlot) {
                return i;
            }
        }
        return -1;
    }

    private static final class Session {
        private final String recipeId;
        private final String[] gridTokens;
        private boolean transitioningInventory;
        private int selectedGridIndex = -1;
        private int pickerPage;

        private Session(String recipeId, String[] gridTokens) {
            this.recipeId = recipeId;
            this.gridTokens = Arrays.copyOf(gridTokens, 9);
        }
    }

    private record SessionHolder(String recipeId, boolean editor) implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(this, 9);
        }
    }
}


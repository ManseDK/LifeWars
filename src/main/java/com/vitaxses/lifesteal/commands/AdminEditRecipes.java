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
import org.bukkit.inventory.CraftingInventory;
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

public class AdminEditRecipes implements CommandExecutor, TabCompleter, Listener {

    private static final String EDITOR_TITLE_PREFIX = "Recipe Editor -  ";
    private static final String PICKER_TITLE_PREFIX = "Item Picker ";
    private static final int[] WORKBENCH_GRID_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final int PICKER_CONTENT_SIZE = 45;
    private static final List<String> OPTIONS = List.of(CraftingRecipes.HEART_ID, CraftingRecipes.REVIVE_BOOK_ID);
    private static final Component EDITOR_HINT_LORE = Component.text(
            "Left click to choose, shift-right click to clear.",
            TextColor.fromHexString("#ADADAD")
    );
    private static final Component PICKER_HINT_LORE = Component.text(
            "Click to use this item for the selected recipe slot.",
            TextColor.fromHexString("#ADADAD")
    );

    private static final List<Material> PICKABLE_MATERIALS = Arrays.stream(Material.values())
            .filter(Material::isItem)
            .filter(material -> !material.isLegacy())
            .filter(material -> material != Material.AIR)
            .sorted(Comparator.comparing(Material::name))
            .toList();

    private final LifeWars main;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public AdminEditRecipes(LifeWars main) {
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
                    "usageError", "%usage%", "/admineditrecipes <heart|revivebook>"));
            return true;
        }

        String recipeId = args[0].toLowerCase(Locale.ROOT);
        if (!OPTIONS.contains(recipeId)) {
            player.sendMessage(main.formatPrefixedMessageComponent(
                    "usageError", "%usage%", "/admineditrecipes <heart|revivebook>"));
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

            saveAndFinishSession(player, session);
            return;
        }

        if (session.transitioningInventory) {
            session.transitioningInventory = false;
            return;
        }

        // Closing picker without selecting means editing is done, so persist changes now.
        saveAndFinishSession(player, session);
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

        if (event.getClick() == ClickType.SHIFT_RIGHT) {
            session.gridTokens[index] = "AIR";
            setEditorSlot(event.getView().getTopInventory(), index, "AIR");
            return;
        }

        if (event.getClick() == ClickType.LEFT) {
            session.selectedGridIndex = index;
            session.pickerPage = 0;
            switchToPicker(player, session);
            return;
        }
    }

    private void handlePickerClick(InventoryClickEvent event, Player player, Session session) {
        event.setCancelled(true);

        if (event.getRawSlot() >= PICKER_CONTENT_SIZE && event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int rawSlot = event.getRawSlot();
        
        if (rawSlot == 0) {
            applyPickerSelection(player, session, CraftingRecipes.CUSTOM_HEART_TOKEN);
            return;
        }
        
        if (rawSlot == 45 && session.pickerPage > 0) {
            session.pickerPage--;
            switchToPicker(player, session);
            return;
        }
        if (rawSlot == 49) {
            switchToEditor(player, session);
            return;
        }
        if (rawSlot == 53 && (session.pickerPage + 1) * PICKER_CONTENT_SIZE < PICKABLE_MATERIALS.size()) {
            session.pickerPage++;
            switchToPicker(player, session);
            return;
        }

        if (rawSlot < 1 || rawSlot >= PICKER_CONTENT_SIZE) {
            return;
        }

        int materialIndex = session.pickerPage * PICKER_CONTENT_SIZE + rawSlot - 1;
        if (materialIndex < 0 || materialIndex >= PICKABLE_MATERIALS.size()) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        applyPickerSelection(player, session, clickedItem.getType().name());
    }

    private void applyPickerSelection(Player player, Session session, String token) {
        if (session.selectedGridIndex < 0 || session.selectedGridIndex >= 9) {
            return;
        }

        session.gridTokens[session.selectedGridIndex] = token;
        switchToEditor(player, session);
    }

    private void openEditor(Player player, Session session) {
        Inventory editor = Bukkit.createInventory(new SessionHolder(session.recipeId, true), InventoryType.WORKBENCH,
                Component.text(EDITOR_TITLE_PREFIX + session.recipeId));

        ItemStack resultItem;
        if (CraftingRecipes.HEART_ID.equals(session.recipeId)) {
            resultItem = main.createHeartItem(1);
        } else {
            resultItem = main.createReviveBook();
        }
        if (editor instanceof CraftingInventory crafting) {
            crafting.setResult(resultItem);
            ItemStack[] matrix = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                matrix[i] = withEditorHintLore(tokenToIcon(session.gridTokens[i]));
            }
            crafting.setMatrix(matrix);
        } else {
            editor.setItem(0, resultItem);
            for (int i = 0; i < 9; i++) {
                setEditorSlot(editor, i, session.gridTokens[i]);
            }
        }

        player.openInventory(editor);
    }

    private void openPicker(Player player, Session session) {
        Inventory picker = Bukkit.createInventory(new SessionHolder(session.recipeId, false), 54,
                Component.text(PICKER_TITLE_PREFIX + " (page " + (session.pickerPage + 1) + ")"));

        // Add custom heart as the first item
        picker.setItem(0, withLore(namedItem(Material.NETHER_STAR, "Use Custom Heart Item"), PICKER_HINT_LORE));

        int start = session.pickerPage * PICKER_CONTENT_SIZE;
        for (int slot = 1; slot < PICKER_CONTENT_SIZE; slot++) {
            int index = start + slot - 1;
            if (index >= PICKABLE_MATERIALS.size()) {
                break;
            }
            picker.setItem(slot, withLore(new ItemStack(PICKABLE_MATERIALS.get(index)), PICKER_HINT_LORE));
        }

        picker.setItem(45, namedItem(Material.ARROW, "Previous page"));
        picker.setItem(49, namedItem(Material.BARRIER, "Back to recipe"));
        picker.setItem(53, namedItem(Material.ARROW, "Next page"));

        player.openInventory(picker);
    }

    private void setEditorSlot(Inventory editor, int gridIndex, String token) {
        ItemStack icon = tokenToIcon(token);
        if (icon != null) {
            icon = withEditorHintLore(icon);
        }
        if (editor instanceof CraftingInventory crafting) {
            ItemStack[] matrix = crafting.getMatrix();
            if (matrix == null || matrix.length < 9) {
                matrix = new ItemStack[9];
            }
            matrix[gridIndex] = icon;
            crafting.setMatrix(matrix);
            return;
        }

        int rawSlot = WORKBENCH_GRID_SLOTS[gridIndex];
        editor.setItem(rawSlot, icon);
    }

    private ItemStack tokenToIcon(String token) {
        if (token == null || token.equalsIgnoreCase("AIR")) {
            return namedItem(Material.GRAY_STAINED_GLASS_PANE, "Empty");
        }

        if (CraftingRecipes.CUSTOM_HEART_TOKEN.equalsIgnoreCase(token)) {
            ItemStack heart = main.createHeartItem(1);
            ItemMeta meta = heart.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>();
                List<Component> existingLore = meta.lore();
                if (existingLore != null) {
                    lore.addAll(existingLore);
                }
                lore.add(Component.text("Used as custom heart ingredient"));
                meta.lore(lore);
                heart.setItemMeta(meta);
            }
            return heart;
        }

        Material material = resolveMaterialToken(token);
        if (material == null || material == Material.AIR) {
            return namedItem(Material.GRAY_STAINED_GLASS_PANE, "Empty");
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(material);
        }
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack withEditorHintLore(ItemStack item) {
        if (item == null) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
        }
        if (meta == null) {
            return item;
        }
        
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

    private ItemStack withLore(ItemStack item, Component loreLine) {
        if (item == null || loreLine == null) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
        }
        if (meta == null) {
            return item;
        }

        List<Component> lore = new ArrayList<>();
        List<Component> existingLore = meta.lore();
        if (existingLore != null) {
            lore.addAll(existingLore);
        }
        lore.add(loreLine);
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

    private void saveAndFinishSession(Player player, Session session) {
        persistSession(player, session);
        sessions.remove(player.getUniqueId());
    }

    private void persistSession(Player player, Session session) {
        if (!sessionDiffersFromOriginal(session)) {
            return;
        }
        main.getCraftingRecipes().saveRecipeGrid(session.recipeId, session.gridTokens);
        main.getCraftingRecipes().registerRecipe();
        player.sendActionBar(main.getMessageComponent("messages.recipeSavedActionbar"));
        session.markCurrentAsOriginal();
    }

    private void switchToEditor(Player player, Session session) {
        session.transitioningInventory = true;
        Bukkit.getScheduler().runTask(main, () -> openEditor(player, session));
    }

    private void switchToPicker(Player player, Session session) {
        session.transitioningInventory = true;
        Bukkit.getScheduler().runTask(main, () -> openPicker(player, session));
    }

    private @Nullable Material resolveMaterialToken(String token) {
        return main.getCraftingRecipes().resolveMaterialToken(token);
    }

    private boolean sessionDiffersFromOriginal(Session session) {
        return !Arrays.equals(session.gridTokens, session.originalGridTokens);
    }

    private static final class Session {
        private final String recipeId;
        private final String[] gridTokens;
        private final String[] originalGridTokens;
        private boolean transitioningInventory;
        private int selectedGridIndex = -1;
        private int pickerPage;

        private Session(String recipeId, String[] gridTokens) {
            this.recipeId = recipeId;
            this.gridTokens = Arrays.copyOf(gridTokens, 9);
            this.originalGridTokens = Arrays.copyOf(gridTokens, 9);
        }

        private void markCurrentAsOriginal() {
            System.arraycopy(gridTokens, 0, originalGridTokens, 0, 9);
        }
    }

    private record SessionHolder(String recipeId, boolean editor) implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(this, 9);
        }
    }
}


package com.vitaxses.lifesteal;

import com.vitaxses.lifesteal.commands.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class LifeWars extends JavaPlugin {

    private static LifeWars instance;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private File bannedPlayersFile;
    private YamlConfiguration bannedPlayersConfig;

    private void loadBannedPlayersFile() {
        bannedPlayersFile = new File(getDataFolder(), "BannedPlayers.yml");
        if (!bannedPlayersFile.exists()) {
            try {
                if (!bannedPlayersFile.createNewFile()) {
                    getLogger().warning("Could not create BannedPlayers.yml");
                }
            } catch (IOException e) {
                getLogger().severe("Error while creating BannedPlayers.yml");
                throw new RuntimeException(e);
            }
        }
        bannedPlayersConfig = YamlConfiguration.loadConfiguration(bannedPlayersFile);
    }

    private void saveBannedPlayersFile() {
        try {
            bannedPlayersConfig.save(bannedPlayersFile);
        } catch (IOException e) {
            getLogger().warning("Error saving BannedPlayers.yml: " + e.getMessage());
        }
    }

    public List<String> getBannedPlayers(boolean toLower) {
        List<String> names = bannedPlayersConfig.getStringList("banned-players");
        if (toLower) {
            return names.stream().map(String::toLowerCase).collect(Collectors.toList());
        }
        return new ArrayList<>(names);
    }

    public void writeToBannedPlayers(String name) {
        List<String> names = bannedPlayersConfig.getStringList("banned-players");
        if (names.stream().noneMatch(n -> n.equalsIgnoreCase(name))) {
            names.add(name);
            bannedPlayersConfig.set("banned-players", names);
            saveBannedPlayersFile();
        }
    }

    public void removeBannedPlayer(String name, boolean ignoreCase) {
        List<String> names = bannedPlayersConfig.getStringList("banned-players");
        boolean removed = ignoreCase
                ? names.removeIf(n -> n.equalsIgnoreCase(name))
                : names.removeIf(n -> n.equals(name));
        if (removed) {
            bannedPlayersConfig.set("banned-players", names);
            saveBannedPlayersFile();
        }
    }

    public void unbanPlayer(String playerName) {
        removeBannedPlayer(playerName, true);
    }

    public void banAndKickPlayer(Player player) {
        writeToBannedPlayers(player.getName());
        player.kick(getMessageComponent("eliminatedJoin"));
    }


    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        instance = this;

        loadBannedPlayersFile();

        requireCommand("reloadconfig").setExecutor(new RLConfig(this));
        requireCommand("withdraw").setExecutor(new Withdraw(this));
        requireCommand("eliminate").setExecutor(new Eliminate(this));
        requireCommand("adminrevive").setExecutor(new AdminRevive(this));
        requireCommand("sethealth").setExecutor(new SetHealth(this));
        requireCommand("editconfig").setExecutor(new EditConfigCm(this));
        requireCommand("editconfig").setTabCompleter(new EditConfigTab());

        AdminSpawnItem adminSpawnItem = new AdminSpawnItem(this);
        requireCommand("adminspawnitem").setExecutor(adminSpawnItem);
        requireCommand("adminspawnitem").setTabCompleter(adminSpawnItem);

        new CraftingRecipes(this).registerRecipe();
        getServer().getPluginManager().registerEvents(new CoreLifesteal(this), this);
        getServer().getPluginManager().registerEvents(new RevivePlayers(this), this);
    }

    public static LifeWars getInstance() {
        return instance;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        getConfig().options().copyDefaults(true);
        if (getConfig().getDefaults() != null) {
            boolean dirty = false;
            for (String key : getConfig().getDefaults().getKeys(true)) {
                if (!getConfig().getDefaults().isConfigurationSection(key) && !getConfig().isSet(key)) {
                    getConfig().set(key, getConfig().getDefaults().get(key));
                    dirty = true;
                }
            }
            if (dirty) saveConfig();
        }
    }

    private org.bukkit.command.PluginCommand requireCommand(String name) {
        org.bukkit.command.PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Missing command in plugin.yml: " + name);
        }
        return command;
    }

    public boolean getBoolean(String modernPath, String legacyPath, boolean defaultValue) {
        if (getConfig().contains(modernPath)) return getConfig().getBoolean(modernPath);
        if (legacyPath != null && getConfig().contains(legacyPath)) return getConfig().getBoolean(legacyPath);
        return defaultValue;
    }

    public int getInt(String modernPath, String legacyPath, int defaultValue) {
        if (getConfig().contains(modernPath)) return getConfig().getInt(modernPath);
        if (legacyPath != null && getConfig().contains(legacyPath)) return getConfig().getInt(legacyPath);
        return defaultValue;
    }

    public String getString(String modernPath, String legacyPath, String defaultValue) {
        if (getConfig().contains(modernPath)) return getConfig().getString(modernPath, defaultValue);
        if (legacyPath != null && getConfig().contains(legacyPath)) return getConfig().getString(legacyPath, defaultValue);
        return defaultValue;
    }


    public Component deserializeText(String raw) {
        if (raw == null) return Component.empty();
        if (raw.contains("<") && raw.contains(">")) {
            try {
                return miniMessage.deserialize(raw);
            } catch (Exception ignored) {
            }
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }

    private String getRawMessage(String key) {
        String modernPath = "messages." + key;
        if (getConfig().contains(modernPath)) return getConfig().getString(modernPath, key);
        return getConfig().getString(key, key);
    }

    public Component getMessageComponent(String key) {
        return deserializeText(getRawMessage(key));
    }

    public String getMessage(String key) {
        return LegacyComponentSerializer.legacyAmpersand().serialize(getMessageComponent(key));
    }

    public Component getPrefixedMessageComponent(String key) {
        Component prefix = deserializeText(getConfig().getString("messages.prefix", ""));
        Component message = getMessageComponent(key);
        if (prefix.equals(Component.empty())) return message;
        return prefix.append(Component.space()).append(message);
    }

    public String getPrefixedMessage(String key) {
        return LegacyComponentSerializer.legacyAmpersand().serialize(getPrefixedMessageComponent(key));
    }

    public String formatMessage(String key, String... replacements) {
        String message = getRawMessage(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return LegacyComponentSerializer.legacyAmpersand().serialize(deserializeText(message));
    }

    public Component formatMessageComponent(String key, String... replacements) {
        String message = getRawMessage(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return deserializeText(message);
    }

    public Component formatPrefixedMessageComponent(String key, String... replacements) {
        Component prefix = deserializeText(getConfig().getString("messages.prefix", ""));
        Component message = formatMessageComponent(key, replacements);
        if (prefix.equals(Component.empty())) return message;
        return prefix.append(Component.space()).append(message);
    }

    public String formatPrefixedMessage(String key, String... replacements) {
        return LegacyComponentSerializer.legacyAmpersand().serialize(formatPrefixedMessageComponent(key, replacements));
    }

    public String plainMessage(String key) {
        return PlainTextComponentSerializer.plainText().serialize(getMessageComponent(key));
    }


    public ItemStack createHeartItem(int amount) {
        ItemStack heart = new ItemStack(Material.NETHER_STAR, amount);
        ItemMeta meta = heart.getItemMeta();
        meta.displayName(deserializeText(getString("items.heartName", "HeartName", "<bold>Heart")));
        meta.lore(List.of(deserializeText(getString("items.heartLore", "HeartLore", "<gray>Right click to use heart"))));
        heart.setItemMeta(meta);
        return heart;
    }

    public ItemStack createReviveBook() {
        ItemStack reviveBook = new ItemStack(Material.ENCHANTED_BOOK, 1);
        ItemMeta meta = reviveBook.getItemMeta();
        meta.displayName(deserializeText(getString("items.reviveBookName", "ReviveItemName", "<aqua>Revive Book")));
        meta.lore(List.of(deserializeText(getString("items.reviveBookLore", "ReviveItemLore", "<dark_aqua>Rename this book with the player name to revive"))));
        reviveBook.setItemMeta(meta);
        return reviveBook;
    }

    public boolean isHeartItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.NETHER_STAR || !itemStack.hasItemMeta()) {
            return false;
        }
        Component expectedName = deserializeText(getString("items.heartName", "HeartName", "<bold>Heart"));
        return expectedName.equals(itemStack.getItemMeta().displayName());
    }

    public String getReviveBookTemplateNamePlain() {
        Component template = deserializeText(getString("items.reviveBookName", "ReviveItemName", "<aqua>Revive Book"));
        return PlainTextComponentSerializer.plainText().serialize(template);
    }

    public String toPlainText(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component == null ? Component.empty() : component);
    }
}

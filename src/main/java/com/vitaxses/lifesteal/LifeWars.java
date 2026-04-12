package com.vitaxses.lifesteal;

import com.vitaxses.lifesteal.commands.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class LifeWars extends JavaPlugin {

    private static LifeWars Instance;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private static File bannedPlayers;

    public List<String> getBannedPlayers(boolean toLower) {
        List<String> s = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(bannedPlayers))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (toLower) s.add(line.toLowerCase());
                else s.add(line);
            }
        } catch (IOException e) {
            getLogger().info("Error Reading BannedPlayers.txt");
        }
        return s;
    }

    public void WriteToBannedPlayers(String s) {
        try (FileWriter writer = new FileWriter(bannedPlayers, true)) {
            writer.write(s + System.lineSeparator());
        } catch (IOException e) {
            getLogger().info("Error Writing To BannedPlayers.txt");
        }
    }

    public void RemoveBannedPlayer(String s, boolean toLower) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(bannedPlayers.toURI()));

            if (toLower) {
                if (lines.removeIf(line -> line.equalsIgnoreCase(s))) {
                    Files.write(Paths.get(bannedPlayers.toURI()), lines);
                }
            } else if (lines.removeIf(line -> line.equals(s))) {
                Files.write(Paths.get(bannedPlayers.toURI()), lines);
            }
        } catch (IOException e) {
            getLogger().info("Error While Removing Banned Player From BannedPlayers.txt");
        }
    }

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        Instance = this;

        bannedPlayers = new File(getDataFolder(), "BannedPlayers.txt");
        if (!bannedPlayers.exists()) {
            try {
                if (!bannedPlayers.createNewFile()) {
                    getLogger().warning("Could not create BannedPlayers.txt");
                }
            } catch (IOException e) {
                getLogger().info("Error While Creating BannedPlayers.txt");
                throw new RuntimeException(e);
            }
        }

        requireCommand("reloadconfig").setExecutor(new RLConfig(this));
        requireCommand("withdraw").setExecutor(new Withdraw(this));
        requireCommand("eliminate").setExecutor(new Eliminate());
        requireCommand("adminrevive").setExecutor(new AdminRevive());
        requireCommand("sethealth").setExecutor(new SetHealth());
        requireCommand("editconfig").setExecutor(new EditConfigCm(this));
        requireCommand("editconfig").setTabCompleter(new editconfigTab());

        CraftingRecipes customRecipeHandler = new CraftingRecipes(this);
        customRecipeHandler.registerRecipe();
        getServer().getPluginManager().registerEvents(new CoreLifesteal(this), this);
        getServer().getPluginManager().registerEvents(new RevivePlayers(this), this);

    }

    public static LifeWars getInstance() {
        return Instance;
    }

    private org.bukkit.command.PluginCommand requireCommand(String name) {
        org.bukkit.command.PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Missing command in plugin.yml: " + name);
        }
        return command;
    }

    public boolean getBoolean(String modernPath, String legacyPath, boolean defaultValue) {
        if (getConfig().contains(modernPath)) {
            return getConfig().getBoolean(modernPath);
        }
        if (legacyPath != null && getConfig().contains(legacyPath)) {
            return getConfig().getBoolean(legacyPath);
        }
        return defaultValue;
    }

    public int getInt(String modernPath, String legacyPath, int defaultValue) {
        if (getConfig().contains(modernPath)) {
            return getConfig().getInt(modernPath);
        }
        if (legacyPath != null && getConfig().contains(legacyPath)) {
            return getConfig().getInt(legacyPath);
        }
        return defaultValue;
    }

    public String getString(String modernPath, String legacyPath, String defaultValue) {
        if (getConfig().contains(modernPath)) {
            return getConfig().getString(modernPath, defaultValue);
        }
        if (legacyPath != null && getConfig().contains(legacyPath)) {
            return getConfig().getString(legacyPath, defaultValue);
        }
        return defaultValue;
    }

    public Component deserializeText(String raw) {
        if (raw == null) {
            return Component.empty();
        }
        if (raw.contains("<") && raw.contains(">")) {
            try {
                return miniMessage.deserialize(raw);
            } catch (Exception ignored) {
                // Fallback to legacy parser below
            }
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }

    private String getRawMessage(String key) {
        String modernPath = "messages." + key;
        if (getConfig().contains(modernPath)) {
            return getConfig().getString(modernPath, key);
        }
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
        if (prefix.equals(Component.empty())) {
            return message;
        }
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
        if (prefix.equals(Component.empty())) {
            return message;
        }
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
        meta.setUnbreakable(true);
        reviveBook.setItemMeta(meta);
        return reviveBook;
    }

    public boolean isHeartItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.NETHER_STAR || !itemStack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = itemStack.getItemMeta();
        Component expectedName = deserializeText(getString("items.heartName", "HeartName", "<bold>Heart"));
        return expectedName.equals(meta.displayName());
    }

    public String getReviveBookTemplateNamePlain() {
        Component template = deserializeText(getString("items.reviveBookName", "ReviveItemName", "<aqua>Revive Book"));
        return PlainTextComponentSerializer.plainText().serialize(template);
    }

    public String toPlainText(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component == null ? Component.empty() : component);
    }

}

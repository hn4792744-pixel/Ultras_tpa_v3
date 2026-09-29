package com.ultras.tpa.configuration;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Typed, cached view of config.yml. */
public final class PluginConfig {

    private final JavaPlugin plugin;

    private int requestExpiration;
    private int sendCooldown;
    private int countdown;
    private boolean cancelOnMove;
    private boolean cancelOnQuit;
    private boolean soundsEnabled;
    private boolean actionbarEnabled;
    private boolean autoOpenDefault;
    private boolean infoEnabled;
    private boolean infoWorld;
    private boolean infoLocation;
    private boolean overrideEnabled;
    private String defaultLanguage;
    private List<String> languages;
    private Set<String> disabledWorlds;
    private Set<String> disabledWorldNames;
    private boolean autoAcceptEnabled;
    private boolean autoAcceptTpahere;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration config = plugin.getConfig();
        requestExpiration = Math.max(5, config.getInt("request-expiration", 60));
        sendCooldown = Math.max(0, config.getInt("send-cooldown", 15));
        countdown = Math.max(0, config.getInt("teleport-countdown", 3));
        cancelOnMove = config.getBoolean("cancel-on-move", true);
        cancelOnQuit = config.getBoolean("cancel-on-quit", true);
        soundsEnabled = config.getBoolean("sounds.enabled", true);
        actionbarEnabled = config.getBoolean("actionbar.enabled", true);
        autoOpenDefault = config.getBoolean("auto-open-gui.default", false);
        infoEnabled = config.getBoolean("player-info.enabled", true);
        infoWorld = config.getBoolean("player-info.show-world", true);
        infoLocation = config.getBoolean("player-info.show-location", true);
        overrideEnabled = config.getBoolean("command-override.enabled", true);

        defaultLanguage = config.getString("language.default", "EN").toUpperCase(Locale.ROOT);
        languages = new ArrayList<>();
        for (String code : config.getStringList("language.allowed")) {
            String upper = code.toUpperCase(Locale.ROOT);
            if (!languages.contains(upper)) {
                languages.add(upper);
            }
        }
        if (!languages.contains(defaultLanguage)) {
            languages.add(0, defaultLanguage);
        }

        autoAcceptEnabled = config.getBoolean("auto-accept.enabled", true);
        autoAcceptTpahere = config.getBoolean("auto-accept.allow-tpahere", true);

        disabledWorlds = new HashSet<>();
        disabledWorldNames = new LinkedHashSet<>();
        for (String world : config.getStringList("disabled-worlds")) {
            if (disabledWorlds.add(world.toLowerCase(Locale.ROOT))) {
                disabledWorldNames.add(world);
            }
        }
    }

    public boolean isWorldDisabled(World world) {
        return disabledWorlds.contains(world.getName().toLowerCase(Locale.ROOT));
    }

    /** Names as written in config.yml, in insertion order. */
    public List<String> disabledWorldNames() {
        return List.copyOf(disabledWorldNames);
    }

    public boolean isWorldNameDisabled(String name) {
        return disabledWorlds.contains(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Disables or re-enables teleporting in a world and saves it to config.yml.
     * @return false when nothing changed (already in that state)
     */
    public boolean setWorldDisabled(String name, boolean disabled) {
        String key = name.toLowerCase(Locale.ROOT);
        boolean changed;
        if (disabled) {
            changed = disabledWorlds.add(key);
            if (changed) {
                disabledWorldNames.add(name);
            }
        } else {
            changed = disabledWorlds.remove(key);
            if (changed) {
                disabledWorldNames.removeIf(existing -> existing.equalsIgnoreCase(name));
            }
        }
        if (changed) {
            plugin.getConfig().set("disabled-worlds", new ArrayList<>(disabledWorldNames));
            plugin.saveConfig();
        }
        return changed;
    }

    public boolean autoAcceptEnabled() {
        return autoAcceptEnabled;
    }

    public boolean autoAcceptTpahere() {
        return autoAcceptTpahere;
    }

    public int requestExpiration() {
        return requestExpiration;
    }

    public int sendCooldown() {
        return sendCooldown;
    }

    public int countdown() {
        return countdown;
    }

    public boolean cancelOnMove() {
        return cancelOnMove;
    }

    public boolean cancelOnQuit() {
        return cancelOnQuit;
    }

    public boolean soundsEnabled() {
        return soundsEnabled;
    }

    public boolean actionbarEnabled() {
        return actionbarEnabled;
    }

    public boolean autoOpenDefault() {
        return autoOpenDefault;
    }

    public boolean playerInfoEnabled() {
        return infoEnabled;
    }

    public boolean infoWorld() {
        return infoWorld;
    }

    public boolean infoLocation() {
        return infoLocation;
    }

    public boolean overrideEnabled() {
        return overrideEnabled;
    }

    public String defaultLanguage() {
        return defaultLanguage;
    }

    public List<String> languages() {
        return languages;
    }
}

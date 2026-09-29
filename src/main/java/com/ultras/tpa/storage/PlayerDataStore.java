package com.ultras.tpa.storage;

import com.ultras.tpa.UltrasTpa;
import com.ultras.tpa.models.PlayerSettings;
import com.ultras.tpa.models.Setting;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player preferences stored by UUID in playerdata.yml.
 * Reads come from memory; writes are debounced and the file IO happens off the main thread.
 */
public final class PlayerDataStore {

    private static final long SAVE_DELAY_TICKS = 100L;

    private final UltrasTpa plugin;
    private final File file;
    private final Map<UUID, PlayerSettings> data = new ConcurrentHashMap<>();
    private final Object ioLock = new Object();
    private boolean savePending;

    public PlayerDataStore(UltrasTpa plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playerdata.yml");
    }

    public void load() {
        data.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            UUID id = parseUuid(key);
            ConfigurationSection section = players.getConfigurationSection(key);
            if (id == null || section == null) {
                continue;
            }
            PlayerSettings settings = new PlayerSettings();
            ConfigurationSection values = section.getConfigurationSection("values");
            if (values != null) {
                for (String valueKey : values.getKeys(false)) {
                    Setting setting = Setting.fromKey(valueKey);
                    if (setting != null) {
                        settings.set(setting, values.getBoolean(valueKey));
                    }
                }
            }
            for (String blockedId : section.getStringList("blocked")) {
                UUID blocked = parseUuid(blockedId);
                if (blocked != null) {
                    settings.setBlocked(blocked, true);
                }
            }
            settings.setLanguage(section.getString("language"));
            data.put(id, settings);
        }
    }

    public PlayerSettings get(UUID id) {
        return data.computeIfAbsent(id, key -> new PlayerSettings());
    }

    /** Schedules a save shortly after the last change. */
    public void markDirty() {
        if (!plugin.isEnabled()) {
            saveNow();
            return;
        }
        if (savePending) {
            return;
        }
        savePending = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            savePending = false;
            String snapshot = snapshot();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> write(snapshot));
        }, SAVE_DELAY_TICKS);
    }

    public void saveNow() {
        write(snapshot());
    }

    private String snapshot() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerSettings> entry : data.entrySet()) {
            PlayerSettings settings = entry.getValue();
            if (settings.isDefault()) {
                continue;
            }
            String base = "players." + entry.getKey();
            for (Map.Entry<Setting, Boolean> value : settings.explicitValues().entrySet()) {
                yaml.set(base + ".values." + value.getKey().key(), value.getValue());
            }
            List<String> blocked = new ArrayList<>();
            for (UUID id : settings.blocked()) {
                blocked.add(id.toString());
            }
            if (!blocked.isEmpty()) {
                yaml.set(base + ".blocked", blocked);
            }
            if (settings.language() != null) {
                yaml.set(base + ".language", settings.language());
            }
        }
        return yaml.saveToString();
    }

    private void write(String content) {
        synchronized (ioLock) {
            try {
                Path target = file.toPath();
                Files.createDirectories(target.getParent());
                Path temp = target.resolveSibling("playerdata.yml.tmp");
                Files.writeString(temp, content, StandardCharsets.UTF_8);
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save playerdata.yml: " + e.getMessage());
            }
        }
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

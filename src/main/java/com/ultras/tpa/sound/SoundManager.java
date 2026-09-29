package com.ultras.tpa.sound;

import com.ultras.tpa.UltrasTpa;
import com.ultras.tpa.models.Setting;
import com.ultras.tpa.utils.YamlFiles;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/** Plays the sounds configured in sounds.yml, respecting the global and per-player switches. */
public final class SoundManager {

    private final UltrasTpa plugin;
    private YamlConfiguration sounds = new YamlConfiguration();

    public SoundManager(UltrasTpa plugin) {
        this.plugin = plugin;
    }

    public void load() {
        sounds = YamlFiles.load(plugin, "sounds.yml");
    }

    public void play(Player player, String event) {
        if (!plugin.settings().soundsEnabled()
                || !plugin.store().get(player.getUniqueId()).isEnabled(Setting.SOUNDS, plugin.settings())) {
            return;
        }
        ConfigurationSection section = sounds.getConfigurationSection("sounds." + event);
        if (section == null) {
            return;
        }
        String key = section.getString("sound", "none");
        if (key.isBlank() || key.equalsIgnoreCase("none")) {
            return;
        }
        player.playSound(player.getLocation(), key, SoundCategory.MASTER,
                (float) section.getDouble("volume", 0.6D), (float) section.getDouble("pitch", 1.0D));
    }
}

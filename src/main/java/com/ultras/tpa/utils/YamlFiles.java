package com.ultras.tpa.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class YamlFiles {

    private YamlFiles() {
    }

    /**
     * Loads a YAML file from the plugin folder, copying the bundled version first if needed.
     * Keys missing from the server copy fall back to the bundled defaults.
     */
    public static YamlConfiguration load(JavaPlugin plugin, String path) {
        File file = new File(plugin.getDataFolder(), path);
        boolean bundled = plugin.getResource(path) != null;
        if (!file.exists()) {
            if (!bundled) {
                plugin.getLogger().warning("Missing file " + path + " and no bundled default exists.");
                return new YamlConfiguration();
            }
            plugin.saveResource(path, false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (bundled) {
            try (InputStream in = plugin.getResource(path);
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                yaml.setDefaults(YamlConfiguration.loadConfiguration(reader));
            } catch (IOException | NullPointerException e) {
                plugin.getLogger().warning("Could not read bundled defaults of " + path + ": " + e.getMessage());
            }
        }
        return yaml;
    }
}

package com.ultras.tpa.language;

import com.ultras.tpa.UltrasTpa;
import com.ultras.tpa.utils.Text;
import com.ultras.tpa.utils.YamlFiles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Loads languages/*.yml and resolves messages in each player's chosen language. */
public final class LanguageManager {

    private final UltrasTpa plugin;
    private final Map<String, YamlConfiguration> files = new HashMap<>();
    private List<String> codes = List.of("EN");
    private String fallback = "EN";

    public LanguageManager(UltrasTpa plugin) {
        this.plugin = plugin;
    }

    public void load() {
        files.clear();
        codes = plugin.settings().languages();
        fallback = plugin.settings().defaultLanguage();
        for (String code : codes) {
            files.put(code, YamlFiles.load(plugin, "languages/" + code.toLowerCase(java.util.Locale.ROOT) + ".yml"));
        }
    }

    public String code(CommandSender sender) {
        if (sender instanceof Player player) {
            String chosen = plugin.store().get(player.getUniqueId()).language();
            if (chosen != null && files.containsKey(chosen)) {
                return chosen;
            }
        }
        return fallback;
    }

    public String raw(CommandSender sender, String key) {
        String value = lookup(code(sender), key);
        if (value == null) {
            value = lookup(fallback, key);
        }
        return value == null ? "<red>" + key : value;
    }

    public List<String> rawList(CommandSender sender, String key) {
        YamlConfiguration file = files.get(code(sender));
        if (file == null || !file.contains(key)) {
            file = files.get(fallback);
        }
        return file == null ? List.of() : file.getStringList(key);
    }

    public Component component(CommandSender sender, String key, TagResolver... resolvers) {
        return Text.parse(raw(sender, key), resolvers);
    }

    public void send(CommandSender sender, String key, TagResolver... resolvers) {
        sender.sendMessage(Text.parse(raw(sender, "prefix") + raw(sender, key), resolvers));
    }

    public void actionBar(Player player, String key, TagResolver... resolvers) {
        player.sendActionBar(component(player, key, resolvers));
    }

    /** A clickable chat button whose label comes from the language file. */
    public Component button(Player player, String key, String command) {
        return component(player, key).clickEvent(ClickEvent.runCommand(command));
    }

    public String displayName(CommandSender sender) {
        return raw(sender, "language-name");
    }

    public int languageCount() {
        return codes.size();
    }

    public String nextLanguage(String current) {
        int index = codes.indexOf(current);
        return codes.get((index + 1) % codes.size());
    }

    private String lookup(String code, String key) {
        YamlConfiguration file = files.get(code);
        return file == null ? null : file.getString(key);
    }
}

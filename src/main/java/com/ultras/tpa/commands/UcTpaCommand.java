package com.ultras.tpa.commands;

import com.ultras.tpa.UltrasTpa;
import com.ultras.tpa.configuration.PluginConfig;
import com.ultras.tpa.models.PlayerSettings;
import com.ultras.tpa.models.Setting;
import com.ultras.tpa.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** /uc_tpa block | sounds | requests | settings | auto | world | reload */
public final class UcTpaCommand implements TabExecutor {

    private final UltrasTpa plugin;

    public UcTpaCommand(UltrasTpa plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            reload(sender);
            return true;
        }
        if (sub.equals("world")) {
            world(sender, args);
            return true;
        }
        if (!(sender instanceof Player player)) {
            plugin.lang().send(sender, "players-only");
            return true;
        }
        switch (sub) {
            case "block" -> block(player, args);
            case "sounds" -> toggleSounds(player);
            case "requests" -> requests(player);
            case "settings" -> plugin.gui().openSettings(player);
            case "auto" -> auto(player, args);
            case "help" -> plugin.lang().rawList(player, "help").forEach(line -> player.sendMessage(Text.parse(line)));
            default -> {
                plugin.lang().send(player, "unknown-sub");
                plugin.sounds().play(player, "error");
            }
        }
        return true;
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("ultras.tpa.reload")) {
            plugin.lang().send(sender, "no-permission");
            return;
        }
        plugin.reloadAll();
        plugin.lang().send(sender, "reloaded");
        if (sender instanceof Player player) {
            plugin.sounds().play(player, "accept");
        }
    }

    private void auto(Player player, String[] args) {
        Boolean value = null;
        if (args.length > 1) {
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "on", "enable", "true" -> value = Boolean.TRUE;
                case "off", "disable", "false" -> value = Boolean.FALSE;
                default -> {
                    plugin.lang().send(player, "usage-auto");
                    plugin.sounds().play(player, "error");
                    return;
                }
            }
        }
        plugin.requests().setAutoAccept(player, value);
    }

    /** /uc_tpa world (disable|enable|list) [world]  - works from console too. */
    private void world(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultras.tpa.world")) {
            plugin.lang().send(sender, "no-permission");
            return;
        }
        PluginConfig config = plugin.settings();
        String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";
        if (action.equals("list")) {
            List<String> names = config.disabledWorldNames();
            if (names.isEmpty()) {
                plugin.lang().send(sender, "world-list-empty");
            } else {
                plugin.lang().send(sender, "world-list", Text.worlds(String.join(", ", names)));
            }
            return;
        }
        boolean disable;
        switch (action) {
            case "disable", "off", "deny" -> disable = true;
            case "enable", "on", "allow" -> disable = false;
            default -> {
                plugin.lang().send(sender, "usage-world");
                return;
            }
        }
        String name;
        if (args.length > 2) {
            name = args[2];
        } else if (sender instanceof Player player) {
            name = player.getWorld().getName();
        } else {
            plugin.lang().send(sender, "usage-world");
            return;
        }
        World world = Bukkit.getWorld(name);
        if (world != null) {
            name = world.getName();
        } else if (disable || !config.isWorldNameDisabled(name)) {
            // Only allow typing a world that is not loaded when re-enabling one that is already in the list.
            plugin.lang().send(sender, "world-not-found", Text.world(name));
            return;
        }
        if (!config.setWorldDisabled(name, disable)) {
            plugin.lang().send(sender, disable ? "world-already-disabled" : "world-already-enabled", Text.world(name));
            return;
        }
        plugin.lang().send(sender, disable ? "world-disabled" : "world-enabled", Text.world(name));
        if (sender instanceof Player player) {
            plugin.sounds().play(player, disable ? "toggle-off" : "toggle-on");
        }
    }

    private void block(Player player, String[] args) {
        if (args.length < 2) {
            plugin.lang().send(player, "usage-block");
            plugin.sounds().play(player, "error");
            return;
        }
        UUID targetId;
        String targetName;
        Player online = Bukkit.getPlayerExact(args[1]);
        if (online != null) {
            targetId = online.getUniqueId();
            targetName = online.getName();
        } else {
            OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(args[1]);
            if (cached == null) {
                plugin.lang().send(player, "player-not-found", Text.player(args[1]));
                plugin.sounds().play(player, "error");
                return;
            }
            targetId = cached.getUniqueId();
            targetName = cached.getName() != null ? cached.getName() : args[1];
        }
        if (targetId.equals(player.getUniqueId())) {
            plugin.lang().send(player, "cannot-block-self");
            plugin.sounds().play(player, "error");
            return;
        }
        boolean nowBlocked = !plugin.store().get(player.getUniqueId()).isBlocked(targetId);
        plugin.requests().setBlocked(player, targetId, targetName, nowBlocked);
    }

    private void toggleSounds(Player player) {
        PlayerSettings settings = plugin.store().get(player.getUniqueId());
        boolean enabled = !settings.isEnabled(Setting.SOUNDS, plugin.settings());
        settings.set(Setting.SOUNDS, enabled);
        plugin.store().markDirty();
        plugin.lang().send(player, enabled ? "sounds-on" : "sounds-off");
        plugin.sounds().play(player, "toggle-on");
    }

    private void requests(Player player) {
        if (plugin.requests().pendingFor(player.getUniqueId()).isEmpty()) {
            plugin.lang().send(player, "no-pending");
            plugin.sounds().play(player, "error");
            return;
        }
        plugin.gui().openRequests(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(List.of("block", "sounds", "requests", "settings", "auto"));
            if (sender.hasPermission("ultras.tpa.reload")) {
                options.add("reload");
            }
            if (sender.hasPermission("ultras.tpa.world")) {
                options.add("world");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("auto")) {
            options.addAll(List.of("on", "off"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("world") && sender.hasPermission("ultras.tpa.world")) {
            options.addAll(List.of("disable", "enable", "list"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("world") && sender.hasPermission("ultras.tpa.world")) {
            if (args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("allow")) {
                options.addAll(plugin.settings().disabledWorldNames());
            } else {
                Bukkit.getWorlds().forEach(w -> options.add(w.getName()));
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("block") && sender instanceof Player player) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.getUniqueId().equals(player.getUniqueId()) && player.canSee(online)) {
                    options.add(online.getName());
                }
            }
            for (UUID blocked : plugin.store().get(player.getUniqueId()).blocked()) {
                String name = Bukkit.getOfflinePlayer(blocked).getName();
                if (name != null) {
                    options.add(name);
                }
            }
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}

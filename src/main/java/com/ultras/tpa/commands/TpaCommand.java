package com.ultras.tpa.commands;

import com.ultras.tpa.UltrasTpa;
import com.ultras.tpa.models.TeleportRequest;
import com.ultras.tpa.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Handles /tpa, /tpahere, /tpaccept (/tpaaccept), /tpadeny, /tpacancel, /tpasetting, /tpauto and /uc_setting. */
public final class TpaCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("accept", "deny", "cancel", "setting", "here", "auto");

    private final UltrasTpa plugin;

    public TpaCommand(UltrasTpa plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.lang().send(sender, "players-only");
            return true;
        }
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "tpa" -> tpa(player, args);
            case "tpahere" -> choosePlayer(player, args);
            case "tpaccept", "tpadeny" -> answer(player, args);
            case "tpacancel" -> plugin.requests().cancelLatest(player);
            case "tpauto" -> autoAccept(player, args);
            default -> plugin.gui().openSettings(player);
        }
        return true;
    }

    private void tpa(Player player, String[] args) {
        if (args.length == 0) {
            plugin.gui().openPlayers(player, 0);
            return;
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "accept", "deny" -> answer(player, rest);
            case "cancel" -> plugin.requests().cancelLatest(player);
            case "setting", "settings" -> plugin.gui().openSettings(player);
            case "here" -> choosePlayer(player, rest);
            case "auto" -> autoAccept(player, rest);
            case "open" -> openRequest(player, rest);
            case "cancelid" -> cancelRequest(player, rest);
            default -> choosePlayer(player, args);
        }
    }

    /** /tpauto [on|off]: no argument toggles. */
    private void autoAccept(Player player, String[] args) {
        Boolean value = null;
        if (args.length > 0) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
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

    /** /tpa [player] and /tpahere [player]: both end in the same selection menu. */
    private void choosePlayer(Player player, String[] args) {
        if (!player.hasPermission("ultras.tpa.send")) {
            plugin.lang().send(player, "no-permission");
            plugin.sounds().play(player, "denied-attempt");
            return;
        }
        if (args.length == 0) {
            plugin.gui().openPlayers(player, 0);
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !player.canSee(target)) {
            boolean known = Bukkit.getOfflinePlayerIfCached(args[0]) != null;
            plugin.lang().send(player, known ? "player-offline" : "player-not-found", Text.player(args[0]));
            plugin.sounds().play(player, "error");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            plugin.lang().send(player, "cannot-self");
            plugin.sounds().play(player, "error");
            return;
        }
        plugin.gui().openSelect(player, target);
    }

    /** /tpaccept and /tpadeny (with or without a player name) open menus; the decision is made there. */
    private void answer(Player player, String[] args) {
        if (args.length == 0) {
            if (plugin.requests().pendingFor(player.getUniqueId()).isEmpty()) {
                plugin.lang().send(player, "no-pending");
                plugin.sounds().play(player, "error");
                return;
            }
            plugin.gui().openRequests(player);
            return;
        }
        TeleportRequest request = plugin.requests().findFrom(player.getUniqueId(), args[0]);
        if (request == null) {
            plugin.lang().send(player, "no-request-from", Text.player(args[0]));
            plugin.sounds().play(player, "error");
            return;
        }
        plugin.gui().openRequest(player, request);
    }

    private void openRequest(Player player, String[] args) {
        UUID id = parseId(args);
        TeleportRequest request = id == null ? null : plugin.requests().get(id);
        if (request == null) {
            plugin.lang().send(player, "request-unavailable");
            plugin.sounds().play(player, "error");
            return;
        }
        plugin.gui().openRequest(player, request);
    }

    private void cancelRequest(Player player, String[] args) {
        UUID id = parseId(args);
        if (id == null) {
            plugin.lang().send(player, "cancel-unavailable");
            plugin.sounds().play(player, "error");
            return;
        }
        plugin.requests().cancelById(player, id);
    }

    private static UUID parseId(String[] args) {
        if (args.length == 0) {
            return null;
        }
        try {
            return UUID.fromString(args[0]);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player) || args.length == 0) {
            return List.of();
        }
        List<String> options = new ArrayList<>();
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "tpa" -> {
                if (args.length == 1) {
                    options.addAll(SUBCOMMANDS);
                    options.addAll(playerNames(player));
                } else if (args.length == 2) {
                    switch (args[0].toLowerCase(Locale.ROOT)) {
                        case "accept", "deny" -> options.addAll(plugin.requests().senderNamesFor(player.getUniqueId()));
                        case "here" -> options.addAll(playerNames(player));
                        case "auto" -> options.addAll(List.of("on", "off"));
                        default -> { }
                    }
                }
            }
            case "tpahere" -> {
                if (args.length == 1) {
                    options.addAll(playerNames(player));
                }
            }
            case "tpaccept", "tpadeny" -> {
                if (args.length == 1) {
                    options.addAll(plugin.requests().senderNamesFor(player.getUniqueId()));
                }
            }
            case "tpauto" -> {
                if (args.length == 1) {
                    options.addAll(List.of("on", "off"));
                }
            }
            default -> { }
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static List<String> playerNames(Player viewer) {
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(viewer.getUniqueId()) && viewer.canSee(online)) {
                names.add(online.getName());
            }
        }
        return names;
    }
}

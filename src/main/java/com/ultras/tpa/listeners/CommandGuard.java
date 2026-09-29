package com.ultras.tpa.listeners;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.ultras.tpa.UltrasTpa;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Makes ULTRAS_TPA win command conflicts without disabling any other plugin.
 * 1. After every plugin is enabled, the TPA labels are re-pointed to ULTRAS_TPA in the CommandMap.
 * 2. For labels that another plugin owned, player execution and tab completion are also intercepted,
 *    because Paper's Brigadier tree may still hold the other plugin's node.
 */
public final class CommandGuard implements Listener {

    private static final List<String> PRIMARY = List.of("tpa", "tpahere", "tpaccept", "tpadeny", "tpacancel", "tpasetting", "tpauto");

    private final UltrasTpa plugin;
    private final Map<String, PluginCommand> intercepted = new ConcurrentHashMap<>();

    public CommandGuard(UltrasTpa plugin) {
        this.plugin = plugin;
    }

    /** Safe to call repeatedly (startup and /uc_tpa reload). */
    public void claim() {
        if (!plugin.settings().overrideEnabled()) {
            return;
        }
        Map<String, Command> known = Bukkit.getCommandMap().getKnownCommands();
        List<String> taken = new ArrayList<>();
        for (String primary : PRIMARY) {
            PluginCommand ours = plugin.getCommand(primary);
            if (ours == null) {
                continue;
            }
            List<String> labels = new ArrayList<>(ours.getAliases());
            labels.add(primary);
            for (String label : labels) {
                Command current = known.get(label);
                if (current != null && current != ours) {
                    intercepted.put(label, ours);
                    known.put(label, ours);
                    taken.add(label);
                }
            }
        }
        if (!taken.isEmpty()) {
            plugin.getLogger().info("Took over conflicting commands: " + String.join(", ", taken));
            Bukkit.getOnlinePlayers().forEach(Player::updateCommands);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (intercepted.isEmpty()) {
            return;
        }
        String message = event.getMessage();
        int space = message.indexOf(' ');
        String label = (space < 0 ? message.substring(1) : message.substring(1, space)).toLowerCase(Locale.ROOT);
        PluginCommand ours = intercepted.get(label);
        if (ours == null) {
            return;
        }
        event.setCancelled(true);
        String rest = space < 0 ? "" : message.substring(space + 1).trim();
        ours.execute(event.getPlayer(), label, rest.isEmpty() ? new String[0] : rest.split("\\s+"));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTab(AsyncTabCompleteEvent event) {
        String buffer = event.getBuffer();
        if (intercepted.isEmpty() || !event.isCommand() || !buffer.startsWith("/")) {
            return;
        }
        int space = buffer.indexOf(' ');
        if (space < 0) {
            return;
        }
        String label = buffer.substring(1, space).toLowerCase(Locale.ROOT);
        PluginCommand ours = intercepted.get(label);
        if (ours == null) {
            return;
        }
        List<String> completions = ours.tabComplete(event.getSender(), label, buffer.substring(space + 1).split(" ", -1));
        event.setCompletions(completions);
        event.setHandled(true);
    }
}

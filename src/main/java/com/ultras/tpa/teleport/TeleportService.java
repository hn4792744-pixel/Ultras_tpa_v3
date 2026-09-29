package com.ultras.tpa.teleport;

import com.ultras.tpa.UltrasTpa;
import com.ultras.tpa.models.Setting;
import com.ultras.tpa.utils.Text;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Runs accepted teleports: countdown, movement cancel, safety checks and the final teleport. */
public final class TeleportService {

    private final UltrasTpa plugin;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public TeleportService(UltrasTpa plugin) {
        this.plugin = plugin;
    }

    private static final class Session {
        final UUID travelerId;
        final UUID destinationId;
        final String destinationName;
        int remaining;
        BukkitTask task;

        Session(UUID travelerId, UUID destinationId, String destinationName, int remaining) {
            this.travelerId = travelerId;
            this.destinationId = destinationId;
            this.destinationName = destinationName;
            this.remaining = remaining;
        }
    }

    public boolean isBusy(UUID travelerId) {
        return sessions.containsKey(travelerId);
    }

    public void start(Player traveler, Player destination) {
        Session session = new Session(traveler.getUniqueId(), destination.getUniqueId(),
                destination.getName(), plugin.settings().countdown());
        sessions.put(session.travelerId, session);
        plugin.lang().send(traveler, "teleporting", Text.player(session.destinationName));
        session.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(session), 0L, 20L);
    }

    private void tick(Session session) {
        if (sessions.get(session.travelerId) != session) {
            session.task.cancel();
            return;
        }
        Player traveler = Bukkit.getPlayer(session.travelerId);
        Player destination = Bukkit.getPlayer(session.destinationId);
        if (traveler == null || destination == null) {
            end(session);
            return;
        }
        if (session.remaining <= 0) {
            end(session);
            perform(traveler, destination, session.destinationName);
            return;
        }
        showCountdown(traveler, session);
        session.remaining--;
    }

    private void showCountdown(Player traveler, Session session) {
        TagResolver[] tags = {Text.player(session.destinationName), Text.seconds(session.remaining)};
        boolean actionbar = plugin.settings().actionbarEnabled()
                && plugin.store().get(traveler.getUniqueId()).isEnabled(Setting.ACTIONBAR, plugin.settings());
        if (actionbar) {
            plugin.lang().actionBar(traveler, "countdown-actionbar", tags);
        } else {
            plugin.lang().send(traveler, "countdown-chat", tags);
        }
        plugin.sounds().play(traveler, "countdown");
    }

    private void perform(Player traveler, Player destination, String destinationName) {
        Location target = destination.getLocation();
        World world = target.getWorld();
        boolean valid = world != null
                && Double.isFinite(target.getX()) && Double.isFinite(target.getY()) && Double.isFinite(target.getZ())
                && !plugin.settings().isWorldDisabled(world)
                && !plugin.settings().isWorldDisabled(traveler.getWorld());
        if (!valid) {
            fail(traveler, "not-allowed-here");
            return;
        }
        traveler.teleportAsync(target, PlayerTeleportEvent.TeleportCause.PLUGIN).thenAccept(success ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!traveler.isOnline()) {
                        return;
                    }
                    if (!success) {
                        fail(traveler, "teleport-failed");
                        return;
                    }
                    plugin.lang().send(traveler, "teleport-success", Text.player(destinationName));
                    plugin.lang().actionBar(traveler, "teleport-success-actionbar");
                    plugin.sounds().play(traveler, "teleport-success");
                }));
    }

    private void fail(Player traveler, String messageKey) {
        plugin.lang().send(traveler, messageKey);
        plugin.sounds().play(traveler, "teleport-fail");
    }

    public void handleMove(Player player) {
        if (!plugin.settings().cancelOnMove()) {
            return;
        }
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        end(session);
        plugin.lang().send(player, "teleport-cancelled-moved", Text.player(session.destinationName));
        plugin.sounds().play(player, "teleport-fail");
    }

    public void handleQuit(Player quitter) {
        Session own = sessions.get(quitter.getUniqueId());
        if (own != null) {
            end(own);
        }
        for (Session session : List.copyOf(sessions.values())) {
            if (!session.destinationId.equals(quitter.getUniqueId())) {
                continue;
            }
            end(session);
            Player traveler = Bukkit.getPlayer(session.travelerId);
            if (traveler != null) {
                plugin.lang().send(traveler, "teleport-cancelled-left", Text.player(quitter.getName()));
                plugin.sounds().play(traveler, "teleport-fail");
            }
        }
    }

    public void cancelAll() {
        for (Session session : List.copyOf(sessions.values())) {
            end(session);
        }
    }

    private void end(Session session) {
        sessions.remove(session.travelerId, session);
        if (session.task != null) {
            session.task.cancel();
        }
    }
}

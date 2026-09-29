package com.ultras.tpa.request;

import com.ultras.tpa.UltrasTpa;
import com.ultras.tpa.configuration.PluginConfig;
import com.ultras.tpa.language.LanguageManager;
import com.ultras.tpa.models.PlayerSettings;
import com.ultras.tpa.models.RequestStatus;
import com.ultras.tpa.models.RequestType;
import com.ultras.tpa.models.Setting;
import com.ultras.tpa.models.TeleportRequest;
import com.ultras.tpa.sound.SoundManager;
import com.ultras.tpa.utils.Text;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns every live request. All state changes go through {@link #finish} which uses the request's
 * one-way status transition, so a request can be accepted, denied, cancelled or expired exactly once.
 */
public final class RequestManager {

    private final UltrasTpa plugin;
    private final Map<UUID, TeleportRequest> byId = new ConcurrentHashMap<>();
    private final Map<UUID, TeleportRequest> bySender = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, TeleportRequest>> byTarget = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastSend = new ConcurrentHashMap<>();

    public RequestManager(UltrasTpa plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ sending

    public synchronized boolean send(Player sender, Player target, RequestType type) {
        LanguageManager lang = plugin.lang();
        SoundManager sounds = plugin.sounds();
        PluginConfig config = plugin.settings();

        if (sender.getUniqueId().equals(target.getUniqueId())) {
            return reject(sender, "error", "cannot-send-self");
        }
        if (!sender.isOnline() || !target.isOnline()) {
            return reject(sender, "error", "player-offline", Text.player(target.getName()));
        }
        if (!sender.hasPermission("ultras.tpa.send")) {
            return reject(sender, "denied-attempt", "no-permission");
        }
        if (config.isWorldDisabled(sender.getWorld())) {
            return reject(sender, "denied-attempt", "not-allowed-here", Text.world(sender.getWorld().getName()));
        }
        if (config.isWorldDisabled(target.getWorld())) {
            return reject(sender, "denied-attempt", "target-world-disabled", Text.player(target.getName()),
                    Text.world(target.getWorld().getName()));
        }
        PlayerSettings targetSettings = plugin.store().get(target.getUniqueId());
        if (!target.hasPermission("ultras.tpa.receive")
                || !targetSettings.isEnabled(Setting.RECEIVE_ALL, config)
                || !targetSettings.isEnabled(type.receiveSetting(), config)) {
            return reject(sender, "denied-attempt", "target-disabled-" + type.key(), Text.player(target.getName()));
        }
        if (targetSettings.isBlocked(sender.getUniqueId())) {
            return reject(sender, "denied-attempt", "blocked-by-target", Text.player(target.getName()));
        }
        if (bySender.containsKey(sender.getUniqueId())) {
            return reject(sender, "error", "already-pending");
        }
        long now = System.currentTimeMillis();
        long cooldownLeft = lastSend.getOrDefault(sender.getUniqueId(), 0L) + config.sendCooldown() * 1000L - now;
        if (cooldownLeft > 0 && !sender.hasPermission("ultras.tpa.bypass.cooldown")) {
            return reject(sender, "error", "cooldown", Text.seconds((cooldownLeft + 999L) / 1000L));
        }

        TeleportRequest request = new TeleportRequest(sender, target, type, now, now + config.requestExpiration() * 1000L);
        register(request);
        lastSend.put(sender.getUniqueId(), now);
        request.setExpiryTask(Bukkit.getScheduler().runTaskLater(plugin, () -> expire(request.id()),
                config.requestExpiration() * 20L));

        lang.send(sender, "request-sent-" + type.key(),
                Text.player(target.getName()), Text.seconds(config.requestExpiration()),
                Placeholder.component("cancel", lang.button(sender, "button-cancel", "/tpa cancelid " + request.id())));
        sounds.play(sender, "send-request");

        boolean auto = config.autoAcceptEnabled()
                && (type == RequestType.TPA || config.autoAcceptTpahere())
                && target.hasPermission("ultras.tpa.autoaccept")
                && targetSettings.isEnabled(Setting.AUTO_ACCEPT, config);
        if (auto) {
            lang.send(target, "auto-accepted-" + type.key(), Text.player(sender.getName()));
            accept(target, request);
            if (!request.isPending()) {
                return true;
            }
        }
        // Normal flow, or auto-accept could not complete (for example the traveler is busy).
        lang.send(target, "request-received-" + type.key(),
                Text.player(sender.getName()), Text.seconds(config.requestExpiration()),
                Placeholder.component("open", lang.button(target, "button-open", "/tpa open " + request.id())));
        sounds.play(target, "receive-" + type.key());

        if (targetSettings.isEnabled(Setting.AUTO_OPEN, config)) {
            plugin.gui().openRequest(target, request);
        }
        return true;
    }

    private boolean reject(Player player, String sound, String key, TagResolver... tags) {
        plugin.lang().send(player, key, tags);
        plugin.sounds().play(player, sound);
        return false;
    }

    // ------------------------------------------------------------------ answering

    public synchronized void accept(Player acceptor, TeleportRequest request) {
        if (!isOpenFor(acceptor, request)) {
            return;
        }
        Player sender = Bukkit.getPlayer(request.senderId());
        if (sender == null) {
            finish(request, RequestStatus.CANCELLED);
            reject(acceptor, "error", "player-offline", Text.player(request.senderName()));
            return;
        }
        boolean senderTravels = request.type() == RequestType.TPA;
        Player traveler = senderTravels ? sender : acceptor;
        Player destination = senderTravels ? acceptor : sender;

        if (plugin.teleports().isBusy(traveler.getUniqueId())) {
            reject(acceptor, "error", "already-teleporting");
            return;
        }
        PluginConfig config = plugin.settings();
        if (config.isWorldDisabled(sender.getWorld()) || config.isWorldDisabled(acceptor.getWorld())) {
            reject(acceptor, "denied-attempt", "not-allowed-here", Text.world(acceptor.getWorld().getName()));
            return;
        }
        if (!finish(request, RequestStatus.ACCEPTED)) {
            return;
        }
        plugin.lang().send(sender, "accepted-" + request.type().key(), Text.player(acceptor.getName()));
        plugin.lang().send(acceptor, "you-accepted", Text.player(sender.getName()));
        plugin.sounds().play(sender, "accept");
        plugin.sounds().play(acceptor, "accept");
        plugin.teleports().start(traveler, destination);
    }

    public synchronized void deny(Player denier, TeleportRequest request) {
        if (!isOpenFor(denier, request) || !finish(request, RequestStatus.DENIED)) {
            return;
        }
        plugin.lang().send(denier, "you-denied", Text.player(request.senderName()));
        plugin.sounds().play(denier, "deny");
        notifySenderDenied(request, denier.getName());
    }

    // ------------------------------------------------------------------ cancelling

    public synchronized void cancelLatest(Player sender) {
        TeleportRequest request = bySender.get(sender.getUniqueId());
        if (request == null) {
            plugin.lang().send(sender, "no-pending");
            plugin.sounds().play(sender, "error");
            return;
        }
        cancel(sender, request);
    }

    public synchronized void cancelById(Player sender, UUID requestId) {
        TeleportRequest request = byId.get(requestId);
        if (request == null || !request.senderId().equals(sender.getUniqueId())) {
            reject(sender, "error", "cancel-unavailable");
            return;
        }
        cancel(sender, request);
    }

    private void cancel(Player sender, TeleportRequest request) {
        if (!finish(request, RequestStatus.CANCELLED)) {
            reject(sender, "error", "cancel-unavailable");
            return;
        }
        plugin.lang().send(sender, "request-cancelled-sender", Text.player(request.targetName()));
        plugin.sounds().play(sender, "cancel");
        Player target = Bukkit.getPlayer(request.targetId());
        if (target != null) {
            plugin.lang().send(target, "request-cancelled-target", Text.player(request.senderName()));
            plugin.sounds().play(target, "cancel");
        }
    }

    // ------------------------------------------------------------------ blocking

    public synchronized void setBlocked(Player owner, UUID targetId, String targetName, boolean blocked) {
        PlayerSettings settings = plugin.store().get(owner.getUniqueId());
        if (!settings.setBlocked(targetId, blocked)) {
            return;
        }
        plugin.store().markDirty();
        if (blocked) {
            Map<UUID, TeleportRequest> incoming = byTarget.get(owner.getUniqueId());
            TeleportRequest pending = incoming == null ? null : incoming.get(targetId);
            if (pending != null && finish(pending, RequestStatus.DENIED)) {
                notifySenderDenied(pending, owner.getName());
            }
        }
        plugin.lang().send(owner, blocked ? "player-blocked" : "player-unblocked", Text.player(targetName));
        plugin.sounds().play(owner, blocked ? "block" : "unblock");
    }

    // ------------------------------------------------------------------ auto accept

    /** @param value true/false to set, or null to toggle */
    public void setAutoAccept(Player player, Boolean value) {
        if (!plugin.settings().autoAcceptEnabled()) {
            reject(player, "error", "auto-accept-unavailable");
            return;
        }
        if (!player.hasPermission("ultras.tpa.autoaccept")) {
            reject(player, "denied-attempt", "no-permission");
            return;
        }
        PlayerSettings settings = plugin.store().get(player.getUniqueId());
        boolean enabled = value != null ? value : !settings.isEnabled(Setting.AUTO_ACCEPT, plugin.settings());
        settings.set(Setting.AUTO_ACCEPT, enabled);
        plugin.store().markDirty();
        plugin.lang().send(player, enabled ? "auto-accept-on" : "auto-accept-off");
        plugin.sounds().play(player, enabled ? "toggle-on" : "toggle-off");
    }

    // ------------------------------------------------------------------ lifecycle

    public synchronized void handleQuit(Player quitter) {
        UUID id = quitter.getUniqueId();
        if (plugin.settings().cancelOnQuit()) {
            List<TeleportRequest> affected = new ArrayList<>();
            TeleportRequest own = bySender.get(id);
            if (own != null) {
                affected.add(own);
            }
            Map<UUID, TeleportRequest> incoming = byTarget.get(id);
            if (incoming != null) {
                affected.addAll(incoming.values());
            }
            for (TeleportRequest request : affected) {
                if (!finish(request, RequestStatus.CANCELLED)) {
                    continue;
                }
                UUID otherId = request.senderId().equals(id) ? request.targetId() : request.senderId();
                Player other = Bukkit.getPlayer(otherId);
                if (other != null) {
                    plugin.lang().send(other, "request-left", Text.player(quitter.getName()));
                    plugin.sounds().play(other, "cancel");
                }
            }
        }
        Long last = lastSend.get(id);
        if (last != null && System.currentTimeMillis() - last >= plugin.settings().sendCooldown() * 1000L) {
            lastSend.remove(id);
        }
    }

    public synchronized void shutdown() {
        byId.values().forEach(TeleportRequest::cancelExpiry);
        byId.clear();
        bySender.clear();
        byTarget.clear();
    }

    private synchronized void expire(UUID requestId) {
        TeleportRequest request = byId.get(requestId);
        if (request == null || !finish(request, RequestStatus.EXPIRED)) {
            return;
        }
        Player target = Bukkit.getPlayer(request.targetId());
        if (target != null) {
            plugin.lang().send(target, "request-expired", Text.player(request.senderName()));
            plugin.sounds().play(target, "expired");
        }
        Player sender = Bukkit.getPlayer(request.senderId());
        if (sender != null) {
            plugin.lang().send(sender, "request-timed-out");
            plugin.sounds().play(sender, "expired");
        }
    }

    // ------------------------------------------------------------------ queries

    /** True if the request is still pending and addressed to this player; otherwise tells the player it is gone. */
    public synchronized boolean isOpenFor(Player target, TeleportRequest request) {
        boolean open = request.isPending() && byId.get(request.id()) == request
                && request.targetId().equals(target.getUniqueId());
        if (open && System.currentTimeMillis() >= request.expiresAt()) {
            expire(request.id());
            open = false;
        }
        if (!open) {
            reject(target, "error", "request-unavailable");
        }
        return open;
    }

    public TeleportRequest get(UUID id) {
        return byId.get(id);
    }

    public List<TeleportRequest> pendingFor(UUID target) {
        Map<UUID, TeleportRequest> incoming = byTarget.get(target);
        if (incoming == null) {
            return List.of();
        }
        long now = System.currentTimeMillis();
        return incoming.values().stream()
                .filter(r -> r.isPending() && r.expiresAt() > now)
                .sorted(Comparator.comparingLong(TeleportRequest::createdAt))
                .toList();
    }

    public TeleportRequest findFrom(UUID target, String senderName) {
        for (TeleportRequest request : pendingFor(target)) {
            if (request.senderName().equalsIgnoreCase(senderName)) {
                return request;
            }
        }
        return null;
    }

    public List<String> senderNamesFor(UUID target) {
        return pendingFor(target).stream().map(TeleportRequest::senderName).toList();
    }

    // ------------------------------------------------------------------ internals

    private boolean finish(TeleportRequest request, RequestStatus status) {
        if (!request.transition(status)) {
            return false;
        }
        byId.remove(request.id());
        bySender.remove(request.senderId(), request);
        Map<UUID, TeleportRequest> incoming = byTarget.get(request.targetId());
        if (incoming != null) {
            incoming.remove(request.senderId(), request);
            if (incoming.isEmpty()) {
                byTarget.remove(request.targetId(), incoming);
            }
        }
        request.cancelExpiry();
        plugin.gui().closeRequestViews(request);
        return true;
    }

    private void register(TeleportRequest request) {
        byId.put(request.id(), request);
        bySender.put(request.senderId(), request);
        byTarget.computeIfAbsent(request.targetId(), key -> new ConcurrentHashMap<>()).put(request.senderId(), request);
    }

    private void notifySenderDenied(TeleportRequest request, String denierName) {
        Player sender = Bukkit.getPlayer(request.senderId());
        if (sender != null) {
            plugin.lang().send(sender, "denied-" + request.type().key(), Text.player(denierName));
            plugin.sounds().play(sender, "deny");
        }
    }
}

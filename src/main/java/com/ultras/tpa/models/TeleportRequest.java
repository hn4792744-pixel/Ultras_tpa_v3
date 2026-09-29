package com.ultras.tpa.models;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/** A single teleport request. The status can only leave PENDING once, which makes double accept / deny impossible. */
public final class TeleportRequest {

    private final UUID id = UUID.randomUUID();
    private final UUID senderId;
    private final UUID targetId;
    private final String senderName;
    private final String targetName;
    private final RequestType type;
    private final long createdAt;
    private final long expiresAt;
    private RequestStatus status = RequestStatus.PENDING;
    private BukkitTask expiryTask;

    public TeleportRequest(Player sender, Player target, RequestType type, long createdAt, long expiresAt) {
        this.senderId = sender.getUniqueId();
        this.targetId = target.getUniqueId();
        this.senderName = sender.getName();
        this.targetName = target.getName();
        this.type = type;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID id() {
        return id;
    }

    public UUID senderId() {
        return senderId;
    }

    public UUID targetId() {
        return targetId;
    }

    public String senderName() {
        return senderName;
    }

    public String targetName() {
        return targetName;
    }

    public RequestType type() {
        return type;
    }

    public long createdAt() {
        return createdAt;
    }

    public long expiresAt() {
        return expiresAt;
    }

    public synchronized RequestStatus status() {
        return status;
    }

    public boolean isPending() {
        return status() == RequestStatus.PENDING;
    }

    /** Moves PENDING to the given final status. Returns false if the request was already finished. */
    public synchronized boolean transition(RequestStatus next) {
        if (status != RequestStatus.PENDING) {
            return false;
        }
        status = next;
        return true;
    }

    public int secondsLeft() {
        return (int) Math.max(0L, (expiresAt - System.currentTimeMillis() + 999L) / 1000L);
    }

    public synchronized void setExpiryTask(BukkitTask task) {
        this.expiryTask = task;
    }

    public synchronized void cancelExpiry() {
        if (expiryTask != null) {
            expiryTask.cancel();
            expiryTask = null;
        }
    }
}

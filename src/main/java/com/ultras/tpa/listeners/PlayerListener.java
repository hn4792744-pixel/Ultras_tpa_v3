package com.ultras.tpa.listeners;

import com.ultras.tpa.UltrasTpa;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {

    private final UltrasTpa plugin;

    public PlayerListener(UltrasTpa plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.requests().handleQuit(event.getPlayer());
        plugin.teleports().handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.hasChangedBlock()) {
            plugin.teleports().handleMove(event.getPlayer());
        }
    }
}

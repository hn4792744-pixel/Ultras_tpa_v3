package com.ultras.tpa.gui;

import com.ultras.tpa.UltrasTpa;
import com.ultras.tpa.models.TeleportRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/** All pending requests received by the player. */
final class RequestsGui {

    private final UltrasTpa plugin;
    private final GuiManager gui;

    RequestsGui(UltrasTpa plugin, GuiManager gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    void open(Player viewer) {
        GuiConfig config = gui.config("requests");
        GuiHolder holder = gui.newHolder(config, viewer);
        List<Integer> slots = config.slots("request-slots", 0, 44);
        List<TeleportRequest> pending = plugin.requests().pendingFor(viewer.getUniqueId());

        for (int i = 0; i < Math.min(slots.size(), pending.size()); i++) {
            TeleportRequest request = pending.get(i);
            config.putAt(slots.get(i), holder, "head", viewer, Bukkit.getOfflinePlayer(request.senderId()), null, null,
                    e -> gui.openRequest(viewer, request), gui.requestTags(viewer, request));
        }
        config.put(holder, "close", viewer, null, null, null, e -> viewer.closeInventory());
        gui.show(viewer, holder);
    }
}

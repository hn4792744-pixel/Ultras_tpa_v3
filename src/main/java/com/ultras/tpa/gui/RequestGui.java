package com.ultras.tpa.gui;

import com.ultras.tpa.UltrasTpa;
import com.ultras.tpa.models.TeleportRequest;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Details of one incoming request with Accept / Deny / Block. */
final class RequestGui {

    private final UltrasTpa plugin;
    private final GuiManager gui;

    RequestGui(UltrasTpa plugin, GuiManager gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    void open(Player viewer, TeleportRequest request) {
        if (!plugin.requests().isOpenFor(viewer, request)) {
            return;
        }
        GuiConfig config = gui.config("request");
        TagResolver tags = gui.requestTags(viewer, request);
        GuiHolder holder = gui.newHolder(config, viewer, tags);
        holder.setRequestId(request.id());

        Player sender = Bukkit.getPlayer(request.senderId());
        config.put(holder, "head", viewer, Bukkit.getOfflinePlayer(request.senderId()), null,
                sender == null ? null : gui.infoLines(viewer, sender), null, tags);
        config.put(holder, "accept", viewer, null, null, null, e -> {
            viewer.closeInventory();
            plugin.requests().accept(viewer, request);
        }, tags);
        config.put(holder, "deny", viewer, null, null, null, e -> {
            viewer.closeInventory();
            plugin.requests().deny(viewer, request);
        }, tags);
        config.put(holder, "block", viewer, null, null, null, e -> {
            viewer.closeInventory();
            plugin.requests().setBlocked(viewer, request.senderId(), request.senderName(), true);
        }, tags);
        config.put(holder, "back", viewer, null, null, null, e -> gui.openRequests(viewer), tags);
        gui.show(viewer, holder);
    }
}

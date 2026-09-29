package com.ultras.tpa.gui;

import com.ultras.tpa.UltrasTpa;
import com.ultras.tpa.models.RequestType;
import com.ultras.tpa.utils.Text;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Player card with the TPA / TPAHERE buttons. */
final class SelectGui {

    private final UltrasTpa plugin;
    private final GuiManager gui;

    SelectGui(UltrasTpa plugin, GuiManager gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    void open(Player viewer, Player target) {
        GuiConfig config = gui.config("select");
        TagResolver player = Text.player(target.getName());
        GuiHolder holder = gui.newHolder(config, viewer, player);
        UUID targetId = target.getUniqueId();
        String targetName = target.getName();

        config.put(holder, "head", viewer, target, null, gui.infoLines(viewer, target), null, player);
        config.put(holder, "tpa", viewer, null, null, null, e -> send(viewer, targetId, targetName, RequestType.TPA), player);
        config.put(holder, "tpahere", viewer, null, null, null, e -> send(viewer, targetId, targetName, RequestType.TPAHERE), player);
        config.put(holder, "back", viewer, null, null, null, e -> gui.openPlayers(viewer, 0));
        config.put(holder, "close", viewer, null, null, null, e -> viewer.closeInventory());
        gui.show(viewer, holder);
    }

    private void send(Player viewer, UUID targetId, String targetName, RequestType type) {
        viewer.closeInventory();
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            plugin.lang().send(viewer, "player-offline", Text.player(targetName));
            plugin.sounds().play(viewer, "error");
            return;
        }
        plugin.requests().send(viewer, target, type);
    }
}

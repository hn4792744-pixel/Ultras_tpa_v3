package com.ultras.tpa.gui;

import com.ultras.tpa.UltrasTpa;
import com.ultras.tpa.utils.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Paged head list. SELECT mode picks a target for teleporting; BLOCK mode toggles blocking. */
final class PlayerListGui {

    enum Mode { SELECT, BLOCK }

    private record Entry(UUID id, String name) {
    }

    private final UltrasTpa plugin;
    private final GuiManager gui;
    private final String id;
    private final Mode mode;

    PlayerListGui(UltrasTpa plugin, GuiManager gui, String id, Mode mode) {
        this.plugin = plugin;
        this.gui = gui;
        this.id = id;
        this.mode = mode;
    }

    void open(Player viewer, int page) {
        GuiHolder holder = gui.newHolder(gui.config(id), viewer);
        render(viewer, holder, page);
        gui.show(viewer, holder);
    }

    private void render(Player viewer, GuiHolder holder, int requestedPage) {
        GuiConfig config = gui.config(id);
        holder.reset();
        config.applyFiller(holder.getInventory());

        List<Entry> entries = entries(viewer);
        List<Integer> slots = config.slots("player-slots", 0, 44);
        int perPage = slots.size();
        int pages = Math.max(1, (entries.size() + perPage - 1) / perPage);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        TagResolver[] pageTags = {
                Placeholder.unparsed("page", String.valueOf(page + 1)),
                Placeholder.unparsed("pages", String.valueOf(pages))};

        for (int i = 0; i < perPage; i++) {
            int index = page * perPage + i;
            if (index >= entries.size()) {
                break;
            }
            Entry entry = entries.get(index);
            placeHead(config, holder, slots.get(i), viewer, entry, page);
        }
        if (page > 0) {
            config.put(holder, "previous", viewer, null, null, null, e -> render(viewer, holder, page - 1), pageTags);
        }
        if (page < pages - 1) {
            config.put(holder, "next", viewer, null, null, null, e -> render(viewer, holder, page + 1), pageTags);
        }
        if (mode == Mode.BLOCK) {
            config.put(holder, "back", viewer, null, null, null, e -> gui.openSettings(viewer));
        }
        config.put(holder, "close", viewer, null, null, null, e -> viewer.closeInventory());
    }

    private void placeHead(GuiConfig config, GuiHolder holder, int slot, Player viewer, Entry entry, int page) {
        OfflinePlayer skull = Bukkit.getOfflinePlayer(entry.id());
        if (mode == Mode.SELECT) {
            Player online = Bukkit.getPlayer(entry.id());
            List<Component> info = online == null ? List.of() : gui.infoLines(viewer, online);
            config.putAt(slot, holder, "head", viewer, skull, null, info,
                    e -> choose(viewer, entry), Text.player(entry.name()));
            return;
        }
        boolean blocked = plugin.store().get(viewer.getUniqueId()).isBlocked(entry.id());
        config.putAt(slot, holder, "head", viewer, skull, blocked, null,
                e -> {
                    plugin.requests().setBlocked(viewer, entry.id(), entry.name(), !blocked);
                    render(viewer, holder, page);
                },
                Text.player(entry.name()),
                gui.stateTags(viewer, !blocked, blocked ? "state-blocked" : "state-allowed"));
    }

    private void choose(Player viewer, Entry entry) {
        Player target = Bukkit.getPlayer(entry.id());
        if (target == null) {
            plugin.lang().send(viewer, "player-offline", Text.player(entry.name()));
            plugin.sounds().play(viewer, "error");
            viewer.closeInventory();
            return;
        }
        gui.openSelect(viewer, target);
    }

    private List<Entry> entries(Player viewer) {
        Map<UUID, Entry> result = new LinkedHashMap<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(viewer.getUniqueId()) && viewer.canSee(online)) {
                result.put(online.getUniqueId(), new Entry(online.getUniqueId(), online.getName()));
            }
        }
        if (mode == Mode.BLOCK) {
            for (UUID blockedId : plugin.store().get(viewer.getUniqueId()).blocked()) {
                String name = Bukkit.getOfflinePlayer(blockedId).getName();
                result.putIfAbsent(blockedId, new Entry(blockedId, name != null ? name : blockedId.toString().substring(0, 8)));
            }
        }
        List<Entry> list = new ArrayList<>(result.values());
        list.sort(Comparator.comparing(entry -> entry.name().toLowerCase(java.util.Locale.ROOT)));
        return list;
    }
}

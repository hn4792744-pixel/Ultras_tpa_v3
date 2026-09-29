package com.ultras.tpa.gui;

import com.ultras.tpa.UltrasTpa;
import com.ultras.tpa.configuration.PluginConfig;
import com.ultras.tpa.models.Setting;
import com.ultras.tpa.models.TeleportRequest;
import com.ultras.tpa.utils.Text;
import com.ultras.tpa.utils.YamlFiles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Entry point for every menu: config loading, shared helpers and the open* methods. */
public final class GuiManager {

    private static final List<String> IDS = List.of("players", "select", "request", "requests", "settings", "block");

    private final UltrasTpa plugin;
    private final Map<String, GuiConfig> configs = new HashMap<>();
    private final PlayerListGui players;
    private final PlayerListGui block;
    private final SelectGui select;
    private final RequestGui request;
    private final RequestsGui requests;
    private final SettingsGui settings;

    public GuiManager(UltrasTpa plugin) {
        this.plugin = plugin;
        this.players = new PlayerListGui(plugin, this, "players", PlayerListGui.Mode.SELECT);
        this.block = new PlayerListGui(plugin, this, "block", PlayerListGui.Mode.BLOCK);
        this.select = new SelectGui(plugin, this);
        this.request = new RequestGui(plugin, this);
        this.requests = new RequestsGui(plugin, this);
        this.settings = new SettingsGui(plugin, this);
    }

    public void reload() {
        configs.clear();
        for (String id : IDS) {
            configs.put(id, new GuiConfig(plugin, id, YamlFiles.load(plugin, "guis/" + id + ".yml")));
        }
    }

    public GuiConfig config(String id) {
        return configs.get(id);
    }

    public void openPlayers(Player viewer, int page) {
        players.open(viewer, page);
    }

    public void openBlock(Player viewer, int page) {
        block.open(viewer, page);
    }

    public void openSelect(Player viewer, Player target) {
        select.open(viewer, target);
    }

    public void openRequest(Player viewer, TeleportRequest teleportRequest) {
        request.open(viewer, teleportRequest);
    }

    public void openRequests(Player viewer) {
        requests.open(viewer);
    }

    public void openSettings(Player viewer) {
        settings.open(viewer);
    }

    /** Closes the request menu of the target if it is currently showing the given request. */
    public void closeRequestViews(TeleportRequest teleportRequest) {
        Player target = Bukkit.getPlayer(teleportRequest.targetId());
        if (target == null) {
            return;
        }
        Inventory top = target.getOpenInventory().getTopInventory();
        if (top.getHolder(false) instanceof GuiHolder holder && teleportRequest.id().equals(holder.requestId())) {
            target.closeInventory();
        }
    }

    GuiHolder newHolder(GuiConfig config, Player viewer, TagResolver... titleTags) {
        GuiHolder holder = new GuiHolder();
        Inventory inventory = Bukkit.createInventory(holder, config.rows() * 9, config.title(viewer, titleTags));
        holder.bind(inventory);
        config.applyFiller(inventory);
        return holder;
    }

    void show(Player viewer, GuiHolder holder) {
        viewer.openInventory(holder.getInventory());
        plugin.sounds().play(viewer, "open-gui");
    }

    /** Placeholders {@code <c>} (state colour) and {@code <state>} (state text). */
    TagResolver stateTags(Player viewer, boolean positive, String textKey) {
        return TagResolver.resolver(
                TagResolver.resolver("c", Tag.styling(TextColor.color(positive ? 0x5FBF7A : 0xC94C58))),
                Placeholder.component("state", plugin.lang().component(viewer, textKey)));
    }

    TagResolver requestTags(Player viewer, TeleportRequest teleportRequest) {
        return TagResolver.resolver(
                Text.player(teleportRequest.senderName()),
                Text.seconds(teleportRequest.secondsLeft()),
                Placeholder.component("type", plugin.lang().component(viewer, "type-" + teleportRequest.type().key())));
    }

    /** Lore lines describing a player, empty when player information is disabled globally or by the player. */
    List<Component> infoLines(Player viewer, Player target) {
        PluginConfig config = plugin.settings();
        if (!config.playerInfoEnabled()
                || !plugin.store().get(target.getUniqueId()).isEnabled(Setting.SHOW_INFO, config)) {
            return List.of();
        }
        List<Component> lines = new ArrayList<>();
        lines.add(plugin.lang().component(viewer, "info-status"));
        if (config.infoWorld()) {
            lines.add(plugin.lang().component(viewer, "info-world", Placeholder.unparsed("world", target.getWorld().getName())));
        }
        if (config.infoLocation()) {
            Location location = target.getLocation();
            lines.add(plugin.lang().component(viewer, "info-location",
                    Placeholder.unparsed("x", String.valueOf(location.getBlockX())),
                    Placeholder.unparsed("y", String.valueOf(location.getBlockY())),
                    Placeholder.unparsed("z", String.valueOf(location.getBlockZ()))));
        }
        return lines;
    }
}

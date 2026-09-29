package com.ultras.tpa.gui;

import com.ultras.tpa.UltrasTpa;
import com.ultras.tpa.models.PlayerSettings;
import com.ultras.tpa.models.Setting;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

/** Toggle menu for the player's preferences. Toggles refresh the open menu in place. */
final class SettingsGui {

    private final UltrasTpa plugin;
    private final GuiManager gui;

    SettingsGui(UltrasTpa plugin, GuiManager gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    void open(Player viewer) {
        GuiHolder holder = gui.newHolder(gui.config("settings"), viewer);
        render(viewer, holder);
        gui.show(viewer, holder);
    }

    private void render(Player viewer, GuiHolder holder) {
        GuiConfig config = gui.config("settings");
        holder.reset();
        config.applyFiller(holder.getInventory());

        PlayerSettings settings = plugin.store().get(viewer.getUniqueId());
        for (Setting setting : Setting.values()) {
            boolean enabled = settings.isEnabled(setting, plugin.settings());
            config.put(holder, setting.key(), viewer, null, enabled, null, e -> toggle(viewer, holder, setting),
                    gui.stateTags(viewer, enabled, enabled ? "state-on" : "state-off"));
        }
        if (plugin.lang().languageCount() > 1) {
            config.put(holder, "language", viewer, null, null, null, e -> switchLanguage(viewer),
                    Placeholder.unparsed("language", plugin.lang().displayName(viewer)));
        }
        config.put(holder, "block-players", viewer, null, null, null, e -> gui.openBlock(viewer, 0));
        config.put(holder, "close", viewer, null, null, null, e -> viewer.closeInventory());
    }

    private void toggle(Player viewer, GuiHolder holder, Setting setting) {
        PlayerSettings settings = plugin.store().get(viewer.getUniqueId());
        boolean enabled = !settings.isEnabled(setting, plugin.settings());
        settings.set(setting, enabled);
        plugin.store().markDirty();
        plugin.sounds().play(viewer, enabled ? "toggle-on" : "toggle-off");
        render(viewer, holder);
    }

    private void switchLanguage(Player viewer) {
        plugin.store().get(viewer.getUniqueId()).setLanguage(plugin.lang().nextLanguage(plugin.lang().code(viewer)));
        plugin.store().markDirty();
        open(viewer);
    }
}

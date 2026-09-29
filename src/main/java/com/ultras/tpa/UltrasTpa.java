package com.ultras.tpa;

import com.ultras.tpa.commands.TpaCommand;
import com.ultras.tpa.commands.UcTpaCommand;
import com.ultras.tpa.configuration.PluginConfig;
import com.ultras.tpa.gui.GuiManager;
import com.ultras.tpa.language.LanguageManager;
import com.ultras.tpa.listeners.CommandGuard;
import com.ultras.tpa.listeners.GuiListener;
import com.ultras.tpa.listeners.PlayerListener;
import com.ultras.tpa.request.RequestManager;
import com.ultras.tpa.sound.SoundManager;
import com.ultras.tpa.storage.PlayerDataStore;
import com.ultras.tpa.teleport.TeleportService;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

/** ULTRAS_TPA by UC_Hussein. */
public final class UltrasTpa extends JavaPlugin {

    private PluginConfig settings;
    private PlayerDataStore store;
    private LanguageManager lang;
    private SoundManager sounds;
    private RequestManager requests;
    private TeleportService teleports;
    private GuiManager gui;
    private CommandGuard guard;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new PluginConfig(this);
        settings.load();
        store = new PlayerDataStore(this);
        store.load();
        lang = new LanguageManager(this);
        lang.load();
        sounds = new SoundManager(this);
        sounds.load();
        requests = new RequestManager(this);
        teleports = new TeleportService(this);
        gui = new GuiManager(this);
        gui.reload();
        guard = new CommandGuard(this);

        TpaCommand tpaCommand = new TpaCommand(this);
        for (String name : List.of("tpa", "tpahere", "tpaccept", "tpadeny", "tpacancel", "tpasetting", "tpauto", "uc_setting")) {
            bind(name, tpaCommand);
        }
        bind("uc_tpa", new UcTpaCommand(this));

        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(new GuiListener(this), this);
        manager.registerEvents(new PlayerListener(this), this);
        manager.registerEvents(guard, this);

        // Runs on the first server tick, i.e. after every plugin has registered its commands.
        getServer().getScheduler().runTask(this, guard::claim);
    }

    @Override
    public void onDisable() {
        if (teleports != null) {
            teleports.cancelAll();
        }
        if (requests != null) {
            requests.shutdown();
        }
        if (store != null) {
            store.saveNow();
        }
    }

    /** Reloads config.yml, languages, sounds and GUIs without a restart. */
    public void reloadAll() {
        reloadConfig();
        settings.load();
        lang.load();
        sounds.load();
        gui.reload();
        guard.claim();
    }

    private void bind(String name, TabExecutor executor) {
        PluginCommand command = Objects.requireNonNull(getCommand(name), "Command missing in plugin.yml: " + name);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public PluginConfig settings() {
        return settings;
    }

    public PlayerDataStore store() {
        return store;
    }

    public LanguageManager lang() {
        return lang;
    }

    public SoundManager sounds() {
        return sounds;
    }

    public RequestManager requests() {
        return requests;
    }

    public TeleportService teleports() {
        return teleports;
    }

    public GuiManager gui() {
        return gui;
    }
}

package com.ultras.tpa.models;

import java.util.Locale;

/** Per-player toggles. Every toggle is ON by default except {@link #AUTO_OPEN} (default from config.yml) and {@link #AUTO_ACCEPT} (always OFF). */
public enum Setting {
    RECEIVE_ALL,
    RECEIVE_TPA,
    RECEIVE_TPAHERE,
    SOUNDS,
    AUTO_OPEN,
    SHOW_INFO,
    ACTIONBAR,
    AUTO_ACCEPT;

    /** Key used in storage and as the item id inside guis/settings.yml. */
    public String key() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public static Setting fromKey(String key) {
        for (Setting setting : values()) {
            if (setting.key().equals(key)) {
                return setting;
            }
        }
        return null;
    }
}

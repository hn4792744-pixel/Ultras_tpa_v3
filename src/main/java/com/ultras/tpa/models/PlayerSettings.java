package com.ultras.tpa.models;

import com.ultras.tpa.configuration.PluginConfig;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Persistent preferences of one player. Mutated on the main thread only; the block set is safe to read from tab completion. */
public final class PlayerSettings {

    private final Map<Setting, Boolean> values = new EnumMap<>(Setting.class);
    private final Set<UUID> blocked = ConcurrentHashMap.newKeySet();
    private volatile String language;

    public boolean isEnabled(Setting setting, PluginConfig config) {
        Boolean value = values.get(setting);
        if (value != null) {
            return value;
        }
        return switch (setting) {
            case AUTO_OPEN -> config.autoOpenDefault();
            case AUTO_ACCEPT -> false;
            default -> true;
        };
    }

    public void set(Setting setting, boolean value) {
        values.put(setting, value);
    }

    public Map<Setting, Boolean> explicitValues() {
        return Collections.unmodifiableMap(values);
    }

    public boolean isBlocked(UUID id) {
        return blocked.contains(id);
    }

    /** @return true when the blocked state actually changed */
    public boolean setBlocked(UUID id, boolean value) {
        return value ? blocked.add(id) : blocked.remove(id);
    }

    public Set<UUID> blocked() {
        return Set.copyOf(blocked);
    }

    public String language() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isDefault() {
        return values.isEmpty() && blocked.isEmpty() && language == null;
    }
}

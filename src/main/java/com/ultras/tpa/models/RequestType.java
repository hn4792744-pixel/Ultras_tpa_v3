package com.ultras.tpa.models;

import java.util.Locale;

public enum RequestType {
    TPA(Setting.RECEIVE_TPA),
    TPAHERE(Setting.RECEIVE_TPAHERE);

    private final Setting receiveSetting;

    RequestType(Setting receiveSetting) {
        this.receiveSetting = receiveSetting;
    }

    public Setting receiveSetting() {
        return receiveSetting;
    }

    /** Lower-case id used in language keys ("tpa" / "tpahere"). */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}

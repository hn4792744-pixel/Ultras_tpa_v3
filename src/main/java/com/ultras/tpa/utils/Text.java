package com.ultras.tpa.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public final class Text {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    /** Code point of the mathematical monospace digit zero, used for the ULTRAS style numbers. */
    private static final int MONO_ZERO = 0x1D7F6;

    private Text() {
    }

    /** Parses MiniMessage text; italics are disabled by default so lore and names look clean. */
    public static Component parse(String input, TagResolver... resolvers) {
        return MINI.deserialize(input, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static TagResolver player(String name) {
        return Placeholder.unparsed("player", name);
    }

    public static TagResolver world(String name) {
        return Placeholder.unparsed("world", name);
    }

    public static TagResolver worlds(String names) {
        return Placeholder.unparsed("worlds", names);
    }

    public static TagResolver seconds(long value) {
        return Placeholder.unparsed("seconds", digits(value));
    }

    public static String digits(long value) {
        StringBuilder builder = new StringBuilder();
        for (char c : Long.toString(Math.max(0L, value)).toCharArray()) {
            builder.appendCodePoint(MONO_ZERO + (c - '0'));
        }
        return builder.toString();
    }
}

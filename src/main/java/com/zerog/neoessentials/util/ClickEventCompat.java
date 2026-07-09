package com.zerog.neoessentials.util;

import net.minecraft.network.chat.ClickEvent;

import java.net.URI;

/**
 * Constructs a {@link ClickEvent} from an action/string-value pair.
 *
 * <p>As of Minecraft 26.1, {@code ClickEvent} is a sealed interface implemented by
 * per-action records ({@code RunCommand}, {@code SuggestCommand}, {@code OpenUrl}, etc.)
 * rather than a single concrete class with an {@code Action}/{@code String} constructor.</p>
 */
public final class ClickEventCompat {
    private ClickEventCompat() {}

    /**
     * Returns a {@link ClickEvent} for the given action/value, matching the value
     * encoding the vanilla client uses for each action (a URL string for
     * {@code OPEN_URL}, a page number string for {@code CHANGE_PAGE}, otherwise
     * the raw string payload).
     */
    public static ClickEvent create(ClickEvent.Action action, String value) {
        return switch (action) {
            case RUN_COMMAND -> new ClickEvent.RunCommand(value);
            case SUGGEST_COMMAND -> new ClickEvent.SuggestCommand(value);
            case OPEN_URL -> new ClickEvent.OpenUrl(URI.create(value));
            case OPEN_FILE -> new ClickEvent.OpenFile(value);
            case COPY_TO_CLIPBOARD -> new ClickEvent.CopyToClipboard(value);
            case CHANGE_PAGE -> new ClickEvent.ChangePage(Integer.parseInt(value));
            default -> throw new IllegalArgumentException("Unsupported ClickEvent action: " + action);
        };
    }
}

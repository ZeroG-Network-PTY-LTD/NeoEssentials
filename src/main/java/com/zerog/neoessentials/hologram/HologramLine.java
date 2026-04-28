package com.zerog.neoessentials.hologram;

import java.util.UUID;

/**
 * Represents one line of a hologram.
 *
 * <p>The {@link #text} field may contain colour codes {@code &x} / {@code &#RRGGBB}
 * as well as placeholder tokens like {@code {neoessentials_server_online}}.
 */
public class HologramLine {

    /** Unique identifier for this line (used for ordered updates). */
    public String lineId = UUID.randomUUID().toString();

    /**
     * Raw template text — may include {@code &} colour codes and
     * {@code {placeholder}} tokens.
     */
    public String text = "";

    public HologramLine() {}

    public HologramLine(String text) {
        this.text = text != null ? text : "";
    }
}


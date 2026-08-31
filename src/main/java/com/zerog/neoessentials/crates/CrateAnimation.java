package com.zerog.neoessentials.crates;

/** How {@code gui/CrateOpeningMenu} reveals the winning reward. */
public enum CrateAnimation {
    /** CS:GO-style horizontal strip that scrolls and decelerates onto the winning reward. */
    ROULETTE,
    /** A handful of slots flicker random rewards a few times before settling. */
    SEQUENTIAL,
    /** No animation — reward granted and announced immediately. */
    INSTANT;

    public static CrateAnimation parse(String value) {
        if (value == null) return SEQUENTIAL;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SEQUENTIAL;
        }
    }
}

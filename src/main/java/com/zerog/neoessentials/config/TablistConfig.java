package com.zerog.neoessentials.config;

/**
 * Tablist, scoreboard, and bossbar customization config
 */
public class TablistConfig {

    public String tablistFormat = "[{group}] {player_name} | Ping: {ping}";
    public String scoreboardFormat = "Score: {score} | Player: {player_name}";
    public String bossbarFormat = "Boss: {bossbar} | {message} [{progress}%]";
    public String showTo = "all";
    public int priority = 1;
    public boolean showHeaderFooter = true;
    public double footerInterval = 5.0;
    public String layout = "DYNAMIC_SIZE";
    public boolean enableScoreboard = true;
    public boolean enableBossbar = true;
    public boolean enableTablist = true;
    public boolean enableNametag = true;
    public int size = 60;

    // Removed defaultTablist; tablistLayouts now handles all conditions
    public java.util.Map<String, PermSet> PermSets = new java.util.HashMap<>();
    public java.util.Map<String, FilterSet> filter = new java.util.HashMap<>();
    public String teamFiltersOrder = null;

    // Main tablist layouts array for config-driven tablist
    public java.util.List<TablistLayout> tablistLayouts = new java.util.ArrayList<>();

    public static class TablistLayout {
    public int priority = 1;
    public String conditionType = "default";
    public String condition = "";
    public java.util.List<String> header = new java.util.ArrayList<>();
    public java.util.List<String> footer = new java.util.ArrayList<>();
    // Added for tablist player formatting
    public String format = "%player%";
    public String color = "";
    public String prefix = "";
    public String suffix = "";
    }


    public static class PermSet {
        public Condition condition = new Condition();
        public String permission;
        public TablistSection tablist = new TablistSection();
    }

    public static class Condition {
        public String type = "all";
        public String value = "";
    }

    public static class FilterSet {
        public String filter;
    }

    public static class TablistSection {
        public java.util.List<String> header = new java.util.ArrayList<>();
        public java.util.List<String> footer = new java.util.ArrayList<>();
    }

    // ...existing code for Placeholder, PlayerSet, Component...

    public static class Placeholder {
        /** Type: static, player, conditional, etc. */
        public String type = "conditional";
        /** Condition for conditional placeholder */
        public String condition = "";
        /** Value if condition is true */
        public String trueValue = "";
        /** Value if condition is false */
        public String falseValue = "";

        public Placeholder() {}
        public Placeholder(String type, String condition, String trueValue, String falseValue) {
            this.type = type;
            this.condition = condition;
            this.trueValue = trueValue;
            this.falseValue = falseValue;
        }
    }

    public static class PlayerSet {
        /** Filter string, e.g. permission:neo.staff */
        public String filter = "";
        /** Optional custom header for this group */
        public java.util.List<String> header = null;
        /** Optional custom footer for this group */
        public java.util.List<String> footer = null;

        public PlayerSet() {}
        public PlayerSet(String filter) {
            this.filter = filter;
        }
        public PlayerSet(String filter, java.util.List<String> header, java.util.List<String> footer) {
            this.filter = filter;
            this.header = header;
            this.footer = footer;
        }
    }

    public static class Component {
        /** Display text for the component */
        public String text = "";
        /** Icon name (e.g. player_head) */
        public String icon = "";
        /** Ping value for display */
        public int ping = 0;
        /** Is this component animated? */
        public boolean animated = false;
        /** Animation interval in seconds */
        public double interval = 0.0;
        /** Animation frames */
        public java.util.List<String> frames = new java.util.ArrayList<>();

        public Component() {}
        public Component(String text, String icon, int ping, boolean animated, double interval, java.util.List<String> frames) {
            this.text = text;
            this.icon = icon;
            this.ping = ping;
            this.animated = animated;
            this.interval = interval;
            this.frames = frames;
        }
    }
}

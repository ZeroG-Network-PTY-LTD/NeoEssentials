package com.zerog.neoessentials.sidebar;

import java.util.List;

/**
 * A named sidebar-scoreboard definition. Multiple boards can be configured; the first one
 * (in descending {@code priority} order) whose {@code conditions} all evaluate true for a
 * given player is shown to them. A board with an empty condition list always matches, so it
 * must be the lowest-priority entry to act as the fallback/default.
 */
public class ScoreboardBoard {
    private final String name;
    private final int priority;
    private final List<String> conditions;
    private List<String> titleFrames;
    private final List<ScoreboardLine> lines;

    public ScoreboardBoard(String name, int priority, List<String> conditions,
                            List<String> titleFrames, List<ScoreboardLine> lines) {
        this.name = name;
        this.priority = priority;
        this.conditions = conditions;
        this.titleFrames = titleFrames;
        this.lines = lines;
    }

    public String getName() { return name; }
    public int getPriority() { return priority; }
    public List<String> getConditions() { return conditions; }
    public List<String> getTitleFrames() { return titleFrames; }
    public void setTitleFrames(List<String> titleFrames) { this.titleFrames = titleFrames; }
    public List<ScoreboardLine> getLines() { return lines; }

    public String currentTitleFrame(int frameIndex) {
        if (titleFrames.isEmpty()) return "";
        return titleFrames.get(frameIndex % titleFrames.size());
    }
}

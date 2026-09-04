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
    /**
     * Per-board slowdown relative to {@code scoreboard.refreshInterval} — this board's own
     * frame only advances once every {@code refreshMultiplier} global refresh cycles (1 =
     * same rate as every other board, the previous only behavior; 3 = a third as often).
     * Deliberately a multiplier of the shared cadence rather than an independent absolute
     * interval — keeps the existing global tick-gate (and everything hung off it: join delay,
     * toggle state, packet-dirty diffing) completely unchanged, so a board that wants to
     * refresh less often (e.g. one built from an expensive placeholder) can, without any of
     * the risk of re-deriving per-board show/hide/packet timing from scratch.
     */
    private final int refreshMultiplier;

    // ── Runtime state (not persisted) ────────────────────────────────────────
    private int ownCycleCount = 0;
    private int ownAnimFrame = 0;

    public ScoreboardBoard(String name, int priority, List<String> conditions,
                            List<String> titleFrames, List<ScoreboardLine> lines) {
        this(name, priority, conditions, titleFrames, lines, 1);
    }

    public ScoreboardBoard(String name, int priority, List<String> conditions,
                            List<String> titleFrames, List<ScoreboardLine> lines, int refreshMultiplier) {
        this.name = name;
        this.priority = priority;
        this.conditions = conditions;
        this.titleFrames = titleFrames;
        this.lines = lines;
        this.refreshMultiplier = Math.max(1, refreshMultiplier);
    }

    public String getName() { return name; }
    public int getPriority() { return priority; }
    public List<String> getConditions() { return conditions; }
    public List<String> getTitleFrames() { return titleFrames; }
    public void setTitleFrames(List<String> titleFrames) { this.titleFrames = titleFrames; }
    public List<ScoreboardLine> getLines() { return lines; }
    public int getRefreshMultiplier() { return refreshMultiplier; }

    public String currentTitleFrame(int frameIndex) {
        if (titleFrames.isEmpty()) return "";
        return titleFrames.get(frameIndex % titleFrames.size());
    }

    /** Advances this board's own frame counter once every {@link #refreshMultiplier} calls —
     *  called once per global refresh cycle by {@link ScoreboardManager#onTick}. */
    void tickOwnFrame() {
        ownCycleCount++;
        if (ownCycleCount >= refreshMultiplier) {
            ownCycleCount = 0;
            ownAnimFrame++;
        }
    }

    /** This board's own current frame index — use in place of the manager's shared
     *  {@code animFrame} so a {@code refreshMultiplier} > 1 actually slows this board down. */
    public int getOwnAnimFrame() { return ownAnimFrame; }
}

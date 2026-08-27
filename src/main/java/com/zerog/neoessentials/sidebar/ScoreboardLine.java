package com.zerog.neoessentials.sidebar;

import java.util.List;

/**
 * One line of a {@link ScoreboardBoard}. {@code frames} has a single element for a static
 * line, or multiple for an animated one (cycled the same way tablist header/footer frames
 * are, via the shared {@link com.zerog.neoessentials.tablist.AnimationManager} tick clock).
 */
public class ScoreboardLine {
    private final List<String> frames;
    private final String condition;

    public ScoreboardLine(List<String> frames, String condition) {
        this.frames = frames;
        this.condition = condition;
    }

    public List<String> getFrames() { return frames; }
    public String getCondition() { return condition; }

    public String currentFrame(int frameIndex) {
        if (frames.isEmpty()) return "";
        return frames.get(frameIndex % frames.size());
    }
}

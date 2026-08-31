package com.zerog.neoessentials.crates;

import java.util.List;
import java.util.Random;
import java.util.function.ToDoubleFunction;

/** Standard cumulative-weight-sum weighted random pick — no existing implementation of this
 *  anywhere else in the codebase (confirmed during planning), so this is new/shared. */
public final class WeightedRandomPicker {
    private static final Random RANDOM = new Random();

    private WeightedRandomPicker() {}

    /** Picks one element from {@code items}, weighted by {@code weightFn}. Non-positive weights
     *  are treated as 0 (never picked). Returns {@code null} if the list is empty or every
     *  weight is <= 0. */
    public static <T> T pick(List<T> items, ToDoubleFunction<T> weightFn) {
        if (items == null || items.isEmpty()) return null;

        double total = 0;
        for (T item : items) {
            double w = weightFn.applyAsDouble(item);
            if (w > 0) total += w;
        }
        if (total <= 0) return null;

        double roll = RANDOM.nextDouble() * total;
        double cumulative = 0;
        for (T item : items) {
            double w = weightFn.applyAsDouble(item);
            if (w <= 0) continue;
            cumulative += w;
            if (roll < cumulative) return item;
        }
        // Floating-point edge case — fall back to the last positively-weighted item.
        for (int i = items.size() - 1; i >= 0; i--) {
            if (weightFn.applyAsDouble(items.get(i)) > 0) return items.get(i);
        }
        return null;
    }
}

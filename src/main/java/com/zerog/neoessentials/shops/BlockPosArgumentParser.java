package com.zerog.neoessentials.shops;

import net.minecraft.core.BlockPos;

public class BlockPosArgumentParser {
    // Expects format: x y z (space-separated)
    public static BlockPos fromString(String s) {
        String[] parts = s.trim().split(" ");
        if (parts.length != 3) throw new IllegalArgumentException("Invalid BlockPos string: " + s);
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
        int z = Integer.parseInt(parts[2]);
        return new BlockPos(x, y, z);
    }
}

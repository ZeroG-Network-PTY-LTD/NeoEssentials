package com.zerog.neoessentials.api;

import java.util.UUID;

public interface TeleportService {
    boolean teleport(UUID playerId, double x, double y, double z);
    // Add more teleport-related methods as needed
}

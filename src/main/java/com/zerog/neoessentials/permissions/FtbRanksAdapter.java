package com.zerog.neoessentials.permissions;

import java.lang.reflect.Method;
import java.util.UUID;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for FTB Ranks integration using reflection to avoid a hard compile-time dependency.
 *
 * <p>FTB Ranks 2101.1.x (NeoForge) exposes its permission check through a static helper:
 * <pre>
 *   FTBRanksAPI.getPermission(ServerPlayer, String, boolean) → Optional<Boolean> / boolean
 * </pre>
 * Older builds used an instance method {@code hasPermission(UUID, String)}.
 * We probe all known patterns and use the first one that succeeds.
 */
public class FtbRanksAdapter implements ExternalPermissionAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FtbRanksAdapter.class);

    private final boolean ftbRanksLoaded;

    // Resolved API method + the object to call it on (null = static)
    private Method resolvedMethod = null;
    private Object  resolvedInstance = null;
    private int     resolvedStrategy = 0; // 1=getPermission(player,node,bool), 2=hasPermission(uuid,node)

    public FtbRanksAdapter() {
        this.ftbRanksLoaded = ModList.get().isLoaded("ftbranks");
        if (ftbRanksLoaded) {
            probeApi();
        }
    }

    private void probeApi() {
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbranks.api.FTBRanksAPI");

            // ── Strategy 1: static getPermission(ServerPlayer, String, boolean) ──────────
            // Used in FTB Ranks 2101.1.x
            try {
                Method m = apiClass.getMethod("getPermission",
                        net.minecraft.server.level.ServerPlayer.class, String.class, boolean.class);
                resolvedMethod   = m;
                resolvedInstance = null; // static
                resolvedStrategy = 1;
                LOGGER.info("FTB Ranks adapter: using getPermission(ServerPlayer, String, boolean) [strategy 1]");
                return;
            } catch (NoSuchMethodException ignored) {}

            // ── Strategy 2: instance hasPermission(UUID, String) via INSTANCE or getInstance() ─
            Object instance = null;
            try {
                instance = apiClass.getField("INSTANCE").get(null);
            } catch (NoSuchFieldException e) {
                try {
                    instance = apiClass.getMethod("getInstance").invoke(null);
                } catch (Exception ignored2) {}
            }

            if (instance != null) {
                try {
                    Method m = instance.getClass().getMethod("hasPermission", UUID.class, String.class);
                    resolvedMethod   = m;
                    resolvedInstance = instance;
                    resolvedStrategy = 2;
                    LOGGER.info("FTB Ranks adapter: using hasPermission(UUID, String) [strategy 2]");
                    return;
                } catch (NoSuchMethodException ignored) {}
            }

            LOGGER.warn("FTB Ranks adapter: could not resolve any known hasPermission API. " +
                        "Permission checks will fall back to OP / internal system.");
        } catch (ClassNotFoundException e) {
            LOGGER.debug("FTB Ranks API class not found — mod may not be installed");
        } catch (Exception e) {
            LOGGER.warn("FTB Ranks adapter init failed: {}", e.getMessage());
        }
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        if (!ftbRanksLoaded || resolvedMethod == null) return false;
        try {
            if (resolvedStrategy == 1) {
                // getPermission(ServerPlayer, String, boolean) – need the live ServerPlayer
                var server = ServerLifecycleHooks.getCurrentServer();
                if (server == null) return false;
                net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player == null) return false; // offline player – can't check
                Object result = resolvedMethod.invoke(null, player, permission, false);
                if (result instanceof Boolean b)             return b;
                if (result instanceof java.util.Optional<?> opt) {
                    Object inner = opt.orElse(null);
                    if (inner instanceof Boolean b) return b;
                }
                // Some versions return a TriState / Permission value
                if (result != null) {
                    try { return (boolean) result.getClass().getMethod("get").invoke(result); }
                    catch (Exception ignored) {}
                    return !result.toString().equalsIgnoreCase("FALSE")
                        && !result.toString().equalsIgnoreCase("UNDEFINED");
                }
                return false;
            } else if (resolvedStrategy == 2) {
                Object result = resolvedMethod.invoke(resolvedInstance, uuid, permission);
                return result instanceof Boolean b && b;
            }
        } catch (Exception e) {
            LOGGER.error("FTB Ranks permission check failed for '{}': {}", permission, e.getMessage());
        }
        return false;
    }

    @Override
    public String getPrefix(UUID uuid) {
        if (!ftbRanksLoaded) return null;
        // FTB Ranks does not expose a chat prefix via a stable API; return null to fall back.
        return null;
    }

    @Override
    public String getSuffix(UUID uuid) {
        if (!ftbRanksLoaded) return null;
        return null;
    }

    @Override
    public void reload() { /* FTB Ranks handles its own reload */ }

    @Override
    public String getName() { return "FTB Ranks"; }

    @Override
    public boolean isAvailable() {
        return ftbRanksLoaded && resolvedMethod != null;
    }
}

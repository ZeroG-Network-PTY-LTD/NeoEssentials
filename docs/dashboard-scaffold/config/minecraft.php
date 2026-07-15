<?php

return [

    // Base URL of the mod's embedded HTTP API, e.g. http://127.0.0.1:8642
    // If the game server isn't publicly reachable, point this at a tunnel/VPN address instead.
    'api_url' => env('MC_API_URL', 'http://127.0.0.1:8642'),

    // The mod's dashboard auth is session-based (POST /api/auth/login), not a
    // static shared token — there's no config value on the mod side to copy a
    // token from. Instead, create a dedicated service account for this Laravel
    // app via the mod's user management (don't reuse the bootstrap admin/admin123
    // account for machine-to-machine calls):
    //   1. Log into the mod's own dashboard once as the default admin.
    //   2. POST /api/auth/users {"username": "...", "password": "...", "role": "ADMIN"}
    //      (or MODERATOR, if you don't want economy/command access from Laravel).
    //   3. Put those credentials here.
    // MinecraftApiService logs in once, caches the resulting sessionId, and
    // transparently re-authenticates if it expires (mod-side idle timeout: 30 min).
    'service_username' => env('MC_SERVICE_USERNAME'),
    'service_password' => env('MC_SERVICE_PASSWORD'),

    // Request timeout in seconds. The mod should respond from an in-memory
    // cache, so this can stay short — a slow response usually means the
    // server is lagging or the mod's cache thread has stalled.
    'timeout' => (int) env('MC_API_TIMEOUT', 4),

    // How long to cache read-only responses (players, status, economy) in
    // Laravel's cache layer, in seconds. Keeps the dashboard snappy on
    // multi-tab / multi-admin use without hammering the mod's API.
    'cache_ttl' => (int) env('MC_API_CACHE_TTL', 3),

    // How long to cache the session token before proactively re-logging in,
    // in seconds. Kept comfortably under the mod's 30-minute idle timeout so
    // a cached session is (almost) never stale; a 401 still triggers an
    // immediate re-login + retry regardless of this value.
    'session_cache_ttl' => (int) env('MC_SESSION_CACHE_TTL', 1500),

];

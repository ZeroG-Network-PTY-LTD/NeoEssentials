<?php

namespace App\Services;

use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;
use RuntimeException;

/**
 * Single point of contact between the dashboard and the mod's embedded
 * HTTP API. Every controller talks to the mod through this class — nothing
 * else in the app should call Http::/... directly against MC_API_URL.
 *
 * Also acts as an anti-corruption layer: the mod's JSON field names/shapes
 * (username-keyed player actions, `world` instead of `dimension`, millisecond
 * timestamps, etc.) are mapped here into the exact shapes the Inertia pages
 * and resources/js/types/minecraft.ts expect, so nothing above this class
 * needs to know about the mod's actual REST contract.
 */
class MinecraftApiService
{
    private string $baseUrl;
    private string $serviceUsername;
    private string $servicePassword;
    private int $timeout;
    private int $cacheTtl;
    private int $sessionCacheTtl;

    public function __construct()
    {
        $this->baseUrl = rtrim((string) config('minecraft.api_url'), '/');
        $this->serviceUsername = (string) config('minecraft.service_username');
        $this->servicePassword = (string) config('minecraft.service_password');
        $this->timeout = (int) config('minecraft.timeout');
        $this->cacheTtl = (int) config('minecraft.cache_ttl');
        $this->sessionCacheTtl = (int) config('minecraft.session_cache_ttl');
    }

    // --- Status / players -----------------------------------------------

    /** Server status: TPS, uptime, online count, memory. Cached briefly. */
    public function status(): array
    {
        return Cache::remember('mc_api:status', $this->cacheTtl, function () {
            $status = $this->get('api/server/status');
            $performance = $this->get('api/stats/performance');

            return [
                'online' => (bool) ($status['online'] ?? false),
                'tps' => (float) ($status['tps'] ?? 0),
                'uptimeSeconds' => (int) round(($status['uptimeMillis'] ?? 0) / 1000),
                'onlineCount' => (int) ($status['playersOnline'] ?? 0),
                'maxPlayers' => (int) ($status['playersMax'] ?? 0),
                'memoryUsedMb' => (int) ($performance['memUsedMb'] ?? 0),
                'memoryMaxMb' => (int) ($performance['memMaxMb'] ?? 0),
            ];
        });
    }

    /** Online players, shaped to match the McPlayer type. Cached briefly. */
    public function players(): array
    {
        $data = Cache::remember('mc_api:players', $this->cacheTtl, fn () => $this->get('api/player/online'));
        $online = $data['online'] ?? [];

        return array_map(fn (array $p) => [
            'uuid' => $p['uuid'],
            'username' => $p['username'],
            // The mod doesn't have a "rank" concept exposed cheaply for a bulk
            // player list (that would mean one /api/permissions lookup per
            // player) — approximate it from operator status instead.
            'rank' => ($p['operator'] ?? false) ? 'op' : 'player',
            'online' => true,
            'health' => (float) ($p['health'] ?? 20),
            'maxHealth' => (float) ($p['maxHealth'] ?? 20),
            'hunger' => (float) ($p['foodLevel'] ?? 20),
            'dimension' => $p['dimension'] ?? 'minecraft:overworld',
            'x' => (float) ($p['x'] ?? 0),
            'y' => (float) ($p['y'] ?? 0),
            'z' => (float) ($p['z'] ?? 0),
            'playtimeMinutes' => 0, // not exposed by the mod's online-player list
            'balance' => 0,          // fetch via economyLeaderboard() if needed
        ], $online);
    }

    // --- Player actions (identifier is a username, per the mod's contract) --

    public function teleportPlayer(string $username, array $payload): array
    {
        return $this->post("api/player/teleport/{$username}", $payload);
    }

    public function healPlayer(string $username): array
    {
        return $this->post("api/player/heal/{$username}", []);
    }

    public function kickPlayer(string $username, string $reason): array
    {
        return $this->post("api/player/kick/{$username}", ['reason' => $reason]);
    }

    public function banPlayer(string $username, string $reason, ?string $duration = null): array
    {
        return $this->post('api/moderation/ban', [
            'target' => $username,
            'playerName' => $username,
            'reason' => $reason,
            'type' => 'NAME',
            'duration' => $duration ? (int) $duration : -1, // seconds; -1 = permanent
        ]);
    }

    public function mutePlayer(string $username, ?string $duration = null): array
    {
        return $this->post('api/moderation/mute', [
            'targetName' => $username,
            'duration' => $duration ? (int) $duration : null, // seconds; omit = indefinite
        ]);
    }

    // --- Economy -----------------------------------------------------------

    /** Balance leaderboard, shaped to match LeaderboardEntry[]. Cached briefly. */
    public function economyLeaderboard(): array
    {
        $data = Cache::remember('mc_api:economy_leaderboard', $this->cacheTtl,
            fn () => $this->get('api/stats/economy'));
        $top = $data['topPlayers'] ?? [];

        return array_map(fn (array $e) => [
            'uuid' => $e['uuid'],
            'username' => $e['name'],
            'balance' => (float) $e['balance'],
        ], $top);
    }

    /** $identifier may be a username or a raw UUID — the mod accepts either. */
    public function economyAdjust(string $identifier, string $action, float $amount): array
    {
        return $this->post("api/economy/{$identifier}", [
            'action' => $action, // give | take | set
            'amount' => $amount,
        ]);
    }

    // --- Warps ---------------------------------------------------------------

    /** Public warps, shaped to match the Warp type. Cached briefly. */
    public function warps(): array
    {
        $data = Cache::remember('mc_api:warps', $this->cacheTtl, fn () => $this->get('api/warps'));
        $warps = $data['warps'] ?? [];

        return array_map(fn (array $w) => [
            'name' => $w['name'],
            'x' => (float) $w['x'],
            'y' => (float) $w['y'],
            'z' => (float) $w['z'],
            'dimension' => $w['world'] ?? 'minecraft:overworld',
            'createdBy' => $w['createdBy'] ?? 'Unknown',
        ], $warps);
    }

    public function createWarp(string $name, array $location): array
    {
        return $this->post('api/warps', array_merge(['name' => $name], $location));
    }

    public function deleteWarp(string $name): array
    {
        return $this->delete("api/warps/{$name}");
    }

    // --- Homes / console -----------------------------------------------------

    public function homes(string $username): array
    {
        return $this->get("api/player/homes/{$username}")['homes'] ?? [];
    }

    public function runCommand(string $command): array
    {
        return $this->post('api/commands/execute', ['command' => $command]);
    }

    /** Recent join/leave/chat/command activity, shaped to match LogEntry[]. */
    public function logs(?int $since = null): array
    {
        $data = $this->get('api/game/events');
        $events = $data['events'] ?? [];

        // Types with a LogEntry equivalent — anything else (e.g. block.break, which
        // the mod also queues for the activity-summary/top-blocks endpoints) has no
        // matching LogEntryType and is dropped here rather than mismapped.
        $typeMap = [
            'player.join' => 'join',
            'player.leave' => 'leave',
            'player.chat' => 'chat',
            'player.command' => 'command',
        ];

        $entries = [];
        foreach ($events as $e) {
            $type = $typeMap[$e['type'] ?? ''] ?? null;
            if ($type === null) {
                continue;
            }

            $message = $e['message'] ?? '';
            // Every message the mod generates for these types starts with the
            // player's name (e.g. "Steve joined the game", "Steve: hi", "Steve ran:
            // /tp"). Extracting it from there — rather than looking the uuid up in
            // the online-players list — works for `leave` and any historical event
            // whose player has since disconnected, not just currently-online ones.
            $username = str_contains($message, ' ') ? strstr($message, ' ', true) : $message;

            $entries[] = [
                'timestamp' => (int) round(($e['timestamp'] ?? 0) / 1000), // ms -> seconds
                'type' => $type,
                'username' => $username ?: '',
                'message' => $message,
            ];
        }

        if ($since !== null) {
            $entries = array_values(array_filter($entries, fn (array $e) => $e['timestamp'] >= $since));
        }

        return $entries;
    }

    // --- internals ---------------------------------------------------

    private function cachedGet(string $cacheKey, string $path): array
    {
        return Cache::remember("mc_api:{$cacheKey}", $this->cacheTtl, fn () => $this->get($path));
    }

    private function get(string $path, array $query = []): array
    {
        return $this->request('get', $path, $query);
    }

    private function post(string $path, array $payload): array
    {
        $result = $this->request('post', $path, $payload);
        $this->bustCaches();
        return $result;
    }

    private function delete(string $path): array
    {
        $result = $this->request('delete', $path);
        $this->bustCaches();
        return $result;
    }

    /**
     * @param bool $isRetry internal — set on the single allowed retry after a 401,
     *                       to avoid an infinite loop if the service account itself is broken.
     */
    private function request(string $method, string $path, array $data = [], bool $isRetry = false): array
    {
        $sessionId = $this->sessionId();

        try {
            $response = Http::withToken($sessionId)
                ->timeout($this->timeout)
                ->{$method}("{$this->baseUrl}/{$path}", $data);
        } catch (\Throwable $e) {
            Log::warning('Minecraft API unreachable', ['path' => $path, 'error' => $e->getMessage()]);
            throw new RuntimeException('Could not reach the Minecraft server API. Is the server online?');
        }

        if ($response->status() === 401) {
            if ($isRetry) {
                throw new RuntimeException('Minecraft API rejected the dashboard service account — check MC_SERVICE_USERNAME/MC_SERVICE_PASSWORD.');
            }
            // Session expired or was invalidated server-side — log in again and retry once.
            Cache::forget('mc_api:session_id');
            return $this->request($method, $path, $data, true);
        }

        if ($response->failed()) {
            Log::warning('Minecraft API error', ['path' => $path, 'status' => $response->status(), 'body' => $response->body()]);
            throw new RuntimeException("Minecraft API returned an error ({$response->status()}).");
        }

        return $response->json() ?? [];
    }

    /** Returns a cached session id, logging in fresh if none is cached or valid. */
    private function sessionId(): string
    {
        return Cache::remember('mc_api:session_id', $this->sessionCacheTtl, function () {
            if (empty($this->serviceUsername) || empty($this->servicePassword)) {
                throw new RuntimeException('MC_SERVICE_USERNAME / MC_SERVICE_PASSWORD are not configured.');
            }

            try {
                $response = Http::timeout($this->timeout)
                    ->post("{$this->baseUrl}/api/auth/login", [
                        'username' => $this->serviceUsername,
                        'password' => $this->servicePassword,
                    ]);
            } catch (\Throwable $e) {
                Log::warning('Minecraft API login unreachable', ['error' => $e->getMessage()]);
                throw new RuntimeException('Could not reach the Minecraft server API to log in. Is the server online?');
            }

            if ($response->failed() || !($response->json('success'))) {
                Log::warning('Minecraft API login failed', ['status' => $response->status(), 'body' => $response->body()]);
                throw new RuntimeException('Minecraft API login failed — check MC_SERVICE_USERNAME/MC_SERVICE_PASSWORD.');
            }

            return $response->json('sessionId');
        });
    }

    private function bustCaches(): void
    {
        foreach (['status', 'players', 'economy_leaderboard', 'warps'] as $key) {
            Cache::forget("mc_api:{$key}");
        }
    }
}

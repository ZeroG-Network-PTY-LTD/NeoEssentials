<?php

namespace App\Http\Controllers;

use App\Services\MinecraftApiService;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Inertia\Inertia;
use Inertia\Response;
use RuntimeException;

class PlayerController extends Controller
{
    public function __construct(private MinecraftApiService $mc)
    {
    }

    public function index(): Response
    {
        return Inertia::render('Dashboard/Players', [
            'players' => $this->mc->players(),
        ]);
    }

    public function teleport(Request $request, string $uuid): RedirectResponse
    {
        $data = $request->validate([
            'target_uuid' => ['nullable', 'string'],
            'x' => ['nullable', 'numeric'],
            'y' => ['nullable', 'numeric'],
            'z' => ['nullable', 'numeric'],
        ]);

        $payload = [];
        if (!empty($data['target_uuid'])) {
            $payload['targetUsername'] = $this->resolveUsername($data['target_uuid']);
        }
        if (isset($data['x'], $data['y'], $data['z'])) {
            $payload['x'] = $data['x'];
            $payload['y'] = $data['y'];
            $payload['z'] = $data['z'];
        }

        $this->mc->teleportPlayer($this->resolveUsername($uuid), $payload);

        return back()->with('success', 'Teleport sent.');
    }

    public function heal(string $uuid): RedirectResponse
    {
        $this->mc->healPlayer($this->resolveUsername($uuid));
        return back()->with('success', 'Player healed and fed.');
    }

    public function kick(Request $request, string $uuid): RedirectResponse
    {
        $data = $request->validate(['reason' => ['required', 'string', 'max:255']]);
        $this->mc->kickPlayer($this->resolveUsername($uuid), $data['reason']);
        return back()->with('success', 'Player kicked.');
    }

    public function ban(Request $request, string $uuid): RedirectResponse
    {
        $data = $request->validate([
            'reason' => ['required', 'string', 'max:255'],
            'duration' => ['nullable', 'string'],
        ]);
        // Ban targets an online player here; the mod's ban endpoint also accepts
        // offline players by name, but this scaffold's UI only lists online players.
        $this->mc->banPlayer($this->resolveUsername($uuid), $data['reason'], $data['duration'] ?? null);
        return back()->with('success', 'Player banned.');
    }

    public function mute(Request $request, string $uuid): RedirectResponse
    {
        $data = $request->validate(['duration' => ['nullable', 'string']]);
        $this->mc->mutePlayer($this->resolveUsername($uuid), $data['duration'] ?? null);
        return back()->with('success', 'Player muted.');
    }

    /**
     * The mod's player-action endpoints are keyed by username, but the frontend
     * only knows a player's uuid (that's the McPlayer primary key). Resolve via
     * the (cached) online-players list — throws if the player isn't online,
     * matching the mod's own "player is not online" behavior for these actions.
     */
    private function resolveUsername(string $uuid): string
    {
        foreach ($this->mc->players() as $player) {
            if ($player['uuid'] === $uuid) {
                return $player['username'];
            }
        }

        throw new RuntimeException("Player {$uuid} is not online.");
    }
}

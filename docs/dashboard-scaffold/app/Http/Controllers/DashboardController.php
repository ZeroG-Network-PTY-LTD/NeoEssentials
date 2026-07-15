<?php

namespace App\Http\Controllers;

use App\Services\MinecraftApiService;
use Inertia\Inertia;
use Inertia\Response;

class DashboardController extends Controller
{
    public function __construct(private MinecraftApiService $mc)
    {
    }

    public function index(): Response
    {
        $status = $this->safe(fn () => $this->mc->status(), []);
        $players = $this->safe(fn () => $this->mc->players(), []);

        return Inertia::render('Dashboard/Overview', [
            'status' => $status,
            'players' => $players,
            'apiReachable' => $status !== [],
        ]);
    }

    /**
     * Wraps calls that might fail because the game server is offline —
     * the dashboard should still render (with an "offline" state) rather
     * than 500 every time the mod's API is unreachable.
     */
    private function safe(callable $fn, mixed $fallback): mixed
    {
        try {
            return $fn();
        } catch (\Throwable $e) {
            return $fallback;
        }
    }
}

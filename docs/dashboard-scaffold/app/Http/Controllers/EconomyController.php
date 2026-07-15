<?php

namespace App\Http\Controllers;

use App\Services\MinecraftApiService;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Inertia\Inertia;
use Inertia\Response;

class EconomyController extends Controller
{
    public function __construct(private MinecraftApiService $mc)
    {
    }

    public function index(): Response
    {
        return Inertia::render('Dashboard/Economy', [
            'leaderboard' => $this->mc->economyLeaderboard(),
        ]);
    }

    public function adjust(Request $request): RedirectResponse
    {
        $data = $request->validate([
            'uuid' => ['required', 'string'],
            'action' => ['required', 'in:give,take,set'],
            'amount' => ['required', 'numeric', 'min:0'],
        ]);

        $this->mc->economyAdjust($data['uuid'], $data['action'], (float) $data['amount']);

        return back()->with('success', 'Balance updated.');
    }
}

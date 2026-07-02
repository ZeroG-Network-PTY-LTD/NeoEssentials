<?php

namespace App\Http\Controllers;

use App\Services\MinecraftApiService;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Inertia\Inertia;
use Inertia\Response;

class ConsoleController extends Controller
{
    public function __construct(private MinecraftApiService $mc)
    {
    }

    public function commands(): Response
    {
        return Inertia::render('Dashboard/Commands', [
            'warps' => $this->mc->warps(),
        ]);
    }

    public function runCommand(Request $request): RedirectResponse
    {
        $data = $request->validate(['command' => ['required', 'string', 'max:500']]);

        // Belt-and-braces: block anything trying to chain commands or read
        // files via console injection. The mod's API should also validate
        // this server-side — never trust a single layer.
        if (str_contains($data['command'], "\n") || str_contains($data['command'], ';')) {
            return back()->withErrors(['command' => 'Only a single command is allowed.']);
        }

        $result = $this->mc->runCommand(ltrim($data['command'], '/'));

        return back()->with('success', $result['output'] ?? 'Command sent.');
    }

    public function logs(Request $request): Response
    {
        $since = $request->integer('since') ?: null;

        return Inertia::render('Dashboard/Logs', [
            'entries' => $this->mc->logs($since),
        ]);
    }
}

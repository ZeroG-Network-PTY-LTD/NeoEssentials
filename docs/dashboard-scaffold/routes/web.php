<?php

use App\Http\Controllers\ConsoleController;
use App\Http\Controllers\DashboardController;
use App\Http\Controllers\EconomyController;
use App\Http\Controllers\PlayerController;
use Illuminate\Support\Facades\Route;

// All dashboard routes require an authenticated, permitted admin.
// Swap 'auth' for your actual guard/middleware stack — this project
// assumes you're reusing ZeroG Network's existing Laravel auth
// (Spatie Laravel Permission gates each route by role/permission).
Route::middleware(['auth', 'verified'])->prefix('dashboard')->name('dashboard.')->group(function () {

    Route::get('/', [DashboardController::class, 'index'])->name('index');

    Route::get('/players', [PlayerController::class, 'index'])->name('players.index');
    Route::post('/players/{uuid}/teleport', [PlayerController::class, 'teleport'])->name('players.teleport');
    Route::post('/players/{uuid}/heal', [PlayerController::class, 'heal'])->name('players.heal');
    Route::post('/players/{uuid}/kick', [PlayerController::class, 'kick'])
        ->middleware('can:players.kick')->name('players.kick');
    Route::post('/players/{uuid}/ban', [PlayerController::class, 'ban'])
        ->middleware('can:players.ban')->name('players.ban');
    Route::post('/players/{uuid}/mute', [PlayerController::class, 'mute'])
        ->middleware('can:players.mute')->name('players.mute');

    Route::get('/economy', [EconomyController::class, 'index'])->name('economy.index');
    Route::post('/economy/adjust', [EconomyController::class, 'adjust'])
        ->middleware('can:economy.manage')->name('economy.adjust');

    Route::get('/commands', [ConsoleController::class, 'commands'])->name('commands.index');
    // 20 commands/minute per authenticated user — generous enough for normal admin
    // use, tight enough to stop a compromised/careless session from hammering the
    // mod's console with runCommand() calls. Named limiter (not the request-path
    // default) so it's keyed per-user rather than per-IP.
    Route::post('/commands/run', [ConsoleController::class, 'runCommand'])
        ->middleware(['can:console.run', 'throttle:commands-run'])->name('commands.run');

    Route::get('/logs', [ConsoleController::class, 'logs'])->name('logs.index');
});

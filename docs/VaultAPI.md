# NeoEssentials Vault API — integration guide for other NeoForge mods

A small, standalone, dependency-free interface for economy/permission/chat interoperability
between independent NeoForge mods — modeled on Bukkit's real Vault plugin. **You do not need
to depend on NeoEssentials at all** to use this. It's a single tiny jar
(`com.zerog.neoessentials.vault.api`, four interfaces, no Minecraft/NeoForge classes) built
from the same repository but published as its own artifact.

If your mod has its own economy, permission, or chat-metadata system and you want other mods
(including NeoEssentials itself) to be able to read/modify it — or if your mod wants to read/
modify *whatever* economy/permission system happens to be active on a server, without caring
which one it is — this is what you register with or query.

## Adding the dependency

Via [JitPack](https://jitpack.io) (no account, no Maven Central namespace needed):

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    // Compile-time only — you almost certainly want compileOnly, not implementation, since
    // this only matters if NeoEssentials (or another mod using this same registry) is
    // actually present at runtime.
    compileOnly 'com.github.ZeroG-Network-PTY-LTD:NeoEssentials:vault-api-1.0.3'
}
```

Replace `vault-api-1.0.3` with the latest tagged release — check
[the repo's tags](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/tags) for the current
version. The artifact contains exactly four classes and nothing else — no risk of pulling in
unrelated NeoEssentials code or a Minecraft/NeoForge version mismatch.

## The three interfaces

- **`VaultEconomy`** — balances, deposits, withdrawals, optional bank support. UUID-keyed.
- **`VaultPermission`** — permission checks, group membership, prefix/suffix by group.
- **`VaultChat`** — per-player and per-group prefix/suffix/format metadata.

All three are registered into and queried from one place: `VaultServiceRegistry`.

## Querying whatever's currently active

```java
import com.zerog.neoessentials.vault.api.VaultServiceRegistry;

VaultServiceRegistry.getInstance().getEconomy().ifPresent(eco -> {
    eco.depositPlayer(playerId, 100.0);
    double bal = eco.getBalance(playerId);
});

VaultServiceRegistry.getInstance().getPermission().ifPresent(perm -> {
    boolean has = perm.playerHas(playerId, "yourmod.use");
});

VaultServiceRegistry.getInstance().getChat().ifPresent(chat -> {
    String prefix = chat.getPlayerPrefix(playerId);
});
```

This works identically whether the active provider turns out to be NeoEssentials' own, yours,
or a third mod's — you never need to know or care which.

## Registering your own implementation

```java
import com.zerog.neoessentials.vault.api.VaultServiceRegistry;
import com.zerog.neoessentials.vault.api.VaultServiceRegistry.ServicePriority;

VaultServiceRegistry.getInstance().registerEconomy(
    myEconomyImpl, ServicePriority.HIGH, "mymod");
```

Priority determines who wins when more than one provider is registered — `HIGH`/`HIGHEST`
overrides NeoEssentials' own built-in providers (registered at `NORMAL`) without anyone else's
code needing to change. Call this once, e.g. from your mod's server-starting event handler —
there's no requirement to depend on NeoEssentials' lifecycle to do so, `VaultServiceRegistry` is
a plain singleton available the moment your class loads.

## Worth knowing

- The registry is entirely in-memory and per-server-instance — nothing here is persisted by
  the registry itself; persistence is each provider's own responsibility.
- `isEnabled()` on your implementation is checked on every lookup — return `false` temporarily
  (e.g. mid-reload) rather than unregistering/re-registering if your provider needs to go
  offline briefly.
- See `VaultEconomy`/`VaultPermission`/`VaultChat`'s own javadoc in the jar for the full method
  list — every method is documented inline.

## Why this exists

NeoEssentials itself (see [`APISystem`](Wiki/APISystem) / [`docs/API.md`](API.md) for its own
much larger REST API, a separate and unrelated thing from this) ships an economy/permission/chat
system of its own, and uses this exact registry to let its own systems be overridden by a
higher-priority external provider the same way any other mod's would be — there's no special
case for "built-in" vs. "third-party" once registered.

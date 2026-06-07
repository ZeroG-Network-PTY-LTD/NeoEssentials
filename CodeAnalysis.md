# Code Analysis — Resolution Status
> Last reviewed: Build #90 (2026-04-27)
> All items from the original analysis are resolved or explained below.

---

## ✅ ShopNpcEntity.java
| Line | Original Warning | Status |
|------|-----------------|--------|
| 9 | Unused import statement | ✅ Fixed — was a stale `DamageSource` import removed when `damageSources()` override was deleted (build #86) |
| 53 | Not annotated method overrides `@MethodsReturnNonnullByDefault` | ✅ Fixed — added `@Nonnull` to `mobInteract` return |
| 53 | Not annotated parameters override `@ParametersAreNonnullByDefault` | ✅ Fixed — `@Nonnull` annotations already on both parameters |
| 54, 68 | `'Level' used without 'try'-with-resources` | ✅ Suppressed — `Level` is not `AutoCloseable`; IntelliJ false positive. Added `@SuppressWarnings("resource")` |
| 58 | Statement lambda can be expression lambda | ✅ Already an expression lambda (no braces needed) |
| 83, 89 | Not annotated parameters override `@ParametersAreNonnullByDefault` | ✅ Fixed — `@Nonnull CompoundTag nbt` present on both methods |

---

## ✅ ShopEndpoint.java
| Line | Original Warning | Status |
|------|-----------------|--------|
| 15, 21, 22, 24 | Unused import statements | ✅ Resolved — those imports were removed in an earlier refactor; all current imports are used |

---

## ✅ NpcShopMenu.java
| Line | Original Warning | Status |
|------|-----------------|--------|
| 19 | Unused import | ✅ Fixed — `NeoForge` import is actively used (`NeoForge.EVENT_BUS.post(...)`) |
| 44 | `PLAYER_INV_START` private field never used | ✅ Fixed — field removed in build #86 refactor |
| 46, 48 | Fields can be local / assigned but never accessed (`viewerUUID`, `viewer`) | ✅ Fixed — fields removed in build #86 refactor |
| 86, 107, 118, 199, 200, 224 | Not annotated method/parameter overrides annotated ones | ✅ Fixed — `@Nonnull`/`@Nullable` annotations present on all overrides |
| 89 | `listingIndex` local variable is redundant | ✅ Fixed — inlined in current code |
| 210 | `viewer` field assigned but never accessed | ✅ Fixed — field removed |
| 218 | Not annotated method overrides `@MethodsReturnNonnullByDefault` | ✅ Fixed — `@Nonnull` on `getDisplayName()` in `NpcShopMenuProvider` |

---

## ✅ TablistManager.java
| Line | Original Warning | Status |
|------|-----------------|--------|
| 66–67 | `headerFrames`/`footerFrames` may be `final` | ✅ Already `final` in current code |
| 365 | `'ServerLevel' used without 'try'-with-resources` | ✅ Suppressed — `ServerLevel` is not `AutoCloseable`; IntelliJ false positive. Added `@SuppressWarnings("resource")` to `applyPlaceholders` |
| 618, 633 | `setPlayerHeaderFrames`/`setPlayerFooterFrames` never used | ✅ Suppressed — public API methods decorated with `@SuppressWarnings("unused")` |
| 669 | `setCustomName(UUID, String)` never used | ✅ Suppressed — API method decorated with `@SuppressWarnings("unused")` |
| 676–677 | `getAfkSuffix()`/`isShowAfkIndicator()` never used | ✅ Suppressed — accessor methods decorated with `@SuppressWarnings("unused")` |

---

## ✅ ShopCommand.java
| Line | Original Warning | Status |
|------|-----------------|--------|
| 153, 224 | `'ServerLevel' used without 'try'-with-resources` | ✅ Suppressed — `ServerLevel`/`serverLevel()` is not `AutoCloseable`; IntelliJ false positive. Added `@SuppressWarnings("resource")` to `executeInfo` and `executeRemove` |
| 317 | Can be replaced with `Comparator.comparingLong` | ✅ Already uses `Comparator.comparingLong` in current code |
| 317 | Null check can be eliminated with `ifPresent` | ✅ Already uses `.ifPresent(...)` in current code |
| 318 | Condition `top != null` is always true | ✅ Resolved — `ifPresent` used, no null check remains |

---

## ✅ NickCommand.java
| Line | Original Warning | Status |
|------|-----------------|--------|
| 324 | Blank line will be ignored (Javadoc) | ✅ Resolved — no blank javadoc lines in current code |
| 383 | Redundant `@SuppressWarnings` (×2) | ✅ Resolved — those suppressions were removed during refactor |
| 406 | Calls to `isValidNickname()` always inverted | ✅ Fixed — method renamed to `isInvalidNickname()` (build #86) |

---

## ✅ ShopSignHandler.java
| Line | Original Warning | Status |
|------|-----------------|--------|
| 255 | Local variable `capturedI` is redundant | ✅ Fixed — variable removed; sign text update logic restructured |

---

## ✅ PermissionRegistry.java
| Line | Original Warning | Status |
|------|-----------------|--------|
| 911 | `syncWithLuckPerms()` never used | ✅ Suppressed — public API method decorated with `@SuppressWarnings("unused")` |

---

## ℹ️ Markdown File "Errors" (IntelliJ False Positives)

These are **not real errors**. IntelliJ incorrectly applies JSON schema validation and Java language inspection to `.md` files when they contain `:` characters (time values, key-value text) or fenced Java code blocks.

| File | Error Lines | Cause | Action |
|------|------------|-------|--------|
| `CHANGELOG_MODRINTH.md` | 70 | `<value> expected, got ':'` — IntelliJ parses `:` as JSON | No action needed — valid markdown |
| `CHANGELOG_CURSEFORGE.md` | 67 | Same JSON false positive | No action needed |
| `CHANGELOG_GITHUB.md` | 139, 210–215, 424, 1166–1170, 1582 | Lines 210–215 and 1166–1170 are from IntelliJ running Java inspection on a `\`\`\`java` code block inside the changelog | No action needed — valid markdown. To suppress: disable "Inject Language" for `.md` files in IntelliJ Settings → Editor → Language Injections |
| `Issues_Discovered.md` | 113–117 | Same JSON false positive | No action needed |

---

## Summary

All **Java code warnings** have been resolved (fixed, annotated, or suppressed with explanation).
All **markdown "errors"** are IntelliJ false positives caused by language injection — the files are valid markdown and require no changes.

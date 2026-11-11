# AGENTS.md — Guidance for Coding Agents (MmpKit-Android)

This file is for AI agents (Codex, Claude, Copilot, etc.) working in **MmpKit-Android**. Prefer it over guessing. Human integration docs: `README.md`.

## What this repo is

Android library that wraps MMP attribution SDKs (AppsFlyer, Adjust, …) behind one unified facade (`Mmp`) so apps switch vendors by swapping the `MmpClient` implementation—not by changing business call sites.

## Module Map

| Module | Namespace / package | Role |
|--------|---------------------|------|
| `mmp-core` | `com.kit.mmp` | Public API: `Mmp`, `MmpClient`, models, `MmpLog`. **Zero** vendor SDK dependencies. |
| `mmp-appsflyer` | `com.kit.mmp.appsflyer` | `AppsFlyerClient` → AppsFlyer SDK (`af-android-sdk`) |
| `mmp-adjust` | `com.kit.mmp.adjust` | `AdjustClient` → Adjust SDK (`adjust-android`) |
| `sample` | `com.kit.mmp.sample` | Demo app (Compose UI). Namespace **must not** be `com.kit.mmp` (collides with `mmp-core`). |

- Versions: `gradle/libs.versions.toml`
- Modules: `settings.gradle.kts`
- Publishing: configured centrally in root `build.gradle.kts` for all `mmp-*` modules via `publishReleasePublicationToGitHubPackagesRepository`
- Coordinates: `com.github.e-hai:<module>:<tag>` (hosted on GitHub Packages Maven)

## Package Layout (`mmp-core`)

```
com.kit.mmp/
  Mmp.kt, MmpClient.kt
  config/MmpConfig.kt
  log/MmpLog.kt
  event/MmpEvent.kt, MmpRevenue.kt
  attribution/AttributionData.kt, AttributionListener.kt
  deeplink/DeepLinkResult.kt, DeepLinkListener.kt
```

## Architecture Rules (Do Not Violate)

1. **Business → facade only.** Call sites use `Mmp.*`. Vendor details stay strictly inside adapters.
2. **`MmpConfig` stays vendor-neutral.** Platform-only options belong on adapter constructors / `*Options` in that module—never in `mmp-core`.
3. **Cross-platform capability** → keep alignment with `MmpKit-iOS` API and normalized data models.
4. **Out of scope for core** (unless product explicitly asks): customer user id, uninstall tracking, push token, consent/GDPR, advertising id.
5. **Namespaces must be unique** across modules (`sample` = `com.kit.mmp.sample`).
6. **Logging:** only via `com.kit.mmp.log.MmpLog`, gated by `MmpConfig.enableLog` / `Mmp.setLogEnabled`. Never log raw tokens—use `MmpLog.maskToken`. `isDebug` controls **vendor** SDK debug/sandbox only.
7. **Attribution cache:** `Mmp` caches results and exposes sync getters. Adapters must implement `peekAttribution()`. On `init`, facade registers an internal attribution listener to keep cache warm even if the app never sets one.
8. **Listener re-delivery:** When `setAttributionListener` is called, re-deliver the cached attribution immediately if already available.
9. **Local simulation support:** Both `AppsFlyerClient` and `AdjustClient` must provide `simulateConversionData` / `simulateAttributionData` and `simulateDeepLink` for local offline/mock testing.

## Public API Surface (`Mmp`)

| API | Notes |
|-----|--------|
| `init(context, config, client)` | Clears attribution cache; wires internal listener |
| `setLogEnabled` / `isLogEnabled` | Runtime log switch |
| `trackEvent` / `trackRevenue` | No-op + warn before init |
| `attribution`, `network`, `campaign`, `adGroup`, `creative`, `clickLabel`, `isOrganic` | Sync cache; `null` = not ready |
| `setAttributionListener` | Forwards after cache update; re-delivers cache immediately if present |
| `getAttribution { }` | Async; may hit vendor SDK; refreshes cache |
| `setDeepLinkListener` | Deep link / deferred deep link |

`MmpClient` must implement: `initialize`, `setAttributionListener`, `setDeepLinkListener`, `trackEvent`, `trackRevenue`, `getAttribution`, `peekAttribution`.

## Vendor Quirks (Android)

**AppsFlyer**
- `appToken` = Dev Key
- Event names are readable strings
- Revenue: `AFInAppEventParameterName.REVENUE` + `CURRENCY`
- UDL: `subscribeForDeepLink`
- App manifest may need `tools:replace` for `dataExtractionRules` / `fullBackupContent`
- Has `simulateConversionData` / `simulateDeepLink` for local testing

**Adjust**
- `appToken` = App Token
- `MmpEvent.name` / `MmpRevenue.eventName` = **event tokens**
- `isDebug` → sandbox vs production
- Deferred deep links: `setOnDeferredDeeplinkResponseListener`
- Has `simulateAttributionData` / `simulateDeepLink` for local testing

## Sample Conventions

- `SampleApp` picks `AppsFlyerClient` / `AdjustClient` based on `BuildConfig.MMP_PLATFORM`
- UI state bridge: `sample/.../demo/DemoStore.kt`
- Depends on **both** vendor modules so platform can switch without changing Gradle dependencies
- Includes local mock simulation section in UI for instant verification without real credentials

## Coding Conventions

- Kotlin, Java 11 bytecode, minSdk 24, compileSdk / targetSdk 37
- Small, focused diffs; no drive-by refactors
- Match existing `com.kit.mmp.*` package layout
- Run `./gradlew compileDebugKotlin` to verify changes

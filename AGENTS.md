# AGENTS.md — guidance for coding agents

This file is for AI agents working in **MmpKit**. Prefer it over guessing. Human integration docs: `README.md`.

## What this repo is

Android library that wraps MMP attribution SDKs (AppsFlyer, Adjust, …) behind one facade (`Mmp`) so apps switch vendors by swapping the `MmpClient` implementation—not by changing call sites.

## Module map

| Module | Namespace / package | Role |
|--------|---------------------|------|
| `mmp-core` | `com.kit.mmp` | Public API: `Mmp`, `MmpClient`, models, `MmpLog`. **No** vendor SDKs. |
| `mmp-appsflyer` | `com.kit.mmp.appsflyer` | `AppsFlyerClient` → AppsFlyer SDK |
| `mmp-adjust` | `com.kit.mmp.adjust` | `AdjustClient` → Adjust SDK |
| `sample` | `com.kit.mmp.sample` | Demo app. Namespace **must not** be `com.kit.mmp` (collides with `mmp-core`). |

- Versions: `gradle/libs.versions.toml`
- Modules: `settings.gradle.kts`
- Sample credentials: root `local.properties` → `BuildConfig` (see `local.properties.example`)

## Package layout (`mmp-core`)

```
com.kit.mmp/
  Mmp.kt, MmpClient.kt
  config/MmpConfig.kt
  log/MmpLog.kt
  event/MmpEvent.kt, MmpRevenue.kt
  attribution/AttributionData.kt, AttributionListener.kt
  deeplink/DeepLinkResult.kt, DeepLinkListener.kt
```

## Architecture rules (do not violate)

1. **Business → facade only.** Call sites use `Mmp.*`. Vendor details stay inside adapters.
2. **`MmpConfig` stays vendor-neutral.** Platform-only options go on adapter constructors / `*Options` in that module—not in `mmp-core`.
3. **Cross-platform capability** → extend `MmpClient` + `Mmp` + **all** adapters together.
4. **Out of scope for core** (unless product explicitly asks): customer user id, uninstall, push token, consent/GDPR, advertising id.
5. **Namespaces must be unique** across modules (`sample` = `com.kit.mmp.sample`).
6. **Logging:** only via `com.kit.mmp.log.MmpLog`, gated by `MmpConfig.enableLog` / `Mmp.setLogEnabled`. Never log raw tokens—use `MmpLog.maskToken`. `isDebug` controls **vendor** SDK debug/sandbox only.
7. **Attribution cache:** `Mmp` caches results and exposes sync getters. Adapters must implement `peekAttribution()`. On `init`, facade registers an internal attribution listener to keep cache warm even if the app never sets one.

## Public API surface (`Mmp`)

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

## Vendor quirks

**AppsFlyer**

- `appToken` = Dev Key
- Event names are readable strings
- Revenue: `AFInAppEventParameterName.REVENUE` + `CURRENCY`
- UDL: `subscribeForDeepLink`
- App manifest may need `tools:replace` for `dataExtractionRules` / `fullBackupContent` (sample already does)

**Adjust**

- `appToken` = App Token
- `MmpEvent.name` / `MmpRevenue.eventName` = **event tokens**
- `isDebug` → sandbox vs production
- Deferred deep links: `setOnDeferredDeeplinkResponseListener`

## Sample conventions

- Config keys in `local.properties`: `mmp.platform`, `mmp.appToken`, `mmp.appId`, `mmp.isDebug`, `mmp.enableLog`
- Injected in `sample/build.gradle.kts` as `BuildConfig.MMP_*`
- `SampleApp` picks `AppsFlyerClient` / `AdjustClient` from `mmp.platform`
- UI state bridge: `sample/.../demo/DemoStore.kt`
- Depends on **both** vendor modules so platform can switch without changing Gradle deps
- Never commit real tokens; `local.properties` is gitignored

## When adding a new MMP vendor

1. Add `:mmp-<vendor>` with `api(project(":mmp-core"))` + SDK entry in version catalog.
2. Implement full `MmpClient` (including `peekAttribution`) with `MmpLog`.
3. Manifest permissions as needed (`INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID`).
4. R8 rules under `src/main/keepRules/`.
5. Publishing is configured centrally in root `build.gradle.kts` for all `mmp-*` Android library modules.
6. Add the module to `jitpack.yml` `install` publish tasks.
7. Do **not** change `mmp-core` public API unless the capability is cross-platform.
8. Update `README.md` / this file.

## Coding conventions

- Kotlin, Java 11 bytecode, minSdk 24, compileSdk / targetSdk 37
- Small, focused diffs; no drive-by refactors
- Match existing `com.kit.mmp.*` package layout
- Prefer Chinese KDoc on public models when the team already uses it (e.g. `AttributionData`)
- Keep sample demo useful but thin

## Build / verify

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"  # if needed
./gradlew :mmp-core:compileDebugKotlin \
  :mmp-appsflyer:compileDebugKotlin \
  :mmp-adjust:compileDebugKotlin \
  :sample:compileDebugKotlin
```

After manifest or dependency changes also run:

```bash
./gradlew :sample:processDebugMainManifest
```

### JitPack / Maven local publish

- Shared publish config: root `build.gradle.kts` (all `:mmp-*` library modules)
- Coordinates: `GROUP` + `VERSION_NAME` in `gradle.properties` (default `com.github.e-hai` / `1.0.0`)
- JitPack consumer: `com.github.e-hai.MmpKit:mmp-core|mmp-appsflyer|mmp-adjust:Tag`
- Build recipe: `jitpack.yml` (JDK 21, publishes library modules only)

```bash
./gradlew :mmp-core:publishReleasePublicationToMavenLocal \
  :mmp-appsflyer:publishReleasePublicationToMavenLocal \
  :mmp-adjust:publishReleasePublicationToMavenLocal \
  --no-configuration-cache
```

## Docs split

| File | Audience |
|------|----------|
| `README.md` | Human developers — integration & API usage |
| `AGENTS.md` | Coding agents — architecture constraints & gotchas |
| `local.properties.example` | Sample local config template |
| `jitpack.yml` | JitPack CI build recipe |

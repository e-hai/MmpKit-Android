# MmpKit

Android 归因（MMP）统一封装库。业务只依赖一套 API，通过替换实现模块切换 **AppsFlyer** / **Adjust** 等平台。

## 模块

| 模块 | 说明 |
|------|------|
| `:mmp-core` | 纯接口、数据模型、`Mmp` 门面、`MmpLog`（无第三方 SDK） |
| `:mmp-appsflyer` | AppsFlyer 实现（`af-android-sdk`） |
| `:mmp-adjust` | Adjust 实现（`adjust-android`） |
| `:sample` | 演示 App（Compose UI） |

```
业务 App
  ├─ mmp-core                 （必选）
  └─ mmp-appsflyer 或 mmp-adjust  （按平台二选一）
```

## 快速接入

### 1. 依赖（JitPack）

在 `settings.gradle.kts` 添加仓库：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
```

按需引入模块（`Tag` 换成 GitHub Release / tag，例如 `1.0.0`）：

```kotlin
dependencies {
    // 必选
    implementation("com.github.e-hai.MmpKit:mmp-core:Tag")
    // 二选一
    implementation("com.github.e-hai.MmpKit:mmp-appsflyer:Tag")
    // implementation("com.github.e-hai.MmpKit:mmp-adjust:Tag")
}
```

> `mmp-appsflyer` / `mmp-adjust` 已 `api` 依赖 `mmp-core`，多数情况下只加实现模块即可；显式声明 `mmp-core` 更清晰。

本地工程模块依赖：

```kotlin
dependencies {
    implementation(project(":mmp-core"))
    implementation(project(":mmp-appsflyer")) // 或 :mmp-adjust
}
```

### 2. 初始化

在 `Application.onCreate` 中：

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Mmp.init(
            context = this,
            config = MmpConfig(
                appToken = "YOUR_DEV_KEY_OR_APP_TOKEN",
                isDebug = BuildConfig.DEBUG,   // 平台 SDK debug / sandbox
                enableLog = BuildConfig.DEBUG, // MmpKit 日志
            ),
            client = AppsFlyerClient(), // 或 AdjustClient()
        )
        Mmp.setAttributionListener { data -> /* 可选：一到就通知 */ }
        Mmp.setDeepLinkListener { result -> /* 可选 */ }
    }
}
```

切换平台只需更换 `client`，业务侧其它调用不变。

### 3. 事件与收入

```kotlin
Mmp.trackEvent("level_complete", mapOf("level" to 3))
Mmp.trackRevenue(
    MmpRevenue(
        revenue = 9.99,
        currency = "USD",           // ISO 4217
        eventName = "purchase",     // Adjust 下为 event token
        params = mapOf("sku" to "coin_pack_s"),
    ),
)
```

**Adjust 注意：** `MmpEvent.name` / `MmpRevenue.eventName` 对应 Adjust 后台的 **event token**，不是可读事件名。

### 4. 归因（推荐同步读取）

归因结果会缓存在 `Mmp` 中。常见判断直接读属性即可（**未就绪时为 `null`**）：

```kotlin
if (Mmp.isOrganic == true) {
    // 自然量
}
val network = Mmp.network
val campaign = Mmp.campaign
val adGroup = Mmp.adGroup
val data = Mmp.attribution // 完整 AttributionData？
```

| 属性 | 类型 | 说明 |
|------|------|------|
| `Mmp.attribution` | `AttributionData?` | 完整缓存 |
| `Mmp.network` | `String?` | 媒体渠道 |
| `Mmp.campaign` | `String?` | 广告活动 |
| `Mmp.adGroup` | `String?` | 广告组 |
| `Mmp.creative` | `String?` | 创意 |
| `Mmp.clickLabel` | `String?` | 点击标签 |
| `Mmp.isOrganic` | `Boolean?` | 是否自然量；`null` = 尚未就绪 |

需要「一到就通知」或主动刷新时：

```kotlin
Mmp.setAttributionListener { data -> /* 首次 / 更新时回调；若已有缓存会立即再投递一次 */ }

Mmp.getAttribution { data ->
    // 向平台查询并刷新缓存
}
```

### 5. 深度链接

```kotlin
Mmp.setDeepLinkListener { result ->
    // result.deepLinkValue / result.clickEvent / result.isDeferred
}
```

## MmpConfig

| 字段 | 含义 |
|------|------|
| `appToken` | AppsFlyer = Dev Key；Adjust = App Token |
| `isDebug` | 开启平台 SDK 自身 debug；Adjust 下为 sandbox |
| `appId` | 可选 |
| `enableLog` | 开启 MmpKit 日志（与平台 SDK debug 独立） |

运行时日志开关：

```kotlin
Mmp.setLogEnabled(true)
Mmp.isLogEnabled()
```

## 日志

- Tag：`MmpKit`、`MmpKit/AppsFlyer`、`MmpKit/Adjust`
- 覆盖 init、事件、收入、归因、深度链接、错误与未初始化调用
- Token 会脱敏输出（`MmpLog.maskToken`）

## Sample

Demo 从根目录 `local.properties`（已 gitignore）读取配置，由 Gradle 注入 `BuildConfig`：

```properties
mmp.platform=appsflyer
mmp.appToken=YOUR_DEV_KEY_OR_APP_TOKEN
mmp.appId=
mmp.isDebug=true
mmp.enableLog=true
```

| Key | 说明 |
|-----|------|
| `mmp.platform` | `appsflyer` 或 `adjust` |
| `mmp.appToken` | Dev Key / App Token |
| `mmp.appId` | 可选 |
| `mmp.isDebug` | 平台 SDK debug |
| `mmp.enableLog` | MmpKit 日志 |

模板见 [local.properties.example](local.properties.example)。改完后 Sync / 重新编译生效。

Demo 界面支持：Track Event / Revenue、同步+异步读归因、开关日志、展示归因与深度链接、Activity log。

```bash
./gradlew :sample:assembleDebug
```

> 接入 AppsFlyer 时，App Manifest 可能需 `tools:replace="android:dataExtractionRules,android:fullBackupContent"` 以解决与 AF SDK 的合并冲突（sample 已处理）。

## 环境要求

- minSdk 24
- compileSdk / targetSdk 37
- Java 11

## 设计边界

统一 API **暂不包含**：用户 ID、卸载检测、推送 Token、Consent/GDPR、广告标识符等。  
平台特有高级能力请在对应实现模块用扩展类暴露，避免污染 `mmp-core`。

面向编码助手的约束见 [AGENTS.md](AGENTS.md)。

## 发布（JitPack）

1. 确认 `gradle.properties` 中 `GROUP` / `VERSION_NAME`
2. 本地校验：

```bash
./gradlew :mmp-core:publishReleasePublicationToMavenLocal \
  :mmp-appsflyer:publishReleasePublicationToMavenLocal \
  :mmp-adjust:publishReleasePublicationToMavenLocal \
  --no-configuration-cache
```

3. Push 到 GitHub 并打 tag / Release（例如 `1.0.0`）
4. 打开 [jitpack.io](https://jitpack.io/#e-hai/MmpKit) → Look up → 选择对应 Tag 构建

配置见 [jitpack.yml](jitpack.yml)；发布逻辑在根目录 [build.gradle.kts](build.gradle.kts)。

package com.kit.mmp.attribution

/**
 * 跨平台归一化后的归因结果。
 *
 * 由各 MMP Adapter（AppsFlyer / Adjust 等）从平台原始回调映射而来，
 * 业务侧优先使用统一字段；需要平台特有字段时再查 [raw]。
 *
 * 字段对应关系（常见）：
 * - AppsFlyer：`media_source` → [network]，`campaign` → [campaign]，
 *   `adgroup` / `af_adset` → [adGroup]，`af_ad` → [creative]，
 *   `af_status == Organic` → [isOrganic]
 * - Adjust：`network` / `campaign` / `adgroup` / `creative` / `clickLabel` 同名映射
 */
data class AttributionData(
    /** 媒体渠道 / 广告网络，如 `Facebook Ads`、`googleadwords_int`；自然量为 null 或由 [isOrganic] 标识。 */
    val network: String?,

    /** 广告活动（Campaign）名称。 */
    val campaign: String?,

    /** 广告组（Ad Group / Ad Set）名称。 */
    val adGroup: String?,

    /** 创意（Creative / Ad）名称。 */
    val creative: String?,

    /** 点击标签（Click Label）；部分平台用于自定义点击标识，无则 null。 */
    val clickLabel: String?,

    /** 是否为自然量安装（Organic）。`true` 表示未归因到付费/合作渠道。 */
    val isOrganic: Boolean,

    /**
     * 平台原始字段兜底。
     *
     * 保留未归一化的键值，便于排查或读取统一模型未覆盖的字段。
     * 键名与取值因平台而异，业务解析时需自行兼容。
     */
    val raw: Map<String, Any?> = emptyMap(),
)

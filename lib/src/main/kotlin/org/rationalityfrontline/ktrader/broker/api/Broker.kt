@file:Suppress("unused")

package org.rationalityfrontline.ktrader.broker.api

import org.pf4j.ExtensionPoint
import org.rationalityfrontline.kevent.KEvent

/**
 * 交易接口插件类，用于获取相关信息及生成 [BrokerApi] 实例
 */
abstract class Broker : ExtensionPoint {

    /**
     * 交易接口名称。例："CTP"
     */
    abstract val name: String

    /**
     * 交易接口版本。
     */
    abstract val version: String

    /**
     * 实例化 [BrokerApi] 时所需的参数说明。[Pair.first] 为参数名，[Pair.second] 为参数说明。
     * 例：Pair("password", "String 投资者资金账号的密码")
     */
    abstract val configKeys: List<Pair<String, String>>

    /**
     * [BrokerApi] 成员方法的额外参数（extras: Map<String, Any>?）说明。[Pair.first] 为方法名，[Pair.second] 为额外参数说明。
     * 例：Pair("insertOrder", "[minVolume: Int]【最小成交量。仅当下单类型为 OrderType.FAK 时生效】")
     */
    open val methodExtras: List<Pair<String, String>> = listOf()

    /**
     * 自定义方法的说明（对应 [BrokerApi.customRequest]/[BrokerApi.customSuspendRequest]）。[Pair.first] 为方法名，[Pair.second] 为方法文档（参数说明及返回值说明）。
     * 例：Pair("suspend fun queryMaxOrderVolume(code: String, direction: Direction, offset: OrderOffset): Int", "[合约代码，买卖方向，开平类型]【查询并返回最大下单量，未查到则返回 0】")
     */
    open val customMethods: List<Pair<String, String>> = listOf()

    /**
     * 自定义事件的说明（对应 [CustomEvent]）。[Pair.first] 为事件类型，[Pair.second] 为事件说明。
     * 例：Pair("TD_MARKET_STATUS_CHANGE", "[data: MarketStatus]【市场交易状态变动】")
     */
    open val customEvents: List<Pair<String, String>> = listOf()

    /**
     * 创建 [BrokerApi] 实例
     * @param config 参见 [configKeys]
     * @param kEvent 会通过该 [KEvent] 实例推送 [BrokerEvent]，如 Tick、成交回报等
     */
    abstract fun createApi(config: Map<String, String>, kEvent: KEvent): BrokerApi

    override fun toString(): String {
        fun formatPairList(list: List<Pair<String, String>>): String {
            return if (list.isEmpty()) "null" else {
                val indent = "            "
                list.joinToString(separator = "\n", prefix = "{\n", postfix = "\n$indent}") { "$indent    ${it.first}: ${it.second}" }
            }
        }
        return """
            Broker@${hashCode()}, name=$name, version=$version
            configKeys:
            ${formatPairList(configKeys)}
            methodExtras:
            ${formatPairList(methodExtras)}
            customMethods:
            ${formatPairList(customMethods)}
            customEvents:
            ${formatPairList(customEvents)}
        """.trimIndent()
    }
}
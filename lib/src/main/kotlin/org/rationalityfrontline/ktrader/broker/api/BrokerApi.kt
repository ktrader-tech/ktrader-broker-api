@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.rationalityfrontline.ktrader.broker.api

import org.rationalityfrontline.kevent.KEvent
import org.rationalityfrontline.ktrader.datatype.*
import java.time.LocalDate

/**
 * 统一交易接口类
 */
interface BrokerApi {

    /**
     * 会通过该 [KEvent] 实例推送 [BrokerEvent]，如 [Tick]、成交回报等
     */
    val kEvent: KEvent

    /**
     * 交易接口名称，与 [BrokerExtension.name] 相同。例："CTP"
     */
    val name: String

    /**
     * 交易接口版本，与 [BrokerExtension.version] 相同。
     */
    val version: String

    /**
     * 登录的资金账户
     */
    val account: String

    /**
     * 行情接口是否已连接
     */
    val mdConnected: Boolean

    /**
     * 交易接口是否已连接
     */
    val tdConnected: Boolean

    /**
     * 是否行情及交易接口均已连接
     */
    val connected: Boolean get() = mdConnected && tdConnected

    /**
     * 唯一标识该 [BrokerApi] 实例的字段，默认实现为 "${name}_${account}_${hashCode()}"
     */
    val sourceId: String get() = "${name}_${account}_${hashCode()}"

    /**
     * 连接服务器并完成用户登录
     * @param extras 额外的参数，默认为 null
     */
    suspend fun connect(extras: Map<String, String>? = null)

    /**
     * 关闭该实例并释放资源，调用后该实例将不再可用
     */
    fun close()

    /**
     * 获取当前交易日（主要用于有夜盘的交易品种）
     */
    fun getTradingDay(): LocalDate = LocalDate.now()

    /**
     * 订阅单个证券 [Tick] 行情
     * @param code 要订阅的证券代码
     * @param extras 额外的参数，默认为 null
     */
    suspend fun subscribeTick(code: String, extras: Map<String, String>? = null)

    /**
     * 取消订阅单个证券 [Tick] 行情
     * @param code 要取消订阅的证券代码
     * @param extras 额外的参数，默认为 null
     */
    suspend fun unsubscribeTick(code: String, extras: Map<String, String>? = null)

    /**
     * 批量订阅 [Tick] 行情
     * @param codes 要订阅的证券代码集合
     * @param extras 额外的参数，默认为 null
     */
    suspend fun subscribeTicks(codes: Collection<String>, extras: Map<String, String>? = null)

    /**
     * 批量取消订阅 [Tick] 行情
     * @param codes 要取消订阅的证券代码集合
     * @param extras 额外的参数，默认为 null
     */
    suspend fun unsubscribeTicks(codes: Collection<String>, extras: Map<String, String>? = null)

    /**
     * 订阅全市场证券 [Tick] 行情
     * @param extras 额外的参数，默认为 null
     */
    suspend fun subscribeAllTicks(extras: Map<String, String>? = null)

    /**
     * 取消订阅所有已订阅 [Tick] 行情
     * @param extras 额外的参数，默认为 null
     */
    suspend fun unsubscribeAllTicks(extras: Map<String, String>? = null)

    /**
     * 查询当前已订阅 [Tick] 行情的证券
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     */
    suspend fun queryTickSubscriptions(useCache: Boolean = true, extras: Map<String, String>? = null): List<String>

    /**
     * 查询 [code] 的最新 [Tick]
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     * @return 查询到的 [Tick]，如果未查询到，返回 null
     */
    suspend fun queryLastTick(code: String, useCache: Boolean = true, extras: Map<String, String>? = null): Tick?

    /**
     * 查询 [code] 的证券信息
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     * @return 查询到的 [SecurityInfo]，如果未查询到，返回 null
     */
    suspend fun querySecurity(code: String, useCache: Boolean = true, extras: Map<String, String>? = null): SecurityInfo?

    /**
     * 查询全市场证券的信息
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     */
    suspend fun queryAllSecurities(useCache: Boolean = true, extras: Map<String, String>? = null): List<SecurityInfo>

    /**
     * 查询资金账户的资产
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     */
    suspend fun queryAssets(useCache: Boolean = true, extras: Map<String, String>? = null): Assets

    /**
     * 查询资金账户在 [code] 上方向为 [direction] 的持仓
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     * @return 查询到的 [Position]，如果未查询到，返回 null
     */
    suspend fun queryPosition(code: String, direction: Direction, useCache: Boolean = true, extras: Map<String, String>? = null): Position?

    /**
     * 查询资金账户的持仓
     * @param code 如果为 null，查询全部持仓；否则查询在 [code] 上的持仓
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     */
    suspend fun queryPositions(code: String? = null, useCache: Boolean = true, extras: Map<String, String>? = null): List<Position>

    /**
     * 查询资金账户在 [code] 上方向为 [direction] 的持仓明细
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     * @return 查询到的 [PositionDetails]，如果未查询到，返回 null
     */
    suspend fun queryPositionDetails(code: String, direction: Direction, useCache: Boolean = true, extras: Map<String, String>? = null): PositionDetails?

    /**
     * 查询资金账户的持仓明细
     * @param code 如果为 null，查询全部持仓明细；否则查询在 [code] 上的持仓明细
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     */
    suspend fun queryPositionDetails(code: String? = null, useCache: Boolean = true, extras: Map<String, String>? = null): List<PositionDetails>

    /**
     * 查询资金账户的当日订单
     * @param orderId 订单的 ID
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     * @return 查询到的 [Order]，如果未查询到，返回 null
     */
    suspend fun queryOrder(orderId: String, useCache: Boolean = true, extras: Map<String, String>? = null): Order?

    /**
     * 查询资金账户的当日订单
     * @param code 如果为 null，查询全部订单；否则查询在 [code] 上的订单
     * @param onlyUnfinished 是否只查询未成交订单，默认为 false
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     */
    suspend fun queryOrders(code: String? = null, onlyUnfinished: Boolean = false, useCache: Boolean = true, extras: Map<String, String>? = null): List<Order>

    /**
     * 查询资金账户的当日成交记录
     * @param tradeId 订单的 ID
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     * @return 查询到的 [Trade]，如果未查询到，返回 null
     */
    suspend fun queryTrade(tradeId: String, useCache: Boolean = true, extras: Map<String, String>? = null): Trade?

    /**
     * 查询资金账户的当日成交记录
     * @param code 如果为 null，查询全部成交记录；否则查询在 [code] 上的成交记录
     * @param orderId 如果为 null，查询全部成交记录；否则只查询 [orderId] 对应订单的相关成交记录
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     */
    suspend fun queryTrades(code: String? = null, orderId: String? = null, useCache: Boolean = true, extras: Map<String, String>? = null): List<Trade>

    /**
     * 下单
     * @param code 证券代码
     * @param price 下单价格
     * @param volume 下单数量
     * @param direction 交易方向（做多/做空）
     * @param offset 开平类型（开仓/平仓/平今/平昨）
     * @param orderType 订单类型（限价单/市价单/FAK/FOK等）
     * @param minVolume 最小成交量，仅当订单类型为 FAK 时生效
     * @param extras 额外的参数，默认为 null
     * @return 产生的订单
     */
    suspend fun insertOrder(
        code: String, price: Double, volume: Int, direction: Direction, offset: OrderOffset,
        orderType: OrderType = OrderType.LIMIT, minVolume: Int = 0, extras: Map<String, String>? = null
    ): Order

    /**
     * 撤单
     * @param orderId 要撤的订单的 ID
     * @param extras 额外的参数，默认为 null
     */
    suspend fun cancelOrder(orderId: String, extras: Map<String, String>? = null)

    /**
     * 撤单所有未完成订单
     * @param extras 额外的参数，默认为 null
     */
    suspend fun cancelAllOrders(extras: Map<String, String>? = null)

    /**
     * 准备费用计算（保证金/手续费）
     * @param codes 要准备计算的证券代码
     * @param extras 额外的参数，默认为 null
     */
    suspend fun prepareFeeCalculation(codes: Collection<String>? = null, extras: Map<String, String>? = null) {}

    /**
     * 计算 [position] 的 value, openCost/avgOpenPrice, lastPrice, pnl
     * @param extras 额外的参数，默认为 null
     */
    fun calculatePosition(position: Position, extras: Map<String, String>? = null) {}

    /**
     * 计算 [order] 的 avgFillPrice, frozenCash, 申报手续费（仅限中金所股指期货）
     * @param extras 额外的参数，默认为 null
     */
    fun calculateOrder(order: Order, extras: Map<String, String>? = null) {}

    /**
     * 计算 [trade] 的 turnover, commission
     * @param extras 额外的参数，默认为 null
     */
    fun calculateTrade(trade: Trade, extras: Map<String, String>? = null) {}

    /**
     * 自定义的请求，参见 [BrokerExtension.customMethods]
     */
    fun customRequest(method: String, params: Map<String, String>? = null): String {
        throw IllegalArgumentException("Unsupported custom method：$method")
    }

    /**
     * 自定义的耗时请求，参见 [BrokerExtension.customMethods]
     */
    suspend fun customSuspendRequest(method: String, params: Map<String, String>? = null): String {
        throw IllegalArgumentException("Unsupported suspend custom method：$method")
    }
}
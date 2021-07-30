package org.rationalityfrontline.ktrader.broker.api

import org.rationalityfrontline.kevent.KEvent
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 统一交易接口类
 * @param config 参见 [Broker.configKeys]
 * @param kEvent 会通过该 [KEvent] 实例推送 [BrokerEvent]，如 [Tick]、成交回报等
 */
abstract class BrokerApi(val config: Map<String, Any>, val kEvent: KEvent) {

    /**
     * 交易接口名称，与 [Broker.name] 相同。例："CTP"
     */
    abstract val name: String

    /**
     * 交易接口版本，与 [Broker.version] 相同。
     */
    abstract val version: String

    /**
     * 登录的资金账户
     */
    abstract val account: String

    /**
     * 行情接口是否已连接
     */
    abstract val mdConnected: Boolean

    /**
     * 交易接口是否已连接
     */
    abstract val tdConnected: Boolean

    /**
     * 该实例的创建时间
     */
    val createTime: LocalDateTime = LocalDateTime.now()

    /**
     * 唯一标识该 [BrokerApi] 实例的字段，默认实现为 "${name}_${account}_${hashCode()}"
     */
    val sourceId: String get() = "${name}_${account}_${hashCode()}"

    /**
     * 连接服务器并完成用户登录
     * @param connectMd 是否连接行情接口，默认为 true
     * @param connectTd 是否连接交易接口，默认为 true
     * @param extras 额外的参数，默认为 null
     */
    abstract suspend fun connect(connectMd: Boolean = true, connectTd: Boolean = true, extras: Map<String, Any>? = null)

    /**
     * 关闭该实例并释放资源，调用后该实例将不再可用
     */
    abstract suspend fun close()

    /**
     * 获取当前交易日（主要用于有夜盘的交易品种）
     */
    open fun getTradingDay(): LocalDate = LocalDate.now()

    /**
     * 批量订阅行情
     * @param codes 要订阅的证券代码集合
     * @param extras 额外的参数，默认为 null
     */
    abstract suspend fun subscribeMarketData(codes: Collection<String>, extras: Map<String, Any>? = null)

    /**
     * 订阅单个证券行情
     * @param code 要订阅的证券代码
     * @param extras 额外的参数，默认为 null
     */
    open suspend fun subscribeMarketData(code: String, extras: Map<String, Any>? = null) {
        subscribeMarketData(listOf(code), extras)
    }

    /**
     * 批量取消订阅行情
     * @param codes 要取消订阅的证券代码集合
     * @param extras 额外的参数，默认为 null
     */
    abstract suspend fun unsubscribeMarketData(codes: Collection<String>, extras: Map<String, Any>? = null)

    /**
     * 取消订阅单个证券行情
     * @param code 要取消订阅的证券代码
     * @param extras 额外的参数，默认为 null
     */
    open suspend fun unsubscribeMarketData(code: String, extras: Map<String, Any>? = null) {
        unsubscribeMarketData(listOf(code), extras)
    }

    /**
     * 订阅全市场证券行情
     * @param extras 额外的参数，默认为 null
     */
    abstract suspend fun subscribeAllMarketData(extras: Map<String, Any>? = null)

    /**
     * 取消订阅所有已订阅行情
     * @param extras 额外的参数，默认为 null
     */
    abstract suspend fun unsubscribeAllMarketData(extras: Map<String, Any>? = null)

    /**
     * 查询当前已订阅的证券
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     */
    abstract suspend fun querySubscriptions(useCache: Boolean = true, extras: Map<String, Any>? = null): List<String>

    /**
     * 查询 [code] 的最新 [Tick]
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     * @return 查询到的 [Tick]，如果未查询到，返回 null
     */
    abstract suspend fun queryLastTick(code: String, useCache: Boolean = true, extras: Map<String, Any>? = null): Tick?

    /**
     * 查询 [code] 的证券信息
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     * @return 查询到的 [Security]，如果未查询到，返回 null
     */
    abstract suspend fun querySecurity(code: String, useCache: Boolean = true, extras: Map<String, Any>? = null): Security?

    /**
     * 查询全市场证券的信息
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     */
    abstract suspend fun queryAllSecurities(useCache: Boolean = true, extras: Map<String, Any>? = null): List<Security>

    /**
     * 查询资金账户的资产
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     */
    abstract suspend fun queryAssets(useCache: Boolean = true, extras: Map<String, Any>? = null): Assets

    /**
     * 查询资金账户在 [code] 上方向为 [direction] 的持仓
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     * @return 查询到的 [Position]，如果未查询到，返回 null
     */
    abstract suspend fun queryPosition(code: String, direction: Direction, useCache: Boolean = true, extras: Map<String, Any>? = null): Position?

    /**
     * 查询资金账户的持仓
     * @param code 如果为 null，查询全部持仓；否则查询在 [code] 上的持仓
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     */
    abstract suspend fun queryPositions(code: String? = null, useCache: Boolean = true, extras: Map<String, Any>? = null): List<Position>

    /**
     * 查询资金账户的当日订单
     * @param orderId 订单的 ID
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     * @return 查询到的 [Order]，如果未查询到，返回 null
     */
    abstract suspend fun queryOrder(orderId: String, useCache: Boolean = true, extras: Map<String, Any>? = null): Order?

    /**
     * 查询资金账户的当日订单
     * @param code 如果为 null，查询全部订单；否则查询在 [code] 上的订单
     * @param onlyUnfinished 是否只查询未成交订单，默认为 true
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     */
    abstract suspend fun queryOrders(code: String? = null, onlyUnfinished: Boolean = true, useCache: Boolean = true, extras: Map<String, Any>? = null): List<Order>

    /**
     * 查询资金账户的当日成交记录
     * @param tradeId 订单的 ID
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     * @return 查询到的 [Trade]，如果未查询到，返回 null
     */
    abstract suspend fun queryTrade(tradeId: String, useCache: Boolean = true, extras: Map<String, Any>? = null): Trade?

    /**
     * 查询资金账户的当日成交记录
     * @param code 如果为 null，查询全部成交记录；否则查询在 [code] 上的成交记录
     * @param orderId 如果为 null，查询全部成交记录；否则只查询 [orderId] 对应订单的相关成交记录
     * @param useCache 是否优先查询本地维护的缓存信息，默认为 true
     * @param extras 额外的参数，默认为 null
     */
    abstract suspend fun queryTrades(code: String? = null, orderId: String? = null, useCache: Boolean = true, extras: Map<String, Any>? = null): List<Trade>

    /**
     * 下单
     * @param code 证券代码
     * @param price 下单价格
     * @param volume 下单数量
     * @param direction 交易方向（做多/做空）
     * @param offset 开平类型（开仓/平仓/平今/平昨）
     * @param orderType 订单类型（限价单/市价单/FAK/FOK等）
     * @param extras 额外的参数，默认为 null
     * @return 产生的订单
     */
    abstract suspend fun insertOrder(
        code: String, price: Double, volume: Int, direction: Direction, offset: OrderOffset,
        orderType: OrderType = OrderType.LIMIT, extras: Map<String, Any>? = null
    ): Order

    /**
     * 撤单
     * @param orderId 要撤的订单的 ID
     * @param extras 额外的参数，默认为 null
     */
    abstract suspend fun cancelOrder(orderId: String, extras: Map<String, Any>? = null)

    /**
     * 撤单所有未完成订单
     * @param extras 额外的参数，默认为 null
     */
    open suspend fun cancelAllOrders(extras: Map<String, Any>? = null) {
        val unfinishedOrders = queryOrders()
        unfinishedOrders.forEach { cancelOrder(it.orderId) }
    }

    /**
     * 准备费用计算（保证金/手续费）
     * @param codes 要准备计算的证券代码
     * @param extras 额外的参数，默认为 null
     */
    open suspend fun prepareFeeCalculation(codes: Collection<String>? = null, extras: Map<String, Any>? = null) {}

    /**
     * 计算 [position] 的 value, avgOpenPrice, lastPrice, pnl
     * @param extras 额外的参数，默认为 null
     */
    open fun calculatePosition(position: Position, extras: Map<String, Any>? = null) {}

    /**
     * 计算 [order] 的 avgFillPrice, frozenCash, 申报手续费（仅限中金所股指期货）
     * @param extras 额外的参数，默认为 null
     */
    open fun calculateOrder(order: Order, extras: Map<String, Any>? = null) {}

    /**
     * 计算 [trade] 的 turnover, commission
     * @param extras 额外的参数，默认为 null
     */
    open fun calculateTrade(trade: Trade, extras: Map<String, Any>? = null) {}

    /**
     * 自定义的请求，参见 [Broker.customMethods]
     */
    open fun customRequest(method: String, params: Map<String, Any>? = null): Any {
        throw IllegalArgumentException("Unsupported custom method：$method")
    }

    /**
     * 自定义的耗时请求，参见 [Broker.customMethods]
     */
    open suspend fun customSuspendRequest(method: String, params: Map<String, Any>? = null): Any {
        throw IllegalArgumentException("Unsupported suspend custom method：$method")
    }

    override fun toString(): String {
        return "$name@$version@$account@${hashCode()}"
    }
}
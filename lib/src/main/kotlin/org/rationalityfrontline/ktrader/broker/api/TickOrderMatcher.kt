package org.rationalityfrontline.ktrader.broker.api

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rationalityfrontline.kevent.KEvent
import org.rationalityfrontline.ktrader.datatype.*
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

/**
 * 依据推送的 Tick 进行订单模拟撮合成交。订单回报及成交回报会通过 [kEvent] 发送
 * @param accountId 生成的订单/成交回报的账户 ID
 * @param sourceId 发送的 BrokerEvent 的 sourceId
 * @param kEvent 用于发送 BrokerEvent
 * @param realApi 用于计算手续费、保证金等
 */
class TickOrderMatcher(val accountId: String, val sourceId: String, val kEvent: KEvent, val realApi: BrokerApi? = null) {

    private val _subscriptions = mutableSetOf<String>()
    /**
     * 订阅了 Tick 的证券代码集合
     */
    val subscriptions: Set<String> = _subscriptions

    /**
     * 缓存的最新 Tick
     */
    private val lastTicks: MutableMap<String, Tick> = mutableMapOf()

    /**
     * 今日的所有订单
     */
    private val todayOrders: MutableMap<String, Order> = mutableMapOf()

    /**
     * 今日未完成的订单
     */
    private val openOrders: MutableMap<String, MutableSet<Order>> = mutableMapOf()

    /**
     * 递增的订单引用（用于产生订单 ID）
     */
    private val orderRef: AtomicInteger = AtomicInteger(0)

    /**
     * 向该订单模拟撮合器推送 Tick
     */
    fun feedTick(tick: Tick) {
        if (tick.code in subscriptions) {
            lastTicks[tick.code] = tick
            matchOrder(tick.code)
        }
    }

    /**
     * 向 [kEvent] 发送一条 [BrokerEvent]
     */
    private fun postBrokerEvent(type: BrokerEventType, data: Any) {
        kEvent.post(type, BrokerEvent(type, sourceId, data))
    }

    /**
     * 将某一 Order 完成后需要进行的操作
     */
    private fun finishOrder(order: Order) {
        val openOrders = openOrders[order.code]
        if (openOrders != null) {
            openOrders.remove(order)
            if (openOrders.isEmpty()) {
                this.openOrders.remove(order.code)
                _subscriptions.remove(order.code)
                lastTicks.remove(order.code)
            }
        }
    }

    /**
     * 按 [price] 与 [volume] 生成一笔成交，同时更新订单状态及数据
     */
    private fun generateTrade(price: Double, volume: Int, order: Order) {
        if (volume <= 0) return
        val trade = Trade(
            accountId, "${accountId}_${System.currentTimeMillis()}_${orderRef.incrementAndGet()}",
            order.orderId, order.code, price, volume, 0.0, order.direction, order.offset, 0.0, LocalDateTime.now()
        )
        order.updateTime = trade.time
        order.filledVolume += trade.volume
        order.status = if (order.filledVolume < order.volume) {
            order.statusMsg = "部分成交"
            OrderStatus.PARTIALLY_FILLED
        } else {
            finishOrder(order)
            order.statusMsg = "全部成交"
            OrderStatus.FILLED
        }
        if (realApi != null) {
            realApi.calculateTrade(trade)
            order.turnover += trade.turnover
            order.commission += trade.commission
            realApi.calculateOrder(order)
        }
        postBrokerEvent(BrokerEventType.TRADE_REPORT, trade.deepCopy())
        postBrokerEvent(BrokerEventType.ORDER_STATUS, order.deepCopy())
    }

    /**
     * 按买档/卖档数据对订单进行逐档模拟撮合，如果全部挡位撮合后依然有未成交余量，将全部余量按最高挡位价格成交。市价单会当作挂单价为极限值的限价单处理。
     */
    private fun simTrade(tick: Tick, order: Order) {
        when (order.direction) {
            Direction.LONG -> {
                val orderPrice = if (order.orderType == OrderType.MARKET) Double.POSITIVE_INFINITY else order.price
                var restVolume = order.volume - order.filledVolume
                var lastPrice = 0.0
                var i = 0
                // 小于等于做多价的卖档都可成交
                while (tick.askPrice[i] <= orderPrice && tick.askVolume[i] > 0 && restVolume > 0) {
                    lastPrice = tick.askPrice[i]
                    generateTrade(lastPrice, min(tick.askVolume[i], restVolume), order)
                    i++
                    restVolume = order.volume - order.filledVolume
                }
                // 如果全部卖档均可成交，且仍有剩余未成单量，那么将剩余单量全部用最高卖档价成交
                if (i > 0 && i == tick.askVolume.size && restVolume > 0) {
                    generateTrade(lastPrice, restVolume, order)
                }
            }
            Direction.SHORT -> {
                val orderPrice = if (order.orderType == OrderType.MARKET) Double.NEGATIVE_INFINITY else order.price
                var restVolume = order.volume - order.filledVolume
                var lastPrice = 0.0
                var i = 0
                // 大于等于做空价的买档都可成交
                while (tick.bidPrice[i] >= orderPrice && tick.bidVolume[i] > 0 && restVolume > 0) {
                    lastPrice = tick.bidPrice[i]
                    generateTrade(lastPrice, min(tick.bidVolume[i], restVolume), order)
                    i++
                    restVolume = order.volume - order.filledVolume
                }
                // 如果全部买档均可成交，且仍有剩余未成单量，那么将剩余单量全部用最低买档价成交
                if (i > 0 && i == tick.bidVolume.size && restVolume > 0) {
                    generateTrade(lastPrice, order.volume - order.filledVolume, order)
                }
            }
            else -> Unit
        }
    }

    /**
     * 判断挂单量是否能按 [tick] 全部撮合成交。FAK 订单的挂单量会优先使用 extras.minVolume。
     */
    private fun canFillVolume(tick: Tick, order: Order): Boolean {
        val orderPrice = if (order.orderType == OrderType.MARKET) Double.POSITIVE_INFINITY else order.price
        var restVolume = order.volume - order.filledVolume
        if (order.orderType == OrderType.FAK) {
            restVolume = order.extras?.get("minVolume")?.toIntOrNull() ?: restVolume
        }
        when (order.direction) {
            Direction.LONG -> {
                var i = 0
                // 小于等于做多价的卖档都可成交
                while (tick.askPrice[i] <= orderPrice && tick.askVolume[i] > 0 && restVolume > 0) {
                    restVolume -= min(tick.askVolume[i], restVolume)
                    i++
                }
            }
            Direction.SHORT -> {
                var i = 0
                // 大于等于做空价的买档都可成交
                while (tick.bidPrice[i] >= orderPrice && tick.bidVolume[i] > 0 && restVolume > 0) {
                    restVolume -= min(tick.bidVolume[i], restVolume)
                    i++
                }
            }
            else -> Unit
        }
        return restVolume == 0
    }

    /**
     * 进行模拟撮合
     */
    private fun matchOrder(code: String) {
        val tick = lastTicks[code] ?: return
        if (tick.status == MarketStatus.CONTINUOUS_MATCHING || tick.status == MarketStatus.AUCTION_MATCHED) {
            val openOrders = openOrders[code] ?: return
            openOrders.forEach { order ->
                when (order.orderType) {
                    // 限价单/市价单的处理逻辑相似，将市价单视为挂单价为极限值的限价单
                    OrderType.LIMIT, OrderType.MARKET -> {
                        simTrade(tick, order)
                    }
                    // FAK 与 FOK 的处理逻辑相似，都是先判断是否能成交，然后决定是否撮合，然后撤单
                    OrderType.FAK, OrderType.FOK -> {
                        if (canFillVolume(tick, order)) {
                            simTrade(tick, order)
                        }
                        if (order.status != OrderStatus.FILLED) {
                            cancelOrder(order.orderId)
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    /**
     * 下单
     * @param code 证券代码
     * @param price 下单价格
     * @param volume 下单数量
     * @param direction 交易方向（做多/做空）
     * @param offset 开平类型（开仓/平仓/平今/平昨）
     * @param orderType 订单类型（限价单/市价单/FAK/FOK等）
     * @param extras 额外的参数，默认为 null
     * @param lastTick 用于确保即使是不活跃成交品种，下单时也能立即判断是否能与买档/卖档成交
     * @return 产生的订单
     */
    fun insertOrder(
        code: String, price: Double, volume: Int, direction: Direction, offset: OrderOffset,
        orderType: OrderType = OrderType.LIMIT, extras: Map<String, String>? = null, lastTick: Tick? = null
    ): Order {
        val now = LocalDateTime.now()
        val order = Order(
            accountId,
            "${accountId}_${System.currentTimeMillis()}_${orderRef.incrementAndGet()}",
            code, price, volume, direction, offset, orderType,
            OrderStatus.SUBMITTING, "报单已提交",
            0, 0.0, 0.0, 0.0, 0.0,
            now, now,
            extras = extras?.toMutableMap()
        )
        todayOrders[order.orderId] = order
        var errorInfo: String? = null
        if (lastTick == null && lastTicks[code] == null) {
            errorInfo = "查询不到 $code 对应的证券的最新 Tick"
        } else {
            if (lastTick != null) lastTicks[code] = lastTick
            _subscriptions.add(code)
            val tick = lastTicks[code]!!
            if (tick.status == MarketStatus.UNKNOWN || tick.status == MarketStatus.CLOSED) {
                errorInfo = "当前时段不可交易（${tick.status}）"
            }
        }
        if (errorInfo == null) {
            if (orderType == OrderType.STOP || orderType == OrderType.CUSTOM || orderType == OrderType.UNKNOWN) {
                errorInfo = "不支持的订单类型（$orderType）"
            }
        }
        if (errorInfo == null) {
            realApi?.calculateOrder(order)
            GlobalScope.launch {
                delay(1)
                postBrokerEvent(BrokerEventType.ORDER_STATUS, order.apply {
                    status = OrderStatus.ACCEPTED
                    statusMsg = "未成交"
                }.deepCopy())
                val openOrders = openOrders.getOrPut(code) { mutableSetOf() }
                openOrders.add(order)
                matchOrder(code)
            }
        } else {
            order.status = OrderStatus.ERROR
            order.statusMsg = errorInfo
        }
        return order.deepCopy()
    }

    /**
     * 撤单
     * @param orderId 要撤的订单的 ID
     * @param extras 额外的参数，默认为 null
     */
    fun cancelOrder(orderId: String, extras: Map<String, String>? = null) {
        var errorInfo: String? = null
        val order = todayOrders[orderId] ?: throw Exception("未找到 $orderId 对应的订单记录")
        if (order.status !in setOf(OrderStatus.UNKNOWN, OrderStatus.SUBMITTING, OrderStatus.ACCEPTED, OrderStatus.PARTIALLY_FILLED)) {
            errorInfo = "订单当前状态（${order.status}）不可撤"
        }
        if (errorInfo == null) {
            order.status = OrderStatus.CANCELED
            order.statusMsg = "已撤单"
            finishOrder(order)
            postBrokerEvent(BrokerEventType.ORDER_STATUS, order.deepCopy())
        } else {
            order.statusMsg = "撤单失败：$errorInfo"
            postBrokerEvent(BrokerEventType.CANCEL_FAILED, order.deepCopy())
        }
    }

    /**
     * 撤单所有未完成订单
     * @param extras 额外的参数，默认为 null
     */
    fun cancelAllOrders(extras: Map<String, String>?) {
        openOrders.values.flatten().forEach { cancelOrder(it.orderId) }
    }

    /**
     * 重置该订单模拟撮合器的状态。当交易日变更时需要调用此方法
     */
    fun reset() {
        _subscriptions.clear()
        lastTicks.clear()
        todayOrders.clear()
        openOrders.clear()
        orderRef.set(0)
    }
}
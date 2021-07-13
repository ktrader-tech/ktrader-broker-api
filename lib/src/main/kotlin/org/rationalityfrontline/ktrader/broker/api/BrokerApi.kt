package org.rationalityfrontline.ktrader.broker.api

import org.rationalityfrontline.kevent.KEvent
import java.time.LocalDate
import java.time.LocalDateTime

abstract class BrokerApi(val config: Map<String, Any>, val kEvent: KEvent) {
    abstract val name: String
    abstract val version: String
    abstract val account: String
    open val sourceId: String get() = "${name}_${account}_${hashCode()}"
    abstract val mdConnected: Boolean
    abstract val tdConnected: Boolean
    val createTime: LocalDateTime = LocalDateTime.now()

    abstract suspend fun connect(connectMd: Boolean = true, connectTd: Boolean = true, extras: Map<String, Any>? = null)

    abstract suspend fun close()

    open fun getTradingDay(): LocalDate = LocalDate.now()

    abstract suspend fun subscribeMarketData(codes: Collection<String>, extras: Map<String, Any>? = null)

    abstract suspend fun unsubscribeMarketData(codes: Collection<String>, extras: Map<String, Any>? = null)

    abstract suspend fun subscribeAllMarketData(extras: Map<String, Any>? = null)

    abstract suspend fun unsubscribeAllMarketData(extras: Map<String, Any>? = null)

    abstract suspend fun querySubscriptions(useCache: Boolean = true, extras: Map<String, Any>? = null): List<String>

    abstract suspend fun queryLastTick(code: String, useCache: Boolean = true, extras: Map<String, Any>? = null): Tick

    abstract suspend fun queryInstrument(code: String, useCache: Boolean = true, extras: Map<String, Any>? = null): Instrument

    abstract suspend fun queryAllInstruments(useCache: Boolean = true, extras: Map<String, Any>? = null): List<Instrument>

    abstract suspend fun queryAssets(useCache: Boolean = true, extras: Map<String, Any>? = null): Assets

    abstract suspend fun queryPositions(code: String? = null, useCache: Boolean = true, extras: Map<String, Any>? = null): List<Position>

    abstract suspend fun queryOrder(orderId: String, useCache: Boolean = true, extras: Map<String, Any>? = null): Order

    abstract suspend fun queryOrders(code: String? = null, onlyUnfinished: Boolean = true, useCache: Boolean = true, extras: Map<String, Any>? = null): List<Order>

    abstract suspend fun queryTrade(tradeId: String, useCache: Boolean = true, extras: Map<String, Any>? = null): Order

    abstract suspend fun queryTrades(code: String? = null, useCache: Boolean = true, extras: Map<String, Any>? = null): List<Trade>

    abstract fun insertOrder(
        code: String, price: Double, volume: Int, direction: Direction, offset: OrderOffset,
        orderType: OrderType = OrderType.LIMIT, extras: Map<String, Any>? = null
    ): Order

    abstract fun cancelOrder(orderId: String, extras: Map<String, Any>? = null)

    open fun prepareFeeCalculation(extras: Map<String, Any>? = null) {}

    open fun calculatePositions(positions: List<Position>, extras: Map<String, Any>? = null): List<Position> = positions

    open fun calculateFrozenCash(order: Order, extras: Map<String, Any>? = null): Double = 0.0

    open fun calculateCommission(order: Order, extras: Map<String, Any>? = null): Double = 0.0

    open suspend fun customSuspendRequest(method: String, params: Map<String, Any>? = null): Any {
        throw IllegalArgumentException("Unsupported suspend custom method：$method")
    }

    open fun customRequest(method: String, params: Map<String, Any>? = null): Any {
        throw IllegalArgumentException("Unsupported custom method：$method")
    }

    override fun toString(): String {
        return "$name@$version@$account@${hashCode()}"
    }
}
package org.rationalityfrontline.ktrader.broker.api

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.coroutines.Continuation

data class Tick(
    val code: String,
    val time: LocalDateTime,
    val lastPrice: Double,
    val bidPrice: Array<Double>,
    val askPrice: Array<Double>,
    val bidVolume: Array<Int>,
    val askVolume: Array<Int>,
    val volume: Int,
    val turnover: Double,
    val openInterest: Int,
    val direction: TickDirection,
    val status: MarketStatus,
    val yesterdayClose: Double,
    val yesterdaySettlementPrice: Double,
    val yesterdayOpenInterest: Int,
    val todayOpenPrice: Double,
    val todayClosePrice: Double,
    val todayHighPrice: Double,
    val todayLowPrice: Double,
    val todayHighLimitPrice: Double,
    val todayLowLimitPrice: Double,
    val todaySettlementPrice: Double,
    val todayAvgPrice: Double,
    val todayVolume: Int,
    val todayTurnover: Double,
    val todayOpenInterest: Int,
    val extras: Map<String, Any>? = null,
)

data class Order(
    val orderId: String,
    val code: String,
    val price: Double,
    val volume: Int,
    val direction: Direction,
    val offset: OrderOffset,
    val orderType: OrderType,
    var status: OrderStatus,
    var filledVolume: Int,
    var turnover: Double,
    var frozenCash: Double,
    var commission: Double,
    val createTime: LocalDateTime,
    var updateTime: LocalDateTime,
    var errorInfo: String = "",
    val extras: Map<String, Any>? = null,
)

data class OrderStatusUpdate(
    val orderId: String,
    val newStatus: OrderStatus,
    val statusMsg: String = "",
    val updateTime: LocalDateTime = LocalDateTime.now(),
    val extras: Map<String, Any>? = null,
)

data class Trade(
    val tradeId: String,
    val orderId: String,
    val code: String,
    val price: Double,
    val volume: Int,
    val direction: Direction,
    val offset: OrderOffset,
    val commission: Double,
    val time: LocalDateTime,
    val extras: Map<String, Any>? = null,
)

data class MarginRate(
    val code: String,
    val longMarginRatioByMoney: Double,
    val longMarginRatioByVolume: Double,
    val shortMarginRatioByMoney: Double,
    val shortMarginRatioByVolume: Double,
    val extras: Map<String, Any>? = null,
)

data class CommissionRate(
    val code: String,
    val openRatioByMoney: Double,
    val openRatioByVolume: Double,
    val closeRatioByMoney: Double,
    val closeRatioByVolume: Double,
    val closeTodayRatioByMoney: Double,
    val closeTodayRatioByVolume: Double,
    val orderInsertFeeByVolume: Double = 0.0,
    val orderCancelFeeByVolume: Double = 0.0,
    val orderInsertFeeByTrade: Double = 0.0,
    val orderCancelFeeByTrade: Double = 0.0,
    val extras: Map<String, Any>? = null,
)

data class Instrument(
    val code: String,
    val type: InstrumentType,
    val name: String,
    val priceTick: Double,
    val isTrading: Boolean,
    val openDate: LocalDate,
    val expireDate: LocalDate? = null,
    val endDeliveryDate: LocalDate? = null,
    val volumeMultiple: Int = 1,
    val isUseMaxMarginSideAlgorithm: Boolean = false,
    var marginRate: MarginRate? = null,
    var commissionRate: CommissionRate? = null,
    val optionsType: OptionsType? = null,
    val extras: Map<String, Any>? = null,
)

data class Assets(
    var total: Double,
    var available: Double,
    var positionValue: Double,
    var frozenByOrder: Double,
    var todayCommission: Double,
    val extras: Map<String, Any>? = null,
)

data class Position(
    val code: String,
    val direction: Direction,
    val yesterdayVolume: Int,
    var volume: Int,
    var value: Double,
    var todayVolume: Int,
    var frozenVolume: Int,
    var closeableVolume: Int,
    var todayOpenVolume: Int,
    var todayCloseVolume: Int,
    var todayCommission: Double,
    val openCost: Double,
    val avgOpenPrice: Double,
    var lastPrice: Double,
    var pnl: Double,
    val extras: Map<String, Any>? = null,
)

data class RequestContinuation(
    val requestId: Int,
    val continuation: Continuation<*>,
    val tag: String = "",
    val data: Any = Unit,
)
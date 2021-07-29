package org.rationalityfrontline.ktrader.broker.api

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Tick
 * @param code 证券代码
 * @param time 产生时间
 * @param lastPrice 最新价
 * @param bidPrice 挂买价（买一，买二，买三 ...）
 * @param askPrice 挂卖价（卖一，卖二，卖三 ...）
 * @param bidVolume 挂买量（买一，买二，买三 ...）
 * @param askVolume 挂卖量（卖一，卖二，卖三 ...）
 * @param volume Tick 内的成交量
 * @param turnover Tick 内的成交额
 * @param openInterest Tick 内的持仓量变动
 * @param direction Tick 的方向
 * @param status 证券当前的市场状态
 * @param preClosePrice 昨日收盘价
 * @param preSettlementPrice 昨日结算价
 * @param preOpenInterest 昨日持仓量
 * @param todayOpenPrice 今日开盘价
 * @param todayClosePrice 今日收盘价
 * @param todayHighPrice 今日最高价
 * @param todayLowPrice 今日最低价
 * @param todayHighLimitPrice 今日涨停价
 * @param todayLowLimitPrice 今日跌停价
 * @param todaySettlementPrice 今日结算价
 * @param todayAvgPrice 今日成交均价
 * @param todayVolume 今日成交量
 * @param todayTurnover 今日成交额
 * @param todayOpenInterest 今日持仓量
 * @param extras 额外数据
 */
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
    var status: MarketStatus,
    val preClosePrice: Double,
    val preSettlementPrice: Double,
    val preOpenInterest: Int,
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
    var extras: MutableMap<String, Any>? = null,
)

/**
 * Order
 * @param accountId 资金账号
 * @param orderId 订单 ID
 * @param code 证券代码
 * @param price 订单价格
 * @param volume 订单数量
 * @param direction 订单交易方向
 * @param offset 仓位开平类型
 * @param orderType 订单类型
 * @param status 订单状态
 * @param statusMsg 订单状态描述
 * @param filledVolume 已成交数量
 * @param turnover 已成交金额（期货/期权是成交价*成交量*合约乘数）
 * @param avgFillPrice 平均成交价格
 * @param frozenCash 挂单冻结资金（仅限开仓）
 * @param commission 手续费
 * @param createTime 订单产生时间
 * @param updateTime 订单更新时间
 * @param extras 额外数据
 */
data class Order(
    val accountId: String,
    val orderId: String,
    val code: String,
    val price: Double,
    val volume: Int,
    val direction: Direction,
    val offset: OrderOffset,
    val orderType: OrderType,
    var status: OrderStatus,
    var statusMsg: String,
    var filledVolume: Int,
    var turnover: Double,
    var avgFillPrice: Double,
    var frozenCash: Double,
    var commission: Double,
    val createTime: LocalDateTime,
    var updateTime: LocalDateTime,
    var extras: MutableMap<String, Any>? = null,
)

/**
 * Trade
 * @param accountId 资金账号
 * @param tradeId 成交记录 ID
 * @param orderId 对应的订单 ID
 * @param code 证券代码
 * @param price 成交价
 * @param volume 成交量
 * @param turnover 成交额（期货/期权是成交价*成交量*合约乘数）
 * @param direction 交易方向
 * @param offset 开平仓类型
 * @param commission 手续费
 * @param time 成交时间
 * @param extras 额外数据
 */
data class Trade(
    val accountId: String,
    val tradeId: String,
    val orderId: String,
    val code: String,
    val price: Double,
    val volume: Int,
    var turnover: Double,
    val direction: Direction,
    var offset: OrderOffset,
    var commission: Double,
    val time: LocalDateTime,
    var extras: MutableMap<String, Any>? = null,
)

/**
 * 期货/期权保证金率
 * @param code 证券代码
 * @param longMarginRatioByMoney 多头保证金率（按金额）。当证券为期权时表示期权固定保证金
 * @param longMarginRatioByVolume 多头保证金（按手数）。当证券为期权时表示期权交易所固定保证金
 * @param shortMarginRatioByMoney 空头保证金率（按金额）。当证券为期权时表示期权最小保证金
 * @param shortMarginRatioByVolume 空头保证金（按手数）。当证券为期权时表示期权交易所最小保证金
 * @param extras 额外数据
 */
data class MarginRate(
    val code: String,
    val longMarginRatioByMoney: Double,
    val longMarginRatioByVolume: Double,
    val shortMarginRatioByMoney: Double,
    val shortMarginRatioByVolume: Double,
    var extras: MutableMap<String, Any>? = null,
)

/**
 * 手续费率
 * @param code 证券代码
 * @param openRatioByMoney 开仓手续费率（按成交额）
 * @param openRatioByVolume 开仓手续费（按手数）
 * @param closeRatioByMoney 平仓手续费率（按成交额）
 * @param closeRatioByVolume 平仓手续费（按手数）
 * @param closeTodayRatioByMoney 平今仓手续费率（按成交额）
 * @param closeTodayRatioByVolume 平今仓手续费（按手数）
 * @param orderInsertFeeByVolume 报单手续费（按手数）
 * @param orderInsertFeeByTrade 报单手续费（按订单）
 * @param orderCancelFeeByVolume 撤单手续费（按手数）
 * @param orderCancelFeeByTrade 撤单手续费（按订单）
 * @param optionsStrikeRationByMoney 期权行权手续费率（按金额）
 * @param optionsStrikeRationByVolume 期权行权手续费（按手数）
 * @param extras 额外数据
 */
data class CommissionRate(
    val code: String,
    val openRatioByMoney: Double,
    val openRatioByVolume: Double,
    val closeRatioByMoney: Double,
    val closeRatioByVolume: Double,
    val closeTodayRatioByMoney: Double,
    val closeTodayRatioByVolume: Double,
    var orderInsertFeeByVolume: Double = 0.0,
    var orderCancelFeeByVolume: Double = 0.0,
    var orderInsertFeeByTrade: Double = 0.0,
    var orderCancelFeeByTrade: Double = 0.0,
    val optionsStrikeRationByMoney: Double = 0.0,
    val optionsStrikeRationByVolume: Double = 0.0,
    var extras: MutableMap<String, Any>? = null,
)

/**
 * 证券信息
 * @param code 证券代码
 * @param type 证券类型
 * @param productId 证券的产品 ID （仅期货/期权）
 * @param name 证券名称
 * @param priceTick 最小变动价格
 * @param isTrading 是否处于可交易状态
 * @param openDate 上市日
 * @param expireDate 最后交易日
 * @param endDeliveryDate 最后交割日
 * @param volumeMultiple 合约乘数
 * @param isUseMaxMarginSideAlgorithm 是否使用大额单边保证金算法
 * @param marginRate 保证金率
 * @param commissionRate 手续费率
 * @param optionsType 期权类型
 * @param optionsUnderlyingCode 期权对应的基础证券代码
 * @param optionsStrikePrice 期权行权价格
 * @param extras 额外数据
 */
data class Security(
    val code: String,
    val type: SecurityType,
    val productId: String = "",
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
    val optionsType: OptionsType = OptionsType.UNKNOWN,
    val optionsUnderlyingCode: String = "",
    val optionsStrikePrice: Double = 0.0,
    var extras: MutableMap<String, Any>? = null,
)

/**
 * 资产
 * @param accountId 资金账号
 * @param total 全部资产（折合现金）
 * @param available 可用资金
 * @param positionValue 持仓占用资金
 * @param frozenByOrder 挂单占用资金
 * @param todayCommission 今日手续费
 * @param extras 额外数据
 */
data class Assets(
    val accountId: String,
    var total: Double,
    var available: Double,
    var positionValue: Double,
    var frozenByOrder: Double,
    var todayCommission: Double,
    var extras: MutableMap<String, Any>? = null,
)

/**
 * 持仓信息
 * @param accountId 资金账号
 * @param code 证券代码
 * @param direction 持仓方向
 * @param preVolume 昨日持仓量
 * @param volume 今日持仓量
 * @param value 持仓占用资金
 * @param todayVolume 今仓数量
 * @param yesterdayVolume 昨仓数量
 * @param frozenVolume 冻结持仓量
 * @param closeableVolume 可平持仓量
 * @param todayOpenVolume 今日累计开仓量
 * @param todayCloseVolume 今日累计平仓量
 * @param todayCommission 今日手续费
 * @param openCost 开仓成本（期货/期权是Σ成交价*成交量*合约乘数）
 * @param avgOpenPrice 开仓均价
 * @param lastPrice 最新价
 * @param pnl 净盈亏额
 * @param extras 额外数据
 */
data class Position(
    val accountId: String,
    val code: String,
    val direction: Direction,
    var preVolume: Int,
    var volume: Int,
    var value: Double,
    var todayVolume: Int,
    var yesterdayVolume: Int,
    var frozenVolume: Int,
    var closeableVolume: Int,
    var todayOpenVolume: Int,
    var todayCloseVolume: Int,
    var todayCommission: Double,
    var openCost: Double,
    var avgOpenPrice: Double,
    var lastPrice: Double,
    var pnl: Double,
    var extras: MutableMap<String, Any>? = null,
)
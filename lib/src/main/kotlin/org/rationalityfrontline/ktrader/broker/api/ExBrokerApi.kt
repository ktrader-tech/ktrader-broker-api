@file:Suppress("unused")

package org.rationalityfrontline.ktrader.broker.api

import org.rationalityfrontline.ktrader.datatype.*

/**
 * 加强版的 [BrokerApi]。提供了 [Bar] 行情订阅，持仓、订单、成交的缓存维护，禁止下单，设置初始资金等扩展功能
 */
interface ExBrokerApi : BrokerApi{

    /**
     * 只读的持仓汇总信息。注意请不要更改其内部的任何对象，否则可能造成数据错误
     */
    val readOnlyPositions: Map<String, BiPosition>

    /**
     * 只读的持仓明细信息。注意请不要更改其内部的任何对象，否则可能造成数据错误
     */
    val readOnlyPositionDetails: Map<String, BiPositionDetails>

    /**
     * 只读的今日订单。注意请不要更改其内部的任何对象，否则可能造成数据错误
     */
    val readOnlyTodayOrders: Map<String, Order>

    /**
     * 只读的今日成交记录。注意请不要更改其内部的任何对象，否则可能造成数据错误
     */
    val readOnlyTodayTrades: List<Trade>

    /**
     * 是否禁用该实例（禁用状态下无法下单）
     */
    val disabled: Boolean

    /**
     * 更改该实例禁用状态。禁用后将无法进行下单操作。
     * 如果禁用前存在未完成订单，禁用时会自动撤单
     */
    suspend fun setDisabled(value: Boolean)

    /**
     * 修改账户初始资金（即累计入金 - 累计出金）
     */
    suspend fun modifyInitialCash(newValue: Double)

    /**
     * 订阅证券 [Bar] 行情
     * @param code 证券代码
     * @param interval Bar 的频率
     */
    suspend fun subscribeBar(code: String, interval: Int)

    /**
     * 取消订阅 [Bar] 行情
     * @param code 证券代码
     * @param interval Bar 的频率
     */
    suspend fun unsubscribeBar(code: String, interval: Int)

    /**
     * 查询当前已订阅 [Bar] 行情的证券
     */
    suspend fun queryBarSubscriptions(): List<BarInfo>

    /**
     * 下单
     * @param code 证券代码
     * @param price 下单价格
     * @param volume 下单数量
     * @param direction 交易方向（做多/做空）
     * @param offset 开平类型（开仓/平仓/平今/平昨）
     * @param closePositionPrice 平仓时指定的所平仓位的开仓价。如果平仓量超过对应开仓价的持仓数量，则剩余平仓量按最接近指定开仓价的持仓依次平仓
     * @param orderType 订单类型（限价单/市价单/FAK/FOK等）
     * @param minVolume 最小成交量，仅当订单类型为 FAK 时生效
     * @param extras 额外的参数，默认为 null
     * @return 产生的订单
     */
    suspend fun insertOrder(
        code: String,
        price: Double,
        volume: Int,
        direction: Direction,
        offset: OrderOffset,
        closePositionPrice: Double?,
        orderType: OrderType,
        minVolume: Int,
        extras: Map<String, String>?
    ): Order

    /**
     * 下单。平仓指定价对于平多为持仓最低价，对于平空为持仓最高价。
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
    override suspend fun insertOrder(
        code: String,
        price: Double,
        volume: Int,
        direction: Direction,
        offset: OrderOffset,
        orderType: OrderType,
        minVolume: Int,
        extras: Map<String, String>?
    ): Order {
        return insertOrder(code, price, volume, direction, offset, null, orderType, minVolume, extras)
    }

    /**
     * cancelOrder(order.orderId) 的快捷写法
     */
    suspend fun cancelOrder(order: Order) {
        cancelOrder(order.orderId)
    }
}
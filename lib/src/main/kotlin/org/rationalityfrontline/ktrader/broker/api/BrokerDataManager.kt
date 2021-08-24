package org.rationalityfrontline.ktrader.broker.api

import org.rationalityfrontline.ktrader.datatype.*
import java.time.LocalDate

/**
 * Broker 交易数据存储标准接口
 */
interface BrokerDataManager {

    /**
     * 保存交易日
     * @param accountId 账户 ID
     * @param tradingDay 交易日
     */
    suspend fun saveTradingDay(accountId: String, tradingDay: LocalDate)

    /**
     * 查询账户 [accountId] 最新的交易日
     */
    suspend fun queryLastTradingDay(accountId: String): LocalDate?

    /**
     * 查询账户 [accountId] 的全部交易日
     */
    suspend fun queryTradingDays(accountId: String): List<LocalDate>

    /**
     * 删除账户 [accountId] 的所有交易日。如果 [accountId] 为 null，则删除所有账户的交易日数据
     */
    suspend fun deleteTradingDays(accountId: String?): Int

    /**
     * 保存账户资金信息 [assets]
     */
    suspend fun saveAssets(assets: Assets)

    /**
     * 查询账户 [accountId] 在 [tradingDay] 的资金信息。如果 [accountId] 为 null 则查询所有账户；如果 [tradingDay] 为 null 则查询所有交易日
     */
    suspend fun queryAssets(accountId: String?, tradingDay: LocalDate?): List<Assets>

    /**
     * 删除账户 [accountId] 在 [tradingDay] 的资金信息。如果 [accountId] 为 null 则删除所有账户；如果 [tradingDay] 为 null 则删除所有交易日
     */
    suspend fun deleteAssets(accountId: String?, tradingDay: LocalDate?): Int

    /**
     * 保存账户持仓汇总信息
     */
    suspend fun savePosition(position: Position)

    /**
     * 查询账户持仓汇总信息
     * @param accountId 账户 ID。为 null 则查询所有账户
     * @param tradingDay 交易日。为 null 则查询所有交易日
     * @param code 证券代码。为 null 则查询所有证券
     * @param direction 持仓方向。为 null 则查询所有持仓方向
     */
    suspend fun queryPositions(accountId: String?, tradingDay: LocalDate?, code: String?, direction: Direction?): List<Position>

    /**
     * 删除账户持仓汇总信息
     * @param accountId 账户 ID。为 null 则删除所有账户
     * @param tradingDay 交易日。为 null 则删除所有交易日
     * @param code 证券代码。为 null 则删除所有证券
     * @param direction 持仓方向。为 null 则删除所有持仓方向
     */
    suspend fun deletePositions(accountId: String?, tradingDay: LocalDate?, code: String?, direction: Direction?): Int

    /**
     * 保存持仓明细信息
     */
    suspend fun savePositionDetail(positionDetail: PositionDetail)

    /**
     * 查询持仓明细信息
     * @param accountId 账户 ID。为 null 则查询所有账户
     * @param code 证券代码。为 null 则查询所有证券
     * @param direction 持仓方向。为 null 则查询所有持仓方向
     */
    suspend fun queryPositionDetails(accountId: String?, code: String?, direction: Direction?): List<PositionDetail>

    /**
     * 删除持仓明细信息
     * @param accountId 账户 ID。为 null 则删除所有账户
     * @param code 证券代码。为 null 则删除所有证券
     * @param direction 持仓方向。为 null 则删除所有持仓方向
     */
    suspend fun deletePositionsDetails(accountId: String?, code: String?, direction: Direction?): Int

    /**
     * 保存订单信息
     */
    suspend fun saveOrder(order: Order)

    /**
     * 依据 [orderId] 查询对应的订单。如果订单不存在则返回 null
     */
    suspend fun queryOrder(orderId: String): Order?

    /**
     * 查询订单信息
     * @param accountId 账户 ID。为 null 则查询所有账户
     * @param tradingDay 交易日。为 null 则查询所有交易日
     * @param code 证券代码。为 null 则查询所有证券
     * @param status 订单状态。为 null 则查询所有状态
     */
    suspend fun queryOrders(accountId: String?, tradingDay: LocalDate?, code: String?, status: OrderStatus?): List<Order>

    /**
     * 删除订单信息
     * @param accountId 账户 ID。为 null 则删除所有账户
     * @param tradingDay 交易日。为 null 则删除所有交易日
     * @param code 证券代码。为 null 则删除所有证券
     * @param status 订单状态。为 null 则删除所有状态
     */
    suspend fun deleteOrders(accountId: String?, tradingDay: LocalDate?, code: String?, status: OrderStatus?): Int

    /**
     * 保存成交信息
     */
    suspend fun saveTrade(trade: Trade)

    /**
     * 依据 [tradeId] 查询对应的成交记录
     */
    suspend fun queryTrade(tradeId: String): Trade?

    /**
     * 查询成交信息
     * @param accountId 账户 ID。为 null 则查询所有账户
     * @param tradingDay 交易日。为 null 则查询所有交易日
     * @param code 证券代码。为 null 则查询所有证券
     * @param orderId 订单 ID。为 null 则查询所有订单
     */
    suspend fun queryTrades(accountId: String?, tradingDay: LocalDate?, code: String?, orderId: String?): List<Trade>

    /**
     * 删除成交信息
     * @param accountId 账户 ID。为 null 则删除所有账户
     * @param tradingDay 交易日。为 null 则删除所有交易日
     * @param code 证券代码。为 null 则删除所有证券
     * @param orderId 订单 ID。为 null 则删除所有订单
     */
    suspend fun deleteTrades(accountId: String?, tradingDay: LocalDate?, code: String?, orderId: String?): Int

    /**
     * 保存属性
     * @param key 属性名，必须保证唯一
     * @param value 属性值
     */
    suspend fun saveProperty(key: String, value: String)

    /**
     * 查询属性值
     * @param key 属性名
     * @return 查询到的属性值。如果未查到属性，则返回 null
     */
    suspend fun queryProperty(key: String): String?

    /**
     * 查询属性值，如果不存在该属性则返回默认值 [default]
     * @param key 属性名
     * @param default 默认值
     */
    suspend fun queryPropertyOrDefault(key: String, default: String): String {
        return queryProperty(key) ?: default
    }

    /**
     * 查询属性值，如果不存在该属性则存储并返回默认值 [default]
     * @param key 属性名
     * @param default 默认值
     */
    suspend fun queryPropertyOrPut(key: String, default: String): String {
        var value = queryProperty(key)
        if (value == null) {
            saveProperty(key, default)
            value = default
        }
        return value
    }

    /**
     * 删除属性
     * @param key 属性名
     */
    suspend fun deleteProperty(key: String): Boolean
}
package org.rationalityfrontline.ktrader.broker.api

import org.rationalityfrontline.kevent.KEvent
import org.rationalityfrontline.ktrader.datatype.*

/**
 * 创建一个模拟交易的 BrokerApi。将依据真实的 BrokerApi [realApi] 所推送的 Tick 更新订单的状态。
 * @param realApi 真实的 BrokerApi
 * @param simAccount 模拟账户的账号。注意订单回报、成交回报等地方返回的 accountId 为 "realApi.account-simAccount"
 * @param dataManager 用于存储及恢复模拟交易账户的数据
 * @param isAsParent 是否作为母账户。母账户取消订阅时会取消真实账户的订阅，connect 与 close 方法也会调用真实账户的对应方法
 */
@Suppress("FunctionName")
fun SimBrokerApi(realApi: BrokerApi, simAccount: String, dataManager: BrokerDataManager, isAsParent: Boolean = false): SepBrokerApi {
    val simRealBrokerApi = SimMatcherBrokerApi(realApi, simAccount, isAsParent)
    return SepBrokerApi(simRealBrokerApi, simAccount, dataManager, isAsParent)
}

/**
 * 实现模拟撮合交易的 BrokerApi，但并未实现 queryAssets, queryPositions 等交易查询相关的方法，因此需要依赖于 [SepBrokerApi] 来提供独立统计等功能来成为完整的 BrokerApi。
 */
internal class SimMatcherBrokerApi(val realApi: BrokerApi, val simAccount: String, val isAsParent: Boolean = false) : BrokerApi by realApi {

    init {
        if (simAccount.contains("[-_\\s]".toRegex())) throw IllegalArgumentException("simAccount 不能含有'-', '_'或空格")
    }

    override val name: String = "${realApi.name}-SIM"
    override val account: String = realApi.account
    override val sourceId: String get() = "${name}_${account}_${hashCode()}"
    override val kEvent: KEvent = KEvent(sourceId)

    /**
     * 订单模拟撮合器
     */
    private val orderMatcher = TickOrderMatcher("$account-$simAccount", sourceId, kEvent, realApi)

    /**
     * 向 [kEvent] 发送一条 [BrokerEvent]
     */
    private fun postBrokerEvent(type: BrokerEventType, data: Any) {
        kEvent.post(type, BrokerEvent(type, sourceId, data))
    }

    /**
     * 向 [kEvent] 发送一条 [BrokerEvent].[LogEvent]
     */
    private fun postBrokerLogEvent(level: LogLevel, msg: String) {
        postBrokerEvent(BrokerEventType.LOG, LogEvent(level, msg))
    }

    override suspend fun connect(extras: Map<String, String>?) {
        postBrokerLogEvent(LogLevel.INFO,  "【SimRealBrokerApi.connect】开始连接")
        realApi.kEvent.subscribeMultiple<BrokerEvent>(BrokerEventType.values().asList(), tag = sourceId) { event ->
            val brokerEvent = event.data
            // 屏蔽所有与交易相关的事件，转发其它所有事件
            when (brokerEvent.type) {
                BrokerEventType.ORDER_STATUS,
                BrokerEventType.TRADE_REPORT,
                BrokerEventType.CANCEL_FAILED -> return@subscribeMultiple
                BrokerEventType.TICK -> {
                    orderMatcher.feedTick(brokerEvent.data as Tick)
                }
                BrokerEventType.NEW_TRADING_DAY -> {
                    orderMatcher.reset()
                }
                else -> Unit
            }
            kEvent.post(event)
        }
        if (isAsParent) realApi.connect(extras)
        postBrokerLogEvent(LogLevel.INFO,  "【SimRealBrokerApi.connect】连接成功")
    }

    override fun close() {
        postBrokerLogEvent(LogLevel.INFO, "【SimRealBrokerApi.close】开始关闭")
        if (isAsParent) realApi.close()
        realApi.kEvent.removeSubscribersByTag(sourceId)
        kEvent.release()
        postBrokerLogEvent(LogLevel.INFO, "【SimRealBrokerApi.close】关闭成功")
    }

    override suspend fun insertOrder(
        code: String,
        price: Double,
        volume: Int,
        direction: Direction,
        offset: OrderOffset,
        orderType: OrderType,
        extras: Map<String, String>?
    ): Order {
        var lastTick: Tick? = null
        // 确保已订阅该证券 Tick 行情
        if (code !in orderMatcher.subscriptions) {
            realApi.subscribeTick(code)
            lastTick = queryLastTick(code)
        }
        return orderMatcher.insertOrder(code, price, volume, direction, offset, orderType, extras, lastTick)
    }

    override suspend fun cancelOrder(orderId: String, extras: Map<String, String>?) {
        orderMatcher.cancelOrder(orderId, extras)
    }

    override suspend fun cancelAllOrders(extras: Map<String, String>?) {
        orderMatcher.cancelAllOrders(extras)
    }
}
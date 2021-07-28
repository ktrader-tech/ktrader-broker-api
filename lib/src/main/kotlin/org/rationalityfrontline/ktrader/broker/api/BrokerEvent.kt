package org.rationalityfrontline.ktrader.broker.api

/**
 * 推送事件类型
 */
enum class BrokerEventType {

    /**
     * 自定义事件类型。[BrokerEvent.data]: [CustomEvent]，自定义事件
     */
    CUSTOM_EVENT,

    /**
     * 行情接口出错。[BrokerEvent.data]: [String]，错误信息
     */
    MD_ERROR,

    /**
     * 行情接口网络连接成功。[BrokerEvent.data]: [Unit]
     */
    MD_NET_CONNECTED,

    /**
     * 行情接口网络连接断开。[BrokerEvent.data]: [String]，断开原因
     */
    MD_NET_DISCONNECTED,

    /**
     * 行情接口用户登录成功。[BrokerEvent.data]: [Unit]
     */
    MD_USER_LOGGED_IN,

    /**
     * 行情接口 [Tick] 推送。[BrokerEvent.data]: [Tick]
     */
    MD_TICK,

    /**
     * 交易接口出错。[BrokerEvent.data]: [String]，错误信息
     */
    TD_ERROR,

    /**
     * 交易接口网络连接成功。[BrokerEvent.data]: [Unit]
     */
    TD_NET_CONNECTED,

    /**
     * 交易接口网络连接断开。[BrokerEvent.data]: [String]，断开原因
     */
    TD_NET_DISCONNECTED,

    /**
     * 交易接口用户登录成功。[BrokerEvent.data]: [Unit]
     */
    TD_USER_LOGGED_IN,

    /**
     * 交易接口订单状态推送。[BrokerEvent.data]: [Order]
     * * [OrderStatus.PARTIALLY_FILLED] 及 [OrderStatus.FILLED] 状态的 [Order] 推送总是后于对应的 [Trade] 推送
     * * 撤单失败时不会推送该事件，但会推送 [TD_CANCEL_FAILED] 事件
     * * 推送的 [Order] 实例不会自己更新状态，是静态的，且每次推送的实例不同
     */
    TD_ORDER_STATUS,

    /**
     * 交易接口撤单失败。[BrokerEvent.data]: [Order]
     * * 与上一次推送的 [Order] 的唯一区别在于 [Order.statusMsg]
     */
    TD_CANCEL_FAILED,

    /**
     * 交易接口成交回报推送。[BrokerEvent.data]: [Trade]
     * * 总是先于对应订单的状态推送
     */
    TD_TRADE_REPORT,
}

/**
 * 推送的事件
 * @param type 事件类型
 * @param sourceId 事件源，参见 [BrokerApi.sourceId]
 * @param data 事件携带的数据，数据类型依据事件类型而定
 */
data class BrokerEvent(
    val type: BrokerEventType,
    val sourceId: String,
    val data: Any,
)

/**
 * 自定义事件
 * @param type 事件类型
 * @param data 事件携带的数据，数据类型依据事件类型而定
 */
data class CustomEvent(
    val type: String,
    val data: Any,
)
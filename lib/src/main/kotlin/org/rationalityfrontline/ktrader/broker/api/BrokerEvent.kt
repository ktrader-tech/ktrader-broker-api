@file:Suppress("unused")

package org.rationalityfrontline.ktrader.broker.api

import org.rationalityfrontline.ktrader.datatype.Order
import org.rationalityfrontline.ktrader.datatype.OrderStatus
import org.rationalityfrontline.ktrader.datatype.Tick
import org.rationalityfrontline.ktrader.datatype.Trade
import java.time.LocalDate

/**
 * 推送事件类型
 */
enum class BrokerEventType {

    /**
     * 自定义事件类型。[BrokerEvent.data]: [CustomEvent]，自定义事件
     */
    CUSTOM_EVENT,

    /**
     * 日志事件。[BrokerEvent.data]: [LogEvent]
     */
    LOG,

    /**
     * 交易日变更。[BrokerEvent.data]: [LocalDate]
     */
    NEW_TRADING_DAY,

    /**
     * 网络连接状态变更。[BrokerEvent.data]: [ConnectionEvent]
     */
    CONNECTION,

    /**
     * [Tick] 行情数据推送。[BrokerEvent.data]: [Tick]
     */
    TICK,

    /**
     * [Bar] 行情数据推送。[BrokerEvent.data]: [Bar]
     */
    Bar,

    /**
     * 订单状态推送。[BrokerEvent.data]: [Order]
     * * [OrderStatus.PARTIALLY_FILLED] 及 [OrderStatus.FILLED] 状态的 [Order] 推送总是后于对应的 [Trade] 推送
     * * 撤单失败时不会推送该事件，但会推送 [CANCEL_FAILED] 事件
     * * 推送的 [Order] 实例不会自己更新状态，是静态的，且每次推送的实例不同
     */
    ORDER_STATUS,

    /**
     * 撤单失败推送。[BrokerEvent.data]: [Order]
     * * 与上一次推送的 [Order] 的唯一区别在于 [Order.statusMsg]
     */
    CANCEL_FAILED,

    /**
     * 成交回报推送。[BrokerEvent.data]: [Trade]
     * * 总是先于对应订单的状态推送
     */
    TRADE_REPORT,
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

/**
 * 日志等级
 */
enum class LogLevel {
    /**
     * 普通信息，用于通知状态变化、执行进度等。
     */
    INFO,

    /**
     * 警告信息，表示发生了可能导致错误的事件。
     */
    WARNING,

    /**
     * 错误信息，表示发生了错误。
     */
    ERROR,
}

/**
 * 日志事件。
 * @param level 日志等级
 * @param msg 消息内容
 */
data class LogEvent(
    val level: LogLevel,
    val msg: String,
)

/**
 * 网络连接及用户登录相关的事件类型
 */
enum class ConnectionEventType {

    /**
     * 自定义事件类型。
     */
    CUSTOM_EVENT,

    /**
     * 行情接口网络连接成功。
     */
    MD_NET_CONNECTED,

    /**
     * 行情接口网络连接断开。
     */
    MD_NET_DISCONNECTED,

    /**
     * 行情接口登录成功。
     */
    MD_LOGGED_IN,

    /**
     * 行情接口退出登录。
     */
    MD_LOGGED_OUT,

    /**
     * 交易接口网络连接成功。
     */
    TD_NET_CONNECTED,

    /**
     * 交易接口网络连接断开。[BrokerEvent.data]: [String]，断开原因
     */
    TD_NET_DISCONNECTED,

    /**
     * 交易接口登录成功。[BrokerEvent.data]: [Unit]
     */
    TD_LOGGED_IN,

    /**
     * 交易接口退出登录。[BrokerEvent.data]: [Unit]
     */
    TD_LOGGED_OUT,
}

/**
 * 网络连接及用户登录相关的事件。
 * @param type 事件类型
 * @param msg 事件描述
 */
data class ConnectionEvent(
    val type: ConnectionEventType,
    val msg: String = "",
)
package org.rationalityfrontline.ktrader.broker.api

enum class BrokerEventType {
    MD_ERROR,
    MD_CUSTOM_EVENT,
    MD_NET_CONNECTED,
    MD_NET_DISCONNECTED,
    MD_USER_LOGGED_IN,
    MD_USER_LOGGED_OUT,
    MD_TICK,
    TD_ERROR,
    TD_CUSTOM_EVENT,
    TD_NET_CONNECTED,
    TD_NET_DISCONNECTED,
    TD_USER_LOGGED_IN,
    TD_USER_LOGGED_OUT,
    TD_ORDER_STATUS,
    TD_ORDER_FILLED,
}

data class BrokerEvent(
    val type: BrokerEventType,
    val sourceId: String,
    val data: Any,
)

data class CustomEvent(
    val type: String,
    val data: Any,
)
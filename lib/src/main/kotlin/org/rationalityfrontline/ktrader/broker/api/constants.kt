package org.rationalityfrontline.ktrader.broker.api

enum class InstrumentType {
    UNKNOWN,
    FUTURES,
    OPTIONS,
    STOCK,
}

enum class OptionsType {
    CALL,
    PUT,
}

enum class Direction {
    LONG,
    SHORT,
}

enum class TickDirection {
    UP,
    STAY,
    DOWN,
}

enum class OrderOffset {
    OPEN,
    CLOSE,
    CLOSE_TODAY,
    CLOSE_YESTERDAY,
}

enum class OrderType {
    CUSTOM,
    LIMIT,
    MARKET,
    FAK,
    FOK,
    STOP,
}

enum class OrderStatus {
    SUBMITTING,
    ACCEPTED,
    PARTIALLY_FILLED,
    FILLED,
    CANCELING,
    CANCELED,
    ERROR,
}

enum class MarketStatus {
    UNKNOWN,
    AUCTION_ORDERING,
    AUCTION_MATCHED,
    CONTINUOUS_MATCHING,
    STOP_TRADING,
    CLOSED,
}
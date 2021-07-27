package org.rationalityfrontline.ktrader.broker.api

enum class InstrumentType {
    UNKNOWN,
    FUTURES,
    OPTIONS,
    STOCK,
}

enum class OptionsType {
    UNKNOWN,
    CALL,
    PUT,
}

enum class Direction {
    UNKNOWN,
    LONG,
    SHORT,
}

enum class TickDirection {
    UNKNOWN,
    UP,
    STAY,
    DOWN,
}

enum class OrderOffset {
    UNKNOWN,
    OPEN,
    CLOSE,
    CLOSE_TODAY,
    CLOSE_YESTERDAY,
}

enum class OrderType {
    UNKNOWN,
    CUSTOM,
    LIMIT,
    MARKET,
    FAK,
    FOK,
    STOP,
}

enum class OrderStatus {
    UNKNOWN,
    SUBMITTING,
    ACCEPTED,
    PARTIALLY_FILLED,
    FILLED,
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
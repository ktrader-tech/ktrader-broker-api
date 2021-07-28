package org.rationalityfrontline.ktrader.broker.api

/**
 * 标的类型
 */
enum class InstrumentType {

    /**
     * 未知
     */
    UNKNOWN,

    /**
     * 期货
     */
    FUTURES,

    /**
     * 期权
     */
    OPTIONS,

    /**
     * 股票
     */
    STOCK,
}

/**
 * 期权类型
 */
enum class OptionsType {

    /**
     * 未知
     */
    UNKNOWN,

    /**
     * 看涨
     */
    CALL,

    /**
     * 看跌
     */
    PUT,
}

/**
 * 交易方向
 */
enum class Direction {

    /**
     * 未知
     */
    UNKNOWN,

    /**
     * 做多
     */
    LONG,

    /**
     * 做空
     */
    SHORT,
}

/**
 * Tick 的方向（最新价相比于上一/这一 Tick 的买一卖一价格）
 */
enum class TickDirection {

    /**
     * 未知
     */
    UNKNOWN,

    /**
     * 向上（最新价 >= 卖一价）
     */
    UP,

    /**
     * 保持（买一价 < 最新价 < 卖一价）
     */
    STAY,

    /**
     * 向下（最新价 <= 买一价）
     */
    DOWN,
}

/**
 * 仓位开平类型
 */
enum class OrderOffset {

    /**
     * 未知
     */
    UNKNOWN,

    /**
     * 开仓
     */
    OPEN,

    /**
     * 平仓
     * * 期货仅上期所/能源中心区分平昨/平今，对其使用平仓等同使用平昨。其它交易所会按持仓情况及手续费最小规则自动选择平昨/平今
     */
    CLOSE,

    /**
     * 平今
     */
    CLOSE_TODAY,

    /**
     * 平昨
     */
    CLOSE_YESTERDAY,
}

/**
 * 订单类型
 */
enum class OrderType {

    /**
     * 未知
     */
    UNKNOWN,

    /**
     * 自定义
     */
    CUSTOM,

    /**
     * 限价单
     */
    LIMIT,

    /**
     * 市价单
     */
    MARKET,

    /**
     * Fill and Kill，即时成交剩余撤销（如果挂单量/最小成交量在该挂单价下能全部成交，则下单能成交的数量并成交，剩余不能成交的下单数量撤销；否则撤销全部下单数量）
     */
    FAK,

    /**
     * Fill or Kill，即时成交或全部撤销（如果挂单量在该挂单价下能全部成交，则下单并成交；否则撤销全部下单数量）
     */
    FOK,

    /**
     * 条件触发单
     */
    STOP,
}

/**
 * 订单状态
 */
enum class OrderStatus {

    /**
     * 未知
     */
    UNKNOWN,

    /**
     * 已发出报单请求但未收到交易所接收确认
     */
    SUBMITTING,

    /**
     * 报单已收到交易所接收确认
     */
    ACCEPTED,

    /**
     * 部分成交
     */
    PARTIALLY_FILLED,

    /**
     * 全部成交
     */
    FILLED,

    /**
     * 已撤单
     */
    CANCELED,

    /**
     * 异常（报单被拒等）
     */
    ERROR,
}

/**
 * 市场状态
 */
enum class MarketStatus {

    /**
     * 未知
     */
    UNKNOWN,

    /**
     * 集合竞价报单
     */
    AUCTION_ORDERING,

    /**
     * 集合竞价撮合
     */
    AUCTION_MATCHED,

    /**
     * 连续交易
     */
    CONTINUOUS_MATCHING,

    /**
     * 暂停交易
     */
    STOP_TRADING,

    /**
     * 闭市
     */
    CLOSED,
}
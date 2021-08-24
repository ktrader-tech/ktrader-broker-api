@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.rationalityfrontline.ktrader.broker.api

import kotlinx.coroutines.*
import org.rationalityfrontline.kevent.KEvent
import org.rationalityfrontline.ktrader.datatype.*
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

/**
 * 虚拟分帐户。在 [parentApi] 中划分出一个虚拟账户，提供独立盈亏统计、行情过滤、交易过滤、Bar 订阅功能。"Sep" 是 "Separate" 的简写。
 * @param parentApi 母账户。可以为另一个 [SepBrokerApi]。
 * @param sepAccount 分帐户 ID。不能包含'-', '_'或空格
 * @param isAsParent 是否作为虚拟母账户（即真实账户下仅有这一个分帐户，且其它分帐户均以本虚拟账户为 [parentApi]）。
 * 虚拟母账户与正常虚拟分帐户的区别为，取消订阅时会取消真实账户的订阅，[connect] 与 [close] 方法也会调用真实账户的对应方法
 */
class SepBrokerApi(val parentApi: BrokerApi, val sepAccount: String, val dataManager: BrokerDataManager, val isAsParent: Boolean = false) : BrokerApi by parentApi {

    init {
        if (sepAccount.contains("[-_\\s]".toRegex())) throw IllegalArgumentException("sepAccount 不能含有'-', '_'或空格")
    }

    override val name: String = "${parentApi.name}-SEP"
    override val account: String = "${parentApi.account}-$sepAccount"
    override val sourceId: String get() = "${name}_${account}_${hashCode()}"
    override val kEvent: KEvent = KEvent(sourceId)

    /**
     * 是否禁用该实例（禁用状态下无法下单）
     */
    var disabled: Boolean = false
        private set

    /**
     * 协程 scope
     */
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * 上次更新的交易日。当 [connected] 处于 false 状态时可能因过期而失效
     */
    private var tradingDay: LocalDate = LocalDate.MIN

    /**
     * 本地维护的账户资金信息
     */
    private val assets: Assets = Assets(account, tradingDay, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

    /**
     * 本地维护的持仓汇总信息
     */
    private val positions: MutableMap<String, BiPosition> = mutableMapOf()

    /**
     * 只读的持仓汇总信息。注意请不要更改其内部的任何对象，否则可能造成数据错误
     */
    val readOnlyPositions: Map<String, BiPosition> = positions

    /**
     * 本地维护的持仓明细信息
     */
    private val positionDetails: MutableMap<String, BiPositionDetails> = mutableMapOf()

    /**
     * 只读的持仓明细信息。注意请不要更改其内部的任何对象，否则可能造成数据错误
     */
    val readOnlyPositionDetails: Map<String, BiPositionDetails> = positionDetails

    /**
     * 本地维护的订单记录，key 为 orderId，value 为 order
     */
    private val todayOrders: MutableMap<String, Order> = mutableMapOf()

    /**
     * 只读的今日订单。注意请不要更改其内部的任何对象，否则可能造成数据错误
     */
    val readOnlyTodayOrders: Map<String, Order> = todayOrders

    /**
     * 本地维护的成交记录
     */
    private val todayTrades: MutableList<Trade> = mutableListOf()

    /**
     * 只读的今日成交记录。注意请不要更改其内部的任何对象，否则可能造成数据错误
     */
    val readOnlyTodayTrades: List<Trade> = todayTrades

    /**
     * 本地缓存的证券信息。仅当前交易日有效，交易日切换时将清空。
     */
    private val securityInfos: MutableMap<String, SecurityInfo> = mutableMapOf()

    /**
     * 当前交易日内已订阅的合约代码集合（当交易日发生更替时上一交易日的订阅会自动失效清零）
     */
    private val tickSubscriptions: MutableSet<String> = mutableSetOf()

    /**
     * 上一次持仓证券的 Tick 推送时间，用于判断是否需要更新并存储持仓汇总及账户资金
     */
    private var lastTickUpdateTime: Long = Long.MAX_VALUE

    /**
     * Bar 生成器
     */
    private val barGenerator: BarGenerator = BarGenerator { bar -> postBrokerEvent(BrokerEventType.Bar, bar) }

    /**
     * 修改账户初始资金（即累计入金 - 累计出金）
     */
    suspend fun modifyInitialCash(newValue: Double) {
        assets.apply {
            initialCash = newValue
            update()
        }
        scope.launch { dataManager.saveAssets(assets) }
    }

    /**
     * 获取真实根账户（即第一个不是 [SepBrokerApi] 的 [parentApi]）
     */
    fun getRootParentApi(): BrokerApi {
        var root = parentApi
        while (root is SepBrokerApi) {
            root = root.parentApi
        }
        return root
    }

    /**
     * 更改该实例禁用状态。禁用后将无法进行下单操作。
     * 如果禁用前存在未完成订单，禁用时会自动撤单
     */
    suspend fun setDisabled(value: Boolean) {
        if (disabled == value) return
        disabled = value
        if (disabled && connected) cancelAllOrders()
    }

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

    /**
     * 获取或查询证券信息
     */
    private suspend fun getOrQuerySecurityInfo(code: String): SecurityInfo? {
        return securityInfos[code] ?: run {
            val securityInfo = querySecurity(code)
            if (securityInfo != null) securityInfos[code] = securityInfo
            return@run securityInfo
        }
    }

    /**
     * 从 [dataManager] 中读取数据并赋值到当前实例
     */
    private suspend fun restore() {
        val tradingDay = dataManager.queryLastTradingDay(account) ?: LocalDate.MIN
        val assets = dataManager.queryAssets(account, tradingDay).firstOrNull() ?: Assets(account, tradingDay, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        val positions = dataManager.queryPositions(account, tradingDay, null, null)
        val positionDetails = dataManager.queryPositionDetails(account, null, null)
        val todayOrders = dataManager.queryOrders(account, tradingDay, null, null)
        val todayTrades = dataManager.queryTrades(account, tradingDay, null, null)
        restore(tradingDay, assets, positions, positionDetails, todayOrders, todayTrades)
    }

    /**
     * 将当前实例的值与传入的值对齐
     */
    private fun restore(
        tradingDay: LocalDate, assets: Assets,
        positions: List<Position>, positionDetails: List<PositionDetail>,
        todayOrders: List<Order>, todayTrades: List<Trade>
    ) {
        this.tradingDay = tradingDay
        this.assets.apply {
            this.tradingDay = assets.tradingDay
            total = assets.total
            available = assets.available
            positionValue = assets.positionValue
            positionPnl = assets.positionPnl
            frozenByOrder = assets.frozenByOrder
            todayCommission = assets.todayCommission
            initialCash = assets.initialCash
            totalClosePnl = assets.totalClosePnl
            totalCommission = assets.totalCommission
            extras = assets.extras?.toMutableMap()

        }
        this.positions.clear()
        this.positions.putAll(positions.toBiPositionsMap())
        this.positionDetails.clear()
        this.positionDetails.putAll(positionDetails.toBiPositionDetailsMap(account))
        this.todayOrders.clear()
        todayOrders.forEach {
            this.todayOrders[it.orderId] = it
        }
        this.todayTrades.clear()
        this.todayTrades.addAll(todayTrades)
    }

    /**
     * 处理交易日变更
     */
    private suspend fun handleNewTradingDay(newTradingDay: LocalDate) {
        if (newTradingDay == tradingDay) return
        tradingDay = newTradingDay
        scope.launch { dataManager.saveTradingDay(account, tradingDay) }
        todayOrders.clear()
        todayTrades.clear()
        securityInfos.clear()
        tickSubscriptions.clear()
        barGenerator.reset()
        // 刷新持仓明细
        val detailsList = mutableListOf<PositionDetails>()
        positionDetails.values.forEach {
            it.long?.run { detailsList.add(this) }
            it.short?.run { detailsList.add(this) }
        }
        detailsList.forEach { it.details.forEach { detail ->
            detail.apply { todayVolume = 0 }
            scope.launch { dataManager.savePositionDetail(detail) }
        } }
        // 刷新持仓汇总
        val positionList = mutableListOf<Position>()
        positions.values.forEach {
            it.long?.run { positionList.add(this) }
            it.short?.run { positionList.add(this) }
        }
        positionList.forEach {
            it.apply {
                tradingDay = newTradingDay
                preVolume = volume
                todayVolume = 0
                frozenVolume = 0
                todayOpenVolume = 0
                todayCloseVolume = 0
                todayCommission = 0.0
            }
            scope.launch { dataManager.savePosition(it) }
        }
        // 刷新资产信息
        assets.apply {
            tradingDay = newTradingDay
            available += frozenByOrder
            frozenByOrder = 0.0
            todayCommission = 0.0
        }
        scope.launch { dataManager.saveAssets(assets) }
        postBrokerEvent(BrokerEventType.NEW_TRADING_DAY, newTradingDay)
    }

    /**
     * 刷新计算所有持仓的 value 及 pnl，然后刷新计算当前 assets
     */
    private suspend fun updateAssets(saveData: Boolean = false) {
        var positionValueSum = 0.0
        var positionPnlSum = 0.0
        val positionList = mutableListOf<Position>()
        positions.values.forEach {
            it.long?.run { positionList.add(this) }
            it.short?.run { positionList.add(this) }
        }
        positionList.forEach {
            calculatePosition(it)
            positionValueSum += it.value
            positionPnlSum += it.pnl
            if (saveData) scope.launch { dataManager.savePosition(it) }
        }
        // 安排 assets
        assets.apply {
            positionValue = positionValueSum
            positionPnl = positionPnlSum
            update()
        }
        if (saveData) scope.launch { dataManager.saveAssets(assets) }
    }

    /**
     * 获取 [order] 对应平仓的持仓价位
     */
    private fun getOrderClosePositionPrice(order: Order): Double {
        return order.closePositionPrice ?: when (order.direction) {
            Direction.LONG -> Double.POSITIVE_INFINITY
            Direction.SHORT -> Double.NEGATIVE_INFINITY
            else -> Double.NEGATIVE_INFINITY
        }
    }

    /**
     * 获取或创建 [order] 对应的持仓汇总信息（并不会用 [dataManager] 保存）
     */
    private fun getOrCreatePosition(order: Order): Position {
        val biPosition = positions.getOrPut(order.code) { BiPosition() }
        val position: Position = when {
            order.direction == Direction.LONG && order.offset == OrderOffset.OPEN ||
                    order.direction == Direction.SHORT && order.offset != OrderOffset.OPEN -> {
                if (biPosition.long == null) {
                    biPosition.long = Position(
                        account, tradingDay,
                        order.code, Direction.LONG, 0, 0, 0, 0, 0, 0,
                        0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
                    )
                }
                biPosition.long!!
            }
            order.direction == Direction.SHORT && order.offset == OrderOffset.OPEN ||
                    order.direction == Direction.LONG && order.offset != OrderOffset.OPEN -> {
                if (biPosition.short == null) {
                    biPosition.short = Position(
                        account, tradingDay,
                        order.code, Direction.SHORT, 0, 0, 0,  0, 0, 0,
                        0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
                    )
                }
                biPosition.short!!
            }
            else -> throw Exception("未知的开平操作：${order.direction}, ${order.offset}")
        }
        return position
    }

    /**
     * 获取或创建 [order] 对应的持仓明细信息（并不会用 [dataManager] 保存）
     */
    private fun getOrCreatePositionDetails(order: Order): PositionDetails {
        val biDetails = positionDetails.getOrPut(order.code) { BiPositionDetails() }
        val details: PositionDetails = when {
            order.direction == Direction.LONG && order.offset == OrderOffset.OPEN ||
                    order.direction == Direction.SHORT && order.offset != OrderOffset.OPEN -> {
                if (biDetails.long == null) {
                    biDetails.long = PositionDetails(account, order.code, Direction.LONG)
                }
                biDetails.long!!
            }
            order.direction == Direction.SHORT && order.offset == OrderOffset.OPEN ||
                    order.direction == Direction.LONG && order.offset != OrderOffset.OPEN -> {
                if (biDetails.short == null) {
                    biDetails.short = PositionDetails(account, order.code, Direction.SHORT)
                }
                biDetails.short!!
            }
            else -> throw Exception("未知的开平操作：${order.direction}, ${order.offset}")
        }
        return details
    }

    /**
     * 冻结 [order] 所冻结的持仓（仅限平仓订单）
     */
    private fun freezePosition(order: Order) {
        val position: Position = getOrCreatePosition(order)
        when (order.offset) {
            OrderOffset.CLOSE,
            OrderOffset.CLOSE_YESTERDAY -> {
                position.frozenVolume += order.volume
            }
            OrderOffset.CLOSE_TODAY -> {
                position.frozenVolume += order.volume
                position.frozenTodayVolume += order.volume
            }
            else -> return
        }
        scope.launch { dataManager.savePosition(position) }
    }

    /**
     * 取消冻结 [order] 所冻结的持仓（仅限平仓订单）
     */
    private fun unfreezePosition(order: Order) {
        val position: Position = getOrCreatePosition(order)
        val restVolume = order.volume - order.filledVolume
        if (restVolume > 0) {
            when (order.offset) {
                OrderOffset.CLOSE,
                OrderOffset.CLOSE_YESTERDAY -> {
                    position.frozenVolume -= restVolume
                }
                OrderOffset.CLOSE_TODAY -> {
                    position.frozenVolume -= restVolume
                    position.frozenTodayVolume -= restVolume
                }
                else -> return
            }
            scope.launch { dataManager.savePosition(position) }
        }
    }

    /**
     * 依据 [order] 与 [trade] 更新持仓信息（包括持仓汇总、持仓明细、账户资金）
     */
    private suspend fun updatePosition(order: Order, trade: Trade) {
        if (trade.offset == OrderOffset.UNKNOWN) return
        val volumeMultiple = getOrQuerySecurityInfo(trade.code)?.volumeMultiple ?: return
        val position = getOrCreatePosition(order)
        val details = getOrCreatePositionDetails(order)
        val detailList = details.details
        if (trade.offset == OrderOffset.OPEN) {
            position.volume += trade.volume
            position.todayVolume += trade.volume
            position.todayOpenVolume += trade.volume
            assets.frozenByOrder -= order.frozenCash * trade.volume / order.volume
            val index = detailList.binarySearch { sign(it.price - trade.price).toInt() }
            val detail = if (index >= 0) {
                detailList[index]
            } else {
                val newDetail = PositionDetail(
                    accountId = account,
                    code = trade.code,
                    direction = trade.direction,
                    price = trade.price,
                )
                detailList.add(-index - 1, newDetail)
                newDetail
            }
            detail.volume += trade.volume
            detail.todayVolume += trade.volume
            detail.updateTime = trade.time
            details.updatePosition(position, volumeMultiple)
            scope.launch {
                dataManager.savePosition(position)
                dataManager.saveAssets(assets)
                dataManager.savePositionDetail(detail)
            }
        } else {
            position.volume -= trade.volume
            position.frozenVolume -= trade.volume
            position.todayCloseVolume += trade.volume
            if (trade.offset == OrderOffset.CLOSE_TODAY) {
                position.todayVolume -= trade.volume
                position.frozenTodayVolume -= trade.volume
            }
            val closePositionPrice = getOrderClosePositionPrice(order)
            var restVolume = trade.volume
            var index = detailList.binarySearch { sign(it.price - closePositionPrice).toInt() }
            if (index < 0) index = -index -1
            val filter: (PositionDetail) -> Boolean = when (trade.offset) {
                OrderOffset.CLOSE_TODAY -> { { it.todayVolume > 0 } }
                OrderOffset.CLOSE_YESTERDAY -> { { it.yesterdayVolume > 0 } }
                else -> { { it.volume > 0 } }
            }
            var index1 = index -1
            var index2 = index
            var detail1: PositionDetail? = null
            var detail2: PositionDetail? = null
            val pnlSign = when (position.direction) {
                Direction.LONG -> 1
                Direction.SHORT -> -1
                else -> return
            }
            while (restVolume > 0) {
                while (index1 >= 0) {
                    detail1 = detailList.getOrNull(index1)
                    if (detail1 != null && filter(detail1)) {
                        break
                    } else {
                        index1--
                    }
                }
                while (index2 < detailList.size) {
                    detail2 = detailList.getOrNull(index2)
                    if (detail2 != null && filter(detail2)) {
                        break
                    } else {
                        index2++
                    }
                }
                var closeDetail: PositionDetail? = null
                if (detail1 == null && detail2 == null) break
                if (detail1 == null) closeDetail = detail2
                if (detail2 == null) closeDetail = detail1
                if (closeDetail == null) {
                    closeDetail = if (abs(detail1!!.price - closePositionPrice) > abs(detail2!!.price - closePositionPrice)) detail2 else detail1
                }
                when (trade.offset) {
                    OrderOffset.CLOSE_TODAY -> {
                        val closeVolume = min(closeDetail.todayVolume, restVolume)
                        closeDetail.volume -= closeVolume
                        closeDetail.todayVolume -= closeVolume
                        restVolume -= closeVolume
                        assets.totalClosePnl += pnlSign * closeVolume * (trade.price - closeDetail.price)
                    }
                    OrderOffset.CLOSE_YESTERDAY -> {
                        val closeVolume = min(closeDetail.yesterdayVolume, restVolume)
                        closeDetail.volume -= closeVolume
                        restVolume -= closeVolume
                        assets.totalClosePnl += pnlSign * closeVolume * (trade.price - closeDetail.price)
                    }
                    else -> {
                        val closeVolume = min(closeDetail.volume, restVolume)
                        val closeTodayVolume = if (closeVolume > closeDetail.yesterdayVolume) closeVolume - closeDetail.yesterdayVolume else 0
                        closeDetail.volume -= closeVolume
                        closeDetail.todayVolume -= closeTodayVolume
                        restVolume -= closeVolume
                        assets.totalClosePnl += pnlSign * closeVolume * (trade.price - closeDetail.price)
                    }
                }
                scope.launch { dataManager.savePositionDetail(closeDetail) }
            }
            details.updatePosition(position, volumeMultiple)
            scope.launch {
                dataManager.savePosition(position)
                dataManager.saveAssets(assets)
            }
        }
    }

    /**
     * 依据 [order] 获取持仓汇总，然后记录 [commission]
     */
    private suspend fun addCommission(order: Order, commission: Double) {
        getOrCreatePosition(order).apply {
            todayCommission += commission
            scope.launch { dataManager.savePosition(this@apply) }
        }
        assets.todayCommission += commission
        assets.totalCommission += commission
        scope.launch { dataManager.saveAssets(assets) }
    }

    /**
     * 向 [parentApi] 注册事件监听，实现独立统计。如果 [isAsParent] 为 true，会调用 parentApi.connect
     */
    override suspend fun connect(extras: Map<String, String>?) {
        postBrokerLogEvent(LogLevel.INFO, "【SepBrokerApi.connect】开始连接")
        postBrokerLogEvent(LogLevel.INFO, "【SepBrokerApi.connect】读取数据...")
        restore()
        postBrokerLogEvent(LogLevel.INFO, "【SepBrokerApi.connect】读取数据成功")
        parentApi.kEvent.subscribeMultiple<BrokerEvent>(BrokerEventType.values().asList(), tag = sourceId) { event -> runBlocking {
            val brokerEvent = event.data
            val data = brokerEvent.data
            when (brokerEvent.type) {
                BrokerEventType.NEW_TRADING_DAY -> {
                    // 仅在已连接时处理交易日更新，否则在连接成功时主动检测是否发生交易日变更
                    if (connected) {
                        handleNewTradingDay(data as LocalDate)
                    }
                }
                BrokerEventType.CONNECTION -> {
                    kEvent.post(event)
                    // 连接成功时主动检测是否发生交易日变更
                    val conEvent = data as ConnectionEvent
                    if (conEvent.type == ConnectionEventType.TD_LOGGED_IN) {
                        val currentTradingDay = parentApi.getTradingDay()
                        if (currentTradingDay != tradingDay) {
                            handleNewTradingDay(currentTradingDay)
                        }
                    }
                }
                BrokerEventType.TICK -> {
                    val tick = data as Tick
                    // Tick 过滤转发
                    if (tick.code in tickSubscriptions) {
                        kEvent.post(event)
                    }
                    // 生成 Bar
                    barGenerator.updateTick(tick)
                    // 判断是否需要保存当日持仓汇总信息及资金信息（主要用于实现每日收盘时的记录）
                    if (tick.code in positions) {
                        lastTickUpdateTime = System.currentTimeMillis()
                        launch {
                            delay(60)
                            if (System.currentTimeMillis() - lastTickUpdateTime > 55 && connected) {
                                updateAssets(true)
                            }
                        }
                    }
                }
                BrokerEventType.Bar -> Unit
                BrokerEventType.ORDER_STATUS -> {
                    val order = data as Order
                    if (order.orderId in todayOrders) {
                        val localOrder = todayOrders[order.orderId]!!
                        // 检查是否存在挂单手续费
                        if (order.status == OrderStatus.ACCEPTED && order.commission > 0.0) {
                            addCommission(localOrder, order.commission)
                        }
                        // 检查是否存在撤单手续费
                        if (order.status == OrderStatus.CANCELED) {
                            val deltaCommission = order.commission - localOrder.commission
                            if (deltaCommission > 0) {
                                addCommission(localOrder, deltaCommission)
                            }
                        }
                        localOrder.update(order)
                        launch { dataManager.saveOrder(localOrder) }
                        // 如果订单已关闭则释放冻结仓位
                        if (localOrder.status == OrderStatus.CANCELED || localOrder.status == OrderStatus.ERROR) {
                            unfreezePosition(localOrder)
                        }
                        postBrokerEvent(BrokerEventType.ORDER_STATUS, localOrder.deepCopy())
                    }
                }
                BrokerEventType.CANCEL_FAILED -> {
                    val order = data as Order
                    if (order.orderId in todayOrders) {
                        val localOrder = todayOrders[order.orderId]!!
                        localOrder.statusMsg = order.statusMsg
                        launch { dataManager.saveOrder(localOrder) }
                        postBrokerEvent(BrokerEventType.CANCEL_FAILED, localOrder.deepCopy())
                    }
                }
                BrokerEventType.TRADE_REPORT -> {
                    val trade = data as Trade
                    if (trade.orderId in todayOrders) {
                        val localTrade = trade.deepCopy().apply { accountId = account }
                        todayTrades.add(localTrade)
                        val localOrder = todayOrders[trade.orderId]!!
                        launch { dataManager.saveTrade(localTrade) }
                        updatePosition(localOrder, localTrade)
                        addCommission(localOrder, localTrade.commission)
                        postBrokerEvent(BrokerEventType.TRADE_REPORT, localTrade)
                    }
                }
                BrokerEventType.LOG,
                BrokerEventType.CUSTOM_EVENT -> kEvent.post(event)
            }
        } }
        if (isAsParent) {
            parentApi.connect(extras)
        } else {
            if (connected) {
                // 应对 parentApi 已连接时调用 connect 的情况
                val currentTradingDay = parentApi.getTradingDay()
                if (currentTradingDay != tradingDay) {
                    handleNewTradingDay(currentTradingDay)
                }
                // 自动订阅持仓证券行情
                parentApi.subscribeTicks(positions.keys)
            }
        }
        postBrokerLogEvent(LogLevel.INFO, "【SepBrokerApi.connect】连接成功")
    }

    /**
     * 取消对 [parentApi] 的事件监听，并释放本实例资源。如果 [isAsParent] 为 true，会调用 parentApi.close
     */
    override fun close() {
        postBrokerLogEvent(LogLevel.INFO, "【SepBrokerApi.close】开始关闭")
        if (isAsParent) parentApi.close()
        parentApi.kEvent.removeSubscribersByTag(sourceId)
        barGenerator.release()
        scope.cancel()
        postBrokerLogEvent(LogLevel.INFO, "【SepBrokerApi.close】关闭成功")
        kEvent.release()
    }

    override suspend fun subscribeTick(code: String, extras: Map<String, String>?) {
        parentApi.subscribeTick(code, extras)
        tickSubscriptions.add(code)
    }

    override suspend fun unsubscribeTick(code: String, extras: Map<String, String>?) {
        if (isAsParent && code !in barGenerator) parentApi.unsubscribeTick(code, extras)
        tickSubscriptions.remove(code)
    }

    override suspend fun subscribeTicks(codes: Collection<String>, extras: Map<String, String>?) {
        parentApi.subscribeTicks(codes, extras)
        tickSubscriptions.addAll(codes)
    }

    override suspend fun unsubscribeTicks(codes: Collection<String>, extras: Map<String, String>?) {
        if (isAsParent) parentApi.unsubscribeTicks(codes.filter { it !in barGenerator }, extras)
        tickSubscriptions.removeAll(codes)
    }

    override suspend fun subscribeAllTicks(extras: Map<String, String>?) {
        parentApi.subscribeAllTicks(extras)
        tickSubscriptions.addAll(parentApi.queryTickSubscriptions())
    }

    override suspend fun unsubscribeAllTicks(extras: Map<String, String>?) {
        if (isAsParent && barGenerator.queryBarSubscriptions(true).isEmpty()) parentApi.unsubscribeAllTicks(extras)
        tickSubscriptions.clear()
    }

    override suspend fun queryTickSubscriptions(useCache: Boolean, extras: Map<String, String>?): List<String> {
        return tickSubscriptions.toList()
    }

    /**
     * 订阅证券 [Bar] 行情
     * @param code 证券代码
     * @param interval Bar 的频率
     */
    suspend fun subscribeBar(code: String, interval: Int) {
        parentApi.subscribeTick(code)
        barGenerator.subscribeBar(code, interval)
    }

    /**
     * 取消 [Bar] 行情
     * @param code 证券代码
     * @param interval Bar 的频率
     */
    suspend fun unsubscribeBar(code: String, interval: Int) {
        barGenerator.unsubscribeBar(code, interval)
        if (isAsParent && code !in tickSubscriptions && code !in barGenerator) {
            parentApi.unsubscribeTick(code)
        }
    }

    /**
     * 查询当前已订阅 [Bar] 行情的证券
     */
    fun queryBarSubscriptions(): List<BarInfo> {
        return barGenerator.queryBarSubscriptions(true)
    }

    override suspend fun queryAssets(useCache: Boolean, extras: Map<String, String>?): Assets {
        updateAssets()
        return assets.deepCopy()
    }

    override suspend fun queryPosition(
        code: String,
        direction: Direction,
        useCache: Boolean,
        extras: Map<String, String>?
    ): Position? {
        val biPosition = positions[code] ?: return null
        return when (direction) {
            Direction.LONG -> biPosition.long
            Direction.SHORT -> biPosition.short
            else -> null
        }?.let {
            calculatePosition(it)
            it.deepCopy()
        }
    }

    override suspend fun queryPositions(code: String?, useCache: Boolean, extras: Map<String, String>?): List<Position> {
        val biPositions = if (code == null) positions.values else positions[code]?.run { listOf(this) } ?: listOf()
        val results = mutableListOf<Position>()
        biPositions.forEach {
            it.long?.run { results.add(this) }
            it.short?.run { results.add(this) }
        }
        return results.map {
            calculatePosition(it)
            it.deepCopy()
        }
    }

    override suspend fun queryPositionDetails(
        code: String,
        direction: Direction,
        useCache: Boolean,
        extras: Map<String, String>?
    ): PositionDetails? {
        val biDetails = positionDetails[code] ?: return null
        return when (direction) {
            Direction.LONG -> biDetails.long
            Direction.SHORT -> biDetails.short
            else -> null
        }?.deepCopy()
    }

    override suspend fun queryPositionDetails(
        code: String?,
        useCache: Boolean,
        extras: Map<String, String>?
    ): List<PositionDetails> {
        val biDetails = if (code == null) positionDetails.values else positionDetails[code]?.run { listOf(this) } ?: listOf()
        val results = mutableListOf<PositionDetails>()
        biDetails.forEach {
            it.long?.run { results.add(deepCopy()) }
            it.short?.run { results.add(deepCopy()) }
        }
        return results
    }

    override suspend fun queryOrder(orderId: String, useCache: Boolean, extras: Map<String, String>?): Order? {
        return todayOrders.values.find { it.orderId == orderId }?.deepCopy()
    }

    override suspend fun queryOrders(
        code: String?,
        onlyUnfinished: Boolean,
        useCache: Boolean,
        extras: Map<String, String>?
    ): List<Order> {
        val finishedStatus = setOf(OrderStatus.FILLED, OrderStatus.CANCELED, OrderStatus.ERROR)
        return todayOrders.values.filter {
            (code?.run { it.code == this } ?: true) && (if (onlyUnfinished) it.status !in finishedStatus else true)
        }.map { it.deepCopy() }
    }

    override suspend fun queryTrade(tradeId: String, useCache: Boolean, extras: Map<String, String>?): Trade? {
        return todayTrades.find { it.tradeId == tradeId }?.deepCopy()
    }

    override suspend fun queryTrades(
        code: String?,
        orderId: String?,
        useCache: Boolean,
        extras: Map<String, String>?
    ): List<Trade> {
        return todayTrades.filter {
            (code?.run { it.code == this } ?: true) && (orderId?.run { it.orderId == this } ?: true)
        }.map { it.deepCopy() }
    }

    /**
     * 下单。平仓指定价对于平多为持仓最低价，对于平空为持仓最高价。
     * @param code 证券代码
     * @param price 下单价格
     * @param volume 下单数量
     * @param direction 交易方向（做多/做空）
     * @param offset 开平类型（开仓/平仓/平今/平昨）
     * @param orderType 订单类型（限价单/市价单/FAK/FOK等）
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
        extras: Map<String, String>?
    ): Order {
        return insertOrder(code, price, volume, direction, offset, null, orderType, extras)
    }

    /**
     * 下单
     * @param code 证券代码
     * @param price 下单价格
     * @param volume 下单数量
     * @param direction 交易方向（做多/做空）
     * @param offset 开平类型（开仓/平仓/平今/平昨）
     * @param closePositionPrice 平仓时指定的所平仓位的开仓价。如果平仓量超过对应开仓价的持仓数量，则剩余平仓量按最接近指定开仓价的持仓依次平仓
     * @param orderType 订单类型（限价单/市价单/FAK/FOK等）
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
        extras: Map<String, String>?
    ): Order {
        if (disabled) throw Exception("该实例已被禁止下单")
        // 校验订单（可用资金及可平仓位是否满足要求）
        var errorInfo: String? = null
        if (offset == OrderOffset.OPEN) {
            val o = Order("", "", code, price, volume, direction, offset, orderType, OrderStatus.SUBMITTING, "", 0, 0.0, 0.0, 0.0, 0.0, LocalDateTime.MIN, LocalDateTime.MIN)
            parentApi.calculateOrder(o)
            updateAssets()
            if (assets.available < o.frozenCash) {
                errorInfo = "可用资金不足：需要 ${o.frozenCash}，可用 ${assets.available}"
            }
        } else {
            val position = when (direction) {
                Direction.LONG -> positions[code]?.short
                Direction.SHORT -> positions[code]?.long
                else -> null
            }
            if (position == null) {
                errorInfo = "可平仓位不足：需要 $volume，可用 0"
            } else {
                var closeableVolume = 0
                when (offset) {
                    OrderOffset.CLOSE -> {
                        closeableVolume = position.volume - position.frozenVolume
                    }
                    OrderOffset.CLOSE_TODAY -> {
                        closeableVolume = position.todayVolume - position.frozenTodayVolume
                    }
                    OrderOffset.CLOSE_YESTERDAY -> {
                        closeableVolume = position.yesterdayVolume - position.frozenYesterdayVolume
                    }
                    else -> Unit
                }
                if (closeableVolume < volume) {
                    errorInfo = "可平仓位不足：需要 $volume，可用 $closeableVolume"
                }
            }
        }
        if (errorInfo != null) throw Exception("订单校验失败：$errorInfo")
        // 下单
        val order = parentApi.insertOrder(code, price, volume, direction, offset, orderType, extras)
        // 记录订单
        order.closePositionPrice = closePositionPrice
        order.accountId = account
        todayOrders[order.orderId] = order
        scope.launch {
            dataManager.saveOrder(order)
            parentApi.subscribeTick(order.code)
        }
        // 更新下单成功带来的账户信息变动
        if (order.status != OrderStatus.ERROR && order.status != OrderStatus.CANCELED) {
            if (offset == OrderOffset.OPEN) {
                assets.frozenByOrder += order.frozenCash
                assets.calculateAvailable()
            } else {
                freezePosition(order)
            }
        }
        return order.deepCopy()
    }

    override suspend fun cancelOrder(orderId: String, extras: Map<String, String>?) {
        if (orderId in todayOrders) {
            parentApi.cancelOrder(orderId, extras)
        } else {
            throw Exception("未找到 $orderId 对应的订单记录")
        }
    }

    override suspend fun cancelAllOrders(extras: Map<String, String>?) {
        queryOrders(onlyUnfinished = true).forEach {
            cancelOrder(it.orderId)
        }
    }
}
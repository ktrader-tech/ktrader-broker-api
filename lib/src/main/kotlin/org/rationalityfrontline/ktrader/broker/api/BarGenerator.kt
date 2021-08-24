package org.rationalityfrontline.ktrader.broker.api

import kotlinx.coroutines.*
import org.rationalityfrontline.ktrader.datatype.Bar
import org.rationalityfrontline.ktrader.datatype.BarInfo
import org.rationalityfrontline.ktrader.datatype.MarketStatus
import org.rationalityfrontline.ktrader.datatype.Tick
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * 60 秒频率内的 [Bar] 实时生成器。[interval] 仅支持能整除 60 的正整数（2, 3, 5, 6, 10, 15, 20, 30, 60）。
 * ## Bar 的生成规则：
 * 预期的单交易日内流程为：可能存在开盘集合竞价（多笔报价 + 一笔撮合成交）-> 连续竞价交易（中间可能夹着多次暂时休市）-> 可能存在收盘集合竞价（多笔报价 + 一笔撮合成交）-> 闭市
 * 1. 对于集合竞价，只统计撮合成交的那一笔
 * 2. 对于开盘集合竞价，将其撮合成交的那一笔统计到连续竞价交易的第一根 Bar 中，且为其开盘价
 * 3. 进入连续竞价交易状态后，每隔 [interval] 秒推送一次 Bar，与期间是否有 Tick 推送（有成交）无关
 * 4. 在休市状态不推送 Bar
 * 5. 如果收到的 Tick 时间在当前 Bar 的 endTime 之后但未超时超过 1 秒，那么将该 Tick 计入当前 Bar，并将其价格作为下一 Bar 的开盘价
 * 6. 预期当证券交易状态发生变动时，无论是否有实际成交，都会推送状态变更的 Tick
 *
 * @param code 证券代码
 * @param interval Bar 的频率（单位：秒）
 * @param barReceiver Bar 的推送回调方法
 */
class SecondBarGenerator(val code: String, val interval: Int, val scope: CoroutineScope, val barReceiver: (Bar) -> Unit) {

    /**
     * 当前的 Bar
     */
    lateinit var currentBar: Bar
        private set

    /**
     * 当前的市场交易状态
     */
    var marketStatus: MarketStatus = MarketStatus.UNKNOWN
        private set

    /**
     * 开盘集合竞价撮合成交的 Tick
     */
    private var firstAuctionTick: Tick? = null

    init {
        val validIntervals = setOf(2, 3, 5, 6, 10, 15, 20, 30, 60)
        // 校验 interval 合法性
        if (interval !in validIntervals) throw IllegalArgumentException("SecondBarGenerator 仅支持生成以下秒级频率的 Bar：$validIntervals")
        reset()
    }

    /**
     * 向 [barReceiver] 推送当前 Bar
     */
    private fun postBar() {
        // 不推送无更新的初始 Bar
        if (currentBar.openPrice != 0.0) {
            barReceiver.invoke(currentBar)
        }
    }

    /**
     * 判断当前是否处于暂停/停止交易的状态
     */
    private fun isStopGenerating(status: MarketStatus = marketStatus): Boolean {
        return status == MarketStatus.STOP_TRADING || status == MarketStatus.CLOSED
    }

    /**
     * 生成下一根 Bar 并将其作为当前 Bar。将依据 [tick] 的时间决定新的 [Bar.startTime]，并将 tick 价格作为新 Bar 的开盘价。
     * 在当前 Bar 的 endTime + 1秒 后，将检查新的 Bar 是否已生成，如果未生成，将推送当前 Bar 并生成新的 Bar (如果 ![isStopGenerating])
     */
    private fun generateNextBar(tick: Tick) {
        if (!isStopGenerating()) {
            val tickSecond = tick.time.second
            val newBar = Bar.createBar(code, interval, tick.time.truncatedTo(ChronoUnit.SECONDS).withSecond(tickSecond - tickSecond % interval), openPrice = tick.lastPrice)
            currentBar = newBar
            scope.launch {
                delay((LocalDateTime.now().until(newBar.endTime, ChronoUnit.SECONDS) + 1) * 1000)
                // 如果时间到了但该 bar 仍未被推送出去，强制推送
                if (currentBar == newBar) postBar()
                generateNextBar(tick.copy(time = newBar.endTime, lastPrice = newBar.closePrice))
            }
        }
    }

    /**
     * 依据 [tick] 来更新自身数值
     */
    fun updateTick(tick: Tick) {
        // 闭市后忽略一切更新
        if (marketStatus == MarketStatus.CLOSED) return
        // 从非交易状态进入交易状态，无视成交量新建 bar
        if (isStopGenerating() && !isStopGenerating(tick.status)) {
            generateNextBar(tick)
        }
        marketStatus = tick.status
        // 如果是无成交量 Tick，不做处理
        if (tick.volume == 0) return
        // 只要成交量不为 0，除了交易日开盘首次集合竞价的数据要合并到当日首根 bar 以外，其它的都正常更新
        if (marketStatus == MarketStatus.AUCTION_ORDERING || marketStatus == MarketStatus.AUCTION_MATCHED) {
            if (firstAuctionTick == null) {
                firstAuctionTick = tick
                return
            }
        }
        // 如果是初始 bar 且存在集合竞价，那么合并 tick
        if (currentBar.openPrice == 0.0 && firstAuctionTick != null) {
            generateNextBar(tick.copy(lastPrice = firstAuctionTick!!.lastPrice))
            currentBar.updateTick(firstAuctionTick!!)
        }
        // 如果当前 bar 已结束
        if (!tick.time.isBefore(currentBar.endTime)) {
            // 如果时间差低于 1 秒，那么视作当前 bar 的数据，并且将该 tick 的价格同时作为当前 bar 的收盘价及下一 bar 的开盘价
            if (tick.time.isBefore(currentBar.endTime.plusSeconds(1))) {
                currentBar.updateTick(tick)
                postBar()
                generateNextBar(tick)
            } else {  // 如果时间差大于 1 秒，那么认为该 tick 与当前 bar 无关
                postBar()
                generateNextBar(tick)
                currentBar.updateTick(tick)
            }
        } else {
            // 如果 bar 还未结束且已开始，更新 tick
            if (tick.time.isAfter(currentBar.startTime)) {
                currentBar.updateTick(tick)
            }
        }
    }

    /**
     * 重置生成器的状态，使其可以在下一交易日正常工作
     */
    fun reset() {
        marketStatus = MarketStatus.UNKNOWN
        firstAuctionTick = null
        // 以最近的整数分钟创建当前 Bar（该 Bar 开盘价为 0.0，如果该 Bar 结束时开盘价依然为 0.0，则不会推送该 Bar）
        val truncatedTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        val plusSeconds = when (truncatedTime.second) {
            0 -> 0
            else -> 60 - truncatedTime.second
        }.toLong()
        currentBar = Bar.createBar(code, interval, truncatedTime.plusSeconds(plusSeconds))
    }
}

/**
 * 任意频率的 [Bar] 实时生成器。
 */
class BarGenerator(val barReceiver: (Bar) -> Unit) {
    private val barSubscriptions: MutableSet<BarInfo> = mutableSetOf()
    private val userSubscriptions: MutableSet<BarInfo> = mutableSetOf()
    private val secondBarGenerators: MutableMap<String, MutableSet<SecondBarGenerator>> = mutableMapOf()
    private val minuteBarCaches: MutableMap<String, MutableList<Bar>> = mutableMapOf()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * 订阅 Bar
     * @param code 证券代码
     * @param interval Bar 的频率
     */
    fun subscribeBar(code: String, interval: Int) {
        subscribeBar(code, interval, true)
    }

    /**
     * 订阅 Bar
     * @param code 证券代码
     * @param interval Bar 的频率
     * @param isUser 是否是用户调用的。
     */
    private fun subscribeBar(code: String, interval: Int, isUser: Boolean) {
        val barInfo = BarInfo(code, interval)
        if (barInfo !in barSubscriptions) {
            when {
                interval <= 0 -> throw IllegalArgumentException("Bar.interval 必须 > 0")
                interval <= 60 -> {
                    val sbgReceiver: (Bar) -> Unit = if (interval == 60) {
                        { bar ->
                            if (isUser) barReceiver.invoke(bar)
                            // 存入缓存并检查是否需要生成分钟以上级别的 Bar
                            minuteBarCaches[code]?.let { barCache ->
                                barCache.add(bar)
                                val size = barCache.size
                                userSubscriptions.filter { it.code == code && it.interval > 60 }.forEach { barInfo ->
                                    val count = barInfo.interval / 60
                                    if (size % count == 0) {
                                        val minuteBars = barCache.takeLast(count)
                                        val newBar = Bar.createBar(code, barInfo.interval, minuteBars[0].startTime, minuteBars.last().endTime, minuteBars[0].openPrice)
                                        minuteBars.forEach { newBar.updateBar(it) }
                                        barReceiver.invoke(newBar)
                                    }
                                }
                            }
                        }
                    } else {
                        { bar ->
                            barReceiver.invoke(bar)
                        }
                    }
                    val sbgSet = secondBarGenerators.getOrPut(code) { mutableSetOf() }
                    sbgSet.add(SecondBarGenerator(code, interval, scope, sbgReceiver))
                }
                else -> {
                    if (interval % 60 != 0) throw IllegalArgumentException("Bar.interval > 60 时必须为 60 的整数倍")
                    minuteBarCaches[code] = mutableListOf()
                    subscribeBar(code, 60, false)
                }
            }
            barSubscriptions.add(barInfo)
        }
        if (isUser) userSubscriptions.add(barInfo)
    }

    /**
     * 取消订阅
     * @param code 证券代码
     * @param interval Bar 的频率
     */
    fun unsubscribeBar(code: String, interval: Int) {
        val barInfo = BarInfo(code, interval)
        when {
            interval <= 60 -> {  // 如果是秒级频率且不存在分钟以上频率的订阅，那么取消订阅
                if (interval < 60 || minuteBarCaches[code] == null) {
                    secondBarGenerators[code]?.removeIf { it.interval == interval }
                    barSubscriptions.remove(barInfo)
                }
            }
            else -> {  // 如果是分钟以上的频率，直接取消订阅，并检查是否取消 60s 频率的订阅
                barSubscriptions.remove(barInfo)
                val hasOtherSub = barSubscriptions.find { it.code == code && it.interval > 60 } != null
                if (!hasOtherSub) {
                    minuteBarCaches.remove(code)
                    if (BarInfo(code, 60) !in userSubscriptions) {
                        unsubscribeBar(code, 60)
                    }
                }
            }
        }
        userSubscriptions.remove(barInfo)
    }

    /**
     * 查询已订阅的 Bar
     * @param isUser 是否查询用户订阅。由于订阅 60 秒以上的 Bar 时会自动订阅 60 秒频率的 Bar，所以用户订阅与实际订阅是不同的。
     */
    fun queryBarSubscriptions(isUser: Boolean): List<BarInfo> {
        return if (isUser) {
            userSubscriptions.toList()
        } else {
            barSubscriptions.toList()
        }
    }

    /**
     * 是否包含 [code] 的订阅
     */
    operator fun contains(code: String): Boolean {
        return userSubscriptions.find { it.code == code } != null
    }

    /**
     * 是否包含 [barInfo] 的订阅
     */
    operator fun contains(barInfo: BarInfo): Boolean {
        return barInfo in userSubscriptions
    }

    /**
     * 推送 Tick 给生成器
     */
    fun updateTick(tick: Tick) {
        secondBarGenerators[tick.code]?.forEach { it.updateTick(tick) }
    }

    /**
     * 重置生成器的状态，清空一切订阅及缓存
     */
    fun reset() {
        barSubscriptions.clear()
        userSubscriptions.clear()
        secondBarGenerators.clear()
        minuteBarCaches.clear()
    }

    /**
     * 释放资源
     */
    fun release() {
        reset()
        scope.cancel()
    }
}
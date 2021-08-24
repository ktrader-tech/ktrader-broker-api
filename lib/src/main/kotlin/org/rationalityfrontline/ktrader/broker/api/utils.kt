package org.rationalityfrontline.ktrader.broker.api

import org.rationalityfrontline.ktrader.datatype.*
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.sign

/**
 * 记录单一合约的双向持仓
 */
data class BiPosition(
    var long: Position? = null,
    var short: Position? = null,
)

/**
 * 记录单一合约的双向持仓明细
 */
data class BiPositionDetails(
    var long: PositionDetails? = null,
    var short: PositionDetails? = null,
)

/**
 * 从 [Position] 集合生成 [BiPosition] Map (key 为 code, value 为 BiPosition)
 */
fun Collection<Position>.toBiPositionsMap(): MutableMap<String, BiPosition> {
    val biMap = mutableMapOf<String, BiPosition>()
    forEach {
        val biPosition = biMap.getOrPut(it.code) { BiPosition() }
        when (it.direction) {
            Direction.LONG -> biPosition.long = it
            Direction.SHORT -> biPosition.short = it
            else -> Unit
        }
    }
    return biMap
}

/**
 * 从 [PositionDetail] 集合生成 [BiPositionDetails] Map (key 为 code, value 为 BiPositionDetails)
 * @param accountId 用于过滤 PositionDetail 的账户 ID
 */
fun Collection<PositionDetail>.toBiPositionDetailsMap(accountId: String? = null): MutableMap<String, BiPositionDetails> {
    val biMap = mutableMapOf<String, BiPositionDetails>()
    if (isEmpty()) return biMap
    val account = accountId ?: first().accountId
    forEach { detail ->
        if (detail.accountId != account) return@forEach
        val biPositionDetails = biMap.getOrPut(detail.code) { BiPositionDetails() }
        var details: PositionDetails? = null
        when (detail.direction) {
            Direction.LONG -> {
                if (biPositionDetails.long == null) {
                    biPositionDetails.long = PositionDetails(detail.accountId, detail.code, detail.direction)
                }
                details = biPositionDetails.long!!
            }
            Direction.SHORT -> {
                if (biPositionDetails.short == null) {
                    biPositionDetails.short = PositionDetails(detail.accountId, detail.code, detail.direction)
                }
                details = biPositionDetails.short!!
            }
            else -> Unit
        }
        if (details != null) {
            val index = details.details.binarySearch { sign(it.price - detail.price).toInt() }
            if (index >= 0) {
                details.details[index].apply {
                    volume += detail.volume
                    todayVolume += detail.todayVolume
                    if (updateTime.isBefore(detail.updateTime)) {
                        updateTime = detail.updateTime
                    }
                }
            } else {
                details.details.add(-index - 1, detail)
            }
        }
    }
    return biMap
}

/**
 * 指定的所平仓位的成本价
 */
var Order.closePositionPrice: Double?
    get() = extras?.get("closePositionPrice")?.toDoubleOrNull()
    set(value) {
        if (extras == null) {
            extras = mutableMapOf()
        }
        extras!!["closePositionPrice"] = value.toString()
    }

/**
 * FAK 订单的最小成交量
 */
var Order.minVolume: Int?
    get() = extras?.get("minVolume")?.toIntOrNull()
    set(value) {
        if (extras == null) {
            extras = mutableMapOf()
        }
        extras!!["minVolume"] = value.toString()
    }
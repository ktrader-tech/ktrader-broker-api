# KTrader-Broker-API 
[![Maven Central](https://img.shields.io/maven-central/v/org.rationalityfrontline.ktrader/ktrader-broker-api.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.rationalityfrontline.ktrader%22%20AND%20a:%22ktrader-broker-api%22)
[![Apache License 2.0](https://img.shields.io/github/license/ktrader-tech/ktrader-broker-api)](https://github.com/ktrader-tech/ktrader-broker-api/blob/master/LICENSE)

[KTrader 量化交易系统](https://github.com/ktrader-tech/ktrader) 的 Broker 统一接口。
> 虽然该项目是为 KTrader 量化交易系统而开发的，但也可以脱离 KTrader 独立使用

## 目标
以统一的方式调用各种交易 API，屏蔽不同 API 的具体调用细节，减少心智负担，提高开发效率。
> 目前主要针对中国国内市场（CTP & XTP）

## 接口速览
带有文档注释的全内容版本参见 [BrokerApi.kt](https://github.com/ktrader-tech/ktrader-broker-api/blob/master/lib/src/main/kotlin/org/rationalityfrontline/ktrader/broker/api/BrokerApi.kt)
及 [BrokerEvent.kt](https://github.com/ktrader-tech/ktrader-broker-api/blob/master/lib/src/main/kotlin/org/rationalityfrontline/ktrader/broker/api/BrokerEvent.kt) 。
```kotlin
abstract class BrokerApi(val config: Map<String, Any>, val kEvent: KEvent) {
    abstract val name: String
    abstract val version: String
    abstract val account: String
    abstract val mdConnected: Boolean
    abstract val tdConnected: Boolean
    val createTime: LocalDateTime = LocalDateTime.now()
    val sourceId: String get() = "${name}_${account}_${hashCode()}"

    abstract suspend fun connect(connectMd: Boolean = true, connectTd: Boolean = true, extras: Map<String, Any>? = null)
    abstract fun close()
    open fun getTradingDay(): LocalDate
    abstract suspend fun subscribeMarketData(codes: Collection<String>, extras: Map<String, Any>? = null)
    open suspend fun subscribeMarketData(code: String, extras: Map<String, Any>? = null)
    abstract suspend fun unsubscribeMarketData(codes: Collection<String>, extras: Map<String, Any>? = null)
    open suspend fun unsubscribeMarketData(code: String, extras: Map<String, Any>? = null)
    abstract suspend fun subscribeAllMarketData(extras: Map<String, Any>? = null)
    abstract suspend fun unsubscribeAllMarketData(extras: Map<String, Any>? = null)
    abstract suspend fun querySubscriptions(useCache: Boolean = true, extras: Map<String, Any>? = null): List<String>
    abstract suspend fun queryLastTick(code: String, useCache: Boolean = true, extras: Map<String, Any>? = null): Tick?
    abstract suspend fun querySecurity(code: String, useCache: Boolean = true, extras: Map<String, Any>? = null): SecurityInfo?
    abstract suspend fun queryAllSecurities(useCache: Boolean = true, extras: Map<String, Any>? = null): List<SecurityInfo>
    abstract suspend fun queryAssets(useCache: Boolean = true, extras: Map<String, Any>? = null): Assets
    abstract suspend fun queryPosition(code: String, direction: Direction, useCache: Boolean = true, extras: Map<String, Any>? = null): Position?
    abstract suspend fun queryPositions(code: String? = null, useCache: Boolean = true, extras: Map<String, Any>? = null): List<Position>
    abstract suspend fun queryOrder(orderId: String, useCache: Boolean = true, extras: Map<String, Any>? = null): Order?
    abstract suspend fun queryOrders(code: String? = null, onlyUnfinished: Boolean = true, useCache: Boolean = true, extras: Map<String, Any>? = null): List<Order>
    abstract suspend fun queryTrade(tradeId: String, useCache: Boolean = true, extras: Map<String, Any>? = null): Trade?
    abstract suspend fun queryTrades(code: String? = null, orderId: String? = null, useCache: Boolean = true, extras: Map<String, Any>? = null): List<Trade>
    abstract suspend fun insertOrder(code: String, price: Double, volume: Int, direction: Direction, offset: OrderOffset, orderType: OrderType = OrderType.LIMIT, extras: Map<String, Any>? = null): Order
    abstract suspend fun cancelOrder(orderId: String, extras: Map<String, Any>? = null)
    open suspend fun cancelAllOrders(extras: Map<String, Any>? = null)
    open suspend fun prepareFeeCalculation(codes: Collection<String>? = null, extras: Map<String, Any>? = null)
    open fun calculatePosition(position: Position, extras: Map<String, Any>? = null)
    open fun calculateOrder(order: Order, extras: Map<String, Any>? = null)
    open fun calculateTrade(trade: Trade, extras: Map<String, Any>? = null)
    open fun customRequest(method: String, params: Map<String, Any>? = null): Any
    open suspend fun customSuspendRequest(method: String, params: Map<String, Any>? = null): Any
}

enum class BrokerEventType {
    CUSTOM_EVENT,
    LOG,
    CONNECTION,
    TICK,
    ORDER_STATUS,
    CANCEL_FAILED,
    TRADE_REPORT,
}
```

## 使用说明
该接口在设计上支持 2 种不同的使用方式：类库，插件。作为类库，直接添加依赖并使用即可。作为插件，会生成一个 ZIP 格式的压缩包插件，然后可以在运行时动态加载或卸载该插件，关于插件化技术请参考 [PF4J](https://github.com/pf4j/pf4j) 。

要实现该接口，需要继承并实现 [BrokerApi](https://github.com/ktrader-tech/ktrader-broker-api/blob/master/lib/src/main/kotlin/org/rationalityfrontline/ktrader/broker/api/BrokerApi.kt) 抽象类，并通过其成员属性 kEvent
将各种推送事件（如 Tick，订单回报，成交回报等）发出。如果需要支持插件的使用方式，那么还需要继承并实现 [Broker](https://github.com/ktrader-tech/ktrader-broker-api/blob/master/lib/src/main/kotlin/org/rationalityfrontline/ktrader/broker/api/Broker.kt) 抽象类。
> [KEvent](https://github.com/RationalityFrontline/kevent) 是一个基于 [Kotlin 协程](https://github.com/Kotlin/kotlinx.coroutines) 实现的强大的事件订阅发布类库。关于可推送的事件类型及事件数据，参见 [BrokerEvent.kt](https://github.com/ktrader-tech/ktrader-broker-api/blob/master/lib/src/main/kotlin/org/rationalityfrontline/ktrader/broker/api/BrokerEvent.kt) 。

## 已有的接口实现
* [KTrader-Broker-CTP](https://github.com/ktrader-tech/ktrader-broker-ctp) CTP 实现（中国期货 & 期权）

## Download

**Gradle:**

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("org.rationalityfrontline.ktrader:ktrader-broker-api:1.1.3")
}
```

**Maven:**

```xml
<dependency>
    <groupId>org.rationalityfrontline.ktrader</groupId>
    <artifactId>ktrader-broker-api</artifactId>
    <version>1.1.3</version>
</dependency>
```

## License

KTrader-Broker-API is released under the [Apache 2.0 license](https://github.com/ktrader-tech/ktrader-broker-api/blob/master/LICENSE).

```
Copyright 2021 RationalityFrontline

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
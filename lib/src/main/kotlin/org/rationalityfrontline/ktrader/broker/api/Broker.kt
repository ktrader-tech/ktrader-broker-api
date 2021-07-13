package org.rationalityfrontline.ktrader.broker.api

import org.pf4j.ExtensionPoint
import org.rationalityfrontline.kevent.KEvent

abstract class Broker : ExtensionPoint {
    abstract val name: String
    abstract val version: String
    abstract val configKeys: List<Pair<String, String>>
    open val methodExtras: List<Pair<String, String>> = listOf()
    open val customMethods: List<Pair<String, String>> = listOf()
    open val customEvents: List<Pair<String, String>> = listOf()
    abstract fun createApi(config: Map<String, Any>, kEvent: KEvent): BrokerApi
    private fun formatPairList(list: List<Pair<String, String>>): String {
        return if (list.isEmpty()) "null" else {
            val indent = "            "
            list.joinToString(separator = "\n", prefix = "{\n", postfix = "\n$indent}") { "$indent    ${it.first}: ${it.second}" }
        }
    }
    override fun toString(): String {
        return """
            Broker@${hashCode()}, name=$name, version=$version
            configKeys:
            ${formatPairList(configKeys)}
            methodExtras:
            ${formatPairList(methodExtras)}
            customMethods:
            ${formatPairList(customMethods)}
            customEvents:
            ${formatPairList(customEvents)}
        """.trimIndent()
    }
}
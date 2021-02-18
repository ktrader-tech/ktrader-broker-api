package org.rationalityfrontline.ktrader.broker.api

import org.pf4j.ExtensionPoint

interface BrokerApi : ExtensionPoint {
    fun getVersion(): String
}
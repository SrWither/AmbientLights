package me.fadel.ambientlights.client.bulb

import me.fadel.ambientlights.client.bulb.wiz.WizBulbClient
import me.fadel.ambientlights.client.bulb.wiz.WizDiscovery

object BulbClientFactory {

    val knownTypes: List<String> = listOf("wiz")

    fun create(type: String, ip: String): BulbClient = when (type.lowercase()) {
        "wiz" -> WizBulbClient(ip)
        else  -> throw IllegalArgumentException("Unknown bulb type: $type")
    }

    fun createDiscovery(type: String): BulbDiscovery? = when (type.lowercase()) {
        "wiz" -> WizDiscovery()
        else  -> null
    }
}

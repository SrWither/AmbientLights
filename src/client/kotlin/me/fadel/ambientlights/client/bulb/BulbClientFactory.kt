package me.fadel.ambientlights.client.bulb

import me.fadel.ambientlights.client.bulb.wiz.WizBulbClient

object BulbClientFactory {

    val knownTypes: List<String> = listOf("wiz")

    fun create(type: String, ip: String): BulbClient = when (type.lowercase()) {
        "wiz" -> WizBulbClient(ip)
        else  -> throw IllegalArgumentException("Unknown bulb type: $type")
    }
}

package me.fadel.ambientlights.client.bulb

import me.fadel.ambientlights.client.color.RGB

interface BulbClient {
    val ip: String
    fun setColor(color: RGB)
}

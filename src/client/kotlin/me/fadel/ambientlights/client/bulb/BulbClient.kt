package me.fadel.ambientlights.client.bulb

import me.fadel.ambientlights.client.color.RGB

interface BulbClient {
    val ip: String
    fun setColor(color: RGB)
    fun readState(): RGB? = null   // optional: read current bulb state before overriding it
}

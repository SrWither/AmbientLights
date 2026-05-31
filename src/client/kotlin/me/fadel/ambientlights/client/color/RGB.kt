package me.fadel.ambientlights.client.color

data class RGB(
    val r: Int,
    val g: Int,
    val b: Int,
    val w: Int = 0,       // warm white (0-255)
    val c: Int = 0,       // cool white (0-255)
    val dimming: Int = 100
)

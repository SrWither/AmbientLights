package me.fadel.ambientlights.client.bulb.wiz

import me.fadel.ambientlights.client.bulb.BulbClient
import me.fadel.ambientlights.client.color.RGB
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class WizBulbClient(override val ip: String) : BulbClient {

    override fun setColor(color: RGB) {
        val r       = color.r.coerceIn(0, 255)
        val g       = color.g.coerceIn(0, 255)
        val b       = color.b.coerceIn(0, 255)
        val w       = color.w.coerceIn(0, 255)
        val c       = color.c.coerceIn(0, 255)
        val dimming = color.dimming.coerceIn(10, 100)

        val payload = """{"method":"setPilot","params":{"state":true,"r":$r,"g":$g,"b":$b,"w":$w,"c":$c,"dimming":$dimming}}"""

        try {
            DatagramSocket().use { socket ->
                val bytes = payload.toByteArray()
                socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName(ip), 38899))
            }
        } catch (e: Exception) {
            System.err.println("WiZ UDP error ($ip): ${e.message}")
        }
    }
}

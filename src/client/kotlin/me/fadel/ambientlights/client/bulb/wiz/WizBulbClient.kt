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

    override fun readState(): RGB? = try {
        DatagramSocket().use { socket ->
            socket.soTimeout = 600
            val payload = """{"method":"getPilot","params":{}}""".toByteArray()
            socket.send(DatagramPacket(payload, payload.size, InetAddress.getByName(ip), 38899))
            val buf = ByteArray(1024)
            val pkt = DatagramPacket(buf, buf.size)
            socket.receive(pkt)
            parseRGB(String(pkt.data, 0, pkt.length))
        }
    } catch (_: Exception) { null }

    private fun parseRGB(json: String): RGB? {
        fun int(name: String) = Regex(""""$name"\s*:\s*(\d+)""").find(json)?.groupValues?.get(1)?.toIntOrNull()

        val dimming = int("dimming") ?: return null

        // Temperature mode: response has "temp" but no r/g/b/w/c fields.
        // Convert Kelvin → approximate warm(w) / cool(c) channel values.
        // WiZ range is 2200 K (full warm) – 6500 K (full cool).
        val temp = int("temp")
        if (temp != null) {
            val span = (6500 - 2200).toFloat()
            val w = ((6500 - temp) / span * 255).toInt().coerceIn(0, 255)
            val c = ((temp - 2200) / span * 255).toInt().coerceIn(0, 255)
            return RGB(0, 0, 0, w = w, c = c, dimming = dimming)
        }

        // RGB / custom mode: response has r, g, b, w, c
        return RGB(
            r       = int("r") ?: 0,
            g       = int("g") ?: 0,
            b       = int("b") ?: 0,
            w       = int("w") ?: 0,
            c       = int("c") ?: 0,
            dimming = dimming
        )
    }
}

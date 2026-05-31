package me.fadel.ambientlights.client.bulb.wiz

import me.fadel.ambientlights.client.bulb.BulbDiscovery
import me.fadel.ambientlights.client.bulb.BulbDiscovery.DiscoveredBulb
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class WizDiscovery : BulbDiscovery {

    private companion object {
        const val PORT       = 38899
        const val SCAN_MS    = 3000L
        const val NAME_MS    = 600
        // Standard WiZ discovery broadcast — phoneMac is an arbitrary client identifier
        val PING = """{"method":"registration","params":{"phoneMac":"AABBCCDDEEFF","register":false,"phoneIp":"0.0.0.0","id":1}}""".toByteArray()
    }

    override fun discover(existingIps: Set<String>, onFound: (DiscoveredBulb) -> Unit, onDone: (Int) -> Unit) {
        val collectedIps = mutableSetOf<String>()

        try {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.send(DatagramPacket(PING, PING.size, InetAddress.getByName("255.255.255.255"), PORT))

                val buf      = ByteArray(2048)
                val deadline = System.currentTimeMillis() + SCAN_MS

                while (System.currentTimeMillis() < deadline) {
                    socket.soTimeout = maxOf(100, deadline - System.currentTimeMillis()).toInt()
                    try {
                        val pkt = DatagramPacket(buf, buf.size)
                        socket.receive(pkt)
                        val ip = pkt.address.hostAddress ?: continue
                        if (ip !in collectedIps && ip !in existingIps) collectedIps.add(ip)
                    } catch (_: SocketTimeoutException) { break }
                }
            }
        } catch (e: Exception) {
            System.err.println("WiZ discovery error: ${e.message}")
        }

        // Query each found bulb for its name, then report
        for (ip in collectedIps) {
            val name = resolveName(ip)
            onFound(DiscoveredBulb(ip, name))
        }

        onDone(collectedIps.size)
    }

    private fun resolveName(ip: String): String {
        // Some WiZ firmwares expose user-set name via getDevInfo
        queryJson(ip, """{"method":"getDevInfo","params":{}}""")
            ?.let { parseField(it, "name") }
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        // Fall back to hardware model name from getSystemConfig
        queryJson(ip, """{"method":"getSystemConfig","params":{}}""")
            ?.let { parseField(it, "moduleName") }
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        return ip
    }

    private fun queryJson(ip: String, payload: String): String? = try {
        DatagramSocket().use { socket ->
            socket.soTimeout = NAME_MS
            val bytes = payload.toByteArray()
            socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName(ip), PORT))
            val buf = ByteArray(1024)
            val pkt = DatagramPacket(buf, buf.size)
            socket.receive(pkt)
            String(pkt.data, 0, pkt.length)
        }
    } catch (_: Exception) { null }

    private fun parseField(json: String, field: String): String? =
        Regex(""""$field"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1)
}

package me.fadel.ambientlights.client.bulb

interface BulbDiscovery {

    data class DiscoveredBulb(val ip: String, val name: String)

    /**
     * Scans the network and reports bulbs as they are found.
     * [existingIps] are skipped (already configured).
     * Callbacks are invoked from the calling thread — caller is responsible for dispatching to the main thread.
     */
    fun discover(
        existingIps: Set<String>,
        onFound: (DiscoveredBulb) -> Unit,
        onDone: (totalFound: Int) -> Unit
    )
}

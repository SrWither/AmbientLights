package me.fadel.ambientlights.client.lighting

import me.fadel.ambientlights.client.AmbientLightsClient
import me.fadel.ambientlights.client.color.RGB
import me.fadel.ambientlights.client.wiz.WizClient
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome

object AmbientController {

    private const val UPDATE_INTERVAL  = 5000L
    private const val BIOME_DEBOUNCE   = 1500L

    private val wiz1 = WizClient("192.168.60.58")
    private val wiz2 = WizClient("192.168.60.62")

    private var lastUpdate         = 0L
    private var lastDimension: ResourceKey<Level>? = null
    private var lastBiome: ResourceKey<Biome>?     = null
    private var biomeChangeTime    = 0L
    private var pendingBiomeUpdate = false

    private var lastSentPrimary:   RGB? = null
    private var lastSentSecondary: RGB? = null

    private enum class AppState { UNINITIALIZED, MENU, IN_WORLD }
    private var currentState = AppState.UNINITIALIZED

    fun tick(client: Minecraft) {

        val level = client.level

        if (level == null) {
            if (currentState != AppState.MENU) {
                AmbientLightsClient.logger.info("Entering menu, setting lights to white")
                currentState       = AppState.MENU
                lastDimension      = null
                lastBiome          = null
                pendingBiomeUpdate = false
                lastSentPrimary    = null
                lastSentSecondary  = null
                wiz1.setColor(RGB(0, 0, 0, w = 150, c = 150, dimming = 100))
                wiz2.setColor(RGB(0, 0, 0, w = 200, c = 80,  dimming = 95))
            }
            return
        }

        if (currentState != AppState.IN_WORLD) {
            AmbientLightsClient.logger.info("Entering world, starting ambient lights")
            currentState = AppState.IN_WORLD
            lastUpdate   = 0L
        }

        val now = System.currentTimeMillis()

        // Cambio de dimensión: instantáneo
        val currentDimension = level.dimension()
        if (currentDimension != lastDimension) {
            AmbientLightsClient.logger.info("Dimension changed to ${currentDimension.location()}, updating instantly")
            lastDimension      = currentDimension
            lastBiome          = null
            pendingBiomeUpdate = false
            lastSentPrimary    = null
            lastSentSecondary  = null
            lastUpdate         = 0L
        }

        // Cambio de bioma: debounce para no disparar en cada borde
        val currentBiome = client.player?.let { level.getBiome(it.blockPosition()).unwrapKey().orElse(null) }
        if (currentBiome != lastBiome) {
            lastBiome          = currentBiome
            biomeChangeTime    = now
            pendingBiomeUpdate = true
        }
        if (pendingBiomeUpdate && now - biomeChangeTime >= BIOME_DEBOUNCE) {
            AmbientLightsClient.logger.info("Biome settled at ${currentBiome?.location()}, updating lights")
            pendingBiomeUpdate = false
            lastUpdate         = 0L
        }

        if (now - lastUpdate < UPDATE_INTERVAL) return
        lastUpdate = now

        val primary   = EnviromentColorProvider.primaryColor(client)
        val secondary = EnviromentColorProvider.secondaryColor(client)

        if (primary != lastSentPrimary) {
            wiz1.setColor(primary)
            lastSentPrimary = primary
        }
        if (secondary != lastSentSecondary) {
            wiz2.setColor(secondary)
            lastSentSecondary = secondary
        }
    }
}

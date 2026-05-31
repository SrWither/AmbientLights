package me.fadel.ambientlights.client.lighting

import me.fadel.ambientlights.client.AmbientLightsClient
import me.fadel.ambientlights.client.color.RGB
import me.fadel.ambientlights.client.wiz.WizClient
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome

object AmbientController {

    private const val UPDATE_INTERVAL  = 5000L
    private const val BIOME_COOLDOWN   = 2000L

    private val wiz1 = WizClient("192.168.60.58")
    private val wiz2 = WizClient("192.168.60.62")

    private var lastUpdate         = 0L
    private var lastDimension: ResourceKey<Level>? = null
    private var lastBiome: ResourceKey<Biome>?     = null
    private var lastBiomeUpdate    = 0L
    private var pendingBiomeUpdate = false

    private var lastInWater       = false
    private var lastInLava        = false

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
                lastInWater        = false
                lastInLava         = false
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
            AmbientLightsClient.logger.info("Dimension changed to ${currentDimension}, updating instantly")
            lastDimension      = currentDimension
            lastBiome          = null
            pendingBiomeUpdate = false
            lastSentPrimary    = null
            lastSentSecondary  = null
            lastUpdate         = 0L
        }

        // Cambio de fluido en ojos: instantáneo y mayor prioridad que bioma/dimensión
        val player = client.player
        if (player != null) {
            val eyeFluid = level.getFluidState(BlockPos.containing(player.getEyePosition()))
            val inWater  = eyeFluid.`is`(FluidTags.WATER)
            val inLava   = eyeFluid.`is`(FluidTags.LAVA)
            if (inWater != lastInWater || inLava != lastInLava) {
                AmbientLightsClient.logger.info("Fluid state changed (water=$inWater lava=$inLava), updating instantly")
                lastInWater       = inWater
                lastInLava        = inLava
                lastSentPrimary   = null
                lastSentSecondary = null
                lastUpdate        = 0L
            }
        }

        // Cambio de bioma: instantáneo, luego cooldown antes del siguiente
        val currentBiome = client.player?.let { level.getBiome(it.blockPosition()).unwrapKey().orElse(null) }
        if (currentBiome != lastBiome) {
            lastBiome = currentBiome
            if (now - lastBiomeUpdate >= BIOME_COOLDOWN) {
                AmbientLightsClient.logger.info("Biome changed to ${currentBiome}, updating instantly")
                lastBiomeUpdate    = now
                pendingBiomeUpdate = false
                lastUpdate         = 0L
            } else {
                pendingBiomeUpdate = true
            }
        }
        if (pendingBiomeUpdate && now - lastBiomeUpdate >= BIOME_COOLDOWN) {
            AmbientLightsClient.logger.info("Biome settled at ${currentBiome}, updating after cooldown")
            pendingBiomeUpdate = false
            lastBiomeUpdate    = now
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

package me.fadel.ambientlights.client.lighting

import me.fadel.ambientlights.client.AmbientLightsClient
import me.fadel.ambientlights.client.color.RGB
import me.fadel.ambientlights.client.config.LightConfig
import me.fadel.ambientlights.client.wiz.WizClient
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome

object AmbientController {

    private const val UPDATE_INTERVAL = 5000L
    private const val BIOME_COOLDOWN  = 2000L

    private data class ManagedLight(val wiz: WizClient, var lastSent: RGB? = null)

    private val lights = mutableListOf<ManagedLight>()

    private var lastUpdate         = 0L
    private var lastDimension: ResourceKey<Level>? = null
    private var lastBiome: ResourceKey<Biome>?     = null
    private var lastBiomeUpdate    = 0L
    private var pendingBiomeUpdate = false
    private var lastInWater        = false
    private var lastInLava         = false

    private enum class AppState { UNINITIALIZED, MENU, IN_WORLD }
    private var currentState = AppState.UNINITIALIZED

    // --- Gestión de focos ---

    fun loadFromConfig() {
        lights.clear()
        LightConfig.load().forEach { ip -> lights.add(ManagedLight(WizClient(ip))) }
        AmbientLightsClient.logger.info("Loaded ${lights.size} light(s) from config")
    }

    fun addLight(ip: String): Boolean {
        if (lights.any { it.wiz.ip == ip }) return false
        lights.add(ManagedLight(WizClient(ip)))
        LightConfig.save(lights.map { it.wiz.ip })
        lastUpdate = 0L  // trigger inmediato para que el nuevo foco reciba color
        AmbientLightsClient.logger.info("Added light: $ip (total: ${lights.size})")
        return true
    }

    fun removeLight(ip: String): Boolean {
        val removed = lights.removeIf { it.wiz.ip == ip }
        if (removed) {
            LightConfig.save(lights.map { it.wiz.ip })
            AmbientLightsClient.logger.info("Removed light: $ip (remaining: ${lights.size})")
        }
        return removed
    }

    fun clearLights() {
        lights.clear()
        LightConfig.save(emptyList())
        AmbientLightsClient.logger.info("Cleared all lights")
    }

    fun listLights(): List<String> = lights.map { it.wiz.ip }

    private fun resetAllLastSent() {
        lights.forEach { it.lastSent = null }
    }

    // --- Tick principal ---

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
                resetAllLastSent()
                val menuPrimary   = RGB(0, 0, 0, w = 150, c = 150, dimming = 100)
                val menuSecondary = RGB(0, 0, 0, w = 200, c = 80,  dimming = 95)
                lights.forEachIndexed { i, light ->
                    light.wiz.setColor(if (i % 2 == 0) menuPrimary else menuSecondary)
                }
            }
            return
        }

        if (currentState != AppState.IN_WORLD) {
            AmbientLightsClient.logger.info("Entering world, starting ambient lights")
            currentState = AppState.IN_WORLD
            lastUpdate   = 0L
        }

        val now = System.currentTimeMillis()

        // Leer todo el estado antes de procesar cambios
        val currentDimension = level.dimension()
        val currentBiome     = client.player?.let { p -> level.getBiome(p.blockPosition()).unwrapKey().orElse(null) }
        val eyeFluid         = client.player?.let { p -> level.getFluidState(BlockPos.containing(p.getEyePosition())) }
        val inWater          = eyeFluid?.`is`(FluidTags.WATER) ?: false
        val inLava           = eyeFluid?.`is`(FluidTags.LAVA)  ?: false

        // Cambio de dimensión
        if (currentDimension != lastDimension) {
            AmbientLightsClient.logger.info("Dimension changed to ${currentDimension}, updating instantly")
            lastDimension      = currentDimension
            lastBiome          = currentBiome
            lastBiomeUpdate    = now
            pendingBiomeUpdate = false
            lastInWater        = inWater
            lastInLava         = inLava
            resetAllLastSent()
            lastUpdate         = 0L
        }

        // Cambio de fluido: instantáneo
        if (inWater != lastInWater || inLava != lastInLava) {
            AmbientLightsClient.logger.info("Fluid state changed (water=$inWater lava=$inLava), updating instantly")
            lastInWater = inWater
            lastInLava  = inLava
            resetAllLastSent()
            lastUpdate  = 0L
        }

        // Cambio de bioma: instantáneo, luego cooldown
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

        if (lights.isEmpty()) return

        val primary   = EnviromentColorProvider.primaryColor(client)
        val secondary = EnviromentColorProvider.secondaryColor(client)

        lights.forEachIndexed { i, light ->
            val color = if (i % 2 == 0) primary else secondary
            if (color != light.lastSent) {
                light.wiz.setColor(color)
                light.lastSent = color
            }
        }
    }
}

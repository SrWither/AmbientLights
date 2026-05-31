package me.fadel.ambientlights.client.lighting

import me.fadel.ambientlights.client.AmbientLightsClient
import me.fadel.ambientlights.client.bulb.BulbClient
import me.fadel.ambientlights.client.bulb.BulbClientFactory
import me.fadel.ambientlights.client.color.RGB
import me.fadel.ambientlights.client.config.LightConfig
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome

object AmbientController {

    enum class LightRole { PRIMARY, SECONDARY }

    data class LightInfo(val alias: String, val ip: String, val role: LightRole, val type: String)

    private data class ManagedLight(
        val alias: String,
        val client: BulbClient,
        val role: LightRole,
        val type: String,
        var lastSent: RGB? = null
    )

    private const val UPDATE_INTERVAL = 5000L
    private const val BIOME_COOLDOWN  = 2000L

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

    // ── Light management ──────────────────────────────────────────────────────

    fun loadFromConfig() {
        lights.clear()
        LightConfig.load().forEach { entry ->
            val role   = if (entry.role.equals("secondary", ignoreCase = true)) LightRole.SECONDARY else LightRole.PRIMARY
            val type   = entry.type ?: "wiz"
            lights.add(ManagedLight(entry.alias, BulbClientFactory.create(type, entry.ip), role, type))
        }
        AmbientLightsClient.logger.info("Loaded ${lights.size} light(s) from config")
    }

    private fun saveConfig() {
        LightConfig.save(lights.map { LightConfig.LightEntry(it.alias, it.client.ip, it.role.name.lowercase(), it.type) })
    }

    fun addLight(alias: String, ip: String, role: LightRole, type: String = "wiz"): Boolean {
        if (lights.any { it.alias == alias }) return false
        lights.add(ManagedLight(alias, BulbClientFactory.create(type, ip), role, type))
        saveConfig()
        lastUpdate = 0L
        AmbientLightsClient.logger.info("Added light '$alias' @ $ip ($type, ${role.name.lowercase()})")
        return true
    }

    fun removeLight(alias: String): Boolean {
        val removed = lights.removeIf { it.alias == alias }
        if (removed) {
            saveConfig()
            AmbientLightsClient.logger.info("Removed light '$alias'")
        }
        return removed
    }

    fun editIp(alias: String, newIp: String): Boolean {
        val index = lights.indexOfFirst { it.alias == alias }
        if (index == -1) return false
        val old = lights[index]
        lights[index] = old.copy(client = BulbClientFactory.create(old.type, newIp), lastSent = null)
        saveConfig()
        lastUpdate = 0L
        AmbientLightsClient.logger.info("Updated '$alias' IP → $newIp")
        return true
    }

    fun editRole(alias: String, role: LightRole): Boolean {
        val index = lights.indexOfFirst { it.alias == alias }
        if (index == -1) return false
        lights[index] = lights[index].copy(role = role, lastSent = null)
        saveConfig()
        lastUpdate = 0L
        AmbientLightsClient.logger.info("Updated '$alias' role → ${role.name.lowercase()}")
        return true
    }

    fun editType(alias: String, newType: String): Boolean {
        val index = lights.indexOfFirst { it.alias == alias }
        if (index == -1) return false
        val old = lights[index]
        lights[index] = old.copy(client = BulbClientFactory.create(newType, old.client.ip), type = newType, lastSent = null)
        saveConfig()
        lastUpdate = 0L
        AmbientLightsClient.logger.info("Updated '$alias' type → $newType")
        return true
    }

    fun clearLights() {
        lights.clear()
        saveConfig()
        AmbientLightsClient.logger.info("Cleared all lights")
    }

    fun listLights(): List<LightInfo> = lights.map { LightInfo(it.alias, it.client.ip, it.role, it.type) }
    fun listAliases(): List<String>   = lights.map { it.alias }

    private fun resetAllLastSent() = lights.forEach { it.lastSent = null }

    // ── Main tick ─────────────────────────────────────────────────────────────

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
                lights.forEach { light ->
                    light.client.setColor(if (light.role == LightRole.PRIMARY) menuPrimary else menuSecondary)
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

        val currentDimension = level.dimension()
        val currentBiome     = client.player?.let { p -> level.getBiome(p.blockPosition()).unwrapKey().orElse(null) }
        val eyeFluid         = client.player?.let { p -> level.getFluidState(BlockPos.containing(p.eyePosition)) }
        val inWater          = eyeFluid?.`is`(FluidTags.WATER) ?: false
        val inLava           = eyeFluid?.`is`(FluidTags.LAVA)  ?: false

        if (currentDimension != lastDimension) {
            AmbientLightsClient.logger.info("Dimension changed to $currentDimension, updating instantly")
            lastDimension      = currentDimension
            lastBiome          = currentBiome
            lastBiomeUpdate    = now
            pendingBiomeUpdate = false
            lastInWater        = inWater
            lastInLava         = inLava
            resetAllLastSent()
            lastUpdate         = 0L
        }

        if (inWater != lastInWater || inLava != lastInLava) {
            AmbientLightsClient.logger.info("Fluid state changed (water=$inWater lava=$inLava), updating instantly")
            lastInWater = inWater
            lastInLava  = inLava
            resetAllLastSent()
            lastUpdate  = 0L
        }

        if (currentBiome != lastBiome) {
            lastBiome = currentBiome
            if (now - lastBiomeUpdate >= BIOME_COOLDOWN) {
                AmbientLightsClient.logger.info("Biome changed to $currentBiome, updating instantly")
                lastBiomeUpdate    = now
                pendingBiomeUpdate = false
                lastUpdate         = 0L
            } else {
                pendingBiomeUpdate = true
            }
        }
        if (pendingBiomeUpdate && now - lastBiomeUpdate >= BIOME_COOLDOWN) {
            AmbientLightsClient.logger.info("Biome settled at $currentBiome, updating after cooldown")
            pendingBiomeUpdate = false
            lastBiomeUpdate    = now
            lastUpdate         = 0L
        }

        if (now - lastUpdate < UPDATE_INTERVAL) return
        lastUpdate = now

        if (lights.isEmpty()) return

        val primary   = EnvironmentColorProvider.primaryColor(client)
        val secondary = EnvironmentColorProvider.secondaryColor(client)

        lights.forEach { light ->
            val color = if (light.role == LightRole.PRIMARY) primary else secondary
            if (color != light.lastSent) {
                light.client.setColor(color)
                light.lastSent = color
            }
        }
    }
}

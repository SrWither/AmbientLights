package me.fadel.ambientlights.client.lighting

import me.fadel.ambientlights.client.AmbientLightsClient
import me.fadel.ambientlights.client.color.RGB
import me.fadel.ambientlights.client.wiz.WizClient
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level

object AmbientController {

    private const val UPDATE_INTERVAL = 5000L

    private val wiz1 = WizClient("192.168.60.58")
    private val wiz2 = WizClient("192.168.60.62")

    private var lastUpdate = 0L
    private var lastDimension: ResourceKey<Level>? = null

    private enum class AppState {
        UNINITIALIZED, MENU, IN_WORLD
    }

    private var currentState = AppState.UNINITIALIZED

    fun tick(client: Minecraft) {

        val level = client.level

        if (level == null) {
            if (currentState != AppState.MENU) {
                AmbientLightsClient.logger.info("Entering menu, setting lights to white")
                currentState = AppState.MENU
                lastDimension = null
                wiz1.setColor(RGB(0, 0, 0, w = 150, c = 150, dimming = 100))
                wiz2.setColor(RGB(0, 0, 0, w = 200, c = 80, dimming = 95))
            }
            return
        }

        // --- A partir de esta línea, sabemos que ESTAMOS DENTRO DEL JUEGO ---

        if (currentState != AppState.IN_WORLD) {
            AmbientLightsClient.logger.info("Entering world, starting ambient lights")
            currentState = AppState.IN_WORLD
            lastUpdate = 0L
        }

        // Cambio de dimensión → actualizamos instantáneamente sin esperar el timer
        val currentDimension = level.dimension()
        if (currentDimension != lastDimension) {
            AmbientLightsClient.logger.info("Dimension changed to $currentDimension, updating lights instantly")
            lastDimension = currentDimension
            lastUpdate = 0L
        }

        val now = System.currentTimeMillis()
        if (now - lastUpdate < UPDATE_INTERVAL) return
        lastUpdate = now

        wiz1.setColor(EnviromentColorProvider.primaryColor(client))
        wiz2.setColor(EnviromentColorProvider.secondaryColor(client))
    }
}
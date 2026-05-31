package me.fadel.ambientlights.client

import me.fadel.ambientlights.client.command.AmbientLightsCommand
import me.fadel.ambientlights.client.gui.LightConfigScreen
import me.fadel.ambientlights.client.lighting.AmbientController
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.options.OptionsScreen
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory

object AmbientLightsClient : ClientModInitializer {

    val logger = LoggerFactory.getLogger("ambientlights-client")

    override fun onInitializeClient() {
        logger.info("Initializing AmbientLights client")

        AmbientController.loadFromConfig()
        AmbientLightsCommand.register()

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            AmbientController.tick(client)
        }

        // Botón en TitleScreen y OptionsScreen para abrir la config GUI
        ScreenEvents.AFTER_INIT.register { client, screen, _, _ ->
            if (screen is TitleScreen || screen is OptionsScreen) {
                net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(screen).add(
                    Button.builder(Component.literal("AmbientLights")) {
                        client.setScreen(LightConfigScreen(screen))
                    }.bounds(4, 4, 120, 20).build()
                )
            }
        }
    }
}

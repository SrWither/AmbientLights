package me.fadel.ambientlights.client

import me.fadel.ambientlights.client.lighting.AmbientController
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import org.slf4j.LoggerFactory

object AmbientLightsClient : ClientModInitializer {
	
	val logger = LoggerFactory.getLogger("ambientlights-client")

    override fun onInitializeClient() {

		logger.info("Initializing AmbientLights client")

        ClientTickEvents.END_CLIENT_TICK.register { client ->

            AmbientController.tick(client)
        }
    }
}
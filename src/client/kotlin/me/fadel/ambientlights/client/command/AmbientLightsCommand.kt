package me.fadel.ambientlights.client.command

import com.mojang.brigadier.arguments.StringArgumentType
import me.fadel.ambientlights.client.lighting.AmbientController
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.minecraft.network.chat.Component

object AmbientLightsCommand {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("ambientlights")
                    .then(ClientCommands.literal("add")
                        .then(ClientCommands.argument("ip", StringArgumentType.string())
                            .executes { ctx ->
                                val ip = StringArgumentType.getString(ctx, "ip")
                                if (AmbientController.addLight(ip)) {
                                    ctx.source.sendFeedback(Component.literal("§aAdded light: $ip"))
                                } else {
                                    ctx.source.sendFeedback(Component.literal("§eLight already exists: $ip"))
                                }
                                1
                            }))
                    .then(ClientCommands.literal("remove")
                        .then(ClientCommands.argument("ip", StringArgumentType.string())
                            .executes { ctx ->
                                val ip = StringArgumentType.getString(ctx, "ip")
                                if (AmbientController.removeLight(ip)) {
                                    ctx.source.sendFeedback(Component.literal("§cRemoved light: $ip"))
                                } else {
                                    ctx.source.sendFeedback(Component.literal("§eLight not found: $ip"))
                                }
                                1
                            }))
                    .then(ClientCommands.literal("list")
                        .executes { ctx ->
                            val lights = AmbientController.listLights()
                            if (lights.isEmpty()) {
                                ctx.source.sendFeedback(Component.literal("§7No lights configured. Use /ambientlights add <ip>"))
                            } else {
                                ctx.source.sendFeedback(Component.literal("§6Lights (${lights.size}):"))
                                lights.forEachIndexed { i, ip ->
                                    val role = if (i % 2 == 0) "primary" else "secondary"
                                    ctx.source.sendFeedback(Component.literal("§7  [$i] $ip §8($role)"))
                                }
                            }
                            1
                        })
                    .then(ClientCommands.literal("clear")
                        .executes { ctx ->
                            val count = AmbientController.listLights().size
                            AmbientController.clearLights()
                            ctx.source.sendFeedback(Component.literal("§cCleared $count light(s)"))
                            1
                        })
            )
        }
    }
}

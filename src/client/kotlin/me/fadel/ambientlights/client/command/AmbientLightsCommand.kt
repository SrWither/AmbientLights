package me.fadel.ambientlights.client.command

import com.mojang.brigadier.arguments.StringArgumentType
import me.fadel.ambientlights.client.lighting.AmbientController
import me.fadel.ambientlights.client.lighting.AmbientController.LightRole
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.minecraft.network.chat.Component

object AmbientLightsCommand {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("ambientlights")

                    // add <alias> <ip> [primary|secondary]
                    .then(ClientCommands.literal("add")
                        .then(ClientCommands.argument("alias", StringArgumentType.word())
                            .then(ClientCommands.argument("ip", StringArgumentType.string())
                                // sin role → primary por defecto
                                .executes { ctx ->
                                    addLight(ctx.source, ctx, LightRole.PRIMARY)
                                }
                                // con role explícito
                                .then(ClientCommands.argument("role", StringArgumentType.word())
                                    .suggests { _, builder ->
                                        builder.suggest("primary")
                                        builder.suggest("secondary")
                                        builder.buildFuture()
                                    }
                                    .executes { ctx ->
                                        val roleStr = StringArgumentType.getString(ctx, "role")
                                        val role = parseRole(roleStr)
                                            ?: return@executes run {
                                                ctx.source.sendFeedback(Component.literal("§cRole must be 'primary' or 'secondary'"))
                                                0
                                            }
                                        addLight(ctx.source, ctx, role)
                                    }))))

                    // remove <alias>
                    .then(ClientCommands.literal("remove")
                        .then(ClientCommands.argument("alias", StringArgumentType.word())
                            .suggests { _, builder ->
                                AmbientController.listAliases().forEach { builder.suggest(it) }
                                builder.buildFuture()
                            }
                            .executes { ctx ->
                                val alias = StringArgumentType.getString(ctx, "alias")
                                if (AmbientController.removeLight(alias)) {
                                    ctx.source.sendFeedback(Component.literal("§cRemoved: $alias"))
                                } else {
                                    ctx.source.sendFeedback(Component.literal("§eNot found: $alias"))
                                }
                                1
                            }))

                    // edit <alias> ip <newip>
                    // edit <alias> role <primary|secondary>
                    .then(ClientCommands.literal("edit")
                        .then(ClientCommands.argument("alias", StringArgumentType.word())
                            .suggests { _, builder ->
                                AmbientController.listAliases().forEach { builder.suggest(it) }
                                builder.buildFuture()
                            }
                            .then(ClientCommands.literal("ip")
                                .then(ClientCommands.argument("newip", StringArgumentType.string())
                                    .executes { ctx ->
                                        val alias = StringArgumentType.getString(ctx, "alias")
                                        val newIp = StringArgumentType.getString(ctx, "newip")
                                        if (AmbientController.editIp(alias, newIp)) {
                                            ctx.source.sendFeedback(Component.literal("§aUpdated '$alias' → $newIp"))
                                        } else {
                                            ctx.source.sendFeedback(Component.literal("§eNot found: $alias"))
                                        }
                                        1
                                    }))
                            .then(ClientCommands.literal("role")
                                .then(ClientCommands.argument("role", StringArgumentType.word())
                                    .suggests { _, builder ->
                                        builder.suggest("primary")
                                        builder.suggest("secondary")
                                        builder.buildFuture()
                                    }
                                    .executes { ctx ->
                                        val alias = StringArgumentType.getString(ctx, "alias")
                                        val role  = parseRole(StringArgumentType.getString(ctx, "role"))
                                            ?: return@executes run {
                                                ctx.source.sendFeedback(Component.literal("§cRole must be 'primary' or 'secondary'"))
                                                0
                                            }
                                        if (AmbientController.editRole(alias, role)) {
                                            ctx.source.sendFeedback(Component.literal("§aUpdated '$alias' role → ${role.name.lowercase()}"))
                                        } else {
                                            ctx.source.sendFeedback(Component.literal("§eNot found: $alias"))
                                        }
                                        1
                                    }))))

                    // list
                    .then(ClientCommands.literal("list")
                        .executes { ctx ->
                            val lights = AmbientController.listLights()
                            if (lights.isEmpty()) {
                                ctx.source.sendFeedback(Component.literal("§7No lights. Use /ambientlights add <alias> <ip>"))
                            } else {
                                ctx.source.sendFeedback(Component.literal("§6Lights (${lights.size}):"))
                                lights.forEach { info ->
                                    val roleColor = if (info.role == LightRole.PRIMARY) "§e" else "§b"
                                    ctx.source.sendFeedback(
                                        Component.literal("§7  ${info.alias} §8| §f${info.ip} §8| $roleColor${info.role.name.lowercase()}")
                                    )
                                }
                            }
                            1
                        })

                    // clear
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

    private fun parseRole(str: String): LightRole? = when (str.lowercase()) {
        "primary"   -> LightRole.PRIMARY
        "secondary" -> LightRole.SECONDARY
        else        -> null
    }

    private fun addLight(
        source: net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource,
        ctx: com.mojang.brigadier.context.CommandContext<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>,
        role: LightRole
    ): Int {
        val alias = StringArgumentType.getString(ctx, "alias")
        val ip    = StringArgumentType.getString(ctx, "ip")
        if (AmbientController.addLight(alias, ip, role)) {
            source.sendFeedback(Component.literal("§aAdded '$alias' @ $ip (${role.name.lowercase()})"))
        } else {
            source.sendFeedback(Component.literal("§eAlias already exists: $alias"))
        }
        return 1
    }
}

package me.fadel.ambientlights.client.command

import com.mojang.brigadier.arguments.StringArgumentType
import me.fadel.ambientlights.client.bulb.BulbClientFactory
import me.fadel.ambientlights.client.lighting.AmbientController
import me.fadel.ambientlights.client.lighting.AmbientController.LightRole
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component

object AmbientLightsCommand {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("ambientlights")

                    // add <alias> <ip> [role] [type]
                    .then(ClientCommands.literal("add")
                        .then(ClientCommands.argument("alias", StringArgumentType.word())
                            .then(ClientCommands.argument("ip", StringArgumentType.string())
                                .executes { ctx ->
                                    doAdd(ctx.source, ctx, LightRole.PRIMARY, "wiz")
                                }
                                .then(ClientCommands.argument("role", StringArgumentType.word())
                                    .suggests { _, b -> rolesSuggestions(b) }
                                    .executes { ctx ->
                                        val role = parseRole(StringArgumentType.getString(ctx, "role"))
                                            ?: return@executes sendError(ctx.source, "Role must be 'primary' or 'secondary'")
                                        doAdd(ctx.source, ctx, role, "wiz")
                                    }
                                    .then(ClientCommands.argument("type", StringArgumentType.word())
                                        .suggests { _, b -> typesSuggestions(b) }
                                        .executes { ctx ->
                                            val role = parseRole(StringArgumentType.getString(ctx, "role"))
                                                ?: return@executes sendError(ctx.source, "Role must be 'primary' or 'secondary'")
                                            val type = StringArgumentType.getString(ctx, "type")
                                            doAdd(ctx.source, ctx, role, type)
                                        })))))

                    // remove <alias>
                    .then(ClientCommands.literal("remove")
                        .then(ClientCommands.argument("alias", StringArgumentType.word())
                            .suggests { _, b -> aliasesSuggestions(b) }
                            .executes { ctx ->
                                val alias = StringArgumentType.getString(ctx, "alias")
                                if (AmbientController.removeLight(alias))
                                    ctx.source.sendFeedback(Component.literal("§cRemoved: $alias"))
                                else
                                    ctx.source.sendFeedback(Component.literal("§eNot found: $alias"))
                                1
                            }))

                    // edit <alias> ip <newip>
                    // edit <alias> role <role>
                    // edit <alias> type <type>
                    .then(ClientCommands.literal("edit")
                        .then(ClientCommands.argument("alias", StringArgumentType.word())
                            .suggests { _, b -> aliasesSuggestions(b) }
                            .then(ClientCommands.literal("ip")
                                .then(ClientCommands.argument("newip", StringArgumentType.string())
                                    .executes { ctx ->
                                        val alias = StringArgumentType.getString(ctx, "alias")
                                        val newIp = StringArgumentType.getString(ctx, "newip")
                                        if (AmbientController.editIp(alias, newIp))
                                            ctx.source.sendFeedback(Component.literal("§aUpdated '$alias' IP → $newIp"))
                                        else
                                            ctx.source.sendFeedback(Component.literal("§eNot found: $alias"))
                                        1
                                    }))
                            .then(ClientCommands.literal("role")
                                .then(ClientCommands.argument("role", StringArgumentType.word())
                                    .suggests { _, b -> rolesSuggestions(b) }
                                    .executes { ctx ->
                                        val alias = StringArgumentType.getString(ctx, "alias")
                                        val role  = parseRole(StringArgumentType.getString(ctx, "role"))
                                            ?: return@executes sendError(ctx.source, "Role must be 'primary' or 'secondary'")
                                        if (AmbientController.editRole(alias, role))
                                            ctx.source.sendFeedback(Component.literal("§aUpdated '$alias' role → ${role.name.lowercase()}"))
                                        else
                                            ctx.source.sendFeedback(Component.literal("§eNot found: $alias"))
                                        1
                                    }))
                            .then(ClientCommands.literal("type")
                                .then(ClientCommands.argument("type", StringArgumentType.word())
                                    .suggests { _, b -> typesSuggestions(b) }
                                    .executes { ctx ->
                                        val alias   = StringArgumentType.getString(ctx, "alias")
                                        val newType = StringArgumentType.getString(ctx, "type")
                                        if (AmbientController.editType(alias, newType))
                                            ctx.source.sendFeedback(Component.literal("§aUpdated '$alias' type → $newType"))
                                        else
                                            ctx.source.sendFeedback(Component.literal("§eNot found: $alias"))
                                        1
                                    }))))

                    // list
                    .then(ClientCommands.literal("list")
                        .executes { ctx ->
                            val lights = AmbientController.listLights()
                            if (lights.isEmpty()) {
                                ctx.source.sendFeedback(Component.literal("§7No lights configured. Use /ambientlights add <alias> <ip>"))
                            } else {
                                ctx.source.sendFeedback(Component.literal("§6Lights (${lights.size}):"))
                                lights.forEach { info ->
                                    val roleColor = if (info.role == LightRole.PRIMARY) "§e" else "§b"
                                    ctx.source.sendFeedback(
                                        Component.literal("§7  ${info.alias} §8| §f${info.ip} §8| §d${info.type} §8| $roleColor${info.role.name.lowercase()}")
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

    private fun doAdd(
        source: FabricClientCommandSource,
        ctx: com.mojang.brigadier.context.CommandContext<FabricClientCommandSource>,
        role: LightRole,
        type: String
    ): Int {
        val alias = StringArgumentType.getString(ctx, "alias")
        val ip    = StringArgumentType.getString(ctx, "ip")
        if (AmbientController.addLight(alias, ip, role, type))
            source.sendFeedback(Component.literal("§aAdded '$alias' @ $ip ($type, ${role.name.lowercase()})"))
        else
            source.sendFeedback(Component.literal("§eAlias already exists: $alias"))
        return 1
    }

    private fun sendError(source: FabricClientCommandSource, msg: String): Int {
        source.sendFeedback(Component.literal("§c$msg"))
        return 0
    }

    private fun parseRole(str: String): LightRole? = when (str.lowercase()) {
        "primary"   -> LightRole.PRIMARY
        "secondary" -> LightRole.SECONDARY
        else        -> null
    }

    private fun rolesSuggestions(builder: com.mojang.brigadier.suggestion.SuggestionsBuilder) =
        builder.apply { suggest("primary"); suggest("secondary") }.buildFuture()

    private fun typesSuggestions(builder: com.mojang.brigadier.suggestion.SuggestionsBuilder) =
        builder.apply { BulbClientFactory.knownTypes.forEach { suggest(it) } }.buildFuture()

    private fun aliasesSuggestions(builder: com.mojang.brigadier.suggestion.SuggestionsBuilder) =
        builder.apply { AmbientController.listAliases().forEach { suggest(it) } }.buildFuture()
}

package com.otternoon.tin

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.command.CommandManager
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import com.mojang.brigadier.arguments.StringArgumentType

class ScentCraft : ModInitializer {
	override fun onInitialize() {
		Database.init()
		UnverifiedManager.register()

		CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
			dispatcher.register(CommandManager.literal("link").then(CommandManager.argument("code", StringArgumentType.string()).executes { ctx ->
				val player = ctx.source.player ?: return@executes 0
				val code = StringArgumentType.getString(ctx, "code")

				Thread {
					val res = ManaoApi.verify(code)
					ctx.source.server.execute {
						if (res.success) {
							Database.saveLink(player.uuid, res.discordId!!)
							UnverifiedManager.remove(player)
							player.sendMessage(Text.literal("Verification Successful! Loading feet pics...").formatted(Formatting.GREEN))
						} else {
							player.sendMessage(Text.literal("Error: ${res.message}").formatted(Formatting.RED))
						}
					}
				}.start()
				1
			}))
		}

		ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
			if (Database.checkStatus(handler.player.uuid) != 0) {
				UnverifiedManager.add(handler.player)
				handler.player.sendMessage(Text.literal("Use /link <code> command\nYou can generate code by running \"/link\" command on Discord.").formatted(Formatting.GOLD))
			}
		}
	}
}
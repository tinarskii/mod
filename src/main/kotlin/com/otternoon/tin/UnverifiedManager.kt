package com.otternoon.tin

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.TypedActionResult
import net.minecraft.util.math.Vec3d
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object UnverifiedManager {
  private val unverified = ConcurrentHashMap.newKeySet<UUID>()
  private val anchor = ConcurrentHashMap<UUID, Vec3d>()

  fun add(player: ServerPlayerEntity): Boolean {
    anchor.putIfAbsent(player.uuid, player.pos)
    return unverified.add(player.uuid)
  }

  fun remove(player: ServerPlayerEntity): Boolean {
    anchor.remove(player.uuid)
    return unverified.remove(player.uuid)
  }

  fun isUnverified(player: ServerPlayerEntity) = unverified.contains(player.uuid)

  fun register() {
    AttackEntityCallback.EVENT.register { p, _, _, _, _ ->
      if (p is ServerPlayerEntity && isUnverified(p)) ActionResult.FAIL else ActionResult.PASS
    }

    UseBlockCallback.EVENT.register { p, _, _, _ ->
      if (p is ServerPlayerEntity && isUnverified(p)) ActionResult.FAIL else ActionResult.PASS
    }

    UseItemCallback.EVENT.register { p, _, _ ->
      if (p is ServerPlayerEntity && isUnverified(p))
        TypedActionResult.fail(p.mainHandStack)
      else
        TypedActionResult.pass(p.mainHandStack)
    }

    AttackBlockCallback.EVENT.register { p, _, _, _, _ ->
      if (p is ServerPlayerEntity && isUnverified(p)) ActionResult.FAIL else ActionResult.PASS
    }

    ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, _, _ ->
      !(entity is ServerPlayerEntity && isUnverified(entity))
    }

    ServerTickEvents.END_SERVER_TICK.register { server ->
      for (uuid in unverified) {
        val player = server.playerManager.getPlayer(uuid) ?: run {
          anchor.remove(uuid)
          continue
        }

        val a = anchor.computeIfAbsent(uuid) { player.pos }

        player.velocity = Vec3d.ZERO
        player.velocityDirty = true
        player.fallDistance = 0f
        player.requestTeleport(a.x, a.y, a.z)
        player.isSprinting = false

        if (player.currentScreenHandler != player.playerScreenHandler) {
          player.closeHandledScreen()
        }
      }
    }
  }
}

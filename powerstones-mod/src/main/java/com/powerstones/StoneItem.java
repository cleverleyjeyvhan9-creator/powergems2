package com.powerstones;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class StoneItem extends Item {

    public final StoneDef def;

    public StoneItem(Settings settings, StoneDef def) {
        super(settings);
        this.def = def;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (def.skill == null) {
            return ActionResult.PASS;
        }
        if (world instanceof ServerWorld serverWorld && user instanceof ServerPlayerEntity serverPlayer) {
            def.skill.run(serverPlayer, serverWorld);
            // ledakan partikel + suara setiap skill dipakai
            serverWorld.spawnParticles(def.aura, user.getX(), user.getY() + 1.0, user.getZ(),
                    40, 0.7, 0.9, 0.7, 0.08);
            serverWorld.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                    SoundCategory.PLAYERS, 1.0F, 1.2F);
            if (def.cooldownSeconds > 0) {
                user.getItemCooldownManager().set(user.getStackInHand(hand), def.cooldownSeconds * 20);
            }
        }
        return ActionResult.SUCCESS;
    }
}

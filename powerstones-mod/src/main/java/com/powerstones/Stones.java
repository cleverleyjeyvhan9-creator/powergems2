package com.powerstones;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** Daftar semua power stone beserta kemampuannya. */
public final class Stones {

    public static final Map<String, StoneItem> ITEMS = new LinkedHashMap<>();
    public static StoneItem LAPIS_MIND;
    public static StoneItem TRUE_STONE;

    private Stones() {}

    // ------------------------------------------------------------------ registrasi

    public static void register() {
        add(new StoneDef("diamond_core").aura(ParticleTypes.ENCHANTED_HIT)
                .passive(StatusEffects.STRENGTH, 0).passive(StatusEffects.RESISTANCE, 0)
                .passive(StatusEffects.HEALTH_BOOST, 0)
                .skill(40, (p, w) -> {
                    give(p, StatusEffects.RESISTANCE, 8, 2);
                    give(p, StatusEffects.ABSORPTION, 8, 2);
                    give(p, StatusEffects.STRENGTH, 8, 1);
                }));

        add(new StoneDef("emerald_pulse").aura(ParticleTypes.HAPPY_VILLAGER)
                .passive(StatusEffects.LUCK, 1).passive(StatusEffects.HERO_OF_THE_VILLAGE, 0)
                .passive(StatusEffects.SATURATION, 0)
                .skill(30, (p, w) -> {
                    give(p, StatusEffects.REGENERATION, 8, 1);
                    give(p, StatusEffects.SPEED, 8, 0);
                    for (LivingEntity e : around(p, 10, false)) {
                        if (e instanceof PlayerEntity) {
                            give(e, StatusEffects.REGENERATION, 8, 1);
                            give(e, StatusEffects.SPEED, 8, 0);
                        }
                    }
                    p.addExperience(30);
                }));

        add(new StoneDef("netherite_heart").aura(ParticleTypes.SMOKE)
                .passive(StatusEffects.FIRE_RESISTANCE, 0).passive(StatusEffects.RESISTANCE, 1)
                .passive(StatusEffects.HEALTH_BOOST, 1)
                .skill(45, (p, w) -> {
                    give(p, StatusEffects.RESISTANCE, 6, 3);
                    give(p, StatusEffects.REGENERATION, 6, 2);
                    give(p, StatusEffects.FIRE_RESISTANCE, 20, 0);
                }));

        add(new StoneDef("obsidian_veil").aura(ParticleTypes.REVERSE_PORTAL)
                .passive(StatusEffects.ABSORPTION, 1).passive(StatusEffects.RESISTANCE, 0)
                .passive(StatusEffects.SLOW_FALLING, 0)
                .skill(40, (p, w) -> {
                    give(p, StatusEffects.INVISIBILITY, 8, 0);
                    give(p, StatusEffects.ABSORPTION, 8, 3);
                    give(p, StatusEffects.SPEED, 8, 1);
                }));

        add(new StoneDef("redstone_spark").aura(ParticleTypes.CRIT)
                .passive(StatusEffects.HASTE, 0).passive(StatusEffects.SPEED, 0)
                .skill(30, (p, w) -> {
                    give(p, StatusEffects.SPEED, 10, 2);
                    give(p, StatusEffects.HASTE, 10, 2);
                    give(p, StatusEffects.JUMP_BOOST, 10, 1);
                }));

        LAPIS_MIND = add(new StoneDef("lapis_mind").aura(ParticleTypes.ENCHANT)
                .passive(StatusEffects.LUCK, 0).passive(StatusEffects.HASTE, 0)
                .passive(StatusEffects.NIGHT_VISION, 0)
                .skill(60, (p, w) -> {
                    p.addExperience(60);
                    give(p, StatusEffects.REGENERATION, 10, 0);
                    give(p, StatusEffects.HASTE, 10, 2);
                }));
        // Fortune Surge (tiap 4 menit) diatur di PowerStones.java

        add(new StoneDef("gold_rush").aura(ParticleTypes.TOTEM_OF_UNDYING)
                .passive(StatusEffects.SPEED, 1).passive(StatusEffects.JUMP_BOOST, 0)
                .passive(StatusEffects.HASTE, 0)
                .skill(30, (p, w) -> {
                    give(p, StatusEffects.SPEED, 8, 3);
                    give(p, StatusEffects.JUMP_BOOST, 8, 2);
                    give(p, StatusEffects.HASTE, 8, 2);
                }));

        add(new StoneDef("amethyst_echo").aura(ParticleTypes.GLOW)
                .passive(StatusEffects.NIGHT_VISION, 0).passive(StatusEffects.SLOW_FALLING, 0)
                .skill(15, (p, w) -> {
                    for (LivingEntity e : around(p, 8, false)) {
                        give(e, StatusEffects.NAUSEA, 8, 0);
                        give(e, StatusEffects.GLOWING, 8, 0);
                        push(e, p, -1.2, 0.3);
                    }
                    give(p, StatusEffects.RESISTANCE, 5, 1);
                    w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                            SoundCategory.PLAYERS, 3.0F, 0.6F);
                }));

        add(new StoneDef("glowstone_beacon").aura(ParticleTypes.END_ROD)
                .passive(StatusEffects.NIGHT_VISION, 0).passive(StatusEffects.REGENERATION, 0)
                .skill(20, (p, w) -> {
                    for (LivingEntity e : around(p, 10, false)) {
                        if (e instanceof PlayerEntity) {
                            e.heal(6.0F);
                            give(e, StatusEffects.ABSORPTION, 10, 1);
                        } else if (e instanceof HostileEntity) {
                            give(e, StatusEffects.BLINDNESS, 5, 0);
                            give(e, StatusEffects.GLOWING, 10, 0);
                        }
                    }
                    p.heal(6.0F);
                    give(p, StatusEffects.ABSORPTION, 10, 1);
                }));

        add(new StoneDef("prismarine_tide").aura(ParticleTypes.NAUTILUS)
                .passive(StatusEffects.DOLPHINS_GRACE, 0)
                .passive(StatusEffects.CONDUIT_POWER, 0)
                .passive(StatusEffects.WATER_BREATHING, 0)
                .passive(StatusEffects.NIGHT_VISION, 0)
                .skill(30, (p, w) -> {
                    for (LivingEntity e : around(p, 8, true)) {
                        give(e, StatusEffects.SLOWNESS, 6, 1);
                        push(e, p, -1.4, 0.2);
                    }
                    give(p, StatusEffects.SPEED, 8, 1);
                    give(p, StatusEffects.REGENERATION, 8, 0);
                }));

        add(new StoneDef("ender_shard").aura(ParticleTypes.PORTAL)
                .passive(StatusEffects.SLOW_FALLING, 0).passive(StatusEffects.JUMP_BOOST, 0)
                .skill(5, (p, w) -> {
                    blink(p, 8);
                    give(p, StatusEffects.SPEED, 3, 1);
                    w.spawnParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 1, 0.5, 0.5);
                    w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                            SoundCategory.PLAYERS, 1.0F, 1.0F);
                }));

        add(new StoneDef("soul_sand_whisper").aura(ParticleTypes.SOUL)
                .passive(StatusEffects.NIGHT_VISION, 0).passive(StatusEffects.SLOW_FALLING, 0)
                .skill(25, (p, w) -> {
                    for (LivingEntity e : around(p, 10, true)) {
                        give(e, StatusEffects.SLOWNESS, 10, 2);
                        give(e, StatusEffects.WEAKNESS, 10, 0);
                    }
                    give(p, StatusEffects.SPEED, 8, 0);
                    give(p, StatusEffects.REGENERATION, 8, 0);
                    w.spawnParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1, p.getZ(), 60, 3, 1, 3, 0.05);
                }));

        add(new StoneDef("magma_fury").aura(ParticleTypes.FLAME)
                .passive(StatusEffects.FIRE_RESISTANCE, 0).passive(StatusEffects.STRENGTH, 0)
                .skill(15, (p, w) -> {
                    for (LivingEntity e : around(p, 6, false)) {
                        e.setOnFireFor(8);
                        e.damage(w, w.getDamageSources().inFire(), 6.0F);
                    }
                    give(p, StatusEffects.STRENGTH, 6, 1);
                    give(p, StatusEffects.SPEED, 6, 0);
                    w.spawnParticles(ParticleTypes.LAVA, p.getX(), p.getY() + 1, p.getZ(), 40, 2, 0.5, 2, 0.1);
                    w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT,
                            SoundCategory.PLAYERS, 1.5F, 0.8F);
                }));

        add(new StoneDef("packed_ice_fang").aura(ParticleTypes.SNOWFLAKE)
                .passive(StatusEffects.RESISTANCE, 0).passive(StatusEffects.JUMP_BOOST, 0)
                .skill(20, (p, w) -> {
                    for (LivingEntity e : around(p, 8, false)) {
                        give(e, StatusEffects.SLOWNESS, 4, 6);
                        e.setFrozenTicks(200);
                    }
                    give(p, StatusEffects.RESISTANCE, 6, 1);
                    w.spawnParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1, p.getZ(), 60, 3, 1, 3, 0.05);
                }));

        add(new StoneDef("end_stone_void").aura(ParticleTypes.CLOUD)
                .passive(StatusEffects.SLOW_FALLING, 0).passive(StatusEffects.JUMP_BOOST, 0)
                .skill(25, (p, w) -> {
                    for (LivingEntity e : around(p, 10, false)) {
                        push(e, p, 1.4, 0.1);
                        give(e, StatusEffects.GLOWING, 8, 0);
                    }
                    w.spawnParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 80, 2, 1, 2, 0.2);
                }));

        add(new StoneDef("sculk_sense").aura(ParticleTypes.SCULK_SOUL)
                .passive(StatusEffects.NIGHT_VISION, 0)
                // pasif: mendeteksi semua makhluk di sekitar (glowing tembus dinding)
                .tick((p, w) -> {
                    for (LivingEntity e : around(p, 16, false)) {
                        give(e, StatusEffects.GLOWING, 2, 0);
                    }
                })
                .skill(10, (p, w) -> {
                    sonicBoom(p, w);
                    give(p, StatusEffects.SPEED, 5, 0);
                }));

        add(new StoneDef("copper_surge").aura(ParticleTypes.ELECTRIC_SPARK)
                .passive(StatusEffects.SPEED, 0).passive(StatusEffects.HASTE, 0)
                .skill(10, (p, w) -> {
                    give(p, StatusEffects.HASTE, 8, 2);
                    give(p, StatusEffects.SPEED, 8, 1);
                    LivingEntity t = target(p, 25);
                    if (t == null) {
                        return;
                    }
                    LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w, SpawnReason.TRIGGERED);
                    if (bolt != null) {
                        bolt.refreshPositionAfterTeleport(t.getX(), t.getY(), t.getZ());
                        w.spawnEntity(bolt);
                    }
                }));

        add(new StoneDef("crying_obsidian_tear").aura(ParticleTypes.DRIPPING_OBSIDIAN_TEAR)
                .passive(StatusEffects.RESISTANCE, 0).passive(StatusEffects.NIGHT_VISION, 0)
                .skill(30, (p, w) -> {
                    for (LivingEntity e : around(p, 10, true)) {
                        give(e, StatusEffects.WEAKNESS, 10, 1);
                    }
                    blink(p, 25);
                    give(p, StatusEffects.ABSORPTION, 8, 1);
                    give(p, StatusEffects.SPEED, 4, 1);
                }));

        add(new StoneDef("blackstone_ward").aura(ParticleTypes.ASH)
                .passive(StatusEffects.RESISTANCE, 0).passive(StatusEffects.ABSORPTION, 0)
                .skill(30, (p, w) -> {
                    p.removeStatusEffect(StatusEffects.POISON);
                    p.removeStatusEffect(StatusEffects.WITHER);
                    give(p, StatusEffects.RESISTANCE, 8, 1);
                    Direction f = p.getHorizontalFacing();
                    Direction side = f.rotateYClockwise();
                    BlockPos base = p.getBlockPos().offset(f, 2);
                    for (int s = -1; s <= 1; s++) {
                        for (int y = 0; y < 3; y++) {
                            BlockPos bp = base.offset(side, s).up(y);
                            if (w.getBlockState(bp).isAir()) {
                                w.setBlockState(bp, Blocks.BLACKSTONE.getDefaultState());
                            }
                        }
                    }
                }));

        add(new StoneDef("quartz_radiance").aura(ParticleTypes.CLOUD)
                .passive(StatusEffects.REGENERATION, 0).passive(StatusEffects.HEALTH_BOOST, 0)
                .skill(30, (p, w) -> {
                    for (StatusEffectInstance i : new ArrayList<>(p.getStatusEffects())) {
                        if (i.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) {
                            p.removeStatusEffect(i.getEffectType());
                        }
                    }
                    p.heal(6.0F);
                    p.getHungerManager().add(6, 0.6F);
                    give(p, StatusEffects.REGENERATION, 6, 1);
                    for (LivingEntity e : around(p, 8, false)) {
                        if (e instanceof PlayerEntity) {
                            e.heal(6.0F);
                        }
                    }
                }));

        add(new StoneDef("galaxy_stone").aura(ParticleTypes.END_ROD)
                .passive(StatusEffects.STRENGTH, 1)   // Strength II selama dipegang
                .passive(StatusEffects.NIGHT_VISION, 0)
                .passive(StatusEffects.SPEED, 0)
                .passive(StatusEffects.RESISTANCE, 0)
                .skill(30, (p, w) -> {                // Cosmic Erase radius 10
                    List<Entity> victims = w.getOtherEntities(p, p.getBoundingBox().expand(10),
                            e -> !(e instanceof PlayerEntity) && e.squaredDistanceTo(p) <= 100.0);
                    for (Entity e : victims) {
                        e.discard();
                    }
                    give(p, StatusEffects.RESISTANCE, 6, 2);
                    w.spawnParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 120, 4, 1.5, 4, 0.05);
                    w.spawnParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 120, 4, 1.5, 4, 0.8);
                }));

        // Sharpness V ~ +3 damage = Strength I. Skill lainnya diatur di PowerStones.java
        TRUE_STONE = add(new StoneDef("true_stone").aura(ParticleTypes.ENCHANTED_HIT)
                .passive(StatusEffects.STRENGTH, 0)
                .passive(StatusEffects.SPEED, 0)
                .sword());
    }

    private static StoneItem add(StoneDef def) {
        Identifier id = Identifier.of(PowerStones.MOD_ID, def.id);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        Item.Settings settings = new Item.Settings().registryKey(key).maxCount(1).rarity(Rarity.EPIC);
        if (def.sword) {
            settings = settings.sword(ToolMaterial.NETHERITE, 3.0F, -2.4F);
        }
        StoneItem item = new StoneItem(settings, def);
        Registry.register(Registries.ITEM, key, item);
        ITEMS.put(def.id, item);
        return item;
    }

    // ------------------------------------------------------------------ skill

    /** Sonic Boom ala Warden: lurus 15 block, menembus armor, 10 damage. */
    private static void sonicBoom(ServerPlayerEntity p, ServerWorld w) {
        Vec3d start = eye(p);
        Vec3d dir = p.getRotationVec(1.0F);
        for (int i = 1; i <= 15; i++) {
            Vec3d pt = start.add(dir.multiply(i));
            w.spawnParticles(ParticleTypes.SONIC_BOOM, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
        }
        Box box = new Box(start, start.add(dir.multiply(15))).expand(1.5);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> x != p && x.isAlive())) {
            e.damage(w, w.getDamageSources().sonicBoom(p), 10.0F);
            e.addVelocity(dir.x * 1.5, 0.4, dir.z * 1.5);
        }
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 3.0F, 1.0F);
    }

    // ------------------------------------------------------------------ helper

    private static Vec3d eye(ServerPlayerEntity p) {
        return new Vec3d(p.getX(), p.getEyeY(), p.getZ());
    }

    private static void give(LivingEntity e, RegistryEntry<StatusEffect> effect, int seconds, int amplifier) {
        e.addStatusEffect(new StatusEffectInstance(effect, seconds * 20, amplifier));
    }

    private static List<LivingEntity> around(ServerPlayerEntity p, double r, boolean hostileOnly) {
        ServerWorld w = p.getEntityWorld();
        return w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(r),
                e -> e != p && e.isAlive() && e.squaredDistanceTo(p) <= r * r
                        && (!hostileOnly || e instanceof HostileEntity));
    }

    /** strength > 0 = tarik mendekat ke pemain, strength < 0 = dorong menjauh. */
    private static void push(LivingEntity e, ServerPlayerEntity p, double strength, double lift) {
        Vec3d away = new Vec3d(e.getX() - p.getX(), 0, e.getZ() - p.getZ());
        if (away.lengthSquared() < 1.0E-4) {
            return;
        }
        Vec3d v = away.normalize().multiply(-strength);
        e.addVelocity(v.x, lift, v.z);
    }

    private static LivingEntity target(ServerPlayerEntity p, double range) {
        Vec3d look = p.getRotationVec(1.0F);
        Vec3d eye = eye(p);
        LivingEntity best = null;
        double bestDot = 0.97;
        for (LivingEntity e : around(p, range, false)) {
            Vec3d to = new Vec3d(e.getX() - eye.x, e.getBodyY(0.5) - eye.y, e.getZ() - eye.z).normalize();
            double dot = look.dotProduct(to);
            if (dot > bestDot) {
                bestDot = dot;
                best = e;
            }
        }
        return best;
    }

    private static void blink(ServerPlayerEntity p, double distance) {
        Vec3d dir = p.getRotationVec(1.0F);
        HitResult hit = p.raycast(distance, 1.0F, false);
        Vec3d dest = hit.getType() == HitResult.Type.MISS
                ? eye(p).add(dir.multiply(distance))
                : hit.getPos().subtract(dir.multiply(0.6));
        p.requestTeleport(dest.x, dest.y - 1.62, dest.z);
    }
}

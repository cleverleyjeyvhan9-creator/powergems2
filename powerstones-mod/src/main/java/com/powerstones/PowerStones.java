package com.powerstones;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PowerStones implements ModInitializer {

    public static final String MOD_ID = "powerstones";

    /** Progres kill True Stone: tersimpan permanen di pemain (tetap ada walau item hilang / pemain mati). */
    public static final AttachmentType<Integer> TRUE_KILLS = AttachmentRegistry.create(
            Identifier.of(MOD_ID, "true_kills"),
            builder -> builder.initializer(() -> 0).persistent(Codec.INT).copyOnDeath());

    // Waktu dunia (tick). Cooldown tetap berjalan walau batu tidak dipegang.
    private static final Map<UUID, Long> surgeUntil = new HashMap<>();
    private static final Map<UUID, Long> surgeReady = new HashMap<>();
    private static final Map<UUID, Long> untouchableUntil = new HashMap<>();
    private static final Map<UUID, Long> untouchableReady = new HashMap<>();
    private static final Map<UUID, Long> eraseReady = new HashMap<>();

    public static final int SKILL_2_KILLS = 25;
    public static final int SKILL_3_KILLS = 100;

    /** Jumlah kill yang ditempel di item True Stone supaya bisa tampil di tooltip (disalin dari progres pemain). */
    public static final ComponentType<Integer> KILLS_COMPONENT = Registry.register(
            Registries.DATA_COMPONENT_TYPE, Identifier.of(MOD_ID, "kills"),
            ComponentType.<Integer>builder().codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT).build());

    @Override
    public void onInitialize() {
        Stones.register();
        registerCreativeTab();

        ServerTickEvents.END_SERVER_TICK.register(PowerStones::onTick);

        // True Stone skill 2: Untouchable (2 detik tak bisa disentuh, cooldown 20 detik)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity p) || !holds(p, Stones.TRUE_STONE)) {
                return true;
            }
            if (p.getAttachedOrElse(TRUE_KILLS, 0) < SKILL_2_KILLS) {
                return true;
            }
            UUID id = p.getUuid();
            long now = p.getEntityWorld().getTime();
            if (now < untouchableUntil.getOrDefault(id, 0L)) {
                return false;
            }
            if (now >= untouchableReady.getOrDefault(id, 0L)) {
                untouchableUntil.put(id, now + 40);
                untouchableReady.put(id, now + 400);
                p.sendMessage(Text.literal("Untouchable!"), true);
                return false;
            }
            return true;
        });

        // True Stone: hitung kill + skill 3 Erase Existence (cooldown 5 menit)
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!(source.getAttacker() instanceof ServerPlayerEntity p) || !(entity instanceof MobEntity)) {
                return true;
            }
            if (!holds(p, Stones.TRUE_STONE)) {
                return true;
            }
            int kills = p.getAttachedOrElse(TRUE_KILLS, 0) + 1;
            p.setAttached(TRUE_KILLS, kills);
            syncKills(p, kills);
            if (kills == SKILL_2_KILLS) {
                p.sendMessage(Text.literal("True Stone: skill Untouchable terbuka!"), false);
            } else if (kills == SKILL_3_KILLS) {
                p.sendMessage(Text.literal("True Stone: skill Erase Existence terbuka! (level max)"), false);
            }
            UUID id = p.getUuid();
            long now = p.getEntityWorld().getTime();
            if (kills >= SKILL_3_KILLS && now >= eraseReady.getOrDefault(id, 0L)) {
                eraseReady.put(id, now + 6000);
                entity.discard(); // hilang tanpa mayat, drop, atau XP
                p.sendMessage(Text.literal("Erase Existence!"), true);
                return false;
            }
            return true;
        });

        // Lapis Mind - Fortune Surge: drop yang muncul di dekat pemain digandakan selama 15 detik
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof ItemEntity item) || item.getItemAge() > 1
                    || item.getCommandTags().contains("ps_dup")) {
                return;
            }
            long now = world.getTime();
            for (ServerPlayerEntity p : world.getPlayers()) {
                Long until = surgeUntil.get(p.getUuid());
                if (until == null || now >= until || !holds(p, Stones.LAPIS_MIND)) {
                    continue;
                }
                double d = item.squaredDistanceTo(p);
                // > 1.5 block dari pemain: abaikan item yang dibuang pemain sendiri
                if (d > 2.25 && d < 64.0) {
                    ItemEntity dup = new ItemEntity(world, item.getX(), item.getY(), item.getZ(),
                            item.getStack().copy());
                    dup.addCommandTag("ps_dup");
                    world.getServer().execute(() -> world.spawnEntity(dup));
                    return;
                }
            }
        });
    }

    private static void onTick(MinecraftServer server) {
        if (server.getTicks() % 20 != 0) {
            return;
        }
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerWorld w = p.getEntityWorld();
            long now = w.getTime();

            // Efek hanya aktif kalau batu dipegang (main hand / off hand)
            for (ItemStack stack : List.of(p.getMainHandStack(), p.getOffHandStack())) {
                if (stack.getItem() instanceof StoneItem stone) {
                    for (StatusEffectInstance e : stone.def.passives) {
                        p.addStatusEffect(new StatusEffectInstance(e));
                    }
                    if (stone.def.tickSkill != null) {
                        stone.def.tickSkill.run(p, w);
                    }
                    // aura partikel selama batu dipegang
                    w.spawnParticles(stone.def.aura, p.getX(), p.getY() + 1.0, p.getZ(), 4, 0.45, 0.7, 0.45, 0.01);
                }
            }

            // Salin progres kill pemain ke semua item True Stone di inventory (untuk tooltip)
            syncKills(p, p.getAttachedOrElse(TRUE_KILLS, 0));

            // Lapis Mind - Fortune Surge: aktif 15 detik, lalu cooldown 4 menit
            UUID id = p.getUuid();
            if (holds(p, Stones.LAPIS_MIND) && now >= surgeReady.getOrDefault(id, 0L)) {
                surgeUntil.put(id, now + 300);
                surgeReady.put(id, now + 4800);
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 300, 2));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 300, 1));
                w.spawnParticles(net.minecraft.particle.ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.0, p.getZ(),
                        80, 0.8, 1.0, 0.8, 0.5);
                p.sendMessage(Text.literal("Fortune Surge aktif! (15 detik)"), true);
            }
        }
    }

    private static void syncKills(ServerPlayerEntity p, int kills) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(Stones.TRUE_STONE) && s.getOrDefault(KILLS_COMPONENT, 0) != kills) {
                s.set(KILLS_COMPONENT, kills);
            }
        }
    }

    private static boolean holds(ServerPlayerEntity p, Item item) {
        return p.getMainHandStack().isOf(item) || p.getOffHandStack().isOf(item);
    }

    private static void registerCreativeTab() {
        RegistryKey<ItemGroup> key = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, "main"));
        Registry.register(Registries.ITEM_GROUP, key, FabricItemGroup.builder()
                .icon(() -> new ItemStack(Stones.ITEMS.get("galaxy_stone")))
                .displayName(Text.translatable("itemGroup.powerstones.main"))
                .entries((context, entries) -> Stones.ITEMS.values().forEach(entries::add))
                .build());
    }
}

package com.powerstones;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/** Deskripsi satu power stone: efek pasif, skill klik kanan, dan skill berkala. */
public final class StoneDef {

    @FunctionalInterface
    public interface Skill {
        void run(ServerPlayerEntity player, ServerWorld world);
    }

    public final String id;
    public final List<StatusEffectInstance> passives = new ArrayList<>();
    public Skill skill;          // dijalankan saat klik kanan
    public Skill tickSkill;      // dijalankan tiap 1 detik selama dipegang
    public int cooldownSeconds;  // cooldown skill klik kanan
    public boolean sword;        // true = item berperilaku seperti pedang
    public ParticleEffect aura = ParticleTypes.ENCHANT; // partikel aura saat dipegang / saat skill dipakai

    public StoneDef(String id) {
        this.id = id;
    }

    /** Efek pasif: diperbarui tiap detik selama batu dipegang, hilang +-5 detik setelah disimpan. */
    public StoneDef passive(RegistryEntry<StatusEffect> effect, int amplifier) {
        passives.add(new StatusEffectInstance(effect, 100, amplifier, true, false, true));
        return this;
    }

    public StoneDef skill(int cooldownSeconds, Skill skill) {
        this.cooldownSeconds = cooldownSeconds;
        this.skill = skill;
        return this;
    }

    public StoneDef tick(Skill skill) {
        this.tickSkill = skill;
        return this;
    }

    public StoneDef aura(ParticleEffect aura) {
        this.aura = aura;
        return this;
    }

    public StoneDef sword() {
        this.sword = true;
        return this;
    }
}

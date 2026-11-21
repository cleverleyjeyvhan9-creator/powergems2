package com.powerstones;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PowerStonesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!stack.isOf(Stones.TRUE_STONE)) {
                return;
            }
            int kills = stack.getOrDefault(PowerStones.KILLS_COMPONENT, 0);
            lines.add(Text.literal("Kill mob: " + kills).formatted(Formatting.GOLD));
            lines.add(Text.literal("Skill 1 - True Edge: aktif").formatted(Formatting.GREEN));
            lines.add(skillLine("Skill 2 - Untouchable", PowerStones.SKILL_2_KILLS, kills));
            lines.add(skillLine("Skill 3 - Erase Existence", PowerStones.SKILL_3_KILLS, kills));
        });
    }

    private static Text skillLine(String name, int need, int kills) {
        if (kills >= need) {
            return Text.literal(name + ": aktif").formatted(Formatting.GREEN);
        }
        return Text.literal(name + ": " + kills + "/" + need + " kill").formatted(Formatting.DARK_GRAY);
    }
}

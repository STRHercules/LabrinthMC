package com.labrinthmc.labrinth.event;

import com.labrinthmc.labrinth.TheLabrinth;
import com.labrinthmc.labrinth.config.LabrinthConfig;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Common gameplay hooks that must also work on a dedicated server. */
public final class LabrinthEvents {
    private static final int INSPECTION_VISION_DURATION_TICKS = 40;

    private LabrinthEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide
                || LabrinthConfig.DARKNESS_MODE.get()
                || !player.isCreative()
                || !TheLabrinth.LABRINTH_DIMENSION.equals(player.level().dimension().location())
                || player.tickCount % 20 != 0) {
            return;
        }

        MobEffectInstance existingVision = player.getEffect(MobEffects.NIGHT_VISION);
        if (existingVision == null || existingVision.getDuration() < INSPECTION_VISION_DURATION_TICKS / 2) {
            // Hidden and ambient so the inspection aid does not add HUD or particle noise.
            player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION,
                    INSPECTION_VISION_DURATION_TICKS,
                    0,
                    true,
                    false,
                    false));
        }
    }
}

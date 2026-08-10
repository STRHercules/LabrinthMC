package com.labrinthmc.labrinth.client;

import com.labrinthmc.labrinth.TheLabrinth;
import com.labrinthmc.labrinth.config.LabrinthConfig;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Client-only visibility adjustments for creative generation inspection. */
@EventBusSubscriber(modid = TheLabrinth.MOD_ID, value = Dist.CLIENT)
public final class LabrinthClientEvents {
    private LabrinthClientEvents() {
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!isInspectionView(event.getCamera())) {
            return;
        }

        // The Labrinth biome intentionally uses a black fog color. Use a neutral
        // inspection backdrop only while the creative visibility toggle is off.
        event.setRed(0.22F);
        event.setGreen(0.22F);
        event.setBlue(0.22F);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.NONE || !isInspectionView(event.getCamera())) {
            return;
        }

        float renderDistance = event.getRenderer().getRenderDistance();
        event.setNearPlaneDistance(renderDistance * 0.75F);
        event.setFarPlaneDistance(renderDistance);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    private static boolean isInspectionView(Camera camera) {
        if (LabrinthConfig.DARKNESS_MODE.get()) {
            return false;
        }

        Entity cameraEntity = camera.getEntity();
        return cameraEntity instanceof Player player
                && player.isCreative()
                && TheLabrinth.LABRINTH_DIMENSION.equals(player.level().dimension().location());
    }
}

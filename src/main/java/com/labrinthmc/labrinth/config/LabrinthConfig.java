package com.labrinthmc.labrinth.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common settings shared by the Labrinth server and client. */
public final class LabrinthConfig {
    public static final ModConfigSpec.BooleanValue DARKNESS_MODE;
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        DARKNESS_MODE = builder
                .comment(
                        "Keep the Labrinth's permanent midnight, dark lighting, and black-fog atmosphere.",
                        "Set to false for a creative-only visual inspection mode; restart after changing it.")
                .define("darkness_mode", true);
        SPEC = builder.build();
    }

    private LabrinthConfig() {
    }
}

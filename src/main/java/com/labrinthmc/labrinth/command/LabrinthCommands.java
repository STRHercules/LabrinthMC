package com.labrinthmc.labrinth.command;

import com.labrinthmc.labrinth.world.generation.GenerationGrid;
import com.labrinthmc.labrinth.world.generation.SpecialStructureCatalog;
import com.labrinthmc.labrinth.world.generation.SpecialStructureDefinition;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Small operator locator for deterministic, non-vanilla Labrinth compounds. */
public final class LabrinthCommands {
    private static final int DEFAULT_RADIUS_CELLS = 32;

    private LabrinthCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("labrinth")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("locate")
                                .then(Commands.literal("all")
                                        .executes(context -> locateAll(
                                                context.getSource(), DEFAULT_RADIUS_CELLS))
                                        .then(Commands.argument(
                                                        "radius", IntegerArgumentType.integer(1, 64))
                                                .executes(context -> locateAll(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "radius")))))
                                .then(Commands.argument("theme", StringArgumentType.word())
                                        .suggests((context, suggestions) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(SpecialStructureDefinition.Theme.values())
                                                        .map(theme -> theme.name().toLowerCase(Locale.ROOT))
                                                        .toList(),
                                                suggestions))
                                        .executes(context -> locate(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "theme"),
                                                DEFAULT_RADIUS_CELLS))
                                        .then(Commands.argument(
                                                        "radius", IntegerArgumentType.integer(1, 64))
                                                .executes(context -> locate(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "theme"),
                                                        IntegerArgumentType.getInteger(context, "radius")))))));
    }

    private static int locateAll(CommandSourceStack source, int radius) {
        ServerPlayer player = source.getEntity() instanceof ServerPlayer serverPlayer
                ? serverPlayer
                : null;
        if (player == null) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        GenerationGrid.Cell center = GenerationGrid.cellForBlock(
                player.getBlockX(), player.getBlockZ());
        int found = 0;
        for (SpecialStructureDefinition.Theme theme : SpecialStructureDefinition.Theme.values()) {
            Optional<SpecialStructureCatalog.Instance> result = SpecialStructureCatalog.findNearest(
                    player.serverLevel().getSeed(), theme, center, radius);
            if (result.isPresent()) {
                sendLocation(source, result.get());
                found++;
            }
        }
        if (found == 0) {
            source.sendFailure(Component.literal(
                    "No selected Labrinth compounds found within " + radius + " cells."));
        }
        return found;
    }

    private static int locate(CommandSourceStack source, String token, int radius) {
        ServerPlayer player = source.getEntity() instanceof ServerPlayer serverPlayer
                ? serverPlayer
                : null;
        if (player == null) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        SpecialStructureDefinition.Theme theme = parseTheme(token);
        if (theme == null) {
            source.sendFailure(Component.literal("Unknown Labrinth compound theme: " + token));
            return 0;
        }
        GenerationGrid.Cell center = GenerationGrid.cellForBlock(
                player.getBlockX(), player.getBlockZ());
        Optional<SpecialStructureCatalog.Instance> result = SpecialStructureCatalog.findNearest(
                player.serverLevel().getSeed(), theme, center, radius);
        if (result.isEmpty()) {
            source.sendFailure(Component.literal(
                    "No " + token + " found within " + radius + " cells."));
            return 0;
        }
        sendLocation(source, result.get());
        return 1;
    }

    private static SpecialStructureDefinition.Theme parseTheme(String token) {
        try {
            return SpecialStructureDefinition.Theme.valueOf(
                    token.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static void sendLocation(
            CommandSourceStack source,
            SpecialStructureCatalog.Instance instance) {
        var bounds = instance.piece().bounds();
        int x = Math.toIntExact((bounds.minBlockX() + bounds.maxBlockXExclusive()) / 2);
        int y = bounds.minY() + 1;
        int z = Math.toIntExact((bounds.minBlockZ() + bounds.maxBlockZExclusive()) / 2);
        source.sendSuccess(() -> Component.literal(
                instance.definition().theme().name().toLowerCase(Locale.ROOT)
                        + " at " + x + " " + y + " " + z
                        + " (floor " + instance.floorIndex()
                        + ", depth " + instance.depth() + ")"), false);
    }
}

package com.labrinthmc.labrinth.command;

import com.labrinthmc.labrinth.world.generation.GenerationGrid;
import com.labrinthmc.labrinth.world.generation.LabrinthDiscoveryTier;
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
        var statsAtRadius = Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                .executes(context -> statsAt(
                        context.getSource(),
                        IntegerArgumentType.getInteger(context, "blockX"),
                        IntegerArgumentType.getInteger(context, "blockZ"),
                        IntegerArgumentType.getInteger(context, "radius")));
        var statsAtBlockZ = Commands.argument("blockZ", IntegerArgumentType.integer())
                .then(statsAtRadius);
        var statsAtBlockX = Commands.argument("blockX", IntegerArgumentType.integer())
                .then(statsAtBlockZ);
        var stats = Commands.literal("stats")
                .executes(context -> stats(context.getSource(), DEFAULT_RADIUS_CELLS))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                        .executes(context -> stats(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "radius"))));
        stats.then(statsAtBlockX);

        var locateAtRadius = Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                .executes(context -> locateAt(
                        context.getSource(),
                        StringArgumentType.getString(context, "theme"),
                        IntegerArgumentType.getInteger(context, "blockX"),
                        IntegerArgumentType.getInteger(context, "blockZ"),
                        IntegerArgumentType.getInteger(context, "radius")));
        var locateAtBlockZ = Commands.argument("blockZ", IntegerArgumentType.integer())
                .then(locateAtRadius);
        var locateAtBlockX = Commands.argument("blockX", IntegerArgumentType.integer())
                .then(locateAtBlockZ);
        var locateAllRadius = Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                .executes(context -> locateAll(
                        context.getSource(),
                        IntegerArgumentType.getInteger(context, "radius")));
        var locate = Commands.literal("locate")
                .then(Commands.literal("all")
                        .executes(context -> locateAll(
                                context.getSource(), DEFAULT_RADIUS_CELLS))
                        .then(locateAllRadius));
        var themeRadius = Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                .executes(context -> locate(
                        context.getSource(),
                        StringArgumentType.getString(context, "theme"),
                        IntegerArgumentType.getInteger(context, "radius")));
        var theme = Commands.argument("theme", StringArgumentType.word())
                .suggests((context, suggestions) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(SpecialStructureDefinition.Theme.values())
                                .map(value -> value.name().toLowerCase(Locale.ROOT))
                                .toList(),
                        suggestions))
                .executes(context -> locate(
                        context.getSource(),
                        StringArgumentType.getString(context, "theme"),
                        DEFAULT_RADIUS_CELLS))
                .then(themeRadius)
                .then(locateAtBlockX);
        locate.then(theme);

        event.getDispatcher().register(
                Commands.literal("labrinth")
                        .requires(source -> source.hasPermission(2))
                        .then(stats)
                        .then(Commands.literal("inspect")
                                .executes(context -> inspect(context.getSource())))
                        .then(locate));
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

    private static int locateAt(CommandSourceStack source, String token,
            int blockX, int blockZ, int radius) {
        SpecialStructureDefinition.Theme theme = parseTheme(token);
        if (theme == null) {
            source.sendFailure(Component.literal("Unknown Labrinth compound theme: " + token));
            return 0;
        }
        Optional<SpecialStructureCatalog.Instance> result = SpecialStructureCatalog.findNearest(
                source.getServer().overworld().getSeed(),
                theme,
                GenerationGrid.cellForBlock(blockX, blockZ),
                radius);
        if (result.isEmpty()) {
            source.sendFailure(Component.literal(
                    "No " + token + " found within " + radius + " cells of "
                            + blockX + ", " + blockZ + "."));
            return 0;
        }
        sendLocation(source, result.get());
        return 1;
    }

    private static int stats(CommandSourceStack source, int radius) {
        ServerPlayer player = source.getEntity() instanceof ServerPlayer serverPlayer
                ? serverPlayer
                : null;
        if (player == null) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        GenerationGrid.Cell center = GenerationGrid.cellForBlock(
                player.getBlockX(), player.getBlockZ());
        SpecialStructureCatalog.Statistics statistics = SpecialStructureCatalog.statistics(
                player.serverLevel().getSeed(), center, radius);
        StringBuilder message = new StringBuilder()
                .append("Labrinth discoveries: ")
                .append(statistics.selectedStructures())
                .append(" / ")
                .append(statistics.candidateOrigins())
                .append(" sector owners, average distance ")
                .append(String.format(Locale.ROOT, "%.1f", statistics.averageCellDistance()))
                .append(" cells");
        for (LabrinthDiscoveryTier tier : LabrinthDiscoveryTier.values()) {
            int count = statistics.count(tier);
            if (count > 0) {
                message.append(" | ").append(tier.name().toLowerCase(Locale.ROOT))
                        .append('=').append(count);
            }
        }
        source.sendSuccess(() -> Component.literal(message.toString()), false);
        return statistics.selectedStructures();
    }

    private static int statsAt(CommandSourceStack source, int blockX, int blockZ, int radius) {
        SpecialStructureCatalog.Statistics statistics = SpecialStructureCatalog.statistics(
                source.getServer().overworld().getSeed(),
                GenerationGrid.cellForBlock(blockX, blockZ),
                radius);
        StringBuilder message = new StringBuilder()
                .append("Labrinth discoveries near ")
                .append(blockX).append(", ").append(blockZ).append(": ")
                .append(statistics.selectedStructures())
                .append(" / ")
                .append(statistics.candidateOrigins())
                .append(" sector owners, average distance ")
                .append(String.format(Locale.ROOT, "%.1f", statistics.averageCellDistance()))
                .append(" cells");
        for (LabrinthDiscoveryTier tier : LabrinthDiscoveryTier.values()) {
            int count = statistics.count(tier);
            if (count > 0) {
                message.append(" | ").append(tier.name().toLowerCase(Locale.ROOT))
                        .append('=').append(count);
            }
        }
        source.sendSuccess(() -> Component.literal(message.toString()), false);
        return statistics.selectedStructures();
    }

    private static int inspect(CommandSourceStack source) {
        ServerPlayer player = source.getEntity() instanceof ServerPlayer serverPlayer
                ? serverPlayer
                : null;
        if (player == null) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        var chunk = player.chunkPosition();
        var instances = SpecialStructureCatalog.intersecting(
                player.serverLevel().getSeed(), new GenerationGrid.Chunk(chunk.x, chunk.z));
        if (instances.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "No selected compound intersects chunk " + chunk.x + ", " + chunk.z + "."),
                    false);
            return 0;
        }
        for (SpecialStructureCatalog.Instance instance : instances) {
            sendLocation(source, instance);
        }
        return instances.size();
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
                        + ", depth " + instance.depth()
                        + ", tier " + instance.definition().tier().name().toLowerCase(Locale.ROOT)
                        + ", region " + instance.region().id()
                        + ", entrances " + instance.openConnectors().size() + ")"), false);
    }
}

package io.sre.resource_lib;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.sre.commands.MathCommands;

public class SREResource implements ModInitializer {
    public static final String MOD_ID = "sre_resource";

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Loadded SRE-lib!");
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("sre:lib").executes((ctx) -> {
                ctx.getSource().sendSuccess(() -> Component.literal("SRE-lib loaded!"), false);
                return 1;
            }));
            dispatcher.register(Commands.literal("sre:lib").executes((ctx) -> {
                ctx.getSource().sendSuccess(() -> Component.literal("SRE-lib loaded!"), false);
                return 1;
            }));
            MathCommands.register(dispatcher);
        });

    }
}
package io.sre.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import io.sre.utils.ExpressionCalculator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class MathCommands {
    private static ExpressionCalculator CALC = new ExpressionCalculator();
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("math")
                .then(Commands.argument("exp", StringArgumentType.greedyString()).suggests((ctx, builder) -> {
                    return null;
                }).executes((ctx) -> {
                    String exp = StringArgumentType.getString(ctx, "exp");
                    int ans = runExp(exp);
                    ctx.getSource().sendSuccess(() -> Component.translatable("'%s' = %s", exp, ans), false);
                    return ans;
                })));
    }

    private static int runExp(String string) {
        return (int)CALC.evaluate(string);
    }

}

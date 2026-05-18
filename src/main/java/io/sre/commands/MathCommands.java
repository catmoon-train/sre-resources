package io.sre.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import io.sre.utils.ExpressionCalculator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class MathCommands {
    private static ExpressionCalculator CALC = new ExpressionCalculator();

    public static CommandSyntaxException createSimpleSyntaxException(Exception e) {
        return new CommandSyntaxException(new SimpleCommandExceptionType(new LiteralMessage("config")),
                new LiteralMessage(e.getMessage()));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("math")
                .then(Commands.argument("exp", StringArgumentType.greedyString()).suggests((ctx, builder) -> {
                    return null;
                }).executes((ctx) -> {
                    String exp = StringArgumentType.getString(ctx, "exp");
                    int ans = 0;
                    try {
                        ans = runExp(exp);
                    } catch (Exception e) {
                        throw createSimpleSyntaxException(e);
                    }
                    final int result = ans;
                    ctx.getSource().sendSuccess(() -> Component.translatable("'%s' = %s", exp, result), false);
                    return ans;
                })));
    }

    private static int runExp(String string) throws ArithmeticException, IllegalArgumentException {
        return (int) CALC.evaluate(string);
    }

}

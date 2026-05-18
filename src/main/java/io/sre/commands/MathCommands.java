package io.sre.commands;

import java.util.Locale;

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
                    String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
                    // 函数名带括号建议
                    String[] functions = { "pow", "sqrt", "floor", "ceil", "round", "sin", "cos", "tan", "log", "exp" };
                    for (String f : functions) {
                        if (f.toLowerCase().startsWith(remaining)) {
                            builder.suggest(f + "(");
                        }
                    }
                    // 常量不带括号
                    String[] constants = { "e", "pi" };
                    for (String c : constants) {
                        if (c.toLowerCase().startsWith(remaining)) {
                            builder.suggest(c);
                        }
                    }
                    return builder.buildFuture();
                }).executes((ctx) -> {
                    String exp = StringArgumentType.getString(ctx, "exp");
                    double ans = 0;
                    try {
                        ans = runExp(exp);
                    } catch (Exception e) {
                        throw createSimpleSyntaxException(e);
                    }
                    final double result = ans;
                    ctx.getSource().sendSuccess(() -> Component.translatable("'%s' = %s", exp, result), false);
                    return (int) ans;
                })));
    }

    private static double runExp(String string) throws ArithmeticException, IllegalArgumentException {
        return CALC.evaluate(string);
    }

}

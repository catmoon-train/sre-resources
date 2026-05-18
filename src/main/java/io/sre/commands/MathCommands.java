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
                .requires(ctx -> ctx.hasPermission(2))
                .then(Commands.argument("exp", StringArgumentType.greedyString()).suggests((ctx, builder) -> {
                    String remaining = builder.getRemaining();

                    // 提取末尾连续的字母作为补全前缀
                    String prefix = "";
                    for (int i = remaining.length() - 1; i >= 0; i--) {
                        char c = remaining.charAt(i);
                        if (Character.isLetter(c)) {
                            prefix = c + prefix;
                        } else {
                            break;
                        }
                    }
                    String lowerPrefix = prefix.toLowerCase(Locale.ROOT);

                    // 计算未闭合的左括号数量
                    int openParens = 0;
                    for (char c : remaining.toCharArray()) {
                        if (c == '(')
                            openParens++;
                        else if (c == ')' && openParens > 0)
                            openParens--;
                    }

                    // 函数名及其提示（自动带左括号）
                    String[][] functions = {
                            { "pow", "pow(base, exponent)" },
                            { "sqrt", "sqrt(number)" },
                            { "floor", "floor(number)" },
                            { "ceil", "ceil(number)" },
                            { "round", "round(number)" },
                            { "sin", "sin(radians)" },
                            { "cos", "cos(radians)" },
                            { "tan", "tan(radians)" },
                            { "log", "log(number)" },
                            { "exp", "exp(number)" }
                    };
                    for (String[] f : functions) {
                        String name = f[0];
                        if (name.startsWith(lowerPrefix)) {
                            builder.suggest(name + "(", Component.literal(f[1]));
                        }
                    }
                    // 若有未闭合的左括号，提供右括号补全项
                    if (openParens > 0) {
                        builder.suggest(")", Component
                                .literal("Close " + openParens + " open parenthesis" + (openParens > 1 ? "es" : "")));
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

package io.sre.commands;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestion;

import io.sre.mixin.SuggestionsBuilderAccessor;
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
                    // 通过 accessor 获取 result 列表
                    List<Suggestion> result = ((SuggestionsBuilderAccessor) builder).getResult();
                    String fullInput = builder.getInput();
                    int cursorStart = builder.getStart(); // 参数起始索引
                    String remaining = builder.getRemaining(); // 光标后到末尾的字符串
                    int cursorAbsolute = cursorStart + remaining.length(); // 光标绝对位置（就在 remaining 末尾）

                    // 1. 提取末尾连续字母前缀
                    String prefix = "";
                    int prefixStartInRemaining = remaining.length();
                    for (int i = remaining.length() - 1; i >= 0; i--) {
                        char c = remaining.charAt(i);
                        if (Character.isLetter(c)) {
                            prefix = c + prefix;
                            prefixStartInRemaining = i;
                        } else {
                            break;
                        }
                    }

                    // 2. 函数补全（有两种策略）
                    boolean showAllFunctionsWhenNoPrefix = true; // 设置为 true：无字母时也显示全部函数（插入模式）

                    if (!prefix.isEmpty() || showAllFunctionsWhenNoPrefix) {
                        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
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
                            if (prefix.isEmpty() || f[0].startsWith(lowerPrefix)) {
                                int replaceStart, replaceEnd;
                                String suggestionText;
                                if (prefix.isEmpty()) {
                                    // 无前缀：在光标处插入 "函数名("，不覆盖任何字符
                                    replaceStart = cursorAbsolute;
                                    replaceEnd = cursorAbsolute;
                                    suggestionText = f[0] + "(";
                                } else {
                                    // 有前缀：替换掉前缀部分（保留前面的数字/运算符）
                                    replaceStart = cursorStart + prefixStartInRemaining;
                                    replaceEnd = cursorStart + remaining.length();
                                    suggestionText = f[0] + "(";
                                }
                                result.add(new Suggestion(
                                        StringRange.between(replaceStart, replaceEnd),
                                        suggestionText,
                                        Component.literal(f[1])));
                            }
                        }
                    }

                    // 3. 右括号补全：只插入一个 ')'，不覆盖原有内容
                    int openParens = 0;
                    for (char c : remaining.toCharArray()) {
                        if (c == '(')
                            openParens++;
                        else if (c == ')' && openParens > 0)
                            openParens--;
                    }
                    if (openParens > 0) {
                        // 在光标处插入一个右括号
                        result.add(new Suggestion(
                                StringRange.between(cursorAbsolute, cursorAbsolute),
                                ")",
                                Component.literal("Insert closing parenthesis")));
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

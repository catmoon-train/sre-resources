package io.sre.utils;

import java.util.*;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

public class ExpressionCalculator {

    public static void main(String[] args) {
        ExpressionCalculator calc = new ExpressionCalculator();
        String[] tests = {
                "1+1*(2*3*pow(5,6))"
        };

        for (String expr : tests) {
            try {
                double result = calc.evaluate(expr);
                System.out.println(expr + " = " + result);
            } catch (Exception e) {
                System.err.println(expr + " -> Error: " + e.getMessage());
            }
        }
    }

    public double evaluate(String expression) {
        Tokenizer tokenizer = new Tokenizer(expression);
        Parser parser = new Parser(tokenizer);
        Expr ast = parser.parse();
        return ast.eval();
    }

    // ---------- 表达式节点 ----------
    interface Expr {
        double eval();
    }

    static class Constant implements Expr {
        final double value;

        Constant(double value) {
            this.value = value;
        }

        public double eval() {
            return value;
        }
    }

    static class BinaryOp implements Expr {
        final Expr left, right;
        final String op;
        final DoubleBinaryOperator operator;

        BinaryOp(Expr left, Expr right, String op, DoubleBinaryOperator operator) {
            this.left = left;
            this.right = right;
            this.op = op;
            this.operator = operator;
        }

        public double eval() {
            if ("&&".equals(op)) {
                double l = left.eval();
                return (l != 0.0) ? (right.eval() != 0.0 ? 1.0 : 0.0) : 0.0;
            }
            if ("||".equals(op)) {
                double l = left.eval();
                return (l != 0.0) ? 1.0 : (right.eval() != 0.0 ? 1.0 : 0.0);
            }
            return operator.applyAsDouble(left.eval(), right.eval());
        }
    }

    static class UnaryOp implements Expr {
        final Expr operand;
        final String op;
        final DoubleUnaryOperator operator;

        UnaryOp(Expr operand, String op, DoubleUnaryOperator operator) {
            this.operand = operand;
            this.op = op;
            this.operator = operator;
        }

        public double eval() {
            return operator.applyAsDouble(operand.eval());
        }
    }

    static class FactorialOp implements Expr {
        final Expr operand;

        FactorialOp(Expr operand) {
            this.operand = operand;
        }

        public double eval() {
            double v = operand.eval();
            if (v < 0 || Math.abs(v - Math.round(v)) > 1e-12)
                throw new ArithmeticException("Factorial supports only non-negative integers: " + v);
            long n = Math.round(v);
            long result = 1;
            for (long i = 2; i <= n; i++)
                result *= i;
            return (double) result;
        }
    }

    static class FunctionCall implements Expr {
        final String name;
        final List<Expr> arguments;

        FunctionCall(String name, List<Expr> arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        public double eval() {
            switch (name) {
                case "floor":
                    checkArgCount(1);
                    return Math.floor(arguments.get(0).eval());
                case "ceil":
                    checkArgCount(1);
                    return Math.ceil(arguments.get(0).eval());
                case "round":
                    checkArgCount(1);
                    return (double) Math.round(arguments.get(0).eval());
                case "pow":
                    checkArgCount(2);
                    return Math.pow(arguments.get(0).eval(), arguments.get(1).eval());
                case "sqrt":
                    checkArgCount(1);
                    return Math.sqrt(arguments.get(0).eval());
                case "sin":
                    checkArgCount(1);
                    return Math.sin(arguments.get(0).eval());
                case "cos":
                    checkArgCount(1);
                    return Math.cos(arguments.get(0).eval());
                case "tan":
                    checkArgCount(1);
                    return Math.tan(arguments.get(0).eval());
                case "log":
                    checkArgCount(1);
                    return Math.log(arguments.get(0).eval());
                case "exp":
                    checkArgCount(1);
                    return Math.exp(arguments.get(0).eval());
                default:
                    throw new IllegalArgumentException("Unknown function: " + name);
            }
        }

        private void checkArgCount(int expected) {
            if (arguments.size() != expected)
                throw new IllegalArgumentException("Function " + name + " requires " + expected + " arguments, but got " + arguments.size());
        }
    }

    // ---------- 词法分析 ----------
    static class Tokenizer {
        private final String source;
        private int pos;
        private final List<Token> tokens = new ArrayList<>();

        Tokenizer(String source) {
            this.source = source.replaceAll("\\s+", "");
            this.pos = 0;
            tokenize();
        }

        private void tokenize() {
            while (pos < source.length()) {
                char c = source.charAt(pos);
                if (Character.isDigit(c)) {
                    parseNumber();
                } else if (Character.isLetter(c)) {
                    parseIdentifier();
                } else if (c == '(' || c == ')' || c == ',' || c == '~') {
                    tokens.add(new Token(TokenType.SYMBOL, String.valueOf(c)));
                    pos++;
                } else if (isOperatorStart(c)) {
                    parseOperator();
                } else {
                    throw new IllegalArgumentException("Illegal character: " + c);
                }
            }
            tokens.add(new Token(TokenType.EOF, ""));
        }

        private void parseNumber() {
            int start = pos;
            while (pos < source.length() && (Character.isDigit(source.charAt(pos)) || source.charAt(pos) == '.'))
                pos++;
            String numStr = source.substring(start, pos);
            try {
                double val = Double.parseDouble(numStr);
                tokens.add(new Token(TokenType.NUMBER, val));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number format: " + numStr);
            }
        }

        private void parseIdentifier() {
            int start = pos;
            while (pos < source.length() && Character.isLetterOrDigit(source.charAt(pos)))
                pos++;
            String id = source.substring(start, pos);
            tokens.add(new Token(TokenType.IDENTIFIER, id));
        }

        private void parseOperator() {
            if (pos + 1 < source.length()) {
                String twoChars = source.substring(pos, pos + 2);
                if (isTwoCharOperator(twoChars)) {
                    tokens.add(new Token(TokenType.SYMBOL, twoChars));
                    pos += 2;
                    return;
                }
            }
            char c = source.charAt(pos);
            String single = String.valueOf(c);
            if (isSingleOperator(single)) {
                tokens.add(new Token(TokenType.SYMBOL, single));
                pos++;
                return;
            }
            throw new IllegalArgumentException("Unknown operator: " + c);
        }

        private boolean isOperatorStart(char c) {
            return "+-*/%^&|!<>=?".indexOf(c) >= 0;
        }

        private boolean isTwoCharOperator(String s) {
            return s.equals("<<") || s.equals(">>") || s.equals("<=") || s.equals(">=") ||
                    s.equals("==") || s.equals("!=") || s.equals("&&") || s.equals("||");
        }

        private boolean isSingleOperator(String s) {
            return "+-*/%^&|!<>=?".contains(s);
        }

        public Token next() {
            return tokens.isEmpty() ? new Token(TokenType.EOF, "") : tokens.remove(0);
        }

        public Token peek() {
            return tokens.isEmpty() ? new Token(TokenType.EOF, "") : tokens.get(0);
        }

        public boolean hasMore() {
            return !tokens.isEmpty() && tokens.get(0).type != TokenType.EOF;
        }
    }

    enum TokenType {
        NUMBER, IDENTIFIER, SYMBOL, EOF
    }

    static class Token {
        TokenType type;
        String text;
        double value;

        Token(TokenType type, String text) {
            this.type = type;
            this.text = text;
        }

        Token(TokenType type, double value) {
            this.type = type;
            this.value = value;
            this.text = String.valueOf(value);
        }

        @Override
        public String toString() {
            return type + (type == TokenType.NUMBER ? " " + value : " " + text);
        }
    }

    // ---------- 语法分析 ----------
    static class Parser {
        private final Tokenizer tokenizer;
        private Token current;
        private static final Map<String, Integer> BINARY_PRECEDENCE = new HashMap<>();
        static {
            BINARY_PRECEDENCE.put("||", 1);
            BINARY_PRECEDENCE.put("&&", 2);
            BINARY_PRECEDENCE.put("|", 3);
            BINARY_PRECEDENCE.put("^", 4);
            BINARY_PRECEDENCE.put("&", 5);
            BINARY_PRECEDENCE.put("==", 6);
            BINARY_PRECEDENCE.put("!=", 6);
            BINARY_PRECEDENCE.put("<", 6);
            BINARY_PRECEDENCE.put(">", 6);
            BINARY_PRECEDENCE.put("<=", 6);
            BINARY_PRECEDENCE.put(">=", 6);
            BINARY_PRECEDENCE.put("<<", 7);
            BINARY_PRECEDENCE.put(">>", 7);
            BINARY_PRECEDENCE.put("+", 8);
            BINARY_PRECEDENCE.put("-", 8);
            BINARY_PRECEDENCE.put("*", 9);
            BINARY_PRECEDENCE.put("/", 9);
            BINARY_PRECEDENCE.put("%", 9);
        }

        Parser(Tokenizer tokenizer) {
            this.tokenizer = tokenizer;
            advance();
        }

        private void advance() {
            current = tokenizer.next();
        }

        private boolean check(TokenType type, String text) {
            return current.type == type && (text == null || current.text.equals(text));
        }

        private void consume(TokenType type, String text) {
            if (check(type, text))
                advance();
            else
                throw new IllegalArgumentException("Expected " + text + ", but got " + current);
        }

        public Expr parse() {
            Expr expr = parseExpression(0);
            if (current.type != TokenType.EOF)
                throw new IllegalArgumentException("Extra token: " + current);
            return expr;
        }

        private Expr parseExpression(int minPrecedence) {
            Expr left = parsePrimary();
            while (current.type == TokenType.SYMBOL && BINARY_PRECEDENCE.containsKey(current.text)) {
                String op = current.text;
                int precedence = BINARY_PRECEDENCE.get(op);
                if (precedence < minPrecedence)
                    break;
                advance();
                Expr right = parseExpression(precedence + 1);
                left = createBinaryNode(left, op, right);
            }
            return left;
        }

        private Expr createBinaryNode(Expr left, String op, Expr right) {
            switch (op) {
                case "+":
                    return new BinaryOp(left, right, op, Double::sum);
                case "-":
                    return new BinaryOp(left, right, op, (a, b) -> a - b);
                case "*":
                    return new BinaryOp(left, right, op, (a, b) -> a * b);
                case "/":
                    return new BinaryOp(left, right, op, (a, b) -> a / b);
                case "%":
                    return new BinaryOp(left, right, op, (a, b) -> a % b);
                case "&":
                    return new BinaryOp(left, right, op, (a, b) -> (long) a & (long) b);
                case "|":
                    return new BinaryOp(left, right, op, (a, b) -> (long) a | (long) b);
                case "^":
                    return new BinaryOp(left, right, op, (a, b) -> (long) a ^ (long) b);
                case "<<":
                    return new BinaryOp(left, right, op, (a, b) -> (long) a << (long) b);
                case ">>":
                    return new BinaryOp(left, right, op, (a, b) -> (long) a >> (long) b);
                case "<":
                    return new BinaryOp(left, right, op, (a, b) -> a < b ? 1.0 : 0.0);
                case ">":
                    return new BinaryOp(left, right, op, (a, b) -> a > b ? 1.0 : 0.0);
                case "<=":
                    return new BinaryOp(left, right, op, (a, b) -> a <= b ? 1.0 : 0.0);
                case ">=":
                    return new BinaryOp(left, right, op, (a, b) -> a >= b ? 1.0 : 0.0);
                case "==":
                    return new BinaryOp(left, right, op, (a, b) -> a == b ? 1.0 : 0.0);
                case "!=":
                    return new BinaryOp(left, right, op, (a, b) -> a != b ? 1.0 : 0.0);
                case "&&":
                    return new BinaryOp(left, right, op, null);
                case "||":
                    return new BinaryOp(left, right, op, null);
                default:
                    throw new IllegalArgumentException("Unknown binary operator " + op);
            }
        }

        private Expr parsePrimary() {
            // 一元前缀运算符
            if (check(TokenType.SYMBOL, "+")) {
                advance();
                return new UnaryOp(parsePrimary(), "+", a -> +a);
            }
            if (check(TokenType.SYMBOL, "-")) {
                advance();
                return new UnaryOp(parsePrimary(), "-", a -> -a);
            }
            if (check(TokenType.SYMBOL, "!")) {
                advance();
                return new UnaryOp(parsePrimary(), "!", a -> (a == 0.0) ? 1.0 : 0.0);
            }
            if (check(TokenType.SYMBOL, "~")) {
                advance();
                return new UnaryOp(parsePrimary(), "~", a -> ~(long) a);
            }

            Expr primary;
            if (check(TokenType.NUMBER, null)) {
                primary = new Constant(current.value);
                advance();
            } else if (check(TokenType.IDENTIFIER, null)) {
                String name = current.text;
                advance();
                if (check(TokenType.SYMBOL, "(")) {
                    advance(); // 跳过 '('
                    List<Expr> args = new ArrayList<>();
                    if (!check(TokenType.SYMBOL, ")")) {
                        args.add(parseExpression(0));
                        while (check(TokenType.SYMBOL, ",")) {
                            advance();
                            args.add(parseExpression(0));
                        }
                    }
                    consume(TokenType.SYMBOL, ")");
                    primary = new FunctionCall(name, args);
                } else {
                    throw new IllegalArgumentException("Variable not supported: " + name);
                }
            } else if (check(TokenType.SYMBOL, "(")) {
                advance();
                primary = parseExpression(0);
                consume(TokenType.SYMBOL, ")");
            } else {
                throw new IllegalArgumentException("Unexpected token: " + current);
            }

            // 后置阶乘（可多个）
            while (check(TokenType.SYMBOL, "!")) {
                advance();
                primary = new FactorialOp(primary);
            }
            return primary;
        }
    }
}
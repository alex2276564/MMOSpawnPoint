package uz.alex2276564.mmospawnpoint.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;

import java.util.*;
import java.util.function.Function;

public class PlaceholderUtils {

    // ------------- Public API -------------

    public static String setPlaceholders(Player player, String text) {
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Exception e) {
            e.printStackTrace();
            return text;
        }
    }

    /**
     * Evaluate placeholder-based condition with full expression support:
     * - Parentheses ()
     * - Operators: !, &&, ||
     * - Comparisons: ==, !=, >, >=, <, <=
     * - Numbers or quoted strings
     * - Variables: %placeholder% (resolved via PAPI), or bare literals (true/false/numbers/strings)
     */
    public static boolean checkPlaceholderCondition(Player player, String condition) {
        if (condition == null || condition.trim().isEmpty()) return false;

        try {
            ExpressionEngine engine = new ExpressionEngine();

            Function<String, String> resolver = var -> {
                // If %...% -> resolve via PAPI
                if (var.startsWith("%") && var.endsWith("%")) {
                    try {
                        return PlaceholderAPI.setPlaceholders(player, var);
                    } catch (Exception e) {
                        if (MMOSpawnPoint.getInstance().getConfigManager().getMainConfig().settings.debugMode) {
                            MMOSpawnPoint.getInstance().getLogger().warning("[MMOSpawnPoint] Placeholder error for: " + var + " -> " + e.getMessage());
                        } else {
                            MMOSpawnPoint.getInstance().getLogger().warning("[MMOSpawnPoint] Placeholder evaluation failed: " + e.getClass().getSimpleName());
                            MMOSpawnPoint.getInstance().getLogger().warning("[MMOSpawnPoint] Please enable the debug mode in config.yml to see all information.");
                        }
                        return "";
                    }
                }
                // Otherwise raw token (could be number/boolean/string)
                return var;
            };

            return engine.evaluate(condition, resolver);
        } catch (Exception e) {
            if (MMOSpawnPoint.getInstance().getConfigManager().getMainConfig().settings.debugMode) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Validate expression syntax quickly (used by validators).
     * Returns true if expression is invalid.
     */
    public static boolean isInvalidLogicalExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) return true;

        try {
            ExpressionEngine engine = new ExpressionEngine();
            engine.parseToRPN(expression); // parse only
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Evaluate permissions expression using the same expression engine.
     * Variables are permission nodes; resolver returns "true"/"false" string.
     */
    public static boolean evaluatePermissionExpression(Player player, String expression, boolean bypass) {
        if (expression == null || expression.trim().isEmpty()) return false;

        if (bypass) return true;

        try {
            ExpressionEngine engine = new ExpressionEngine();

            Function<String, String> resolver = var -> {
                // treat bare token as permission node
                boolean has = player.hasPermission(var);
                return has ? "true" : "false";
            };

            return engine.evaluate(expression, resolver);
        } catch (Exception e) {
            if (MMOSpawnPoint.getInstance().getConfigManager().getMainConfig().settings.debugMode) {
                e.printStackTrace();
            }
            return false;
        }
    }

    // ------------- Expression Engine -------------

    /**
     * Simple boolean expression engine:
     * - tokens: parentheses, !, &&, ||, ==, !=, >, >=, <, <=
     * - operands: numbers, booleans, quoted strings ("..." or '...'), variables
     * - variables resolved via provided resolver
     * - precedence: ! > comparisons > && > ||
     */
    private static class ExpressionEngine {

        private static final Set<String> OPERATORS = Set.of("!", "&&", "||", "==", "!=", ">", "<", ">=", "<=");
        private static final Map<String, Integer> PRECEDENCE = Map.of(
                "!", 4,
                "==", 3, "!=", 3, ">", 3, "<", 3, ">=", 3, "<=", 3,
                "&&", 2,
                "||", 1
        );

        public boolean evaluate(String expr, Function<String, String> resolver) {
            List<String> rpn = parseToRPN(expr);
            Deque<Object> stack = new ArrayDeque<>();

            for (String token : rpn) {
                if (OPERATORS.contains(token)) {
                    if (token.equals("!")) {
                        Object a = stack.pop();
                        boolean av = asBoolean(a);
                        stack.push(!av);
                    } else if (token.equals("&&") || token.equals("||")) {
                        Object b = stack.pop();
                        Object a = stack.pop();
                        boolean av = asBoolean(a);
                        boolean bv = asBoolean(b);
                        stack.push(token.equals("&&") ? (av && bv) : (av || bv));
                    } else {
                        // comparison operators
                        Object b = stack.pop();
                        Object a = stack.pop();
                        int cmpMode = compareOperands(a, b);
                        boolean res = switch (token) {
                            case "==" -> cmpMode == 0;
                            case "!=" -> cmpMode != 0;
                            case ">" -> cmpMode > 0;
                            case ">=" -> cmpMode >= 0;
                            case "<" -> cmpMode < 0;
                            case "<=" -> cmpMode <= 0;
                            default -> false;
                        };
                        stack.push(res);
                    }
                } else {
                    // operand: resolve variables and normalize
                    stack.push(resolveOperand(token, resolver));
                }
            }

            if (stack.isEmpty()) return false;
            return asBoolean(stack.pop());
        }

        public List<String> parseToRPN(String expr) {
            List<String> tokens = tokenize(expr);
            List<String> output = new ArrayList<>();
            Deque<String> ops = new ArrayDeque<>();

            for (String t : tokens) {
                if (isLeftParen(t)) {
                    ops.push(t);
                } else if (isRightParen(t)) {
                    while (!ops.isEmpty() && !isLeftParen(ops.peek())) {
                        output.add(ops.pop());
                    }
                    if (ops.isEmpty() || !isLeftParen(ops.peek())) {
                        throw new IllegalArgumentException("Mismatched parentheses");
                    }
                    ops.pop(); // remove '('
                } else if (OPERATORS.contains(t)) {
                    while (!ops.isEmpty() && OPERATORS.contains(ops.peek())) {
                        String top = ops.peek();
                        if ((isRightAssociative(t) && precedence(t) < precedence(top)) ||
                                (!isRightAssociative(t) && precedence(t) <= precedence(top))) {
                            output.add(ops.pop());
                        } else break;
                    }
                    ops.push(t);
                } else {
                    // operand
                    output.add(t);
                }
            }

            while (!ops.isEmpty()) {
                String op = ops.pop();
                if (isLeftParen(op)) throw new IllegalArgumentException("Mismatched parentheses");
                output.add(op);
            }

            return output;
        }

        private List<String> tokenize(String expr) {
            List<String> tokens = new ArrayList<>();
            char[] arr = expr.toCharArray();
            int n = arr.length;
            int i = 0;

            while (i < n) {
                char c = arr[i];

                if (Character.isWhitespace(c)) {
                    i++;
                    continue;
                }

                // parentheses
                if (c == '(' || c == ')') {
                    tokens.add(String.valueOf(c));
                    i++;
                    continue;
                }

                // multi-char operators
                if (i + 1 < n) {
                    String two = "" + arr[i] + arr[i + 1];
                    if (two.equals("&&") || two.equals("||") || two.equals("==") || two.equals("!=") || two.equals(">=") || two.equals("<=")) {
                        tokens.add(two);
                        i += 2;
                        continue;
                    }
                }

                // single-char operators
                if (c == '!' || c == '>' || c == '<') {
                    tokens.add(String.valueOf(c));
                    i++;
                    continue;
                }

                // quoted string
                if (c == '"' || c == '\'') {
                    char quote = c;
                    int j = i + 1;
                    StringBuilder sb = new StringBuilder();
                    while (j < n) {
                        if (arr[j] == quote) {
                            break;
                        }
                        // allow escaping with backslash
                        if (arr[j] == '\\' && j + 1 < n) {
                            sb.append(arr[j + 1]);
                            j += 2;
                            continue;
                        }
                        sb.append(arr[j]);
                        j++;
                    }
                    if (j >= n || arr[j] != quote) {
                        throw new IllegalArgumentException("Unclosed string literal");
                    }
                    tokens.add("\"" + sb + "\""); // normalized as double-quoted
                    i = j + 1;
                    continue;
                }

                // placeholder %...% or identifier/number
                if (c == '%') {
                    int j = i + 1;
                    while (j < n && arr[j] != '%') j++;
                    if (j >= n) throw new IllegalArgumentException("Unclosed placeholder");
                    String ph = expr.substring(i, j + 1);
                    tokens.add(ph);
                    i = j + 1;
                    continue;
                }

                // identifier or number: [A-Za-z0-9._:-]+
                int j = i;
                String allowed = "._:-";
                while (j < n) {
                    char ch = arr[j];
                    if (Character.isLetterOrDigit(ch) || allowed.indexOf(ch) >= 0) {
                        j++;
                    } else {
                        break;
                    }
                }
                if (j == i) {
                    throw new IllegalArgumentException("Unexpected character at " + i + ": '" + c + "'");
                }
                String ident = expr.substring(i, j);
                tokens.add(ident);
                i = j;
            }

            return tokens;
        }

        private boolean isLeftParen(String t) {
            return "(".equals(t);
        }

        private boolean isRightParen(String t) {
            return ")".equals(t);
        }

        private int precedence(String op) {
            return PRECEDENCE.getOrDefault(op, 0);
        }

        private boolean isRightAssociative(String op) {
            return "!".equals(op);
        }

        private Object resolveOperand(String token, Function<String, String> resolver) {
            // quoted string -> strip quotes
            if ((token.startsWith("\"") && token.endsWith("\""))) {
                return token.substring(1, token.length() - 1);
            }
            // booleans
            if ("true".equalsIgnoreCase(token)) return true;
            if ("false".equalsIgnoreCase(token)) return false;

            // try number
            try {
                if (token.contains(".")) {
                    return Double.parseDouble(token);
                } else {
                    // still parse as double to unify comparisons
                    return Double.parseDouble(token);
                }
            } catch (NumberFormatException ignored) {
            }

            // variable -> resolve to string
            String val = resolver.apply(token);
            if (val == null) val = "";

            // Try boolean
            if ("true".equalsIgnoreCase(val)) return true;
            if ("false".equalsIgnoreCase(val)) return false;

            // Try number
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException ignored) {
            }

            return val; // treat as string
        }

        private boolean asBoolean(Object o) {
            if (o instanceof Boolean b) return b;
            if (o instanceof Number n) return n.doubleValue() != 0.0;
            if (o instanceof String s) {
                if ("true".equalsIgnoreCase(s)) return true;
                if ("false".equalsIgnoreCase(s)) return false;
                // non-empty string -> true
                return !s.isEmpty();
            }
            return false;
        }

        private int compareOperands(Object a, Object b) {
            // number vs number
            if (a instanceof Number an && b instanceof Number bn) {
                return Double.compare(an.doubleValue(), bn.doubleValue());
            }
            // compare as strings
            String as = String.valueOf(a);
            String bs = String.valueOf(b);
            return as.compareTo(bs);
        }
    }
}
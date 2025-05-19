package uz.alex2276564.smartspawnpoint.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderUtils {

    // Regular expression to validate logical expressions
    private static final Pattern LOGICAL_EXPR_PATTERN = Pattern.compile(
            "(?:\\s*\\(\\s*)*" +                  // Opening parentheses with spaces
                    "(?:[^&|()]+)" +                      // Expression without operators
                    "(?:\\s*(?:&&|\\|\\|)\\s*[^&|()]+)*" + // && or || operators followed by expression
                    "(?:\\s*\\)\\s*)*"                    // Closing parentheses with spaces
    );

    // Check for balanced parentheses
    private static boolean hasBalancedParentheses(String expression) {
        Stack<Character> stack = new Stack<>();
        for (char c : expression.toCharArray()) {
            if (c == '(') {
                stack.push(c);
            } else if (c == ')') {
                if (stack.isEmpty() || stack.pop() != '(') {
                    return false;
                }
            }
        }
        return stack.isEmpty();
    }

    // Check for consecutive operators
    private static boolean hasConsecutiveOperators(String expression) {
        return expression.contains("&&&&") || expression.contains("||||") ||
                expression.contains("&&||") || expression.contains("||&&");
    }

    // Check for operators at the beginning or end of expression
    private static boolean hasOperatorsAtEnds(String expression) {
        String trimmed = expression.trim();
        return trimmed.startsWith("&&") || trimmed.startsWith("||") ||
                trimmed.endsWith("&&") || trimmed.endsWith("||");
    }

    // Check for empty parentheses
    private static boolean hasEmptyParentheses(String expression) {
        return expression.contains("()");
    }

    // Main validation method
    public static boolean isValidLogicalExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        // Check for basic errors
        if (!hasBalancedParentheses(expression)) {
            return false; // Unbalanced parentheses
        }

        if (hasConsecutiveOperators(expression)) {
            return false; // Consecutive operators
        }

        if (hasOperatorsAtEnds(expression)) {
            return false; // Operators at beginning or end
        }

        if (hasEmptyParentheses(expression)) {
            return false; // Empty parentheses
        }

        // Check using regular expression
        Matcher matcher = LOGICAL_EXPR_PATTERN.matcher(expression);
        return matcher.matches();
    }

    public static String setPlaceholders(Player player, String text) {
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Exception e) {
            e.printStackTrace();
            return text;
        }
    }

    public static boolean checkPlaceholderCondition(Player player, String condition) {
        try {
            // Validate expression
            if (!isValidLogicalExpression(condition)) {
                // Log error and return false
                SmartSpawnPoint.getInstance().getLogger().warning("[SmartSpawnPoint] Invalid logical expression: " + condition);
                return false;
            }

            // Check for complex conditions with && or ||
            if (condition.contains("&&") || condition.contains("||")) {
                return evaluateComplexCondition(player, condition);
            }

            // Simple condition handling (original code)
            return evaluateSimpleCondition(player, condition);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean evaluateComplexCondition(Player player, String condition) {
        // Split by OR first (lowest precedence)
        String[] orParts = condition.split("\\|\\|");

        for (String orPart : orParts) {
            // Split by AND (higher precedence)
            String[] andParts = orPart.trim().split("&&");
            boolean andResult = true;

            for (String andPart : andParts) {
                if (!evaluateSimpleCondition(player, andPart.trim())) {
                    andResult = false;
                    break;
                }
            }

            // If any OR part is true, the whole condition is true
            if (andResult) {
                return true;
            }
        }

        // If no OR part was true, the whole condition is false
        return false;
    }

    private static boolean evaluateSimpleCondition(Player player, String condition) {
        // Parse condition format: "%placeholder% operator value"
        // Example: "%player_level% > 10"

        // Find position of the first operator
        int operatorStartPos = -1;
        String[] possibleOperators = {"==", "!=", ">=", "<=", ">", "<", "="};
        for (String op : possibleOperators) {
            int pos = condition.indexOf(op);
            if (pos > 0 && (operatorStartPos == -1 || pos < operatorStartPos)) {
                operatorStartPos = pos;
            }
        }

        if (operatorStartPos == -1) {
            return false;
        }

        // Split into placeholder, operator and value
        String placeholder = condition.substring(0, operatorStartPos).trim();

        // Determine the operator
        String operator = null;
        for (String op : possibleOperators) {
            if (condition.substring(operatorStartPos).startsWith(op)) {
                operator = op;
                break;
            }
        }

        if (operator == null) {
            return false;
        }

        // Get the value
        String valueStr = condition.substring(operatorStartPos + operator.length()).trim();

        // Get the actual value from PlaceholderAPI
        String actualValueStr = PlaceholderAPI.setPlaceholders(player, placeholder);

        // Try to convert to numbers if possible
        try {
            double actualValue = Double.parseDouble(actualValueStr);
            double expectedValue = Double.parseDouble(valueStr);

            return compareValues(actualValue, expectedValue, operator);
        } catch (NumberFormatException e) {
            // If not numbers, compare as strings
            return compareStrings(actualValueStr, valueStr, operator);
        }
    }

    private static boolean compareValues(double actual, double expected, String operator) {
        switch (operator) {
            case "=":
            case "==":
                return actual == expected;
            case "!=":
                return actual != expected;
            case ">":
                return actual > expected;
            case ">=":
                return actual >= expected;
            case "<":
                return actual < expected;
            case "<=":
                return actual <= expected;
            default:
                return false;
        }
    }

    private static boolean compareStrings(String actual, String expected, String operator) {
        switch (operator) {
            case "=":
            case "==":
                return actual.equals(expected);
            case "!=":
                return !actual.equals(expected);
            default:
                return false;
        }
    }
}

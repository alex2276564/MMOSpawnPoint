package uz.alex2276564.smartspawnpoint.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class PlaceholderUtils {

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
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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
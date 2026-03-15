package uz.alex2276564.mmospawnpoint.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlaceholderUtils.ExpressionEngine")
class PlaceholderUtilsExpressionEngineTest {

    private final PlaceholderUtils.ExpressionEngine engine = new PlaceholderUtils.ExpressionEngine();

    private Function<String, String> resolver(Map<String, String> values) {
        return key -> values.getOrDefault(key, key);
    }

    @Nested
    @DisplayName("RPN parsing")
    class RpnParsingTests {

        @Test
        @DisplayName("Converts a simple expression to RPN")
        void convertsSimpleExpressionToRpn() {
            assertEquals(
                    List.of("true", "false", "!", "&&"),
                    engine.parseToRPN("true && !false")
            );
        }

        @Test
        @DisplayName("Converts a parenthesized expression to RPN")
        void convertsParenthesizedExpressionToRpn() {
            assertEquals(
                    List.of("a", "b", "||", "c", "&&"),
                    engine.parseToRPN("(a || b) && c")
            );
        }
    }

    @Nested
    @DisplayName("Boolean evaluation")
    class BooleanEvaluationTests {

        @Test
        @DisplayName("Evaluates boolean literals")
        void evaluatesBooleanLiterals() {
            assertTrue(engine.evaluate("true", key -> key));
            assertFalse(engine.evaluate("false", key -> key));
        }

        @Test
        @DisplayName("Evaluates unary negation")
        void evaluatesUnaryNegation() {
            assertTrue(engine.evaluate("!false", key -> key));
            assertFalse(engine.evaluate("!true", key -> key));
            assertTrue(engine.evaluate("!!true", key -> key));
        }

        @Test
        @DisplayName("Respects operator precedence")
        void respectsOperatorPrecedence() {
            assertFalse(engine.evaluate("false || true && false", key -> key));
            assertTrue(engine.evaluate("(false || true) && true", key -> key));
            assertTrue(engine.evaluate("!false && true", key -> key));
        }

        @Test
        @DisplayName("Supports nested parentheses")
        void supportsNestedParentheses() {
            assertTrue(engine.evaluate("((true && false) || (true && true))", key -> key));
            assertFalse(engine.evaluate("!((true || false) && true)", key -> key));
            assertTrue(engine.evaluate("(true && (false || true))", key -> key));
        }
    }

    @Nested
    @DisplayName("Comparison evaluation")
    class ComparisonEvaluationTests {

        @ParameterizedTest(name = "{0} => {1}")
        @CsvSource({
                "5 == 5, true",
                "5 == 6, false",
                "10 != 5, true",
                "5 != 5, false",
                "10 > 5, true",
                "5 > 10, false",
                "10 >= 10, true",
                "10 >= 11, false",
                "5 < 10, true",
                "10 < 5, false",
                "5 <= 5, true",
                "6 <= 5, false"
        })
        @DisplayName("Evaluates numeric comparisons")
        void evaluatesNumericComparisons(String expression, boolean expected) {
            assertEquals(expected, engine.evaluate(expression, key -> key));
        }

        @Test
        @DisplayName("Evaluates floating-point comparisons")
        void evaluatesFloatingPointComparisons() {
            assertTrue(engine.evaluate("3.14 > 3.0", key -> key));
            assertTrue(engine.evaluate("2.5 <= 2.5", key -> key));
            assertFalse(engine.evaluate("1.9 >= 2.0", key -> key));
        }

        @Test
        @DisplayName("Evaluates string comparisons case-sensitively")
        void evaluatesStringComparisonsCaseSensitively() {
            assertTrue(engine.evaluate("\"SURVIVAL\" == 'SURVIVAL'", key -> key));
            assertFalse(engine.evaluate("\"Survival\" == 'SURVIVAL'", key -> key));
            assertTrue(engine.evaluate("\"test\" != \"other\"", key -> key));
        }

        @Test
        @DisplayName("Evaluates lexicographic string comparisons")
        void evaluatesLexicographicStringComparisons() {
            assertTrue(engine.evaluate("\"apple\" < \"banana\"", key -> key));
            assertTrue(engine.evaluate("\"zebra\" > \"apple\"", key -> key));
        }

        @Test
        @DisplayName("Combines comparisons with boolean logic")
        void combinesComparisonsWithBooleanLogic() {
            assertTrue(engine.evaluate("10 >= 5 && 2 < 3", key -> key));
            assertFalse(engine.evaluate("1 > 2 || 3 < 2", key -> key));
            assertTrue(engine.evaluate("5 == 5 && (3 < 4 || 1 > 2)", key -> key));
        }
    }

    @Nested
    @DisplayName("Variable resolution")
    class VariableResolutionTests {

        @Test
        @DisplayName("Resolves boolean and numeric variables")
        void resolvesBooleanAndNumericVariables() {
            Map<String, String> values = Map.of(
                    "vip", "true",
                    "banned", "false",
                    "%level%", "15"
            );

            assertTrue(engine.evaluate("vip && !banned", resolver(values)));
            assertTrue(engine.evaluate("%level% >= 10", resolver(values)));
            assertFalse(engine.evaluate("%level% < 10", resolver(values)));
        }

        @Test
        @DisplayName("Resolves placeholder-style variables")
        void resolvesPlaceholderStyleVariables() {
            Map<String, String> values = Map.of(
                    "%player_level%", "25",
                    "%player_health%", "15.5",
                    "%player_mode%", "SURVIVAL"
            );

            assertTrue(engine.evaluate("%player_level% > 20", resolver(values)));
            assertTrue(engine.evaluate("%player_health% >= 15.0", resolver(values)));
            assertTrue(engine.evaluate("%player_mode% == 'SURVIVAL'", resolver(values)));
        }

        @Test
        @DisplayName("Handles mixed variables and literals")
        void handlesMixedVariablesAndLiterals() {
            Map<String, String> values = Map.of(
                    "%player_gamemode%", "SURVIVAL",
                    "has.permission", "true"
            );

            assertTrue(engine.evaluate(
                    "%player_gamemode% == \"SURVIVAL\" && has.permission && 5 > 3",
                    resolver(values)
            ));
        }

        @Test
        @DisplayName("Handles quoted strings with spaces")
        void handlesQuotedStringsWithSpaces() {
            assertTrue(engine.evaluate("\"hello world\" == 'hello world'", key -> key));
            assertFalse(engine.evaluate("\"hello world\" == 'hello'", key -> key));
        }

        @Test
        @DisplayName("Handles escaped quotes inside strings")
        void handlesEscapedQuotesInsideStrings() {
            assertTrue(engine.evaluate("\"hello\\\"world\" == \"hello\\\"world\"", key -> key));
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("Treats null as invalid")
        void treatsNullAsInvalid() {
            assertTrue(PlaceholderUtils.isInvalidLogicalExpression(null));
        }

        @ParameterizedTest(name = "Invalid: {0}")
        @ValueSource(strings = {
                "",
                "   ",
                "&&&&",
                "||||",
                "(",
                ")",
                "(((",
                "true &&",
                "&& true",
                "|| false",
                "\"hello",
                "'world",
                "%broken",
                "5 === 5"
        })
        @DisplayName("Detects invalid expressions")
        void detectsInvalidExpressions(String expression) {
            assertTrue(PlaceholderUtils.isInvalidLogicalExpression(expression));
        }

        @ParameterizedTest(name = "Valid: {0}")
        @ValueSource(strings = {
                "true",
                "false",
                "true && false",
                "!true || false",
                "mmospawnpoint.vip && !mmospawnpoint.banned",
                "(true || false) && true",
                "%player_level% > 10",
                "\"text\" == \"text\"",
                "5 >= 3 && 2 < 10",
                "(a || b) && (c || d)"
        })
        @DisplayName("Accepts valid expressions")
        void acceptsValidExpressions(String expression) {
            assertFalse(PlaceholderUtils.isInvalidLogicalExpression(expression));
        }
    }

    @Nested
    @DisplayName("Parsing failures")
    class ParsingFailureTests {

        @Test
        @DisplayName("Throws on mismatched parentheses")
        void throwsOnMismatchedParentheses() {
            assertThrows(IllegalArgumentException.class, () -> engine.parseToRPN("(true && false"));
            assertThrows(IllegalArgumentException.class, () -> engine.parseToRPN("true && false)"));
            assertThrows(IllegalArgumentException.class, () -> engine.parseToRPN("((true)"));
        }

        @Test
        @DisplayName("Throws on unclosed string literals")
        void throwsOnUnclosedStringLiterals() {
            assertThrows(IllegalArgumentException.class, () -> engine.parseToRPN("\"abc"));
            assertThrows(IllegalArgumentException.class, () -> engine.parseToRPN("'xyz"));
        }

        @Test
        @DisplayName("Throws on unclosed placeholders")
        void throwsOnUnclosedPlaceholders() {
            assertThrows(IllegalArgumentException.class, () -> engine.parseToRPN("%broken"));
        }

        @Test
        @DisplayName("Handles whitespace correctly")
        void handlesWhitespaceCorrectly() {
            assertTrue(engine.evaluate("  true   &&   false   ||   true  ", key -> key));
            assertTrue(engine.evaluate("\ttrue\t&&\t!false", key -> key));
            assertTrue(engine.evaluate("true&&!false", key -> key));
        }
    }
}
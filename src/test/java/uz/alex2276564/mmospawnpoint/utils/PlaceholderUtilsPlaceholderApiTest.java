package uz.alex2276564.mmospawnpoint.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PlaceholderUtils PlaceholderAPI integration")
class PlaceholderUtilsPlaceholderApiTest {

    private Logger mockLogger;

    @BeforeEach
    void setUp() {
        mockLogger = mock(Logger.class);
        PlaceholderUtils.configure(mockLogger, () -> false);
    }

    @Test
    @DisplayName("setPlaceholders delegates to PlaceholderAPI")
    void setPlaceholdersDelegatesToPlaceholderApi() {
        Player player = mock(Player.class);
        String input = "Hello %player_name%!";
        String resolved = "Hello Alex!";

        try (MockedStatic<PlaceholderAPI> placeholderApi = mockStatic(PlaceholderAPI.class)) {
            placeholderApi.when(() -> PlaceholderAPI.setPlaceholders(player, input))
                    .thenReturn(resolved);

            assertEquals(resolved, PlaceholderUtils.setPlaceholders(player, input));
        }
    }

    @Test
    @DisplayName("setPlaceholders returns the original text when PlaceholderAPI fails")
    void setPlaceholdersReturnsOriginalTextWhenPlaceholderApiFails() {
        Player player = mock(Player.class);
        String input = "Hello %player_name%!";

        try (MockedStatic<PlaceholderAPI> placeholderApi = mockStatic(PlaceholderAPI.class)) {
            placeholderApi.when(() -> PlaceholderAPI.setPlaceholders(player, input))
                    .thenThrow(new RuntimeException("PlaceholderAPI failure"));

            assertEquals(input, PlaceholderUtils.setPlaceholders(player, input));
            // In non-debug mode we expect a warning without stack trace
            verify(mockLogger).warning(contains("Placeholder replacement failed"));
        }
    }

    @Test
    @DisplayName("setPlaceholders logs full stack trace when debug mode is enabled")
    void setPlaceholdersLogsFullStackTraceWhenDebugModeEnabled() {
        PlaceholderUtils.configure(mockLogger, () -> true);
        Player player = mock(Player.class);
        String input = "Hello %player_name%!";

        try (MockedStatic<PlaceholderAPI> placeholderApi = mockStatic(PlaceholderAPI.class)) {
            placeholderApi.when(() -> PlaceholderAPI.setPlaceholders(player, input))
                    .thenThrow(new RuntimeException("PlaceholderAPI failure"));

            assertEquals(input, PlaceholderUtils.setPlaceholders(player, input));
            // In debug mode we expect logger.log(...) with the exception
            verify(mockLogger).log(
                    any(Level.class),
                    contains("Placeholder replacement error"),
                    any(RuntimeException.class)
            );
        }
    }

    @Test
    @DisplayName("checkPlaceholderCondition returns false for null or blank input")
    void checkPlaceholderConditionReturnsFalseForNullOrBlankInput() {
        Player player = mock(Player.class);

        assertFalse(PlaceholderUtils.checkPlaceholderCondition(player, null));
        assertFalse(PlaceholderUtils.checkPlaceholderCondition(player, ""));
        assertFalse(PlaceholderUtils.checkPlaceholderCondition(player, "   "));
    }

    @Test
    @DisplayName("checkPlaceholderCondition evaluates numeric placeholder expressions")
    void checkPlaceholderConditionEvaluatesNumericPlaceholderExpressions() {
        Player player = mock(Player.class);
        String expression = "%player_level% >= 10 && %player_level% < 20";

        try (MockedStatic<PlaceholderAPI> placeholderApi = mockStatic(PlaceholderAPI.class)) {
            placeholderApi.when(() -> PlaceholderAPI.setPlaceholders(player, "%player_level%"))
                    .thenReturn("15");

            assertTrue(PlaceholderUtils.checkPlaceholderCondition(player, expression));
        }
    }

    @Test
    @DisplayName("checkPlaceholderCondition evaluates mixed string and numeric placeholders")
    void checkPlaceholderConditionEvaluatesMixedStringAndNumericPlaceholders() {
        Player player = mock(Player.class);
        String expression = "%player_world% == 'dungeon' && %player_level% > 50";

        try (MockedStatic<PlaceholderAPI> placeholderApi = mockStatic(PlaceholderAPI.class)) {
            placeholderApi.when(() -> PlaceholderAPI.setPlaceholders(player, "%player_world%"))
                    .thenReturn("dungeon");
            placeholderApi.when(() -> PlaceholderAPI.setPlaceholders(player, "%player_level%"))
                    .thenReturn("75");

            assertTrue(PlaceholderUtils.checkPlaceholderCondition(player, expression));
        }
    }

    @Test
    @DisplayName("checkPlaceholderCondition returns false when resolved placeholders do not satisfy the expression")
    void checkPlaceholderConditionReturnsFalseWhenExpressionDoesNotMatch() {
        Player player = mock(Player.class);
        String expression = "%player_gamemode% == 'SURVIVAL' || %player_gamemode% == 'ADVENTURE'";

        try (MockedStatic<PlaceholderAPI> placeholderApi = mockStatic(PlaceholderAPI.class)) {
            placeholderApi.when(() -> PlaceholderAPI.setPlaceholders(player, "%player_gamemode%"))
                    .thenReturn("CREATIVE");

            assertFalse(PlaceholderUtils.checkPlaceholderCondition(player, expression));
        }
    }

    @Test
    @DisplayName("checkPlaceholderCondition returns false when PlaceholderAPI throws during placeholder resolution")
    void checkPlaceholderConditionReturnsFalseWhenPlaceholderApiThrowsDuringResolution() {
        Player player = mock(Player.class);
        String expression = "%player_level% > 10";

        try (MockedStatic<PlaceholderAPI> placeholderApi = mockStatic(PlaceholderAPI.class)) {
            placeholderApi.when(() -> PlaceholderAPI.setPlaceholders(player, "%player_level%"))
                    .thenThrow(new RuntimeException("Placeholder resolution failed"));

            assertFalse(PlaceholderUtils.checkPlaceholderCondition(player, expression));
            // We expect at least one warning about placeholder evaluation failure
            verify(mockLogger, atLeastOnce()).warning(anyString());
        }
    }

    @Test
    @DisplayName("checkPlaceholderCondition returns false for malformed expressions")
    void checkPlaceholderConditionReturnsFalseForMalformedExpressions() {
        Player player = mock(Player.class);

        assertFalse(PlaceholderUtils.checkPlaceholderCondition(player, "&&&&"));
        assertFalse(PlaceholderUtils.checkPlaceholderCondition(player, "((("));
        assertFalse(PlaceholderUtils.checkPlaceholderCondition(player, "true &&"));
    }

    @Test
    @DisplayName("checkPlaceholderCondition handles empty placeholder results gracefully")
    void checkPlaceholderConditionHandlesEmptyPlaceholderResultsGracefully() {
        Player player = mock(Player.class);
        String expression = "%player_world% == ''";

        try (MockedStatic<PlaceholderAPI> placeholderApi = mockStatic(PlaceholderAPI.class)) {
            placeholderApi.when(() -> PlaceholderAPI.setPlaceholders(player, "%player_world%"))
                    .thenReturn("");

            assertTrue(PlaceholderUtils.checkPlaceholderCondition(player, expression));
        }
    }

    @Test
    @DisplayName("checkPlaceholderCondition treats non-empty strings as truthy")
    void checkPlaceholderConditionTreatsNonEmptyStringsAsTruthy() {
        Player player = mock(Player.class);
        String expression = "%player_name%"; // non-empty string = true

        try (MockedStatic<PlaceholderAPI> placeholderApi = mockStatic(PlaceholderAPI.class)) {
            placeholderApi.when(() -> PlaceholderAPI.setPlaceholders(player, "%player_name%"))
                    .thenReturn("Alex");

            assertTrue(PlaceholderUtils.checkPlaceholderCondition(player, expression));
        }
    }

    @Test
    @DisplayName("configure can be called multiple times safely (last wins)")
    void configureCanBeCalledMultipleTimesSafely() {
        Logger logger1 = mock(Logger.class);
        Logger logger2 = mock(Logger.class);

        PlaceholderUtils.configure(logger1, () -> true);
        PlaceholderUtils.configure(logger2, () -> false);

        Player player = mock(Player.class);
        String input = "test";

        try (MockedStatic<PlaceholderAPI> placeholderApi = mockStatic(PlaceholderAPI.class)) {
            placeholderApi.when(() -> PlaceholderAPI.setPlaceholders(player, input))
                    .thenThrow(new RuntimeException("error"));

            PlaceholderUtils.setPlaceholders(player, input);

            // Should use logger2 (last configured)
            verify(logger2).warning(anyString());
            verify(logger1, never()).warning(anyString());
        }
    }
}
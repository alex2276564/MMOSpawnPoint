package uz.alex2276564.mmospawnpoint.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@DisplayName("PlaceholderUtils PlaceholderAPI integration")
class PlaceholderUtilsPlaceholderApiTest {

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
}
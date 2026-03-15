package uz.alex2276564.mmospawnpoint.utils;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@DisplayName("PlaceholderUtils permission expressions")
class PlaceholderUtilsPermissionTest {

    @Test
    @DisplayName("Evaluates a simple permission expression")
    void evaluatesSimplePermissionExpression() {
        Player player = mock(Player.class);
        when(player.hasPermission("mmospawnpoint.vip")).thenReturn(true);

        assertTrue(PlaceholderUtils.evaluatePermissionExpression(
                player,
                "mmospawnpoint.vip",
                false
        ));

        verify(player).hasPermission("mmospawnpoint.vip");
    }

    @Test
    @DisplayName("Evaluates a complex permission expression")
    void evaluatesComplexPermissionExpression() {
        Player player = mock(Player.class);

        when(player.hasPermission("mmospawnpoint.vip")).thenReturn(true);
        when(player.hasPermission("mmospawnpoint.banned")).thenReturn(false);
        when(player.hasPermission("mmospawnpoint.admin")).thenReturn(false);

        assertTrue(PlaceholderUtils.evaluatePermissionExpression(
                player,
                "mmospawnpoint.vip && !mmospawnpoint.banned",
                false
        ));

        assertFalse(PlaceholderUtils.evaluatePermissionExpression(
                player,
                "mmospawnpoint.admin || mmospawnpoint.banned",
                false
        ));
    }

    @Test
    @DisplayName("Bypass short-circuits permission evaluation")
    void bypassShortCircuitsPermissionEvaluation() {
        Player player = mock(Player.class);

        assertTrue(PlaceholderUtils.evaluatePermissionExpression(
                player,
                "any.permission.node",
                true
        ));

        verify(player, never()).hasPermission(anyString());
    }

    @Test
    @DisplayName("Returns false for null or blank expressions")
    void returnsFalseForNullOrBlankExpressions() {
        Player player = mock(Player.class);

        assertFalse(PlaceholderUtils.evaluatePermissionExpression(player, null, false));
        assertFalse(PlaceholderUtils.evaluatePermissionExpression(player, "", false));
        assertFalse(PlaceholderUtils.evaluatePermissionExpression(player, "   ", false));
    }
}
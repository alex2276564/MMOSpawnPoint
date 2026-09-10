package uz.alex2276564.mmospawnpoint;

import uz.alex2276564.mmospawnpoint.config.MMOSpawnPointConfigManager;
import uz.alex2276564.mmospawnpoint.manager.SpawnManager;
import uz.alex2276564.mmospawnpoint.party.PartyManager;
import uz.alex2276564.mmospawnpoint.utils.adventure.MessageManager;
import uz.alex2276564.mmospawnpoint.utils.runner.Runner;

import java.util.logging.Logger;

/**
 * Lightweight service container intended for command handlers only.
 * <p>
 * This record groups together the core services that are commonly needed
 * by multiple commands, so we don't have to pass 4-5 constructor parameters
 * every time.
 * <p>
 * IMPORTANT:
 * - Use this ONLY for commands and command-related classes.
 * - For listeners, utilities and other components prefer explicit
 * dependencies in constructors (no "god" service containers).
 */
public record MMOSpawnPointServices(
        Runner runner,
        MMOSpawnPointConfigManager configManager,
        MessageManager messageManager,
        Logger logger,
        SpawnManager spawnManager,
        PartyManager partyManager // may be null if party is disabled
) {
}
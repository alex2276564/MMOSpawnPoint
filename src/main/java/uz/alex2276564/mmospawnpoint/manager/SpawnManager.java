package uz.alex2276564.mmospawnpoint.manager;

import io.papermc.lib.PaperLib;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig.SpawnPointsConfig;
import uz.alex2276564.mmospawnpoint.events.MSPPostTeleportEvent;
import uz.alex2276564.mmospawnpoint.events.MSPPreTeleportEvent;
import uz.alex2276564.mmospawnpoint.party.PartyManager;
import uz.alex2276564.mmospawnpoint.utils.PlaceholderUtils;
import uz.alex2276564.mmospawnpoint.utils.SafeLocationFinder;
import uz.alex2276564.mmospawnpoint.utils.SecurityUtils;
import uz.alex2276564.mmospawnpoint.utils.runner.TaskHandle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import static uz.alex2276564.mmospawnpoint.utils.SafeLocationFinder.resolveMinY;

/**
 * SpawnManager (Runner + Folia-safe)
 *
 * - Uses your Runner API for all scheduling/teleports
 * - Paper path: multiple attempts per tick within a time budget (main thread)
 * - Folia path: exactly one attempt per tick scheduled on the correct region thread via runAtLocation
 * - Waiting room when requireSafe=true
 * - Chunk-aware area sampling (each attempt stays within a single chunk)
 * - Clean cancellation and player cleanup
 */
public class SpawnManager {

    private final MMOSpawnPoint plugin;

    // Death locations are accessed from event threads; keep it concurrent
    private final Map<UUID, Location> deathLocations = new ConcurrentHashMap<>();

    // Shared pending entry state for AFTER and WAITING_ROOM phases
    private record PendingEntry(SpawnPointsConfig.Destination loc, SpawnPointsConfig.ActionsConfig global) {}
    private final Map<UUID, PendingEntry> pendingAfterActions = new ConcurrentHashMap<>();
    private final Map<UUID, PendingEntry> pendingWaitingRoomActions = new ConcurrentHashMap<>();

    // Track active Safe Search jobs per player (waiting-room async search)
    private final Map<UUID, SafeSearchJob> activeSafeSearchJobs = new ConcurrentHashMap<>();

    // Safe Search parameters (from config)
    private final int attemptsPerTick;
    private final long timeBudgetNs;

    @Setter
    private PartyManager partyManager;

    public SpawnManager(MMOSpawnPoint plugin) {
        this.plugin = plugin;
        var batch = plugin.getConfigManager().getMainConfig().settings.safeSearchBatch;
        this.attemptsPerTick = Math.max(10, batch.attemptsPerTick);
        this.timeBudgetNs = Math.max(1, batch.timeBudgetMillis) * 1_000_000L;
    }

    // ========== Lifecycle / housekeeping ==========

    public void cleanup() {
        for (SafeSearchJob job : activeSafeSearchJobs.values()) {
            try {
                job.cancel();
            } catch (Exception ignored) {}
        }
        activeSafeSearchJobs.clear();
        pendingWaitingRoomActions.clear();
        pendingAfterActions.clear();
        deathLocations.clear();
    }

    public void cleanupPlayerData(UUID playerId) {
        try {
            deathLocations.remove(playerId);
            cancelSafeSearchJob(playerId);
            pendingWaitingRoomActions.remove(playerId);
            pendingAfterActions.remove(playerId);
            if (isDebug()) {
                plugin.getLogger().info("Cleaned up spawn manager data for player: " + playerId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error cleaning up spawn manager data for " + playerId + ": " + e.getMessage());
        }
    }

    public void recordDeathLocation(Player player, Location location) {
        // Keep the first death location only until it is consumed at respawn
        deathLocations.putIfAbsent(player.getUniqueId(), location.clone());
        if (isDebug()) {
            plugin.getLogger().info("Recorded death location for " + player.getName() + ": " + locationToString(location));
        }
    }

    // ========== Entry points (join/death flows) ==========

    public boolean processJoinSpawn(Player player) {
        try {
            if (isDebug()) {
                plugin.getLogger().info("Processing join spawn for " + player.getName());
            }

            // Party join scoped
            String partyScope = plugin.getConfigManager().getMainConfig().party.scope;
            if (plugin.getConfigManager().getMainConfig().party.enabled
                    && partyManager != null
                    && ("join".equalsIgnoreCase(partyScope) || "both".equalsIgnoreCase(partyScope))) {

                Location partyLocation = partyManager.findPartyJoinLocation(player);
                if (partyLocation != null && partyLocation != PartyManager.FALLBACK_TO_NORMAL_SPAWN_MARKER) {
                    teleportPlayerWithDelay(player, partyLocation, "join");
                    return true;
                }
            }

            // Normal rules
            Location joinLocation = findSpawnLocationByPriority("join", player.getLocation(), player);
            if (joinLocation != null) {
                teleportPlayerWithDelay(player, joinLocation, "join");
                return true;
            }

            if (isDebug()) {
                plugin.getLogger().info("No join spawn location found for " + player.getName());
            }
            plugin.getMessageManager().sendMessageKeyed(player, "general.noSpawnFound", plugin.getConfigManager().getMessagesConfig().general.noSpawnFound);
            return false;

        } catch (Exception e) {
            plugin.getLogger().severe("Error processing join spawn for " + player.getName() + ": " + e.getMessage());
            if (isDebug()) e.printStackTrace();
            return false;
        }
    }

    /**
     * Resolve join spawn location for PlayerSpawnLocationEvent.
     * <p>
     * - Does NOT teleport the player.
     * - Does NOT send "noSpawnFound" messages.
     * - Returns null to keep vanilla spawnLocation.
     * <p>
     * Used when settings.teleport.useSetSpawnLocationForJoin = true
     * and join.waitForResourcePack = false.
     */
    public Location resolveJoinSpawnLocationForSpawnEvent(Player player, Location baseSpawnLocation) {
        try {
            if (isDebug()) {
                plugin.getLogger().info("Resolving join spawn (spawn-location event) for " + player.getName()
                        + " from base location " + locationToString(baseSpawnLocation));
            }

            // Party join scoped
            String partyScope = plugin.getConfigManager().getMainConfig().party.scope;
            if (plugin.getConfigManager().getMainConfig().party.enabled
                    && partyManager != null
                    && ("join".equalsIgnoreCase(partyScope) || "both".equalsIgnoreCase(partyScope))) {

                Location partyLocation = partyManager.findPartyJoinLocation(player);
                if (partyLocation != null && partyLocation != PartyManager.FALLBACK_TO_NORMAL_SPAWN_MARKER) {
                    if (isDebug()) {
                        plugin.getLogger().info("Using party join spawn location for "
                                + player.getName() + ": " + locationToString(partyLocation));
                    }
                    return partyLocation;
                }
            }

            // Normal MSP join rules (world/region/coordinate entries)
            Location joinLocation = findSpawnLocationByPriority("join", baseSpawnLocation, player);
            if (joinLocation != null) {
                if (isDebug()) {
                    plugin.getLogger().info("Using MSP join spawn location for "
                            + player.getName() + ": " + locationToString(joinLocation));
                }
                return joinLocation;
            }

            if (isDebug()) {
                plugin.getLogger().info("No MSP join spawn location found for "
                        + player.getName() + " – keeping vanilla spawn location");
            }
            return null;

        } catch (Exception e) {
            plugin.getLogger().severe("Error resolving join spawn for "
                    + player.getName() + ": " + e.getMessage());
            if (isDebug()) e.printStackTrace();
            return null;
        }
    }

    public boolean processDeathSpawn(Player player) {
        try {
            Location deathLocation = deathLocations.remove(player.getUniqueId());
            if (deathLocation == null) {
                if (isDebug()) {
                    plugin.getLogger().info("No death location found for " + player.getName() + ", using server default");
                }
                return false;
            }

            if (isDebug()) {
                plugin.getLogger().info("Processing death spawn for " + player.getName()
                        + " who died at " + locationToString(deathLocation));
            }

            // Party death scoped
            String partyScope = plugin.getConfigManager().getMainConfig().party.scope;
            if (plugin.getConfigManager().getMainConfig().party.enabled
                    && partyManager != null
                    && ("death".equalsIgnoreCase(partyScope) || "both".equalsIgnoreCase(partyScope))) {

                Location partyLocation = partyManager.findPartyRespawnLocation(player, deathLocation);
                if (partyLocation != null && partyLocation != PartyManager.FALLBACK_TO_NORMAL_SPAWN_MARKER) {
                    if (isDebug()) {
                        plugin.getLogger().info("Using party respawn location for "
                                + player.getName() + ": " + locationToString(partyLocation));
                    }
                    teleportPlayerWithDelay(player, partyLocation, "death");
                    return true;
                }
            }

            // Normal rules
            Location spawnLocation = findSpawnLocationByPriority("death", deathLocation, player);
            if (spawnLocation != null) {
                teleportPlayerWithDelay(player, spawnLocation, "death");
                return true;
            }

            if (isDebug()) {
                plugin.getLogger().warning("No death spawn location found for " + player.getName());
            }
            plugin.getMessageManager().sendMessageKeyed(player, "general.noSpawnFound", plugin.getConfigManager().getMessagesConfig().general.noSpawnFound);
            return false;

        } catch (Exception e) {
            plugin.getLogger().severe("Error processing death spawn for "
                    + player.getName() + ": " + e.getMessage());
            if (isDebug()) e.printStackTrace();
            return false;
        }
    }

    // ========== Core resolution flow ==========

    /**
     * Returns final location or waiting-room location if requireSafe=true (async search will continue).
     */
    public Location findSpawnLocationByPriority(String eventType, Location referenceLocation, Player player) {
        List<SpawnEntry> matchingEntries = plugin.getConfigManager().getMatchingSpawnEntries(eventType, referenceLocation);

        if (isDebug()) {
            plugin.getLogger().info("Found " + matchingEntries.size()
                    + " matching spawn entries for " + eventType);
        }

        for (SpawnEntry entry : matchingEntries) {
            if (isDebug()) {
                plugin.getLogger().info("Checking spawn entry with priority "
                        + entry.calculatedPriority() + " from " + entry.fileName());
            }

            Location spawnLocation = processSpawnEntry(entry, player, eventType);
            if (spawnLocation != null) {
                if (isDebug()) {
                    plugin.getLogger().info("Selected spawn entry with priority "
                            + entry.calculatedPriority() + " from " + entry.fileName());
                }
                return spawnLocation;
            }
        }
        return null;
    }

    private Location processSpawnEntry(SpawnEntry entry, Player player, String eventType) {
        SpawnPointsConfig.SpawnPointEntry data = entry.spawnData();
        return processEntry(player, data.conditions, data.destinations, data.actions, data.waitingRoom, eventType);
    }

    /**
     * Processes one spawn entry:
     * - run BEFORE/WAITING_ROOM actions as needed
     * - if requireSafe=true => start async search job and return waiting-room loc
     * - else => return final (possibly unsafe) location, and remember AFTER phase
     */
    private Location processEntry(
            Player player,
            SpawnPointsConfig.ConditionsConfig conditions,
            List<SpawnPointsConfig.Destination> destinations,
            SpawnPointsConfig.ActionsConfig globalActions,
            SpawnPointsConfig.WaitingRoomConfig entryWaitingRoom,
            String eventType
    ) {
        if (isDebug()) {
            plugin.getLogger().info("processEntry eventType=" + eventType);
        }

        if (conditionsNotMet(player, conditions)) {
            return null;
        }

        // Actions-only entry (no teleport)
        if (destinations == null || destinations.isEmpty()) {
            runPhaseForActions(player, globalActions, SpawnPointsConfig.Phase.AFTER);
            return null;
        }

        SpawnPointsConfig.Destination selected = selectDestination(player, destinations);
        if (selected == null) return null;

        boolean requireSafe = selected.requireSafe;
        boolean useWaitingRoom = plugin.getConfigManager().getMainConfig().settings.waitingRoom.enabled && requireSafe;

        // Is this a weighted region scenario? (more than one destination option)
        boolean hasMultipleDestinations = destinations.size() > 1;

        if (useWaitingRoom) {
            if (isDebug()) {
                plugin.getLogger().info("processEntry: using waiting room for eventType=" + eventType);
            }

            // BEFORE strictly before any teleport
            runPhaseForEntry(player, selected, globalActions, SpawnPointsConfig.Phase.BEFORE);

            // Defer WAITING_ROOM phase until the player is actually in the waiting room
            pendingWaitingRoomActions.put(player.getUniqueId(), new PendingEntry(selected, globalActions));

            // Start async safe search (now with hasMultipleDestinations flag)
            long enteredMs = System.currentTimeMillis();
            startBatchedLocationSearchForSelected(
                    player,
                    selected,
                    globalActions,
                    enteredMs,
                    hasMultipleDestinations,
                    eventType
            );

            // For spawn-location based flows (death/join), schedule WAITING_ROOM phase a bit later,
            // and only consume pendingWaitingRoomActions when the player is actually online.
            if (shouldScheduleWaitingRoomPhase(eventType)) {
                plugin.getRunner().runGlobalLater(() -> {
                    if (player.isOnline()) {
                        runWaitingRoomPhaseIfPending(player);
                    }
                }, 2L);
            }

            // Waiting room location (PlayerRespawnEvent / PlayerSpawnLocationEvent will use this)
            return getBestWaitingRoom(selected.waitingRoom, entryWaitingRoom);
        }

        // Non-safe destination → immediate final
        runPhaseForEntry(player, selected, globalActions, SpawnPointsConfig.Phase.BEFORE);

        Location finalLoc = resolveNonSafeLocation(selected);
        if (finalLoc == null) return null;

        // AFTER runs after final teleport (teleportPlayerWithDelay) or after vanilla respawn/spawn
        // when useSetRespawnLocationForDeath/useSetSpawnLocationForJoin are enabled.
        pendingAfterActions.put(player.getUniqueId(), new PendingEntry(selected, globalActions));
        return finalLoc;
    }

    private boolean shouldScheduleWaitingRoomPhase(String eventType) {
        var mainCfg = plugin.getConfigManager().getMainConfig();

        if ("death".equalsIgnoreCase(eventType)) {
            return mainCfg.settings.teleport.useSetRespawnLocationForDeath;
        }
        if ("join".equalsIgnoreCase(eventType)) {
            // Only when we actually use PlayerSpawnLocationEvent for join,
            // and resource-pack waiting is not enabled.
            return mainCfg.settings.teleport.useSetSpawnLocationForJoin
                    && !mainCfg.join.waitForResourcePack;
        }
        return false;
    }

    /**
     * Run WAITING_ROOM phase once (if pending). Returns true if executed.
     */
    private boolean runWaitingRoomPhaseIfPending(Player player) {
        UUID id = player.getUniqueId();
        PendingEntry wr = pendingWaitingRoomActions.get(id);
        if (wr == null) {
            return false;
        }
        if (!player.isOnline()) {
            // Do not consume entry yet; let cleanupPlayerData or next attempt handle it.
            return false;
        }
        pendingWaitingRoomActions.remove(id);
        runPhaseForEntry(player, wr.loc, wr.global, SpawnPointsConfig.Phase.WAITING_ROOM);
        return true;
    }

    // ========== Destination selection / conditions ==========

    private SpawnPointsConfig.Destination selectDestination(Player player, List<SpawnPointsConfig.Destination> options) {
        if (options.size() == 1) return options.get(0);

        // 1) Compute effective weights once
        List<Integer> weights = new ArrayList<>(options.size());
        int total = 0;
        for (SpawnPointsConfig.Destination opt : options) {
            int w = getEffectiveWeight(player, opt);
            weights.add(w);
            total += w;
        }

        // 2) Debug log of weights (after conditions)
        if (isDebug()) {
            StringBuilder sb = new StringBuilder();
            sb.append("[MMOSpawnPoint] Destination weights (after conditions): ");
            for (int i = 0; i < options.size(); i++) {
                SpawnPointsConfig.Destination opt = options.get(i);
                int w = weights.get(i);
                sb.append("#").append(i + 1)
                        .append("{world=").append(opt.world)
                        .append(", requireSafe=").append(opt.requireSafe)
                        .append(", weight=").append(w)
                        .append("}");
                if (i < options.size() - 1) sb.append(", ");
            }
            plugin.getLogger().info(sb.toString());
        }

        if (total <= 0) return null;

        // 3) Weighted pick using precomputed weights
        int rnd = ThreadLocalRandom.current().nextInt(total);
        int acc = 0;
        for (int i = 0; i < options.size(); i++) {
            acc += weights.get(i);
            if (rnd < acc) {
                return options.get(i);
            }
        }
        // Fallback
        return options.get(0);
    }

    private int getEffectiveWeight(Player player, SpawnPointsConfig.Destination option) {
        int weight = option.weight;
        if (option.weightConditions != null) {
            boolean bypass = player.isOp() || player.hasPermission("*");
            for (SpawnPointsConfig.WeightConditionEntry cond : option.weightConditions) {
                if (!matchesCondition(player, cond.type, cond.value, bypass)) {
                    continue;
                }

                String mode = normalizeMode(cond.mode);
                switch (mode) {
                    case "set" -> weight = cond.weight;
                    case "add" -> weight = weight + cond.weight;
                    case "mul" -> weight = (int) Math.round(weight * (double) cond.weight);
                }
                weight = clampWeight(weight);
            }
        }
        return weight;
    }

    private boolean conditionsNotMet(Player player, SpawnPointsConfig.ConditionsConfig conditions) {
        if (conditions == null) return false;

        boolean bypass = player.isOp() || player.hasPermission("*");

        if (conditions.permissions != null && !conditions.permissions.isEmpty()) {
            for (String permissionExpr : conditions.permissions) {
                if (!PlaceholderUtils.evaluatePermissionExpression(player, permissionExpr, bypass)) {
                    return true;
                }
            }
        }

        if (conditions.placeholders != null && !conditions.placeholders.isEmpty() && plugin.isPlaceholderAPIEnabled()) {
            for (String placeholderExpr : conditions.placeholders) {
                if (!PlaceholderUtils.checkPlaceholderCondition(player, placeholderExpr)) {
                    return true;
                }
            }
        }

        return false;
    }

    // ========== Waiting-room: async safe-location search job ==========

    private void startBatchedLocationSearchForSelected(Player player, SpawnPointsConfig.Destination selected, SpawnPointsConfig.ActionsConfig global, long entered, boolean hasMultiple, String event) {
        UUID pid = player.getUniqueId();
        SafeSearchJob newJob = new SafeSearchJob(player, selected, global, entered, hasMultiple, event);

        activeSafeSearchJobs.compute(pid, (id, old) -> {
            if (old != null) {
                try { old.cancel(); } catch (Exception ignored) {}
            }
            return newJob;
        });

        newJob.start();
    }

    private void cancelSafeSearchJob(UUID playerId) {
        activeSafeSearchJobs.computeIfPresent(playerId, (id, old) -> {
            try { old.cancel(); } catch (Exception ignored) {}
            return null;
        });
    }

    /**
     * One async search job per player while they are in the waiting room.
     * <p>
     * Paper:
     *  - Multiple attempts per tick on the main thread within timeBudgetNs
     * Folia:
     *  - Exactly one attempt per tick; we schedule that attempt on the region owning the candidate location (runAtLocation)
     */
    private final class SafeSearchJob {

        final Player player;
        final UUID playerId;
        final SpawnPointsConfig.Destination option;
        final SpawnPointsConfig.ActionsConfig globalActions;
        final long waitingEnteredAtMs;
        final String eventType;

        // Chunk-loading state (used by attemptInRect)
        int pendingChunkX = Integer.MIN_VALUE;
        int pendingChunkZ = Integer.MIN_VALUE;
        boolean waitingChunkLoad = false;

        final World world;
        final boolean isPoint;
        int currentRadius;
        int attemptCount;

        // cache profile
        final boolean cacheEnabled;
        final boolean cachePlayerSpecific;
        @Getter
        final String cacheTypeTag;

        // Debug counters (optional)
        int failFeet = 0;
        int failHead = 0;
        int failGroundNotSolid = 0;
        int failGroundBlacklisted = 0;
        int failGroundNotWhitelisted = 0;

        // Runner task handle
        private TaskHandle handle;
        private volatile boolean attemptInProgress = false;

        // Precomputed include/exclude rects for area search
        final List<Rect> includeRects;
        final List<Rect> excludeRects;

        SafeSearchJob(Player p,
                      SpawnPointsConfig.Destination option,
                      SpawnPointsConfig.ActionsConfig globalActions,
                      long waitingEnteredAtMs,
                      boolean areaMultiple,
                      String eventType) {
            this.player = p;
            this.playerId = p.getUniqueId();
            this.option = option;
            this.globalActions = globalActions;
            this.waitingEnteredAtMs = waitingEnteredAtMs;
            this.eventType = eventType.toLowerCase(Locale.ROOT);

            this.world = Bukkit.getWorld(option.world);
            this.isPoint = isPointOption(option);
            this.currentRadius = plugin.getConfigManager().getMainConfig().settings.safeLocationRadius;
            this.attemptCount = 0;

            // Build rects only if world is present
            if (this.world != null) {
                this.includeRects = buildRectsForOption(option, world);
                this.excludeRects = toRects(option.excludeRects, world);
            } else {
                this.includeRects = Collections.emptyList();
                this.excludeRects = Collections.emptyList();
            }

            // Determine cache profile from main config
            var cacheCfg = plugin.getConfigManager().getMainConfig().settings.safeLocationCache.spawnTypeCaching;

            boolean cEnabled;
            boolean cPlayerSpecific;
            String cTag;

            if (isPoint) {
                cEnabled = cacheCfg.pointSafe.enabled;
                cPlayerSpecific = cacheCfg.pointSafe.playerSpecific;
                cTag = "POINT_SAFE";
            } else {
                if (areaMultiple) {
                    cEnabled = cacheCfg.areaSafeMultiple.enabled;
                    cPlayerSpecific = cacheCfg.areaSafeMultiple.playerSpecific;
                    cTag = "AREA_SAFE_MULTIPLE";
                } else {
                    cEnabled = cacheCfg.areaSafeSingle.enabled;
                    cPlayerSpecific = cacheCfg.areaSafeSingle.playerSpecific;
                    cTag = "AREA_SAFE_SINGLE";
                }
            }

            // Per-destination cache override (optional)
            if (option.cache != null) {
                if (option.cache.enabled != null) cEnabled = option.cache.enabled;
                if (option.cache.playerSpecific != null) cPlayerSpecific = option.cache.playerSpecific;
            }

            this.cacheEnabled = cEnabled;
            this.cachePlayerSpecific = cPlayerSpecific;
            this.cacheTypeTag = cTag;
        }

        void start() {
            handle = plugin.getRunner().runGlobalTimer(this::tick, 1L, 1L);
        }

        void cancel() {
            if (handle != null) handle.cancel();
        }

        private void tick() {
            try {
                if (world == null) {
                    finish(null, false);
                    return;
                }

                // Timeout — should work even if the player is not yet considered online
                long timeoutMs = plugin.getConfigManager().getMainConfig().settings.waitingRoom.asyncSearchTimeout * 1000L;
                if (timeoutMs > 0 && System.currentTimeMillis() - waitingEnteredAtMs > timeoutMs) {
                    if (isDebug()) {
                        plugin.getLogger().warning("[MMOSpawnPoint] Safe search TIMEOUT for "
                                + player.getName() + " in world=" + world.getName()
                                + " attempts=" + attemptCount
                                + " fail{feet=" + failFeet
                                + ", head=" + failHead
                                + ", groundNotSolid=" + failGroundNotSolid
                                + ", blacklisted=" + failGroundBlacklisted
                                + ", notWhitelisted=" + failGroundNotWhitelisted + "}"
                        );
                    }
                    finish(null, false);
                    return;
                }

                // If the player is not yet online
                if (!player.isOnline()) {
                    // For join scenarios (PlayerSpawnLocationEvent), the player may not be online yet.
                    // In this case, we simply wait for the next tick (or until the timeout/quit cleanup triggers).
                    if ("join".equals(this.eventType)) {
                        return;
                    }
                    // For death and other scenarios — exit immediately.
                    finish(null, false);
                    return;
                }

            if (plugin.getRunner().isFolia()) {
                // Folia: do one attempt per tick on the proper region thread
                if (attemptInProgress) return;
                attemptInProgress = true;
                attemptCount++;

                // Choose candidate location (no world access on global thread)
                final Location candidateRegionLoc = chooseCandidateRegionLocation();

                plugin.getRunner().runAtLocation(candidateRegionLoc, () -> {
                    try {
                        Location found = singleAttemptInRegion(candidateRegionLoc);
                        if (found != null) {
                            plugin.getRunner().runGlobal(() -> finish(found, true));
                        }
                    } finally {
                        attemptInProgress = false;
                    }
                });

            } else {
                // Paper: burst multiple attempts within the time budget on main thread
                long endBy = System.nanoTime() + timeBudgetNs;
                int attemptsThisTick = 0;

                while (attemptsThisTick < attemptsPerTick && System.nanoTime() < endBy) {
                    attemptsThisTick++;
                    attemptCount++;

                    Location found = singleAttemptLocal();
                    if (found != null) {
                        finish(found, true);
                        return;
                    }
                }
            }
            } catch (Throwable t) {
                plugin.getLogger().severe("[SafeSearchJob] tick fatal: " + t.getMessage());
                try { finish(null, false); } catch (Exception ignored) {}
            }
        }

        /**
         * Choose a region location where the attempt will be executed on Folia.
         * - For fixed point: use that point (approx y=100 if not provided)
         * - For area: choose a random chunk center inside one of include rects (or axes-derived rect)
         */
        private Location chooseCandidateRegionLocation() {
            if (isPoint) {
                double x = option.x.value;
                double z = option.z.value;
                double y = (option.y != null && option.y.isValue()) ? option.y.value : 100.0;
                return new Location(world, x, y, z);
            }

            Rect rect = includeRects.isEmpty() ? rectFromAxesOption(option, world) : pickRect(includeRects);

            double rxMinX = Math.min(rect.minX(), rect.maxX());
            double rxMaxX = Math.max(rect.minX(), rect.maxX());
            double rzMinZ = Math.min(rect.minZ(), rect.maxZ());
            double rzMaxZ = Math.max(rect.minZ(), rect.maxZ());

            double x = ThreadLocalRandom.current().nextDouble(rxMinX, rxMaxX);
            double z = ThreadLocalRandom.current().nextDouble(rzMinZ, rzMaxZ);

            int cx = ((int) Math.floor(x)) >> 4;
            int cz = ((int) Math.floor(z)) >> 4;

            double centerX = (cx << 4) + 8.0;
            double centerZ = (cz << 4) + 8.0;
            double centerY = (rect.minY() + rect.maxY()) / 2.0;

            return new Location(world, centerX, centerY, centerZ);
        }

        /**
         * Paper attempt: safe to read world on main thread.
         */
        private Location singleAttemptLocal() {
            SafeLocationFinder.YSelectionOverride yov = buildYOverride(option);

            if (isPoint) {
                double x = option.x.value;
                double z = option.z.value;
                double y = (option.y != null && option.y.isValue())
                        ? option.y.value
                        : world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1.0;
                Location base = new Location(world, x, y, z);

                // Ensure chunk is loaded
                if (!world.isChunkLoaded(base.getBlockX() >> 4, base.getBlockZ() >> 4)) {
                    PaperLib.getChunkAtAsync(base, true).thenRun(() -> {});
                    return null;
                }

                Set<Material> wl = toMaterialSet(option.groundWhitelist);
                String tag = getCacheTypeTag();
                Location found = SafeLocationFinder.withYSelectionOverride(yov, () ->
                        cacheEnabled
                                ? SafeLocationFinder.cachedFindSafeNear(base, currentRadius, wl, playerId, cachePlayerSpecific, true, tag, null)
                                : SafeLocationFinder.attemptSafeNearOnce(base, currentRadius, wl)
                );

                if (found == null) {
                    bumpFailCounters();
                    maybeExpandRadiusForNearSearch();
                    return null;
                }
                applyYawPitch(option, found);
                return found;

            } else {
                // Area path
                Rect rect = includeRects.isEmpty() ? rectFromAxesOption(option, world) : pickRect(includeRects);
                Location found = SafeLocationFinder.withYSelectionOverride(yov, () ->
                        attemptInRectLocal(rect, excludeRects, toMaterialSet(option.groundWhitelist))
                );
                if (found == null) {
                    bumpFailCounters();
                    return null;
                }
                applyYawPitch(option, found);
                return found;
            }
        }


        /**
         * Folia attempt on the correct region thread.
         * The parameter regionLoc is where this task is scheduled (must be in the same chunk as sampling).
         */
        // SafeSearchJob method
        private Location singleAttemptInRegion(Location regionLoc) {
            SafeLocationFinder.YSelectionOverride yov = buildYOverride(option);

            if (isPoint) {
                double x = option.x.value;
                double z = option.z.value;

                int cx = ((int) Math.floor(x)) >> 4;
                int cz = ((int) Math.floor(z)) >> 4;

                if (!world.isChunkLoaded(cx, cz)) {
                    double px = (cx << 4) + 8.0;
                    double pz = (cz << 4) + 8.0;
                    Location probe = new Location(world, px, regionLoc.getY(), pz);
                    PaperLib.getChunkAtAsync(probe, true).thenRun(() -> {});
                    return null;
                }

                double y = (option.y != null && option.y.isValue())
                        ? option.y.value
                        : world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1.0;

                Location base = new Location(world, x, y, z);

                Set<Material> wl = toMaterialSet(option.groundWhitelist);
                String tag = getCacheTypeTag();
                Location found = SafeLocationFinder.withYSelectionOverride(yov, () ->
                        cacheEnabled
                                ? SafeLocationFinder.cachedFindSafeNear(base, currentRadius, wl, playerId, cachePlayerSpecific, true, tag, null)
                                : SafeLocationFinder.attemptSafeNearOnce(base, currentRadius, wl)
                );
                if (found == null) {
                    bumpFailCounters();
                    maybeExpandRadiusForNearSearch();
                    return null;
                }

                applyYawPitch(option, found);
                return found;
            } else {
                int cx = regionLoc.getBlockX() >> 4;
                int cz = regionLoc.getBlockZ() >> 4;

                if (!world.isChunkLoaded(cx, cz)) {
                    double px = (cx << 4) + 8.0;
                    double pz = (cz << 4) + 8.0;
                    Location probe = new Location(world, px, regionLoc.getY(), pz);
                    PaperLib.getChunkAtAsync(probe, true).thenRun(() -> {});
                    return null;
                }

                Rect rect = includeRects.isEmpty()
                        ? rectFromAxesOption(option, world)
                        : pickRectClosestToChunkCenter(regionLoc, includeRects);

                double chunkMinX = (cx << 4);
                double chunkMaxX = chunkMinX + 15.0;
                double chunkMinZ = (cz << 4);
                double chunkMaxZ = chunkMinZ + 15.0;

                double rxMinX = Math.min(rect.minX(), rect.maxX());
                double rxMaxX = Math.max(rect.minX(), rect.maxX());
                double rzMinZ = Math.min(rect.minZ(), rect.maxZ());
                double rzMaxZ = Math.max(rect.minZ(), rect.maxZ());

                double minX = Math.max(rxMinX, chunkMinX);
                double maxX = Math.min(rxMaxX, chunkMaxX);
                double minZ = Math.max(rzMinZ, chunkMinZ);
                double maxZ = Math.min(rzMaxZ, chunkMaxZ);

                if (minX > maxX || minZ > maxZ) {
                    return null;
                }

                Set<Material> wl = toMaterialSet(option.groundWhitelist);
                String tag = getCacheTypeTag();
                Predicate<Location> notExcluded = l -> isOutsideAny(l, excludeRects);

                Location found = SafeLocationFinder.withYSelectionOverride(yov, () ->
                        cacheEnabled
                                ? SafeLocationFinder.cachedFindSafeInAreaValidated(
                                world, minX, maxX, rect.minY(), rect.maxY(), minZ, maxZ,
                                wl, playerId, cachePlayerSpecific, true, tag, notExcluded)
                                : SafeLocationFinder.attemptSafeInAreaOnce(world, minX, maxX, rect.minY(), rect.maxY(), minZ, maxZ, wl)
                );

                if (found == null) return null;

                if (!excludeRects.isEmpty()) {
                    for (Rect ex : excludeRects) {
                        if (isInsideRect(found, ex)) return null;
                    }
                }

                applyYawPitch(option, found);
                return found;
            }
        }

        /**
         * Paper-only area attempt with chunk awareness.
         */
        private Location attemptInRectLocal(Rect rect, List<Rect> exclude, Set<Material> wl) {
            SafeLocationFinder.YSelectionOverride yov = buildYOverride(option);

            double rxMinX = Math.min(rect.minX(), rect.maxX());
            double rxMaxX = Math.max(rect.minX(), rect.maxX());
            double rzMinZ = Math.min(rect.minZ(), rect.maxZ());
            double rzMaxZ = Math.max(rect.minZ(), rect.maxZ());

            // 1) Pending chunk already requested
            if (waitingChunkLoad) {
                if (!world.isChunkLoaded(pendingChunkX, pendingChunkZ)) {
                    return null; // still loading
                }
                waitingChunkLoad = false;

                double chunkMinX = (pendingChunkX << 4);
                double chunkMaxX = chunkMinX + 15.0;
                double chunkMinZ = (pendingChunkZ << 4);
                double chunkMaxZ = chunkMinZ + 15.0;

                double minX = Math.max(rxMinX, chunkMinX);
                double maxX = Math.min(rxMaxX, chunkMaxX);
                double minZ = Math.max(rzMinZ, chunkMinZ);
                double maxZ = Math.min(rzMaxZ, chunkMaxZ);

                pendingChunkX = Integer.MIN_VALUE;
                pendingChunkZ = Integer.MIN_VALUE;

                if (minX > maxX || minZ > maxZ) {
                    return null;
                }

                String tag = getCacheTypeTag();
                Predicate<Location> notExcluded = l -> isOutsideAny(l, exclude);

                // no need to re-check exclude here because validated() already enforced it
                return SafeLocationFinder.withYSelectionOverride(yov, () ->
                        cacheEnabled
                                ? SafeLocationFinder.cachedFindSafeInAreaValidated(
                                world, minX, maxX, rect.minY(), rect.maxY(), minZ, maxZ,
                                wl, playerId, cachePlayerSpecific, true, tag, notExcluded)
                                : SafeLocationFinder.attemptSafeInAreaOnce(world, minX, maxX, rect.minY(), rect.maxY(), minZ, maxZ, wl)
                );
            }

            // 2) Pick random chunk inside rect
            double x = ThreadLocalRandom.current().nextDouble(rxMinX, rxMaxX);
            double z = ThreadLocalRandom.current().nextDouble(rzMinZ, rzMaxZ);
            int cx = ((int) Math.floor(x)) >> 4;
            int cz = ((int) Math.floor(z)) >> 4;

            if (!world.isChunkLoaded(cx, cz)) {
                pendingChunkX = cx;
                pendingChunkZ = cz;
                waitingChunkLoad = true;

                double px = (cx << 4) + 8.0;
                double pz = (cz << 4) + 8.0;
                Location loadProbe = new Location(world, px, rect.minY(), pz);
                PaperLib.getChunkAtAsync(loadProbe, true).thenRun(() -> {});
                return null;
            }

            double chunkMinX = (cx << 4);
            double chunkMaxX = chunkMinX + 15.0;
            double chunkMinZ = (cz << 4);
            double chunkMaxZ = chunkMinZ + 15.0;

            double minX = Math.max(rxMinX, chunkMinX);
            double maxX = Math.min(rxMaxX, chunkMaxX);
            double minZ = Math.max(rzMinZ, chunkMinZ);
            double maxZ = Math.min(rzMaxZ, chunkMaxZ);

            if (minX > maxX || minZ > maxZ) {
                return null;
            }

            String tag = getCacheTypeTag();
            Predicate<Location> notExcluded = l -> isOutsideAny(l, exclude);

            return SafeLocationFinder.withYSelectionOverride(yov, () ->
                    cacheEnabled
                            ? SafeLocationFinder.cachedFindSafeInAreaValidated(
                            world, minX, maxX, rect.minY(), rect.maxY(), minZ, maxZ,
                            wl, playerId, cachePlayerSpecific, true, tag, notExcluded)
                            : SafeLocationFinder.attemptSafeInAreaOnce(world, minX, maxX, rect.minY(), rect.maxY(), minZ, maxZ, wl)
            );
        }

        private void bumpFailCounters() {
            if (!isDebug()) return;
            var tag = SafeLocationFinder.getAndClearLastFailReason();
            if (tag == null) return;
            switch (tag) {
                case FEET_NOT_PASSABLE -> failFeet++;
                case HEAD_NOT_PASSABLE -> failHead++;
                case GROUND_NOT_SOLID -> failGroundNotSolid++;
                case GROUND_BLACKLISTED -> failGroundBlacklisted++;
                case GROUND_NOT_WHITELISTED -> failGroundNotWhitelisted++;
            }
        }

        private void maybeExpandRadiusForNearSearch() {
            // Expand radius occasionally if we keep failing at fixed-point safe search
            int step = Math.max(2, plugin.getConfigManager().getMainConfig().settings.maxSafeLocationAttempts / 3);
            if ((attemptCount % step) == 0) {
                currentRadius = Math.min(
                        currentRadius * 2,
                        Math.max(currentRadius, plugin.getConfigManager().getMainConfig().settings.safeLocationRadius * 4)
                );
            }
        }

        private Rect pickRectClosestToChunkCenter(Location chunkCenter, List<Rect> rects) {
            if (rects.isEmpty()) return rectFromAxesOption(option, world);
            Rect best = rects.get(0);
            double bx = chunkCenter.getX();
            double bz = chunkCenter.getZ();
            double bestDist = Double.MAX_VALUE;

            for (Rect r : rects) {
                double cx = (Math.min(r.minX(), r.maxX()) + Math.max(r.minX(), r.maxX())) / 2.0;
                double cz = (Math.min(r.minZ(), r.maxZ()) + Math.max(r.minZ(), r.maxZ())) / 2.0;
                double d2 = (cx - bx) * (cx - bx) + (cz - bz) * (cz - bz);
                if (d2 < bestDist) {
                    bestDist = d2;
                    best = r;
                }
            }
            return best;
        }

        /**
         * Finalize job: cancel, remove from registry, teleport with min-stay logic, run AFTER actions.
         */
        private void finish(Location found, boolean success) {
            try { cancel(); } catch (Exception ignored) {}
            activeSafeSearchJobs.remove(playerId);

            if (!success || found == null) return;

            int delayConfig = plugin.getConfigManager().getMainConfig().settings.teleport.delayTicks;
            int minStayTicks = plugin.getConfigManager().getMainConfig().settings.waitingRoom.minStayTicks;
            long elapsedMs = System.currentTimeMillis() - waitingEnteredAtMs;
            long requiredMs = Math.max(0L, minStayTicks * 50L - elapsedMs);
            int requiredTicks = (int) Math.ceil(requiredMs / 50.0);
            int finalDelay = Math.max(1, Math.max(delayConfig, requiredTicks));

            Runnable afterTeleport = () ->
                    runPhaseForEntry(player, option, globalActions, SpawnPointsConfig.Phase.AFTER);

            teleportCore(player, found, eventType, finalDelay, afterTeleport);
        }

        private List<Rect> buildRectsForOption(SpawnPointsConfig.Destination option, World world) {
            // If rects are explicitly provided — use them
            List<Rect> rects = toRects(option.rects, world);
            if (!rects.isEmpty()) return rects;

            // If it's a fixed point — no rects (use fixed-point behavior)
            if (isPointOption(option)) {
                return Collections.emptyList();
            }

            // Otherwise build a single rect from axis (xyz)
            RegionSpec area = resolveAreaFromOption(option, world);
            return List.of(new Rect(area.minX(), area.maxX(), area.minY(), area.maxY(), area.minZ(), area.maxZ()));
        }

        private boolean isOutsideAny(Location loc, List<Rect> rects) {
            if (rects == null || rects.isEmpty() || loc == null) return true;
            for (Rect r : rects) {
                if (isInsideRect(loc, r)) return false;
            }
            return true;
        }
    }

    // ========== Helpers (outer scope) ==========

    private Rect rectFromAxesOption(SpawnPointsConfig.Destination option, World world) {
        RegionSpec area = resolveAreaFromOption(option, world);
        return new Rect(area.minX(), area.maxX(), area.minY(), area.maxY(), area.minZ(), area.maxZ());
    }

    private void applyYawPitch(SpawnPointsConfig.Destination option, Location loc) {
        float yaw = (option.yaw == null) ? loc.getYaw()
                : option.yaw.isValue() ? option.yaw.value.floatValue()
                : (float) (option.yaw.min + ThreadLocalRandom.current().nextDouble() * (option.yaw.max - option.yaw.min));
        float pitch = (option.pitch == null) ? loc.getPitch()
                : option.pitch.isValue() ? option.pitch.value.floatValue()
                : (float) (option.pitch.min + ThreadLocalRandom.current().nextDouble() * (option.pitch.max - option.pitch.min));
        pitch = (float) clampPitch(pitch);

        loc.setYaw(yaw);
        loc.setPitch(pitch);
    }

    private record Rect(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {}
    private record RegionSpec(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {}

    private List<Rect> toRects(List<SpawnPointsConfig.RectSpec> list, World world) {
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<Rect> out = new ArrayList<>();
        for (SpawnPointsConfig.RectSpec r : list) {
            if (r == null || r.x == null || r.z == null) continue;

            double minX = r.x.isValue() ? r.x.value : r.x.min;
            double maxX = r.x.isValue() ? r.x.value : r.x.max;
            double minZ = r.z.isValue() ? r.z.value : r.z.min;
            double maxZ = r.z.isValue() ? r.z.value : r.z.max;

            double minY;
            double maxY;
            if (r.y == null) {
                minY = resolveMinY(world);
                maxY = world.getMaxHeight();
            } else if (r.y.isValue()) {
                minY = r.y.value;
                maxY = r.y.value;
            } else {
                minY = r.y.min;
                maxY = r.y.max;
            }

            out.add(new Rect(minX, maxX, minY, maxY, minZ, maxZ));
        }
        return out;
    }

    private Rect pickRect(List<Rect> rects) {
        if (rects == null || rects.isEmpty()) return null;
        return rects.get(ThreadLocalRandom.current().nextInt(rects.size()));
    }

    private boolean isInsideRect(Location loc, Rect r) {
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        double minX = Math.min(r.minX(), r.maxX());
        double maxX = Math.max(r.minX(), r.maxX());
        double minZ = Math.min(r.minZ(), r.maxZ());
        double maxZ = Math.max(r.minZ(), r.maxZ());

        return x >= minX && x <= maxX
                && y >= Math.min(r.minY(), r.maxY()) && y <= Math.max(r.minY(), r.maxY())
                && z >= minZ && z <= maxZ;
    }

    private RegionSpec resolveAreaFromOption(SpawnPointsConfig.Destination option, World world) {
        double centerX = world.getWorldBorder().getCenter().getX();
        double centerZ = world.getWorldBorder().getCenter().getZ();

        double minX;
        double maxX;
        double minZ;
        double maxZ;

        if (option.x == null) {
            minX = centerX - 32.0; maxX = centerX + 32.0;
        } else if (option.x.isValue()) {
            minX = option.x.value; maxX = option.x.value;
        } else {
            minX = option.x.min; maxX = option.x.max;
        }

        if (option.z == null) {
            minZ = centerZ - 32.0; maxZ = centerZ + 32.0;
        } else if (option.z.isValue()) {
            minZ = option.z.value; maxZ = option.z.value;
        } else {
            minZ = option.z.min; maxZ = option.z.max;
        }

        double minY = resolveMinY(world);
        double maxY = world.getMaxHeight();
        if (option.y != null) {
            if (option.y.isValue()) {
                minY = option.y.value; maxY = option.y.value;
            } else {
                minY = option.y.min; maxY = option.y.max;
            }
        }

        return new RegionSpec(minX, maxX, minY, maxY, minZ, maxZ);
    }

    /**
     * Resolve a non-safe destination location (no SafeLocationFinder).
     * <p>
     * Runtime world==null check is intentional:
     * - Destinations may reference worlds that are not loaded/created yet.
     * - Instead of crashing, we treat such destinations as unusable and return null.
     */
    private Location resolveNonSafeLocation(SpawnPointsConfig.Destination option) {
        World world = Bukkit.getWorld(option.world);
        if (world == null) return null;

        // Always map legacy xyz axis to a single rect if rects are not provided
        List<Rect> rects = toRects(option.rects, world);
        if (rects.isEmpty()) {
            rects = List.of(rectFromAxesOption(option, world));
        }
        List<Rect> exclude = toRects(option.excludeRects, world);

        Rect r = pickRect(rects);
        if (r == null) return null;

        // Try up to 16 random samples
        double rxMinX = Math.min(r.minX(), r.maxX());
        double rxMaxX = Math.max(r.minX(), r.maxX());
        double rzMinZ = Math.min(r.minZ(), r.maxZ());
        double rzMaxZ = Math.max(r.minZ(), r.maxZ());

        for (int i = 0; i < 16; i++) {
            double x = (rxMinX == rxMaxX) ? rxMinX : ThreadLocalRandom.current().nextDouble(rxMinX, Math.nextUp(rxMaxX));
            double z = (rzMinZ == rzMaxZ) ? rzMinZ : ThreadLocalRandom.current().nextDouble(rzMinZ, Math.nextUp(rzMaxZ));

            double y;
            if (r.minY() == r.maxY()) {
                y = r.minY();
            } else {
                double dy = Math.max(1.0, (r.maxY() - r.minY()));
                y = r.minY() + ThreadLocalRandom.current().nextDouble(dy);
            }

            float yaw = computeYaw(option);
            float pitch = (float) clampPitch(computePitch(option));
            Location loc = new Location(world, x, y, z, yaw, pitch);

            if (!exclude.isEmpty()) {
                boolean insideEx = false;
                for (Rect ex : exclude) {
                    if (isInsideRect(loc, ex)) {
                        insideEx = true;
                        break; }
                }
                if (insideEx) continue;
            }
            return loc;
        }
        return null;
    }

    /**
     * Build a Material set from config names.
     * <p>
     * Runtime validation is intentional:
     * - The list is optional in config (may be null/empty).
     * - Unknown or legacy materials are silently skipped here, because:
     *   • config validators already report them on reload/startup;
     *   • at runtime we just want a safe, minimal set for fast lookups.
     */
    private Set<Material> toMaterialSet(List<String> names) {
        if (names == null || names.isEmpty()) return Collections.emptySet();
        Set<Material> set = new HashSet<>();
        for (String n : names) {
            Material m = Material.matchMaterial(n);
            if (m != null) {
                set.add(m);
            }
        }
        return set;
    }

    private float computeYaw(SpawnPointsConfig.Destination option) {
        if (option.yaw == null) return 0.0f;
        if (option.yaw.isValue()) return option.yaw.value.floatValue();
        double d = option.yaw.min + ThreadLocalRandom.current().nextDouble() * (option.yaw.max - option.yaw.min);
        return (float) d;
    }

    private float computePitch(SpawnPointsConfig.Destination option) {
        if (option.pitch == null) return 0.0f;
        if (option.pitch.isValue()) return option.pitch.value.floatValue();
        double d = option.pitch.min + ThreadLocalRandom.current().nextDouble() * (option.pitch.max - option.pitch.min);
        return (float) d;
    }

    private boolean isPointOption(SpawnPointsConfig.Destination option) {
        return option.x != null && option.z != null
                && option.x.isValue() && option.z.isValue()
                && (option.y == null || option.y.isValue());
    }

    private static String normalizeMode(String mode) {
        if (mode == null) return "set";
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "set", "add", "mul" -> mode.toLowerCase(Locale.ROOT);
            default -> "set";
        };
    }

    private static int clampChance(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private static int clampWeight(int v) {
        return Math.max(1, v);
    }

    private double clampPitch(double pitch) {
        if (pitch < -90.0) return -90.0;
        return Math.min(pitch, 90.0);
    }

    private SafeLocationFinder.YSelectionOverride buildYOverride(SpawnPointsConfig.Destination opt) {
        if (opt == null || opt.ySelection == null) return null;
        return new SafeLocationFinder.YSelectionOverride(
                opt.ySelection.mode, opt.ySelection.first, opt.ySelection.firstShare, opt.ySelection.respectRange
        );
    }

    // ========== Actions / commands / messaging ==========

    /**
     * Run actions with phase ordering relative to globalActions as per actionExecutionMode.
     */
    private void runPhaseForEntry(Player player,
                                  SpawnPointsConfig.Destination selected,
                                  SpawnPointsConfig.ActionsConfig globalActions,
                                  SpawnPointsConfig.Phase phase) {
        String mode = selected.actionExecutionMode == null
                ? "before"
                : selected.actionExecutionMode.toLowerCase(Locale.ROOT);

        switch (mode) {
            case "after" -> {
                runPhaseForActions(player, globalActions, phase);
                runPhaseForActions(player, selected.actions, phase);
            }
            case "instead" -> runPhaseForActions(player, selected.actions, phase);
            default -> {
                // treat null/unknown as "before"
                runPhaseForActions(player, selected.actions, phase);
                runPhaseForActions(player, globalActions, phase);
            }
        }
    }

    private void runPhaseForActions(Player player, SpawnPointsConfig.ActionsConfig actions, SpawnPointsConfig.Phase phase) {
        if (actions == null) return;

        if (actions.messages != null) {
            for (SpawnPointsConfig.MessageEntry msg : actions.messages) {
                if (!isPhase(msg.phases, phase)) continue;

                int chance = getEffectiveMessageChance(player, msg);
                if (roll(chance)) {
                    if (isDebug()) {
                        plugin.getLogger().info("runPhaseForActions: sending message to " + player.getName()
                                + " phase=" + phase + " text=" + msg.text);
                    }
                    plugin.getMessageManager().sendMessage(player, processPlaceholders(player, msg.text));
                } else if (isDebug()) {
                    plugin.getLogger().info("runPhaseForActions: skipped message due to chance for " + player.getName()
                            + " phase=" + phase + " text=" + msg.text);
                }
            }
        }

        if (actions.commands != null) {
            for (SpawnPointsConfig.CommandActionEntry cmd : actions.commands) {
                if (!isPhase(cmd.phases, phase)) continue;

                int chance = getEffectiveCommandChance(player, cmd);
                if (isDebug()) {
                    plugin.getLogger().info("runPhaseForActions: Checking command for " + player.getName()
                            + " with chance: " + chance + ", phase: " + phase);
                }

                if (roll(chance)) {
                    if (isDebug()) {
                        plugin.getLogger().info("runPhaseForActions: executing command for " + player.getName()
                                + " phase=" + phase + " command=" + cmd.command);
                    }
                    executeCommand(player, cmd.command);
                }  else if (isDebug()) {
                    plugin.getLogger().info("runPhaseForActions: skipped command due to chance for " + player.getName()
                            + " phase=" + phase + " command=" + cmd.command);
                }
            }
        }
    }

    private boolean isPhase(List<SpawnPointsConfig.Phase> phases, SpawnPointsConfig.Phase phase) {
        if (phases == null || phases.isEmpty()) {
            return phase == SpawnPointsConfig.Phase.AFTER;
        }
        return phases.contains(phase);
    }

    private int getEffectiveMessageChance(Player player, SpawnPointsConfig.MessageEntry message) {
        return applyChanceConditions(player, message.chance, message.chanceConditions);
    }

    private int getEffectiveCommandChance(Player player, SpawnPointsConfig.CommandActionEntry command) {
        return applyChanceConditions(player, command.chance, command.chanceConditions);
    }

    private void executeCommand(Player player, String command) {

        String safeName = SecurityUtils.sanitize(player.getName(), SecurityUtils.SanitizeType.PLAYER_NAME);
        String processedCommand = processPlaceholders(player, command.replace("%player%", safeName));

        plugin.getRunner().runGlobal(() -> {
            if (isDebug()) {
                plugin.getLogger().info("Executing command: " + processedCommand);
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
        });
    }

    private void teleportPlayerWithDelay(Player player, Location location, String eventType) {
        int delayTicks = plugin.getConfigManager().getMainConfig().settings.teleport.delayTicks;

        Runnable afterTeleport = () -> {
            // If this teleport was to WAITING ROOM (i.e., we have pending WR actions) — run them first
            if (runWaitingRoomPhaseIfPending(player)) {
                // WAITING_ROOM executed — AFTER for final destination will be run by SafeSearchJob.finish()
                return;
            }

            // Otherwise, non-waiting flow: run AFTER if pending
            PendingEntry pending = pendingAfterActions.remove(player.getUniqueId());
            if (pending != null) {
                runPhaseForEntry(player, pending.loc, pending.global, SpawnPointsConfig.Phase.AFTER);
                sendTeleportMessage(player, eventType);
            }
        };

        teleportCore(player, location, eventType, delayTicks, afterTeleport);
    }

    /**
     * Shared teleport core: PRE -> async teleport -> POST -> afterTeleport
     */
    private void teleportCore(Player player,
                              Location location,
                              String eventType,
                              int delayTicks,
                              Runnable afterTeleport) {

        int safeDelay = Math.max(0, delayTicks);

        Runnable task = () -> {
            if (!player.isOnline()) return;

            Location from = player.getLocation().clone();

            // PRE
            MSPPreTeleportEvent pre = new MSPPreTeleportEvent(
                    player, eventType, "FINAL", from, location.clone()
            );
            Bukkit.getPluginManager().callEvent(pre);
            if (pre.isCancelled()) return;

            Location to = pre.getTo();

            plugin.getRunner().teleportAsync(player, to).thenAccept(success -> {
                if (!Boolean.TRUE.equals(success)) return;

                plugin.getRunner().runAtEntity(player, () -> {
                    // POST
                    MSPPostTeleportEvent post = new MSPPostTeleportEvent(
                            player, eventType, "FINAL", from, to
                    );
                    Bukkit.getPluginManager().callEvent(post);

                    if (afterTeleport != null) {
                        afterTeleport.run();
                    }
                });
            });
        };

        if (safeDelay == 0) {
            plugin.getRunner().runAtEntity(player, task);
        } else {
            plugin.getRunner().runAtEntityLater(player, task, safeDelay);
        }
    }

    private void sendTeleportMessage(Player player, String eventType) {
        if (!"join".equalsIgnoreCase(eventType)) {
            return;
        }

        String message = plugin.getConfigManager().getMessagesConfig().join.teleportedOnJoin;
            plugin.getMessageManager().sendMessageKeyed(player, "join.teleportedOnJoin", message);
    }

    private String processPlaceholders(Player player, String text) {
        return plugin.isPlaceholderAPIEnabled()
                ? PlaceholderUtils.setPlaceholders(player, text)
                : text;
    }

    // ========== Misc utils ==========

    private boolean isDebug() {
        return plugin.getConfigManager().getMainConfig().settings.debugMode;
    }

    private String locationToString(Location loc) {
        if (loc == null) return "null";
        return "World: " + loc.getWorld().getName()
                + ", X: " + String.format("%.2f", loc.getX())
                + ", Y: " + String.format("%.2f", loc.getY())
                + ", Z: " + String.format("%.2f", loc.getZ());
    }

    /**
     * Waiting room resolution priority:
     * - local (destination.waitingRoom) > entry.waitingRoom > global settings.waitingRoom.
     * <p>
     * Runtime world==null fallback is intentional:
     * - If the configured waiting room world does not exist, we fall back to the
     *   default world spawn instead of throwing.
     */
    private Location getBestWaitingRoom(SpawnPointsConfig.WaitingRoomConfig local, SpawnPointsConfig.WaitingRoomConfig entry) {
        SpawnPointsConfig.WaitingRoomConfig target = (local != null) ? local : entry;
        if (target == null) target = plugin.getConfigManager().getMainConfig().settings.waitingRoom.location;

        World world = Bukkit.getWorld(target.world);
        if (world == null) {
            plugin.getLogger().warning("Waiting room world not found: " + target.world + " — falling back to default world spawn.");
            return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        return new Location(world, target.x, target.y, target.z, target.yaw, target.pitch);
    }

    /**
     * Runs AFTER phase if a non-waiting-room flow (requireSafe=false) set it pending,
     * and we used vanilla setRespawnLocation (no teleportPlayerWithDelay call).
     * Call this 1 tick after the actual respawn so the player is already at the new location.
     */
    public void runAfterPhaseIfPending(Player player, String eventType) {
        try {
            PendingEntry pending = pendingAfterActions.remove(player.getUniqueId());
            if (pending == null) {
                if (isDebug())
                    plugin.getLogger().info("runAfterPhaseIfPending: no pending entry for " + player.getName());
                return; // nothing to do
            }

            if (isDebug()) {
                plugin.getLogger().info("runAfterPhaseIfPending: running AFTER for "
                        + player.getName() + " event=" + eventType
                        + " destWorld=" + pending.loc.world);
            }

            // Execute AFTER for the resolved entry
            runPhaseForEntry(player, pending.loc, pending.global, SpawnPointsConfig.Phase.AFTER);

            // For join spawn-location flows, also send the join.teleportedOnJoin message
            if ("join".equalsIgnoreCase(eventType)) {
                sendTeleportMessage(player, "join");
            }
        } catch (Exception e) {
            if (isDebug()) {
                plugin.getLogger().warning("Error while running AFTER phase for " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private int applyChanceConditions(Player player,
                                      int baseChance,
                                      List<SpawnPointsConfig.ChanceConditionEntry> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return baseChance;
        }

        int chance = baseChance;
        boolean bypass = player.isOp() || player.hasPermission("*");

        for (SpawnPointsConfig.ChanceConditionEntry condition : conditions) {
            if (!matchesCondition(player, condition.type, condition.value, bypass)) {
                continue;
            }

            String mode = normalizeMode(condition.mode);
            switch (mode) {
                case "set" -> chance = clampChance(condition.weight);
                case "add" -> chance = clampChance(chance + condition.weight);
                case "mul" -> chance = clampChance((int) Math.round(chance * (double) condition.weight));
            }
        }

        return chance;
    }

    /**
     * Evaluate a single Weight/Chance condition (permission | placeholder).
     * Bypass flag applies only to permissions (OP / "*"), not to placeholders.
     */
    private boolean matchesCondition(Player player,
                                     String type,
                                     String value,
                                     boolean bypassPermissions) {
        if (type == null || value == null) return false;

        return switch (type.toLowerCase(Locale.ROOT)) {
            case "permission" ->
                    PlaceholderUtils.evaluatePermissionExpression(player, value, bypassPermissions);
            case "placeholder" ->
                    plugin.isPlaceholderAPIEnabled() && PlaceholderUtils.checkPlaceholderCondition(player, value);
            default -> false;
        };
    }

    private boolean roll(int chance) {
        return chance >= 100 || ThreadLocalRandom.current().nextInt(100) < chance;
    }
}
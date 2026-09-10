package uz.alex2276564.mmospawnpoint.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

@UtilityClass
public class WorldGuardUtils {

    private static Logger logger;
    private static Supplier<Boolean> debugSupplier = () -> false;

    /**
     * Configure logger and debug flag for WorldGuard-related diagnostics.
     * Call once from MMOSpawnPoint.onEnable().
     */
    public static void configure(Logger log, Supplier<Boolean> debug) {
        logger = log;
        debugSupplier = (debug != null) ? debug : () -> false;
    }

    public static Set<String> getRegionsAt(Location location) {
        Set<String> regions = new HashSet<>();

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));

            for (ProtectedRegion region : set) {
                regions.add(region.getId());
            }
        } catch (Exception e) {
            if (logger != null) {
                boolean debug = false;
                try {
                    debug = debugSupplier.get();
                } catch (Exception ignored) {
                    // Ignore errors while reading debug flag
                }

                if (debug) {
                    logger.log(
                            Level.WARNING,
                            "[MMOSpawnPoint] Failed to query WorldGuard regions at " + location,
                            e
                    );
                } else {
                    logger.warning(
                            "[MMOSpawnPoint] Failed to query WorldGuard regions at " + location
                                    + ": " + e.getClass().getSimpleName()
                                    + " (enable debugMode in config.yml to see full stack trace)"
                    );
                }
            }
        }

        return regions;
    }
}
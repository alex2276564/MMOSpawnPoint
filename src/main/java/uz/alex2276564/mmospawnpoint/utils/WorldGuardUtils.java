package uz.alex2276564.mmospawnpoint.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

@UtilityClass
public class WorldGuardUtils {

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
            MMOSpawnPoint plugin = MMOSpawnPoint.getInstance();
            if (plugin != null) {
                boolean debug = false;
                try {
                    debug = plugin.getConfigManager().getMainConfig().settings.debugMode;
                } catch (Exception ignored) {
                    // Ignore errors while reading debug flag
                }

                if (debug) {
                    plugin.getLogger().log(
                            Level.WARNING,
                            "[MMOSpawnPoint] Failed to query WorldGuard regions at " + location,
                            e
                    );
                } else {
                    plugin.getLogger().warning(
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
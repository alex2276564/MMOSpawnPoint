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
            e.printStackTrace();
        }

        return regions;
    }
}
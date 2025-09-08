package uz.alex2276564.mmospawnpoint.commands.subcommands.simulate;

import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SimulateContext {
    private static final Map<UUID, Location> PREV = new ConcurrentHashMap<>();

    private SimulateContext() {
    }

    public static void setPrev(UUID id, Location loc) {
        PREV.put(id, loc);
    }

    public static Location popPrev(UUID id) {
        return PREV.remove(id);
    }
}
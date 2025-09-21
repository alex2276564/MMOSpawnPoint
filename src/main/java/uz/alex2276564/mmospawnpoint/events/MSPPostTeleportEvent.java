package uz.alex2276564.mmospawnpoint.events;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after MSP completes a teleport (waiting room or final).
 * - eventType: "death" | "join"
 * - stage: "WAITING_ROOM" | "FINAL"
 * Purely informational (not cancellable).
 */
public class MSPPostTeleportEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    @Getter
    private final String eventType;
    @Getter
    private final String stage;
    @Getter
    private final Location from;
    @Getter
    private final Location to;

    public MSPPostTeleportEvent(@NotNull Player who,
                                @NotNull String eventType,
                                @NotNull String stage,
                                @NotNull Location from,
                                @NotNull Location to) {
        super(who);
        this.eventType = eventType;
        this.stage = stage;
        this.from = from;
        this.to = to;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
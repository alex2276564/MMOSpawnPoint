package uz.alex2276564.mmospawnpoint.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before MSP performs a teleport (waiting room or final).
 * - eventType: "death" | "join"
 * - stage: "WAITING_ROOM" | "FINAL"
 * Listeners can cancel or modify the target 'to' location.
 */
public class MSPPreTeleportEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    @Getter
    private final String eventType;
    @Getter
    private final String stage;

    @Getter
    @Setter
    private Location from;
    @Getter
    @Setter
    private Location to;

    private boolean cancelled = false;

    public MSPPreTeleportEvent(@NotNull Player who,
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
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
package dev.rds.rdsEvents.events;

import org.bukkit.scheduler.BukkitTask;

public final class ActiveEvent {
    private final EventDefinition definition;
    private final long startedAtMillis;
    private BukkitTask endTask;

    public ActiveEvent(EventDefinition definition) {
        this.definition = definition;
        this.startedAtMillis = System.currentTimeMillis();
    }

    public EventDefinition definition() {
        return definition;
    }

    public long startedAtMillis() {
        return startedAtMillis;
    }

    public void setEndTask(BukkitTask endTask) {
        this.endTask = endTask;
    }

    public void cancelEndTask() {
        if (endTask != null) {
            endTask.cancel();
            endTask = null;
        }
    }

    public long remainingSeconds() {
        long elapsed = (System.currentTimeMillis() - startedAtMillis) / 1000L;
        return Math.max(0L, definition.durationSeconds() - elapsed);
    }
}

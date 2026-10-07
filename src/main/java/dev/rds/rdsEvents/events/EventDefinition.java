package dev.rds.rdsEvents.events;

import java.util.List;

public record EventDefinition(
        String id,
        String displayName,
        long durationSeconds,
        String startMessage,
        String endMessage,
        List<String> startCommands,
        List<String> endCommands
) {
    public EventDefinition {
        durationSeconds = Math.max(0L, durationSeconds);
        startCommands = List.copyOf(startCommands);
        endCommands = List.copyOf(endCommands);
    }
}

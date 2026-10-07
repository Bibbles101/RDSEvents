package dev.rds.rdsEvents.events;

import dev.rds.rdsEvents.RDSEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EventManager {

    private final RDSEvents plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, EventDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, ActiveEvent> activeEvents = new LinkedHashMap<>();

    public EventManager(RDSEvents plugin) {
        this.plugin = plugin;
    }

    public void load() {
        definitions.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("events");
        if (section == null) {
            return;
        }

        for (String rawId : section.getKeys(false)) {
            ConfigurationSection event = section.getConfigurationSection(rawId);
            if (event == null) {
                continue;
            }

            String id = rawId.toLowerCase(Locale.ROOT);
            String displayName = event.getString("display-name", id);
            long duration = Math.max(0L, event.getLong("duration-seconds", 0L));
            String startMessage = event.getString("start-message", "");
            String endMessage = event.getString("end-message", "");
            List<String> startCommands = event.getStringList("start-commands");
            List<String> endCommands = event.getStringList("end-commands");

            definitions.put(id, new EventDefinition(
                    id, displayName, duration, startMessage, endMessage, startCommands, endCommands
            ));
        }
    }

    public Collection<EventDefinition> getDefinitions() {
        return List.copyOf(definitions.values());
    }

    public Collection<ActiveEvent> getActiveEvents() {
        return List.copyOf(activeEvents.values());
    }

    public EventDefinition getDefinition(String id) {
        return definitions.get(id.toLowerCase(Locale.ROOT));
    }

    public ActiveEvent getActive(String id) {
        return activeEvents.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean start(String id) {
        EventDefinition definition = getDefinition(id);
        if (definition == null || activeEvents.containsKey(definition.id())) {
            return false;
        }

        ActiveEvent active = new ActiveEvent(definition);
        activeEvents.put(definition.id(), active);

        broadcast(definition.startMessage());
        runCommands(definition.startCommands());

        if (plugin.getConfig().getBoolean("settings.announce-start-stop", true)
                && !definition.startMessage().isBlank()) {
            // The event message itself is already the announcement.
        }

        if (definition.durationSeconds() > 0) {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(
                    plugin,
                    () -> stop(definition.id(), true),
                    definition.durationSeconds() * 20L
            );
            active.setEndTask(task);
        }

        return true;
    }

    public boolean stop(String id, boolean announce) {
        ActiveEvent active = activeEvents.remove(id.toLowerCase(Locale.ROOT));
        if (active == null) {
            return false;
        }

        active.cancelEndTask();
        EventDefinition definition = active.definition();

        if (announce) {
            broadcast(definition.endMessage());
        }

        runCommands(definition.endCommands());
        return true;
    }

    public void stopAll(boolean announce) {
        for (String id : new ArrayList<>(activeEvents.keySet())) {
            stop(id, announce);
        }
    }

    public void reload() {
        stopAll(false);
        plugin.reloadConfig();
        load();
    }

    private void broadcast(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        Bukkit.getServer().broadcast(miniMessage.deserialize(message));
    }

    private void runCommands(List<String> commands) {
        for (String command : commands) {
            if (command == null || command.isBlank()) {
                continue;
            }
            String clean = command.startsWith("/") ? command.substring(1) : command;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), clean);
        }
    }
}

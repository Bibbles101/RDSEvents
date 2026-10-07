package dev.rds.rdsEvents;

import dev.rds.rdsEvents.command.EventCommand;
import dev.rds.rdsEvents.events.EventManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class RDSEvents extends JavaPlugin {

    private EventManager eventManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        eventManager = new EventManager(this);
        eventManager.load();

        EventCommand command = new EventCommand(this, eventManager);
        getCommand("rdsevent").setExecutor(command);
        getCommand("rdsevent").setTabCompleter(command);

        getLogger().info("RDSEvents enabled with " + eventManager.getDefinitions().size() + " configured event(s).");
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.stopAll(false);
        }
    }

    public EventManager getEventManager() {
        return eventManager;
    }
}

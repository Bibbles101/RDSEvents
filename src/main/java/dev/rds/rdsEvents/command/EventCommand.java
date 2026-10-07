package dev.rds.rdsEvents.command;

import dev.rds.rdsEvents.RDSEvents;
import dev.rds.rdsEvents.events.ActiveEvent;
import dev.rds.rdsEvents.events.EventDefinition;
import dev.rds.rdsEvents.events.EventManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class EventCommand implements TabExecutor {

    private final RDSEvents plugin;
    private final EventManager manager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public EventCommand(RDSEvents plugin, EventManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rdsevents.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            list(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /rdsevent start <id>");
                    return true;
                }

                EventDefinition definition = manager.getDefinition(args[1]);
                if (definition == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown event: " + args[1]);
                    return true;
                }

                if (!manager.start(definition.id())) {
                    sender.sendMessage(ChatColor.RED + "That event is already running.");
                    return true;
                }

                sender.sendMessage(ChatColor.GREEN + "Started " + definition.id() + ".");
            }
            case "stop" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /rdsevent stop <id>");
                    return true;
                }

                if (!manager.stop(args[1], true)) {
                    sender.sendMessage(ChatColor.RED + "That event isn't running.");
                    return true;
                }

                sender.sendMessage(ChatColor.GREEN + "Stopped " + args[1] + ".");
            }
            case "reload" -> {
                manager.reload();
                sender.sendMessage(ChatColor.GREEN + "RDSEvents reloaded.");
            }
            default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /rdsevent <list|start|stop|reload>");
        }

        return true;
    }

    private void list(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "RDSEvents");

        if (manager.getDefinitions().isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No events are configured.");
            return;
        }

        for (EventDefinition definition : manager.getDefinitions()) {
            ActiveEvent active = manager.getActive(definition.id());
            String state = active == null
                    ? ChatColor.GRAY + "inactive"
                    : ChatColor.GREEN + "active (" + formatRemaining(active.remainingSeconds()) + ")";
            sender.sendMessage(ChatColor.WHITE + " - " + definition.id() + ChatColor.GRAY + " [" + state + ChatColor.GRAY + "]");
        }
    }

    private String formatRemaining(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        long remainder = seconds % 60;
        return minutes + "m " + remainder + "s";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("rdsevents.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return partial(args[0], List.of("list", "start", "stop", "reload"));
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("stop"))) {
            List<String> ids = new ArrayList<>();
            if (args[0].equalsIgnoreCase("start")) {
                manager.getDefinitions().forEach(event -> {
                    if (manager.getActive(event.id()) == null) ids.add(event.id());
                });
            } else {
                manager.getActiveEvents().forEach(event -> ids.add(event.definition().id()));
            }
            return partial(args[1], ids);
        }

        return Collections.emptyList();
    }

    private List<String> partial(String input, List<String> values) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted()
                .toList();
    }
}

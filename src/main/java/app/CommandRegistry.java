package app;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.HashMap;
import java.util.Map;

import static app.Bot.log;

/*
    CommandRegistry stores all the custom ID prefixes, and delegates slashcommand, button, and modal
    interaction events passed to it from the listeners in Bot.java to the appropriate SlashCommand
 */

public class CommandRegistry {
    private static final Map<String, SlashCommandInterface> commands = new HashMap<>();

    public static void registerCommand(SlashCommandInterface command) {
        commands.put(command.getName(), command);
    }

    public static SlashCommandInterface getCommand(String name) {
        return commands.get(name);
    }

    public static void handleCommand(SlashCommandInteractionEvent event) {
        SlashCommandInterface cmd = commands.get(event.getName());
        if (cmd != null) {
            cmd.executeCommand(event);
        } else {
            log.error("Attempted to handleCommand, but couldn't find command {}", event.getName());
            event.reply("It seems this command has not been implemented yet").setEphemeral(true).queue();
        }
    }

    public static void handleModal(ModalInteractionEvent event) {
        SlashCommandInterface cmd = commands.get(event.getModalId().split(":")[0]);
        if (cmd != null) {
            cmd.executeModal(event);
        } else {
            log.error("Attempted to handleModal but couldn't find related command. Modal ID: {}", event.getId());
        }
    }

    public static void handleButton(ButtonInteractionEvent event) {
        SlashCommandInterface cmd = commands.get(event.getButton().getId().split(":")[0]);
        log.debug("Button with ID {} has been clicked", event.getButton().getId());
        if (cmd != null) {
            cmd.executeButton(event);
        } else {
            log.error("Attempted to handleButton but couldn't find related command. Button ID: {}", event.getId());
        }
    }

    public static void handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        SlashCommandInterface cmd = commands.get(event.getName());
        if (cmd != null) {
            cmd.executeAutoComplete(event);
        }
    }

    public static Map<String, SlashCommandInterface> getAllCommands() {
        return commands;
    }

}
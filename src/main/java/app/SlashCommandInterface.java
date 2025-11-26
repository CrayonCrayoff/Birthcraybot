package app;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public interface SlashCommandInterface {
    String getName();
    CommandData getCommandData();
    void executeCommand(SlashCommandInteractionEvent event);
    void executeModal(ModalInteractionEvent event);
    void executeButton(ButtonInteractionEvent event);
    void executeAutoComplete(CommandAutoCompleteInteractionEvent event);
}

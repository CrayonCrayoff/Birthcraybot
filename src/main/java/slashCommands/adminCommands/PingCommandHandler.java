package slashCommands.adminCommands;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import app.SlashCommandInterface;

public class PingCommandHandler implements SlashCommandInterface {
    @Override
    public String getName() {
        return "ping";
    };

    @Override
    public CommandData getCommandData() {
        return Commands.slash(this.getName(), "Ping the bot to check if it's live")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED);
    }

    @Override
    public void executeCommand(SlashCommandInteractionEvent event) {
        event.getHook().editOriginal("Pong!").queue();
    }

    @Override
    public void executeModal(ModalInteractionEvent event) {
        // this command doesn't use modals
    }

    @Override
    public void executeButton(ButtonInteractionEvent event) {
        // this command doesn't use buttons
    }

    @Override
    public void executeAutoComplete(CommandAutoCompleteInteractionEvent event) {
        // this command doesn't use autocomplete
    }
}

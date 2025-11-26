package slashCommands.userCommands;

import app.Database;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import app.SlashCommandInterface;

import java.sql.SQLException;

import static app.Bot.log;

public class RemoveBirthdayCommandHandler implements SlashCommandInterface {

    @Override
    public String getName() {
        return "remove-birthday";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(this.getName(), "Remove your birthday from the database")
                .setContexts(InteractionContextType.GUILD);
    }

    @Override
    public void executeCommand(SlashCommandInteractionEvent event) {
        event.getHook().sendMessage("""
                        Are you sure you want to remove your birthday?
                        
                        WARNING: THIS CANNOT BE UNDONE
                        
                        If you remove your birthday from ALL servers, and later want to set it again,
                        you'd have to do /set-birthday in each separate server.""")
                .setComponents(
                        ActionRow.of(
                                Button.of(ButtonStyle.SUCCESS,getName() + ":cancel","No, I changed my mind")
                        ),
                        ActionRow.of(
                                Button.of(ButtonStyle.DANGER,getName() + ":remove-this-guild","Yes, from this server"),
                                Button.of(ButtonStyle.DANGER,getName() + ":remove-all-guilds","Yes, from ALL servers")
                        )
                )
                .queue();
    }

    @Override
    public void executeModal(ModalInteractionEvent event) {
        // this command doesn't use modals
    }

    @Override
    public void executeButton(ButtonInteractionEvent event) {
        String buttonSuffix = event.getButton().getId().split(":")[1];
        log.debug("Button with suffix {} has been sent to RemoveBirthdayCommand.executeButton()", buttonSuffix);
        event.deferEdit().queue();
        switch (buttonSuffix) {
            case "cancel", "changed-mind":
                event.getHook().deleteOriginal().queue();
                break;
            case "remove-this-guild", "remove-all-guilds":
                askForConfirmation(event, buttonSuffix);
                break;
            case "confirm-this":
                removeBirthdayThisGuild(event);
                break;
            case "confirm-all":
                removeBirthdayAllGuilds(event);
                break;
            default:
                log.error("executeButton method in RemoveBirthdayCommand.java couldn't recognize buttonEvent");
                break;
        }
    }

    @Override
    public void executeAutoComplete(CommandAutoCompleteInteractionEvent event) {
        // this command doesn't use autocomplete
    }

    private void askForConfirmation(ButtonInteractionEvent event, String buttonSuffix) {
        String buttonTag = (buttonSuffix.equals("remove-this-guild")) ? ":confirm-this" : ":confirm-all";
        event.getHook()
                .editOriginal("Are you *REALLY* sure?")
                .setActionRow(
                        Button.of(
                                ButtonStyle.SUCCESS, getName() + ":changed-mind", "I changed my mind")
                        , Button.of(
                                ButtonStyle.DANGER, getName() + buttonTag, "Yes, I'm positive")
                ).queue();
    }

    private void removeBirthdayThisGuild(ButtonInteractionEvent event) {
        try {
            String guildId = event.getGuild().getId();
            String userId = event.getUser().getId();

            int rowsRemoved = Database.removeBirthdaySingleGuild(guildId, userId);
            if (rowsRemoved == 1) {
                event.getHook()
                        .editOriginal("Your birthday has been removed!")
                        .setComponents()
                        .queue();
            } else {
                event.getHook()
                        .editOriginal("You do not have a birthday registered to remove")
                        .setComponents()
                        .queue();
            }
        } catch (SQLException e) {
            event.getHook().editOriginal("Something went wrong.").queue();
        }
    }

    private void removeBirthdayAllGuilds(ButtonInteractionEvent event) {
        try {
            String userId = event.getUser().getId();

            int rowsRemoved = Database.removeBirthdayAllGuilds(userId);
            if (rowsRemoved >= 1) {
                event.getHook()
                        .editOriginal("Your birthday has been removed!")
                        .setComponents()
                        .queue();
            } else {
                event.getHook()
                        .editOriginal("Could not find a registered birthday to remove")
                        .setComponents()
                        .queue();
            }
        } catch (SQLException e) {
            event.getHook().editOriginal("Something went wrong.").queue();
        }
    }
}

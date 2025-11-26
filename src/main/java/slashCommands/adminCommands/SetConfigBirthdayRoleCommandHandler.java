package slashCommands.adminCommands;

import app.Database;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import app.SlashCommandInterface;

import java.sql.SQLException;
import java.util.List;

public class SetConfigBirthdayRoleCommandHandler implements SlashCommandInterface {
    @Override
    public String getName() {
        return "set-config-birthday-role";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(this.getName(), "Set a special birthday role given to people whose birthday it is")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                .addOption(
                        OptionType.STRING,
                        "birthday-role",
                        "Set the role given to people whose birthday it is",
                        true,
                        true
                );
    }

    @Override
    public void executeCommand(SlashCommandInteractionEvent event) {
        String roleId = event.getOption("birthday-role").getAsString();
        Role role;
        try {
            role = event.getGuild().getRoleById(roleId);
            if (role == null) throw new NullPointerException();
        } catch (NumberFormatException | NullPointerException e) {
            event.getHook().editOriginal("I don't recognize that role name/ID. Try again.").queue();
            return;
        }

        try{
            Database.setBirthdayRole(event.getGuild().getId(), roleId);
            event.getHook()
                    .editOriginal("Users whose birthday it is will be given the role: " + role.getName())
                    .queue();
        } catch (SQLException e) {
            event.getHook().editOriginal("Something went wrong.").queue();
        }
    }

    @Override
    public void executeModal(ModalInteractionEvent event) {

    }

    @Override
    public void executeButton(ButtonInteractionEvent event) {

    }

    @Override
    public void executeAutoComplete(CommandAutoCompleteInteractionEvent event) {
        // this command only has one argument field, other commands might need a switch statement
        List<Command.Choice> choices = event.getGuild().getRoles().stream()
                .filter(r -> r.getName().toLowerCase().contains(event.getFocusedOption().getValue().toLowerCase()))
                .limit(25)
                .map(r -> new Command.Choice(r.getName(), r.getId()))
                .toList();

        event.replyChoices(choices).queue();
    }
}

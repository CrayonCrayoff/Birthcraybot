package slashCommands.adminCommands;

import app.Database;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
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

public class SetConfigChannelCommandHandler implements SlashCommandInterface {
    @Override
    public String getName() {
        return "set-config-channel";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(this.getName(), "Set the shoutout channel")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                .addOption(
                        OptionType.STRING,
                        "channel",
                        "Set the channel where the shoutout will happen",
                        true,
                        true
                );
    }

    @Override
    public void executeCommand(SlashCommandInteractionEvent event) {
//        String guildId = event.getGuild().getId();
        String channelId = event.getOption("channel").getAsString();
        TextChannel channel;
        try {
            channel = event.getGuild().getTextChannelById(channelId);
            if (channel == null) throw new NullPointerException();
        } catch (NumberFormatException | NullPointerException e) {
            event.getHook().editOriginal("I don't recognize that channel name/ID. Try again.").queue();
            return;
        }

        try {
            Database.registerConfigChannel(event.getGuild().getId(), channelId);
            event.getHook()
                    .editOriginal("Birthcraybot shoutouts will happen in " + channel.getName())
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
        List<Command.Choice> choices = event.getGuild().getTextChannels().stream()
                .filter(c ->
                        c.canTalk() &&
                        c.getName().toLowerCase().contains(event.getFocusedOption().getValue().toLowerCase()))
                .limit(25)
                .map(c -> {
                    String categoryName = (c.getParentCategory() != null) ?
                            c.getParentCategory().getName() : "No category";
                    String displayName = String.format("Category %s: %s", categoryName, c.getName());
                    return new Command.Choice(displayName, c.getId());
                })
                .toList();

        event.replyChoices(choices).queue();
    }
}

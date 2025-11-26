package slashCommands.adminCommands;

import app.Database;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import app.SlashCommandInterface;

import java.sql.SQLException;

public class SetConfigMessagesCommandHandler implements SlashCommandInterface {
    @Override
    public String getName() {
        return "set-config-messages";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), "Set the shoutout messages")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED);
    }

    @Override
    public void executeCommand(SlashCommandInteractionEvent event) {
        event.getHook().sendMessage("""
                        Press the button to set custom shoutout messages.
                        Use `%s` to indicate where the usernames are supposed to go in the message.
                        If you don't include `%s`, usernames will be put at the end of the message.""")
                .addActionRow(Button.primary(getName() + ":setmessagebutton", "Register messages"))
                .queue();
    }

    @Override
    public void executeModal(ModalInteractionEvent event) {
        String singleMessage = formatMessage(
                event.getValue("singleBirthdayMessage").getAsString().strip(),
                event);
        String multipleMessage = formatMessage(
                event.getValue("multipleBirthdayMessage").getAsString().strip(),
                event);

        if (singleMessage.equals("invalid") || multipleMessage.equals("invalid")) {
            event.getHook()
                    .editOriginal("You should only put one \"%s\" in the messages")
                    .setComponents(ActionRow.of(Button.primary(getName() + ":setmessagebutton", "Try again")))
                    .queue();
            return;
        }
        try {
            Database.registerConfigMessages(event.getGuild().getId(), singleMessage, multipleMessage);
        } catch (SQLException e) {
            event.getHook().editOriginal("Something went wrong").queue();
            return;
        }

        String exampleSingle = String.format(singleMessage, "<username>");
        String exampleMultiple = String.format(multipleMessage, "<user1>, <user2>, <user3>");

        event.getHook()
                .editOriginal(String.format("""
                        Custom messages recorded! They will show up as:
                        %s%n%n%s""", exampleSingle, exampleMultiple))
                .setComponents()
                .queue();
    }

    @Override
    public void executeButton(ButtonInteractionEvent event) {
        TextInput singleMessage = TextInput.create(
                        "singleBirthdayMessage",
                        "Shoutout message for one birthday",
                        TextInputStyle.SHORT
                )
                .setPlaceholder("(Default) Happy birthday to %s!")
                .setRequired(false)
                .build();

        TextInput multipleMessage = TextInput.create(
                        "multipleBirthdayMessage",
                        "Shoutout message for multiple birthdays",
                        TextInputStyle.SHORT
                )
                .setPlaceholder("(Default) Happy birthday to %s!")
                .setRequired(false)
                .build();

        Modal modal = Modal.create(getName() + ":modal", "Register Or Edit Shoutout Messages")
                .addComponents(
                        ActionRow.of(singleMessage),
                        ActionRow.of(multipleMessage)
                )
                .build();
        event.replyModal(modal).queue();
    }

    @Override
    public void executeAutoComplete(CommandAutoCompleteInteractionEvent event) {

    }

    private String formatMessage(String message, ModalInteractionEvent event) {
        if (message.isEmpty()) {
            return "Happy birthday to %s!";
        }
        // count occurrences of placeholder %s
        int count = 0;
        int index = message.indexOf("%s");
        while (index != -1) {
            count++;
            index = message.indexOf("%s", index + 2);
        }
        if (count > 1) {  // invalid input
            return "invalid";
        } else if (count == 1) {  // no need to change
            return message;
        } else {  // add placeholder
            return message + " %s";
        }
    }
}

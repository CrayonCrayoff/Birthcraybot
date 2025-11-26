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
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import app.SlashCommandInterface;

import java.sql.SQLException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.util.*;

import static app.Bot.log;

public class SetBirthdayCommandHandler implements SlashCommandInterface {
    private final Map<String, Integer> validMonthEntries;
    private final String[] months = {
            "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december"
    };

    public SetBirthdayCommandHandler() {
        // define valid month entries
        validMonthEntries = new HashMap<>();
        for (int i = 0; i < months.length; i++) {
            validMonthEntries.put(months[i], i+1);
            validMonthEntries.put(months[i].substring(0, 3), i+1);
        }
    }

    @Override
    public String getName() {
        return "set-birthday";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(this.getName(), "Register or edit your birthday")
                .setContexts(InteractionContextType.GUILD);
    }

    @Override
    public void executeCommand(SlashCommandInteractionEvent event) {
        // first check if user birthday is already registered in this Guild
        String guildId = event.getGuild().getId();
        String userId = event.getUser().getId();
        try {
            String birthday = Database.retrieveBirthday(guildId, userId);
            // if no birthday registered: let user click button to go into entry UI
            if (birthday.isEmpty()) {
                event.getHook().sendMessage("Press the button to register your birthday")
                        .addActionRow(Button.primary(getName() + ":setbutton", "Register my birthday"))
                        .queue();
            } else {  // if false: birthday already registered. Let user click button to edit.
                LocalDate date = LocalDate.parse(birthday, DateTimeFormatter.ISO_DATE);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d");

                event.getHook().sendMessage(
                                "You already have a birthday registered. It's set to: " + date.format(formatter) +
                                        "\nWould you like to edit your registered birthday?")
                        .addActionRow(Button.primary(getName() + ":editbutton", "Edit my birthday"))
                        .queue();
            }
        } catch (SQLException e) {
            event.getHook().editOriginal("Something went wrong").queue();
        }
    }

    @Override
    public void executeModal(ModalInteractionEvent event) {
        String dayValue = event.getValue("day").getAsString().strip().toLowerCase();
        String monthValue = event.getValue("month").getAsString().strip().toLowerCase();

        LocalDate entryDate = convertToDate(dayValue, monthValue);

        if (entryDate == null) {
            event.getHook()
                    .sendMessage("""
                            Invalid day and/or month values.
                            Valid day format: numbers between 1-31 (or less depending on month)
                            Valid month format examples: "Jan", "January" or "1".
                            Use button to try again
                            """)
                    .addActionRow(Button.primary(getName() + ":setbutton", "Try again"))
                    .queue();
            return;
        }
        try {
            String guildId = event.getGuild().getId();
            String userId = event.getUser().getId();

            Database.registerBirthday(guildId, userId, entryDate);
//            String month = months[entryDate.getMonthValue()-1].charAt(0) +
//                    months[entryDate.getMonthValue()-1].substring(1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d");
            String successMessage = "Your birthday has been registered! " +
                    "(" + entryDate.format(formatter) + ")";
            event.getHook().editOriginal(successMessage).queue();
        } catch (SQLException e) {
            event.getHook().editOriginal("Something went wrong.").queue();
            log.error("SQL Exception attempting to store registered birthday: {}", String.valueOf(e));
        }
    }

    public void executeButton(ButtonInteractionEvent event) {
        showBirthdayRegisterModal(event);
    }

    private void showBirthdayRegisterModal(ButtonInteractionEvent event) {
        // Day input
        TextInput dayInput = TextInput.create("day", "Day", TextInputStyle.SHORT)
                .setPlaceholder("Enter day number (1-31)")
                .setRequired(true)
                .build();

        // Month input
        TextInput monthInput = TextInput.create("month", "Month", TextInputStyle.SHORT)
                .setPlaceholder("Enter month number or name (e.g., 1, Jan or January)")
                .setRequired(true)
                .build();

        event.getButton().getId();
        Modal modal = Modal.create(getName() + ":modal", "Register Or Edit Your Birthday")
                .addComponents(
                        ActionRow.of(dayInput),
                        ActionRow.of(monthInput)
                )
                .build();
        event.replyModal(modal).queue();
        event.getHook().deleteOriginal().queue();
    }

    private LocalDate convertToDate(String day, String month) {
        try {
            // parse day value
            int dayAsInt = Integer.parseInt(day);
            day = (dayAsInt < 10) ? "0" + dayAsInt : Integer.toString(dayAsInt);

            // parse month value
            int monthAsInt = validMonthEntries.getOrDefault(month, -1);
            if (monthAsInt == -1) monthAsInt = Integer.parseInt(month);
            month = (monthAsInt < 10) ? "0" + monthAsInt : Integer.toString(monthAsInt);

            return LocalDate.parse("2000-" + month + "-" + day, DateTimeFormatter.ISO_DATE);
        } catch (NumberFormatException e) {
            log.error("NumberFormatException occurred: {}", e.toString());
            return null;
        } catch (DateTimeParseException e) {
            log.error("DateTimeParseException occurred: {}", e.toString());
            return null;
        }
    }

    @Override
    public void executeAutoComplete(CommandAutoCompleteInteractionEvent event) {
        // this command doesn't use autocomplete
    }
}

package slashCommands.adminCommands;

import app.Database;
import app.SlashCommandInterface;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static app.Bot.log;

public class ExportBirthdayCommandHandler implements SlashCommandInterface {
    @Override
    public String getName() {
        return "export-birthdays";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(this.getName(), "Get all registered birthdays for your guild in a text file")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED);
    }

    @Override
    public void executeCommand(SlashCommandInteractionEvent event) {
        Map<String, String> birthdaysByUserIds = Database.retrieveBirthdaysForExport(event.getGuild().getId());

        if (birthdaysByUserIds.isEmpty()) {
            event.getHook()
                    .editOriginal("No birthdays found in the database")
                    .queue();
            return;
        }

        convertUserIds(birthdaysByUserIds, event);
    }

    @Override
    public void executeModal(ModalInteractionEvent event) {

    }

    @Override
    public void executeButton(ButtonInteractionEvent event) {

    }

    @Override
    public void executeAutoComplete(CommandAutoCompleteInteractionEvent event) {

    }

    private void convertUserIds(
            Map<String, String> birthdaysByUserIds, SlashCommandInteractionEvent event
    ) {
        AtomicInteger pendingUsers = new AtomicInteger(birthdaysByUserIds.size());
        Map<String, String> birthdaysByUsernames = new HashMap<>();
        Guild guild = event.getGuild();

        for (String userId : birthdaysByUserIds.keySet()) {
            guild.retrieveMemberById(userId).queue(
                    member -> {
                        birthdaysByUsernames.put(member.getEffectiveName(), birthdaysByUserIds.get(userId));
                        if (pendingUsers.decrementAndGet() == 0) {
                            makeAndSendExportFile(birthdaysByUsernames, event);
                        }
                    },
                    error -> {
                        birthdaysByUsernames.put("Unknown user with ID " + userId, birthdaysByUserIds.get(userId));
                        if (pendingUsers.decrementAndGet() == 0) {
                            makeAndSendExportFile(birthdaysByUsernames, event);
                        }
                    }
            );
        }
    }

    private void makeAndSendExportFile(Map<String, String> birthdaysByUserName, SlashCommandInteractionEvent event)  {
        StringBuilder sb = new StringBuilder();

        for (String username : birthdaysByUserName.keySet()) {
            sb.append("User: ").append(username).append(" | ")
                    .append("Birthday: ").append(birthdaysByUserName.get(username))
                    .append("\n");
        }

        File file;
        try {
            file = File.createTempFile("birthdays_", ".txt");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(sb.toString());
            }
        } catch (IOException e) {
            event.getHook().editOriginal("Something went wrong building the file.").queue();
            return;
        }

        event.getHook()
                .editOriginal("")
                .setFiles(FileUpload.fromData(file))
                .queue();

        try {
            file.delete();
        } catch (Exception e) {
            log.error("Failed to delete export file: ", e);
        }

    }

}

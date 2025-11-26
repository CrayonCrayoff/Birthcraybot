package slashCommands.adminCommands;

import app.Database;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import app.SlashCommandInterface;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;

import java.util.Map;

import static app.Bot.jda;
import static app.Bot.log;

public class CheckConfigsCommandHandler implements SlashCommandInterface {
    @Override
    public String getName() {
        return "check-configs";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(this.getName(), "See if Birthcraybot is set up correctly")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED);
    }

    @Override
    public void executeCommand(SlashCommandInteractionEvent event) {
        try {
            Map<String, String> configs = Database.retrieveConfig(event.getGuild().getId());
            String responseText = checkConfigsAndBuildMessage(configs);
            event.getHook().editOriginal(responseText).queue();
        } catch (Exception e) {
            log.error("Something went wrong retrieving the configs in CheckConfigsCommandHandler.", e);
            event.getHook().editOriginal("Something went wrong retrieving settings").queue();
        }
    }

    @Override
    public void executeModal(ModalInteractionEvent event) {

    }

    @Override
    public void executeButton(ButtonInteractionEvent event) {
        event.getHook().editOriginal("Sending test message...").queue();

    }

    @Override
    public void executeAutoComplete(CommandAutoCompleteInteractionEvent event) {

    }

    private String checkConfigsAndBuildMessage(Map<String, String> configs) {
        if (configs.isEmpty()) {
            return "No settings were found. You have to at least use the /set-config-channel once.";
        }

        StringBuilder configCheckMessage = new StringBuilder();
        configCheckMessage.append("These are the settings configurations as they're currently registered in the database:\n\n");
        configCheckMessage.append(buildChannelIdResponse(configs.get("channelId"))).append("\n\n");
        configCheckMessage.append(buildShoutoutMessagesResponse(
                configs.get("shoutoutMessageOne"),
                configs.get("shoutoutMessageMultiple")
            )
        ).append("\n\n");
        configCheckMessage.append(buildShoutoutRoleResponse(configs.get("shoutoutRoleId"))).append("\n\n");
        configCheckMessage.append(buildBirthdayRoleResponse(configs.get("birthdayRoleId")));

        return configCheckMessage.toString();
    }

    private String buildChannelIdResponse(String channelId) {
        StringBuilder channelIdResponse = new StringBuilder();
        channelIdResponse.append("* `Shoutout channel:`\n");
        if (channelId != null) {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null && channel.canTalk()) {
                channelIdResponse.append(
                        "> :white_check_mark: Shoutout channel is set up properly. Shoutouts will happen in: #"
                ).append(channel.getName());
            } else if (channel != null) {
                channelIdResponse.append("> :x: Shoutout channel is set to: ");
                channelIdResponse.append(channel.getName());
                channelIdResponse.append(", but Birthcraybot can't send messages to it. Check permissions");
            } else {
                channelIdResponse.append(
                        "> :x: Birthday channel ID found in the settings, but it doesn't seem to correspond to a valid channel"
                );
            }
        } else {
            channelIdResponse.append("""
                    > :x: No shoutout channel has been set.
                    > **IMPORTANT:** you **HAVE** to run /set-shoutout-channel at least once, or Birthcraybot ***CANNOT*** work properly."""
            );
        }

        return channelIdResponse.toString();
    }

    private String buildShoutoutMessagesResponse(String singleMessage, String multipleMessage) {
        StringBuilder shoutoutMessagesResponse = new StringBuilder();

        shoutoutMessagesResponse.append("* `Message for single birthday:`\n")
                .append("> :white_check_mark: ")
                .append(String.format(singleMessage, "<user mention>"))
                .append("\n");
        shoutoutMessagesResponse.append("* `Message for multiple birthdays:`\n")
                .append("> :white_check_mark: ")
                .append(String.format(multipleMessage, "<user1 mention>, <user2 mention>, and <user3 mention>"));

        return shoutoutMessagesResponse.toString();
    }

    private String buildShoutoutRoleResponse(String shoutoutRoleId) {
        StringBuilder shoutoutRoleResponse = new StringBuilder();
        shoutoutRoleResponse.append("* `Shoutout role`\n");

        if (shoutoutRoleId == null) {
             shoutoutRoleResponse.append("> :large_orange_diamond: Shoutout role is not set up. (This is optional, so that's okay)");
             return shoutoutRoleResponse.toString();
        }

        Role role = jda.getRoleById(shoutoutRoleId);
        if (role == null) {
            shoutoutRoleResponse.append("""
                    > :large_orange_diamond: Shoutout role has been set, but it doesn't seem to correspond to a valid role.
                    > (This is optional, so that's okay)"""
            );
        } else {
            shoutoutRoleResponse.append("> :white_check_mark: Shoutout role has been set up properly. Users with the role \"")
                    .append(role.getName())
                    .append("\" will be notified about other people's birthdays");
        }

        return shoutoutRoleResponse.toString();
    }

    private String buildBirthdayRoleResponse(String birthdayRoleId) {
        StringBuilder birthdayRoleResponse = new StringBuilder();
        birthdayRoleResponse.append("* `Birthday role`\n");
        if (birthdayRoleId == null) {
            birthdayRoleResponse.append("> :large_orange_diamond: Birthday role is not set up. (This is optional, so that's okay)");
            return birthdayRoleResponse.toString();
        }

        Role role = jda.getRoleById(birthdayRoleId);

        if (role == null) {
            birthdayRoleResponse.append("""
                    > :large_orange_diamond: Birthday role has been set, but it doesn't seem to correspond to a valid role.
                    > (This is optional, so that's okay)"""
            );
        } else if (!role.getGuild().getSelfMember().canInteract(role)) {
            birthdayRoleResponse.append("""
                    > :large_orange_diamond: Birthday role has been set, but Birthcraybot can't interact with it. Check role hierarchy.
                    > (This is optional, so that's okay)"""
            );
        } else {
            birthdayRoleResponse.append("> :white_check_mark: Birthday role has been set up properly. Users whose birthday it is will be given the \"")
                    .append(role.getName())
                    .append("\" role");
        }

        return birthdayRoleResponse.toString();
    }
}

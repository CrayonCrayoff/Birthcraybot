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

public class HelpAdminCommandHandler implements SlashCommandInterface {
    @Override
    public String getName() {
        return "help-admin";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(this.getName(), "How to use Birthcraybot (as an administrator)")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED);
    }

    @Override
    public void executeCommand(SlashCommandInteractionEvent event) {
        event.getHook().editOriginal(
                """
                        *Birthcraybot responds to all commands with messages only visible to you, so you don't have to worry about spamming a channel.*
                        
                        *When a user leaves your server for any reason (kicked, banned or voluntarily), Birthcraybot removes their birthday automatically.*
                        
                        __**The following commands are available to users with administrator privileges:**__
                        
                        * `/check-configs`
                        > Check how Birthcraybot is configured in your server. Will tell you if something is wrong.

                        * `/set-config-channel`
                        >  Lets you set up the shoutout channel for Birthcraybot.
                        >  **IMPORTANT:** you **HAVE** to run this command at least once, or Birthcraybot ***CANNOT*** work properly.
                        
                        * `/set-birthday-role`
                        > (Optional) Register a role that will be given to the people whose birthday it is.
                        > **IMPORTANT:** Make sure Birthcraybot is above the specified role in the Roles hierarchy.
                        > **IMPORTANT:** Birthcraybot uses this information to remove the role as well the day after. If you ever change this, you might need to manually remove the role from people who have been given it.
                        
                        * `/set-config-messages`
                        > (Optional) Lets you register the shoutout messages for when it's somebody's birthday. Use %s as a placeholder for where the usernames will go. If you don't include a placeholder, the user mentions will go at the end of the message by default.
                        
                        * `/set-shoutout-role`
                        > (Optional) Register a role that will be used to notify guild members about (other) people's birthdays. It will show at the beginning of the shoutout message.
                        
                        * `/export-birthdays`
                        >  This command returns all the registered birthdays for your server in a text file.
                        """
        ).queue();
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

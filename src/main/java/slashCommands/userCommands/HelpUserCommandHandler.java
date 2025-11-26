package slashCommands.userCommands;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import app.SlashCommandInterface;

public class HelpUserCommandHandler implements SlashCommandInterface {
    @Override
    public String getName() {
        return "help-user";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(this.getName(), "How to use Birthcraybot")
                .setContexts(InteractionContextType.GUILD);
    }

    @Override
    public void executeCommand(SlashCommandInteractionEvent event) {
        event.getHook().editOriginal(
                """
                        *Birthcraybot responds to all commands with messages only visible to you, so you don't have to worry about spamming a channel.*
                        
                        __**The following commands are available to all users:**__
                        
                        * `/help-user`
                        >  You're looking at it. :D
                        
                        * `/about-birthcraybot`
                        > A little tidbit about Birthcraybot and its developer
                        
                        * `/set-birthday`
                        >  This command lets you register your birthday.
                        >  If you already have a birthday registered, you can instead edit your birthday.
                        >  It will give you a button to press which opens a pop-up screen.
                        
                        * `/remove-birthday`
                        >  This command lets you remove your birthday from the database.
                        >  You can safely use this command, it will show you some buttons to confirm.
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
        // this comman doesn't use autocomplete
    }
}

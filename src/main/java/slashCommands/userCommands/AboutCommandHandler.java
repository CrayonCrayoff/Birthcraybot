package slashCommands.userCommands;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import app.SlashCommandInterface;

public class AboutCommandHandler implements SlashCommandInterface {
    @Override
    public String getName() {
        return "about-birthcraybot";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(this.getName(), "Some information about Birthcraybot and its developer")
                .setContexts(InteractionContextType.GUILD);
    }

    @Override
    public void executeCommand(SlashCommandInteractionEvent event) {
        event.getHook().editOriginal("""
                Hi! I'm Birthcraybot! I let users register their birthdays and shout them out on their special day!
                
                To learn how to use me, consider running the /help-user command if you're a general user, as well as the /help-admin command if you're a server administrator.
                
                I was built by CrayonCrayoff as a personal project. If you run into any bugs or other problems, you can send me a DM. I will forward it to my developer. Include screenshots if you can.
                
                I am completely free. No premium features or subscription model, what you see is what you get.
                However, if you appreciate the work I'm doing here, please consider going to Crayon's ko-fi and leaving a small tip. (https://ko-fi.com/crayoncrayoff)""")
                .queue();
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

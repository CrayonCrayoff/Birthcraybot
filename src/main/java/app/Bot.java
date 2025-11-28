package app;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.StatusChangeEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.UnavailableGuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import slashCommands.adminCommands.*;
import slashCommands.userCommands.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Bot extends ListenerAdapter {

    public static final Logger log = LoggerFactory.getLogger(Bot.class);
    private static User developer;
    public static JDA jda;

    public static void main(String[] args) throws InterruptedException {
        // ensure environment variables are present (throws IllegalStateException if not)
        String botToken = Config.require("BOT_TOKEN");

        // attempt to initialize database
        Database.initializeDB();

        // attempt to start the bot
        jda = JDABuilder.createDefault(botToken)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(new Bot())
                .build()
                .awaitReady(); // wait for the bot to fully connect before continuing

        // shutdown hook (no dangling connections in case of mishaps)
        Runtime.getRuntime().addShutdownHook(
                new Thread(
                        () -> {
                            jda.shutdown();
                            log.info("Gracefully shut down the bot. Have a nice day!");
                        }
                )
        );

        // store developer User object for forwarding DMs in OnMessageInteractionEvent()
        String developerId = Config.require("DEVELOPER_ID");
        jda.retrieveUserById(developerId).queue(user -> developer = user);

        // sync all commands to Discord
        syncCommandsToDiscord();

        // startup birthday shoutout check (in case of missed shoutouts because of reboot or other mishaps)
        // only shoutout immediately on boot if next noon UTC is tomorrow. Otherwise, just schedule for noon today.
        long nextDelay = BirthdayShoutoutHandler.secondsToNextNoonUTC();
        if (nextDelay > (12 * 60 * 60)) BirthdayShoutoutHandler.checkAndShoutout();
        BirthdayShoutoutHandler.startBirthdayCheckRoutine();
    }


    @Override
    public void onStatusChange(StatusChangeEvent event) {
        JDA.Status newState = event.getNewStatus();

        log.info("Status changed to {}", newState);

        if (newState == JDA.Status.DISCONNECTED || newState == JDA.Status.FAILED_TO_LOGIN) {
            log.error("Bot disconnected. Shutting it down gracefully.");
            System.exit(0);
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        int totalGuildCount = event.getGuildTotalCount();
        int guildAvailableCount = event.getGuildAvailableCount();
        int guildUnavailableCount = event.getGuildUnavailableCount();

        log.info(
                "Attempted to connect to {} guild(s). Successfully connected to {} guild(s). {}",
                totalGuildCount,
                guildAvailableCount,
                (guildUnavailableCount > 0 ? "Couldn't connect to {} guild(s)." : "")
        );

        registerAllCommands(); // store all commands in the command Registry for later lookup
    }

    // logic to respond to messages
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Message msg = event.getMessage();
        if (msg.getChannelType() != ChannelType.PRIVATE) return;  // only respond to DMs
        User author = msg.getAuthor();
        if (author.isBot()) return;  // bot should ignore bot messages

        // respond to person who DMed the bot
        event.getChannel().sendMessage(
                "I don't do anything in DMs myself, but I will attempt to forward your message to my developer.")
                .queue();

        // construct message with the text content of the message received
        String forwardMsg = String.format(
                "I received a message from %s. They wrote:%n%n %s",
                author.getGlobalName(),
                msg.getContentDisplay().isEmpty() ? "<no text>" : msg.getContentDisplay()
        );
        developer.openPrivateChannel().queue(devChannel -> {
            // send message content to developer
            devChannel.sendMessage(forwardMsg).queue();

            // attempt to send attached files, if any
            final long MAX_ATTACHMENT_SIZE = 25 * 1024 * 1024;  // Discord bot upload limit is apparently 25 MB
            List<String> bigFileNames = new ArrayList<>();
            for (Message.Attachment attachment : msg.getAttachments()) {
                if (attachment.getSize() > MAX_ATTACHMENT_SIZE) {
                    bigFileNames.add(attachment.getFileName());
                } else {
                    attachment.getProxy()
                            .download()
                            .thenAccept(fileData -> devChannel.sendFiles(FileUpload.fromData(
                                    fileData, attachment.getFileName()))
                            .queue());
                }
            }
            if (!bigFileNames.isEmpty()) {
                String fileMessage = "Attached file(s) " + bigFileNames + " too large to send";
                devChannel.sendMessage(fileMessage).queue();
                event.getChannel().sendMessage(fileMessage).queue();
            }
        });
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        // don't respond to slash commands in DMs
        if (event.getGuild() == null) {
            event.reply("I only respond to slash commands in servers, sorry!").queue();
            return;
        }
        event.deferReply(true).queue();
        CommandRegistry.handleCommand(event);
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        event.deferReply(true).queue();
        CommandRegistry.handleModal(event);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        CommandRegistry.handleButton(event);
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        CommandRegistry.handleAutoComplete(event);
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        Database.purgeOnBotLeave(event.getGuild().getId());
    }

    @Override
    public void onUnavailableGuildLeave(@NotNull UnavailableGuildLeaveEvent event) {
        Database.purgeOnBotLeave(event.getGuildId());
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        String guildId = event.getGuild().getId();
        String userId = event.getUser().getId();
        try {
            Database.removeBirthdaySingleGuild(guildId, userId);
        } catch (SQLException e) {
            log.error("Something went wrong purging on GuildMemberRemoveEvent", e);
        }

    }

    private static void registerAllCommands() {
        // user commands
        CommandRegistry.registerCommand(new AboutCommandHandler());
        CommandRegistry.registerCommand(new HelpUserCommandHandler());
        CommandRegistry.registerCommand(new RemoveBirthdayCommandHandler());
        CommandRegistry.registerCommand(new SetBirthdayCommandHandler());

        // admin commands
        CommandRegistry.registerCommand(new CheckConfigsCommandHandler());
        CommandRegistry.registerCommand(new ExportBirthdayCommandHandler());
        CommandRegistry.registerCommand(new HelpAdminCommandHandler());
        CommandRegistry.registerCommand(new PingCommandHandler());
        CommandRegistry.registerCommand(new SetConfigBirthdayRoleCommandHandler());
        CommandRegistry.registerCommand(new SetConfigChannelCommandHandler());
        CommandRegistry.registerCommand(new SetConfigMessagesCommandHandler());
        CommandRegistry.registerCommand(new SetConfigShoutoutRoleCommandHandler());
    }

    private static void syncCommandsToDiscord() {
        // ensure mode variable is available in environment
        String mode = Config.require("MODE");
        CommandListUpdateAction commands;
        Guild testGuild;
        if ("DEV".equals(mode)) {
            // ensure test guild ID variable is present in environment
            String testGuildId = Config.require("TEST_GUILD_ID");
            testGuild = jda.getGuildById(testGuildId);
            if (testGuild == null) {
                throw new IllegalStateException("Test Guild not found");
            }
            // if in DEV mode, prepare to only sync commands to the test guild
            commands = testGuild.updateCommands();
        } else {
            // if in PROD mode, prepare to sync commands globally
            commands = jda.updateCommands();
        }

        // collect all commands in the CommandRegistry and sync them
        commands.addCommands(
                CommandRegistry
                        .getAllCommands()
                        .values()
                        .stream()
                        .map(SlashCommandInterface::getCommandData)
                        .toList()
        ).queue();
        log.info("Synced all slash commands!");
    }
}

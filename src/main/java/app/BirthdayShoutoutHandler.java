package app;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static app.Bot.jda;
import static app.Bot.log;

public class BirthdayShoutoutHandler {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void startBirthdayCheckRoutine() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    checkAndShoutout();
                } catch (Exception e) {
                    log.error("Error during birthday check: ", e);
                } finally {
                    long nextDelay = secondsToNextNoonUTC();
                    if (nextDelay <= 1) {
                        nextDelay = 24 * 60 * 60; // schedule for 24 hours from now if too close to noon
                        log.info("nextDelay calculated too close to noon, set to 24 hours");
                    }
                    log.info("nextDelay calculated at {} seconds", nextDelay);
                    scheduler.schedule(this, nextDelay, TimeUnit.SECONDS);
                }
            }
        };
        long initialDelay = secondsToNextNoonUTC();
        log.info("initialDelay calculated at {} seconds", initialDelay);
        scheduler.schedule(task, initialDelay, TimeUnit.SECONDS);
    }

    public static long secondsToNextNoonUTC() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime nextNoon = now.withHour(12).withMinute(0).withSecond(0).withNano(0);
        if (!nextNoon.isAfter(now)) nextNoon = nextNoon.plusDays(1);
        return Duration.between(Instant.now(), nextNoon.toInstant()).toSeconds();
    }

    public static void checkAndShoutout() {
        try {
            Map<String, List<String>> usersByGuild = Database.retrieveTodaysBirthdays();
            log.info("Called checkAndShoutout(), usersByGuild = {}", usersByGuild);
            if (usersByGuild.isEmpty()) {
                collectPreviousBirthdayMembers(membersByRole -> {
                    removeBirthdayRole(membersByRole, () -> {});
                });
                return;
            }

            // chain lambdas together to ensure each async call finishes on discord's side before calling the next
            collectPreviousBirthdayMembers(membersByRole -> {
                removeBirthdayRole(membersByRole, () -> {
                    for (String guildId : usersByGuild.keySet()) {
                        try {
                            Map<String, String> configs = Database.retrieveConfig(guildId);
                            List<String> userIds = usersByGuild.get(guildId);

                            sendShoutout(configs, userIds, () -> {
                                assignBirthdayRole(configs, guildId, userIds);
                            });
                        } catch (Exception e) {
                            log.error("Something went wrong fetching configs for Guild ID {} in checkAndShoutout", guildId);
                        }
                    }
                });
            });
            Database.updateLastShoutout(usersByGuild);
        } catch (Exception e) {
            log.error("Something went wrong in checkAndShoutout: ", e);
        }
    }

    private static void collectPreviousBirthdayMembers(Consumer<Map<Role, List<Member>>> onDone) {
        log.info("collectPreviousBirthdayMembers method called");
        Map<Role, List<Member>> pendingRoleRemovals = new HashMap<>();
        Map<String, String> birthdayRoleIdsPerGuild = Database.retrieveGuildAndBirthdayRoleIds();

        if (birthdayRoleIdsPerGuild.isEmpty()) {
            log.info("No birthday role IDs found in the database's shoutout config table");
            onDone.accept(pendingRoleRemovals); // move to next method anyway, passing an empty HashMap
            return;
        }

        Map<String, Set<String>> userIdsToExcludeByGuildId = Database.retrieveUserIdsToExclude();
        AtomicInteger pendingGuildsToCheck = new AtomicInteger(birthdayRoleIdsPerGuild.size());

        for (String guildId : birthdayRoleIdsPerGuild.keySet()) {
            String roleId = birthdayRoleIdsPerGuild.get(guildId);
            Guild guild = jda.getGuildById(guildId);
            Role birthdayRole = jda.getRoleById(roleId);
            Set<String> userIdsToExclude = userIdsToExcludeByGuildId.getOrDefault(guildId, new HashSet<>());

            if (guild == null || birthdayRole == null) {
                if (pendingGuildsToCheck.decrementAndGet() == 0) {
                    onDone.accept(pendingRoleRemovals);
                }
                continue;
            }

            guild.findMembersWithRoles(birthdayRole)
                    .onSuccess(members -> {
                        if (members != null && !members.isEmpty()) {
                            // don't add members whose birthday is today and have already been shouted out
                            log.info("Members before filtering: {}", members);
                            members = members.stream().filter(m -> !userIdsToExclude.contains(m.getId())).toList();
                            log.info("Members after filtering: {}", members);
                            pendingRoleRemovals.put(birthdayRole, members);
                        }
                        if (pendingGuildsToCheck.decrementAndGet() == 0) {
                            log.info("Finished collecting previous birthday Members");
                            onDone.accept(pendingRoleRemovals);
                        }
                    })
                    .onError(error -> {
                        log.error("Something went wrong collecting birthday Members {}", guildId, error);
                        if (pendingGuildsToCheck.decrementAndGet() == 0) {
                            onDone.accept(pendingRoleRemovals);
                        }
                    });
        }
    }

    private static void removeBirthdayRole(Map<Role, List<Member>> membersByRole, Runnable onDone) {
        log.info("removeBirthdayRole() called");

        AtomicInteger pendingRoleRemovalCount = new AtomicInteger(0);

        for (List<Member> members : membersByRole.values()) {
            pendingRoleRemovalCount.getAndAdd(members.size());
        }

        if (pendingRoleRemovalCount.intValue() == 0) {
            log.info("No Members found that had the birthday role");
            onDone.run();
        }

        for (Role role : membersByRole.keySet()) {
            Guild guild = role.getGuild();
            List<Member> members = membersByRole.get(role);

            for (Member m : members) {
                guild.removeRoleFromMember(m, role).queue(
                        success -> {
                            if (pendingRoleRemovalCount.decrementAndGet() == 0) {
                                log.info("Finished removing birthday roles from Members");
                                onDone.run();
                            }
                        },
                        error -> {
                            if (pendingRoleRemovalCount.decrementAndGet() == 0) {
                                onDone.run();
                            }
                        });
            }
        }
    }

    private static void sendShoutout(Map<String, String> configs, List<String> userIds, Runnable onDone) {
        try {
            if (configs == null || configs.isEmpty()) {
                log.info("No configs found");
                onDone.run(); // move to next method anyway
                return;
            }

            String channelId = configs.get("channelId");
            if (channelId == null || channelId.isEmpty()) {
                log.info("in sendShoutout(), channelId was null");
                onDone.run(); // move to next method anyway
                return;
            }

            String shoutoutRoleId = configs.get("shoutoutRoleId");
            String messageFormatOne = configs.get("shoutoutMessageOne");
            String messageFormatMultiple = configs.get("shoutoutMessageMultiple");
            String message = buildShoutoutMessage(shoutoutRoleId, messageFormatOne, messageFormatMultiple, userIds);
            TextChannel channel = Bot.jda.getTextChannelById(channelId);
            if (channel == null) {
                log.error("For shoutout, unable to find channel with ID {}", channelId);
            } else if (!channel.canTalk()) {
                log.error("Unable to send message to channel {} with ID {}", channel.getName(), channelId);
            } else {
                channel.sendMessage(message).queue();
            }
            onDone.run();
        } catch (Exception e) {
            log.error("Something went wrong trying to sendShoutout(): ", e);
        }
    }

    private static String buildShoutoutMessage(
            String shoutoutRoleId, String messageFormatOne, String messageFormatMultiple, List<String> userIds
    ) {
            StringBuilder shoutoutMessage = new StringBuilder();

            if (shoutoutRoleId != null) {
                String shoutoutRolePing = "<@&" + shoutoutRoleId + "> ";
                shoutoutMessage.append(shoutoutRolePing);
            }

            String mentionFormat = "<@%s>";

            if (userIds.size() == 1) {
                String mention = String.format(mentionFormat, userIds.get(0));
                if (!messageFormatOne.contains("%s")) messageFormatOne += " %s";
                shoutoutMessage.append(String.format(messageFormatOne, mention));
            } else {
                StringBuilder userMentions = new StringBuilder();
                for (int i = 0; i < userIds.size() - 1; i++) {  // userIds except for last one
                    String mention = String.format(mentionFormat, userIds.get(i));
                    userMentions.append(mention).append(", ");
                }
                userMentions.append("and ").append(String.format(mentionFormat, userIds.get(userIds.size()-1)));
                if (!messageFormatMultiple.contains("%s")) messageFormatMultiple += " %s";
                shoutoutMessage.append(String.format(messageFormatMultiple, userMentions));
            }

            return shoutoutMessage.toString();
    }

    private static void assignBirthdayRole(Map<String, String> configs, String guildId, List<String> userIds) {
        log.info("assignBirthdayRole() called");
        Guild guild = jda.getGuildById(guildId);
        String roleId = configs.get("birthdayRoleId");
        if (guild == null || roleId == null) {
            log.info("Guild was null or roleId was null");
            return;
        }

        Role birthdayRole = jda.getRoleById(configs.get("birthdayRoleId"));
        if (birthdayRole == null) {
            log.info("birthdayRole in assignBirthdayRole() was null");
            return;
        }

        for (String id : userIds) {
            guild.retrieveMemberById(id).queue(member -> {
                log.info("Retrieved member {}, adding role {}", member.getId(), birthdayRole.getId());
                try {
                    guild.addRoleToMember(member, birthdayRole).queue(
                            success -> log.info("Role added successfully"),
                            error -> log.error("Failed to add role", error)
                    );
                } catch (Exception e) {
                    log.error("Something went wrong assigning the birthday role: ", e);
                }
            }, error -> {
                log.error("Failed to retrieve member {}", id, error);
            });
        }
    }
}
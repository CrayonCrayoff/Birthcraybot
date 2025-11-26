package app;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {
    private static final String DB_PATH = "data/birthcray.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;
    private static final Logger log = LoggerFactory.getLogger(Database.class);

    static {
        File dataFolder = new File("data");
        if (!dataFolder.exists()) {
            boolean createdFolder = dataFolder.mkdir();
            if (createdFolder) {
                log.info("Data folder wasn't present. Created it.");
            } else {
                throw new IllegalStateException("Data folder wasn't present and failed to create it.");
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDB() {
        String createTableStatement = """
                CREATE TABLE IF NOT EXISTS birthdays (
                    guild_id TEXT NOT NULL,
                    user_id TEXT NOT NULL,
                    birthday TEXT NOT NULL,
                    last_shoutout TEXT NOT NULL,
                    PRIMARY KEY (guild_id, user_id)
                );
                """;

        try (Connection conn = getConnection()) {
            conn.createStatement().execute(createTableStatement);
        } catch (SQLException e) {
            log.error("Failed to create birthdays table. Exception: {}", String.valueOf(e));
        }

        createTableStatement = """
                CREATE TABLE IF NOT EXISTS shoutoutconfig (
                    guild_id TEXT NOT NULL,
                    channel_id TEXT,
                    shoutout_message_one TEXT DEFAULT 'Happy birthday to: %s!',
                    shoutout_message_multiple TEXT DEFAULT 'Happy birthday to: %s!',
                    shoutout_role_id TEXT,
                    birthday_role_id TEXT,
                    PRIMARY KEY (guild_id)
                );
                """;

        try (Connection conn = getConnection()) {
            conn.createStatement().execute(createTableStatement);
            log.info("Using database file at: {}", new File(DB_PATH).getAbsolutePath());
        } catch (SQLException e) {
            log.error("Failed to create shoutoutconfig table. Exception: {}", String.valueOf(e));
        }
    }

    public static void registerBirthday(String guildId, String userId, LocalDate birthday) throws SQLException {
        String statement = """
                INSERT INTO birthdays (guild_id, user_id, birthday, last_shoutout)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (guild_id, user_id)
                DO UPDATE SET
                    birthday = excluded.birthday,
                    last_shoutout = excluded.last_shoutout""";

        try (Connection conn = getConnection()) {
            PreparedStatement updateStatement = conn.prepareStatement(statement);
            updateStatement.setString(1, guildId);
            updateStatement.setString(2, userId);
            updateStatement.setString(3, birthday.toString());
            updateStatement.setString(4, birthday.toString());

            int rowsAltered = updateStatement.executeUpdate();

            if (rowsAltered != 1) {  // just in case
                log.error("registerBirthday in Database.java didn't alter exactly 1 line, but {}", rowsAltered);
                throw new SQLException("registerBirthday in Database.java altered 0 or more than 1 line.");
            }
        } catch (SQLException e) {
            log.error("Failed to insert/update birthday data. Error: {}", String.valueOf(e));
            throw e;
        }
    }

    public static void registerConfigChannel(String guildId, String channelId) throws SQLException {
        String statement = """
                INSERT INTO shoutoutconfig (guild_id, channel_id)
                VALUES (?, ?)
                ON CONFLICT (guild_id)
                DO UPDATE SET channel_id = excluded.channel_id""";

        try (Connection conn = getConnection()) {
            log.info("Attempting to upsert channelID {} for guildID {}", channelId, guildId);
            PreparedStatement updateStatement = conn.prepareStatement(statement);
            updateStatement.setString(1, guildId);
            updateStatement.setString(2, channelId);

            int rowsAltered = updateStatement.executeUpdate();

            if (rowsAltered != 1) {  // just in case
                log.error("registerConfigChannel in Database.java didn't alter exactly 1 line, but {}", rowsAltered);
                throw new SQLException("registerConfigChannel in Database.java altered 0 or more than 1 line.");
            }
        } catch (SQLException e) {
            log.error("Failed to upsert config data. Error: {}", String.valueOf(e));
            throw e;
        }
    }

    public static void registerConfigMessages(
            String guildId, String singleMessage, String multipleMessage
    ) throws SQLException {
        String statement = """
                INSERT INTO shoutoutconfig (guild_id, shoutout_message_one, shoutout_message_multiple)
                VALUES (?, ?, ?)
                ON CONFLICT (guild_id)
                DO UPDATE SET
                shoutout_message_one = excluded.shoutout_message_one,
                shoutout_message_multiple = excluded.shoutout_message_multiple""";

        try (Connection conn = getConnection()) {
            log.info("Attempting to upsert messages {} and {} for guildID {}", singleMessage, multipleMessage, guildId);
            PreparedStatement updateStatement = conn.prepareStatement(statement);
            updateStatement.setString(1, guildId);
            updateStatement.setString(2, singleMessage);
            updateStatement.setString(3, multipleMessage);

            int rowsAltered = updateStatement.executeUpdate();

            if (rowsAltered != 1) {  // just in case
                log.error("registerConfigMessages in Database.java didn't alter exactly 1 line, but {}", rowsAltered);
                throw new SQLException("registerConfigMessages in Database.java altered 0 or more than 1 line.");
            }
        } catch (SQLException e) {
            log.error("Failed to insert/update config messages data. Error: {}", String.valueOf(e));
            throw e;
        }
    }

    public static String retrieveBirthday(String guildId, String userId) throws SQLException {
        String statement = """
                SELECT birthday FROM birthdays
                WHERE guild_id = ? AND user_id = ?
                """;
        try (Connection conn = getConnection()) {
            PreparedStatement queryStatement = conn.prepareStatement(statement);
            queryStatement.setString(1, guildId);
            queryStatement.setString(2, userId);

            try (ResultSet res = queryStatement.executeQuery()) {
                if (res.next()) {
                    return res.getString("birthday");
                } else {
                    return "";
                }
            }
        } catch (SQLException e) {
            log.error("Failed to fetch birthday data. Error: {}", String.valueOf(e));
            throw e;
        }
    }

    public static int removeBirthdaySingleGuild(String guildId, String userId) throws SQLException {
        String statement = """
                DELETE FROM birthdays
                WHERE guild_id = ? AND user_id = ?
                """;
        try (Connection conn = getConnection()) {
            PreparedStatement deleteStatement = conn.prepareStatement(statement);
            deleteStatement.setString(1, guildId);
            deleteStatement.setString(2, userId);

            int rowsRemoved = deleteStatement.executeUpdate();

            if (rowsRemoved > 1) {  // just in case
                log.error("deleteStatement in removeBirthday altered {} lines instead of 0 or 1", rowsRemoved);
                throw new SQLException("deleteStatement altered more than 1 line");
            }
            return rowsRemoved;
        } catch (SQLException e) {
            log.error("Exception while deleting birthday data from single Guild. Error: ", e);
            throw e;
        }
    }

    public static int removeBirthdayAllGuilds(String userId) throws SQLException {
        String statement = """
                DELETE FROM birthdays
                WHERE user_id = ?
                """;
        try (Connection conn = getConnection()) {
            PreparedStatement deleteStatement = conn.prepareStatement(statement);
            deleteStatement.setString(1, userId);
            return deleteStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Exception while deleting birthday data from all Guilds. Error: {}", String.valueOf(e));
            throw e;
        }
    }

    public static Map<String, List<String>> retrieveTodaysBirthdays() throws SQLException {
        String statement = """
                SELECT guild_id, user_id, birthday
                FROM birthdays
                WHERE strftime('%m-%d', birthday) = strftime('%m-%d', 'now')
                AND strftime('%Y-%m-%d', last_shoutout) <> strftime('%Y-%m-%d', 'now');
                """;

        Map<String, List<String>> usersByGuild = new HashMap<>();

        try (Connection conn = getConnection()) {
            PreparedStatement queryStatement = conn.prepareStatement(statement);
            ResultSet res = queryStatement.executeQuery();

            while (res.next()) {
                String guildId = res.getString("guild_id");
                String userId = res.getString("user_id");
                if (usersByGuild.containsKey(guildId)) {
                    usersByGuild.get(guildId).add(userId);
                } else {
                    usersByGuild.put(guildId, new ArrayList<>());
                    usersByGuild.get(guildId).add(userId);
                }
            }
            return usersByGuild;

        } catch (SQLException e) {
            log.error("Failed to fetch today's birthday data. Error: {}", String.valueOf(e));
            throw e;
        }
    }

    public static Map<String, Set<String>> retrieveUserIdsToExclude() {
        String statement = """
                SELECT guild_id, user_id
                FROM birthdays
                WHERE strftime('%m-%d', birthday) = strftime('%m-%d', 'now')
                AND strftime('%Y-%m-%d', last_shoutout) = strftime('%Y-%m-%d', 'now');
                """;

        Map<String, Set<String>> usersByGuild = new HashMap<>();

        try (Connection conn = getConnection()) {
            PreparedStatement queryStatement = conn.prepareStatement(statement);
            ResultSet res = queryStatement.executeQuery();

            while (res.next()) {
                String guildId = res.getString("guild_id");
                String userId = res.getString("user_id");
                if (usersByGuild.containsKey(guildId)) {
                    usersByGuild.get(guildId).add(userId);
                } else {
                    usersByGuild.put(guildId, new HashSet<>());
                    usersByGuild.get(guildId).add(userId);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to fetch user IDs to exclude. Error: {}", String.valueOf(e));
        }
        return usersByGuild;
    }

    public static void updateLastShoutout(Map<String, List<String>> usersByGuildId) throws SQLException {
        if (usersByGuildId.isEmpty()) return;

        String statement = """
                UPDATE birthdays
                SET last_shoutout = strftime('%Y-%m-%d', 'now')
                WHERE guild_id = ? AND user_id = ?
                """;

        try (Connection conn = getConnection()) {
            PreparedStatement updateStatement = conn.prepareStatement(statement);

            for (String guildId : usersByGuildId.keySet()) {
                for (String userId : usersByGuildId.get(guildId)) {
                    updateStatement.setString(1, guildId);
                    updateStatement.setString(2, userId);
                    updateStatement.addBatch();
                }
            }
            updateStatement.executeBatch();
        }
    }

    public static Map<String, String> retrieveConfig(String guildId) throws SQLException {
        String statement = """
                SELECT * FROM shoutoutconfig
                WHERE guild_id = ?""";

        try (Connection conn = getConnection()) {
            PreparedStatement queryStatement = conn.prepareStatement(statement);
            queryStatement.setString(1, guildId);
            ResultSet res = queryStatement.executeQuery();

            Map<String, String> outputConfig = new HashMap<>();
            if (res.next()) {
                outputConfig.put("channelId", res.getString("channel_id"));
                outputConfig.put("shoutoutMessageOne", res.getString("shoutout_message_one"));
                outputConfig.put("shoutoutMessageMultiple", res.getString("shoutout_message_multiple"));
                outputConfig.put("shoutoutRoleId", res.getString("shoutout_role_id"));
                outputConfig.put("birthdayRoleId", res.getString("birthday_role_id"));
            }
            return outputConfig;
        } catch (SQLException e) {
            log.error("Failed to fetch config information for {}", guildId);
            throw e;
        }
    }

    public static void setShoutoutRole(String guildId, String roleId) throws SQLException {
        String statement = """
                UPDATE shoutoutconfig
                SET shoutout_role_id = ?
                WHERE guild_id = ?
                """;

        try (Connection conn = getConnection()) {
            PreparedStatement updateStatement = conn.prepareStatement(statement);
            updateStatement.setString(1, roleId);
            updateStatement.setString(2, guildId);
            updateStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Something went wrong updating the shoutout_role_id");
            throw new SQLException(e);
        }
    }

    public static void setBirthdayRole(String guildId, String roleId) throws SQLException {
        String statement = """
            INSERT INTO shoutoutconfig (guild_id, birthday_role_id)
            VALUES (?, ?)
            ON CONFLICT(guild_id)
            DO UPDATE SET birthday_role_id = excluded.birthday_role_id
            """;


        try (Connection conn = getConnection()) {
            PreparedStatement updateStatement = conn.prepareStatement(statement);
            updateStatement.setString(1, guildId);
            updateStatement.setString(2, roleId);
            updateStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Something went wrong updating the birthday_role_id");
            throw new SQLException(e);
        }
    }

    public static Map<String, String> retrieveGuildAndBirthdayRoleIds() {
        String statement = """
                SELECT guild_id, birthday_role_id FROM shoutoutconfig""";

        try (Connection conn = getConnection()) {
            PreparedStatement queryStatement = conn.prepareStatement(statement);
            ResultSet res = queryStatement.executeQuery();

            Map<String, String> output = new HashMap<>();
            while (res.next()) {
                String guildId = res.getString("guild_id");
                String birthdayRoleId = res.getString("birthday_role_id");
                if (birthdayRoleId != null) output.put(guildId, birthdayRoleId);
            }
            return output;
        } catch (SQLException e) {
            log.error("Exception while retrieving birthday role IDs: ", e);
        }
        return new HashMap<>();
    }

    public static Map<String, String> retrieveBirthdaysForExport(String guildId) {
        String statement = """
                SELECT user_id, birthday FROM birthdays
                WHERE guild_id = ?""";

        try (Connection conn = getConnection()) {
            PreparedStatement queryStatement = conn.prepareStatement(statement);
            queryStatement.setString(1, guildId);
            ResultSet res = queryStatement.executeQuery();

            Map<String, String> output = new HashMap<>();
            while (res.next()) {
                String userId = res.getString("user_id");
                String birthday = res.getString("birthday");
                output.put(userId, birthday);
            }
            return output;
        } catch (SQLException e) {
            log.error("Exception while retrieving birthday role IDs: ", e);
        }
        return new HashMap<>();
    }

    public static void purgeOnBotLeave(String guildId) {
        String statement = """
                DELETE FROM birthdays, shoutoutconfig
                WHERE guild_id = ?""";

        try (Connection conn = getConnection()) {
            PreparedStatement updateStatement = conn.prepareStatement(statement);
            updateStatement.setString(1, guildId);
            updateStatement.executeUpdate();
        } catch (Exception e) {
            log.error("Failed to purge database after bot (Unavailable)GuildLeaveEvent");
        }
    }
}

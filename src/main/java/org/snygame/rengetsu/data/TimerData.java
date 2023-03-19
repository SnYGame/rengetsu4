package org.snygame.rengetsu.data;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TimerData {
    private static PreparedStatement addTimerStmt;
    private static PreparedStatement getDataStmt;
    private static PreparedStatement removeDataStmt;
    private static PreparedStatement getAddTimersStmt;
    private static PreparedStatement cleanupTableStmt;

    static void initializeStatements(Connection connection) throws SQLException {
        QueryBuilder qb;

        qb = new QueryBuilder();
        qb.insertIgnoreInto("timer(channel_id, user_id, message, set_on, end_on)");
        qb.values("(?, ?, ?, ?, ?)");
        addTimerStmt = qb.build(connection, 1);

        qb = new QueryBuilder();
        qb.select("timer.channel_id, timer.user_id, timer.message, timer.set_on");
        qb.from("timer");
        qb.where("timer.timer_id = ?");
        getDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("timer");
        qb.where("timer.timer_id = ?");
        removeDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("timer.timer_id, timer.end_on");
        qb.from("timer");
        getAddTimersStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("timer");
        qb.where("timer.end_on < ?");
        cleanupTableStmt = qb.build(connection);
    }

    public static long addTimer(long channelId, long userId, String message, Instant setOn, Instant endOn) throws SQLException {
        addTimerStmt.setLong(1, channelId);
        addTimerStmt.setLong(2, userId);
        addTimerStmt.setString(3, message);
        addTimerStmt.setTimestamp(4, Timestamp.from(setOn));
        addTimerStmt.setTimestamp(5, Timestamp.from(endOn));
        addTimerStmt.execute();

        ResultSet rs = addTimerStmt.getGeneratedKeys();

        if (rs.next()) {
            return rs.getLong(1);
        }

        throw new RuntimeException();
    }

    public static Data getData(long timerId) throws SQLException {
        getDataStmt.setLong(1, timerId);
        ResultSet rs = getDataStmt.executeQuery();
        if (rs.next()) {
            return new Data(rs.getLong("channel_id"), rs.getLong("user_id"),
                    rs.getString("message"), rs.getTimestamp("set_on").toInstant());
        }
        return null;
    }

    public static int removeData(long timerId) throws SQLException {
        removeDataStmt.setLong(1, timerId);
        return removeDataStmt.executeUpdate();
    }

    public static List<Timer> getAllTimers() throws SQLException {
        ResultSet rs = getAddTimersStmt.executeQuery();
        ArrayList<Timer> timers = new ArrayList<>();
        while (rs.next()) {
            timers.add(new Timer(rs.getLong("timer_id"), rs.getTimestamp("end_on").toInstant()));
        }
        return timers;
    }

    public static int cleanupTable() throws SQLException {
        cleanupTableStmt.setTimestamp(1, Timestamp.from(Instant.now()));
        return cleanupTableStmt.executeUpdate();
    }

    public record Data(long channelId, long userId, String message, Instant setOn) {}
    public record Timer(long timerId, Instant endOn) {}
}

package org.snygame.rengetsu.data;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TimerData {
    private static PreparedStatement countTimerStmt;
    private static PreparedStatement addTimerStmt;
    private static PreparedStatement getDataStmt;
    private static PreparedStatement removeDataStmt;
    private static PreparedStatement getAddTimersStmt;
    private static PreparedStatement cleanupTableStmt;
    private static PreparedStatement listTimersStmt;

    static void initializeStatements(Connection connection) throws SQLException {
        QueryBuilder qb;

        qb = new QueryBuilder();
        qb.select("COUNT(*) as cnt");
        qb.from("timer");
        qb.where("timer.user_id = ?");
        countTimerStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.insertIgnoreInto("timer(channel_id, user_id, message, set_on, end_on)");
        qb.values("(?, ?, ?, ?, ?)");
        addTimerStmt = qb.build(connection, 1);

        qb = new QueryBuilder();
        qb.select("*");
        qb.from("timer");
        qb.where("timer.timer_id = ?");
        getDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("timer");
        qb.where("timer.timer_id = ?");
        removeDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("*");
        qb.from("timer");
        getAddTimersStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("timer");
        qb.where("timer.end_on < ?");
        cleanupTableStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("*");
        qb.from("timer");
        qb.where("timer.user_id = ?");
        listTimersStmt = qb.build(connection);
    }

    public static long addTimer(long channelId, long userId, String message, Instant setOn, Instant endOn) throws SQLException {
        countTimerStmt.setLong(1, userId);
        ResultSet rs = countTimerStmt.executeQuery();

        if (rs.next() && rs.getInt("cnt") >= 5) {
            return -1;
        }

        addTimerStmt.setLong(1, channelId);
        addTimerStmt.setLong(2, userId);
        addTimerStmt.setString(3, message);
        addTimerStmt.setTimestamp(4, Timestamp.from(setOn));
        addTimerStmt.setTimestamp(5, Timestamp.from(endOn));
        addTimerStmt.execute();

        rs = addTimerStmt.getGeneratedKeys();

        if (rs.next()) {
            return rs.getLong(1);
        }

        throw new RuntimeException();
    }

    public static Data getData(long timerId) throws SQLException {
        getDataStmt.setLong(1, timerId);
        ResultSet rs = getDataStmt.executeQuery();
        if (rs.next()) {
            return new Data(rs.getLong("timer_id"), rs.getLong("channel_id"), rs.getLong("user_id"),
                    rs.getString("message"), rs.getTimestamp("set_on").toInstant(),
                    rs.getTimestamp("end_on").toInstant());
        }
        return null;
    }

    public static int removeData(long timerId) throws SQLException {
        removeDataStmt.setLong(1, timerId);
        return removeDataStmt.executeUpdate();
    }

    public static List<Data> getAllTimers() throws SQLException {
        ResultSet rs = getAddTimersStmt.executeQuery();
        ArrayList<Data> timers = new ArrayList<>();
        while (rs.next()) {
            timers.add(new Data(rs.getLong("timer_id"), rs.getLong("channel_id"), rs.getLong("user_id"),
                    rs.getString("message"), rs.getTimestamp("set_on").toInstant(),
                    rs.getTimestamp("end_on").toInstant()));
        }
        return timers;
    }

    public static int cleanupTable() throws SQLException {
        cleanupTableStmt.setTimestamp(1, Timestamp.from(Instant.now()));
        return cleanupTableStmt.executeUpdate();
    }

    public static List<Data> listTimers(long user_id) throws SQLException {
        listTimersStmt.setLong(1, user_id);
        ResultSet rs = listTimersStmt.executeQuery();
        ArrayList<Data> timers = new ArrayList<>();
        while (rs.next()) {
            timers.add(new Data(rs.getLong("timer_id"), rs.getLong("channel_id"), rs.getLong("user_id"),
                    rs.getString("message"), rs.getTimestamp("set_on").toInstant(),
                    rs.getTimestamp("end_on").toInstant()));
        }
        return timers;
    }

    public record Data(long timerId, long channelId, long userId, String message, Instant setOn, Instant endOn) {}
}

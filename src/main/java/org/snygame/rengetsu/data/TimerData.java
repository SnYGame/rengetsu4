package org.snygame.rengetsu.data;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TimerData {
    public static long addTimer(long channelId, long userId, String message, Instant setOn, Instant endOn) throws SQLException {
        QueryBuilder qb = new QueryBuilder();
        qb.insertIgnoreInto("timer(channel_id, user_id, message, set_on, end_on)");
        qb.values("(?, ?, ?, ?, ?)");
        qb.returning("timer_id");
        qb.addParameter(channelId, Types.INTEGER);
        qb.addParameter(userId, Types.INTEGER);
        qb.addParameter(message, Types.VARCHAR);
        qb.addParameter(Timestamp.from(setOn), Types.TIMESTAMP);
        qb.addParameter(Timestamp.from(endOn), Types.TIMESTAMP);
        PreparedStatement ps = qb.build(DatabaseManager.connection);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            long timerId = rs.getLong("timer_id");
            rs.close();
            return timerId;
        }

        throw new RuntimeException();
    }

    public static Instant getEndTime(long timerId) throws SQLException {
        QueryBuilder qb = new QueryBuilder();
        qb.select("timer.end_on");
        qb.from("timer");
        qb.where("timer.timer_id = ?");
        qb.addParameter(timerId, Types.INTEGER);
        PreparedStatement ps = qb.build(DatabaseManager.connection);
        ResultSet rs = ps.executeQuery();
        if (rs.first()) {
            return rs.getTimestamp("end_on").toInstant();
        }
        return null;
    }

    public static Data getData(long timerId) throws SQLException {
        QueryBuilder qb = new QueryBuilder();
        qb.select("timer.channel_id, timer.user_id, timer.message, timer.set_on");
        qb.from("timer");
        qb.where("timer.timer_id = ?");
        qb.addParameter(timerId, Types.INTEGER);
        PreparedStatement ps = qb.build(DatabaseManager.connection);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new Data(rs.getLong("channel_id"), rs.getLong("user_id"),
                    rs.getString("message"), rs.getTimestamp("set_on").toInstant());
        }
        return null;
    }

    public static int removeData(long timerId) throws SQLException {
        QueryBuilder qb = new QueryBuilder();
        qb.deleteFrom("timer");
        qb.where("timer.timer_id = ?");
        qb.addParameter(timerId, Types.INTEGER);
        PreparedStatement ps = qb.build(DatabaseManager.connection);
        return ps.executeUpdate();
    }

    public static List<Timer> getAllTimers() throws SQLException {
        QueryBuilder qb = new QueryBuilder();
        qb.select("timer.timer_id, timer.end_on");
        qb.from("timer");
        PreparedStatement ps = qb.build(DatabaseManager.connection);
        ResultSet rs = ps.executeQuery();
        ArrayList<Timer> timers = new ArrayList<>();
        while (rs.next()) {
            timers.add(new Timer(rs.getLong("timer_id"), rs.getTimestamp("end_on").toInstant()));
        }
        return timers;
    }

    public static int cleanupTable() throws SQLException {
        QueryBuilder qb = new QueryBuilder();
        qb.deleteFrom("timer");
        qb.where("timer.end_on < ?");
        qb.addParameter(Timestamp.from(Instant.now()), Types.TIMESTAMP);
        PreparedStatement ps = qb.build(DatabaseManager.connection);
        return ps.executeUpdate();
    }

    public record Data(long channelId, long userId, String message, Instant setOn) {}
    public record Timer(long timerId, Instant endOn) {}
}

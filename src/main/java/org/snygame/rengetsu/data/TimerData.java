package org.snygame.rengetsu.data;

import org.snygame.rengetsu.Rengetsu;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TimerData extends TableData {
    private final PreparedStatement countTimerStmt;
    private final PreparedStatement addTimerStmt;
    private final PreparedStatement getDataStmt;
    private final PreparedStatement removeDataStmt;
    private final PreparedStatement getAllTimersStmt;
    private final PreparedStatement cleanupTableStmt;
    private final PreparedStatement listTimersStmt;

    TimerData(Rengetsu rengetsu, Connection connection) throws SQLException {
        super(rengetsu, connection);
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
        getAllTimersStmt = qb.build(connection);

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

    public long addTimer(long channelId, long userId, String message, Instant setOn, Instant endOn) throws SQLException {
        synchronized (connection) {
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
                long timerId = rs.getLong(1);
                connection.commit();
                return timerId;
            }

            throw new RuntimeException();
        }
    }

    public Data getData(long timerId) throws SQLException {
        synchronized (connection) {
            getDataStmt.setLong(1, timerId);
            ResultSet rs = getDataStmt.executeQuery();
            if (rs.next()) {
                return new Data(rs.getLong("timer_id"), rs.getLong("channel_id"), rs.getLong("user_id"),
                        rs.getString("message"), rs.getTimestamp("set_on").toInstant(),
                        rs.getTimestamp("end_on").toInstant());
            }
            return null;
        }
    }

    public void removeData(long timerId) throws SQLException {
        synchronized (connection) {
            removeDataStmt.setLong(1, timerId);
            removeDataStmt.executeUpdate();
            connection.commit();
        }
    }

    public List<Data> getAllTimers() throws SQLException {
        synchronized (connection) {
            ResultSet rs = getAllTimersStmt.executeQuery();
            ArrayList<Data> timers = new ArrayList<>();
            while (rs.next()) {
                timers.add(new Data(rs.getLong("timer_id"), rs.getLong("channel_id"), rs.getLong("user_id"),
                        rs.getString("message"), rs.getTimestamp("set_on").toInstant(),
                        rs.getTimestamp("end_on").toInstant()));
            }
            return timers;
        }
    }

    public int cleanupTable() throws SQLException {
        synchronized (connection) {
            cleanupTableStmt.setTimestamp(1, Timestamp.from(Instant.now()));
            int rows = cleanupTableStmt.executeUpdate();
            connection.commit();
            return rows;
        }
    }

    public List<Data> listTimers(long user_id) throws SQLException {
        synchronized (connection) {
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
    }

    public record Data(long timerId, long channelId, long userId, String message, Instant setOn, Instant endOn) {}
}

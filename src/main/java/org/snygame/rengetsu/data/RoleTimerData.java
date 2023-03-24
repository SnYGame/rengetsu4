package org.snygame.rengetsu.data;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RoleTimerData {
    private final Connection connection;

    private final PreparedStatement addTimerStmt;
    private final PreparedStatement getDataStmt;
    private final PreparedStatement removeDataStmt;
    private final PreparedStatement getAllTimersStmt;
    private final PreparedStatement getTimerIdStmt;

    RoleTimerData(Connection connection) throws SQLException {
        this.connection = connection;

        QueryBuilder qb;

        qb = new QueryBuilder();
        qb.insertIgnoreInto("role_timer(role_id, server_id, user_id, end_on)");
        qb.values("(?, ?, ?, ?)");
        addTimerStmt = qb.build(connection, 1);

        qb = new QueryBuilder();
        qb.select("*");
        qb.from("role_timer");
        qb.where("role_timer.timer_id = ?");
        getDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("role_timer");
        qb.where("role_timer.timer_id = ?");
        removeDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("*");
        qb.from("role_timer");
        getAllTimersStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("*");
        qb.from("role_timer");
        qb.where("role_timer.server_id = ? AND role_timer.user_id = ?");
        getTimerIdStmt = qb.build(connection);
    }

    public long addTimer(long roleId, long serverId, long userId, Instant endOn) throws SQLException {
        synchronized (connection) {
            addTimerStmt.setLong(1, roleId);
            addTimerStmt.setLong(2, serverId);
            addTimerStmt.setLong(3, userId);
            addTimerStmt.setTimestamp(4, Timestamp.from(endOn));
            addTimerStmt.execute();

            ResultSet rs = addTimerStmt.getGeneratedKeys();

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
                return new Data(rs.getLong("timer_id"), rs.getLong("role_id"), rs.getLong("server_id"),
                        rs.getLong("user_id"), rs.getTimestamp("end_on").toInstant());
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
                timers.add(new Data(rs.getLong("timer_id"), rs.getLong("role_id"), rs.getLong("server_id"),
                        rs.getLong("user_id"), rs.getTimestamp("end_on").toInstant()));
            }
            return timers;
        }
    }

    public List<Data> getTimerIds(long serverId, long userId) throws SQLException {
        synchronized (connection) {
            getTimerIdStmt.setLong(1, serverId);
            getTimerIdStmt.setLong(2, userId);

            ResultSet rs = getTimerIdStmt.executeQuery();

            ArrayList<Data> timers = new ArrayList<>();
            while (rs.next()) {
                timers.add(new Data(rs.getLong("timer_id"), rs.getLong("role_id"), rs.getLong("server_id"),
                        rs.getLong("user_id"), rs.getTimestamp("end_on").toInstant()));
            }
            return timers;
        }
    }

    public record Data(long timerId, long roleId, long serverId, long userId, Instant endOn) {}
}

package org.snygame.rengetsu.data;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RoleTimerData {
    private static Connection connection;

    private static PreparedStatement addTimerStmt;
    private static PreparedStatement getDataStmt;
    private static PreparedStatement removeDataStmt;
    private static PreparedStatement getAllTimersStmt;
    private static PreparedStatement cleanupTableStmt;
    private static PreparedStatement getTimerIdStmt;

    static void initializeStatements(Connection connection) throws SQLException {
        RoleTimerData.connection = connection;

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
        qb.deleteFrom("role_timer");
        qb.where("role_timer.end_on < ?");
        cleanupTableStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("*");
        qb.from("role_timer");
        qb.where("role_timer.server_id = ? AND role_timer.user_id = ?");
        getTimerIdStmt = qb.build(connection);
    }

    public static long addTimer(long roleId, long serverId, long userId, Instant endOn) throws SQLException {
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

    public static Data getData(long timerId) throws SQLException {
        getDataStmt.setLong(1, timerId);
        ResultSet rs = getDataStmt.executeQuery();
        if (rs.next()) {
            return new Data(rs.getLong("timer_id"), rs.getLong("role_id"), rs.getLong("server_id"),
                    rs.getLong("user_id"), rs.getTimestamp("end_on").toInstant());
        }
        return null;
    }

    public static void removeData(long timerId) throws SQLException {
        removeDataStmt.setLong(1, timerId);
        removeDataStmt.executeUpdate();
        connection.commit();
    }

    public static List<Data> getAllTimers() throws SQLException {
        ResultSet rs = getAllTimersStmt.executeQuery();
        ArrayList<Data> timers = new ArrayList<>();
        while (rs.next()) {
            timers.add(new Data(rs.getLong("timer_id"), rs.getLong("role_id"), rs.getLong("server_id"),
                    rs.getLong("user_id"), rs.getTimestamp("end_on").toInstant()));
        }
        return timers;
    }

    public static int cleanupTable() throws SQLException {
        cleanupTableStmt.setTimestamp(1, Timestamp.from(Instant.now()));
        int rows = cleanupTableStmt.executeUpdate();
        connection.commit();
        return rows;
    }

    public static List<Data> getTimerIds(long serverId, long userId) throws SQLException {
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

    public record Data(long timerId, long roleId, long serverId, long userId, Instant endOn) {}
}

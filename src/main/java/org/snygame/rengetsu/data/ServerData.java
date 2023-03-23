package org.snygame.rengetsu.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServerData {
    private static Connection connection;

    private static PreparedStatement initializeServerStmt;
    private static PreparedStatement getInactiveDaysStmt;
    private static PreparedStatement getUserLogsStmt;
    private static PreparedStatement getMessageLogsStmt;

    private static PreparedStatement setInactiveDaysStmt;
    private static PreparedStatement clearUserLogsStmt;
    private static PreparedStatement clearMessageLogsStmt;
    private static PreparedStatement addUserLogsStmt;
    private static PreparedStatement addMessageLogsStmt;


    static void initializeStatements(Connection connection) throws SQLException {
        ServerData.connection = connection;

        QueryBuilder qb;

        qb = new QueryBuilder();
        qb.insertIgnoreInto("server(server_id)");
        qb.values("(?)");
        initializeServerStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("server.inactive_days");
        qb.from("server");
        qb.where("server.server_id = ?");
        getInactiveDaysStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("server_usr_log.channel_id");
        qb.from("server_usr_log");
        qb.where("server_usr_log.server_id = ?");
        getUserLogsStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("server_msg_log.channel_id");
        qb.from("server_msg_log");
        qb.where("server_msg_log.server_id = ?");
        getMessageLogsStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.replaceInto("server(server_id, inactive_days)");
        qb.values("(?, ?)");
        setInactiveDaysStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("server_usr_log");
        qb.where("server_id = ?");
        clearUserLogsStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("server_msg_log");
        qb.where("server_id = ?");
        clearMessageLogsStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.insertIgnoreInto("server_usr_log(server_id, channel_id)");
        qb.values("(?, ?)");
        addUserLogsStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.insertIgnoreInto("server_msg_log(server_id, channel_id)");
        qb.values("(?, ?)");
        addMessageLogsStmt = qb.build(connection);
    }

    public static void initializeServer(long id) throws SQLException {
        initializeServerStmt.setLong(1, id);
        initializeServerStmt.executeUpdate();
        connection.commit();
    }

    public static int getInactiveDays(long id) throws SQLException {
        getInactiveDaysStmt.setLong(1, id);
        ResultSet rs = getInactiveDaysStmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("inactive_days");
        }
        return 0;
    }

    public static List<Long> getUserLogs(long id) throws SQLException {
        getUserLogsStmt.setLong(1, id);
        ResultSet rs = getUserLogsStmt.executeQuery();
        ArrayList<Long> channelIds = new ArrayList<>();
        while (rs.next()) {
            channelIds.add(rs.getLong("channel_id"));
        }
        return channelIds;
    }

    public static List<Long> getMessageLogs(long id) throws SQLException {
        getMessageLogsStmt.setLong(1, id);
        ResultSet rs = getMessageLogsStmt.executeQuery();
        ArrayList<Long> channelIds = new ArrayList<>();
        while (rs.next()) {
            channelIds.add(rs.getLong("channel_id"));
        }
        return channelIds;
    }

    public static int setInactiveDays(long id, int days) throws SQLException {
        setInactiveDaysStmt.setLong(1, id);
        setInactiveDaysStmt.setLong(2, days);
        int row = setInactiveDaysStmt.executeUpdate();
        connection.commit();
        return row;
    }

    public static void setUserLogs(long id, List<Long> channelIds) throws SQLException {
        try {
            clearUserLogsStmt.setLong(1, id);
            clearUserLogsStmt.executeUpdate();

            addUserLogsStmt.setLong(1, id);
            for (Long channelId : channelIds) {
                addUserLogsStmt.setLong(2, channelId);
                addUserLogsStmt.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }

    public static void setMessageLogs(long id, List<Long> channelIds) throws SQLException {
        try {
            clearMessageLogsStmt.setLong(1, id);
            clearMessageLogsStmt.executeUpdate();

            addMessageLogsStmt.setLong(1, id);
            for (Long channelId: channelIds) {
                addMessageLogsStmt.setLong(2, channelId);
                addMessageLogsStmt.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }
}

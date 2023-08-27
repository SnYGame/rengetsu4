package org.snygame.rengetsu.data;

import org.snygame.rengetsu.Rengetsu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServerData extends TableData {
    private final PreparedStatement initializeServerStmt;
    private final PreparedStatement getInactiveDaysStmt;
    private final PreparedStatement getUserLogsStmt;
    private final PreparedStatement getMessageLogsStmt;

    private final PreparedStatement setInactiveDaysStmt;
    private final PreparedStatement clearUserLogsStmt;
    private final PreparedStatement clearMessageLogsStmt;
    private final PreparedStatement addUserLogsStmt;
    private final PreparedStatement addMessageLogsStmt;

    private final PreparedStatement getReportLogsStmt;
    private final PreparedStatement clearReportLogsStmt;
    private final PreparedStatement addReportLogsStmt;


    ServerData(Rengetsu rengetsu, Connection connection) throws SQLException {
        super(rengetsu, connection);

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

        qb = new QueryBuilder();
        qb.select("server_report_log.channel_id");
        qb.from("server_report_log");
        qb.where("server_report_log.server_id = ?");
        getReportLogsStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("server_report_log");
        qb.where("server_id = ?");
        clearReportLogsStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.insertIgnoreInto("server_report_log(server_id, channel_id)");
        qb.values("(?, ?)");
        addReportLogsStmt = qb.build(connection);
    }

    public void initializeServer(long id) throws SQLException {
        synchronized (connection) {
            initializeServerStmt.setLong(1, id);
            initializeServerStmt.executeUpdate();
            connection.commit();
        }
    }

    public int getInactiveDays(long id) throws SQLException {
        synchronized (connection) {
            getInactiveDaysStmt.setLong(1, id);
            ResultSet rs = getInactiveDaysStmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("inactive_days");
            }
            return 0;
        }
    }

    public List<Long> getUserLogs(long id) throws SQLException {
        synchronized (connection) {
            getUserLogsStmt.setLong(1, id);
            ResultSet rs = getUserLogsStmt.executeQuery();
            ArrayList<Long> channelIds = new ArrayList<>();
            while (rs.next()) {
                channelIds.add(rs.getLong("channel_id"));
            }
            return channelIds;
        }
    }

    public List<Long> getMessageLogs(long id) throws SQLException {
        synchronized (connection) {
            getMessageLogsStmt.setLong(1, id);
            ResultSet rs = getMessageLogsStmt.executeQuery();
            ArrayList<Long> channelIds = new ArrayList<>();
            while (rs.next()) {
                channelIds.add(rs.getLong("channel_id"));
            }
            return channelIds;
        }
    }

    public int setInactiveDays(long id, int days) throws SQLException {
        synchronized (connection) {
            setInactiveDaysStmt.setLong(1, id);
            setInactiveDaysStmt.setLong(2, days);
            int row = setInactiveDaysStmt.executeUpdate();
            connection.commit();
            return row;
        }
    }

    public void setUserLogs(long id, List<Long> channelIds) throws SQLException {
        synchronized (connection) {
            try {
                initializeServer(id);
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
    }

    public void setMessageLogs(long id, List<Long> channelIds) throws SQLException {
        synchronized (connection) {
            try {
                initializeServer(id);
                clearMessageLogsStmt.setLong(1, id);
                clearMessageLogsStmt.executeUpdate();

                addMessageLogsStmt.setLong(1, id);
                for (Long channelId : channelIds) {
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

    public List<Long> getReportLogs(long id) throws SQLException {
        synchronized (connection) {
            getReportLogsStmt.setLong(1, id);
            ResultSet rs = getReportLogsStmt.executeQuery();
            ArrayList<Long> channelIds = new ArrayList<>();
            while (rs.next()) {
                channelIds.add(rs.getLong("channel_id"));
            }
            return channelIds;
        }
    }

    public void setReportLogs(long id, List<Long> channelIds) throws SQLException {
        synchronized (connection) {
            try {
                initializeServer(id);
                clearReportLogsStmt.setLong(1, id);
                clearReportLogsStmt.executeUpdate();

                addReportLogsStmt.setLong(1, id);
                for (Long channelId : channelIds) {
                    addReportLogsStmt.setLong(2, channelId);
                    addReportLogsStmt.executeUpdate();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }
}

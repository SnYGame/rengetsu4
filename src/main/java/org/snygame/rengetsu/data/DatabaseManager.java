package org.snygame.rengetsu.data;

import org.snygame.rengetsu.util.Resources;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static UserData userData;
    private static TimerData timerData;
    private static RoleData roleData;
    private static ServerData serverData;
    private static RoleTimerData roleTimerData;

    public static void connectSqlite(String dbPath, String tablePath) throws SQLException, IOException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        connection.setAutoCommit(false);
        connection.createStatement().execute("PRAGMA foreign_keys = true");

        createTables(connection, tablePath);
        userData = new UserData(connection);
        timerData = new TimerData(connection);
        roleData = new RoleData(connection);
        serverData = new ServerData(connection);
        roleTimerData = new RoleTimerData(connection);
        connection.commit();
    }

    private static void createTables(Connection connection, String path) throws IOException, SQLException {
        String ddl = Resources.getResourceFileAsString(path);
        Statement statement = connection.createStatement();
        statement.executeUpdate(ddl);
    }

    public static UserData getUserData() {
        return userData;
    }

    public static TimerData getTimerData() {
        return timerData;
    }

    public static RoleData getRoleData() {
        return roleData;
    }

    public static ServerData getServerData() {
        return serverData;
    }

    public static RoleTimerData getRoleTimerData() {
        return roleTimerData;
    }
}

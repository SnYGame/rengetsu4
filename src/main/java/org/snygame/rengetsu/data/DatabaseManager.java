package org.snygame.rengetsu.data;

import org.snygame.rengetsu.RengClass;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.util.Resources;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager extends RengClass {
    private final UserData userData;
    private final TimerData timerData;
    private final RoleData roleData;
    private final ServerData serverData;
    private final RoleTimerData roleTimerData;

    private final Connection connection;

    public DatabaseManager(Rengetsu rengetsu, String dbPath, String tablePath) throws SQLException, IOException {
        super(rengetsu);

        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        connection.setAutoCommit(false);
        connection.createStatement().execute("PRAGMA foreign_keys = true");

        createTables(connection, tablePath);
        userData = new UserData(rengetsu, connection);
        timerData = new TimerData(rengetsu, connection);
        roleData = new RoleData(rengetsu, connection);
        serverData = new ServerData(rengetsu, connection);
        roleTimerData = new RoleTimerData(rengetsu, connection);
        connection.commit();
    }

    private static void createTables(Connection connection, String path) throws IOException, SQLException {
        String ddl = Resources.getResourceFileAsString(path);
        Statement statement = connection.createStatement();
        statement.executeUpdate(ddl);
    }

    public UserData getUserData() {
        return userData;
    }

    public TimerData getTimerData() {
        return timerData;
    }

    public RoleData getRoleData() {
        return roleData;
    }

    public ServerData getServerData() {
        return serverData;
    }

    public RoleTimerData getRoleTimerData() {
        return roleTimerData;
    }

    public Connection getConnection() {
        return connection;
    }
}

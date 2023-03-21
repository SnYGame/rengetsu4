package org.snygame.rengetsu.data;

import org.snygame.rengetsu.util.Resources;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    public static void connectSqlite(String dbPath, String tablePath) throws SQLException, IOException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        connection.setAutoCommit(false);

        createTables(connection, tablePath);
        UserData.initializeStatements(connection);
        TimerData.initializeStatements(connection);
        RoleData.initializeStatements(connection);
        connection.commit();
    }

    private static void createTables(Connection connection, String path) throws IOException, SQLException {
        String ddl = Resources.getResourceFileAsString(path);
        Statement statement = connection.createStatement();
        statement.executeUpdate(ddl);
    }
}

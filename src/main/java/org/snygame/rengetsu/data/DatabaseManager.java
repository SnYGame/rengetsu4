package org.snygame.rengetsu.data;

import org.snygame.rengetsu.util.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.stream.Collectors;

public class DatabaseManager {
    public static void connectSqlite(String dbPath, String tablePath) throws SQLException, IOException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        createTables(connection, tablePath);
        UserData.initializeStatements(connection);
        TimerData.initializeStatements(connection);
    }

    private static void createTables(Connection connection, String path) throws IOException, SQLException {
        String ddl = Resources.getResourceFileAsString(path);
        Statement statement = connection.createStatement();
        statement.executeUpdate(ddl);
    }
}

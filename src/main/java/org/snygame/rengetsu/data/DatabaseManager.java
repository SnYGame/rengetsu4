package org.snygame.rengetsu.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.stream.Collectors;

public class DatabaseManager {
    private static Connection connection = null;

    public static void connectSqlite(String path) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
    }

    public static void createTables(String path) throws IOException, SQLException {
        String ddl = getResourceFileAsString(path);
        System.out.println(ddl);
        Statement statement = connection.createStatement();
        statement.executeUpdate(ddl);
    }

    private static String getResourceFileAsString(String fileName) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream resourceAsStream = classLoader.getResourceAsStream(fileName)) {
            if (resourceAsStream == null) return null;
            try (InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream);
                BufferedReader reader = new BufferedReader(inputStreamReader)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }
}

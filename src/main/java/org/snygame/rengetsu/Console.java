package org.snygame.rengetsu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Console extends RengClass {
    public Console(Rengetsu rengetsu) {
        super(rengetsu);
    }

    void runConsole() throws IOException {
        Connection connection = rengetsu.getDatabaseManager().getConnection();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        for (;;) {
            try {
                String line = br.readLine();
                synchronized (connection) {
                    Statement statement = connection.createStatement();
                    if (statement.execute(line)) {
                        ResultSet rs = statement.getResultSet();
                        int columns = rs.getMetaData().getColumnCount();
                        StringJoiner joiner = new StringJoiner(", ");
                        for (int i = 0; i < columns; i++) {
                            joiner.add(rs.getMetaData().getColumnLabel(i + 1));
                        }
                        System.out.println(joiner);
                        while (rs.next()) {
                            joiner = new StringJoiner(", ");
                            for (int i = 0; i < columns; i++) {
                                joiner.add(rs.getString(i + 1));
                            }
                            System.out.println(joiner);
                        }
                    } else {
                        int rows = statement.getUpdateCount();
                        System.out.printf("Update count: %d.", rows);
                    }
                    connection.commit();
                }
            } catch (SQLException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}

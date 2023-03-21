package org.snygame.rengetsu.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ServerData {
    private static Connection connection;

    private static PreparedStatement initializeServerStmt;

    static void initializeStatements(Connection connection) throws SQLException {
        ServerData.connection = connection;

        QueryBuilder qb;

        qb = new QueryBuilder();
        qb.insertIgnoreInto("server(server_id)");
        qb.values("(?)");
        initializeServerStmt = qb.build(connection);
    }

    public static int initializeServer(long id) throws SQLException {
        initializeServerStmt.setLong(1, id);
        int rows = initializeServerStmt.executeUpdate();
        connection.commit();
        return rows;
    }
}

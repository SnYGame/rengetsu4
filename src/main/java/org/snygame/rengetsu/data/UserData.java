package org.snygame.rengetsu.data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.time.Instant;

public class UserData {
    private static int initializeUser(long id) throws SQLException {
        QueryBuilder qb = new QueryBuilder();
        qb.insertIgnoreInto("user(user_id)");
        qb.values("(?)");
        qb.addParameter(id, Types.INTEGER);
        PreparedStatement ps = qb.build(DatabaseManager.connection);
        return ps.executeUpdate();
    }

    public static BigInteger getSaltAmount(long id) {
        try {
            QueryBuilder qb = new QueryBuilder();
            qb.select("u.salt_amount");
            qb.from("user u");
            qb.where("u.user_id = ?");
            qb.addParameter(id, Types.INTEGER);
            PreparedStatement ps = qb.build(DatabaseManager.connection);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("salt_amount").toBigInteger();
            }

            if (initializeUser(id) > 0) {
                return BigInteger.ZERO;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static BigInteger claimSalt(long id) {
        try {
            QueryBuilder qb = new QueryBuilder();
            qb.select("u.salt_amount, u.salt_last_claim");
            qb.from("user u");
            qb.where("u.user_id = ?");
            qb.addParameter(id, Types.INTEGER);
            PreparedStatement ps = qb.build(DatabaseManager.connection);
            ResultSet rs = ps.executeQuery();

            BigInteger saltAmount;
            Timestamp lastClaim;
            if (rs.next()) {
                saltAmount = rs.getBigDecimal("salt_amount").toBigInteger();
                lastClaim = rs.getTimestamp("salt_last_claim");
            } else {
                saltAmount = BigInteger.ZERO;
                lastClaim = Timestamp.from(Instant.EPOCH);
                initializeUser(id);
            }

            long remain = lastClaim.getTime() + 1000 * 60 * 60 * 24 - System.currentTimeMillis();
            if (remain > 0) {
                return BigInteger.valueOf(-remain);
            }

            saltAmount = saltAmount.add(BigInteger.valueOf(2000));
            qb = new QueryBuilder();
            qb.update("user");
            qb.set("salt_amount = ?, salt_last_claim = ?, salt_reminded = ?");
            qb.where("user_id = ?");
            qb.addParameter(new BigDecimal(saltAmount), Types.BIGINT);
            qb.addParameter(Timestamp.from(Instant.now()), Types.TIMESTAMP);
            qb.addParameter(false, Types.BOOLEAN);
            qb.addParameter(id, Types.INTEGER);
            ps = qb.build(DatabaseManager.connection);
            if (ps.executeUpdate() > 0) {
                return saltAmount;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}

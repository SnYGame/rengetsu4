package org.snygame.rengetsu.data;

import discord4j.common.util.Snowflake;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserData {
    public static final int DAY_MILLI = 1000 * 60 * 60 * 24;

    private static Connection connection;

    private static PreparedStatement initializeUserStmt;
    private static PreparedStatement getSaltAmountStmt;
    private static PreparedStatement getSaltClaimStmt;
    private static PreparedStatement setSaltClaimStmt;
    private static PreparedStatement setSaltStmt;
    private static PreparedStatement getRemindStmt;
    private static PreparedStatement setRemindStmt;
    private static PreparedStatement getRemindIdsStmt;

    static void initializeStatements(Connection connection) throws SQLException {
        UserData.connection = connection;

        QueryBuilder qb;

        qb = new QueryBuilder();
        qb.insertIgnoreInto("user(user_id)");
        qb.values("(?)");
        initializeUserStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("u.salt_amount");
        qb.from("user u");
        qb.where("u.user_id = ?");
        getSaltAmountStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("u.salt_amount, u.salt_last_claim");
        qb.from("user u");
        qb.where("u.user_id = ?");
        getSaltClaimStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.update("user");
        qb.set("salt_amount = ?, salt_last_claim = ?");
        qb.where("user_id = ?");
        setSaltClaimStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.update("user");
        qb.set("salt_amount = ?");
        qb.where("user_id = ?");
        setSaltStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("u.salt_remind");
        qb.from("user u");
        qb.where("u.user_id = ?");
        getRemindStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.update("user");
        qb.set("salt_remind = ?");
        qb.where("user_id = ?");
        setRemindStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("u.user_id");
        qb.from("user u");
        qb.where("u.salt_remind");
        getRemindIdsStmt = qb.build(connection);
    }

    private static int initializeUser(long id) throws SQLException {
        initializeUserStmt.setLong(1, id);
        int rows = initializeUserStmt.executeUpdate();
        connection.commit();
        return rows;
    }

    public static BigInteger getSaltAmount(long id) throws SQLException {
        getSaltAmountStmt.setLong(1, id);
        ResultSet rs = getSaltAmountStmt.executeQuery();
        if (rs.next()) {
            return rs.getBigDecimal("salt_amount").toBigInteger();
        }

        if (initializeUser(id) > 0) {
            return BigInteger.ZERO;
        }

        throw new RuntimeException();
    }

    public static BigInteger claimSalt(long id) throws SQLException {
        getSaltClaimStmt.setLong(1, id);
        ResultSet rs = getSaltClaimStmt.executeQuery();

        BigInteger saltAmount;
        long lastClaim;
        if (rs.next()) {
            saltAmount = rs.getBigDecimal("salt_amount").toBigInteger();
            lastClaim = rs.getLong("salt_last_claim");
        } else {
            saltAmount = BigInteger.ZERO;
            lastClaim = 0;
            initializeUser(id);
        }

        long remain = (lastClaim + 1) * DAY_MILLI - System.currentTimeMillis();
        if (remain > 0) {
            return BigInteger.valueOf(-remain);
        }

        saltAmount = saltAmount.add(BigInteger.valueOf(2000));

        setSaltClaimStmt.setBigDecimal(1, new BigDecimal(saltAmount));
        setSaltClaimStmt.setLong(2, System.currentTimeMillis() / DAY_MILLI);
        setSaltClaimStmt.setLong(3, id);
        if (setSaltClaimStmt.executeUpdate() > 0) {
            connection.commit();
            return saltAmount;
        }

        throw new RuntimeException();
    }

    public static BigInteger giveSalt(long idSender, long idRecipient, BigInteger amount) throws SQLException {
        BigInteger senderSalt = getSaltAmount(idSender);
        BigInteger recipientSalt = getSaltAmount(idRecipient);

        senderSalt = senderSalt.subtract(amount);
        recipientSalt = recipientSalt.add(amount);
        if (senderSalt.signum() == -1) {
            return senderSalt;
        }

        setSaltStmt.setBigDecimal(1, new BigDecimal(senderSalt));
        setSaltStmt.setLong(2, idSender);
        if (setSaltStmt.executeUpdate() > 0) {
            setSaltStmt.setBigDecimal(1, new BigDecimal(recipientSalt));
            setSaltStmt.setLong(2, idRecipient);
            if (setSaltStmt.executeUpdate() > 0) {
                connection.commit();
                return senderSalt;
            }
        }

        throw new RuntimeException();
    }

    public static boolean toggleRemind(long id) throws SQLException {
        getRemindStmt.setLong(1, id);
        ResultSet rs = getRemindStmt.executeQuery();
        boolean remind;
        if (rs.next()) {
            remind = rs.getBoolean("salt_remind");
        } else {
            remind = false;
            initializeUser(id);
        }

        setRemindStmt.setBoolean(1, !remind);
        setRemindStmt.setLong(2, id);
        if (setRemindStmt.executeUpdate() > 0) {
            connection.commit();
            return !remind;
        }

        throw new RuntimeException();
    }

    public static List<Snowflake> getRemindIds() throws SQLException {
        ResultSet rs = getRemindIdsStmt.executeQuery();

        ArrayList<Snowflake> ids = new ArrayList<>();
        while (rs.next()) {
            ids.add(Snowflake.of(rs.getLong("user_id")));
        }

        return ids;
    }
}

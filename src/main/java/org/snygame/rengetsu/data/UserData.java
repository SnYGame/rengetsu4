package org.snygame.rengetsu.data;

import discord4j.common.util.Snowflake;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class UserData {
    public static final int DAY_MILLI = 1000 * 60 * 60 * 24;

    private static int initializeUser(long id) throws SQLException {
        QueryBuilder qb = new QueryBuilder();
        qb.insertIgnoreInto("user(user_id)");
        qb.values("(?)");
        qb.addParameter(id, Types.INTEGER);
        PreparedStatement ps = qb.build(DatabaseManager.connection);
        return ps.executeUpdate();
    }

    public static BigInteger getSaltAmount(long id) throws SQLException {
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

        throw new RuntimeException();
    }

    public static BigInteger claimSalt(long id) throws SQLException {
        QueryBuilder qb = new QueryBuilder();
        qb.select("u.salt_amount, u.salt_last_claim");
        qb.from("user u");
        qb.where("u.user_id = ?");
        qb.addParameter(id, Types.INTEGER);
        PreparedStatement ps = qb.build(DatabaseManager.connection);
        ResultSet rs = ps.executeQuery();

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
        qb = new QueryBuilder();
        qb.update("user");
        qb.set("salt_amount = ?, salt_last_claim = ?");
        qb.where("user_id = ?");
        qb.addParameter(new BigDecimal(saltAmount), Types.BIGINT);
        qb.addParameter(System.currentTimeMillis() / DAY_MILLI, Types.INTEGER);
        qb.addParameter(id, Types.INTEGER);
        ps = qb.build(DatabaseManager.connection);
        if (ps.executeUpdate() > 0) {
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

        QueryBuilder qb = new QueryBuilder();
        qb.update("user");
        qb.set("salt_amount = ?");
        qb.where("user_id = ?");
        qb.addParameter(new BigDecimal(senderSalt), Types.BIGINT);
        qb.addParameter(idSender, Types.INTEGER);
        PreparedStatement ps = qb.build(DatabaseManager.connection);
        if (ps.executeUpdate() > 0) {
            qb = new QueryBuilder();
            qb.update("user");
            qb.set("salt_amount = ?");
            qb.where("user_id = ?");
            qb.addParameter(new BigDecimal(recipientSalt), Types.BIGINT);
            qb.addParameter(idRecipient, Types.INTEGER);
            ps = qb.build(DatabaseManager.connection);

            if (ps.executeUpdate() > 0) {
                return senderSalt;
            }
        }

        throw new RuntimeException();
    }

    public static boolean toggleRemind(long id) throws SQLException {
        QueryBuilder qb = new QueryBuilder();
        qb.select("u.salt_remind");
        qb.from("user u");
        qb.where("u.user_id = ?");
        qb.addParameter(id, Types.INTEGER);
        PreparedStatement ps = qb.build(DatabaseManager.connection);
        ResultSet rs = ps.executeQuery();
        boolean remind;
        if (rs.next()) {
            remind = rs.getBoolean("salt_remind");
        } else {
            remind = false;
            initializeUser(id);
        }

        qb = new QueryBuilder();
        qb.update("user");
        qb.set("salt_remind = ?");
        qb.where("user_id = ?");
        qb.addParameter(!remind, Types.BOOLEAN);
        qb.addParameter(id, Types.INTEGER);
        ps = qb.build(DatabaseManager.connection);
        if (ps.executeUpdate() > 0) {
            return !remind;
        }

        throw new RuntimeException();
    }

    public static List<Snowflake> getRemindIds() throws SQLException {
        QueryBuilder qb = new QueryBuilder();
        qb.select("u.user_id");
        qb.from("user u");
        qb.where("u.salt_remind");
        PreparedStatement ps = qb.build(DatabaseManager.connection);
        ResultSet rs = ps.executeQuery();

        ArrayList<Snowflake> ids = new ArrayList<>();
        while (rs.next()) {
            ids.add(Snowflake.of(rs.getLong("user_id")));
        }

        return ids;
    }
}

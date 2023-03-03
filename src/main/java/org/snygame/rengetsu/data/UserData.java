package org.snygame.rengetsu.data;

import java.sql.*;
import java.time.Instant;

public class UserData {
    public static Data getUserData(long id) {
        try {
            QueryBuilder qb = new QueryBuilder();
            qb.select("u.salt_amount, u.salt_last_claim, u.salt_remind, u.salt_reminded");
            qb.from("user u");
            qb.where("u.user_id = ?");
            qb.addParameter(id, Types.INTEGER);
            PreparedStatement ps = qb.build(DatabaseManager.connection);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Data(rs.getLong("salt_amount"), rs.getTimestamp("salt_last_claim"),
                        rs.getBoolean("salt_remind"), rs.getBoolean("salt_reminded"));
            }

            qb = new QueryBuilder();
            qb.insertIgnoreInto("user(user_id)");
            qb.values("(?)");
            qb.addParameter(id, Types.INTEGER);
            ps = qb.build(DatabaseManager.connection);
            if (ps.executeUpdate() > 0) {
                return new Data();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean claimSalt(long id, long saltAmount) {
        try {
            QueryBuilder qb = new QueryBuilder();
            qb.update("user");
            qb.set("salt_amount = ?, salt_last_claim = ?, salt_reminded = ?");
            qb.where("user_id = ?");
            qb.addParameter(saltAmount, Types.INTEGER);
            qb.addParameter(Timestamp.from(Instant.now()), Types.TIMESTAMP);
            qb.addParameter(false, Types.BOOLEAN);
            qb.addParameter(id, Types.INTEGER);
            PreparedStatement ps = qb.build(DatabaseManager.connection);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public record Data(long saltAmount, Timestamp saltLastClaim, boolean saltRemind, boolean saltReminded) {
        private Data() {
            this(0, Timestamp.from(Instant.EPOCH), false, false);
        }
    }
}

package org.snygame.rengetsu.data;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.tasks.TaskManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RoleData extends TableData {
    private final PreparedStatement getRoleDataStmt;
    private final PreparedStatement getRoleRequestDataStmt;
    private final PreparedStatement getRoleOnRemoveDataStmt;
    private final PreparedStatement getRoleOnAddDataStmt;

    private final PreparedStatement setRoleDataStmt;
    private final PreparedStatement setRoleRequestDataStmt;
    private final PreparedStatement deleteRoleRequestDataStmt;
    private final PreparedStatement clearRoleOnRemoveDataStmt;
    private final PreparedStatement clearRoleOnAddDataStmt;
    private final PreparedStatement addRoleOnRemoveDataStmt;
    private final PreparedStatement addRoleOnAddDataStmt;

    private final PreparedStatement clearRoleDataStmt;

    private final PreparedStatement getRolesToAddOnJoinStmt;
    private final PreparedStatement getRolesToAddOnInactiveStmt;

    RoleData(Rengetsu rengetsu, Connection connection) throws SQLException {
        super(rengetsu, connection);

        QueryBuilder qb;

        qb = new QueryBuilder();
        qb.select("role.add_on_join, role.add_on_inactive");
        qb.from("role");
        qb.where("role.role_id = ? AND role.server_id = ?");
        getRoleDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("role_requestable.temp, role_requestable.agreement");
        qb.from("role_requestable");
        qb.where("role_requestable.role_id = ? AND role_requestable.server_id = ?");
        getRoleRequestDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("role_add_when_removed.to_add_id");
        qb.from("role_add_when_removed");
        qb.where("role_add_when_removed.role_id = ? AND role_add_when_removed.server_id = ?");
        getRoleOnRemoveDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("role_remove_when_added.to_remove_id");
        qb.from("role_remove_when_added");
        qb.where("role_remove_when_added.role_id = ? AND role_remove_when_added.server_id = ?");
        getRoleOnAddDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.replaceInto("role(role_id, server_id, add_on_join, add_on_inactive)");
        qb.values("(?, ?, ?, ?)");
        setRoleDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.replaceInto("role_requestable(role_id, server_id, temp, agreement)");
        qb.values("(?, ?, ?, ?)");
        setRoleRequestDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("role_requestable");
        qb.where("role_requestable.role_id = ? AND role_requestable.server_id = ?");
        deleteRoleRequestDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("role_add_when_removed");
        qb.where("role_add_when_removed.role_id = ? AND role_add_when_removed.server_id = ?");
        clearRoleOnRemoveDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("role_remove_when_added");
        qb.where("role_remove_when_added.role_id = ? AND role_remove_when_added.server_id = ?");
        clearRoleOnAddDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.insertIgnoreInto("role_add_when_removed");
        qb.values("(?, ?, ?)");
        addRoleOnRemoveDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.insertIgnoreInto("role_remove_when_added");
        qb.values("(?, ?, ?)");
        addRoleOnAddDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("role");
        qb.where("role.role_id = ? AND role.server_id = ?");
        clearRoleDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("role.role_id");
        qb.from("role");
        qb.where("role.server_id = ? AND role.add_on_join = TRUE");
        getRolesToAddOnJoinStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("role.role_id");
        qb.from("role");
        qb.where("role.server_id = ? AND role.add_on_inactive = TRUE");
        getRolesToAddOnInactiveStmt = qb.build(connection);
    }

    public Data getRoleData(long roleId, long serverId) throws SQLException {
        synchronized (connection) {
            getRoleDataStmt.setLong(1, roleId);
            getRoleDataStmt.setLong(2, serverId);
            ResultSet rs = getRoleDataStmt.executeQuery();

            Data data = new Data(roleId, serverId);

            if (!rs.next()) {
                return data;
            }

            data.addJoin = rs.getBoolean("add_on_join");
            data.addInactive = rs.getBoolean("add_on_inactive");

            getRoleRequestDataStmt.setLong(1, roleId);
            getRoleRequestDataStmt.setLong(2, serverId);
            rs = getRoleRequestDataStmt.executeQuery();

            if (rs.next()) {
                data.requestable = new Data.Requestable(rs.getBoolean("temp"), rs.getString("agreement"));
            }

            getRoleOnRemoveDataStmt.setLong(1, roleId);
            getRoleOnRemoveDataStmt.setLong(2, serverId);
            rs = getRoleOnRemoveDataStmt.executeQuery();

            while (rs.next()) {
                data.addWhenRemoved.add(rs.getLong("to_add_id"));
            }

            getRoleOnAddDataStmt.setLong(1, roleId);
            getRoleOnAddDataStmt.setLong(2, serverId);
            rs = getRoleOnAddDataStmt.executeQuery();

            while (rs.next()) {
                data.removeWhenAdded.add(rs.getLong("to_remove_id"));
            }

            return data;
        }
    }

    public void saveRoleData(Data data) throws SQLException {
        synchronized (connection) {
            try {
                setRoleDataStmt.setLong(1, data.roleId);
                setRoleDataStmt.setLong(2, data.serverId);
                setRoleDataStmt.setBoolean(3, data.addJoin);
                setRoleDataStmt.setBoolean(4, data.addInactive);
                setRoleDataStmt.executeUpdate();

                if (data.requestable == null) {
                    deleteRoleRequestDataStmt.setLong(1, data.roleId);
                    deleteRoleRequestDataStmt.setLong(2, data.serverId);
                    deleteRoleRequestDataStmt.executeUpdate();
                } else {
                    setRoleRequestDataStmt.setLong(1, data.roleId);
                    setRoleRequestDataStmt.setLong(2, data.serverId);
                    setRoleRequestDataStmt.setBoolean(3, data.requestable.temp);
                    setRoleRequestDataStmt.setString(4, data.requestable.agreement);
                    setRoleRequestDataStmt.executeUpdate();
                }

                clearRoleOnRemoveDataStmt.setLong(1, data.roleId);
                clearRoleOnRemoveDataStmt.setLong(2, data.serverId);
                clearRoleOnRemoveDataStmt.executeUpdate();

                clearRoleOnAddDataStmt.setLong(1, data.roleId);
                clearRoleOnAddDataStmt.setLong(2, data.serverId);
                clearRoleOnAddDataStmt.executeUpdate();

                addRoleOnRemoveDataStmt.setLong(1, data.roleId);
                addRoleOnRemoveDataStmt.setLong(2, data.serverId);
                for (Long roleId : data.addWhenRemoved) {
                    addRoleOnRemoveDataStmt.setLong(3, roleId);
                    addRoleOnRemoveDataStmt.executeUpdate();
                }

                addRoleOnAddDataStmt.setLong(1, data.roleId);
                addRoleOnAddDataStmt.setLong(2, data.serverId);
                for (Long roleId : data.removeWhenAdded) {
                    addRoleOnAddDataStmt.setLong(3, roleId);
                    addRoleOnAddDataStmt.executeUpdate();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void deleteRoleData(long roleId, long serverId) throws SQLException {
        synchronized (connection) {
            clearRoleDataStmt.setLong(1, roleId);
            clearRoleDataStmt.setLong(2, serverId);
            clearRoleDataStmt.executeUpdate();
            connection.commit();
        }
    }

    public List<Long> getRolesToAddWhenRemoved(long roleId, long serverId) throws SQLException {
        synchronized (connection) {
            getRoleOnRemoveDataStmt.setLong(1, roleId);
            getRoleOnRemoveDataStmt.setLong(2, serverId);
            ResultSet rs = getRoleOnRemoveDataStmt.executeQuery();
            ArrayList<Long> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getLong("to_add_id"));
            }
            return ids;
        }
    }

    public List<Long> getRolesToRemoveWhenAdded(long roleId, long serverId) throws SQLException {
        synchronized (connection) {
            getRoleOnAddDataStmt.setLong(1, roleId);
            getRoleOnAddDataStmt.setLong(2, serverId);
            ResultSet rs = getRoleOnAddDataStmt.executeQuery();
            ArrayList<Long> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getLong("to_remove_id"));
            }
            return ids;
        }
    }

    public List<Long> getRolesToAddOnJoin(long serverId) throws SQLException {
        synchronized (connection) {
            getRolesToAddOnJoinStmt.setLong(1, serverId);
            ResultSet rs = getRolesToAddOnJoinStmt.executeQuery();
            ArrayList<Long> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getLong("role_id"));
            }
            return ids;
        }
    }

    public List<Long> getRolesToAddOnInactive(long serverId) throws SQLException {
        synchronized (connection) {
            getRolesToAddOnInactiveStmt.setLong(1, serverId);
            ResultSet rs = getRolesToAddOnInactiveStmt.executeQuery();
            ArrayList<Long> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getLong("role_id"));
            }
            return ids;
        }
    }

    public static InteractionApplicationCommandCallbackSpec buildMenu(Data data) {
        InteractionApplicationCommandCallbackSpec.Builder builder = InteractionApplicationCommandCallbackSpec.builder();
        builder.content("").ephemeral(true);
        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
        embed.title("Role settings for");
        embed.description("<@&%d>".formatted(data.roleId));

        RoleData.Data.Requestable requestable = data.requestable;

        embed.addField("Added on join", data.addJoin ? "Enabled" : "Disabled", true);
        embed.addField("Added on inactivity", data.addInactive ? "Enabled" : "Disabled", true);
        embed.addField("Requestable", requestable != null ? "Enabled" : "Disabled", true);

        builder.addComponent(ActionRow.of(
                data.addJoin ? Button.secondary("role:add_join:%s:false".formatted(data.uid), "Toggle adding to new members") :
                        Button.primary("role:add_join:%s:true".formatted(data.uid), "Toggle adding to new members"),
                data.addInactive ? Button.secondary("role:add_inactive:%s:false".formatted(data.uid), "Toggle adding to inactive members") :
                        Button.primary("role:add_inactive:%s:true".formatted(data.uid), "Toggle adding to inactive members"),
                data.requestable != null ? Button.secondary("role:requestable:%s:false".formatted(data.uid), "Toggle requesting") :
                        Button.primary("role:requestable:%s:true".formatted(data.uid), "Toggle requesting")
        ));

        if (requestable != null) {
            embed.addField("Require time parameter", requestable.temp ? "Enabled" : "Disabled", true);
            embed.addField("Agreement", Objects.requireNonNullElse(requestable.agreement, "N/A"), true);

            builder.addComponent(ActionRow.of(
                    requestable.temp ? Button.secondary("role:temp:%s:false".formatted(data.uid), "Toggle time parameter") :
                            Button.primary("role:temp:%s:true".formatted(data.uid), "Toggle time parameter"),
                    Button.primary("role:agreement:%s".formatted(data.uid), "Set agreement")
            ));
        }

        embed.addField("Roles to add if removed", data.addWhenRemoved.isEmpty() ? "None" :
                data.addWhenRemoved.stream().map("<@&%d>"::formatted).collect(Collectors.joining(", ")), false);
        embed.addField("Roles to remove if added", data.removeWhenAdded.isEmpty() ? "None" :
                data.removeWhenAdded.stream().map("<@&%d>"::formatted).collect(Collectors.joining(", ")), false);

        builder.addComponent(ActionRow.of(
                Button.primary("role:on_remove:%s".formatted(data.uid), "Set roles to add if removed"),
                Button.primary("role:on_add:%s".formatted(data.uid), "Set roles to remove if added")
        ));

        builder.addComponent(ActionRow.of(
                Button.success("role:save:%s".formatted(data.uid), "Save"),
                Button.danger("role:clear:%s".formatted(data.uid), "Clear data")
        ));

        return builder.addEmbed(embed.build()).build();
    }

    private final HashMap<Integer, Data> tempData = new HashMap<>();
    private final HashMap<Key, Integer> tempKeys = new HashMap<>();

    public Data getTempData(int uid) {
        synchronized (tempData) {
            return tempData.get(uid);
        }
    }

    public void removeTempData(Data data) {
        synchronized (tempData) {
            tempKeys.remove(new Key(data.roleId, data.serverId));
            tempData.remove(data.uid);
            data.removalTask.cancel(false);
        }
    }

    public void putTempData(Data data) {
        synchronized (tempData) {
            Key key = new Key(data.roleId, data.serverId);
            if (tempKeys.containsKey(key)) {
                int uid = tempKeys.remove(key);
                Data temp = tempData.remove(uid);
                if (temp != null) {
                    temp.removalTask.cancel(false);
                }
            }

            data.removalTask = TaskManager.service.schedule(() -> {
                tempData.remove(data.uid);
                tempKeys.remove(key);
            }, 15, TimeUnit.MINUTES);

            tempKeys.put(key, data.uid);
            tempData.put(data.uid, data);
        }
    }

    private record Key(long roleId, long serverId) {}

    public static class Data {
        private static int nextUid;

        public final int uid;
        public long roleId;
        public long serverId;
        public boolean addJoin;
        public boolean addInactive;
        public Requestable requestable;
        public final List<Long> addWhenRemoved = new ArrayList<>();
        public final List<Long> removeWhenAdded = new ArrayList<>();

        private ScheduledFuture<?> removalTask;

        public Data(long roleId, long serverId) {
            this.roleId = roleId;
            this.serverId = serverId;
            uid = nextUid++;
        }

        public static class Requestable {
            public boolean temp;
            public String agreement;

            public Requestable(boolean temp, String agreement) {
                this.temp = temp;
                this.agreement = agreement;
            }
        }
    }
}

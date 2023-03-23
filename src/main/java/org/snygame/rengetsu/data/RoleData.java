package org.snygame.rengetsu.data;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.snygame.rengetsu.tasks.TaskManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RoleData {
    private static Connection connection;

    private static PreparedStatement getRoleDataStmt;
    private static PreparedStatement getRoleRequestDataStmt;
    private static PreparedStatement getRoleOnRemoveDataStmt;
    private static PreparedStatement getRoleOnAddDataStmt;

    private static PreparedStatement setRoleDataStmt;
    private static PreparedStatement setRoleRequestDataStmt;
    private static PreparedStatement deleteRoleRequestDataStmt;
    private static PreparedStatement clearRoleOnRemoveDataStmt;
    private static PreparedStatement clearRoleOnAddDataStmt;
    private static PreparedStatement addRoleOnRemoveDataStmt;
    private static PreparedStatement addRoleOnAddDataStmt;

    private static PreparedStatement clearRoleDataStmt;

    private static PreparedStatement getRolesToAddOnJoinStmt;
    private static PreparedStatement getRolesToAddOnInactiveStmt;

    static void initializeStatements(Connection connection) throws SQLException {
        RoleData.connection = connection;

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

    public static Data getRoleData(long roleId, long serverId) throws SQLException {
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

    public static void saveRoleData(Data data) throws SQLException {
        try {
            ServerData.initializeServer(data.serverId);

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

    public static void deleteRoleData(long roleId, long serverId) throws SQLException {
        clearRoleDataStmt.setLong(1, roleId);
        clearRoleDataStmt.setLong(2, serverId);
        clearRoleDataStmt.executeUpdate();
        connection.commit();
    }

    public static List<Long> getRolesToAddWhenRemoved(long roleId, long serverId) throws SQLException {
        getRoleOnRemoveDataStmt.setLong(1, roleId);
        getRoleOnRemoveDataStmt.setLong(2, serverId);
        ResultSet rs = getRoleOnRemoveDataStmt.executeQuery();
        ArrayList<Long> ids = new ArrayList<>();
        while (rs.next()) {
            ids.add(rs.getLong("to_add_id"));
        }
        return ids;
    }

    public static List<Long> getRolesToRemoveWhenAdded(long roleId, long serverId) throws SQLException {
        getRoleOnAddDataStmt.setLong(1, roleId);
        getRoleOnAddDataStmt.setLong(2, serverId);
        ResultSet rs = getRoleOnAddDataStmt.executeQuery();
        ArrayList<Long> ids = new ArrayList<>();
        while (rs.next()) {
            ids.add(rs.getLong("to_remove_id"));
        }
        return ids;
    }

    public static List<Long> getRolesToAddOnJoin(long serverId) throws SQLException {
        getRolesToAddOnJoinStmt.setLong(1, serverId);
        ResultSet rs = getRolesToAddOnJoinStmt.executeQuery();
        ArrayList<Long> ids = new ArrayList<>();
        while (rs.next()) {
            ids.add(rs.getLong("role_id"));
        }
        return ids;
    }

    public static List<Long> getRolesToAddOnInactive(long serverId) throws SQLException {
        getRolesToAddOnInactiveStmt.setLong(1, serverId);
        ResultSet rs = getRolesToAddOnInactiveStmt.executeQuery();
        ArrayList<Long> ids = new ArrayList<>();
        while (rs.next()) {
            ids.add(rs.getLong("role_id"));
        }
        return ids;
    }

    public static InteractionApplicationCommandCallbackSpec buildMenu(Data roleData) {
        InteractionApplicationCommandCallbackSpec.Builder builder = InteractionApplicationCommandCallbackSpec.builder();
        builder.content("");
        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
        embed.title("Role settings for");
        embed.description("<@&%d>".formatted(roleData.roleId));

        RoleData.Data.Requestable requestable = roleData.requestable;

        String key = "%d:%d".formatted(roleData.roleId, roleData.serverId);

        embed.addField("Added on join", roleData.addJoin ? "Enabled" : "Disabled", true);
        embed.addField("Added on inactivity", roleData.addInactive ? "Enabled" : "Disabled", true);
        embed.addField("Requestable", requestable != null ? "Enabled" : "Disabled", true);

        builder.addComponent(ActionRow.of(
                roleData.addJoin ? Button.danger("role:%s:add_join:false".formatted(key), "Disable adding to new members") :
                        Button.success("role:%s:add_join:true".formatted(key), "Enable adding to new members"),
                roleData.addInactive ? Button.danger("role:%s:add_inactive:false".formatted(key), "Disable adding to inactive members") :
                        Button.success("role:%s:add_inactive:true".formatted(key), "Enable adding to inactive members"),
                roleData.requestable != null ? Button.danger("role:%s:requestable:false".formatted(key), "Disable requesting") :
                        Button.success("role:%s:requestable:true".formatted(key), "Enable requesting")
        ));

        if (requestable != null) {
            embed.addField("Require time parameter", requestable.temp ? "Enabled" : "Disabled", true);
            embed.addField("Agreement", Objects.requireNonNullElse(requestable.agreement, "N/A"), true);

            builder.addComponent(ActionRow.of(
                    requestable.temp ? Button.danger("role:%s:temp:false".formatted(key), "Disable time parameter") :
                            Button.success("role:%s:temp:true".formatted(key), "Enable time parameter"),
                    Button.primary("role:%s:agreement".formatted(key), "Set agreement")
            ));
        }

        embed.addField("Roles to add if removed", roleData.addWhenRemoved.isEmpty() ? "None" :
                roleData.addWhenRemoved.stream().map("<@&%d>"::formatted).collect(Collectors.joining(", ")), false);
        embed.addField("Roles to remove if added", roleData.removeWhenAdded.isEmpty() ? "None" :
                roleData.removeWhenAdded.stream().map("<@&%d>"::formatted).collect(Collectors.joining(", ")), false);

        builder.addComponent(ActionRow.of(
                Button.primary("role:%s:on_remove".formatted(key), "Set roles to add if removed"),
                Button.primary("role:%s:on_add".formatted(key), "Set roles to remove if added")
        ));

        builder.addComponent(ActionRow.of(
                Button.success("role:%s:save".formatted(key), "Save"),
                Button.danger("role:%s:no_save".formatted(key), "Cancel"),
                Button.danger("role:%s:clear".formatted(key), "Clear data")
        ));

        return builder.addEmbed(embed.build()).build();
    }

    private static final HashMap<Key, Data> tempData = new HashMap<>();

    public static Data getTempData(long roleId, long serverId) {
        return tempData.get(new Key(roleId, serverId));
    }

    public static void removeTempData(Data data) {
        tempData.remove(new Key(data.roleId, data.serverId));
        data.removalTask.cancel(false);
    }

    public static void putTempData(Data data) {
        data.removalTask = TaskManager.service.schedule(() -> tempData.remove(new Key(data.roleId, data.serverId)), 30, TimeUnit.MINUTES);
        tempData.put(new Key(data.roleId, data.serverId), data);
    }

    private record Key(long roleId, long serverId) {}

    public static class Data {
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

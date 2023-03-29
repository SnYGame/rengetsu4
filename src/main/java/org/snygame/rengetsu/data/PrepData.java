package org.snygame.rengetsu.data;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.tasks.TaskManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PrepData extends TableData {
    private final PreparedStatement setPrepDataStmt;
    private final PreparedStatement clearPrepRollsStmt;
    private final PreparedStatement addPrepDicerollsStmt;
    private final PreparedStatement addPrepCalculationStmt;
    private final PreparedStatement deletePrepDataStmt;

    private final PreparedStatement hasPrepDataStmt;
    private final PreparedStatement getPrepDataStmt;
    private final PreparedStatement getPrepRollStmt;
    private final PreparedStatement listPrepNamesStmt;

    PrepData(Rengetsu rengetsu, Connection connection) throws SQLException {
        super(rengetsu, connection);

        QueryBuilder qb;
        qb = new QueryBuilder();
        qb.replaceInto("prep(user_id, key, name, descr, roll_count)");
        qb.values("(?, ?, ?, ?, ?)");
        setPrepDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("prep_roll");
        qb.where("user_id = ? AND key = ?");
        clearPrepRollsStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.replaceInto("prep_roll(user_id, key, pos, descr, query)");
        qb.values("(?, ?, ?, ?, ?)");
        addPrepDicerollsStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.replaceInto("prep_roll(user_id, key, pos, descr, query, bytecode)");
        qb.values("(?, ?, ?, ?, ?, ?)");
        addPrepCalculationStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("prep");
        qb.where("user_id = ? AND key = ?");
        deletePrepDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("COUNT(*) AS cnt");
        qb.from("prep");
        qb.where("prep.user_id = ? AND prep.key = ?");
        hasPrepDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("prep.name, prep.descr, prep.roll_count");
        qb.from("prep");
        qb.where("prep.user_id = ? AND prep.key = ?");
        getPrepDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("prep_roll.descr, prep_roll.query, prep_roll.bytecode");
        qb.from("prep_roll");
        qb.where("prep_roll.user_id = ? AND prep_roll.key = ? AND prep_roll.pos = ?");
        getPrepRollStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("prep.key, prep.name");
        qb.from("prep");
        qb.where("prep.user_id = ?");
        listPrepNamesStmt = qb.build(connection);
    }

    public void savePrepData(Data data) throws SQLException {
        synchronized (connection) {
            try {
                DatabaseManager databaseManager = rengetsu.getDatabaseManager();
                databaseManager.getUserData().initializeUser(data.userId);

                setPrepDataStmt.setLong(1, data.userId);
                setPrepDataStmt.setString(2, data.key);
                setPrepDataStmt.setString(3, data.name);
                setPrepDataStmt.setString(4, data.description);
                setPrepDataStmt.setInt(5, data.dicerolls.size());
                setPrepDataStmt.executeUpdate();

                clearPrepRollsStmt.setLong(1, data.userId);
                clearPrepRollsStmt.setString(2, data.key);
                clearPrepRollsStmt.executeUpdate();

                for (int i = 0; i < data.dicerolls.size(); i++) {
                    switch (data.dicerolls.get(i)) {
                        case Data.DicerollData dicerollData -> {
                            addPrepDicerollsStmt.setLong(1, data.userId);
                            addPrepDicerollsStmt.setString(2, data.key);
                            addPrepDicerollsStmt.setInt(3, i);
                            addPrepDicerollsStmt.setString(4, dicerollData.description);
                            addPrepDicerollsStmt.setString(5, dicerollData.query);
                            addPrepDicerollsStmt.executeUpdate();
                        }
                        case Data.CalculationData calculationData -> {
                            addPrepCalculationStmt.setLong(1, data.userId);
                            addPrepCalculationStmt.setString(2, data.key);
                            addPrepCalculationStmt.setInt(3, i);
                            addPrepCalculationStmt.setString(4, calculationData.description);
                            addPrepCalculationStmt.setString(5, calculationData.query);
                            addPrepCalculationStmt.setBytes(6, calculationData.bytecode);
                            addPrepCalculationStmt.executeUpdate();
                        }
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void deletePrepData(long userId, String key) throws SQLException {
        synchronized (connection) {
            deletePrepDataStmt.setLong(1, userId);
            deletePrepDataStmt.setString(2, key);
            deletePrepDataStmt.executeUpdate();
            connection.commit();
        }
    }

    public boolean hasPrepData(long userId, String key) throws SQLException {
        synchronized (connection) {
            hasPrepDataStmt.setLong(1, userId);
            hasPrepDataStmt.setString(2, key);
            ResultSet rs = hasPrepDataStmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("cnt") > 0;
            }
            throw new RuntimeException();
        }
    }

    public Data getPrepData(long userId, String key) throws SQLException {
        synchronized (connection) {
            getPrepDataStmt.setLong(1, userId);
            getPrepDataStmt.setString(2, key);
            ResultSet rs = getPrepDataStmt.executeQuery();
            if (!rs.next()) {
                return null;
            }
            Data prepData = new Data(userId, key);
            prepData.name = rs.getString("name");
            prepData.description = rs.getString("descr");
            int rollCount = rs.getInt("roll_count");

            getPrepRollStmt.setLong(1, userId);
            getPrepRollStmt.setString(2, key);
            for (int i = 0; i < rollCount; i++) {
                getPrepRollStmt.setInt(3, i);
                rs = getPrepRollStmt.executeQuery();

                if (!rs.next()) {
                    throw new RuntimeException();
                }

                byte[] bytecode = rs.getBytes("bytecode");

                if (bytecode == null) {
                    prepData.dicerolls.add(new Data.DicerollData(rs.getString("descr"),
                            rs.getString("query")));
                } else {
                    prepData.dicerolls.add(new Data.CalculationData(rs.getString("descr"),
                            rs.getString("query"), bytecode));
                }
            }

            return prepData;
        }
    }

    public List<NameData> listPrepNames(long userId) throws SQLException {
        synchronized (connection) {
            listPrepNamesStmt.setLong(1, userId);
            ResultSet rs = listPrepNamesStmt.executeQuery();

            ArrayList<NameData> names = new ArrayList<>();
            while (rs.next()) {
                names.add(new NameData(rs.getString("key"), rs.getString("name")));
            }
            return names;
        }
    }

    public static InteractionApplicationCommandCallbackSpec buildMenu(Data prepData) {
        InteractionApplicationCommandCallbackSpec.Builder builder = InteractionApplicationCommandCallbackSpec.builder();
        builder.content("Key: %s".formatted(prepData.key));
        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
        embed.title(prepData.name);
        embed.description(prepData.description);
        embed.fields(prepData.dicerolls.stream().map(rollData ->
                EmbedCreateFields.Field.of(rollData.description,
                        rollData.query, true)).toList());

        String key = "%d:%s".formatted(prepData.userId, prepData.key);

        builder.addComponent(ActionRow.of(
                Button.primary("prep:%s:edit".formatted(key), "Edit description")
        ));

        builder.addComponent(ActionRow.of(
                Button.primary("prep:%s:add_roll".formatted(key), "Add diceroll"),
                Button.primary("prep:%s:add_calc".formatted(key), "Add calculation"),
                Button.primary("prep:%s:del_roll".formatted(key), "Remove dicerolls/calculations")
                        .disabled(prepData.dicerolls.isEmpty())
        ));

        builder.addComponent(ActionRow.of(
                Button.success("prep:%s:save".formatted(key), "Save"),
                Button.danger("prep:%s:no_save".formatted(key), "Cancel"),
                Button.danger("prep:%s:delete".formatted(key), "Delete")
        ));

        return builder.addEmbed(embed.build()).build();
    }

    private final HashMap<Key, Data> tempData = new HashMap<>();

    public Data getTempData(long userId, String key) {
        return tempData.get(new Key(userId, key));
    }

    public void removeTempData(Data data) {
        tempData.remove(new Key(data.userId, data.key));
        data.removalTask.cancel(false);
    }

    public void putTempData(Data data) {
        Key key = new Key(data.userId, data.key);
        if (tempData.containsKey(key)) {
            tempData.remove(key).removalTask.cancel(false);
        }

        data.removalTask = TaskManager.service.schedule(() -> tempData.remove(key), 30, TimeUnit.MINUTES);
        tempData.put(key, data);
    }

    public record NameData(String key, String name) {}

    private record Key(long userId, String key) {}

    public static class Data {
        public long userId;
        public String key;
        public String name;
        public String description;
        public final List<RollData> dicerolls = new ArrayList<>();

        private ScheduledFuture<?> removalTask;

        public Data(long userId, String key) {
            this.userId = userId;
            this.key = key;
        }

        public static sealed abstract class RollData permits DicerollData, CalculationData {
            public String description;
            public String query;

            public RollData(String description, String query) {
                this.description = description;
                this.query = query;
            }
        }

        public static final class DicerollData extends RollData {
            public DicerollData(String description, String query) {
                super(description, query);
            }
        }

        public static final class CalculationData extends RollData {
            public byte[] bytecode;

            public CalculationData(String description, String query, byte[] bytecode) {
                super(description, query);
                this.bytecode = bytecode;
            }
        }
    }
}

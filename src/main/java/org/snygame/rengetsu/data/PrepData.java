package org.snygame.rengetsu.data;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.antlr.v4.runtime.*;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.parser.RengCalcLexer;
import org.snygame.rengetsu.parser.RengCalcParser;
import org.snygame.rengetsu.tasks.TaskManager;
import org.snygame.rengetsu.util.math.ASTGenerator;
import org.snygame.rengetsu.util.math.ASTNode;
import org.snygame.rengetsu.util.math.Type;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PrepData extends TableData {
    private final PreparedStatement setPrepDataStmt;
    private final PreparedStatement clearPrepRollsStmt;
    private final PreparedStatement clearPrepParamsStmt;
    private final PreparedStatement addPrepDicerollsStmt;
    private final PreparedStatement addPrepCalculationStmt;
    private final PreparedStatement addPrepParamStmt;
    private final PreparedStatement deletePrepDataStmt;

    private final PreparedStatement hasPrepDataStmt;
    private final PreparedStatement getPrepDataStmt;
    private final PreparedStatement getPrepRollStmt;
    private final PreparedStatement getPrepParamStmt;
    private final PreparedStatement listPrepNamesStmt;

    PrepData(Rengetsu rengetsu, Connection connection) throws SQLException {
        super(rengetsu, connection);

        QueryBuilder qb;
        qb = new QueryBuilder();
        qb.replaceInto("prep(user_id, key, name, descr, roll_count, var_count, param_count)");
        qb.values("(?, ?, ?, ?, ?, ?, ?)");
        setPrepDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("prep_roll");
        qb.where("user_id = ? AND key = ?");
        clearPrepRollsStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("prep_param");
        qb.where("user_id = ? AND key = ?");
        clearPrepParamsStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.replaceInto("prep_roll(user_id, key, pos, descr, query, variable, result)");
        qb.values("(?, ?, ?, ?, ?, ?, ?)");
        addPrepDicerollsStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.replaceInto("prep_roll(user_id, key, pos, descr, query, bytecode)");
        qb.values("(?, ?, ?, ?, ?, ?)");
        addPrepCalculationStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.replaceInto("prep_param(user_id, key, pos, name, fixed_type, var_type, result)");
        qb.values("(?, ?, ?, ?, ?, ?, ?)");
        addPrepParamStmt = qb.build(connection);

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
        qb.select("prep.name, prep.descr, prep.roll_count, prep.var_count, prep.param_count");
        qb.from("prep");
        qb.where("prep.user_id = ? AND prep.key = ?");
        getPrepDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("prep_roll.descr, prep_roll.query, prep_roll.bytecode, prep_roll.variable, prep_roll.result");
        qb.from("prep_roll");
        qb.where("prep_roll.user_id = ? AND prep_roll.key = ? AND prep_roll.pos = ?");
        getPrepRollStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("prep_param.name, prep_param.fixed_type, prep_param.var_type, prep_param.result");
        qb.from("prep_param");
        qb.where("prep_param.user_id = ? AND prep_param.key = ? AND prep_param.pos = ?");
        getPrepParamStmt = qb.build(connection);

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
                setPrepDataStmt.setInt(6, data.varCount);
                setPrepDataStmt.setInt(7, data.params.length);
                setPrepDataStmt.executeUpdate();

                clearPrepRollsStmt.setLong(1, data.userId);
                clearPrepRollsStmt.setString(2, data.key);
                clearPrepRollsStmt.executeUpdate();

                clearPrepParamsStmt.setLong(1, data.userId);
                clearPrepParamsStmt.setString(2, data.key);
                clearPrepParamsStmt.executeUpdate();


                addPrepDicerollsStmt.setLong(1, data.userId);
                addPrepDicerollsStmt.setString(2, data.key);
                addPrepCalculationStmt.setLong(1, data.userId);
                addPrepCalculationStmt.setString(2, data.key);

                for (int i = 0; i < data.dicerolls.size(); i++) {
                    switch (data.dicerolls.get(i)) {
                        case Data.DicerollData dicerollData -> {
                            addPrepDicerollsStmt.setInt(3, i);
                            addPrepDicerollsStmt.setString(4, dicerollData.description);
                            addPrepDicerollsStmt.setString(5, dicerollData.query);
                            addPrepDicerollsStmt.setString(6, dicerollData.variable);
                            addPrepDicerollsStmt.setByte(7, dicerollData.result);
                            addPrepDicerollsStmt.executeUpdate();
                        }
                        case Data.CalculationData calculationData -> {
                            addPrepCalculationStmt.setInt(3, i);
                            addPrepCalculationStmt.setString(4, calculationData.description);
                            addPrepCalculationStmt.setString(5, calculationData.query);
                            addPrepCalculationStmt.setBytes(6, calculationData.bytecode);
                            addPrepCalculationStmt.executeUpdate();
                        }
                    }
                }

                addPrepParamStmt.setLong(1, data.userId);
                addPrepParamStmt.setString(2, data.key);
                for (int i = 0; i < data.parameterData.size(); i++) {
                    Data.ParameterData parameterData = data.parameterData.get(i);
                    addPrepParamStmt.setInt(3, i);
                    addPrepParamStmt.setString(4, parameterData.name);
                    addPrepParamStmt.setInt(5, parameterData.type.ordinal());
                    addPrepParamStmt.setByte(6, parameterData.ofVarType);
                    addPrepParamStmt.setByte(7, parameterData.result);
                    addPrepParamStmt.executeUpdate();
                }
                connection.commit();
                clearCacheIf(data.userId);
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public boolean deletePrepData(long userId, String key) throws SQLException {
        synchronized (connection) {
            deletePrepDataStmt.setLong(1, userId);
            deletePrepDataStmt.setString(2, key);
            int rows = deletePrepDataStmt.executeUpdate();
            connection.commit();
            clearCacheIf(userId);
            return rows > 0;
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
            prepData.varCount = rs.getInt("var_count");
            int paramCount = rs.getInt("param_count");

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
                            rs.getString("query"), rs.getString("variable"),
                            rs.getByte("result")));
                } else {
                    prepData.dicerolls.add(new Data.CalculationData(rs.getString("descr"),
                            rs.getString("query"), bytecode));
                }
            }
            getPrepParamStmt.setLong(1, userId);
            getPrepParamStmt.setString(2, key);
            for (int i = 0; i < paramCount; i++) {
                getPrepParamStmt.setInt(3, i);
                rs = getPrepParamStmt.executeQuery();

                if (!rs.next()) {
                    throw new RuntimeException();
                }

                prepData.parameterData.add(new Data.ParameterData(rs.getString("name"),
                        Type.FixedType.values()[rs.getInt("fixed_type")],
                        rs.getByte("var_type"), rs.getByte("result")));
            }
            prepData.params = prepData.parameterData.stream().map(Data.ParameterData::name).toList().toArray(new String[paramCount]);

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

    public static InteractionApplicationCommandCallbackSpec buildMenu(Data data) {
        InteractionApplicationCommandCallbackSpec.Builder builder = InteractionApplicationCommandCallbackSpec.builder();
        builder.content("Key: %s".formatted(data.key)).ephemeral(true);
        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
        embed.title(data.name);
        embed.description(data.description);
        if (data.params.length > 0) {
            embed.addField("Parameters", String.join(", ", data.params), false);
        }
        for (Data.RollData rollData: data.dicerolls) {
            if (rollData instanceof Data.DicerollData diceroll && diceroll.variable != null) {
                embed.addField(rollData.description, "%s = %s".formatted(diceroll.variable, rollData.query), false);
            } else {
                embed.addField(rollData.description, rollData.query, false);
            }
        }

        builder.addComponent(ActionRow.of(
                Button.primary("prep:edit:%d".formatted(data.uid), "Edit description"),
                Button.primary("prep:params:%d".formatted(data.uid), "Edit parameters")
        ));

        builder.addComponent(ActionRow.of(
                Button.primary("prep:add_roll:%d".formatted(data.uid), "Add dice roll"),
                Button.primary("prep:add_calc:%d".formatted(data.uid), "Add calculation"),
                Button.primary("prep:del_roll:%d".formatted(data.uid), "Remove dice rolls/calculations")
                        .disabled(data.dicerolls.isEmpty())
        ));

        if (data.editing) {
            builder.addComponent(ActionRow.of(
                    Button.success("prep:save:%d".formatted(data.uid), "Save"),
                    Button.danger("prep:delete:%d".formatted(data.uid), "Delete")
            ));
        } else {
            builder.addComponent(ActionRow.of(
                    Button.success("prep:save:%d".formatted(data.uid), "Save")
            ));
        }

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
            tempKeys.remove(new Key(data.userId, data.key));
            tempData.remove(data.uid);
            data.removalTask.cancel(false);
        }
    }

    public void putTempData(Data data) {
        synchronized (tempData) {
            Key key = new Key(data.userId, data.key);
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

    public record NameData(String key, String name) {}

    private record Key(long userId, String key) {}

    public static class Data {
        private static int nextUid;

        public final int uid;
        public long userId;
        public String key;
        public String name;
        public String description;
        public String[] params = new String[0];
        public boolean editing = true;
        public final ArrayList<RollData> dicerolls = new ArrayList<>();
        public final ArrayList<ParameterData> parameterData = new ArrayList<>();
        public int varCount;

        private ScheduledFuture<?> removalTask;

        public Data(long userId, String key) {
            this.userId = userId;
            this.key = key;

            uid = nextUid++;
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
            public String variable;
            public byte result;

            public DicerollData(String description, String query, String variable) {
                super(description, query);
                this.variable = variable;
            }

            public DicerollData(String description, String query, String variable, byte result) {
                super(description, query);
                this.variable = variable;
                this.result = result;
            }
        }

        public static final class CalculationData extends RollData {
            private ASTNode ast;
            public byte[] bytecode;

            public CalculationData(String description, String query, ASTNode ast) {
                super(description, query);
                this.ast = ast;
            }

            public CalculationData(String description, String query, byte[] bytecode) {
                super(description, query);
                this.bytecode = bytecode;
            }

            public ASTNode getAst() {
                if (ast == null) {
                    RengCalcLexer lexer = new RengCalcLexer(CharStreams.fromString(query));
                    List<String> errors = new ArrayList<>();
                    ANTLRErrorListener listener = new BaseErrorListener() {
                        @Override
                        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                            errors.add("%d: %s\n".formatted(charPositionInLine, msg));
                        }
                    };
                    lexer.removeErrorListeners();
                    lexer.addErrorListener(listener);

                    RengCalcParser parser = new RengCalcParser(new CommonTokenStream(lexer));
                    parser.removeErrorListeners();
                    parser.addErrorListener(listener);

                    RengCalcParser.CalculationContext pt = parser.calculation();

                    if (!errors.isEmpty()) {
                        throw new IllegalStateException("Syntax error(s) found in stored query\n" + String.join("\n", errors));
                    }

                    ast = new ASTGenerator().visit(pt);
                }
                return ast;
            }
        }

        public record ParameterData(String name, Type.FixedType type, byte ofVarType, byte result) {}
    }

    private record AutoCompleteCache(long userId, List<NameData> nameData) {}
    private AutoCompleteCache autoCompleteCache = null;

    private void clearCacheIf(long userId) {
        AutoCompleteCache cache = autoCompleteCache;
        if (cache != null && cache.userId == userId) {
            autoCompleteCache = null;
        }
    }

    public List<NameData> getAutoCompleteData(long userId) throws SQLException {
        AutoCompleteCache cache = autoCompleteCache;
        if (cache != null && cache.userId == userId) {
            return cache.nameData;
        }

        List<NameData> nameData = listPrepNames(userId);
        autoCompleteCache = new AutoCompleteCache(userId, nameData);
        return nameData;
    }
}

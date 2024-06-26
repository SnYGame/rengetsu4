package org.snygame.rengetsu.data;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.tasks.TaskManager;
import org.snygame.rengetsu.util.math.ASTGenerator;
import org.snygame.rengetsu.util.math.ASTNode;
import org.snygame.rengetsu.util.math.Parser;
import org.snygame.rengetsu.util.math.Type;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;
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
    private final PreparedStatement getPrepRollStmt;
    private final PreparedStatement getPrepParamStmt;
    private final PreparedStatement listPrepNamesStmt;

    private final PreparedStatement createNamespaceStmt;
    private final PreparedStatement listNamespacesStmt;
    private final PreparedStatement deleteNamespaceStmt;
    private final PreparedStatement getNamespaceExistStmt;
    private final PreparedStatement renameNamespaceStmt;

    private final PreparedStatement listLoadedPrepNamesStmt;
    private final PreparedStatement getLoadedPrepDataStmt;
    private final PreparedStatement getNamespaceLoadedStmt;
    private final PreparedStatement setNamespaceLoadedStmt;
    private final PreparedStatement getPrepDataStmt;
    private final PreparedStatement changePrepNamespaceStmt;
    private final PreparedStatement checkLoadConflictStmt;
    private final PreparedStatement importNamespaceStmt;
    private final PreparedStatement getImportedPrepDataStmt;

    PrepData(Rengetsu rengetsu, Connection connection) throws SQLException {
        super(rengetsu, connection);

        QueryBuilder qb;
        qb = new QueryBuilder();
        qb.replaceInto("prep(user_id, namespace, key, name, descr, roll_count, var_count, param_count)");
        qb.values("(?, ?, ?, ?, ?, ?, ?, ?)");
        setPrepDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("prep_roll");
        qb.where("user_id = ? AND namespace = ? AND key = ?");
        clearPrepRollsStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("prep_param");
        qb.where("user_id = ? AND namespace = ? AND key = ?");
        clearPrepParamsStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.replaceInto("prep_roll(user_id, namespace, key, pos, descr, query, variable, result)");
        qb.values("(?, ?, ?, ?, ?, ?, ?, ?)");
        addPrepDicerollsStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.replaceInto("prep_roll(user_id, namespace, key, pos, descr, query, bytecode)");
        qb.values("(?, ?, ?, ?, ?, ?, ?)");
        addPrepCalculationStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.replaceInto("prep_param(user_id, namespace, key, pos, name, fixed_type, var_type, result)");
        qb.values("(?, ?, ?, ?, ?, ?, ?, ?)");
        addPrepParamStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("prep");
        qb.where("user_id = ? AND namespace = ? AND key = ?");
        deletePrepDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("COUNT(*) AS cnt");
        qb.from("prep");
        qb.where("prep.user_id = ? AND namespace = ? AND prep.key = ?");
        hasPrepDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("prep_roll.descr, prep_roll.query, prep_roll.bytecode, prep_roll.variable, prep_roll.result");
        qb.from("prep_roll");
        qb.where("prep_roll.user_id = ? AND prep_roll.namespace = ? AND prep_roll.key = ? AND prep_roll.pos = ?");
        getPrepRollStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("prep_param.name, prep_param.fixed_type, prep_param.var_type, prep_param.result");
        qb.from("prep_param");
        qb.where("prep_param.user_id = ? AND prep_param.namespace = ? AND prep_param.key = ? AND prep_param.pos = ?");
        getPrepParamStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("prep.key, prep.name");
        qb.from("prep");
        qb.where("prep.user_id = ? AND prep.namespace = ?");
        listPrepNamesStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.insertIgnoreInto("prep_namespace(user_id, key)");
        qb.values("(?, ?)");
        createNamespaceStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("prep_namespace.key, prep_namespace.loaded");
        qb.from("prep_namespace");
        qb.where("prep_namespace.user_id = ? AND NOT prep_namespace.key = ''");
        listNamespacesStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.deleteFrom("prep_namespace");
        qb.where("prep_namespace.user_id = ? AND prep_namespace.key = ?");
        deleteNamespaceStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("COUNT(*) as cnt");
        qb.from("prep_namespace");
        qb.where("prep_namespace.user_id = ? AND prep_namespace.key = ?");
        getNamespaceExistStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.update("prep_namespace");
        qb.set("key = ?");
        qb.where("prep_namespace.user_id = ? AND prep_namespace.key = ?");
        renameNamespaceStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("prep.key, prep.name, prep.namespace");
        qb.from("prep LEFT JOIN prep_namespace ON prep.user_id = prep_namespace.user_id AND prep.namespace = prep_namespace.key");
        qb.where("prep.user_id = ? AND (prep_namespace.loaded = TRUE OR prep.namespace = '')");
        listLoadedPrepNamesStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("prep.name, prep.namespace, prep.descr, prep.roll_count, prep.var_count, prep.param_count");
        qb.from("prep LEFT JOIN prep_namespace ON prep.user_id = prep_namespace.user_id AND prep.namespace = prep_namespace.key");
        qb.where("prep.user_id = ? AND prep.key = ? AND (prep_namespace.loaded = TRUE OR prep.namespace = '')");
        getLoadedPrepDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("prep_namespace.loaded");
        qb.from("prep_namespace");
        qb.where("prep_namespace.user_id = ? AND prep_namespace.key = ?");
        getNamespaceLoadedStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.update("prep_namespace");
        qb.set("loaded = ?");
        qb.where("prep_namespace.user_id = ? AND prep_namespace.key = ?");
        setNamespaceLoadedStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("prep.name, prep.namespace, prep.descr, prep.roll_count, prep.var_count, prep.param_count");
        qb.from("prep");
        qb.where("prep.user_id = ? AND prep.namespace = ? AND prep.key = ?");
        getPrepDataStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.update("prep");
        qb.set("namespace = ?");
        qb.where("prep.user_id = ? AND prep.namespace = ? AND prep.key = ?");
        changePrepNamespaceStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("COUNT(*) > 1 AS conflict");
        qb.from("prep LEFT JOIN prep_namespace ON prep.user_id = prep_namespace.user_id AND prep.namespace = prep_namespace.key");
        qb.where("prep.user_id = ? AND prep.key = ? AND (prep_namespace.loaded = TRUE OR prep.namespace = '')");
        checkLoadConflictStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.insertIgnoreInto("prep_namespace_import(user_id, key, borrow_id, borrow_key)");
        qb.values(("(?, ?, ?, ?)"));
        importNamespaceStmt = qb.build(connection);

        qb = new QueryBuilder();
        qb.select("prep.name, prep_namespace_import.key AS namespace, prep.descr, prep.roll_count, prep.var_count, prep.param_count");
        qb.from("prep_namespace_import INNER JOIN prep ON prep_namespace_import.borrow_id = prep.user_id AND " +
                "prep_namespace_import.borrow_key = prep.namespace");
        qb.where("prep_namespace_import.user_id = ? AND prep.key = ?");
        getImportedPrepDataStmt = qb.build(connection);
    }

    public ReturnValue savePrepData(Data data) {
        synchronized (connection) {
            try {
                try {
                    if (!data.namespace.equals(data.oldNamespace)) {
                        getNamespaceExistStmt.setLong(1, data.userId);
                        getNamespaceExistStmt.setString(2, data.namespace);
                        ResultSet rs = getNamespaceExistStmt.executeQuery();

                        if (!rs.next()) {
                            throw new RuntimeException();
                        }

                        if (rs.getInt("cnt") == 0) {
                            return ReturnValue.NAMESPACE_NO_EXIST;
                        }

                        getPrepDataStmt.setLong(1, data.userId);
                        getPrepDataStmt.setString(2, data.namespace);
                        getPrepDataStmt.setString(3, data.key);
                        rs = getPrepDataStmt.executeQuery();

                        if (rs.next()) {
                            return ReturnValue.KEY_EXIST;
                        }

                        changePrepNamespaceStmt.setString(1, data.namespace);
                        changePrepNamespaceStmt.setLong(2, data.userId);
                        changePrepNamespaceStmt.setString(3, data.oldNamespace);
                        changePrepNamespaceStmt.setString(4, data.key);
                        int rows = changePrepNamespaceStmt.executeUpdate();
                        if (rows > 0) {
                            checkLoadConflictStmt.setLong(1, data.userId);
                            checkLoadConflictStmt.setString(2, data.key);
                            rs = checkLoadConflictStmt.executeQuery();
                            if (!rs.next()) {
                                throw new RuntimeException();
                            }

                            if (rs.getBoolean("conflict")) {
                                return ReturnValue.KEY_CONFLICT;
                            }
                        }
                    }

                    DatabaseManager databaseManager = rengetsu.getDatabaseManager();
                    databaseManager.getUserData().initializeUser(data.userId);

                    setPrepDataStmt.setLong(1, data.userId);
                    setPrepDataStmt.setString(2, data.namespace);
                    setPrepDataStmt.setString(3, data.key);
                    setPrepDataStmt.setString(4, data.name);
                    setPrepDataStmt.setString(5, data.description);
                    setPrepDataStmt.setInt(6, data.rolls.size());
                    setPrepDataStmt.setInt(7, data.varCount);
                    setPrepDataStmt.setInt(8, data.params.length);
                    setPrepDataStmt.executeUpdate();

                    clearPrepRollsStmt.setLong(1, data.userId);
                    clearPrepRollsStmt.setString(2, data.namespace);
                    clearPrepRollsStmt.setString(3, data.key);
                    clearPrepRollsStmt.executeUpdate();

                    clearPrepParamsStmt.setLong(1, data.userId);
                    clearPrepParamsStmt.setString(2, data.namespace);
                    clearPrepParamsStmt.setString(3, data.key);
                    clearPrepParamsStmt.executeUpdate();


                    addPrepDicerollsStmt.setLong(1, data.userId);
                    addPrepDicerollsStmt.setString(2, data.namespace);
                    addPrepDicerollsStmt.setString(3, data.key);
                    addPrepCalculationStmt.setLong(1, data.userId);
                    addPrepCalculationStmt.setString(2, data.namespace);
                    addPrepCalculationStmt.setString(3, data.key);

                    for (int i = 0; i < data.rolls.size(); i++) {
                        switch (data.rolls.get(i)) {
                            case Data.DiceRollData diceRollData -> {
                                addPrepDicerollsStmt.setInt(4, i);
                                addPrepDicerollsStmt.setString(5, diceRollData.description);
                                addPrepDicerollsStmt.setString(6, diceRollData.query);
                                addPrepDicerollsStmt.setString(7, diceRollData.variable);
                                addPrepDicerollsStmt.setByte(8, diceRollData.result);
                                addPrepDicerollsStmt.executeUpdate();
                            }
                            case Data.CalculationData calculationData -> {
                                addPrepCalculationStmt.setInt(4, i);
                                addPrepCalculationStmt.setString(5, calculationData.description);
                                addPrepCalculationStmt.setString(6, calculationData.query);
                                addPrepCalculationStmt.setBytes(7, calculationData.bytecode);
                                addPrepCalculationStmt.executeUpdate();
                            }
                        }
                    }

                    addPrepParamStmt.setLong(1, data.userId);
                    addPrepParamStmt.setString(2, data.namespace);
                    addPrepParamStmt.setString(3, data.key);
                    for (int i = 0; i < data.parameterData.size(); i++) {
                        Data.ParameterData parameterData = data.parameterData.get(i);
                        addPrepParamStmt.setInt(4, i);
                        addPrepParamStmt.setString(5, parameterData.name);
                        addPrepParamStmt.setInt(6, parameterData.type.ordinal());
                        addPrepParamStmt.setByte(7, parameterData.ofVarType);
                        addPrepParamStmt.setByte(8, parameterData.result);
                        addPrepParamStmt.executeUpdate();
                    }
                    connection.commit();
                    return ReturnValue.SUCCESS;
                } finally {
                    connection.rollback();
                }
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return ReturnValue.DATABASE_ERROR;
            }
        }
    }

    public ReturnValue deletePrepData(long userId, String namespace, String key) {
        synchronized (connection) {
            try {
                deletePrepDataStmt.setLong(1, userId);
                deletePrepDataStmt.setString(2, namespace);
                deletePrepDataStmt.setString(3, key);
                deletePrepDataStmt.executeUpdate();
                connection.commit();
                return ReturnValue.SUCCESS;
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return ReturnValue.DATABASE_ERROR;
            }
        }
    }

    public ReturnValue validateCreate(long userId, String namespace, String key) {
        synchronized (connection) {
            try {
                getNamespaceLoadedStmt.setLong(1, userId);
                getNamespaceLoadedStmt.setString(2, namespace);
                ResultSet rs = getNamespaceLoadedStmt.executeQuery();

                if (!rs.next()) {
                    return ReturnValue.NAMESPACE_NO_EXIST;
                }

                if (!rs.getBoolean("loaded")) {
                    return ReturnValue.NAMESPACE_NO_LOADED;
                }

                getLoadedPrepDataStmt.setLong(1, userId);
                getLoadedPrepDataStmt.setString(2, key);
                rs = getLoadedPrepDataStmt.executeQuery();
                if (rs.next()) {
                    return ReturnValue.KEY_LOADED;
                }

                hasPrepDataStmt.setLong(1, userId);
                hasPrepDataStmt.setString(2, namespace);
                hasPrepDataStmt.setString(3, key);
                rs = hasPrepDataStmt.executeQuery();

                if (rs.next()) {
                    return rs.getLong("cnt") > 0 ? ReturnValue.KEY_LOADED : ReturnValue.SUCCESS;
                }
                throw new RuntimeException();
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return ReturnValue.DATABASE_ERROR;
            }
        }
    }

    public ReturnValue createNamespace(long userId, String name) {
        synchronized (connection) {
            try {
                createNamespaceStmt.setLong(1, userId);
                createNamespaceStmt.setString(2, name);
                int rows = createNamespaceStmt.executeUpdate();
                connection.commit();
                return rows > 0 ? ReturnValue.SUCCESS : ReturnValue.NAMESPACE_EXIST;
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return ReturnValue.DATABASE_ERROR;
            }
        }
    }

    public ArrayList<NamespaceData> listNamespaces(long userId) throws SQLException {
        synchronized (connection) {
            listNamespacesStmt.setLong(1, userId);
            ResultSet rs = listNamespacesStmt.executeQuery();

            ArrayList<NamespaceData> names = new ArrayList<>();
            while (rs.next()) {
                String key = rs.getString("key");
                if (!key.isEmpty()) {
                    names.add(new NamespaceData(key, rs.getBoolean("loaded")));
                }
            }
            return names;
        }
    }

    public ReturnValue deleteNamespace(long userId, String name) {
        synchronized (connection) {
            try {
                deleteNamespaceStmt.setLong(1, userId);
                deleteNamespaceStmt.setString(2, name);
                int rows = deleteNamespaceStmt.executeUpdate();
                connection.commit();
                return rows > 0 ? ReturnValue.SUCCESS : ReturnValue.NAMESPACE_NO_EXIST;
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return ReturnValue.DATABASE_ERROR;
            }
        }
    }

    public ReturnValue validateNamepaceMove(long userId, String namespace, String key) {
        try {
            getNamespaceExistStmt.setLong(1, userId);
            getNamespaceExistStmt.setString(2, namespace);
            ResultSet rs = getNamespaceExistStmt.executeQuery();

            if (!rs.next()) {
                throw new RuntimeException();
            }

            if (rs.getInt("cnt") == 0) {
                return ReturnValue.NAMESPACE_NO_EXIST;
            }

            getPrepDataStmt.setLong(1, userId);
            getPrepDataStmt.setString(2, namespace);
            getPrepDataStmt.setString(3, key);
            rs = getPrepDataStmt.executeQuery();

            if (rs.next()) {
                return ReturnValue.KEY_EXIST;
            }

            return ReturnValue.SUCCESS;
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("SQL Error", e);
            return ReturnValue.DATABASE_ERROR;
        }
    }

    public QueryResult<Void> renameNamespace(long userId, String name, String newName) {
        synchronized (connection) {
            try {
                getNamespaceExistStmt.setLong(1, userId);
                getNamespaceExistStmt.setString(2, newName);
                ResultSet rs = getNamespaceExistStmt.executeQuery();
                if (!rs.next()) {
                    throw new RuntimeException();
                }
                if (rs.getLong("cnt") > 0) {
                    return new QueryResult<>(ReturnValue.NAMESPACE_EXIST, newName);
                }

                renameNamespaceStmt.setString(1, newName);
                renameNamespaceStmt.setLong(2, userId);
                renameNamespaceStmt.setString(3, name);
                int rows = renameNamespaceStmt.executeUpdate();
                connection.commit();
                return rows > 0 ? new QueryResult<>((Void) null) : new QueryResult<>(ReturnValue.NAMESPACE_NO_EXIST, name);
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return new QueryResult<>(ReturnValue.DATABASE_ERROR);
            }
        }
    }

    public QueryResult<List<NameData>> listLoadedPrepNames(long userId) {
        synchronized (connection) {
            try {
                listLoadedPrepNamesStmt.setLong(1, userId);
                ResultSet rs = listLoadedPrepNamesStmt.executeQuery();

                ArrayList<NameData> names = new ArrayList<>();
                while (rs.next()) {
                    String namespace = rs.getString("namespace");
                    names.add(new NameData(rs.getString("key"), namespace.isEmpty() ? "(default)" : namespace,
                            rs.getString("name")));
                }
                return new QueryResult<>(names);
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return new QueryResult<>(ReturnValue.DATABASE_ERROR);
            }
        }
    }

    public ReturnValue deleteLoadedPrepData(long userId, String key) {
        synchronized (connection) {
            try {
                getLoadedPrepDataStmt.setLong(1, userId);
                getLoadedPrepDataStmt.setString(2, key);
                ResultSet rs = getLoadedPrepDataStmt.executeQuery();
                if (!rs.next()) {
                    return ReturnValue.KEY_NO_EXIST_OR_LOADED;
                }
                deletePrepDataStmt.setLong(1, userId);
                deletePrepDataStmt.setString(2, rs.getString("namespace"));
                deletePrepDataStmt.setString(3, key);
                int rows = deletePrepDataStmt.executeUpdate();
                connection.commit();
                return rows > 0 ? ReturnValue.SUCCESS : ReturnValue.KEY_NO_EXIST_OR_LOADED;
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return ReturnValue.DATABASE_ERROR;
            }
        }
    }

    public QueryResult<Data> getLoadedPrepData(long userId, String key) {
        synchronized (connection) {
            try {
                getLoadedPrepDataStmt.setLong(1, userId);
                getLoadedPrepDataStmt.setString(2, key);
                ResultSet rs = getLoadedPrepDataStmt.executeQuery();
                if (!rs.next()) {
                    return new QueryResult<>(ReturnValue.KEY_NO_EXIST_OR_LOADED);
                }
                Data prepData = new Data(userId, rs.getString("namespace"), key);
                prepData.name = rs.getString("name");
                prepData.description = rs.getString("descr");
                int rollCount = rs.getInt("roll_count");
                prepData.varCount = rs.getInt("var_count");
                int paramCount = rs.getInt("param_count");

                getPrepRollStmt.setLong(1, userId);
                getPrepRollStmt.setString(2, prepData.namespace);
                getPrepRollStmt.setString(3, key);
                for (int i = 0; i < rollCount; i++) {
                    getPrepRollStmt.setInt(4, i);
                    rs = getPrepRollStmt.executeQuery();

                    if (!rs.next()) {
                        throw new RuntimeException();
                    }

                    byte[] bytecode = rs.getBytes("bytecode");

                    if (bytecode == null) {
                        prepData.rolls.add(new Data.DiceRollData(rs.getString("descr"),
                                rs.getString("query"), rs.getString("variable"),
                                rs.getByte("result")));
                    } else {
                        prepData.rolls.add(new Data.CalculationData(rs.getString("descr"),
                                rs.getString("query"), bytecode));
                    }
                }
                getPrepParamStmt.setLong(1, userId);
                getPrepParamStmt.setString(2, prepData.namespace);
                getPrepParamStmt.setString(3, key);
                for (int i = 0; i < paramCount; i++) {
                    getPrepParamStmt.setInt(4, i);
                    rs = getPrepParamStmt.executeQuery();

                    if (!rs.next()) {
                        throw new RuntimeException();
                    }

                    prepData.parameterData.add(new Data.ParameterData(rs.getString("name"),
                            Type.FixedType.values()[rs.getInt("fixed_type")],
                            rs.getByte("var_type"), rs.getByte("result")));
                }
                prepData.params = prepData.parameterData.stream().map(Data.ParameterData::name).toList().toArray(new String[paramCount]);

                return new QueryResult<>(prepData);
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return new QueryResult<>(ReturnValue.DATABASE_ERROR);
            }
        }
    }

    public QueryResult<Data> getLoadedOrImportedPrepData(long userId, String key) {
        synchronized (connection) {
            try {
                getLoadedPrepDataStmt.setLong(1, userId);
                getLoadedPrepDataStmt.setString(2, key);
                ResultSet rs = getLoadedPrepDataStmt.executeQuery();
                if (!rs.next()) {
                    getImportedPrepDataStmt.setLong(1, userId);
                    getImportedPrepDataStmt.setString(2, key);
                    rs = getImportedPrepDataStmt.executeQuery();

                    if (!rs.next()) {
                        return new QueryResult<>(ReturnValue.KEY_NO_EXIST_OR_LOADED);
                    }
                }
                Data prepData = new Data(userId, rs.getString("namespace"), key);
                prepData.name = rs.getString("name");
                prepData.description = rs.getString("descr");
                int rollCount = rs.getInt("roll_count");
                prepData.varCount = rs.getInt("var_count");
                int paramCount = rs.getInt("param_count");

                getPrepRollStmt.setLong(1, userId);
                getPrepRollStmt.setString(2, prepData.namespace);
                getPrepRollStmt.setString(3, key);
                for (int i = 0; i < rollCount; i++) {
                    getPrepRollStmt.setInt(4, i);
                    rs = getPrepRollStmt.executeQuery();

                    if (!rs.next()) {
                        throw new RuntimeException();
                    }

                    byte[] bytecode = rs.getBytes("bytecode");

                    if (bytecode == null) {
                        prepData.rolls.add(new Data.DiceRollData(rs.getString("descr"),
                                rs.getString("query"), rs.getString("variable"),
                                rs.getByte("result")));
                    } else {
                        prepData.rolls.add(new Data.CalculationData(rs.getString("descr"),
                                rs.getString("query"), bytecode));
                    }
                }
                getPrepParamStmt.setLong(1, userId);
                getPrepParamStmt.setString(2, prepData.namespace);
                getPrepParamStmt.setString(3, key);
                for (int i = 0; i < paramCount; i++) {
                    getPrepParamStmt.setInt(4, i);
                    rs = getPrepParamStmt.executeQuery();

                    if (!rs.next()) {
                        throw new RuntimeException();
                    }

                    prepData.parameterData.add(new Data.ParameterData(rs.getString("name"),
                            Type.FixedType.values()[rs.getInt("fixed_type")],
                            rs.getByte("var_type"), rs.getByte("result")));
                }
                prepData.params = prepData.parameterData.stream().map(Data.ParameterData::name).toList().toArray(new String[paramCount]);

                return new QueryResult<>(prepData);
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return new QueryResult<>(ReturnValue.DATABASE_ERROR);
            }
        }
    }

    public QueryResult<Void> setNamespaceLoaded(long userId, String name, boolean loaded) {
        synchronized (connection) {
            try {
                getNamespaceLoadedStmt.setLong(1, userId);
                getNamespaceLoadedStmt.setString(2, name);
                ResultSet rs = getNamespaceLoadedStmt.executeQuery();
                if (!rs.next()) {
                    return new QueryResult<>(ReturnValue.NAMESPACE_NO_EXIST);
                }
                if (rs.getBoolean("loaded") == loaded) {
                    return loaded ? new QueryResult<>(ReturnValue.NAMESPACE_LOADED) :
                            new QueryResult<>(ReturnValue.NAMESPACE_NO_LOADED);
                }

                if (loaded) {
                    listPrepNamesStmt.setLong(1, userId);
                    listPrepNamesStmt.setString(2, name);
                    rs = listPrepNamesStmt.executeQuery();
                    while (rs.next()) {
                        String key = rs.getString("key");

                        getLoadedPrepDataStmt.setLong(1, userId);
                        getLoadedPrepDataStmt.setString(2, key);
                        if (getLoadedPrepDataStmt.executeQuery().next()) {
                            return new QueryResult<>(ReturnValue.KEY_CONFLICT, key);
                        }
                    }
                }

                setNamespaceLoadedStmt.setBoolean(1, loaded);
                setNamespaceLoadedStmt.setLong(2, userId);
                setNamespaceLoadedStmt.setString(3, name);
                int rows = setNamespaceLoadedStmt.executeUpdate();
                connection.commit();
                return rows > 0 ? new QueryResult<>((Void) null) : new QueryResult<>(ReturnValue.NAMESPACE_NO_EXIST);
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return new QueryResult<>(ReturnValue.DATABASE_ERROR);
            }
        }
    }

    public QueryResult<Void> importNamespace(long userId, String reference, long borrowId, String borrowNamespace) {
        synchronized (connection) {
            try {
                getNamespaceExistStmt.setLong(1, borrowId);
                getNamespaceExistStmt.setString(2, borrowNamespace);
                ResultSet rs = getNamespaceExistStmt.executeQuery();
                if (!rs.next()) {
                    throw new RuntimeException();
                }
                if (rs.getInt("cnt") == 0) {
                    return new QueryResult<>(ReturnValue.NAMESPACE_NO_EXIST, borrowNamespace);
                }

                getNamespaceExistStmt.setLong(1, userId);
                getNamespaceExistStmt.setString(2, reference);
                rs = getNamespaceExistStmt.executeQuery();
                if (!rs.next()) {
                    throw new RuntimeException();
                }
                if (rs.getInt("cnt") > 0) {
                    return new QueryResult<>(ReturnValue.NAMESPACE_EXIST, reference);
                }

                importNamespaceStmt.setLong(1, userId);
                importNamespaceStmt.setString(2, reference);
                importNamespaceStmt.setLong(3, borrowId);
                importNamespaceStmt.setString(4, borrowNamespace);
                importNamespaceStmt.executeUpdate();
                connection.commit();
                return new QueryResult<>((Void) null);
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return new QueryResult<>(ReturnValue.DATABASE_ERROR);
            }
        }
    }

    public static InteractionApplicationCommandCallbackSpec buildMenu(Data data) {
        InteractionApplicationCommandCallbackSpec.Builder builder = InteractionApplicationCommandCallbackSpec.builder();
        builder.content("Key: %s".formatted(data.key)).ephemeral(true);
        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
        embed.title(data.name);
        embed.description(data.description);
        embed.addField("Namespace", data.namespace.isEmpty() ? "(default)" : data.namespace, false);
        if (data.params.length > 0) {
            embed.addField("Parameters", String.join(", ", data.params), false);
        }

        String rollDesc = "";
        StringJoiner joiner = new StringJoiner("\n");
        for (Data.RollData rollData: data.rolls) {
            if (rollData.description != null) {
                if (joiner.length() > 0) {
                    embed.addField(rollDesc, joiner.toString(), false);
                    joiner = new StringJoiner("\n");
                }
                rollDesc = rollData.description;
            }
            if (rollData instanceof Data.DiceRollData diceroll && diceroll.variable != null) {
                joiner.add("%s=%s".formatted(diceroll.variable, rollData.query.replace("*", "\\*")));
            } else {
                joiner.add(rollData.query.replace("*", "\\*"));
            }
        }

        if (joiner.length() > 0) {
            embed.addField(rollDesc, joiner.toString(), false);
        }

        builder.addComponent(ActionRow.of(
                Button.primary("prep:edit:%d".formatted(data.uid), "Edit description"),
                Button.primary("prep:namespace:%d".formatted(data.uid), "Change namespace"),
                Button.primary("prep:params:%d".formatted(data.uid), "Edit parameters")
        ));

        builder.addComponent(ActionRow.of(
                Button.primary("prep:add_roll:%d".formatted(data.uid), "Add dice roll"),
                Button.primary("prep:add_calc:%d".formatted(data.uid), "Add calculation"),
                Button.primary("prep:del_roll:%d".formatted(data.uid), "Remove dice rolls/calculations")
                        .disabled(data.rolls.isEmpty())
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

    public record NameData(String key, String namespace, String name) {}

    public record NamespaceData(String key, boolean loaded) {}

    private record Key(long userId, String key) {}

    public static class Data {
        private static int nextUid;

        public final int uid;
        public long userId;
        public String key;
        public String name;
        public String description;
        public String namespace;
        public String[] params = new String[0];
        public boolean editing = true;
        public final ArrayList<RollData> rolls = new ArrayList<>();
        public final ArrayList<ParameterData> parameterData = new ArrayList<>();
        public int varCount;

        private ScheduledFuture<?> removalTask;
        private final String oldNamespace;

        public Data(long userId, String namespace, String key) {
            this.userId = userId;
            this.namespace = namespace;
            this.oldNamespace = namespace;
            this.key = key;

            uid = nextUid++;
        }

        public static sealed abstract class RollData permits DiceRollData, CalculationData {
            public String description;
            public String query;

            public RollData(String description, String query) {
                this.description = description;
                this.query = query;
            }
        }

        public static final class DiceRollData extends RollData {
            public String variable;
            public byte result;

            public DiceRollData(String description, String query, String variable) {
                super(description, query);
                this.variable = variable;
            }

            public DiceRollData(String description, String query, String variable, byte result) {
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
                    Parser.ParseTree pt = Parser.parseCalculation(query);

                    if (!pt.errors().isEmpty()) {
                        throw new IllegalStateException("Syntax error(s) found in stored query\n" + String.join("\n", pt.errors()));
                    }

                    ast = new ASTGenerator().visit(pt.parseTree());
                }
                return ast;
            }
        }

        public record ParameterData(String name, Type.FixedType type, byte ofVarType, byte result) {}
    }

    public InteractionApplicationCommandCallbackSpec getPrepList(long userId, String namespace) {
        synchronized (connection) {
            InteractionApplicationCommandCallbackSpec.Builder builder = InteractionApplicationCommandCallbackSpec.builder();

            try {
                listPrepNamesStmt.setLong(1, userId);
                listPrepNamesStmt.setString(2, namespace);
                ResultSet rs = listPrepNamesStmt.executeQuery();

                ArrayList<NameData> prepEffs = new ArrayList<>();
                while (rs.next()) {
                    prepEffs.add(new NameData(rs.getString("key"), namespace, rs.getString("name")));
                }

                String loaded = null;

                if (!namespace.isEmpty()) {
                    getNamespaceLoadedStmt.setLong(1, userId);
                    getNamespaceLoadedStmt.setString(2, namespace);
                    rs = getNamespaceLoadedStmt.executeQuery();
                    loaded = rs.next() && rs.getBoolean("loaded") ? "(loaded)" : "(unloaded)";
                }

                builder.embeds(List.of(EmbedCreateSpec.builder().title(namespace.isEmpty() ? "Default namespace" :
                                "Namespace %s %s".formatted(namespace, loaded))
                                .description("%d prepared effects".formatted(prepEffs.size()))
                        .fields(prepEffs.stream()
                                .map(data -> EmbedCreateFields.Field.of(data.name, "key: " + data.key, false)).toList())
                        .build()));

                listNamespacesStmt.setLong(1, userId);
                rs = listNamespacesStmt.executeQuery();

                ArrayList<String> names = new ArrayList<>();
                names.add("");
                while (rs.next()) {
                    String key2 = rs.getString("key");
                    if (!key2.isEmpty()) {
                        names.add(key2);
                    }
                }

                List<LayoutComponent> rows = new ArrayList<>();

                if (prepEffs.size() > 0) {
                    rows.add( ActionRow.of(SelectMenu.of("prep_list:prep", prepEffs.stream().map(
                            data -> SelectMenu.Option.of(data.name, "%d:%s:%s".formatted(userId, namespace, data.key))
                    ).toList()).withPlaceholder("Select prepared effect")));
                }

                rows.add(ActionRow.of(SelectMenu.of("prep_list:namespace", names.stream().map(
                        key2 -> SelectMenu.Option.of(key2.isEmpty() ? "(default)" : key2, "%d:%s".formatted(userId, key2))
                ).toList()).withPlaceholder("Select namespace")));

                builder.components(rows);
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return builder.content("**[Error]** " + ReturnValue.DATABASE_ERROR.format(null, null))
                        .ephemeral(true).build();
            }
            builder.ephemeral(true);
            return builder.build();
        }
    }

    public InteractionApplicationCommandCallbackSpec getPrepList(long userId, String namespace, String key) {
        synchronized (connection) {
            try {
                getPrepDataStmt.setLong(1, userId);
                getPrepDataStmt.setString(2, namespace);
                getPrepDataStmt.setString(3, key);
                ResultSet rs = getPrepDataStmt.executeQuery();
                if (!rs.next()) {
                    return null;
                }
                Data prepData = new Data(userId, rs.getString("namespace"), key);
                prepData.name = rs.getString("name");
                prepData.description = rs.getString("descr");
                int rollCount = rs.getInt("roll_count");
                prepData.varCount = rs.getInt("var_count");
                int paramCount = rs.getInt("param_count");

                getPrepRollStmt.setLong(1, userId);
                getPrepRollStmt.setString(2, prepData.namespace);
                getPrepRollStmt.setString(3, key);
                for (int i = 0; i < rollCount; i++) {
                    getPrepRollStmt.setInt(4, i);
                    rs = getPrepRollStmt.executeQuery();

                    if (!rs.next()) {
                        throw new RuntimeException();
                    }

                    byte[] bytecode = rs.getBytes("bytecode");

                    if (bytecode == null) {
                        prepData.rolls.add(new Data.DiceRollData(rs.getString("descr"),
                                rs.getString("query"), rs.getString("variable"),
                                rs.getByte("result")));
                    } else {
                        prepData.rolls.add(new Data.CalculationData(rs.getString("descr"),
                                rs.getString("query"), bytecode));
                    }
                }
                getPrepParamStmt.setLong(1, userId);
                getPrepParamStmt.setString(2, prepData.namespace);
                getPrepParamStmt.setString(3, key);
                for (int i = 0; i < paramCount; i++) {
                    getPrepParamStmt.setInt(4, i);
                    rs = getPrepParamStmt.executeQuery();

                    if (!rs.next()) {
                        throw new RuntimeException();
                    }

                    prepData.parameterData.add(new Data.ParameterData(rs.getString("name"),
                            Type.FixedType.values()[rs.getInt("fixed_type")],
                            rs.getByte("var_type"), rs.getByte("result")));
                }
                prepData.params = prepData.parameterData.stream().map(Data.ParameterData::name).toList().toArray(new String[paramCount]);

                InteractionApplicationCommandCallbackSpec spec = buildMenu(prepData).withContent("");
                listPrepNamesStmt.setLong(1, userId);
                listPrepNamesStmt.setString(2, namespace);
                rs = listPrepNamesStmt.executeQuery();

                ArrayList<NameData> prepEffs = new ArrayList<>();
                while (rs.next()) {
                    prepEffs.add(new NameData(rs.getString("key"), namespace, rs.getString("name")));
                }

                listNamespacesStmt.setLong(1, userId);
                rs = listNamespacesStmt.executeQuery();

                ArrayList<String> names = new ArrayList<>();
                names.add("");
                while (rs.next()) {
                    String key2 = rs.getString("key");
                    if (!key2.isEmpty()) {
                        names.add(key2);
                    }
                }

                return spec.withComponents(List.of(
                        ActionRow.of(SelectMenu.of("prep_list:prep", prepEffs.stream().map(
                                data -> SelectMenu.Option.of(data.name, "%d:%s:%s".formatted(userId, namespace, data.key))
                                        .withDefault(data.key.equals(key))
                        ).toList())),
                        ActionRow.of(SelectMenu.of("prep_list:namespace", names.stream().map(
                                key2 -> SelectMenu.Option.of(key2.isEmpty() ? "(default)" : key2, "%d:%s".formatted(userId, key2))
                        ).toList()).withPlaceholder("Select namespace"))
                )).withEphemeral(true);
            } catch (SQLException e) {
                Rengetsu.getLOGGER().error("SQL Error", e);
                return InteractionApplicationCommandCallbackSpec.builder()
                        .content("**[Error]** Database error").ephemeral(true).build();
            }
        }
    }

    public enum ReturnValue {
        SUCCESS(null), DATABASE_ERROR("Database error"),
        NAMESPACE_NO_EXIST("Namespace \"${namespace}\" does not exist."),
        NAMESPACE_EXIST("Namespace \"${namespace}\" already exists."),
        NAMESPACE_NO_LOADED("Namespace \"${namespace}\" is not loaded."),
        NAMESPACE_LOADED("Namespace \"${namespace}\" is already loaded."),
        KEY_LOADED("Prepared effect with key `${key}` already exists in loaded namespaces."),
        KEY_EXIST("Prepared effect with key `${key}` already exists in namespace \"${namespace}\"."),
        KEY_NO_EXIST_OR_LOADED("Prepared effect with key `${key}` does not exists or is not loaded."),
        KEY_CONFLICT("This action will result in loading conflict with key `${key}`.");

        private final String error;

        ReturnValue(String error) {
            this.error = error;
        }

        public String format(String namespace, String key) {
            String message = error;
            if (namespace != null) {
                message = message.replace("${namespace}", namespace);
            }
            if (key != null) {
                message = message.replace("${key}", key);
            }
            return message;
        }
    }

    public record QueryResult<T>(ReturnValue retVal, T item, String arg) {
        public QueryResult(ReturnValue retVal) {
            this(retVal, null, null);
        }

        public QueryResult(ReturnValue retVal, String arg) {
            this(retVal, null, arg);
        }

        public QueryResult(T item) {
            this(ReturnValue.SUCCESS, item, null);
        }
    }
}

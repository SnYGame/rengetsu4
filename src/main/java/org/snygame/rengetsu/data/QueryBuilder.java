package org.snygame.rengetsu.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueryBuilder {
    private String select;
    private String from;
    private String where;

    private String replace;
    private String insert;
    private boolean ignore;
    private final List<String> values;

    private String delete;

    private String update;
    private String set;

    public QueryBuilder() {
        values = new ArrayList<>();
    }

    public void select(String columns) {
        select = columns;
    }

    public void from(String table) {
        from = table;
    }

    public void where(String conditions) {
        where = conditions;
    }

    public void replaceInto(String table) {
        replace = table;
    }

    public void insertIgnoreInto(String table) {
        insert = table;
        ignore = true;
    }

    public void values(String... values) {
        Collections.addAll(this.values, values);
    }

    public void deleteFrom(String table) {
        delete = table;
    }

    public void update(String table) {
        update = table;
    }

    public void set(String values) {
        set = values;
    }

    public PreparedStatement build(Connection connection) throws SQLException {
        return build(connection, 0);
    }

    public PreparedStatement build(Connection connection, int autoGeneratedKeys) throws SQLException {
        String query = asString();
        return connection.prepareStatement(query, autoGeneratedKeys);
    }

    public String asString() {
        if (replace != null) {
            return replaceString();
        }

        if (insert != null) {
            return insertString();
        }

        if (select != null) {
            return selectString();
        }

        if (delete != null) {
            return deleteString();
        }

        if (update != null) {
            return updateString();
        }

        throw new IllegalStateException("No valid queries can be created.");
    }

    private String selectString() {
        StringBuilder sb = new StringBuilder("SELECT " + select + "\nFROM " + from);

        if (where != null) {
            sb.append("\nWHERE ");
            sb.append(where);
        }

        return sb.toString();
    }

    private String replaceString() {
        StringBuilder sb = new StringBuilder("REPLACE INTO " + replace);

        if (select != null) {
            sb.append("\n");
            return sb + selectString();
        }

        appendList(sb, values, " VALUES ", ",\n");
        return sb.toString();
    }

    private String insertString() {
        StringBuilder sb = new StringBuilder("INSERT" + (ignore ? " OR IGNORE" : "") + " INTO " + insert);

        if (select != null) {
            sb.append("\n");
            return sb + selectString();
        }

        appendList(sb, values, " VALUES ", ",\n");
        return sb.toString();
    }

    private String deleteString() {
        StringBuilder sb = new StringBuilder("DELETE FROM " + delete);
        if (where != null) {
            sb.append("\nWHERE ");
            sb.append(where);
        }

        return sb.toString();
    }

    private String updateString() {
        StringBuilder sb = new StringBuilder("UPDATE " + update);
        if (set != null) {
            sb.append("\nSET ");
            sb.append(set);
        }
        if (where != null) {
            sb.append("\nWHERE ");
            sb.append(where);
        }
        return sb.toString();
    }

    private void appendList(StringBuilder sb, List<String> list, String clause, String seperator) {
        if (list.size() > 0) {
            sb.append(clause);
            for (int i = 0; i < list.size(); i++) {
                if (i != 0) {
                    sb.append(seperator);
                }
                sb.append(list.get(i));
            }
        }
    }
}
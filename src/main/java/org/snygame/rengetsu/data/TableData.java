package org.snygame.rengetsu.data;

import org.snygame.rengetsu.RengClass;
import org.snygame.rengetsu.Rengetsu;

import java.sql.Connection;

public abstract class TableData extends RengClass {
    protected final Connection connection;

    public TableData(Rengetsu rengetsu, Connection connection) {
        super(rengetsu);

        this.connection = connection;
    }
}

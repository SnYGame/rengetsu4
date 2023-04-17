package org.snygame.rengetsu.util.agm;

import org.snygame.rengetsu.RengClass;
import org.snygame.rengetsu.Rengetsu;

import java.util.HashMap;

public class AgmManager extends RengClass {
    private final HashMap<Long, GameState> gameStates = new HashMap<>();

    public AgmManager(Rengetsu rengetsu) {
        super(rengetsu);
    }

    public GameState getGameState(long id) {
        return gameStates.get(id);
    }

    public void resetGameState(long id) {
        gameStates.put(id, new GameState(id));
    }
}

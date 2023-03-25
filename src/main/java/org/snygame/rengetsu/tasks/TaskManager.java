package org.snygame.rengetsu.tasks;

import org.snygame.rengetsu.RengClass;
import org.snygame.rengetsu.Rengetsu;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TaskManager extends RengClass {
    public static ScheduledExecutorService service = Executors.newScheduledThreadPool(4);

    private final DailyTask dailyTask;
    private final RoleTimerTask roleTimerTask;
    private final TimerTask timerTask;

    public TaskManager(Rengetsu rengetsu) {
        super(rengetsu);

        dailyTask = new DailyTask(rengetsu);
        roleTimerTask = new RoleTimerTask(rengetsu);
        timerTask = new TimerTask(rengetsu);
    }

    public DailyTask getDailyTask() {
        return dailyTask;
    }

    public RoleTimerTask getRoleTimerTask() {
        return roleTimerTask;
    }

    public TimerTask getTimerTask() {
        return timerTask;
    }
}

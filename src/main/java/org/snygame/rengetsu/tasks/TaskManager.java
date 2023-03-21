package org.snygame.rengetsu.tasks;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TaskManager {
    public static ScheduledExecutorService service = Executors.newScheduledThreadPool(4);
}

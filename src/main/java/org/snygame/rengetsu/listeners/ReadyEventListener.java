package org.snygame.rengetsu.listeners;

import discord4j.core.event.domain.lifecycle.ReadyEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.tasks.RoleTimerTask;
import org.snygame.rengetsu.tasks.DailyTask;
import org.snygame.rengetsu.tasks.TaskManager;
import org.snygame.rengetsu.tasks.TimerTask;
import reactor.core.publisher.Mono;

public class ReadyEventListener extends Listener {
    public ReadyEventListener(Rengetsu rengetsu) {
        super(rengetsu);
    }

    public Mono<Void> handle(ReadyEvent event) {
        TaskManager taskManager = rengetsu.getTaskManager();
        taskManager.getDailyTask().startTask(event.getClient());
        taskManager.getTimerTask().startup(event.getClient());
        taskManager.getRoleTimerTask().startup(event.getClient());
        return Mono.empty();
    }
}

package org.snygame.rengetsu.listeners;

import discord4j.core.event.domain.lifecycle.ReadyEvent;
import org.snygame.rengetsu.tasks.RoleTimerTask;
import org.snygame.rengetsu.tasks.DailyTask;
import org.snygame.rengetsu.tasks.TimerTask;
import reactor.core.publisher.Mono;

public class ReadyEventListener {
    public static Mono<Void> handle(ReadyEvent event) {
        DailyTask.startTask(event.getClient());
        TimerTask.startup(event.getClient());
        RoleTimerTask.startup(event.getClient());
        return Mono.empty();
    }
}

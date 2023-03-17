package org.snygame.rengetsu.listeners;

import discord4j.core.event.domain.lifecycle.ReadyEvent;
import org.snygame.rengetsu.tasks.SaltRemindTask;
import org.snygame.rengetsu.tasks.TimerTask;
import reactor.core.publisher.Mono;

public class ReadyEventListener {
    public static Mono<Void> handle(ReadyEvent event) {
        SaltRemindTask.startTask(event.getClient());
        TimerTask.startup(event.getClient());
        return Mono.empty();
    }
}

package org.snygame.rengetsu.listeners;

import discord4j.core.event.domain.lifecycle.ReadyEvent;
import org.snygame.rengetsu.tasks.SaltRemindTask;
import reactor.core.publisher.Mono;

public class ReadyEventListener {
    public static Mono<Void> handle(ReadyEvent event) {
        SaltRemindTask.startTask(event.getClient());
        return Mono.empty();
    }
}

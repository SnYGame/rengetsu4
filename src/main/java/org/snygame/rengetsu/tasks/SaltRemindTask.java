package org.snygame.rengetsu.tasks;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.User;
import org.snygame.rengetsu.data.UserData;
import reactor.core.publisher.Flux;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SaltRemindTask {
    private static final int DAY_MILLI = 1000 * 60 * 60 * 24;

    public static void startTask(GatewayDiscordClient client) {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(() -> {
            try {
                List<Snowflake> ids = UserData.getRemindIds();
                Flux.fromIterable(ids).flatMap(client::getUserById).flatMap(User::getPrivateChannel)
                        .flatMap(channel -> channel.createMessage("**[PLACEHOLDER]** Your daily salt is available to be claimed."))
                        .subscribe();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, DAY_MILLI - System.currentTimeMillis() % DAY_MILLI, DAY_MILLI, TimeUnit.MILLISECONDS);
    }
}

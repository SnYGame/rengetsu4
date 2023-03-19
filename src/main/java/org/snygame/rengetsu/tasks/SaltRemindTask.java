package org.snygame.rengetsu.tasks;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;
import org.snygame.rengetsu.data.UserData;
import reactor.core.publisher.Flux;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SaltRemindTask {
    public static void startTask(GatewayDiscordClient client) {
        TaskManager.service.scheduleAtFixedRate(() -> {
            try {
                List<Snowflake> ids = UserData.getRemindIds();
                Flux.fromIterable(ids).flatMap(client::getUserById).flatMap(User::getPrivateChannel)
                        .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder().content("Your daily salt is available to be claimed.").addComponent(
                                ActionRow.of(
                                        Button.primary("salt_claim:%d"
                                                        .formatted(System.currentTimeMillis() / UserData.DAY_MILLI),
                                                "Claim")
                                )
                        ).build()))
                        .subscribe();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, UserData.DAY_MILLI - System.currentTimeMillis() % UserData.DAY_MILLI, UserData.DAY_MILLI,
                TimeUnit.MILLISECONDS);
    }
}

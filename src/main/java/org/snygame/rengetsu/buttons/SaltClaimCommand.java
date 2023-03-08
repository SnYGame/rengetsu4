package org.snygame.rengetsu.buttons;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.snygame.rengetsu.data.UserData;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.sql.SQLException;

public class SaltClaimCommand implements ButtonCommand {

    @Override
    public String getName() {
        return "salt_claim";
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event) {
        long day = Long.parseLong(event.getCustomId().split(":")[1]);

        if (System.currentTimeMillis() / UserData.DAY_MILLI > day) {
            return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                    .content("This button has expired.")
                    .addComponent(
                            ActionRow.of(
                                    Button.danger("disabled", "Expired").disabled()
                            )
                    ).build()).then();
        }

        return Mono.justOrEmpty(event.getInteraction().getUser())
                .map(User::getId).map(Snowflake::asLong).flatMap(id -> {
                    BigInteger result;
                    try {
                        result = UserData.claimSalt(id);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return event.reply("**[Error]** Database error").withEphemeral(true);
                    }

                    if (result.signum() == -1) {
                        int remain = result.negate().intValue();
                        int remainSeconds = remain / 1000;
                        int h = remainSeconds / 3600;
                        int m = (remainSeconds % 3600) / 60;
                        int s = remainSeconds % 60;
                        StringBuilder sb = new StringBuilder("Your next available claim is in");

                        if (h > 0)
                            sb.append(" %d hour%s".formatted(h, h != 1 ? "s" : ""));

                        if (m > 0)
                            sb.append(" %d minute%s".formatted(m, m != 1 ? "s" : ""));

                        if (s > 0 || (h == 0 && m == 0))
                            sb.append(" %d second%s".formatted(s, s != 1 ? "s" : ""));
                        return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                                .content(sb.toString())
                                .addComponent(
                                        ActionRow.of(
                                                Button.danger("disabled", "Already claimed").disabled()
                                        )
                                ).build()).then();
                    }

                    return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                            .content("You now have %d salt.".formatted(result))
                            .addComponent(
                                    ActionRow.of(
                                            Button.success("disabled", "Claim success").disabled()
                                    )
                            ).build()).then();
                }).then();
    }
}

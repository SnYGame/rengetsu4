package org.snygame.rengetsu.buttons;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.UserData;
import org.snygame.rengetsu.listeners.InteractionListener;
import org.snygame.rengetsu.util.TimeStrings;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.sql.SQLException;

public class SaltClaimButton extends InteractionListener.CommandDelegate<ButtonInteractionEvent> {
    public SaltClaimButton(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "salt_claim";
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        UserData userData = databaseManager.getUserData();
        long day = Long.parseLong(event.getCustomId().split(":")[1]);

        if (System.currentTimeMillis() / TimeStrings.DAY_MILLI > day) {
            return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                    .content("This button has expired.")
                    .addComponent(
                            ActionRow.of(
                                    Button.secondary("disabled", "Claim").disabled()
                            )
                    ).build()).then();
        }

        long id = event.getInteraction().getUser().getId().asLong();

        BigInteger result;
        try {
            result = userData.claimSalt(id);
        } catch (SQLException e) {
            Rengetsu.getLOGGER().error("SQL Error", e);
            return event.reply("**[Error]** Database error").withEphemeral(true);
        }

        if (result.signum() == -1) {
            int remain = result.negate().intValue();
            StringBuilder sb = new StringBuilder("Your next available claim is in ");
            sb.append(TimeStrings.secondsToEnglish(remain / 1000));
            return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                    .content(sb.append(".").toString())
                    .addComponent(
                            ActionRow.of(
                                    Button.secondary("disabled", "Claim").disabled()
                            )
                    ).build());
        }

        return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                .content("You now have %d salt.".formatted(result))
                .addComponent(
                        ActionRow.of(
                                Button.secondary("disabled", "Claim").disabled()
                        )
                ).build());
    }
}

package org.snygame.rengetsu.buttons;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.snygame.rengetsu.tasks.TimerTask;
import reactor.core.publisher.Mono;

public class CancelTimerCommand implements ButtonCommand {

    @Override
    public String getName() {
        return "cancel_timer";
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event) {
        long timerId = Long.parseLong(event.getCustomId().split(":")[1]);
        if (event.getInteraction().getUser().getId().asLong() != Long.parseLong(event.getCustomId().split(":")[2])) {
            return event.reply("**[Error]** You do not have permission to do that").withEphemeral(true);
        }

        if (TimerTask.cancelTimer(timerId)) {
            return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                    .content("Your timer has been canceled.")
                    .addComponent(
                            ActionRow.of(
                                    Button.danger("disabled", "Canceled").disabled()
                            )
                    ).build()).then();
        }

        return event.reply("**[Error]** Timer has already completed or canceled").withEphemeral(true);
    }
}

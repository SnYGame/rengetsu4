package org.snygame.rengetsu.buttons;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.rest.util.AllowedMentions;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.TimerData;
import org.snygame.rengetsu.listeners.InteractionListener;
import org.snygame.rengetsu.tasks.TaskManager;
import reactor.core.publisher.Mono;

import java.sql.SQLException;

public class TimerButton extends InteractionListener.CommandDelegate<ButtonInteractionEvent> {
    public TimerButton(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "timer";
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        TimerData timerData = databaseManager.getTimerData();
        TaskManager taskManager = rengetsu.getTaskManager();
        String[] args = event.getCustomId().split(":");
        long timerId = Long.parseLong(args[1]);
        long ownerId = Long.parseLong(args[2]);
        long userId = event.getInteraction().getUser().getId().asLong();

        switch (args[3]) {
            case "cancel" -> {
                if (userId != ownerId) {
                    return event.reply("**[Error]** You do not have permission to do that").withEphemeral(true);
                }

                if (taskManager.getTimerTask().cancelTimer(timerId)) {
                    return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                            .content("Your timer has been canceled.")
                            .addComponent(
                                    ActionRow.of(
                                            Button.primary("disabled", "Subscribe").disabled(),
                                            Button.secondary("disabled2", "Cancel").disabled()
                                    )
                            ).build()).then();
                }
                return event.reply("**[Error]** Timer has already completed or canceled").withEphemeral(true);
            }
            case "subscribe" -> {
                if (userId == ownerId) {
                    return event.reply("**[Error]** You cannot subscribe to your own timer").withEphemeral(true);
                }

                try {
                    if (timerData.getData(timerId) == null) {
                        return event.reply("**[Error]** Timer has already completed or canceled").withEphemeral(true);
                    }

                    if (timerData.subscribeTimer(userId, timerId)) {
                        return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                                .content("<@%d> has subscribed to timer %d.".formatted(userId, timerId))
                                .allowedMentions(AllowedMentions.suppressAll())
                                .addComponent(ActionRow.of(
                                        Button.danger("timer:%d:0:unsubscribe".formatted(timerId), "Unsubscribe"))
                                ).build());
                    }
                } catch (SQLException e) {
                    Rengetsu.getLOGGER().error("SQL Error", e);
                    return event.reply("**[Error]** Database error").withEphemeral(true);
                }

                return event.reply("**[Error]** You have already subscribed to timer %d".formatted(timerId)).withEphemeral(true);
            }
            case "unsubscribe" -> {
                try {
                    if (timerData.unsubscribeTimer(userId, timerId)) {
                        return event.reply("You have unsubscribed from timer %d.".formatted(timerId));
                    }
                } catch (SQLException e) {
                    Rengetsu.getLOGGER().error("SQL Error", e);
                    return event.reply("**[Error]** Database error").withEphemeral(true);
                }

                return event.reply("**[Error]** You are not subscribed to timer %d".formatted(timerId)).withEphemeral(true);
            }
        }

        throw new RuntimeException();
    }
}

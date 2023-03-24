package org.snygame.rengetsu.buttons;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.MessageEditSpec;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.RoleData;
import org.snygame.rengetsu.data.RoleTimerData;
import org.snygame.rengetsu.data.TimerData;
import org.snygame.rengetsu.tasks.RoleTimerTask;
import org.snygame.rengetsu.util.TimeStrings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public class RequestRoleAgreementButton implements ButtonInteraction {
    @Override
    public String getName() {
        return "requestrole";
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event) {
        RoleData roleData = DatabaseManager.getRoleData();
        RoleTimerData roleTimerData = DatabaseManager.getRoleTimerData();
        String[] args = event.getCustomId().split(":");

        if (args[3].equals("decline")) {
            return Mono.justOrEmpty(event.getMessage()).flatMap(message -> message.edit(MessageEditSpec.builder()
                    .addComponent(ActionRow.of(
                            Button.success("disabled","Accept").disabled(),
                            Button.danger("disabled2", "Declined").disabled()
                    )).build())).then(event.reply("You have declined the agreement."));
        }

        List<Long> toRemoveIds;

        try {
            toRemoveIds = roleData.getRolesToRemoveWhenAdded(Long.parseLong(args[1]), Long.parseLong(args[2]));
        } catch (SQLException e) {
            e.printStackTrace();
            return event.reply("**[Error]** Database error").withEphemeral(true);
        }

        int duration = Integer.parseInt(args[4]);

        return Mono.justOrEmpty(event.getMessage()).flatMap(message -> message.edit(MessageEditSpec.builder()
                .addComponent(ActionRow.of(
                        Button.success("disabled","Accepted").disabled(),
                        Button.danger("disabled2", "Decline").disabled()
                )).build())).then(event.getInteraction().getUser().asMember(Snowflake.of(args[2])).flatMap(member ->
                member.addRole(Snowflake.of(args[1])).then(Flux.fromIterable(toRemoveIds).map(Snowflake::of).flatMap(id ->
                        member.removeRole(id, "Removed after adding new role")).then()).then(
                        event.getClient().getRoleById(Snowflake.of(args[2]), Snowflake.of(args[1])).map(Role::getName).flatMap(name -> {
                            if (duration > 0) {
                                long timerId;
                                try {
                                    timerId = roleTimerData.addTimer(Long.parseLong(args[1]), Long.parseLong(args[2]), member.getId().asLong(),
                                            Instant.ofEpochMilli(System.currentTimeMillis() + duration * 1000L));
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    return event.reply("**[Error]** Database error").withEphemeral(true);
                                }
                                RoleTimerTask.startTask(event.getClient(), timerId, duration * 1000L);
                                return event.reply("You have been given the %s role for %s.".formatted(
                                        name, TimeStrings.secondsToEnglish(duration)
                                ));
                            } else {
                                return event.reply("You have been given the %s role.".formatted(name));
                            }
                        }))));
    }
}

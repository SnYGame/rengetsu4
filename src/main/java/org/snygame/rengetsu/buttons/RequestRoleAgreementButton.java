package org.snygame.rengetsu.buttons;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.MessageEditSpec;
import org.snygame.rengetsu.util.TimeStrings;
import reactor.core.publisher.Mono;

public class RequestRoleAgreementButton implements ButtonInteraction {
    @Override
    public String getName() {
        return "requestrole";
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event) {
        String[] args = event.getCustomId().split(":");

        if (args[3].equals("decline")) {
            return Mono.justOrEmpty(event.getMessage()).flatMap(message -> message.edit(MessageEditSpec.builder()
                    .addComponent(ActionRow.of(
                            Button.success("disabled","Accept").disabled(),
                            Button.danger("disabled2", "Declined").disabled()
                    )).build())).then(event.reply("You have declined the agreement."));
        }

        int duration = Integer.parseInt(args[4]);

        return Mono.justOrEmpty(event.getMessage()).flatMap(message -> message.edit(MessageEditSpec.builder()
                .addComponent(ActionRow.of(
                        Button.success("disabled","Accepted").disabled(),
                        Button.danger("disabled2", "Decline").disabled()
                )).build())).then(event.getInteraction().getUser().asMember(Snowflake.of(args[2])).flatMap(member ->
                member.addRole(Snowflake.of(args[1])).then(
                        event.getClient().getRoleById(Snowflake.of(args[2]), Snowflake.of(args[1])).map(Role::getName).flatMap(name ->
                        event.reply("You have been given the %s role%s.".formatted(
                                name, duration > 0 ? " for %s".formatted(TimeStrings.secondsToEnglish(duration)) : ""
                        ))))));
    }
}

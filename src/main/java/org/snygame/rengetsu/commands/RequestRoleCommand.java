package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.http.client.ClientException;
import org.snygame.rengetsu.data.RoleData;
import org.snygame.rengetsu.util.TimeStrings;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.Optional;

public class RequestRoleCommand implements SlashCommand {
    private static final int MAX_DURATION = 60 * 60 * 24 * 30;

    @Override
    public String getName() {
        return "requestrole";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return Mono.justOrEmpty(event.getOption("role")
                .flatMap(ApplicationCommandInteractionOption::getValue))
                .flatMap(ApplicationCommandInteractionOptionValue::asRole)
                .flatMap(role -> {
                    if (!role.isEveryone()) {
                        RoleData.Data roleData;

                        try {
                            roleData = RoleData.getRoleData(role.getId().asLong(), role.getGuildId().asLong());
                        } catch (SQLException e) {
                            e.printStackTrace();
                            return event.reply("**[Error]** Database error").withEphemeral(true);
                        }

                        RoleData.Data.Requestable requestable;
                        if ((requestable = roleData.requestable) != null) {
                            Optional<Integer> duration = event.getOption("duration").flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asString).map(TimeStrings::readDuration);

                            int actualDuration = duration.orElse(0);

                            return Mono.justOrEmpty(event.getInteraction().getMember()).flatMap(member -> {
                                if (member.getRoleIds().contains(role.getId())) {
                                    return member.removeRole(role.getId(), "Requested")
                                            .then(event.reply("Your %s role has been removed.".formatted(role.getName())));
                                } else {
                                    if (duration.isEmpty() && requestable.temp) {
                                        return event.reply("**[Error]** You must provide a duration for this role").withEphemeral(true);
                                    }

                                    if (duration.isPresent() && (duration.get() <= 0 || duration.get() > MAX_DURATION)) {
                                        return event.reply("**[Error]** Duration must represent time between 1 second and 30 days").withEphemeral(true);
                                    }

                                    if (requestable.agreement == null) {
                                        if (requestable.temp) {
                                            return member.addRole(role.getId(), "Requested")
                                                    .then(event.reply("You have been given the %s role for %s.".formatted(role.getName(),
                                                            TimeStrings.secondsToEnglish(actualDuration))));
                                        } else {
                                            return member.addRole(role.getId(), "Requested")
                                                    .then(event.reply("You have been given the %s role.".formatted(role.getName())));
                                        }
                                    } else {
                                        return member.getPrivateChannel().flatMap(channel -> channel.createMessage(MessageCreateSpec.builder()
                                                .content("Please read the following to receive the %s role:\n\n%s".formatted(role.getName(), requestable.agreement))
                                                .addComponent(ActionRow.of(
                                                        Button.success("requestrole:%d:%d:accept:%d"
                                                                .formatted(roleData.roleId, roleData.serverId, actualDuration),"Accept"),
                                                        Button.danger("requestrole:%d:%d:decline"
                                                                .formatted(roleData.roleId, roleData.serverId), "Decline")
                                                ))
                                                .build()))
                                                .then(event.reply("Please check your DMs to receive the %s role.".formatted(role.getName())))
                                                .onErrorResume(ClientException.class, ignore ->
                                                        event.reply("**[Error]** Please enable DMs to receive this role.").withEphemeral(true));
                                    }
                                }
                            });
                        }
                    }

                    return event.reply("**[Error]** Role is not requestable").withEphemeral(true);
                });
    }
}

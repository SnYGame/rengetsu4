package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.presence.Status;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

public class HereCommand implements SlashCommand {
    @Override
    public String getName() {
        return "here";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return Mono.justOrEmpty(event.getOption("role")
                .flatMap(ApplicationCommandInteractionOption::getValue))
                .flatMap(ApplicationCommandInteractionOptionValue::asRole)
                .flatMap(role ->
                        role.isMentionable() ?
                        event.getInteraction().getGuild().flatMapMany(Guild::getMembers)
                        .filter(member -> member.getRoleIds().contains(role.getId()))
                        .filterWhen(member -> member.getPresence().map(Presence::getStatus)
                                .map(status -> status != Status.OFFLINE))
                        .map(Member::getMention).collect(Collectors.joining(" "))
                        .flatMap(pings -> event.reply("Pinging %s\n%s".formatted(role.getName(), pings))) :
                                event.reply("%s is not pingable".formatted(role.getName())).withEphemeral(true)
                        );
    }
}

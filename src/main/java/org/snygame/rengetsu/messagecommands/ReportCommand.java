package org.snygame.rengetsu.messagecommands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.core.object.entity.Message;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.listeners.InteractionListener;
import org.snygame.rengetsu.modals.ReportModal;
import reactor.core.publisher.Mono;

public class ReportCommand extends InteractionListener.CommandDelegate<MessageInteractionEvent> {
    public ReportCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "Report to Admins";
    }

    @Override
    public Mono<Void> handle(MessageInteractionEvent event) {
        Message message = event.getResolvedMessage();
        String sid = event.getInteraction().getGuildId().map(Snowflake::asString).orElse("");
        String cid = message.getChannelId().asString();
        String mid = message.getId().asString();
        return ReportModal.createModal(event, "",
                "https://discord.com/channels/%s/%s/%s".formatted(sid, cid, mid));
    }
}

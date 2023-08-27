package org.snygame.rengetsu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.listeners.InteractionListener;
import org.snygame.rengetsu.modals.ReportModal;
import reactor.core.publisher.Mono;

public class ReportCommand extends InteractionListener.CommandDelegate<ChatInputInteractionEvent> {
    public ReportCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "report";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return ReportModal.createModal(event, "", "");
    }
}

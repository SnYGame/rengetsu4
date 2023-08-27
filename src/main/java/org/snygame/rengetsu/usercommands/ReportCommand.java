package org.snygame.rengetsu.usercommands;

import discord4j.core.event.domain.interaction.UserInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.listeners.InteractionListener;
import org.snygame.rengetsu.modals.ReportModal;
import reactor.core.publisher.Mono;

public class ReportCommand extends InteractionListener.CommandDelegate<UserInteractionEvent> {
    public ReportCommand(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "Report to Admins";
    }

    @Override
    public Mono<Void> handle(UserInteractionEvent event) {
        return ReportModal.createModal(event, event.getResolvedUser().getMention(), "");
    }
}

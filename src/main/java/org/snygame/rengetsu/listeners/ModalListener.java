package org.snygame.rengetsu.listeners;

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.modals.PrepModal;
import org.snygame.rengetsu.modals.ReportModal;
import org.snygame.rengetsu.modals.RoleAgreementModal;

import java.util.List;

public class ModalListener extends InteractionListener<ModalSubmitInteractionEvent> {
    public ModalListener(Rengetsu rengetsu) {
        super(rengetsu, List.of(
                new RoleAgreementModal(rengetsu),
                new PrepModal(rengetsu),
                new ReportModal(rengetsu)
        ));
    }
}

package org.snygame.rengetsu.modals;

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.discordjson.possible.Possible;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.RoleData;
import org.snygame.rengetsu.listeners.InteractionListener;
import reactor.core.publisher.Mono;

public class RoleAgreementModal extends InteractionListener.CommandDelegate<ModalSubmitInteractionEvent> {
    public RoleAgreementModal(Rengetsu rengetsu) {
        super(rengetsu);
    }

    @Override
    public String getName() {
        return "role";
    }

    @Override
    public Mono<Void> handle(ModalSubmitInteractionEvent event) {
        DatabaseManager databaseManager = rengetsu.getDatabaseManager();
        RoleData roleData = databaseManager.getRoleData();
        String[] args = event.getCustomId().split(":");

        RoleData.Data data = roleData.getTempData(Integer.parseInt(args[2]));
        if (data == null) {
            return event.edit("**[Error]** Cached data is missing, run the command again")
                    .withComponents().withEmbeds().withEphemeral(true);
        }

        Possible<String> option = event.getComponents().get(0).getData().components().get().get(0).value();
        if (data.requestable != null) {
            data.requestable.agreement = option.isAbsent() || option.get().isBlank() ? null : option.get();
        }

        return event.edit(RoleData.buildMenu(data));
    }
}

package org.snygame.rengetsu.modals;

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.discordjson.possible.Possible;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.data.DatabaseManager;
import org.snygame.rengetsu.data.RoleData;
import reactor.core.publisher.Mono;

public class RoleAgreementModal extends ModalInteraction {
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

        RoleData.Data data = roleData.getTempData(Long.parseLong(args[1]), Long.parseLong(args[2]));
        if (data == null) {
            return event.reply("**[Error]** Cached role data is missing, run the command again").withEphemeral(true);
        }

        Possible<String> option = event.getComponents().get(0).getData().components().get().get(0).value();
        if (data.requestable != null) {
            data.requestable.agreement = option.isAbsent() || option.get().isBlank() ? null : option.get();
        }

        return event.edit(RoleData.buildMenu(data));
    }
}

package org.snygame.rengetsu.modals;

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.discordjson.possible.Possible;
import org.snygame.rengetsu.data.RoleData;
import reactor.core.publisher.Mono;

public class RoleAgreementModal implements ModalInteraction {
    @Override
    public String getName() {
        return "role";
    }

    @Override
    public Mono<Void> handle(ModalSubmitInteractionEvent event) {
        String[] args = event.getCustomId().split(":");

        RoleData.Data roleData = RoleData.getTempData(Long.parseLong(args[1]), Long.parseLong(args[2]));
        if (roleData == null) {
            return event.reply("**[Error]** Cached role data is missing, run the command again").withEphemeral(true);
        }

        Possible<String> option = event.getComponents().get(0).getData().components().get().get(0).value();
        if (!option.isAbsent() && !option.get().isBlank() && roleData.requestable != null) {
            roleData.requestable.agreement = option.get();
        }

        return event.edit(RoleData.buildMenu(roleData));
    }
}

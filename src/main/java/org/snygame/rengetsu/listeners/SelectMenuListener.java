package org.snygame.rengetsu.listeners;

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.selectmenu.*;

import java.util.List;

public class SelectMenuListener extends InteractionListener<SelectMenuInteractionEvent> {
    public SelectMenuListener(Rengetsu rengetsu) {
        super(rengetsu, List.of(
                new RoleRemovalSelectMenu(rengetsu),
                new PrepRollRemovalSelectMenu(rengetsu),
                new ManualJumpSelectMenu(rengetsu),
                new SettingsLogSelectMenu(rengetsu),
                new PrepListSelectMenu(rengetsu)
        ));
    }
}

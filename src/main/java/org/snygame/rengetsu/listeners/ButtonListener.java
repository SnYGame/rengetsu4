package org.snygame.rengetsu.listeners;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import org.snygame.rengetsu.Rengetsu;
import org.snygame.rengetsu.buttons.*;

import java.util.List;

public class ButtonListener extends InteractionListener<ButtonInteractionEvent> {
    public ButtonListener(Rengetsu rengetsu) {
        super(rengetsu, List.of(
                new SaltClaimButton(rengetsu),
                new TimerButton(rengetsu),
                new RoleSetButton(rengetsu),
                new RequestRoleAgreementButton(rengetsu),
                new PrepEditButton(rengetsu),
                new ManualPageButton(rengetsu),
                new SettingsClearLogButton(rengetsu),
                new StackProcessButton(rengetsu)
        ));
    }
}

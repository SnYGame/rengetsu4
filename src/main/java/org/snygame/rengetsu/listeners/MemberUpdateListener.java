package org.snygame.rengetsu.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import org.snygame.rengetsu.data.RoleTimerData;
import org.snygame.rengetsu.tasks.RoleTimerTask;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class MemberUpdateListener {
    public static Mono<Void> handle(MemberUpdateEvent event) {
        return event.getMember().mapNotNull(member -> {
            try {
                List<RoleTimerData.Data> timerIds = RoleTimerData.getTimerIds(member.getGuildId().asLong(), member.getId().asLong());
                Set<Snowflake> roles = member.getRoleIds();
                timerIds.stream().filter(timer -> !roles.contains(Snowflake.of(timer.roleId())))
                        .map(RoleTimerData.Data::timerId).forEach(RoleTimerTask::cancelTimer);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });
    }
}

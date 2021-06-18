package com.motorbesitzen.statuswatcher.bot.event;

import com.motorbesitzen.statuswatcher.data.dao.DiscordGuild;
import com.motorbesitzen.statuswatcher.data.repo.DiscordGuildRepo;
import com.motorbesitzen.statuswatcher.util.LogUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Handles role deletions.
 */
@Service
class RoleDeletionListener extends ListenerAdapter {

	private final DiscordGuildRepo guildRepo;

	@Autowired
	private RoleDeletionListener(final DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	/**
	 * Checks if the deleted role was the reaction role. If it was the reaction role the role gets deleted
	 * from the database.
	 * @param event The Discord event with all its information when a role gets deleted.
	 */
	@Override
	public void onRoleDelete(@Nonnull final RoleDeleteEvent event) {
		final Role deletedRole = event.getRole();
		final Guild guild = event.getGuild();
		final long guildId = guild.getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		dcGuildOpt.ifPresent(
				dcGuild -> updateRole(dcGuild, deletedRole)
		);
	}

	/**
	 * Checks if the deleted role was the reaction role. If it was the reaction role the role gets deleted
	 * from the database.
	 * @param dcGuild The Discord guild as saved in the database.
	 * @param deletedRole The deleted role.
	 */
	private void updateRole(final DiscordGuild dcGuild, final Role deletedRole) {
		final long tagRoleId = dcGuild.getRoleId();
		final long deletedRoleId = deletedRole.getIdLong();
		if (tagRoleId != deletedRoleId) {
			return;
		}

		dcGuild.setRoleId(0);
		guildRepo.save(dcGuild);
		LogUtil.logInfo("[" + dcGuild.getId() + "] Removed tag role due to role deletion.");
	}
}

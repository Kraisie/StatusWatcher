package com.motorbesitzen.statuswatcher.bot.event;

import com.motorbesitzen.statuswatcher.data.dao.DiscordGuild;
import com.motorbesitzen.statuswatcher.data.repo.DiscordGuildRepo;
import com.motorbesitzen.statuswatcher.util.LogUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Handles channel deletions.
 */
@Service
class ChannelDeletionListener extends ListenerAdapter {

	private final DiscordGuildRepo guildRepo;

	@Autowired
	private ChannelDeletionListener(final DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	/**
	 * Checks if the deleted channel was the status channel. If it was the status channel the channel gets deleted
	 * from the database.
	 * @param event The Discord event with all its information when a channel gets deleted.
	 */
	@Override
	public void onTextChannelDelete(@NotNull final TextChannelDeleteEvent event) {
		final TextChannel deletedChannel = event.getChannel();
		final Guild guild = event.getGuild();
		final long guildId = guild.getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		dcGuildOpt.ifPresent(
				dcGuild -> updateChannel(dcGuild, deletedChannel)
		);
	}

	/**
	 * Checks if the deleted channel was the status channel. If it was the status channel the channel gets deleted
	 * from the database.
	 * @param dcGuild The Discord guild as saved in the database.
	 * @param deletedChannel The deleted channel.
	 */
	private void updateChannel(final DiscordGuild dcGuild, final TextChannel deletedChannel) {
		final long statusChannelId = dcGuild.getStatusChannelId();
		final long deletedChannelId = deletedChannel.getIdLong();
		if (statusChannelId != deletedChannelId) {
			return;
		}

		dcGuild.setStatusChannelId(0);
		guildRepo.save(dcGuild);
		LogUtil.logInfo("[" + dcGuild.getId() + "] Removed status channel due to channel deletion.");
	}
}

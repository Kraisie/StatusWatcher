package com.motorbesitzen.statuswatcher.bot.event;

import com.motorbesitzen.statuswatcher.data.dao.DiscordGuild;
import com.motorbesitzen.statuswatcher.data.repo.DiscordGuildRepo;
import com.motorbesitzen.statuswatcher.util.LogUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Handles message deletions.
 */
@Service
class MessageDeletionListener extends ListenerAdapter {

	private final DiscordGuildRepo guildRepo;

	@Autowired
	private MessageDeletionListener(final DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	/**
	 * Checks if the deleted message was the reaction message. If it was the reaction message the message gets deleted
	 * from the database.
	 * @param event The Discord event with all its information when a message gets deleted.
	 */
	@Override
	public void onGuildMessageDelete(@NotNull final GuildMessageDeleteEvent event) {
		final long deletedMessageId = event.getMessageIdLong();
		final Guild guild = event.getGuild();
		final long guildId = guild.getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		dcGuildOpt.ifPresent(
				dcGuild -> updateStatusMessage(dcGuild, deletedMessageId)
		);
	}

	/**
	 * Checks if the deleted message was the reaction message. If it was the reaction message the message gets deleted
	 * from the database.
	 * @param dcGuild The Discord guild as saved in the database.
	 * @param deletedMessageId The ID of the deleted message.
	 */
	private void updateStatusMessage(final DiscordGuild dcGuild, final long deletedMessageId) {
		final long reactionMessageId = dcGuild.getReactionMessageId();
		if (reactionMessageId != deletedMessageId) {
			return;
		}

		dcGuild.setReactionMessageId(0);
		guildRepo.save(dcGuild);
		LogUtil.logInfo("[" + dcGuild.getId() + "] Removed reaction message due to message deletion.");
	}
}

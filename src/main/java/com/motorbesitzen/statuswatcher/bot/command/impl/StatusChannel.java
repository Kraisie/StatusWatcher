package com.motorbesitzen.statuswatcher.bot.command.impl;

import com.motorbesitzen.statuswatcher.bot.command.CommandImpl;
import com.motorbesitzen.statuswatcher.data.dao.DiscordGuild;
import com.motorbesitzen.statuswatcher.data.repo.DiscordGuildRepo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("statuschannel")
class StatusChannel extends CommandImpl {

	private final DiscordGuildRepo guildRepo;

	@Autowired
	private StatusChannel(final DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "statuschannel";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getUsage() {
		return getName() + " #channel";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Update the channel to send status changes in.";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final Guild guild = event.getGuild();
		final long guildId = guild.getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> createDiscordGuild((guildId)));
		handleMessage(event, dcGuild);
	}

	/**
	 * Creates a Discord guild representation and saves it in the database
	 * @param guildId The ID of the Discord guild.
	 * @return The created Discord guild.
	 */
	private DiscordGuild createDiscordGuild(final long guildId) {
		final DiscordGuild dcGuild = DiscordGuild.createDefault(guildId);
		guildRepo.save(dcGuild);
		return dcGuild;
	}

	/**
	 * Updates the status channel to post status changes in.
	 * @param event The Discord event with all its information when a message is received.
	 * @param dcGuild The Discord guild as saved in the database.
	 */
	private void handleMessage(final GuildMessageReceivedEvent event, final DiscordGuild dcGuild) {
		final Message message = event.getMessage();
		final List<TextChannel> mentionedChannels = message.getMentionedChannels();
		if (mentionedChannels.size() == 0) {
			sendErrorMessage(event.getChannel(), "You need to mention a text channel!");
			return;
		}

		final TextChannel mentionedChannel = mentionedChannels.get(0);
		if (!mentionedChannel.canTalk()) {
			sendErrorMessage(event.getChannel(), "I can not access that channel! " +
					"Please fix the channel permissions or use another channel.");
			return;
		}

		final long channelId = mentionedChannel.getIdLong();
		dcGuild.setStatusChannelId(channelId);
		guildRepo.save(dcGuild);
		answer(event.getChannel(), "Updated channel to send status changes in.");
	}
}
package com.motorbesitzen.statuswatcher.bot.command.impl;

import com.motorbesitzen.statuswatcher.bot.command.CommandImpl;
import com.motorbesitzen.statuswatcher.bot.service.EnvSettings;
import com.motorbesitzen.statuswatcher.data.dao.DiscordGuild;
import com.motorbesitzen.statuswatcher.data.repo.DiscordGuildRepo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Displays some info about the guild and its settings.
 */
@Service("info")
class Info extends CommandImpl {

	private final DiscordGuildRepo guildRepo;
	private final EnvSettings envSettings;

	@Autowired
	private Info(final DiscordGuildRepo guildRepo, final EnvSettings envSettings) {
		this.guildRepo = guildRepo;
		this.envSettings = envSettings;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "info";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getUsage() {
		return getName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Shows your current bot settings.";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		dcGuildOpt.ifPresent(discordGuild -> sendInfo(event, discordGuild));
	}

	/**
	 * Sends the info message.
	 * @param event The Discord event with all its information when a message is received.
	 * @param dcGuild The Discord guild as saved in the database.
	 */
	private void sendInfo(final GuildMessageReceivedEvent event, final DiscordGuild dcGuild) {
		final Guild guild = event.getGuild();
		final MessageEmbed embedInfo = buildEmbed(guild, dcGuild);
		answer(event.getChannel(), embedInfo);
	}

	/**
	 * Creates the embedded message for the guild information.
	 * @param guild The guild to display information about.
	 * @param dcGuild The Discord guild as saved in the database.
	 * @return the embedded message for the guild information.
	 */
	private MessageEmbed buildEmbed(final Guild guild, final DiscordGuild dcGuild) {
		return new EmbedBuilder()
				.setTitle("Info for \"" + guild.getName() + "\":")
				.setColor(getEmbedColor())
				.addField("Channel:", "<#" + dcGuild.getStatusChannelId() + ">", true)
				.addField("Role:", "<@&" + dcGuild.getRoleId() + ">", true)
				.setFooter(
						"Interval: " + envSettings.getProductStatusRequestInterval() + " seconds"
				).build();
	}
}

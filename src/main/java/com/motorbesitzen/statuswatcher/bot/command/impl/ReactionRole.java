package com.motorbesitzen.statuswatcher.bot.command.impl;

import com.motorbesitzen.statuswatcher.bot.command.CommandImpl;
import com.motorbesitzen.statuswatcher.data.dao.DiscordGuild;
import com.motorbesitzen.statuswatcher.data.repo.DiscordGuildRepo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Updates the reaction role to a new one.
 */
@Service("reactionrole")
class ReactionRole extends CommandImpl {

	private final DiscordGuildRepo guildRepo;

	@Autowired
	private ReactionRole(final DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "reactionrole";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getUsage() {
		return getName() + " @role";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Update the role to ping on status changes.";
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
	 * Handles the message information to update the role that gets pinged on status changes.
	 * @param event The Discord event with all its information when a message is received.
	 * @param dcGuild The Discord guild as saved in the database.
	 */
	private void handleMessage(final GuildMessageReceivedEvent event, final DiscordGuild dcGuild) {
		final Message message = event.getMessage();
		final List<Role> mentionedRoles = message.getMentionedRoles();
		if (mentionedRoles.size() == 0) {
			sendErrorMessage(event.getChannel(), "You need to mention a role!");
			return;
		}

		final Role mentionedRole = mentionedRoles.get(0);
		final long roleId = mentionedRole.getIdLong();
		dcGuild.setRoleId(roleId);
		guildRepo.save(dcGuild);
		answer(event.getChannel(), "Updated role to mention on changes.");
	}
}

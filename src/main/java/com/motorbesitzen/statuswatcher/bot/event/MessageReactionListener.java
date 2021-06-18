package com.motorbesitzen.statuswatcher.bot.event;

import com.motorbesitzen.statuswatcher.data.dao.DiscordGuild;
import com.motorbesitzen.statuswatcher.data.repo.DiscordGuildRepo;
import com.motorbesitzen.statuswatcher.util.LogUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Handles reactions to messages.
 */
@Service
class MessageReactionListener extends ListenerAdapter {

	private final DiscordGuildRepo guildRepo;

	private MessageReactionListener(final DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	/**
	 * Handles reactions that get added to a message.
	 * @param event The Discord event with all its information when a reaction is added.
	 */
	@Override
	public void onGuildMessageReactionAdd(@Nonnull final GuildMessageReactionAddEvent event) {
		handleEvent(event);
	}

	/**
	 * Handles reactions that get removed from a message.
	 * @param event The Discord event with all its information when a reaction gets removed.
	 */
	@Override
	public void onGuildMessageReactionRemove(@Nonnull final GuildMessageReactionRemoveEvent event) {
		handleEvent(event);
	}

	/**
	 * Handles any reaction event.
	 * @param event The Discord event with all its information about the reaction.
	 */
	private void handleEvent(final GenericGuildMessageReactionEvent event) {
		final Guild guild = event.getGuild();
		final long guildId = guild.getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		dcGuildOpt.ifPresent(
				dcGuild -> handleReaction(event, dcGuild)
		);
	}

	/**
	 * Handles the reaction and assigns the reaction role if the reaction got added. Removes the reaction role
	 * from the member if the reaction got removed.
	 * @param event The Discord event with all its information about the reaction.
	 * @param dcGuild The Discord guild as saved in the database.
	 */
	private void handleReaction(final GenericGuildMessageReactionEvent event, final DiscordGuild dcGuild) {
		final long messageId = event.getMessageIdLong();
		if (!isValidReaction(event, messageId)) {
			return;
		}

		final Guild guild = event.getGuild();
		final long reactionRoleId = dcGuild.getRoleId();
		final Role reactionRole = guild.getRoleById(reactionRoleId);
		if (!canAssignRole(guild, reactionRole)) {
			LogUtil.logWarning("[" + guild.getId() + "] Can not assign reaction role to member.");
			return;
		}

		event.retrieveMember().queue(
				member -> {
					if (event instanceof GuildMessageReactionAddEvent) {
						assignRole(member, reactionRole);
					} else if (event instanceof GuildMessageReactionRemoveEvent) {
						removeRole(member, reactionRole);
					}
				},
				throwable -> LogUtil.logDebug("Could not retrieve member of reaction!")
		);
	}

	/**
	 * Checks if the reaction is a valid reaction in terms of the reaction role.
	 * @param event The Discord event with all its information about the reaction.
	 * @param reactionMessageId The ID of the message the reaction got added to.
	 * @return {@code true} if the reaction is a valid role reaction.
	 */
	private boolean isValidReaction(final GenericGuildMessageReactionEvent event, final long reactionMessageId) {
		final long messageId = event.getMessageIdLong();
		if (messageId != reactionMessageId) {
			return false;
		}

		final String reactionName = event.getReactionEmote().getName();
		if (!reactionName.equalsIgnoreCase("✅")) {
			event.getReaction().clearReactions().queue();
			return false;
		}

		return true;
	}

	/**
	 * Checks if the bot can assign the reaction role.
	 * @param guild The Discord guild.
	 * @param role The role to check.
	 * @return {@code true} if the bot can assign the reaction role.
	 */
	private boolean canAssignRole(final Guild guild, final Role role) {
		if (role == null) {
			return false;
		}

		return guild.getSelfMember().canInteract(role);
	}

	/**
	 * Assigns a role to a member.
	 * @param member The member to add the role to.
	 * @param role The role to assign the member.
	 */
	private void assignRole(final Member member, final Role role) {
		if (member == null) {
			return;
		}

		final Guild guild = member.getGuild();
		guild.addRoleToMember(member, role).queue(
				null,
				throwable -> LogUtil.logError("[" + guild.getId() + "] Unexpected Error while assigning role to member!", throwable)
		);
	}

	/**
	 * Removes a role from a member.
	 * @param member The member to remove the role from
	 * @param role The role to remove from the member.
	 */
	private void removeRole(final Member member, final Role role) {
		if (member == null) {
			return;
		}

		final Guild guild = member.getGuild();
		guild.removeRoleFromMember(member, role).queue(
				null,
				throwable -> LogUtil.logError("[" + guild.getId() + "] Unexpected Error while removing role from member!", throwable)
		);
	}
}

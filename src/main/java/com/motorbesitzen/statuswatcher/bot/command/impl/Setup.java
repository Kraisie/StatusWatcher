package com.motorbesitzen.statuswatcher.bot.command.impl;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.motorbesitzen.statuswatcher.bot.command.CommandImpl;
import com.motorbesitzen.statuswatcher.data.dao.DiscordGuild;
import com.motorbesitzen.statuswatcher.data.repo.DiscordGuildRepo;
import com.motorbesitzen.statuswatcher.util.LogUtil;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Performs the whole setup process. Can be done step by step with the other commands.
 */
@Service("setup")
class Setup extends CommandImpl {

	private static final int TIMEOUT_MINS = 5;
	private final DiscordGuildRepo guildRepo;
	private final EventWaiter eventWaiter;

	@Autowired
	private Setup(final DiscordGuildRepo guildRepo, final EventWaiter eventWaiter) {
		this.guildRepo = guildRepo;
		this.eventWaiter = eventWaiter;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "setup";
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
		return "Perform the setup process.";
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
		startSetupDialog(event, dcGuild);
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
	 * Starts the setup dialog by asking for the status channel.
	 * @param event The Discord event with all its information when a message is received.
	 * @param dcGuild The Discord guild as saved in the database.
	 */
	private void startSetupDialog(final GuildMessageReceivedEvent event, final DiscordGuild dcGuild) {
		final TextChannel originalChannel = event.getChannel();
		final long originalChannelId = originalChannel.getIdLong();
		final long originalAuthorId = event.getAuthor().getIdLong();
		originalChannel.sendMessage("Please mention the channel to send status changes in.").queue(
				msg -> eventWaiter.waitForEvent(
						GuildMessageReceivedEvent.class,
						newEvent -> isValidChannelReply(newEvent, originalChannelId, originalAuthorId),
						newEvent -> handleChannelReply(newEvent, dcGuild),
						TIMEOUT_MINS, TimeUnit.MINUTES,
						() -> sendErrorMessage(originalChannel, "Timeout exceeded. Please start the setup process again.")
				)
		);
	}

	/**
	 * Checks if a reply is valid in the dialog.
	 * @param newEvent The Discord event with all its information when a message is received.
	 * @param originalChannelId The original channel ID of the channel where the dialog happens.
	 * @param originalAuthorId The original authur ID of the user who started the dialog.
	 * @return {@code true} if it is still the same dialog and the answer is valid in regards to the question.
	 */
	private boolean isValidChannelReply(final GuildMessageReceivedEvent newEvent, final long originalChannelId,
										final long originalAuthorId) {
		if (isWrongDialog(newEvent, originalChannelId, originalAuthorId)) {
			return false;
		}

		final Message message = newEvent.getMessage();
		final List<TextChannel> mentionedChannels = message.getMentionedChannels();
		if (mentionedChannels.size() != 1) {
			sendErrorMessage(newEvent.getChannel(), "Please mention exactly one channel! (ID or name does not work)");
			return false;
		}

		if (!mentionedChannels.get(0).canTalk()) {
			sendErrorMessage(newEvent.getChannel(), "Can not talk in that channel! (read/send messages)");
			return false;
		}

		return true;
	}

	/**
	 * Checks if a new message event is part of the original dialog by checking if author and channel match.
	 * @param newEvent The Discord event with all its information when a message is received.
	 * @param originalChannelId The original channel ID of the channel where the dialog happens.
	 * @param originalAuthorId The original author ID of the user who started the dialog.
	 * @return {@code true} if the message is part of the dialog.
	 */
	private boolean isWrongDialog(final GuildMessageReceivedEvent newEvent, final long originalChannelId, final long originalAuthorId) {
		final TextChannel channel = newEvent.getChannel();
		if (channel.getIdLong() != originalChannelId) {
			return true;
		}

		final User author = newEvent.getAuthor();
		return author.getIdLong() != originalAuthorId;
	}

	/**
	 * Handles the data of the message that should determine the status channel and requests the reaction role from the dialog partner.
	 * @param event The Discord event with all its information when a message is received.
	 * @param dcGuild The Discord guild as saved in the database.
	 */
	private void handleChannelReply(final GuildMessageReceivedEvent event, final DiscordGuild dcGuild) {
		updateStatusChannel(event, dcGuild);
		final TextChannel originalChannel = event.getChannel();
		final long originalChannelId = originalChannel.getIdLong();
		final long originalAuthorId = event.getAuthor().getIdLong();
		originalChannel.sendMessage("Please mention the role to ping on status changes.").queue(
				msg -> eventWaiter.waitForEvent(
						GuildMessageReceivedEvent.class,
						newEvent -> isValidRoleReply(newEvent, originalChannelId, originalAuthorId),
						newEvent -> handleRoleReply(newEvent, dcGuild),
						TIMEOUT_MINS, TimeUnit.MINUTES,
						() -> sendErrorMessage(originalChannel, "Timeout exceeded. Please start the setup process again.")
				)
		);
	}

	/**
	 * Updates the status channel in the database representation of the Discord guild. Does *NOT* save it to the
	 * database just yet.
	 * @param event The Discord event with all its information when a message is received.
	 * @param dcGuild The Discord guild as saved in the database.
	 */
	private void updateStatusChannel(final GuildMessageReceivedEvent event, final DiscordGuild dcGuild) {
		final Message message = event.getMessage();
		final List<TextChannel> mentionedChannels = message.getMentionedChannels();
		if (mentionedChannels.size() == 0) {
			LogUtil.logError("[SETUP] Mentioned channels shouldn't be null! Not setting status channel.");
			sendErrorMessage(event.getChannel(), "Can not find mentioned channel!");
			return;
		}

		final TextChannel statusChannel = mentionedChannels.get(0);
		final long statusChannelId = statusChannel.getIdLong();
		dcGuild.setStatusChannelId(statusChannelId);
	}

	/**
	 * Checks if a reply is valid in the dialog.
	 * @param event The Discord event with all its information when a message is received.
	 * @param originalChannelId The original channel ID of the channel where the dialog happens.
	 * @param originalAuthorId The original author ID of the user who started the dialog.
	 * @return {@code true} if it is still the same dialog and the answer is valid in regards to the question.
	 */
	private boolean isValidRoleReply(final GuildMessageReceivedEvent event, final long originalChannelId,
									 final long originalAuthorId) {
		if (isWrongDialog(event, originalChannelId, originalAuthorId)) {
			return false;
		}

		final Message message = event.getMessage();
		final List<Role> mentionedRoles = message.getMentionedRoles();
		if (mentionedRoles.size() != 1) {
			sendErrorMessage(event.getChannel(), "Please mention exactly one role! (ID or name does not work)");
			return false;
		}

		final Role mentionedRole = mentionedRoles.get(0);
		final Guild guild = mentionedRole.getGuild();
		if (!guild.getSelfMember().canInteract(mentionedRole)) {
			sendErrorMessage(event.getChannel(), "I can not assign that role to members! Please choose " +
					"another role or move my role above that role.");
			return false;
		}

		return true;
	}

	/**
	 * Handles the data of the message that should determine the reaction role and informs about the setup success.
	 * Persists the information of the setup process in the database.
	 * @param event The Discord event with all its information when a message is received.
	 * @param dcGuild The Discord guild as saved in the database.
	 */
	private void handleRoleReply(final GuildMessageReceivedEvent event, final DiscordGuild dcGuild) {
		updateReactionRole(event, dcGuild);
		sendReactionMessage(event, dcGuild);
		guildRepo.save(dcGuild);
		answer(event.getChannel(), "Setup process completed!");
	}

	/**
	 * Updates the reaction role in the database representation of the Discord guild. Does *NOT* save it to the
	 * database just yet.
	 * @param event The Discord event with all its information when a message is received.
	 * @param dcGuild The Discord guild as saved in the database.
	 */
	private void updateReactionRole(final GuildMessageReceivedEvent event, final DiscordGuild dcGuild) {
		final Message message = event.getMessage();
		final List<Role> mentionedRoles = message.getMentionedRoles();
		if (mentionedRoles.size() == 0) {
			LogUtil.logError("[SETUP] Mentioned roles shouldn't be null! Not setting reaction role.");
			sendErrorMessage(event.getChannel(), "Can not find mentioned role!");
			return;
		}

		final Role mentionedRole = mentionedRoles.get(0);
		final long mentionedRoleId = mentionedRole.getIdLong();
		dcGuild.setRoleId(mentionedRoleId);
	}

	/**
	 * Sends the reaction message in the status channel at which users can get the reaction role assigned.
	 * @param event The Discord event with all its information when a message is received.
	 * @param dcGuild The Discord guild as saved in the database.
	 */
	private void sendReactionMessage(final GuildMessageReceivedEvent event, final DiscordGuild dcGuild) {
		final long statusChannelId = dcGuild.getStatusChannelId();
		final Guild guild = event.getGuild();
		final TextChannel statusChannel = guild.getTextChannelById(statusChannelId);
		if (statusChannel == null) {
			LogUtil.logError("[SETUP] Status channel doesn't exist anymore.");
			sendErrorMessage(event.getChannel(), "Can not find status channel anymore!");
			return;
		}

		statusChannel.sendMessage("React with ✅ to this message to get informed on status changes.").queue(
				msg -> {
					saveNewMessage(dcGuild, msg);
					addReaction(event.getChannel(), msg);
				},
				throwable -> {
					LogUtil.logError("[SETUP] Could not send reaction message " +
							"in \"" + statusChannel.getName() + "\" of \"" + guild.getName() + "\":", throwable);
					sendErrorMessage(event.getChannel(), "Could not send reaction message!");
				}
		);
	}

	/**
	 * Saves the message ID of the new reaction message in the database.
	 * @param dcGuild The Discord guild as saved in the database.
	 * @param message The new reaction message.
	 */
	private void saveNewMessage(final DiscordGuild dcGuild, final Message message) {
		dcGuild.setReactionMessageId(message.getIdLong());
		guildRepo.save(dcGuild);
	}

	/**
	 * Adds the reaction to the reaction message which users can use to get the reaction role assigned.
	 * @param callerChannel The channel where the command got used.
	 * @param message The message to react to.
	 */
	private void addReaction(final TextChannel callerChannel, final Message message) {
		final Guild guild = message.getGuild();
		message.addReaction("✅").queue(
				v -> LogUtil.logDebug("Added role reaction to new reaction message."),
				throwable -> {
					LogUtil.logError(
							"[SETUP] Could not add role reaction to reaction message " +
									"in \"" + message.getChannel().getName() + "\" of \"" + guild.getName() + "\":", throwable);
					sendErrorMessage(callerChannel, "Could not add reaction to role reaction message! " +
							"Please add the reaction \"✅\" yourself.");
				}
		);
	}
}

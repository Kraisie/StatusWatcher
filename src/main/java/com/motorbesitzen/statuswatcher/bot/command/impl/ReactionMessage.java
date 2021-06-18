package com.motorbesitzen.statuswatcher.bot.command.impl;

import com.motorbesitzen.statuswatcher.bot.command.CommandImpl;
import com.motorbesitzen.statuswatcher.data.dao.DiscordGuild;
import com.motorbesitzen.statuswatcher.data.repo.DiscordGuildRepo;
import com.motorbesitzen.statuswatcher.util.LogUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Triggers a new reaction message. Can be useful if anything breaks.
 */
@Service("reactionmessage")
class ReactionMessage extends CommandImpl {

	private final DiscordGuildRepo guildRepo;

	@Autowired
	private ReactionMessage(final DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "reactionmessage";
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
		return "Triggers a new reaction message in the current status channel. Tries to delete " +
				"the old one but only searches the current status channel. Even if the old one does " +
				"not get deleted it will not assign roles on reaction anymore.";
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
	 * Handles the command information and executes any action that needs to be done. Tries to send
	 * a reaction message and adds the needed reaction to it.
	 *
	 * @param event The Discord event with all its information when a message is received.
	 * @param dcGuild The Discord guild as saved in the database.
	 */
	private void handleMessage(final GuildMessageReceivedEvent event, final DiscordGuild dcGuild) {
		final Guild guild = event.getGuild();
		final long statusChannelId = dcGuild.getStatusChannelId();
		final TextChannel statusChannel = guild.getTextChannelById(statusChannelId);
		if (statusChannel == null) {
			sendErrorMessage(event.getChannel(), "There is no status channel set or it does not exist!");
			return;
		}

		if (!statusChannel.canTalk()) {
			sendErrorMessage(event.getChannel(), "I can not access the current status channel! " +
					"Please fix the channel permissions or use another channel as the status channel.");
			return;
		}

		statusChannel.sendMessage("React with ✅ to this message to get informed on status changes.").queue(
				msg -> {
					deleteOldMessage(statusChannel, dcGuild.getReactionMessageId());
					saveNewMessage(dcGuild, msg);
					addRoleReaction(event.getChannel(), msg);
				},
				throwable -> {
					LogUtil.logError(
							"[" + dcGuild.getId() + "] Could not send new reaction message " +
									"in \"" + statusChannel.getName() + "\" of \"" + guild.getName() + "\":", throwable);
					sendErrorMessage(event.getChannel(), "Could not send reaction message!");
				}
		);
	}

	/**
	 * Tries to delete the old status message if it can still access/find it.
	 * @param statusChannel The current status channel of the guild.
	 * @param messageId The message ID of the last reaction message.
	 */
	private void deleteOldMessage(final TextChannel statusChannel, final long messageId) {
		final Guild guild = statusChannel.getGuild();
		statusChannel.retrieveMessageById(messageId).queue(
				oldMsg -> oldMsg.delete().queue(
						v -> LogUtil.logDebug("Deleted old status message."),
						throwable -> LogUtil.logError("[" + guild.getId() + "] Could not delete old " +
								"reaction message in \"" + statusChannel.getName() + "\" of \"" +
								guild.getName() + "\":", throwable)
				),
				throwable -> LogUtil.logDebug("Old reaction message not found!")
		);
	}

	/**
	 * Saves information about the new reaction message in the database.
	 * @param dcGuild The Discord guild that 'owns' this reaction message.
	 * @param message The actual reaction message.
	 */
	private void saveNewMessage(final DiscordGuild dcGuild, final Message message) {
		dcGuild.setReactionMessageId(message.getIdLong());
		guildRepo.save(dcGuild);
	}

	/**
	 * Tries to add the needed reaction to the reaction message.
	 * @param callerChannel The text channel in which the command got used in.
	 * @param message The message to add the reaction to - the reaction message.
	 */
	private void addRoleReaction(final TextChannel callerChannel, final Message message) {
		final Guild guild = message.getGuild();
		final TextChannel statusChannel = message.getTextChannel();
		message.addReaction("✅").queue(
				v -> LogUtil.logDebug("Added role reaction to new reaction message."),
				throwable -> {
					LogUtil.logError(
							"[" + guild.getId() + "] Could not add role reaction to reaction message " +
									"in \"" + statusChannel.getName() + "\" of \"" + guild.getName() + "\":", throwable);
					sendErrorMessage(callerChannel, "Could not add reaction to role reaction message! " +
							"Please add the reaction \"✅\" yourself.");
				}
		);
	}
}

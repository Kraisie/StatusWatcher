package com.motorbesitzen.statuswatcher.bot.service;

import com.motorbesitzen.statuswatcher.bot.scraper.ProductStatusApiRequest;
import com.motorbesitzen.statuswatcher.bot.scraper.entity.ProductStatus;
import com.motorbesitzen.statuswatcher.data.ProductStatusAliasMapper;
import com.motorbesitzen.statuswatcher.data.dao.DiscordGuild;
import com.motorbesitzen.statuswatcher.data.repo.DiscordGuildRepo;
import com.motorbesitzen.statuswatcher.util.LogUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The part of the bot that checks the product status list periodically and sends a message on changes.
 */
@Service
class StatusChecker {

	private static final int START_DELAY_MS = 5000;
	private final JDA jda;
	private final DiscordGuildRepo guildRepo;
	private final EnvSettings envSettings;
	private final ProductStatusApiRequest apiRequest;
	private final ProductStatusAliasMapper aliasMapper;
	private final ScheduledExecutorService scheduler;
	private List<ProductStatus> lastStatusList;

	@Autowired
	private StatusChecker(final JDA jda, final DiscordGuildRepo guildRepo, final EnvSettings envSettings,
						 final ProductStatusApiRequest apiRequest, final ProductStatusAliasMapper aliasMapper) {
		this.jda = jda;
		this.guildRepo = guildRepo;
		this.envSettings = envSettings;
		this.apiRequest = apiRequest;
		this.aliasMapper = aliasMapper;
		this.lastStatusList = new ArrayList<>();
		this.scheduler = Executors.newScheduledThreadPool(1);
	}

	/**
	 * Starts the ScheduledExecutorService to periodically check the status.
	 */
	void start() {
		final long delayMs = envSettings.getProductStatusRequestInterval();
		scheduler.scheduleWithFixedDelay(this::run, START_DELAY_MS, delayMs, TimeUnit.MILLISECONDS);
		LogUtil.logInfo("Starting status checker...");
	}

	/**
	 * Performs the check and the underlying procedures on a status change.
	 * Gets called periodically by the ScheduledExecutorService.
	 */
	private void run() {
		try {
			doStatusCheck();
		} catch (Exception e) {
			LogUtil.logError("Unexpected exception:", e);
		} catch (Throwable t) {
			LogUtil.logError("Unexpected error:", t);
		}
	}

	/**
	 * Requests the current status list and compares it to the one from the last cycle. Sends a message if
	 * there are status changes.
	 */
	private void doStatusCheck() {
		final List<ProductStatus> statusList;
		try {
			statusList = apiRequest.getStatusList();
		} catch (SocketTimeoutException e) {
			LogUtil.logError("Skipping check due to API timeout.");
			return;
		} catch (IOException e) {
			LogUtil.logError("Skipping check due to IO exception:", e);
			return;
		}

		if (lastStatusList.isEmpty()) {
			lastStatusList = statusList;
			return;
		}

		final List<String> statusChanges = getStatusChanges(statusList);
		if (!statusChanges.isEmpty()) {
			sendStatusUpdate(statusChanges);
		}

		lastStatusList = statusList;
	}

	/**
	 * Compares the last and the current status list to each other and checks for status changes. Creates a textual
	 * list of status changes indicating the product and the old and new status.
	 * @param statusList The current status list.
	 * @return A ist of changes in text form.
	 */
	private List<String> getStatusChanges(final List<ProductStatus> statusList) {
		final List<String> statusChanges = new ArrayList<>();
		for (ProductStatus newProduct : statusList) {
			for (ProductStatus oldProduct : lastStatusList) {
				final String newProductName = newProduct.getProductName();
				final String oldProductName = oldProduct.getProductName();
				if (!newProductName.equals(oldProductName)) {
					continue;
				}

				final String newProductStatus = aliasMapper.getAliasOf(newProduct.getProductStatus());
				final String oldProductStatus = aliasMapper.getAliasOf(oldProduct.getProductStatus());
				if (!newProductStatus.equals(oldProductStatus)) {
					statusChanges.add("**" + newProductName + ":** " + oldProductStatus + " → " + newProductStatus);
				}
			}
		}

		return statusChanges;
	}

	/**
	 * Starts the process of sending a message on status changes in each of the guilds the bot is in.
	 * @param statusChanges The textual list of product status changes.
	 */
	private void sendStatusUpdate(final List<String> statusChanges) {
		final List<Guild> guilds = jda.getGuilds();
		for (Guild guild : guilds) {
			final long guildId = guild.getIdLong();
			final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
			dcGuildOpt.ifPresent(
					dcGuild -> sendTagMessage(guild, dcGuild, statusChanges)
			);
		}
	}

	/**
	 * Sends the actual change message and tags the reaction role if there is one set.
	 * @param guild The guild to send the change message in.
	 * @param dcGuild The Discord guild as saved in the database.
	 * @param statusChanges The textual list of product status changes.
	 */
	private void sendTagMessage(final Guild guild, final DiscordGuild dcGuild, final List<String> statusChanges) {
		final long channelId = dcGuild.getStatusChannelId();
		final TextChannel channel = guild.getTextChannelById(channelId);
		if (channel == null) {
			dcGuild.setStatusChannelId(0);
			guildRepo.save(dcGuild);
			LogUtil.logInfo("[" + dcGuild.getId() + "] Removed status channel due to channel not existing anymore.");
			return;
		}

		if (!channel.canTalk()) {
			LogUtil.logWarning("[" + dcGuild.getId() + "] Missing permissions for status channel in guild \"" + guild.getName() + "\"!");
			return;
		}

		final long roleId = dcGuild.getRoleId();
		final Role role = guild.getRoleById(roleId);
		final String roleMention = role != null ? role.getAsMention() : "";
		final String changeMessage = buildChangeMessage(roleMention, statusChanges);
		final MessageEmbed changeListEmbed = buildChangeListEmbed(statusChanges);
		channel.sendMessage(changeMessage).setEmbeds(changeListEmbed).queue(
				msg -> msg.delete().queueAfter(
						envSettings.getDeletionDelay(), TimeUnit.HOURS,
						v -> LogUtil.logDebug("Deleted status change message successfully."),
						throwable -> LogUtil.logError("Could not delete status change message:", throwable)
				),
				throwable -> LogUtil.logError("Could not send status change message:", throwable)
		);
	}

	/**
	 * Builds the summary of how many statuses have changes since last time.
	 * @param roleMention The reaction role as a Discord mention that leads to a ping in the Discord client for users
	 *                    with that role.
	 * @param statusChanges The textual list of product status changes.
	 * @return a short summary text about the changes.
	 */
	private String buildChangeMessage(final String roleMention, final List<String> statusChanges) {
		final String summary = statusChanges.size() == 1 ?
				statusChanges.size() + " status has changed!" :
				statusChanges.size() + " statuses have changed!";
		return roleMention.isBlank() ? summary : roleMention + "\n" + summary;
	}

	/**
	 * Creates an embedded message about the product status changes.
	 * @param statusChanges The textual list of product status changes.
	 * @return An embedded message informing about the changes.
	 */
	private MessageEmbed buildChangeListEmbed(final List<String> statusChanges) {
		final String changeListText = buildChangeListText(statusChanges);
		final EmbedBuilder eb = new EmbedBuilder();
		eb.setDescription(changeListText);
		return eb.build();
	}

	/**
	 * Builds the list of changes for the embedded message.
	 * @param statusChanges The textual list of product status changes.
	 * @return a textual concatenation of changes.
	 */
	private String buildChangeListText(final List<String> statusChanges) {
		final StringBuilder sb = new StringBuilder();
		for (String statusChange : statusChanges) {
			sb.append(statusChange).append("\n");
		}

		sb.setLength(sb.length() - 1);
		return sb.toString();
	}
}

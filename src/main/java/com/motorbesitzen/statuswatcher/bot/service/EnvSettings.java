package com.motorbesitzen.statuswatcher.bot.service;

import com.motorbesitzen.statuswatcher.util.ParseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * The class that handles the environment variables.
 */
@Component
public class EnvSettings {

	private final Environment environment;

	@Autowired
	EnvSettings(final Environment environment) {
		this.environment = environment;
	}

	/**
	 * Defines the Discord token that is used to control the bot.
	 *
	 * @return The bot token if there is one set, {@code null} if there is none set.
	 */
	public String getToken() {
		return environment.getProperty("DC_TOKEN");
	}

	/**
	 * Defines the command prefix which has to be used to mark a message as an command. A message like
	 * "?help" will get identified as the help command if the command prefix is "?". If no prefix is set there
	 * is no special prefix needed to use a command. Thus "help" would trigger the help command.
	 *
	 * @return The set command prefix if there is one. If there is none set it returns an empty String.
	 */
	public String getCommandPrefix() {
		return environment.getProperty("CMD_PREFIX", "");
	}

	/**
	 * Defines the product status API URL that gets used to request the status of the listed products.
	 *
	 * @return The set product status API URL if there is one. If there is none set it returns an empty String.
	 */
	public String getProductStatusApiUrl() {
		return environment.getProperty("PRODUCT_STATUS_API_URL", "");
	}

	/**
	 * Defines the interval between product status requests. If the interval is set to be above 24h it defaults to
	 * 24 hours, if it is below one second it defaults to one second. If the interval is not given or set as -1
	 * it defaults to a 60 second interval.
	 *
	 * @return The interval between product status API requests in Milliseconds (ms). If there is none set it returns
	 * the default of 60000ms which is 60 seconds between each request.
	 */
	public long getProductStatusRequestInterval() {
		final String intervalText = environment.getProperty("PRODUCT_STATUS_REQUEST_INTERVAL_MS", "60000");
		final long interval = ParseUtil.safelyParseStringToLong(intervalText);
		if (interval == -1) {
			return 60000;
		}
		return Math.max(1000, Math.min(86400000, interval));
	}

	/**
	 * Defines the time in hours until the bot deletes its messages. If the delay is set to be above 24h it defaults to
	 * 24 hours, if it is below one hour or invalid it defaults to an one hour delay.
	 *
	 * @return The time in hours until the bot deletes its messages.
	 */
	public int getDeletionDelay() {
		final String deletionDelayText = environment.getProperty("DELETION_DELAY_HRS");
		final int delay = ParseUtil.safelyParseStringToInt(deletionDelayText);
		return Math.max(1, Math.min(24, delay));
	}
}

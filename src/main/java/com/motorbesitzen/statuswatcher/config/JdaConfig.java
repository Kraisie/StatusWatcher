package com.motorbesitzen.statuswatcher.config;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.motorbesitzen.statuswatcher.bot.service.EnvSettings;
import com.motorbesitzen.statuswatcher.util.LogUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.security.auth.login.LoginException;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Provides beans for any JDA and JDA-Utilities related class.
 */
@Configuration
public class JdaConfig {

	/**
	 * Provides a Bean of the EventWaiter of the JDA-Utilities to Spring.
	 * @return an EventWaiter object.
	 */
	@Bean
	public EventWaiter buildEventWaiter() {
		// let spring control the EvenWaiter as it is recommended to only use 1 instance
		return new EventWaiter(Executors.newSingleThreadScheduledExecutor(), false);
	}

	/**
	 * Provides the JDA object by starting the bot. If the bot can not be started the application gets stopped.
	 *
	 * @param envSettings        The class that handles the environment variables.
	 * @param eventListeners     A list of event listeners.
	 * @param applicationContext The Spring application context.
	 * @return The 'core object' of the bot, the JDA.
	 */
	@Bean
	JDA startBot(final EnvSettings envSettings, final Map<String, ? extends ListenerAdapter> eventListeners,
				 final ApplicationContext applicationContext, final EventWaiter eventWaiter) {
		final String discordToken = getToken(envSettings, applicationContext);
		final JDABuilder jdaBuilder = buildBot(discordToken, eventListeners, eventWaiter);
		final JDA jda = botLogin(jdaBuilder);
		if (jda == null) {
			shutdown(applicationContext);
			return null;
		}

		return jda;
	}

	/**
	 * Gets the token from the environment variables. Stops the application if no token is set.
	 *
	 * @param envSettings        The class that handles the environment variables.
	 * @param applicationContext The Spring application context.
	 * @return The token as a {@code String}.
	 */
	private String getToken(final EnvSettings envSettings, final ApplicationContext applicationContext) {
		final String discordToken = envSettings.getToken();
		if (discordToken == null) {
			LogUtil.logError("RoleWatcher Discord token is null! Please check the environment variables and add a token.");
			shutdown(applicationContext);
			return null;
		}

		if (discordToken.isBlank()) {
			LogUtil.logError("RoleWatcher Discord token is empty! Please check the environment variables and add a token.");
			shutdown(applicationContext);
			return null;
		}

		return discordToken;
	}

	/**
	 * Initializes the bot with the needed information.
	 *
	 * @param discordToken   The Discord token of the bot.
	 * @param eventListeners A list of event listeners.
	 * @return A <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/JDA.html">JDA instance</a> of the bot.
	 */
	private JDABuilder buildBot(final String discordToken,
								final Map<String, ? extends ListenerAdapter> eventListeners,
								final EventWaiter eventWaiter) {
		final Activity activity = Activity.watching("status");
		final JDABuilder builder =
				JDABuilder.createLight(
						discordToken,
						EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS)
				).setStatus(OnlineStatus.ONLINE).setActivity(activity)
						.addEventListeners(eventWaiter);

		for (Map.Entry<String, ? extends ListenerAdapter> eventListener : eventListeners.entrySet()) {
			builder.addEventListeners(eventListener.getValue());
		}

		return builder;
	}

	/**
	 * Logs in the bot to the API.
	 *
	 * @param builder The builder that is supposed to generate the JDA instance.
	 * @return The JDA instance, the 'core' of the API/the bot.
	 */
	private JDA botLogin(final JDABuilder builder) {
		try {
			return builder.build();
		} catch (LoginException e) {
			LogUtil.logError("Token is invalid! Please check your token and add a valid Discord token.");
			LogUtil.logDebug(e.getMessage());
		}

		return null;
	}

	/**
	 * Gracefully stops the Spring application and the JVM afterwards.
	 *
	 * @param applicationContext The Spring application context.
	 */
	private void shutdown(final ApplicationContext applicationContext) {
		SpringApplication.exit(applicationContext, () -> 1);
		System.exit(1);
	}
}

package com.motorbesitzen.statuswatcher.bot.service;

import com.motorbesitzen.statuswatcher.util.LogUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

/**
 * Gets created when the Spring application is ready and thus is able to start the bot.
 * Initiates the bot start up.
 */
@Service
class BotBuilder implements ApplicationListener<ApplicationReadyEvent> {

	private final StatusChecker statusChecker;

	@Autowired
	private BotBuilder(final StatusChecker statusChecker) {
		this.statusChecker = statusChecker;
	}

	/**
	 * Gets called by spring as late as conceivably possible to indicate that the application is ready.
	 * Starts the StatusChecker and by that the underlying bot.
	 *
	 * @param event Event provided by Spring when the Spring application is ready.
	 */
	@Override
	public void onApplicationEvent(@NotNull final ApplicationReadyEvent event) {
		LogUtil.logInfo("Application ready, starting the status checker...");
		statusChecker.start();
	}
}

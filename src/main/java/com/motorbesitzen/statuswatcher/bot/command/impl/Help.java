package com.motorbesitzen.statuswatcher.bot.command.impl;

import com.motorbesitzen.statuswatcher.bot.command.Command;
import com.motorbesitzen.statuswatcher.bot.command.CommandImpl;
import com.motorbesitzen.statuswatcher.bot.service.EnvSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A command that display the available commands and how to use them.
 */
@Service("help")
class Help extends CommandImpl {

	private static final int FIELDS_PER_EMBED = 25;
	private final EnvSettings envSettings;
	private final Map<String, Command> commandMap;

	@Autowired
	private Help(final EnvSettings envSettings, final Map<String, Command> commandMap) {
		this.envSettings = envSettings;
		this.commandMap = commandMap;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "help";
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
		return "Shows all commands and information on how to use them.";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final TextChannel channel = event.getChannel();
		final List<Command> commands = new ArrayList<>(commandMap.values());
		if (commands.size() == 0) {
			sendErrorMessage(channel, "No commands found!");
			return;
		}

		final int pages = (commands.size() / FIELDS_PER_EMBED) + 1;
		EmbedBuilder eb = buildEmbedPage(1, pages);
		for (int i = 0; i < commands.size(); i++) {
			if (i > 0 && i % 25 == 0) {
				answer(channel, eb.build());
				eb = buildEmbedPage((i / FIELDS_PER_EMBED) + 1, pages);
			}

			final Command command = commands.get(i);
			addHelpEntry(eb, command);
		}

		answer(channel, eb.build());
	}

	/**
	 * Creates the EmbedBuilder for an embedded page with color, title and description.
	 * @param page The number of the page this EmbedBuilder represents.
	 * @param totalPages The total number of pages.
	 * @return the EmbedBuilder for an embedded page with color, title and description.
	 */
	private EmbedBuilder buildEmbedPage(final int page, final int totalPages) {
		return new EmbedBuilder()
				.setColor(getEmbedColor())
				.setTitle(
						page == 1 && totalPages == 1 ?
								"Commands and their variations" :
								"Commands and their variations [" + page + "/" + totalPages + "]"
				).setDescription(
						"A list of all commands you can use and what they do. "
				);
	}

	/**
	 * Adds an entry to the EmbedBuilder for a command.
	 * @param eb The EmbedBuilder to add entries to.
	 * @param command The command to add an entry to.
	 */
	private void addHelpEntry(final EmbedBuilder eb, final Command command) {
		final String prefix = envSettings.getCommandPrefix();
		final String title = prefix + command.getUsage();
		eb.addField(title, command.getDescription(), false);
	}
}

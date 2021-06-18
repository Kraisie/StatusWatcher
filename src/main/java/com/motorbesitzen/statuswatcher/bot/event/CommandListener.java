package com.motorbesitzen.statuswatcher.bot.event;

import com.motorbesitzen.statuswatcher.bot.command.Command;
import com.motorbesitzen.statuswatcher.bot.service.EnvSettings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Checks all incoming messages for commands and the needed permissions to use it. If all data is correct it
 * executes the command. Commands are limited to messages inside of guilds so this listener does not check for
 * messages sent in a private channel (like direct messages).
 */
@Service
class CommandListener extends ListenerAdapter {

	/**
	 * Contains all Command subclasses (and CommandImpl itself) which are registered as a Bean
	 */
	private final Map<String, Command> commandMap;
	private final EnvSettings envSettings;

	/**
	 * Private constructor to be used by Spring autowiring.
	 *
	 * @param commandMap A {@code Map} of Beans that implement the {@link Command}
	 *                   interface. The map contains the name of the Bean  as {@code String}
	 *                   (key) and the implementation (value).
	 */
	@Autowired
	private CommandListener(final Map<String, Command> commandMap, final EnvSettings envSettings) {
		this.commandMap = commandMap;
		this.envSettings = envSettings;
	}

	/**
	 * Gets triggered by a JDA <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/events/message/guild/GuildMessageReceivedEvent.html">GuildMessageReceivedEvent</a>
	 * which gets fired each time the bot receives a message in a <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 * of a <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Guild.html">Guild</a>.
	 * Performs all needed steps to verify if a message is a valid command by an authorized
	 * <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Member.html">Member</a>.
	 * Calls the commands method to execute the command on success.
	 *
	 * @param event The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/events/message/guild/GuildMessageReceivedEvent.html">GuildMessageReceivedEvent</a>
	 *              provided by JDA.
	 */
	@Override
	public void onGuildMessageReceived(@Nonnull final GuildMessageReceivedEvent event) {
		// check if valid message
		final Message message = event.getMessage();
		if (!isValidMessage(message)) {
			return;
		}

		// check if channel is valid for command usage
		final TextChannel channel = event.getChannel();
		if (!isValidChannel(channel)) {
			return;
		}

		// check if valid command prefix
		final String cmdPrefix = envSettings.getCommandPrefix();
		final String messageContent = message.getContentRaw();
		if (!isValidCommandPrefix(cmdPrefix, messageContent)) {
			return;
		}

		// check if user has the needed permissions
		final Member author = event.getMember();
		if (!isAuthorizedAuthor(author)) {
			return;
		}

		// identify command
		final String commandName = identifyCommandName(cmdPrefix, messageContent);
		final Command command = commandMap.get(commandName);
		if (command == null) {
			return;
		}

		executeCommand(event, command);
	}

	/**
	 * Checks if the message is invalid e.g. due to the author being a bot or due to the message being a webhook
	 * message.
	 *
	 * @param message The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Message.html">Message</a>
	 *                which the bot received.
	 * @return {@code true} if the message is valid, {@code false} if the message is invalid.
	 */
	private boolean isValidMessage(final Message message) {
		if (message.getType() != MessageType.DEFAULT) {
			return false;
		}

		if (message.getAuthor().isBot()) {
			return false;
		}

		return !message.isWebhookMessage();
	}

	/**
	 * Checks if the message uses the correct command prefix for the bot which is defined in the environment variables.
	 *
	 * @param cmdPrefix      The used command prefix by the bot.
	 * @param messageContent The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Message.html#getContentRaw()">raw content of the Message</a>
	 *                       which the bot received.
	 * @return {@code true} if the prefix is valid, {@code false} if the prefix is invalid.
	 */
	private boolean isValidCommandPrefix(final String cmdPrefix, final String messageContent) {
		return messageContent.startsWith(cmdPrefix);
	}

	/**
	 * Checks if the channel is valid where the bot received the message.
	 *
	 * @param channel The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                in which the message got sent.
	 * @return {@code true} if the channel is valid as the bot can answer to a command, {@code false} if the bot can not
	 * send messages in the channel
	 */
	private boolean isValidChannel(final TextChannel channel) {
		return channel.canTalk();
	}

	/**
	 * Checks if the author of the command has permission to use the command.
	 *
	 * @param author The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Member.html">Member</a>
	 *               that sent the message.
	 * @return {@code true} if the author has the needed permission to use the command, {@code false} if not.
	 */
	private boolean isAuthorizedAuthor(final Member author) {
		if (author == null) {
			return false;
		}

		return author.hasPermission(Permission.ADMINISTRATOR);
	}

	/**
	 * Identifies the command name of the used command in the message.
	 *
	 * @param cmdPrefix      The used command prefix by the bot.
	 * @param messageContent The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Message.html#getContentRaw()">raw content of the Message</a>
	 *                       which the bot received.
	 * @return the name of the used command in lower case.
	 */
	private String identifyCommandName(final String cmdPrefix, final String messageContent) {
		final String[] tokens = messageContent.split(" ");
		final String fullCommand = tokens[0];
		final String commandName = fullCommand.replace(cmdPrefix, "");
		return commandName.toLowerCase();        // lower case is needed for the matching to work in any case! DO NOT remove it!
	}

	/**
	 * Executes a command and handles exception if the bot does not have the needed permissions to
	 * execute that command in the channel/guild.
	 *
	 * @param event   The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/events/message/guild/GuildMessageReceivedEvent.html">GuildMessageReceivedEvent</a>
	 *                provided by JDA.
	 * @param command The command to execute.
	 */
	private void executeCommand(final GuildMessageReceivedEvent event, final Command command) {
		try {
			command.execute(event);
		} catch (InsufficientPermissionException e) {
			String message = "Bot does not have the needed permission " + e.getPermission() + " for that command.";
			event.getChannel().sendMessage(message).queue();
		}
	}
}

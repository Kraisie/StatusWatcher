package com.motorbesitzen.statuswatcher.bot.command;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.concurrent.TimeUnit;

/**
 * Basic implementation of a Command. Has all needed methods to send messages, answer to commands and log (debug) actions.
 * All subclasses (Commands) can use these functions.
 */
@Service
public abstract class CommandImpl implements Command {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public abstract String getName();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public abstract String getUsage();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public abstract String getDescription();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public abstract void execute(final GuildMessageReceivedEvent event);

	/**
	 * Sends an answer to a channel. Does not do anything different than {@link #sendMessage(TextChannel, String)} but
	 * clarifies that the message will be send as an answer to a command in the caller channel.
	 *
	 * @param channel <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                to send the message in.
	 * @param message The message content to send as answer.
	 */
	protected void answer(final TextChannel channel, final String message) {
		sendMessage(channel, message);
	}

	/**
	 * Sends an embedded message as answer to a channel. Does not do anything different than
	 * {@link #sendMessage(TextChannel, MessageEmbed)} but clarifies that the message will be send as an answer to a
	 * command in the caller channel.
	 *
	 * @param channel <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                to send the message in.
	 * @param message The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/MessageEmbed.html">embedded message</a>
	 *                to send as answer.
	 */
	protected void answer(final TextChannel channel, final MessageEmbed message) {
		sendMessage(channel, message);
	}

	/**
	 * Sends a message to a channel. Does not do anything if bot can not write in that channel.
	 *
	 * @param channel <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                to send the message in.
	 * @param message The message content to send as answer.
	 */
	protected void sendMessage(final TextChannel channel, final String message) {
		if (channel.canTalk()) {
			channel.sendMessage(message).queue();
		}
	}

	/**
	 * Sends an embedded message to a channel. Does not do anything if bot can not write in that channel.
	 *
	 * @param channel <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                to send the message in.
	 * @param message The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/MessageEmbed.html">embedded message</a>
	 *                to send as answer.
	 */
	protected void sendMessage(final TextChannel channel, final MessageEmbed message) {
		if (channel.canTalk()) {
			channel.sendMessage(message).queue();
		}
	}

	/**
	 * Used to clarify in the code that an error message is sent, doesn't do anything else than {@link #sendMessage(TextChannel, String)}.
	 *
	 * @param channel      The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                     where the original message is located in.
	 * @param errorMessage The error message to send.
	 */
	protected void sendErrorMessage(final TextChannel channel, final String errorMessage) {
		sendMessage(channel, errorMessage);
	}

	/**
	 * Defines the default color for an embed message.
	 *
	 * @return the default color for an embed message.
	 */
	protected Color getEmbedColor() {
		return new Color(222, 105, 12);
	}
}

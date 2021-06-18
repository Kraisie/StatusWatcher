package com.motorbesitzen.statuswatcher.data.dao;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class DiscordGuild {

	@Id
	private long id;

	private long statusChannelId;

	private long roleId;

	private long reactionMessageId;

	// JPA
	protected DiscordGuild() {
	}

	protected DiscordGuild(long guildId) {
		this.id = guildId;
	}

	public static DiscordGuild createDefault(long guildId) {
		return new DiscordGuild(guildId);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getStatusChannelId() {
		return statusChannelId;
	}

	public void setStatusChannelId(long statusChannelId) {
		this.statusChannelId = statusChannelId;
	}

	public long getRoleId() {
		return roleId;
	}

	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}

	public long getReactionMessageId() {
		return reactionMessageId;
	}

	public void setReactionMessageId(long reactionMessageId) {
		this.reactionMessageId = reactionMessageId;
	}
}

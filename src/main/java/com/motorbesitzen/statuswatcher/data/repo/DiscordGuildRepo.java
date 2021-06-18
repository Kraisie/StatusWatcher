package com.motorbesitzen.statuswatcher.data.repo;

import com.motorbesitzen.statuswatcher.data.dao.DiscordGuild;
import org.springframework.data.repository.CrudRepository;

public interface DiscordGuildRepo extends CrudRepository<DiscordGuild, Long> {

}

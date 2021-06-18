package com.motorbesitzen.statuswatcher.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motorbesitzen.statuswatcher.data.ProductStatusAliasMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * Configures the Mapper that saves aliases for the status names of the product status APIs.
 */
@Configuration
class ProductStatusAliasMapperConfig {

	private static final String CONFIG_FILE = "statusconfig.json";

	/**
	 * Imports the content of the JSON config file and creates a mapper with that information.
	 * Does not create aliases if the file does not exist or contains any error.
	 *
	 * @param mapper A ObjectMapper to marshall the JSON of the config file.
	 * @return A mapper that maps status names to aliases the bot shall use instead.
	 */
	@Bean
	ProductStatusAliasMapper createProductStatusAliasMapper(final ObjectMapper mapper) {
		final Path configPath = Paths.get(CONFIG_FILE);
		final File configFile = configPath.toFile();
		if (!configFile.exists()) {
			return new ProductStatusAliasMapper(new HashMap<>());
		}

		try {
			return mapper.readValue(configFile, ProductStatusAliasMapper.class);
		} catch (IOException e) {
			return new ProductStatusAliasMapper(new HashMap<>());
		}
	}
}

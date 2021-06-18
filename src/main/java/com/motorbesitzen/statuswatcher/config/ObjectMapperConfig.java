package com.motorbesitzen.statuswatcher.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Spring controlled Jackson ObjectMapper for JSON.
 */
@Configuration
class ObjectMapperConfig {

	/**
	 * Creates the ObjectMapper with some config changes.
	 * @return a 'custom' ObjectMapper.
	 */
	@Bean
	ObjectMapper createObjectMapper() {
		return new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true);
	}
}

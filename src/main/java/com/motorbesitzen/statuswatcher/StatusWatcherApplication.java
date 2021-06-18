package com.motorbesitzen.statuswatcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts everything, the 'main' class of the application.
 */
@SpringBootApplication
public class StatusWatcherApplication {

	/**
	 * Starts anything Spring related.
	 * @param args The start arguments used to start the program.
	 */
	public static void main(String[] args) {
		SpringApplication.run(StatusWatcherApplication.class, args);
	}

}

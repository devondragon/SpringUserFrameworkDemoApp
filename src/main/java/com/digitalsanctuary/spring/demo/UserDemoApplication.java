package com.digitalsanctuary.spring.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class UserApplication. Basic Spring Boot Application Setup. Adds Async support and Scheduling support to the default Spring Boot stack.
 */
@Slf4j
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class UserDemoApplication {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		log.info("Starting UserDemoApplication...");
		SpringApplication.run(UserDemoApplication.class, args);
		log.info("UserDemoApplication started.");
	}

}

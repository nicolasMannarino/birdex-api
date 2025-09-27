package com.birdex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BirdexApplication {

	public static void main(String[] args) {
		SpringApplication.run(BirdexApplication.class, args);
	}

}

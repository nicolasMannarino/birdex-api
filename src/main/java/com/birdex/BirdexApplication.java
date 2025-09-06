package com.birdex;

import java.util.TimeZone;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class BirdexApplication {

	@Bean
	ApplicationRunner debugProps(Environment env) {
	return args -> {
		System.out.println(">> JDBC URL = " + env.getProperty("spring.datasource.url"));
		System.out.println(">> JVM TZ   = " + TimeZone.getDefault().getID());
	};
	}

	static {
    TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
    System.setProperty("user.timezone", "America/Argentina/Buenos_Aires");
  	}
	
	public static void main(String[] args) {
		SpringApplication.run(BirdexApplication.class, args);
	}

}


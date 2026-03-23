package com.spring.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

import com.spring.starter.common.config.JwtProperties;
import com.spring.starter.common.config.OAuth2Properties;

@SpringBootApplication
@EnableConfigurationProperties({
    JwtProperties.class,
	OAuth2Properties.class,
})
@EnableAsync   // Required for @Async in MailService
public class MySpringBootStarterApplication {

	public static void main(String[] args) {
		SpringApplication.run(MySpringBootStarterApplication.class, args);
	}

}

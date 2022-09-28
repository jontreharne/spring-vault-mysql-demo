package com.example.mysqldemo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
@Slf4j
public class MysqlDemoApplication {
	@Value("${spring.datasource.username}")
	private String dbUser;

	@Value("${spring.datasource.password}")
	private String dbPass;
	@Value("${secret:not_found}")
	private String secret;

	public static void main(String[] args) {
		SpringApplication.run(MysqlDemoApplication.class, args);
	}
	@PostConstruct
	public void initIt() throws Exception {
		log.info("Got DB User: " + dbUser);
		log.info("Got DB Pass: " + dbPass);
		log.info("Got secret: " + secret);
	}
}

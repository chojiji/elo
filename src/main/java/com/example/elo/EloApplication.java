package com.example.elo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class EloApplication {

	public static void main(String[] args) {
		SpringApplication.run(EloApplication.class, args);
	}
}

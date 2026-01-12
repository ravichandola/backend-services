package com.demo.payment;

import com.demo.payment.config.DotenvApplicationContextInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(PaymentApplication.class);
		// Add initializer to load .env file
		application.addInitializers(new DotenvApplicationContextInitializer());
		application.run(args);
	}

}

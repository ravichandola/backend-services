package com.demo.gateway;

import com.demo.gateway.config.DotenvApplicationContextInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(GatewayApplication.class);
        application.addInitializers(new DotenvApplicationContextInitializer());
        application.run(args);
    }
}

package com.demo.gateway;

import com.demo.gateway.config.DotenvApplicationContextInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
	org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(GatewayApplication.class);
        application.addInitializers(new DotenvApplicationContextInitializer());
        application.run(args);
    }
}

package com.racines_app_back.www;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;

@SpringBootApplication(exclude = { OAuth2ClientAutoConfiguration.class })
public class RacinesAppBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(RacinesAppBackApplication.class, args);
    }

}

package com.racines_app_back.www.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(createInfo())
                .servers(createServers());
    }

    private Info createInfo() {
        return new Info()
                .title("Racines App API")
                .version("1.0.0")
                .description("API pour l'application Racines - Gestion de généalogie")
                .contact(createContact())
                .license(createLicense());
    }

    private Contact createContact() {
        return new Contact()
                .name("Racines App Team")
                .email("amadoulandoure004@gmail.com");
    }

    private License createLicense() {
        return new License()
                .name("Proprietary")
                .url("https://racines-app-back.onrender.com");
    }

    private List<Server> createServers() {
        return List.of(
                new Server()
                        .url("https://racines-app-back.onrender.com")
                        .description("Production Server"),
                new Server()
                        .url("http://localhost:8080")
                        .description("Local Development Server"));
    }
}

package de.capswap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI capSwapOpenApi() {
        return new OpenAPI().info(new Info()
                .title("CapSwap API")
                .description("B2B-Bartering-Plattform: Unternehmen tauschen Ressourcen und Dienstleistungen direkt miteinander.")
                .version("v1"));
    }
}

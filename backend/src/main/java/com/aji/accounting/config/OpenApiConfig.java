package com.aji.accounting.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI accountingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Akuntansi Manufaktur API")
                .version("v1")
                .description("REST API pembukuan akuntansi manufaktur: GL, AP/AR, persediaan, produksi & biaya, laporan.")
                .contact(new Contact().name("Aji Prakosa")));
    }
}

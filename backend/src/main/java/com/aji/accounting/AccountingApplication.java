package com.aji.accounting;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AccountingApplication {

    public static final String ZONE = "Asia/Jakarta";

    public static void main(String[] args) {
        SpringApplication.run(AccountingApplication.class, args);
    }

    @PostConstruct
    void setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZONE));
    }
}

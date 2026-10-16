package com.aji.accounting.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class JacksonConfig {

    /**
     * BigDecimal dikirim sebagai string ("1250000.00") agar presisi tidak hilang di JavaScript.
     */
    @Bean
    public Module bigDecimalAsStringModule() {
        SimpleModule module = new SimpleModule("BigDecimalAsString");
        module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
        return module;
    }
}

package com.birdex.config;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConstraintsConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer increaseJacksonLimits() {
        return builder -> builder.postConfigurer((ObjectMapper mapper) -> {
            mapper.getFactory().setStreamReadConstraints(
                    StreamReadConstraints.builder()
                            .maxStringLength(50_000_000)
                            .build()
            );
        });
    }
}

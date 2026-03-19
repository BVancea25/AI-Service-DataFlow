package com.dataflow.aiservice.Config.Security;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class CustomRestTemplateConfiguration {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder,
                                     SslBundles sslBundles){
        return builder
                .sslBundle(sslBundles.getBundle("internal-service-bundle"))
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }
}

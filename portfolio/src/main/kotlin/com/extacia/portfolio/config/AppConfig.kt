package com.extacia.portfolio.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder
import java.time.Duration

@Configuration
class AppConfig {

    @Value("\${github.api.timeout.connect:5000}")
    private lateinit var connectTimeout: String

    @Value("\${github.api.timeout.read:10000}")
    private lateinit var readTimeout: String

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder
            .setConnectTimeout(Duration.ofMillis(connectTimeout.toLong()))
            .setReadTimeout(Duration.ofMillis(readTimeout.toLong()))
            .build()
    }
}

package com.extacia.portfolio

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.cache.annotation.EnableCaching
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.slf4j.LoggerFactory

@SpringBootApplication
@EnableScheduling
@EnableCaching
class PortfolioApplication {
    companion object {
        private val logger = LoggerFactory.getLogger(PortfolioApplication::class.java)
    }

    init {
        logger.info("""
            ╔══════════════════════════════════════════════╗
            ║  🚀Portfolio Application Starting...	       ║
            ║   Built with Kotlin + Spring Boot 3.3.4      ║
            ║   GitHub Portfolio Dashboard                 ║
            ╚══════════════════════════════════════════════╝
        """.trimIndent())
    }
}

fun main(args: Array<String>) {
    runApplication<PortfolioApplication>(*args)
}

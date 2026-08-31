package com.extacia.portfolio.controller

import com.extacia.portfolio.dto.RepositoryResponse
import com.extacia.portfolio.model.PortfolioStats
import com.extacia.portfolio.model.RepoTypeCounts
import com.extacia.portfolio.service.PortfolioService
import com.extacia.portfolio.service.GitHubService
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.core.io.Resource
import org.springframework.core.io.ClassPathResource
import org.slf4j.LoggerFactory

@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = ["*"])
class PortfolioController(
    private val portfolioService: PortfolioService,
    private val gitHubService: GitHubService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PortfolioController::class.java)
    }

    @GetMapping("/repos")
    fun getRepositories(
        @RequestParam(required = false) language: String?,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "true") excludeForks: Boolean,
        @RequestParam(defaultValue = "true") excludeArchived: Boolean,
        @RequestParam(defaultValue = "stars") sortBy: String
    ): ResponseEntity<List<RepositoryResponse>> {
        try {
            logger.info("📊 Fetching repositories - language: $language, search: $search, sort: $sortBy")

            val repos = portfolioService.getAllRepositories(
                language = language,
                searchQuery = search,
                excludeForks = excludeForks,
                excludeArchived = excludeArchived,
                sortBy = sortBy
            )

            return ResponseEntity.ok(repos)
        } catch (e: Exception) {
            logger.error("❌ Error fetching repositories: ${e.message}")
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
        }
    }

    @GetMapping("/stats")
    fun getStats(): ResponseEntity<PortfolioStats> {
        try {
            logger.info("📈 Fetching portfolio statistics")
            val stats = portfolioService.getPortfolioStats()
            return ResponseEntity.ok(stats)
        } catch (e: Exception) {
            logger.error("❌ Error fetching stats: ${e.message}")
            // Return empty stats with 503
            val emptyStats = PortfolioStats(
                totalRepos = 0,
                totalStars = 0,
                totalForks = 0,
                totalWatchers = 0,
                languages = emptyMap(),
                topLanguages = emptyList(),
                mostStarredRepo = null,
                mostForkedRepo = null,
                recentlyUpdated = emptyList(),
                repoCountByType = RepoTypeCounts(0, 0, 0, 0)
            )
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(emptyStats)
        }
    }

    @GetMapping("/repos/{name}")
    fun getRepository(@PathVariable name: String): ResponseEntity<RepositoryResponse> {
        logger.info("🔍 Fetching repository: $name")

        val repos = portfolioService.getAllRepositories()
        val repo = repos.find { it.name.equals(name, ignoreCase = true) }

        return if (repo != null) {
            ResponseEntity.ok(repo)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    @GetMapping("/search")
    fun searchRepositories(@RequestParam q: String): ResponseEntity<List<RepositoryResponse>> {
        logger.info("🔎 Searching repositories: $q")
        val results = portfolioService.searchRepositories(q)
        return ResponseEntity.ok(results)
    }

    @GetMapping("/languages")
    fun getLanguages(): ResponseEntity<List<String>> {
        try {
            val stats = portfolioService.getPortfolioStats()
            val languages = stats.languages.keys.sorted()
            return ResponseEntity.ok(languages)
        } catch (e: Exception) {
            logger.error("❌ Error fetching languages: ${e.message}")
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(emptyList())
        }
    }

    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "Portfolio API",
                "version" to "1.0.0",
                "cachedRepos" to gitHubService.getCacheInfo()["cachedRepos"].toString()
            )
        )
    }

    @GetMapping("/cache-info")
    fun getCacheInfo(): ResponseEntity<Map<String, Any>> {
        val info = gitHubService.getCacheInfo()
        return ResponseEntity.ok(info)
    }

   @GetMapping("/cv")
    fun getCV(): ResponseEntity<ByteArray> {
        return try {
            logger.info("📄 Serving CV PDF")

            // Try multiple possible paths
            val possiblePaths = listOf(
                "static/cv/ExtaciaFakero_CV.pdf",
                "cv/ExtaciaFakero_CV.pdf",
                "ExtaciaFakero_CV.pdf"
            )

            var resource: ClassPathResource? = null
            var foundPath = ""

            for (path in possiblePaths) {
                val testResource = ClassPathResource(path)
                if (testResource.exists()) {
                    resource = testResource
                    foundPath = path
                    break
                }
            }

            if (resource != null) {
                logger.info("✅ Found CV at: $foundPath")
                val bytes = resource!!.inputStream.readBytes()
                ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"Extacia_Fakero_CV.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes)
            } else {
                logger.warn("⚠️ CV file not found")
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("❌ Error serving CV: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}

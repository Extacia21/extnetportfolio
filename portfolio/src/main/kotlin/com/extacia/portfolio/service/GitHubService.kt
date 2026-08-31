package com.extacia.portfolio.service

import com.extacia.portfolio.model.Repository
import com.extacia.portfolio.model.LanguageStats
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.HttpClientErrorException
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.Duration

// Extension function to safely get text or null
fun JsonNode.asTextOrNull(): String? {
    return if (this.isNull) null else this.asText()
}

@Service
class GitHubService(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitHubService::class.java)
        private const val GITHUB_API_BASE = "https://api.github.com"
        private val dateFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    }

    @Value("\${github.username}")
    private lateinit var username: String

    @Value("\${github.token}")
    private lateinit var token: String

    // Cache with timestamp
    private var cachedRepos = mutableListOf<Repository>()
    private var lastSyncTime: LocalDateTime? = null
    private var isSyncing = false
    private var rateLimitRemaining = 60
    private var rateLimitReset: LocalDateTime? = null

    @Cacheable("repositories")
    @Scheduled(cron = "\${github.sync.cron:0 0 */6 * * *}")
    fun syncRepositories(): List<Repository> {
        if (isSyncing) {
            logger.info("⏳ Sync already in progress, skipping...")
            return cachedRepos
        }

        if (rateLimitRemaining < 10 && cachedRepos.isNotEmpty()) {
            logger.warn("⚠️ Rate limit low ($rateLimitRemaining remaining). Using cached data.")
            return cachedRepos
        }

        logger.info("🔄 Syncing repositories from GitHub for user: $username")
        isSyncing = true

        return try {
            val repos = fetchAllRepositories()
            cachedRepos.clear()
            cachedRepos.addAll(repos)
            lastSyncTime = LocalDateTime.now()
            logger.info("✅ Synced ${repos.size} repositories")
            repos
        } catch (e: HttpClientErrorException) {
            if (e.statusCode.value() == 403) {
                logger.warn("⚠️ Rate limit exceeded. Using cached data (${cachedRepos.size} repos).")
                if (cachedRepos.isNotEmpty()) {
                    cachedRepos
                } else {
                    emptyList()
                }
            } else {
                logger.error("❌ GitHub API error: ${e.statusCode} - ${e.responseBodyAsString}")
                if (cachedRepos.isNotEmpty()) {
                    logger.info("📦 Returning cached data (${cachedRepos.size} repos)")
                    cachedRepos
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to sync repositories: ${e.message}")
            if (cachedRepos.isNotEmpty()) {
                logger.info("📦 Returning cached data (${cachedRepos.size} repos)")
                cachedRepos
            } else {
                emptyList()
            }
        } finally {
            isSyncing = false
        }
    }

    private fun fetchAllRepositories(): List<Repository> {
        val repos = mutableListOf<Repository>()
        var page = 1
        var hasMore = true

        while (hasMore) {
            val url = "$GITHUB_API_BASE/users/$username/repos?per_page=100&page=$page&sort=updated&direction=desc"
            val response = fetchFromGitHub(url)

            if (response.isEmpty()) {
                hasMore = false
            } else {
                response.forEach { repoNode ->
                    repos.add(parseRepository(repoNode))
                }
                page++
                Thread.sleep(500)
            }
        }

        return repos
    }

    private fun fetchFromGitHub(url: String): List<JsonNode> {
        return try {
            val headers = org.springframework.http.HttpHeaders()
            headers.set("Authorization", "token $token")
            headers.set("Accept", "application/vnd.github.v3+json")

            val entity = org.springframework.http.HttpEntity<String>(headers)
            val response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                entity,
                String::class.java
            )

            val remaining = response.headers.getFirst("X-RateLimit-Remaining")
            val reset = response.headers.getFirst("X-RateLimit-Reset")

            if (remaining != null) {
                rateLimitRemaining = remaining.toInt()
                logger.debug("📊 GitHub API rate limit remaining: $remaining")
            }

            if (reset != null) {
                rateLimitReset = LocalDateTime.now().plusSeconds(reset.toLong() - System.currentTimeMillis() / 1000)
            }

            if (response.statusCode.is2xxSuccessful) {
                val jsonArray = objectMapper.readTree(response.body)
                jsonArray.map { it }
            } else {
                logger.warn("⚠️ GitHub API returned status: ${response.statusCode}")
                emptyList()
            }
        } catch (e: HttpClientErrorException) {
            if (e.statusCode.value() == 403) {
                logger.warn("⚠️ Rate limit exceeded for URL: $url")
                throw e
            }
            logger.error("❌ GitHub API error: ${e.statusCode} - ${e.responseBodyAsString}")
            emptyList()
        } catch (e: Exception) {
            logger.error("❌ Error fetching from GitHub: ${e.message}")
            emptyList()
        }
    }

    private fun parseRepository(node: JsonNode): Repository {
        val languages = fetchLanguages(node.path("languages_url").asText())

        return Repository(
            id = node.path("id").asLong(),
            name = node.path("name").asText(),
            fullName = node.path("full_name").asText(),
            description = node.path("description").asTextOrNull(),
            language = node.path("language").asTextOrNull(),
            stars = node.path("stargazers_count").asInt(),  // Keep as Int
            forks = node.path("forks_count").asInt(),
            openIssues = node.path("open_issues_count").asInt(),
            watchers = node.path("watchers_count").asInt(),
            isArchived = node.path("archived").asBoolean(),
            isFork = node.path("fork").asBoolean(),
            isPrivate = node.path("private").asBoolean(),
            createdAt = parseDate(node.path("created_at").asText()),
            updatedAt = parseDate(node.path("updated_at").asText()),
            pushedAt = node.path("pushed_at").asTextOrNull()?.let { parseDate(it) },
            cloneUrl = node.path("clone_url").asText(),
            htmlUrl = node.path("html_url").asText(),
            homepage = node.path("homepage").asTextOrNull(),
            topics = node.path("topics").map { it.asText() },
            languages = languages
        )
    }

    private fun fetchLanguages(url: String): Map<String, Long> {
        return try {
            val headers = org.springframework.http.HttpHeaders()
            headers.set("Authorization", "token $token")
            headers.set("Accept", "application/vnd.github.v3+json")

            val entity = org.springframework.http.HttpEntity<String>(headers)
            val response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                entity,
                String::class.java
            )

            if (response.statusCode.is2xxSuccessful) {
                val map = objectMapper.readValue(response.body, Map::class.java) as Map<String, Int>
                // Convert Int to Long for consistency
                return map.mapValues { it.value.toLong() }
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            logger.warn("⚠️ Failed to fetch languages for repository: ${e.message}")
            emptyMap()
        }
    }

    private fun parseDate(dateString: String): LocalDateTime {
        return try {
            LocalDateTime.parse(dateString, dateFormatter)
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }

    fun getRepositories(): List<Repository> {
        val isStale = if (lastSyncTime != null) {
            val hoursSinceSync = Duration.between(lastSyncTime, LocalDateTime.now()).toHours()
            hoursSinceSync >= 6
        } else {
            true
        }

        if (cachedRepos.isEmpty() || isStale) {
            logger.info("📦 Cache is stale or empty. Syncing...")
            return syncRepositories()
        }

        logger.info("📦 Using cached data (${cachedRepos.size} repos, last sync: ${lastSyncTime})")
        return cachedRepos
    }

    fun getRepositoryByName(name: String): Repository? {
        return getRepositories().find { it.name.equals(name, ignoreCase = true) }
    }

    fun getCacheInfo(): Map<String, Any> {
        return mapOf(
            "cachedRepos" to cachedRepos.size,
            "lastSyncTime" to (lastSyncTime?.toString() ?: "Never"),
            "rateLimitRemaining" to rateLimitRemaining,
            "rateLimitReset" to (rateLimitReset?.toString() ?: "Unknown")
        )
    }

    @Cacheable("language_stats")
    fun getLanguageStats(): List<LanguageStats> {
        val repos = getRepositories()
        val totalRepos = repos.size

        if (totalRepos == 0) return emptyList()

        val languageCount = repos
            .filter { !it.isFork && !it.isArchived }
            .mapNotNull { it.language }
            .groupingBy { it }
            .eachCount()

        return languageCount.map { (name, count) ->
            LanguageStats(
                name = name,
                count = count,
                percentage = (count.toDouble() / totalRepos * 100)
            )
        }.sortedByDescending { it.count }
    }
}

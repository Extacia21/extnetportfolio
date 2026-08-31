package com.extacia.portfolio.service

import com.extacia.portfolio.model.PortfolioStats
import com.extacia.portfolio.model.RepoTypeCounts
import com.extacia.portfolio.model.LanguageStats
import com.extacia.portfolio.dto.RepositoryResponse
import org.springframework.stereotype.Service

@Service
class PortfolioService(
    private val gitHubService: GitHubService
) {

    fun getAllRepositories(
        language: String? = null,
        searchQuery: String? = null,
        excludeForks: Boolean = true,
        excludeArchived: Boolean = true,
        sortBy: String = "stars"
    ): List<RepositoryResponse> {
        var repos = gitHubService.getRepositories()

        if (excludeForks) {
            repos = repos.filter { !it.isFork }
        }

        if (excludeArchived) {
            repos = repos.filter { !it.isArchived }
        }

        if (!language.isNullOrBlank()) {
            repos = repos.filter { it.language?.equals(language, ignoreCase = true) ?: false }
        }

        if (!searchQuery.isNullOrBlank()) {
            val query = searchQuery.lowercase()
            repos = repos.filter {
                it.name.lowercase().contains(query) ||
                it.description?.lowercase()?.contains(query) ?: false ||
                it.topics.any { topic -> topic.lowercase().contains(query) }
            }
        }

        repos = when (sortBy.lowercase()) {
            "stars" -> repos.sortedByDescending { it.stars }
            "forks" -> repos.sortedByDescending { it.forks }
            "updated" -> repos.sortedByDescending { it.updatedAt }
            "created" -> repos.sortedByDescending { it.createdAt }
            "name" -> repos.sortedBy { it.name }
            else -> repos
        }

        return repos.map { RepositoryResponse.fromRepository(it) }
    }

    fun getPortfolioStats(): PortfolioStats {
        val repos = gitHubService.getRepositories()
        val visibleRepos = repos.filter { !it.isFork && !it.isArchived }

        val totalRepos = visibleRepos.size
        val totalStars = visibleRepos.sumOf { it.stars }
        val totalForks = visibleRepos.sumOf { it.forks }
        val totalWatchers = visibleRepos.sumOf { it.watchers }

        // Language statistics - use Int for counts
        val languageCount = visibleRepos
            .mapNotNull { it.language }
            .groupingBy { it }
            .eachCount()
            .mapValues { it.value } // Already Int

        val topLanguages = languageCount.map { (name, count) ->
            LanguageStats(
                name = name,
                count = count,
                percentage = (count.toDouble() / totalRepos * 100)
            )
        }.sortedByDescending { it.count }

        val mostStarred = visibleRepos.maxByOrNull { it.stars }?.name
        val mostForked = visibleRepos.maxByOrNull { it.forks }?.name
        val recentRepos = visibleRepos.sortedByDescending { it.updatedAt }.take(5).map { it.name }

        val repoTypes = RepoTypeCounts(
            public = repos.count { !it.isPrivate },
            private = repos.count { it.isPrivate },
            forked = repos.count { it.isFork },
            archived = repos.count { it.isArchived }
        )

        return PortfolioStats(
            totalRepos = totalRepos,
            totalStars = totalStars,
            totalForks = totalForks,
            totalWatchers = totalWatchers,
            languages = languageCount,
            topLanguages = topLanguages,
            mostStarredRepo = mostStarred,
            mostForkedRepo = mostForked,
            recentlyUpdated = recentRepos,
            repoCountByType = repoTypes
        )
    }

    fun searchRepositories(query: String): List<RepositoryResponse> {
        return getAllRepositories(searchQuery = query)
    }
}
